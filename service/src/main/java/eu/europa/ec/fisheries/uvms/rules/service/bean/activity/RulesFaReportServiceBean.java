/*
 Developed by the European Commission - Directorate General for Maritime Affairs and Fisheries @ European Union, 2015-2016.

 This file is part of the Integrated Fisheries Data Management (IFDM) Suite. The IFDM Suite is free software: you can redistribute it
 and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of
 the License, or any later version. The IFDM Suite is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 details. You should have received a copy of the GNU General Public License along with the IFDM Suite. If not, see <http://www.gnu.org/licenses/>.
 */

package eu.europa.ec.fisheries.uvms.rules.service.bean.activity;

import javax.annotation.PostConstruct;
import javax.ejb.*;
import javax.xml.bind.UnmarshalException;
import java.util.*;
import java.util.stream.Collectors;
import eu.europa.ec.fisheries.schema.rules.module.v1.SetFLUXFAReportMessageRequest;
import eu.europa.ec.fisheries.schema.rules.rule.v1.RawMsgType;
import eu.europa.ec.fisheries.uvms.activity.model.schemas.MessageType;
import eu.europa.ec.fisheries.uvms.commons.message.api.MessageException;
import eu.europa.ec.fisheries.uvms.commons.service.exception.ServiceException;
import eu.europa.ec.fisheries.uvms.exchange.model.exception.ExchangeModelMarshallException;
import eu.europa.ec.fisheries.uvms.exchange.model.mapper.ExchangeModuleRequestMapper;
import eu.europa.ec.fisheries.uvms.rules.dao.RulesDao;
import eu.europa.ec.fisheries.uvms.rules.dto.GearMatrix;
import eu.europa.ec.fisheries.uvms.rules.entity.FADocumentID;
import eu.europa.ec.fisheries.uvms.rules.entity.FAUUIDType;
import eu.europa.ec.fisheries.uvms.rules.message.consumer.bean.ActivityOutQueueConsumer;
import eu.europa.ec.fisheries.uvms.rules.service.AssetService;
import eu.europa.ec.fisheries.uvms.rules.service.bean.RulePostProcessBean;
import eu.europa.ec.fisheries.uvms.rules.service.bean.RulesConfigurationCache;
import eu.europa.ec.fisheries.uvms.rules.service.bean.RulesEngineBean;
import eu.europa.ec.fisheries.uvms.rules.service.bean.RulesExchangeServiceBean;
import eu.europa.ec.fisheries.uvms.rules.service.business.AbstractFact;
import eu.europa.ec.fisheries.uvms.rules.service.business.ValidationResult;
import eu.europa.ec.fisheries.uvms.rules.service.config.ExtraValueType;
import eu.europa.ec.fisheries.uvms.rules.service.exception.RulesValidationException;
import eu.europa.ec.fisheries.uvms.rules.service.mapper.FAReportQueryResponseIdsMapper;
import eu.europa.ec.fisheries.uvms.rules.service.mapper.RulesFLUXMessageHelper;
import eu.europa.ec.fisheries.uvms.rules.service.mapper.xpath.util.XPathRepository;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import un.unece.uncefact.data.standard.fluxfareportmessage._3.FLUXFAReportMessage;
import un.unece.uncefact.data.standard.fluxresponsemessage._6.FLUXResponseMessage;
import un.unece.uncefact.data.standard.unqualifieddatatype._20.IDType;
import static eu.europa.ec.fisheries.uvms.rules.service.config.BusinessObjectType.RECEIVING_FA_REPORT_MSG;
import static eu.europa.ec.fisheries.uvms.rules.service.config.ExtraValueType.*;

