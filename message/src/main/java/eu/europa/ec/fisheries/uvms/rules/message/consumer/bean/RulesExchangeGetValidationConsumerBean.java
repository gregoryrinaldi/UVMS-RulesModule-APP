/*
﻿Developed with the contribution of the European Commission - Directorate General for Maritime Affairs and Fisheries
© European Union, 2015-2016.

This file is part of the Integrated Fisheries Data Management (IFDM) Suite. The IFDM Suite is free software: you can
redistribute it and/or modify it under the terms of the GNU General Public License as published by the
Free Software Foundation, either version 3 of the License, or any later version. The IFDM Suite is distributed in
the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details. You should have received a
copy of the GNU General Public License along with the IFDM Suite. If not, see <http://www.gnu.org/licenses/>.
 */
package eu.europa.ec.fisheries.uvms.rules.message.consumer.bean;

import eu.europa.ec.fisheries.schema.rules.module.v1.RulesBaseRequest;
import eu.europa.ec.fisheries.schema.rules.module.v1.RulesModuleMethod;
import eu.europa.ec.fisheries.uvms.commons.message.api.MessageConstants;
import eu.europa.ec.fisheries.uvms.commons.message.context.MappedDiagnosticContext;
import eu.europa.ec.fisheries.uvms.rules.message.event.ErrorEvent;
import eu.europa.ec.fisheries.uvms.rules.message.event.GetValidationResultsByRawGuid;
import eu.europa.ec.fisheries.uvms.rules.message.event.carrier.EventMessage;
import eu.europa.ec.fisheries.uvms.rules.model.constant.FaultCode;
import eu.europa.ec.fisheries.uvms.rules.model.exception.RulesModelMarshallException;
import eu.europa.ec.fisheries.uvms.rules.model.mapper.JAXBMarshaller;
import eu.europa.ec.fisheries.uvms.rules.model.mapper.ModuleResponseMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;
import java.util.UUID;

/**
 * Message driven bean that receives all messages that
 * have a message selector set to 'ValidationResultsByRawGuid'.
 */
@MessageDriven(mappedName = MessageConstants.QUEUE_MODULE_RULES, activationConfig = {
        @ActivationConfigProperty(propertyName = MessageConstants.MESSAGING_TYPE_STR, propertyValue = MessageConstants.CONNECTION_TYPE),
        @ActivationConfigProperty(propertyName = MessageConstants.DESTINATION_TYPE_STR, propertyValue = MessageConstants.DESTINATION_TYPE_QUEUE),
        @ActivationConfigProperty(propertyName = MessageConstants.DESTINATION_STR, propertyValue = MessageConstants.RULES_MESSAGE_IN_QUEUE_NAME),
        @ActivationConfigProperty(propertyName = "messageSelector", propertyValue = "messageSelector = 'ValidationResultsByRawGuid'"),
        @ActivationConfigProperty(propertyName = "maxMessagesPerSessions", propertyValue = "1000"),
        @ActivationConfigProperty(propertyName = "initialRedeliveryDelay", propertyValue = "60000"),
        @ActivationConfigProperty(propertyName = "maximumRedeliveries", propertyValue = "3"),
        @ActivationConfigProperty(propertyName = "maxSessions", propertyValue = "10")
})
public class RulesExchangeGetValidationConsumerBean implements MessageListener {

    private static final Logger LOG = LoggerFactory.getLogger(RulesEventMessageConsumerBean.class);

    @Inject
    @GetValidationResultsByRawGuid
    private Event<EventMessage> getValidationResultsByRawMsgGuid;

    @Inject
    @ErrorEvent
    private Event<EventMessage> errorEvent;

    @Override
    public void onMessage(Message message) {
        String id = UUID.randomUUID().toString();
        MDC.put("clientName", id);
        MDC.remove("requestId");
        TextMessage textMessage = (TextMessage) message;
        MappedDiagnosticContext.addMessagePropertiesToThreadMappedDiagnosticContext(textMessage);
        try {
            RulesBaseRequest request = JAXBMarshaller.unmarshallTextMessage(textMessage, RulesBaseRequest.class);
            RulesModuleMethod method = request.getMethod();
            LOG.info("\nRequest message method: {}", method.value());
            switch (method) {
                case GET_VALIDATION_RESULT_BY_RAW_GUID_REQUEST:
                    getValidationResultsByRawMsgGuid.fire(new EventMessage(textMessage));
                    break;
                default:
                    LOG.error("[ Request method '{}' is not implemented ]", method.name());
                    errorEvent.fire(new EventMessage(textMessage, ModuleResponseMapper.createFaultMessage(FaultCode.RULES_MESSAGE, "Method not implemented:" + method.name())));
                    break;
            }
        } catch (NullPointerException | RulesModelMarshallException e) {
            LOG.error("[ Error when receiving message in rules: {}]", e.getMessage());
            errorEvent.fire(new EventMessage(textMessage, ModuleResponseMapper.createFaultMessage(FaultCode.RULES_MESSAGE, "Error when receiving message in rules:" + e.getMessage())));
        } finally {
            MDC.remove("clientName");
        }
    }

}

