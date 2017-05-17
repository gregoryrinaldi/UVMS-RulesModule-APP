/*
 *
 * Developed by the European Commission - Directorate General for Maritime Affairs and Fisheries © European Union, 2015-2016.
 *
 * This file is part of the Integrated Fisheries Data Management (IFDM) Suite. The IFDM Suite is free software: you can redistribute it
 * and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or any later version. The IFDM Suite is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details. You should have received a copy of the GNU General Public License along with the IFDM Suite. If not, see <http://www.gnu.org/licenses/>.
 *
 *
 */

package eu.europa.ec.fisheries.uvms.rules.service.bean;

import eu.europa.ec.fisheries.schema.exchange.v1.ExchangeLogStatusTypeType;
import eu.europa.ec.fisheries.schema.rules.module.v1.ReceiveSalesQueryRequest;
import eu.europa.ec.fisheries.schema.rules.module.v1.ReceiveSalesReportRequest;
import eu.europa.ec.fisheries.schema.rules.module.v1.ReceiveSalesResponseRequest;
import eu.europa.ec.fisheries.schema.rules.rule.v1.ValidationMessageType;
import eu.europa.ec.fisheries.schema.sales.SalesModuleMethod;
import eu.europa.ec.fisheries.uvms.activity.model.exception.ActivityModelMarshallException;
import eu.europa.ec.fisheries.uvms.activity.model.mapper.ActivityModuleRequestMapper;
import eu.europa.ec.fisheries.uvms.exchange.model.exception.ExchangeModelMarshallException;
import eu.europa.ec.fisheries.uvms.exchange.model.mapper.ExchangeModuleRequestMapper;
import eu.europa.ec.fisheries.uvms.mdr.model.exception.MdrModelMarshallException;
import eu.europa.ec.fisheries.uvms.mdr.model.mapper.MdrModuleMapper;
import eu.europa.ec.fisheries.uvms.rules.message.constants.DataSourceQueue;
import eu.europa.ec.fisheries.uvms.rules.message.consumer.RulesResponseConsumer;
import eu.europa.ec.fisheries.uvms.rules.message.exception.MessageException;
import eu.europa.ec.fisheries.uvms.rules.message.producer.RulesMessageProducer;
import eu.europa.ec.fisheries.uvms.rules.model.dto.ValidationResultDto;
import eu.europa.ec.fisheries.uvms.rules.model.exception.RulesModelMarshallException;
import eu.europa.ec.fisheries.uvms.rules.model.mapper.JAXBMarshaller;
import eu.europa.ec.fisheries.uvms.rules.service.MessageService;
import eu.europa.ec.fisheries.uvms.rules.service.business.AbstractFact;
import eu.europa.ec.fisheries.uvms.rules.service.config.BusinessObjectType;
import eu.europa.ec.fisheries.uvms.rules.service.exception.RulesServiceException;
import eu.europa.ec.fisheries.uvms.rules.service.exception.RulesValidationException;
import eu.europa.ec.fisheries.uvms.rules.service.helper.SalesMessageServiceBeanHelper;
import eu.europa.ec.fisheries.uvms.sales.model.exception.SalesMarshallException;
import eu.europa.ec.fisheries.uvms.sales.model.mapper.SalesModuleRequestMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import un.unece.uncefact.data.standard.fluxfareportmessage._3.FLUXFAReportMessage;
import un.unece.uncefact.data.standard.fluxresponsemessage._6.FLUXResponseMessage;
import un.unece.uncefact.data.standard.reusableaggregatebusinessinformationentity._20.FLUXParty;
import un.unece.uncefact.data.standard.reusableaggregatebusinessinformationentity._20.FLUXResponseDocument;
import un.unece.uncefact.data.standard.reusableaggregatebusinessinformationentity._20.ValidationQualityAnalysis;
import un.unece.uncefact.data.standard.reusableaggregatebusinessinformationentity._20.ValidationResultDocument;
import un.unece.uncefact.data.standard.unqualifieddatatype._20.CodeType;
import un.unece.uncefact.data.standard.unqualifieddatatype._20.DateTimeType;
import un.unece.uncefact.data.standard.unqualifieddatatype._20.IDType;
import un.unece.uncefact.data.standard.unqualifieddatatype._20.TextType;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.util.*;