@Slf4j
@LocalBean
@Stateless
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class RulesFaReportServiceBean {

    private static final ValidationResult FAILURE = new ValidationResult(true, false, false, null);

    private FAReportQueryResponseIdsMapper faIdsMapper;

    @EJB
    private RulesConfigurationCache rulesConfigurationCache;

    @EJB
    private RulesEngineBean rulesEngine;

    @EJB
    private RulePostProcessBean rulePostProcess;

    @EJB
    private GearMatrix fishingGearTypeCharacteristics;

    @EJB
    private RulesActivityServiceBean activityService;

    @EJB
    private RulesExchangeServiceBean exchangeService;

    @EJB
    private AssetService assetService;

    @EJB
    private ActivityOutQueueConsumer activityConsumer;

    @EJB
    private RulesFAResponseServiceBean faResponseValidatorAndSender;

    @EJB
    private RulesDao rulesDao;

    private RulesFLUXMessageHelper fluxMessageHelper;

    @PostConstruct
    public void init() {
        faIdsMapper = FAReportQueryResponseIdsMapper.INSTANCE;
        fluxMessageHelper = new RulesFLUXMessageHelper(rulesConfigurationCache);
    }

    public void evaluateIncomingFLUXFAReport(SetFLUXFAReportMessageRequest request) {
        final String requestStr = request.getRequest();
        final String exchangeLogGuid = request.getLogGuid();
        FLUXFAReportMessage fluxfaReportMessage = null;
        try {
            fluxfaReportMessage = fluxMessageHelper.unMarshallAndValidateSchema(requestStr);
            List<IDType> messageGUID = collectReportMessageIds(fluxfaReportMessage);

            Set<FADocumentID> idsFromIncomingMessage = fluxMessageHelper.mapToFADocumentID(fluxfaReportMessage);
            List<FADocumentID> reportAndMessageIdsFromDB = rulesDao.loadFADocumentIDByIdsByIds(idsFromIncomingMessage);

            List<String> faIdsPerTripsFromMessage = fluxMessageHelper.collectFaIdsAndTripIds(fluxfaReportMessage);
            List<String> faIdsPerTripsListFromDb = rulesDao.loadExistingFaIdsPerTrip(faIdsPerTripsFromMessage);

            Map<ExtraValueType, Object> extraValues = collectExtraValues(request.getSenderOrReceiver(), fluxfaReportMessage, reportAndMessageIdsFromDB, faIdsPerTripsListFromDb, true);
            extraValues.put(XML, requestStr);
            Collection<AbstractFact> faReportFacts = rulesEngine.evaluate(RECEIVING_FA_REPORT_MSG, fluxfaReportMessage, extraValues, String.valueOf(messageGUID));

            idsFromIncomingMessage.removeAll(reportAndMessageIdsFromDB);
            faIdsPerTripsFromMessage.removeAll(faIdsPerTripsListFromDb);

            ValidationResult faReportValidationResult = rulePostProcess.checkAndUpdateValidationResult(faReportFacts, requestStr, exchangeLogGuid, RawMsgType.FA_REPORT);
            exchangeService.updateExchangeMessage(exchangeLogGuid, fluxMessageHelper.calculateMessageValidationStatus(faReportValidationResult));

            if (faReportValidationResult != null && !faReportValidationResult.isError()) {
                log.debug(" The Validation of Report is successful, forwarding message to Activity.");
                boolean hasPermissions = activityService.checkSubscriptionPermissions(requestStr, MessageType.FLUX_FA_REPORT_MESSAGE);
                if (hasPermissions) {
                    log.debug(" Request has permissions. Going to send FaReportMessage to Activity Module...");
                    activityService.sendRequestToActivity(requestStr, request.getType(), MessageType.FLUX_FA_REPORT_MESSAGE, exchangeLogGuid);
                    rulesDao.saveFaIdsPerTripList(faIdsPerTripsFromMessage);

                    Set<FADocumentID> result = idsFromIncomingMessage.stream()
                            .filter(faDocumentID -> !FAUUIDType.FA_REPORT_REF_ID.equals(faDocumentID.getType()))
                            .collect(Collectors.toSet());

                    rulesDao.createFaDocumentIdEntity(result);// remove ref ids

                } else {
                    log.debug(" Request doesn't have permissions!");
                }
            } else {
                log.debug("Validation resulted in errors.");
            }

            FLUXResponseMessage fluxResponseMessage = fluxMessageHelper.generateFluxResponseMessageForFaReport(faReportValidationResult, fluxfaReportMessage);
            XPathRepository.INSTANCE.clear(faReportFacts);

            exchangeService.evaluateAndSendToExchange(fluxResponseMessage, request, request.getType(), fluxMessageHelper.isCorrectUUID(messageGUID), MDC.getCopyOfContextMap());

        } catch (UnmarshalException e) {
            log.error(" Error while trying to parse FLUXFAReportMessage received message! It is malformed!");
            exchangeService.updateExchangeMessage(exchangeLogGuid, fluxMessageHelper.calculateMessageValidationStatus(FAILURE));
            exchangeService.sendFLUXResponseMessageOnException(e.getMessage(), requestStr, request, null);
        } catch (RulesValidationException | ServiceException e) {
            log.error(" Error during validation of the received FLUXFAReportMessage!", e);
            exchangeService.updateExchangeMessage(exchangeLogGuid, fluxMessageHelper.calculateMessageValidationStatus(FAILURE));
            exchangeService.sendFLUXResponseMessageOnException(e.getMessage(), requestStr, request, fluxfaReportMessage);
        }
        log.debug("Finished eval of FLUXFAReportMessage " + exchangeLogGuid);
    }

    public void evaluateOutgoingFaReport(SetFLUXFAReportMessageRequest request) {
        final String requestStr = request.getRequest();
        final String logGuid = request.getLogGuid();
        log.info(" Going to evaluate FLUXFAReportMessage with GUID [[ " + logGuid + " ]].");
        FLUXFAReportMessage fluxfaReportMessage = null;
        try {
            fluxfaReportMessage = fluxMessageHelper.unMarshallAndValidateSchema(requestStr);
            List<IDType> messageGUID = collectReportMessageIds(fluxfaReportMessage);

            log.info("Evaluating FLUXFAReportMessage with ID [ " + messageGUID + " ].");

            Set<FADocumentID> idsFromIncommingMessage = fluxMessageHelper.mapToFADocumentID(fluxfaReportMessage);
            List<String> faIdsPerTripsFromMessage = fluxMessageHelper.collectFaIdsAndTripIds(fluxfaReportMessage);

            List<FADocumentID> reportAndMessageIdsFromDB = rulesDao.loadFADocumentIDByIdsByIds(idsFromIncommingMessage);
            List<String> faIdsPerTripsListFromDb = rulesDao.loadExistingFaIdsPerTrip(faIdsPerTripsFromMessage);

            Map<ExtraValueType, Object> extraValuesMap = collectExtraValues(request.getSenderOrReceiver(), fluxfaReportMessage, reportAndMessageIdsFromDB, faIdsPerTripsListFromDb, false);
            extraValuesMap.put(XML, requestStr);
            Collection<AbstractFact> faReportFacts = rulesEngine.evaluate(RECEIVING_FA_REPORT_MSG, fluxfaReportMessage, extraValuesMap, String.valueOf(fluxfaReportMessage.getFLUXReportDocument().getIDS()));
            ValidationResult faReportValidationResult = rulePostProcess.checkAndUpdateValidationResult(faReportFacts, requestStr, logGuid, RawMsgType.FA_REPORT);
            if (faReportValidationResult != null && !faReportValidationResult.isError()) {
                log.info(" The Validation of FLUXFAReportMessage is successful, forwarding message to Exchange");
                exchangeService.sendToExchange(ExchangeModuleRequestMapper.createSendFaReportMessageRequest(request.getRequest(), "movement", logGuid, request.getFluxDataFlow(), request.getSenderOrReceiver(), request.getOnValue(), "IMPLEMENTTODT_FROM_REQUEST", "IMPLEMENTTO_FROM_REQUEST", "IMPLEMENTTO_FROM_REQUEST"));
            } else {
                log.info(" Validation resulted in errors. Not going to send msg to Exchange module..");
            }
            XPathRepository.INSTANCE.clear(faReportFacts);

        } catch (UnmarshalException e) {
            log.error(" Error while trying to parse FLUXFAReportMessage received message! It is malformed!");
            exchangeService.updateExchangeMessage(logGuid, fluxMessageHelper.calculateMessageValidationStatus(FAILURE));
            exchangeService.sendFLUXResponseMessageOnException(e.getMessage(), requestStr, request, null);
        } catch (RulesValidationException e) {
            log.error(" Error during validation of the received FLUXFAReportMessage!", e);
            exchangeService.updateExchangeMessage(logGuid, fluxMessageHelper.calculateMessageValidationStatus(FAILURE));
            exchangeService.sendFLUXResponseMessageOnException(e.getMessage(), requestStr, request, fluxfaReportMessage);
        } catch (MessageException | ExchangeModelMarshallException e) {
            log.error(" Error during validation of the received FLUXFAReportMessage!", e);
        }
    }

    private Map<ExtraValueType, Object> collectExtraValues(String senderReceiver, FLUXFAReportMessage fluxfaReportMessage, List<FADocumentID> reportAndMessageIdsFromDB, List<String> faIdsPerTripsListFromDb, boolean isIncomingMessage) {
        Map<ExtraValueType, Object> extraValues = new EnumMap<>(ExtraValueType.class);
        extraValues.put(SENDER_RECEIVER, senderReceiver);
        extraValues.put(ExtraValueType.ASSET, assetService.findHistoryOfAssetBy(fluxfaReportMessage.getFAReportDocuments()));
        extraValues.put(FA_QUERY_AND_REPORT_IDS, faIdsMapper.mapToFishingActivityIdDto(reportAndMessageIdsFromDB));
        extraValues.put(FISHING_GEAR_TYPE_CHARACTERISTICS, fishingGearTypeCharacteristics.getMatrix());
        if (isIncomingMessage) {
            extraValues.put(TRIP_ID, faIdsPerTripsListFromDb);
        }
        return extraValues;
    }

    private List<IDType> collectReportMessageIds(FLUXFAReportMessage fluxfaReportMessage) {
        List<IDType> reportGUID = null;
        if (fluxfaReportMessage.getFLUXReportDocument() != null) {
            reportGUID = fluxfaReportMessage.getFLUXReportDocument().getIDS();
        }
        return reportGUID;
    }

}