/**
 * Created by padhyad on 5/9/2017.
 */
@Stateless
@Slf4j
public class MessageServiceBean implements MessageService {

    @EJB
    RulesResponseConsumer consumer;

    @EJB
    RulesMessageProducer producer;

    @EJB
    RulesEngineBean rulesEngine;

    @EJB
    RulePostProcessBean rulePostprocessBean;

    @EJB
    RulesPreProcessBean rulesPreProcessBean;

    @EJB
    SalesMessageServiceBeanHelper salesHelper;

    @Override
    public void receiveSalesQueryRequest(String request) {
        try {
            ReceiveSalesQueryRequest receiveSalesQueryRequest = eu.europa.ec.fisheries.uvms.sales.model.mapper.JAXBMarshaller.unmarshallString(request, ReceiveSalesQueryRequest.class);

            //validate
            salesHelper.handleReceiveSalesQueryRequest(receiveSalesQueryRequest, rulesEngine);

            //send to sales
            String salesReportRequestAsString = SalesModuleRequestMapper.createSalesQueryRequest(receiveSalesQueryRequest.getRequest(), SalesModuleMethod.QUERY);
            sendToSales(salesReportRequestAsString);

            //update log status
            String updateLogStatusRequest = ExchangeModuleRequestMapper.createUpdateLogStatusRequest("guid", ExchangeLogStatusTypeType.FAILED);
            sendToExchange(updateLogStatusRequest);
        } catch (SalesMarshallException | RulesValidationException | MessageException | ExchangeModelMarshallException e) {
            log.error("Couldn't marshall FLUXSalesQueryMessage", e);
        }
    }

    @Override
    public void receiveSalesReportRequest(String request) {
        try {
            ReceiveSalesReportRequest receiveSalesReportRequest = eu.europa.ec.fisheries.uvms.sales.model.mapper.JAXBMarshaller.unmarshallString(request, ReceiveSalesReportRequest.class);

            //validate
            salesHelper.handleReceiveSalesReportRequest(receiveSalesReportRequest, rulesEngine);

            //send to sales
            String salesReportRequestAsString = SalesModuleRequestMapper.createSalesReportRequest(receiveSalesReportRequest.getRequest(), SalesModuleMethod.REPORT);
            sendToSales(salesReportRequestAsString);

            //update log status
            String updateLogStatusRequest = ExchangeModuleRequestMapper.createUpdateLogStatusRequest(receiveSalesReportRequest.getLogGuid(), ExchangeLogStatusTypeType.FAILED);
            sendToExchange(updateLogStatusRequest);

        } catch (SalesMarshallException | RulesValidationException | MessageException | ExchangeModelMarshallException e) {
            log.error("Couldn't marshall FLUXSalesQueryMessage", e);
        }
    }

    @Override
    public void receiveSalesResponseRequest(String request) {
        try {
            ReceiveSalesResponseRequest rulesRequest = eu.europa.ec.fisheries.uvms.sales.model.mapper.JAXBMarshaller.unmarshallString(request, ReceiveSalesResponseRequest.class);

            //validate
            salesHelper.handleReceiveSalesResponseRequest(rulesRequest, rulesEngine);

            //update log status
            String updateLogStatusRequest = ExchangeModuleRequestMapper.createUpdateLogStatusRequest(rulesRequest.getLogGuid(), ExchangeLogStatusTypeType.FAILED);
            sendToExchange(updateLogStatusRequest);
        } catch (SalesMarshallException | RulesValidationException | MessageException | ExchangeModelMarshallException e) {
            log.error("Couldn't marshall FLUXSalesQueryMessage", e);
        }
    }

    @Override
    public void sendSalesResponseRequest(String request) {
        try {
            String sendSalesResponseRequestAsText = salesHelper.handleSendSalesResponseRequest(request, rulesEngine);
            sendToExchange(sendSalesResponseRequestAsText);
        } catch (ExchangeModelMarshallException | RulesValidationException | MessageException | SalesMarshallException e) {
            log.error("Couldn't marshall SendSalesResponseRequest", e);
        }
    }

    @Override
    public void sendSalesReportRequest(String request) {
        try {
            String sendSalesReportRequestAsText = salesHelper.handleSendSalesReportRequest(request, rulesEngine);
            sendToExchange(sendSalesReportRequestAsText);
        } catch (ExchangeModelMarshallException | RulesValidationException | MessageException | SalesMarshallException e) {
            log.error("Couldn't marshall SendSalesReportRequest", e);
        }
    }

    @Override
    public void setFLUXFAReportMessageReceived(String fluxFAReportMessage, eu.europa.ec.fisheries.schema.rules.exchange.v1.PluginType pluginType, String username) throws RulesServiceException {
        log.debug("inside setFLUXFAReportMessageReceived", fluxFAReportMessage);
        try {
            FLUXFAReportMessage fluxfaReportMessage = JAXBMarshaller.unMarshallMessage(fluxFAReportMessage, FLUXFAReportMessage.class);
            if (fluxfaReportMessage != null) {
                FLUXResponseMessage fluxResponseMessageType;
                Map<Boolean, ValidationResultDto> validationMap = rulesPreProcessBean.checkDuplicateIdInRequest(fluxfaReportMessage);
                boolean isContinueValidation = validationMap.entrySet().iterator().next().getKey();
                log.info("Validation continue : {}", isContinueValidation);

                if (isContinueValidation) {
                    log.info("Trigger rule engine to do validation of incoming message");
                    List<AbstractFact> faReportFacts = rulesEngine.evaluate(BusinessObjectType.FLUX_ACTIVITY_REQUEST_MSG, fluxfaReportMessage);
                    ValidationResultDto faReportValidationResult = rulePostprocessBean.checkAndUpdateValidationResult(faReportFacts, fluxFAReportMessage);
                    updateValidationResultWithExisting(faReportValidationResult, validationMap.get(isContinueValidation));

                    // TODO send exchange ack

                    if (!faReportValidationResult.isError()) {
                        log.info("Validation of Report is successful, forwarding message to Activity");
                        sendRequestToActivity(fluxFAReportMessage, username, pluginType);
                    }
                    fluxResponseMessageType = generateFluxResponseMessage(faReportValidationResult, fluxfaReportMessage.getFLUXReportDocument().getIDS());
                } else {
                    fluxResponseMessageType = generateFluxResponseMessage(validationMap.get(isContinueValidation), fluxfaReportMessage.getFLUXReportDocument().getIDS());
                }
                sendResponseToExchange(fluxResponseMessageType, username);
            }
        } catch (RulesValidationException e) {
            log.error(e.getMessage(), e);
            // TODO send exchange ACK and send Response
        }
        catch (RulesModelMarshallException e) {
            throw new RulesServiceException(e.getMessage(), e);
        }
    }

    private void updateValidationResultWithExisting(ValidationResultDto faReportValidationResult, ValidationResultDto previousValidationResultDto) {
        if (previousValidationResultDto != null) {
            faReportValidationResult.setIsError(faReportValidationResult.isError() || previousValidationResultDto.isError());
            faReportValidationResult.setIsWarning(faReportValidationResult.isWarning() || previousValidationResultDto.isWarning());
            faReportValidationResult.setIsOk(faReportValidationResult.isOk() || previousValidationResultDto.isOk());
            faReportValidationResult.getValidationMessages().addAll(previousValidationResultDto.getValidationMessages());
        }
    }

    @Override
    public FLUXResponseMessage generateFluxResponseMessage(ValidationResultDto faReportValidationResult, List<IDType> idTypes) {
        FLUXResponseMessage responseMessage = new FLUXResponseMessage();
        try {
            FLUXResponseDocument fluxResponseDocument = new FLUXResponseDocument();

            IDType responseId = new IDType();
            responseId.setValue(String.valueOf(new Random().nextInt()));
            responseId.setSchemeID(UUID.randomUUID().toString());
            fluxResponseDocument.setIDS(Arrays.asList(responseId)); // Set random ID
            fluxResponseDocument.setReferencedID((idTypes != null && !idTypes.isEmpty()) ? idTypes.get(0) : null); // Set Request Id
            GregorianCalendar date = DateTime.now(DateTimeZone.UTC).toGregorianCalendar();
            XMLGregorianCalendar calender = DatatypeFactory.newInstance().newXMLGregorianCalendar(date);
            DateTimeType dateTime = new DateTimeType();
            dateTime.setDateTime(calender);
            fluxResponseDocument.setCreationDateTime(dateTime); // Set creation date time

            CodeType responseCode = new CodeType();
            if (faReportValidationResult.isError()) {
                responseCode.setValue("NOK");
            } else if (faReportValidationResult.isWarning()) {
                responseCode.setValue("WOK");
            } else {
                responseCode.setValue("OK");
            }
            responseCode.setListID("FLUX_GP_RESPONSE");
            fluxResponseDocument.setResponseCode(responseCode); // Set response Code

            if (faReportValidationResult.isError() || faReportValidationResult.isWarning()) {
                TextType rejectionReason = new TextType();
                rejectionReason.setValue("VALIDATION");
                fluxResponseDocument.setRejectionReason(rejectionReason); // Set rejection reason
            }

            fluxResponseDocument.setRelatedValidationResultDocuments(getValidationResultDocument(faReportValidationResult)); // Set validation result

            fluxResponseDocument.setRespondentFLUXParty(getRespondedFluxParty()); // Set flux party in the response

            responseMessage.setFLUXResponseDocument(fluxResponseDocument);
        } catch (DatatypeConfigurationException e) {
            log.error(e.getMessage(), e);
        }
        return responseMessage;
    }

    private List<ValidationResultDocument> getValidationResultDocument(ValidationResultDto faReportValidationResult) throws DatatypeConfigurationException {
        ValidationResultDocument validationResultDocument = new ValidationResultDocument();

        GregorianCalendar date = DateTime.now(DateTimeZone.UTC).toGregorianCalendar();
        XMLGregorianCalendar calender = DatatypeFactory.newInstance().newXMLGregorianCalendar(date);
        DateTimeType dateTime = new DateTimeType();
        dateTime.setDateTime(calender);
        validationResultDocument.setCreationDateTime(dateTime);

        IDType idType = new IDType();
        idType.setValue("XEU"); // TODO to be received from Global config
        idType.setSchemeID("FLUX_GP_PARTY");
        validationResultDocument.setValidatorID(idType);

        List<ValidationQualityAnalysis> validationQuality = new ArrayList<>();
        for (ValidationMessageType validationMessage : faReportValidationResult.getValidationMessages()) {
            ValidationQualityAnalysis analysis = new ValidationQualityAnalysis();

            IDType identification = new IDType();
            identification.setValue(validationMessage.getBrId());
            analysis.setID(identification);

            CodeType level = new CodeType();
            level.setValue(validationMessage.getLevel());
            analysis.setLevelCode(level);

            CodeType type = new CodeType();
            type.setValue(validationMessage.getErrorType().value());
            analysis.setTypeCode(type);

            TextType text = new TextType();
            text.setValue(validationMessage.getMessage());
            analysis.getResults().add(text);

            TextType referenceItem = new TextType();
            text.setValue("X-path"); // SET Xpath
            analysis.getReferencedItems().add(referenceItem);

            validationQuality.add(analysis);
        }
        validationResultDocument.setRelatedValidationQualityAnalysises(validationQuality);
        return Arrays.asList(validationResultDocument);
    }

    private FLUXParty getRespondedFluxParty() {
        IDType idType = new IDType();
        idType.setValue("XEU"); // TODO to be received from Global config
        idType.setSchemeID("FLUX_GP_PARTY");

        FLUXParty fluxParty = new FLUXParty();
        fluxParty.setIDS(Arrays.asList(idType));
        return fluxParty;
    }

    public void sendRequestToActivity(String fluxFAReportMessage, String username, eu.europa.ec.fisheries.schema.rules.exchange.v1.PluginType pluginType) throws RulesServiceException {
        try {
            String setFLUXFAReportMessageRequest = ActivityModuleRequestMapper.mapToSetFLUXFAReportMessageRequest(fluxFAReportMessage, username, pluginType.toString());
            producer.sendDataSourceMessage(setFLUXFAReportMessageRequest, DataSourceQueue.ACTIVITY);
        } catch (ActivityModelMarshallException | MessageException e) {
            throw new RulesServiceException(e.getMessage(), e);
        }
    }

    private void sendToSales(String message) throws MessageException {
        producer.sendDataSourceMessage(message, DataSourceQueue.SALES);
    }

    private void sendToExchange(String message) throws MessageException {
        producer.sendDataSourceMessage(message, DataSourceQueue.EXCHANGE);
    }

    @Override
    public void sendResponseToExchange(FLUXResponseMessage fluxResponseMessageType, String username) throws RulesServiceException {
        try {
            //Validate response message
            String fluxResponse = JAXBMarshaller.marshallJaxBObjectToString(fluxResponseMessageType);
            List<AbstractFact> fluxResponseFacts = rulesEngine.evaluate(BusinessObjectType.FLUX_ACTIVITY_RESPONSE_MSG, fluxResponseMessageType);
            ValidationResultDto fluxResponseValidationResult = rulePostprocessBean.checkAndUpdateValidationResult(fluxResponseFacts, fluxResponse);

            // TODO create final response based on exchange contract
            //Create Response
            String fluxFAReponseText = ExchangeModuleRequestMapper.createFluxFAResponseRequest(fluxResponse, username);
            producer.sendDataSourceMessage(fluxFAReponseText, DataSourceQueue.EXCHANGE);
        } catch (RulesValidationException e) {
            log.error(e.getMessage(), e);
            // TODO send error Response
        } catch (RulesModelMarshallException | ExchangeModelMarshallException | MessageException e) {
            throw new RulesServiceException(e.getMessage(), e);
        }
    }


    /*
	 * Maps a Request String to a eu.europa.ec.fisheries.schema.exchange.module.v1.SetFLUXMDRSyncMessageRequest
	 * to send a message to ExchangeModule
	 *
	 * @see eu.europa.ec.fisheries.uvms.rules.service.RulesService#mapAndSendFLUXMdrRequestToExchange(java.lang.String)
	 */
    @Override
    public void mapAndSendFLUXMdrRequestToExchange(String request) {
        String exchangerStrReq;
        try {
            exchangerStrReq = ExchangeModuleRequestMapper.createFluxMdrSyncEntityRequest(request, StringUtils.EMPTY);
            if(StringUtils.isNotEmpty(exchangerStrReq)){
                producer.sendDataSourceMessage(exchangerStrReq, DataSourceQueue.EXCHANGE);
            } else {
                log.error("ERROR : REQUEST TO BE SENT TO EXCHANGE MODULE RESULTS NULL. NOT SENDING IT!");
            }

        } catch (ExchangeModelMarshallException e) {
            log.error("Unable to marshall SetFLUXMDRSyncMessageRequest in RulesServiceBean.mapAndSendFLUXMdrRequestToExchange(String) : "+e.getMessage());
        } catch (MessageException e) {
            log.error("Unable to send SetFLUXMDRSyncMessageRequest to ExchangeModule : "+e.getMessage());
        }
    }

    @Override
    public void mapAndSendFLUXMdrResponseToMdrModule(String request) {
        String mdrSyncResponseReq;
        try {
            mdrSyncResponseReq = MdrModuleMapper.createFluxMdrSyncEntityRequest(request, StringUtils.EMPTY);
            if(StringUtils.isNotEmpty(mdrSyncResponseReq)){
                producer.sendDataSourceMessage(mdrSyncResponseReq, DataSourceQueue.MDR_EVENT);
            } else {
                log.error("ERROR : REQUEST TO BE SENT TO MDR MODULE RESULTS NULL. NOT SENDING IT!");
            }
        } catch (MdrModelMarshallException e) {
            log.error("Unable to marshall SetFLUXMDRSyncMessageResponse in RulesServiceBean.mapAndSendFLUXMdrResponseToMdrModule(String) : "+e.getMessage());
        } catch (MessageException e) {
            log.error("Unable to send SetFLUXMDRSyncMessageResponse to MDR Module : "+e.getMessage());
        }

    }
}
