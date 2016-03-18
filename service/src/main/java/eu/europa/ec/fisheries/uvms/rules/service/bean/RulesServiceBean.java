package eu.europa.ec.fisheries.uvms.rules.service.bean;

import eu.europa.ec.fisheries.schema.exchange.movement.v1.MovementRefType;
import eu.europa.ec.fisheries.schema.exchange.movement.v1.MovementRefTypeType;
import eu.europa.ec.fisheries.schema.exchange.movement.v1.SetReportMovementType;
import eu.europa.ec.fisheries.schema.mobileterminal.types.v1.*;
import eu.europa.ec.fisheries.schema.movement.search.v1.MovementMapResponseType;
import eu.europa.ec.fisheries.schema.movement.search.v1.MovementQuery;
import eu.europa.ec.fisheries.schema.movement.search.v1.RangeCriteria;
import eu.europa.ec.fisheries.schema.movement.search.v1.RangeKeyType;
import eu.europa.ec.fisheries.schema.movement.v1.MovementBaseType;
import eu.europa.ec.fisheries.schema.movement.v1.MovementType;
import eu.europa.ec.fisheries.schema.rules.alarm.v1.AlarmReportType;
import eu.europa.ec.fisheries.schema.rules.alarm.v1.AlarmStatusType;
import eu.europa.ec.fisheries.schema.rules.asset.v1.AssetId;
import eu.europa.ec.fisheries.schema.rules.asset.v1.AssetIdList;
import eu.europa.ec.fisheries.schema.rules.customrule.v1.CustomRuleType;
import eu.europa.ec.fisheries.schema.rules.customrule.v1.SubscritionOperationType;
import eu.europa.ec.fisheries.schema.rules.customrule.v1.UpdateSubscriptionType;
import eu.europa.ec.fisheries.schema.rules.mobileterminal.v1.IdList;
import eu.europa.ec.fisheries.schema.rules.module.v1.GetTicketsAndRulesByMovementsResponse;
import eu.europa.ec.fisheries.schema.rules.movement.v1.RawMovementType;
import eu.europa.ec.fisheries.schema.rules.previous.v1.PreviousReportType;
import eu.europa.ec.fisheries.schema.rules.search.v1.*;
import eu.europa.ec.fisheries.schema.rules.search.v1.ListPagination;
import eu.europa.ec.fisheries.schema.rules.source.v1.GetAlarmListByQueryResponse;
import eu.europa.ec.fisheries.schema.rules.source.v1.GetTicketListByMovementsResponse;
import eu.europa.ec.fisheries.schema.rules.source.v1.GetTicketListByQueryResponse;
import eu.europa.ec.fisheries.schema.rules.ticket.v1.TicketStatusType;
import eu.europa.ec.fisheries.schema.rules.ticket.v1.TicketType;
import eu.europa.ec.fisheries.uvms.asset.model.exception.AssetModelMapperException;
import eu.europa.ec.fisheries.uvms.asset.model.mapper.AssetModuleRequestMapper;
import eu.europa.ec.fisheries.uvms.asset.model.mapper.AssetModuleResponseMapper;
import eu.europa.ec.fisheries.uvms.audit.model.exception.AuditModelMarshallException;
import eu.europa.ec.fisheries.uvms.audit.model.mapper.AuditLogMapper;
import eu.europa.ec.fisheries.uvms.config.service.ParameterService;
import eu.europa.ec.fisheries.uvms.exchange.model.exception.ExchangeModelMapperException;
import eu.europa.ec.fisheries.uvms.mobileterminal.model.exception.MobileTerminalModelMapperException;
import eu.europa.ec.fisheries.uvms.mobileterminal.model.exception.MobileTerminalUnmarshallException;
import eu.europa.ec.fisheries.uvms.mobileterminal.model.mapper.MobileTerminalModuleRequestMapper;
import eu.europa.ec.fisheries.uvms.mobileterminal.model.mapper.MobileTerminalModuleResponseMapper;
import eu.europa.ec.fisheries.uvms.movement.model.exception.ModelMarshallException;
import eu.europa.ec.fisheries.uvms.movement.model.mapper.MovementModuleRequestMapper;
import eu.europa.ec.fisheries.uvms.movement.model.mapper.MovementModuleResponseMapper;
import eu.europa.ec.fisheries.uvms.notifications.NotificationMessage;
import eu.europa.ec.fisheries.uvms.rules.message.constants.DataSourceQueue;
import eu.europa.ec.fisheries.uvms.rules.message.consumer.RulesResponseConsumer;
import eu.europa.ec.fisheries.uvms.rules.message.exception.MessageException;
import eu.europa.ec.fisheries.uvms.rules.message.producer.RulesMessageProducer;
import eu.europa.ec.fisheries.uvms.rules.model.constant.AuditObjectTypeEnum;
import eu.europa.ec.fisheries.uvms.rules.model.constant.AuditOperationEnum;
import eu.europa.ec.fisheries.uvms.rules.model.exception.RulesFaultException;
import eu.europa.ec.fisheries.uvms.rules.model.exception.RulesModelMapperException;
import eu.europa.ec.fisheries.uvms.rules.model.exception.RulesModelMarshallException;
import eu.europa.ec.fisheries.uvms.rules.model.mapper.JAXBMarshaller;
import eu.europa.ec.fisheries.uvms.rules.model.mapper.RulesDataSourceRequestMapper;
import eu.europa.ec.fisheries.uvms.rules.model.mapper.RulesDataSourceResponseMapper;
import eu.europa.ec.fisheries.uvms.rules.model.mapper.RulesModuleResponseMapper;
import eu.europa.ec.fisheries.uvms.rules.service.RulesService;
import eu.europa.ec.fisheries.uvms.rules.service.ValidationService;
import eu.europa.ec.fisheries.uvms.rules.service.business.*;
import eu.europa.ec.fisheries.uvms.rules.service.event.AlarmReportCountEvent;
import eu.europa.ec.fisheries.uvms.rules.service.event.AlarmReportEvent;
import eu.europa.ec.fisheries.uvms.rules.service.event.TicketCountEvent;
import eu.europa.ec.fisheries.uvms.rules.service.event.TicketEvent;
import eu.europa.ec.fisheries.uvms.rules.service.exception.InputArgumentException;
import eu.europa.ec.fisheries.uvms.rules.service.exception.RulesServiceException;
import eu.europa.ec.fisheries.uvms.rules.service.mapper.ExchangeMovementMapper;
import eu.europa.ec.fisheries.uvms.rules.service.mapper.MovementFactMapper;
import eu.europa.ec.fisheries.uvms.rules.service.mapper.RawMovementFactMapper;
import eu.europa.ec.fisheries.uvms.rules.service.mapper.RulesDozerMapper;
import eu.europa.ec.fisheries.uvms.user.model.mapper.UserModuleRequestMapper;
import eu.europa.ec.fisheries.wsdl.asset.group.AssetGroup;
import eu.europa.ec.fisheries.wsdl.asset.types.*;
import eu.europa.ec.fisheries.wsdl.user.module.GetContactDetailResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.jms.JMSException;
import javax.jms.TextMessage;
import javax.xml.datatype.XMLGregorianCalendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

@Stateless
public class RulesServiceBean implements RulesService {

    private final static Logger LOG = LoggerFactory.getLogger(RulesServiceBean.class);

    @EJB
    ParameterService parameterService;

    @EJB
    RulesResponseConsumer consumer;

    @EJB
    RulesMessageProducer producer;

    @Inject
    @AlarmReportEvent
    private Event<NotificationMessage> alarmReportEvent;

    @Inject
    @TicketEvent
    private Event<NotificationMessage> ticketEvent;

    @Inject
    @AlarmReportCountEvent
    private Event<NotificationMessage> alarmReportCountEvent;

    @Inject
    @TicketCountEvent
    private Event<NotificationMessage> ticketCountEvent;

    @EJB
    RulesValidator rulesValidator;

    @EJB
    ValidationService validationService;

    private String getOrganisationName(String userName) throws eu.europa.ec.fisheries.uvms.user.model.exception.ModelMarshallException, MessageException, RulesModelMarshallException {
        String userRequest = UserModuleRequestMapper.mapToGetContactDetailsRequest(userName);
        String userMessageId = producer.sendDataSourceMessage(userRequest, DataSourceQueue.USER);
        TextMessage userMessage = consumer.getMessage(userMessageId, TextMessage.class);
        GetContactDetailResponse userResponse = JAXBMarshaller.unmarshallTextMessage(userMessage, GetContactDetailResponse.class);

        if (userResponse != null && userResponse.getContactDetails() != null) {
            return userResponse.getContactDetails().getOrganisationName();
        } else {
            return null;
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param customRule
     * @throws RulesServiceException
     * @throws RulesFaultException
     */
    @Override
    public CustomRuleType createCustomRule(CustomRuleType customRule, String username) throws RulesServiceException, RulesFaultException {
        LOG.info("Create invoked in service layer");
        try {
            // Get organisation of user
            String organisationName = getOrganisationName(customRule.getUpdatedBy());
            if (organisationName != null) {
                customRule.setOrganisation(organisationName);
            } else {
                LOG.warn("User {} is not connected to any organisation!", customRule.getUpdatedBy());
            }

            String request = RulesDataSourceRequestMapper.mapCreateCustomRule(customRule, username);
            String messageId = producer.sendDataSourceMessage(request, DataSourceQueue.INTERNAL);
            TextMessage response = consumer.getMessage(messageId, TextMessage.class);

            // TODO: Rewrite so rules are loaded when changed
//            rulesValidator.reloadRules();

            CustomRuleType customRuleType = RulesDataSourceResponseMapper.mapToCreateCustomRuleFromResponse(response, messageId);
            sendAuditMessage(AuditObjectTypeEnum.CUSTOM_RULE, AuditOperationEnum.CREATE, customRuleType.getGuid(), null, username);
            return customRuleType;

        } catch (RulesModelMapperException | JMSException | MessageException e) {
            throw new RulesServiceException(e.getMessage());
        } catch (eu.europa.ec.fisheries.uvms.user.model.exception.ModelMarshallException e) {
            throw new RulesServiceException(e.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param guid
     * @return
     * @throws RulesServiceException
     */
    @Override
    public CustomRuleType getCustomRuleByGuid(String guid) throws RulesServiceException, RulesModelMapperException, RulesFaultException {
        LOG.info("Get Custom Rule by guid invoked in service layer");
        try {
            String request = RulesDataSourceRequestMapper.mapGetCustomRule(guid);
            String messageId = producer.sendDataSourceMessage(request, DataSourceQueue.INTERNAL);
            TextMessage response = consumer.getMessage(messageId, TextMessage.class);

            return RulesDataSourceResponseMapper.getCustomRuleResponse(response, messageId);
        } catch (MessageException ex) {
            throw new RulesServiceException(ex.getMessage());
        } catch (JMSException e) {
            throw new RulesServiceException(e.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param oldCustomRule
     * @throws RulesServiceException
     */
    @Override
    public CustomRuleType updateCustomRule(CustomRuleType oldCustomRule, String username) throws RulesServiceException, RulesFaultException {
        LOG.info("Update custom rule invoked in service layer");
        try {
            // Get organisation of user
            String organisationName = getOrganisationName(oldCustomRule.getUpdatedBy());
            if (organisationName != null) {
                oldCustomRule.setOrganisation(organisationName);
            } else {
                LOG.warn("User {} is not connected to any organisation!", oldCustomRule.getUpdatedBy());
            }

            String request = RulesDataSourceRequestMapper.mapUpdateCustomRule(oldCustomRule, username);
            String messageId = producer.sendDataSourceMessage(request, DataSourceQueue.INTERNAL);
            TextMessage response = consumer.getMessage(messageId, TextMessage.class);

            // TODO: Rewrite so rules are loaded when changed
//            rulesValidator.reloadRules();

            CustomRuleType newCustomRule = RulesDataSourceResponseMapper.mapToUpdateCustomRuleFromResponse(response, messageId);
            sendAuditMessage(AuditObjectTypeEnum.CUSTOM_RULE, AuditOperationEnum.DELETE, oldCustomRule.getGuid(), null, username);
            sendAuditMessage(AuditObjectTypeEnum.CUSTOM_RULE, AuditOperationEnum.CREATE, newCustomRule.getGuid(), null, username);
            return newCustomRule;
        } catch (RulesModelMapperException | MessageException | JMSException | eu.europa.ec.fisheries.uvms.user.model.exception.ModelMarshallException e) {
            throw new RulesServiceException(e.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param updateSubscriptionType
     */
    @Override
    public CustomRuleType updateSubscription(UpdateSubscriptionType updateSubscriptionType, String username) throws RulesServiceException, RulesFaultException {
        LOG.info("Update subscription invoked in service layer");
        try {
            boolean validRequest = updateSubscriptionType.getSubscription().getType() != null && updateSubscriptionType.getSubscription().getOwner() != null;
            if (!validRequest) {
                throw new RulesServiceException("Not a valid subscription!");
            }

            String request = RulesDataSourceRequestMapper.mapUpdateSubscription(updateSubscriptionType, username);
            String messageId = producer.sendDataSourceMessage(request, DataSourceQueue.INTERNAL);
            TextMessage response = consumer.getMessage(messageId, TextMessage.class);

            if (SubscritionOperationType.ADD.equals(updateSubscriptionType.getOperation())) {
                // TODO: Don't log rule guid, log subscription guid?
                sendAuditMessage(AuditObjectTypeEnum.CUSTOM_RULE_SUBSCRIPTION, AuditOperationEnum.CREATE, updateSubscriptionType.getRuleGuid(), updateSubscriptionType.getSubscription().getOwner() + "/" + updateSubscriptionType.getSubscription().getType(), username);
            } else if (SubscritionOperationType.REMOVE.equals(updateSubscriptionType.getOperation())) {
                // TODO: Don't log rule guid, log subscription guid?
                sendAuditMessage(AuditObjectTypeEnum.CUSTOM_RULE_SUBSCRIPTION, AuditOperationEnum.DELETE, updateSubscriptionType.getRuleGuid(), updateSubscriptionType.getSubscription().getOwner() + "/" + updateSubscriptionType.getSubscription().getType(), username);
            }

            return RulesDataSourceResponseMapper.mapToUpdateCustomRuleFromResponse(response, messageId);
        } catch (RulesModelMapperException | MessageException | JMSException e) {
            throw new RulesServiceException(e.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param guid
     * @throws RulesServiceException
     */
    @Override
    public CustomRuleType deleteCustomRule(String guid, String username) throws RulesServiceException, RulesFaultException {
        LOG.info("Deleting custom rule by guid: {}.", guid);
        if (guid == null) {
            throw new InputArgumentException("No custom rule to remove");
        }

        try {
            String request = RulesDataSourceRequestMapper.mapDeleteCustomRule(guid, username);
            String messageId = producer.sendDataSourceMessage(request, DataSourceQueue.INTERNAL);
            TextMessage response = consumer.getMessage(messageId, TextMessage.class);

            // TODO: Rewrite so rules are loaded when changed
//            rulesValidator.reloadRules();

            CustomRuleType customRuleType = RulesDataSourceResponseMapper.mapToDeleteCustomRuleFromResponse(response, messageId);
            sendAuditMessage(AuditObjectTypeEnum.CUSTOM_RULE, AuditOperationEnum.DELETE, customRuleType.getGuid(), null, username);
            return customRuleType;
        } catch (RulesModelMapperException | MessageException | JMSException e) {
            throw new RulesServiceException(e.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return
     * @throws RulesServiceException
     */
    @Override
    public GetAlarmListByQueryResponse getAlarmList(AlarmQuery query) throws RulesServiceException, RulesFaultException {
        LOG.info("Get alarm list invoked in service layer");
        try {
            String request = RulesDataSourceRequestMapper.mapAlarmList(query);
            String messageId = producer.sendDataSourceMessage(request, DataSourceQueue.INTERNAL);
            TextMessage response = consumer.getMessage(messageId, TextMessage.class);

            if (response == null) {
                LOG.error("[ Error when getting list, response from JMS Queue is null ]");
                throw new RulesServiceException("[ Error when getting list, response from JMS Queue is null ]");
            }

            return RulesDataSourceResponseMapper.mapToAlarmListFromResponse(response, messageId);
        } catch (RulesModelMapperException | MessageException | JMSException ex) {
            throw new RulesServiceException(ex.getMessage());
        }
    }

    @Override
    public GetTicketListByQueryResponse getTicketList(String loggedInUser, TicketQuery query) throws RulesServiceException, RulesFaultException {
        LOG.info("Get ticket list invoked in service layer");
        try {
            String request = RulesDataSourceRequestMapper.mapTicketList(loggedInUser, query);
            String messageId = producer.sendDataSourceMessage(request, DataSourceQueue.INTERNAL);
            TextMessage response = consumer.getMessage(messageId, TextMessage.class);

            if (response == null) {
                LOG.error("[ Error when getting list, response from JMS Queue is null ]");
                throw new RulesServiceException("[ Error when getting list, response from JMS Queue is null ]");
            }

            return RulesDataSourceResponseMapper.mapToTicketListFromResponse(response, messageId);
        } catch (RulesModelMapperException | MessageException | JMSException ex) {
            throw new RulesServiceException(ex.getMessage());
        }
    }

    @Override
    public GetTicketListByMovementsResponse getTicketsByMovements(List<String> movements) throws RulesServiceException, RulesFaultException {
        LOG.info("Get tickets by movements invoked in service layer");
        try {
            String request = RulesDataSourceRequestMapper.mapTicketsByMovements(movements);
            String messageId = producer.sendDataSourceMessage(request, DataSourceQueue.INTERNAL);
            TextMessage response = consumer.getMessage(messageId, TextMessage.class);

            if (response == null) {
                LOG.error("[ Error when getting tickets by movements, response from JMS Queue is null ]");
                throw new RulesServiceException("[ Error when getting tickets by movements, response from JMS Queue is null ]");
            }

            return RulesDataSourceResponseMapper.mapToTicketsByMovementsFromResponse(response, messageId);
        } catch (RulesModelMapperException | MessageException | JMSException ex) {
            throw new RulesServiceException(ex.getMessage());
        }
    }

    @Override
    public GetTicketsAndRulesByMovementsResponse getTicketsAndRulesByMovements(List<String> movements) throws RulesServiceException {
        LOG.info("Get tickets and rules by movements invoked in service layer");

        try {
            String request = RulesDataSourceRequestMapper.mapTicketsAndRulesByMovements(movements);

            String messageId = producer.sendDataSourceMessage(request, DataSourceQueue.INTERNAL);
            TextMessage response = consumer.getMessage(messageId, TextMessage.class);

            if (response == null) {
                LOG.error("[ Error when getting tickets and rules by movements, response from JMS Queue is null ]");
                throw new RulesServiceException("[ Error when getting tickets and rules by movements, response from JMS Queue is null ]");
            }

            return RulesModuleResponseMapper.mapToGetTicketsAndRulesByMovementsFromResponse(response);
        } catch (RulesModelMapperException | MessageException ex) {
            throw new RulesServiceException(ex.getMessage());
        }
    }

    @Override
    public long countTicketsByMovements(List<String> movements) throws RulesServiceException, RulesFaultException {
        LOG.info("Get number of tickets by movements invoked in service layer");
        try {
            String request = RulesDataSourceRequestMapper.mapCountTicketsByMovements(movements);
            String messageId = producer.sendDataSourceMessage(request, DataSourceQueue.INTERNAL);
            TextMessage response = consumer.getMessage(messageId, TextMessage.class);

            return RulesDataSourceResponseMapper.mapToCountTicketsByMovementsFromResponse(response, messageId);
        } catch (RulesModelMapperException | MessageException | JMSException ex) {
            throw new RulesServiceException(ex.getMessage());
        }
    }

    @Override
    public TicketType updateTicketStatus(TicketType ticket, String username) throws RulesServiceException, RulesFaultException {
        LOG.info("Update ticket status invoked in service layer");
        try {
            String request = RulesDataSourceRequestMapper.mapUpdateTicketStatus(ticket, username);
            String messageId = producer.sendDataSourceMessage(request, DataSourceQueue.INTERNAL);
            TextMessage response = consumer.getMessage(messageId, TextMessage.class);
            TicketType updatedTicket = RulesDataSourceResponseMapper.mapToSetTicketStatusFromResponse(response, messageId);

            // Notify long-polling clients of the update
            ticketEvent.fire(new NotificationMessage("guid", updatedTicket.getGuid()));

            // Notify long-polling clients of the change (no value since FE will need to fetch it)
            ticketCountEvent.fire(new NotificationMessage("ticketCount", null));

            sendAuditMessage(AuditObjectTypeEnum.TICKET, AuditOperationEnum.UPDATE, updatedTicket.getGuid(), null, username);

            return updatedTicket;

        } catch (RulesModelMapperException | MessageException | JMSException e) {
            throw new RulesServiceException(e.getMessage());
        }
    }

    @Override
    public List<TicketType> updateTicketStatusByQuery(String loggedInUser, TicketQuery query, TicketStatusType status) throws RulesServiceException, RulesFaultException {
        LOG.info("Update all ticket status invoked in service layer");
        try {
            String request = RulesDataSourceRequestMapper.mapUpdateTicketStatusByQuery(loggedInUser, query, status);
            String messageId = producer.sendDataSourceMessage(request, DataSourceQueue.INTERNAL);
            TextMessage response = consumer.getMessage(messageId, TextMessage.class);
            List<TicketType> updatedTickets = RulesDataSourceResponseMapper.mapToUpdateTicketStatusByQueryFromResponse(response, messageId);

            // Notify long-polling clients of the update
            for (TicketType updatedTicket : updatedTickets) {
                ticketEvent.fire(new NotificationMessage("guid", updatedTicket.getGuid()));
                sendAuditMessage(AuditObjectTypeEnum.TICKET, AuditOperationEnum.UPDATE, updatedTicket.getGuid(), null, loggedInUser);
            }

            // Notify long-polling clients of the change (no value since FE will need to fetch it)
            ticketCountEvent.fire(new NotificationMessage("ticketCount", null));

            return updatedTickets;

        } catch (RulesModelMapperException | MessageException | JMSException e) {
            throw new RulesServiceException(e.getMessage());
        }
    }

    @Override
    public long getNumberOfAssetsNotSending() throws RulesServiceException, RulesFaultException {
        try {
            String request = RulesDataSourceRequestMapper.getNumberOfAssetsNotSending();
            String messageId = producer.sendDataSourceMessage(request, DataSourceQueue.INTERNAL);
            TextMessage response = consumer.getMessage(messageId, TextMessage.class);

            return RulesDataSourceResponseMapper.mapToGetNumberOfAssetsNotSendingFromResponse(response, messageId);
        } catch (MessageException | JMSException | RulesModelMapperException e) {
            LOG.error("[ Error when getting number of open tickets ] {}", e.getMessage());
            throw new RulesServiceException("[ Error when getting number of open alarms. ]");
        }
    }

    @Override
    public AlarmReportType updateAlarmStatus(AlarmReportType alarm, String username) throws RulesServiceException, RulesFaultException {
        LOG.info("Update alarm status invoked in service layer");
        try {
            String request = RulesDataSourceRequestMapper.mapUpdateAlarmStatus(alarm, username);
            String messageId = producer.sendDataSourceMessage(request, DataSourceQueue.INTERNAL);
            TextMessage response = consumer.getMessage(messageId, TextMessage.class);

            AlarmReportType updatedAlarm = RulesDataSourceResponseMapper.mapToSetAlarmStatusFromResponse(response, messageId);

            // Notify long-polling clients of the change
            alarmReportEvent.fire(new NotificationMessage("guid", updatedAlarm.getGuid()));

            // Notify long-polling clients of the change (no vlaue since FE will need to fetch it)
            alarmReportCountEvent.fire(new NotificationMessage("alarmCount", null));

            sendAuditMessage(AuditObjectTypeEnum.ALARM, AuditOperationEnum.UPDATE, updatedAlarm.getGuid(), null, username);

            return updatedAlarm;
        } catch (RulesModelMapperException | MessageException | JMSException e) {
            throw new RulesServiceException(e.getMessage());
        }
    }

    // Triggered by RulesTimerBean
    @Override
    public List<PreviousReportType> getPreviousMovementReports() throws RulesServiceException, RulesFaultException {
        LOG.info("Get previous movement reports invoked in service layer");
        try {
            String request = RulesDataSourceRequestMapper.mapGetPreviousReports();
            String messageId = producer.sendDataSourceMessage(request, DataSourceQueue.INTERNAL);
            TextMessage response = consumer.getMessage(messageId, TextMessage.class);

            return RulesDataSourceResponseMapper.mapToGetPreviousReportsResponse(response, messageId);
        } catch (RulesModelMapperException | MessageException | JMSException e) {
            throw new RulesServiceException(e.getMessage());
        }
    }

    // Triggered by timer rule
    @Override
    public void timerRuleTriggered(String ruleName, PreviousReportFact fact) throws RulesServiceException, RulesFaultException {
        LOG.info("Timer rule triggered invoked in service layer");
        try {
            // Check if ticket already is created for this asset
            String getTicketRequest = RulesDataSourceRequestMapper.mapGetTicketByAssetAndRule(fact.getAssetGuid(), ruleName);
            String messageIdTicket = producer.sendDataSourceMessage(getTicketRequest, DataSourceQueue.INTERNAL);
            TextMessage ticketResponse = consumer.getMessage(messageIdTicket, TextMessage.class);
            boolean noTicketCreated = RulesDataSourceResponseMapper.mapToGetTicketByAssetGuidFromResponse(ticketResponse, messageIdTicket).getTicket() == null;

            if (noTicketCreated) {
                createAssetNotSendingTicket(ruleName, fact, "Triggered by rule: " +ruleName);
            }
        } catch (RulesModelMapperException | MessageException | JMSException e) {
            throw new RulesServiceException(e.getMessage());
        }
    }

    private void createAssetNotSendingTicket(String ruleName, PreviousReportFact fact, String username) throws RulesModelMapperException, MessageException, RulesFaultException, RulesServiceException, JMSException {
        TicketType ticket = new TicketType();

        ticket.setAssetGuid(fact.getAssetGuid());
        ticket.setOpenDate(RulesUtil.dateToString(new Date()));
        ticket.setRuleName(ruleName);
        ticket.setRuleGuid(ruleName);
        ticket.setUpdatedBy(username);
        ticket.setStatus(TicketStatusType.OPEN);
        ticket.setMovementGuid(fact.getMovementGuid());
        ticket.setGuid(UUID.randomUUID().toString());

        String createTicketRequest = RulesDataSourceRequestMapper.mapCreateTicket(ticket, username);
        String ticketMessageId = producer.sendDataSourceMessage(createTicketRequest, DataSourceQueue.INTERNAL);
        TextMessage ticketResponse = consumer.getMessage(ticketMessageId, TextMessage.class);

        TicketType createdTicket = RulesDataSourceResponseMapper.mapSingleTicketFromResponse(ticketResponse, ticketMessageId);

        sendAuditMessage(AuditObjectTypeEnum.TICKET, AuditOperationEnum.CREATE, createdTicket.getGuid(), null, username);

        // Notify long-polling clients of the change
        ticketCountEvent.fire(new NotificationMessage("ticketCount", null));
    }

    @Override
    public AlarmReportType getAlarmReportByGuid(String guid) throws RulesServiceException, RulesFaultException {
        try {
            String getAlarmReportRequest = RulesDataSourceRequestMapper.mapGetAlarmByGuid(guid);
            String messageId = producer.sendDataSourceMessage(getAlarmReportRequest, DataSourceQueue.INTERNAL);
            TextMessage response = consumer.getMessage(messageId, TextMessage.class);

            return RulesDataSourceResponseMapper.mapSingleAlarmFromResponse(response, messageId);
        } catch (MessageException | JMSException | RulesModelMapperException e) {
            LOG.error("[ Error when getting alarm by GUID ] {}", e.getMessage());
            throw new RulesServiceException("[ Error when getting alarm by GUID. ]");
        }
    }

    @Override
    public TicketType getTicketByGuid(String guid) throws RulesServiceException, RulesFaultException {
        try {
            String getTicketReportRequest = RulesDataSourceRequestMapper.mapGetTicketByGuid(guid);
            String messageId = producer.sendDataSourceMessage(getTicketReportRequest, DataSourceQueue.INTERNAL);
            TextMessage response = consumer.getMessage(messageId, TextMessage.class);

            return RulesDataSourceResponseMapper.mapSingleTicketFromResponse(response, messageId);
        } catch (MessageException | JMSException | RulesModelMapperException e) {
            LOG.error("[ Error when getting ticket by GUID ] {}", e.getMessage());
            throw new RulesServiceException("[ Error when getting ticket by GUID. ]");
        }
    }

    @Override
    public String reprocessAlarm(List<String> alarmGuids, String username) throws RulesServiceException, RulesFaultException {
        LOG.info("Reprocess alarms invoked in service layer");
        try {
            AlarmQuery query = new AlarmQuery();
            ListPagination pagination = new ListPagination();
            pagination.setListSize(alarmGuids.size());
            pagination.setPage(1);
            query.setPagination(pagination);

            for (String alarmGuid : alarmGuids) {
                AlarmListCriteria criteria = new AlarmListCriteria();
                criteria.setKey(AlarmSearchKey.ALARM_GUID);
                criteria.setValue(alarmGuid);

                query.getAlarmSearchCriteria().add(criteria);
            }

            // We only want open alarms
            AlarmListCriteria openCrit = new AlarmListCriteria();
            openCrit.setKey(AlarmSearchKey.STATUS);
            openCrit.setValue(AlarmStatusType.OPEN.name());
            query.getAlarmSearchCriteria().add(openCrit);

            query.setDynamic(true);

            String request = RulesDataSourceRequestMapper.mapAlarmList(query);
            String messageId = producer.sendDataSourceMessage(request, DataSourceQueue.INTERNAL);
            TextMessage response = consumer.getMessage(messageId, TextMessage.class);

            if (response == null) {
                LOG.error("[ Error when getting alarm list, response from JMS Queue is null ]");
                throw new RulesServiceException("[ Error when getting list, response from JMS Queue is null ]");
            }

            List<AlarmReportType> alarms = RulesDataSourceResponseMapper.mapToAlarmListFromResponse(response, messageId).getAlarms();

            for (AlarmReportType alarm : alarms) {
                // Cannot reprocess without a movement (i.e. "Asset not sending" alarm)
                if (alarm.getRawMovement() == null) {
                    continue;
                }

                // Mark the alarm as REPROCESSED before reprocessing. That will create a new alarm (if still wrong) with the items remaining.
                alarm.setStatus(AlarmStatusType.REPROCESSED);
                alarm = updateAlarmStatus(alarm, username);

                sendAuditMessage(AuditObjectTypeEnum.ALARM, AuditOperationEnum.UPDATE, alarm.getGuid(), null, username);

                RawMovementType rawMovementType = alarm.getRawMovement();

                // TODO: Use better type (some variation of PluginType...)
                String pluginType = alarm.getPluginType();
                //NHI
                setMovementReportReceived(rawMovementType, pluginType, username);
            }

//            return RulesDataSourceResponseMapper.mapToAlarmListFromResponse(response, messageId);
            // TODO: Better
            return "OK";

        } catch (RulesModelMapperException | MessageException | JMSException e) {
            throw new RulesServiceException(e.getMessage());
        }
    }

    @Override
    public void setMovementReportReceived(final RawMovementType rawMovement, String pluginType, String username) throws RulesServiceException {
        try {
            Date auditTimestamp = new Date();
            Date auditTotalTimestamp = new Date();

            Asset asset = null;

            // Get Mobile Terminal if it exists
            MobileTerminalType mobileTerminal = getMobileTerminalByRawMovement(rawMovement);
            auditTimestamp = auditLog("Time to fetch from Mobile Terminal Module:", auditTimestamp);

            // Get Asset
            if (mobileTerminal != null) {
                String connectId = mobileTerminal.getConnectId();
                if (connectId != null) {
                    asset = getAssetByConnectId(connectId);
                }
            } else {
                asset = getAssetByCfrIrcs(rawMovement.getAssetId());
            }
            auditTimestamp = auditLog("Time to fetch from Asset Module:", auditTimestamp);

            RawMovementFact rawMovementFact = RawMovementFactMapper.mapRawMovementFact(rawMovement, mobileTerminal, asset, pluginType);
            LOG.debug("rawMovementFact:{}", rawMovementFact);

            rulesValidator.evaluate(rawMovementFact);
            auditTimestamp = auditLog("Time to validate sanity:", auditTimestamp);

            if (rawMovementFact.isOk()) {
                MovementFact movementFact = collectMovementData(mobileTerminal, asset, rawMovement, username);

                LOG.info("Validating movement from Movement Module");
                rulesValidator.evaluate(movementFact);

                auditLog("Rules total time:", auditTotalTimestamp);

                // Tell Exchange that a movement was persisted in Movement
                sendBackToExchange(movementFact.getMovementGuid(), rawMovement, MovementRefTypeType.MOVEMENT, username);
            } else {
                // Tell Exchange that the report caused an alarm
                sendBackToExchange(null, rawMovement, MovementRefTypeType.ALARM, username);
            }
        } catch (MessageException | MobileTerminalModelMapperException | MobileTerminalUnmarshallException | JMSException | AssetModelMapperException | RulesModelMapperException | InterruptedException | ExecutionException e) {
            throw new RulesServiceException(e.getMessage());
        }
    }

    private MovementFact collectMovementData(MobileTerminalType mobileTerminal, Asset asset, final RawMovementType rawMovement, final String username) throws MessageException, RulesModelMapperException, ExecutionException, InterruptedException {
        int threadNum = 4;
        ExecutorService executor = Executors.newFixedThreadPool(threadNum);
        Integer numberOfReportsLast24Hours = null;
        final String assetGuid = asset.getAssetId().getGuid();

        final XMLGregorianCalendar positionTime = rawMovement.getPositionTime();

        FutureTask<Long> timeDiffAndPersistMovementTask = new FutureTask<>(new Callable<Long>() {
            @Override
            public Long call() {
                return timeDiffAndPersistMovement(assetGuid, positionTime, username);
            }
        });
        executor.execute(timeDiffAndPersistMovementTask);

        FutureTask<Integer> numberOfReportsLast24HoursTask = new FutureTask<>(new Callable<Integer>() {
            @Override
            public Integer call() {
                return numberOfReportsLast24Hours(assetGuid, positionTime);
            }
        });
        executor.execute(numberOfReportsLast24HoursTask);

        FutureTask<MovementType> sendToMovementTask = new FutureTask<>(new Callable<MovementType>() {
            @Override
            public MovementType call() {
                return sendToMovement(assetGuid, rawMovement, username);
            }
        });
        executor.execute(sendToMovementTask);

        FutureTask<List<AssetGroup>> assetGroupTask = new FutureTask<>(new Callable<List<AssetGroup>>() {
            @Override
            public List<AssetGroup> call() {
                return getAssetGroup(assetGuid);
            }
        });
        executor.execute(assetGroupTask);

        // Get channel guid
        String channelGuid = "";
        if (mobileTerminal != null) {
            channelGuid = getChannelGuid(mobileTerminal, rawMovement);
        }

        // Get channel type
        String comChannelType = null;
        if (rawMovement.getComChannelType() != null) {
            comChannelType = rawMovement.getComChannelType().name();
        }

        // TODO: Get vicinityOf to validate on
        // ??? Maybe Movement?
//        String vicinityOf = "GO_GET_IT!!!";

        // Get data from parallel tasks
        Date auditParallelTimestamp = new Date();
        Long timeDiffInSeconds = timeDiffAndPersistMovementTask.get();
        List<AssetGroup> assetGroups = assetGroupTask.get();
        numberOfReportsLast24Hours = numberOfReportsLast24HoursTask.get();
        MovementType createdMovement = sendToMovementTask.get();
        auditLog("Total time for parallel tasks:", auditParallelTimestamp);

        MovementFact movementFact = MovementFactMapper.mapMovementFact(createdMovement, mobileTerminal, asset, comChannelType, assetGroups, timeDiffInSeconds, numberOfReportsLast24Hours, channelGuid);
        LOG.debug("movementFact:{}", movementFact);

        return movementFact;
    }

    private Long timeDiffAndPersistMovement(String assetGuid, XMLGregorianCalendar positionTime, String username) {
        Date auditTimestamp = new Date();

        // This needs to be done before persisting last report
        Long timeDiffInSeconds = null;
        Long timeDiff = timeDiffFromLastCommunication(assetGuid, positionTime);
        timeDiffInSeconds = timeDiff != null ? timeDiff / 1000 : null;
        auditTimestamp = auditLog("Time to fetch time difference to previous report:", auditTimestamp);

        persistLastCommunication(assetGuid, positionTime, username);
        auditLog("Time to persist the position time:", auditTimestamp);

        return timeDiffInSeconds;
    }

    private Long timeDiffFromLastCommunication(String assetGuid, XMLGregorianCalendar thisTime) {
        LOG.info("Fetching time difference to previous movement report");

        Long timeDiff = null;
        try {
            String request = RulesDataSourceRequestMapper.mapGetPreviousReportByAssetGuid(assetGuid);
            String messageId = producer.sendDataSourceMessage(request, DataSourceQueue.INTERNAL);
            TextMessage response = consumer.getMessage(messageId, TextMessage.class);

            PreviousReportType previousReport = RulesDataSourceResponseMapper.mapToGetPreviousReportByAssetGuidResponse(response, messageId);

            XMLGregorianCalendar previousTime = previousReport.getPositionTime();

            timeDiff = thisTime.toGregorianCalendar().getTimeInMillis() - previousTime.toGregorianCalendar().getTimeInMillis();
        } catch (Exception e) {
            // If something goes wrong, continue with the other validation
            LOG.warn("[ Error when fetching time difference of previous movement reports ]");
        }
        return timeDiff;
    }

    private void persistLastCommunication(String assetGuid, XMLGregorianCalendar positionTime, String username) {
        PreviousReportType thisReport = new PreviousReportType();

        thisReport.setPositionTime(positionTime);
        thisReport.setAssetGuid(assetGuid);

        String upsertPreviousReportequest = null;
        try {
            upsertPreviousReportequest = RulesDataSourceRequestMapper.mapUpsertPreviousReport(thisReport, username);
            producer.sendDataSourceMessage(upsertPreviousReportequest, DataSourceQueue.INTERNAL);
        } catch (RulesModelMapperException | MessageException e) {
            LOG.error("[ Error persisting report. ] {}", e.getMessage());
            e.printStackTrace();
        }
    }

    private Integer numberOfReportsLast24Hours(String assetGuid, XMLGregorianCalendar thisTime) {
        LOG.info("Fetching number of reports last 24 hours");

        Date auditTimestamp = new Date();

        Integer numberOfMovements = null;
        MovementQuery query = new MovementQuery();

        // Range
        RangeCriteria dateRangeCriteria = new RangeCriteria();
        dateRangeCriteria.setKey(RangeKeyType.DATE);
        GregorianCalendar twentyFourHoursAgo = thisTime.toGregorianCalendar();
        twentyFourHoursAgo.add(GregorianCalendar.HOUR, -24);
        String from = RulesUtil.gregorianToString(twentyFourHoursAgo);
        dateRangeCriteria.setFrom(from);
        String to = RulesUtil.xmlGregorianToString(thisTime);
        dateRangeCriteria.setTo(to);
        query.getMovementRangeSearchCriteria().add(dateRangeCriteria);

        // Id
        eu.europa.ec.fisheries.schema.movement.search.v1.ListCriteria idCriteria = new eu.europa.ec.fisheries.schema.movement.search.v1.ListCriteria();
        idCriteria.setKey(eu.europa.ec.fisheries.schema.movement.search.v1.SearchKey.CONNECT_ID);
        idCriteria.setValue(assetGuid);
        query.getMovementSearchCriteria().add(idCriteria);

        try {
            String request = MovementModuleRequestMapper.mapToGetMovementMapByQueryRequest(query);
            String messageId = producer.sendDataSourceMessage(request, DataSourceQueue.MOVEMENT);
            TextMessage response = consumer.getMessage(messageId, TextMessage.class);

            List<MovementMapResponseType> result = MovementModuleResponseMapper.mapToMovementMapResponse(response);

            List<MovementType> movements;

            if (result == null || result.isEmpty()) {
                LOG.warn("[ Error when fetching sum of previous movement reports: No result found");
                return null;
            } else if (result.size() != 1) {
                LOG.warn("[ Error when fetching sum of previous movement reports: Duplicate assets found ({})", result.size());
                return null;
            } else if (!assetGuid.equals(result.get(0).getKey())) {
                LOG.warn("[ Error when fetching sum of previous movement reports: Wrong asset found ({})", result.get(0).getKey());
                return null;
            } else {
                movements = result.get(0).getMovements();
            }

            numberOfMovements = movements != null ? movements.size() : 0;
        } catch (Exception e) {
            // If something goes wrong, continue with the other validation
            LOG.warn("[ Error when fetching sum of previous movement reports:{} ]", e.getMessage());
        }

        auditLog("Time to fetch number of reports last 24 hours:", auditTimestamp);

        return numberOfMovements;
    }

    private MovementType sendToMovement(String assetGuid, RawMovementType rawMovement, String username) {
        LOG.info("Send the validated raw position to Movement");

        Date auditTimestamp = new Date();

        MovementType createdMovement = null;
        try {
            MovementBaseType movementBaseType = RulesDozerMapper.getInstance().getMapper().map(rawMovement, MovementBaseType.class);
            movementBaseType.setConnectId(assetGuid);
            String createMovementRequest = MovementModuleRequestMapper.mapToCreateMovementRequest(movementBaseType, username);
            String messageId = producer.sendDataSourceMessage(createMovementRequest, DataSourceQueue.MOVEMENT);
            TextMessage movementResponse = consumer.getMessage(messageId, TextMessage.class);

            createdMovement = RulesDozerMapper.mapCreateMovementToMovementType(movementResponse);
        } catch (ModelMarshallException | MessageException e) {
            LOG.error("[ Error when getting movement from Movement , movementResponse from JMS Queue is null ]");
        }

        auditLog("Time to get movement from Movement Module:", auditTimestamp);

        return createdMovement;
    }

    private List<AssetGroup> getAssetGroup(String assetGuid) {
        LOG.info("Fetch asset groups from Asset");

        Date auditTimestamp = new Date();

        TextMessage getAssetResponse = null;
        String getAssetMessageId = null;
        List<AssetGroup> assetGroups = null;
        try {
            String getAssetRequest = AssetModuleRequestMapper.createAssetGroupListByAssetGuidRequest(assetGuid);
            getAssetMessageId = producer.sendDataSourceMessage(getAssetRequest, DataSourceQueue.ASSET);
            getAssetResponse = consumer.getMessage(getAssetMessageId, TextMessage.class);

            assetGroups = AssetModuleResponseMapper.mapToAssetGroupListFromResponse(getAssetResponse, getAssetMessageId);
        } catch (AssetModelMapperException | MessageException e) {
            LOG.warn("[ Failed while fetching asset groups ]", e.getMessage());
        }

        auditLog("Time to get asset groups:", auditTimestamp);

        return assetGroups;
    }

    private void sendBackToExchange(String guid, RawMovementType rawMovement, MovementRefTypeType status, String username) throws RulesModelMarshallException, MessageException {
        LOG.info("Sending back processed movement to exchange");

        // Map response
        MovementRefType movementRef = new MovementRefType();
        movementRef.setMovementRefGuid(guid);
        movementRef.setType(status);
        movementRef.setAckResponseMessageID(rawMovement.getAckResponseMessageID());

        // Map movement
        SetReportMovementType setReportMovementType = ExchangeMovementMapper.mapExchangeMovement(rawMovement);

        try {
            String exchangeResponseText = ExchangeMovementMapper.mapToProcessedMovementResponse(setReportMovementType, movementRef, username);
//            String exchangeResponseText = eu.europa.ec.fisheries.uvms.exchange.model.mapper.JAXBMarshaller.marshallJaxBObjectToString(ExchangeModuleRequestMapper.mapToProcessedMovementResopnse(setReportMovementType, movementRef));
            producer.sendDataSourceMessage(exchangeResponseText, DataSourceQueue.EXCHANGE);
        } catch (ExchangeModelMapperException e) {
            e.printStackTrace();
        }
    }

    private Asset getAssetByConnectId(String connectId) throws AssetModelMapperException, MessageException {
        LOG.info("Fetch asset by connectId '{}'", connectId);

        AssetListQuery query = new AssetListQuery();
        AssetListCriteria criteria = new AssetListCriteria();
        AssetListCriteriaPair criteriaPair = new AssetListCriteriaPair();
        criteriaPair.setKey(ConfigSearchField.GUID);
        criteriaPair.setValue(connectId);
        criteria.getCriterias().add(criteriaPair);
        criteria.setIsDynamic(true);

        query.setAssetSearchCriteria(criteria);

        AssetListPagination pagination = new AssetListPagination();
        // To leave room to find erroneous results - it must be only one in the list
        pagination.setListSize(2);
        pagination.setPage(1);
        query.setPagination(pagination);

        String getAssetRequest = AssetModuleRequestMapper.createAssetListModuleRequest(query);
        String getAssetMessageId = producer.sendDataSourceMessage(getAssetRequest, DataSourceQueue.ASSET);
        TextMessage getAssetResponse = consumer.getMessage(getAssetMessageId, TextMessage.class);

        List<Asset> resultList = AssetModuleResponseMapper.mapToAssetListFromResponse(getAssetResponse, getAssetMessageId);

        return resultList.size() != 1 ? null : resultList.get(0);
    }

    private Asset getAssetByCfrIrcs(AssetId assetId) {
        LOG.info("Fetch asset by assetId");

        Asset asset = null;
        try {
            // If no asset information exists, don't look for one
            if (assetId == null || assetId.getAssetIdList() == null) {
                LOG.warn("No asset information exists!");
                return null;
            }

            List<AssetIdList> ids = assetId.getAssetIdList();

            String cfr = null;
            String ircs = null;

            // Get possible search parameters
            for (AssetIdList id : ids) {
                if (id.getIdType().equals(eu.europa.ec.fisheries.schema.rules.asset.v1.AssetIdType.CFR)) {
                    cfr = id.getValue();
                }
                if (id.getIdType().equals(eu.europa.ec.fisheries.schema.rules.asset.v1.AssetIdType.IRCS)) {
                    ircs = id.getValue();
                }
            }

            if (ircs != null && cfr != null) {
                asset = getAsset(AssetIdType.CFR, cfr);
                // If the asset matches on ircs as well we have a winner
                if (asset != null && asset.getIrcs().equals(ircs)) {
                    return asset;
                }
            } else if (cfr != null && ircs == null) {
                return getAsset(AssetIdType.CFR, cfr);
            } else if (cfr == null && ircs != null) {
                return getAsset(AssetIdType.IRCS, ircs);
            }

        } catch (Exception e) {
            // Log and continue validation
            LOG.warn("Could not find asset!");
        }
        return null;
    }

    private Asset getAsset(AssetIdType type, String value) throws AssetModelMapperException, MessageException {
        String getAssetListRequest = AssetModuleRequestMapper.createGetAssetModuleRequest(value, type);
        String getAssetMessageId = producer.sendDataSourceMessage(getAssetListRequest, DataSourceQueue.ASSET);
        TextMessage getAssetResponse = consumer.getMessage(getAssetMessageId, TextMessage.class);

        return AssetModuleResponseMapper.mapToAssetFromResponse(getAssetResponse, getAssetMessageId);
    }

    private MobileTerminalType getMobileTerminalByRawMovement(RawMovementType rawMovement) throws MessageException, MobileTerminalModelMapperException, MobileTerminalUnmarshallException, JMSException {
        LOG.info("Fetch mobile terminal");
        MobileTerminalListQuery query = new MobileTerminalListQuery();

        // If no mobile terminal information exists, don't look for one
        if (rawMovement.getMobileTerminal() == null || rawMovement.getMobileTerminal().getMobileTerminalIdList() == null) {
            return null;
        }

        List<IdList> ids = rawMovement.getMobileTerminal().getMobileTerminalIdList();

        MobileTerminalSearchCriteria criteria = new MobileTerminalSearchCriteria();
        for (IdList id : ids) {
            eu.europa.ec.fisheries.schema.mobileterminal.types.v1.ListCriteria crit = new eu.europa.ec.fisheries.schema.mobileterminal.types.v1.ListCriteria();
            switch (id.getType()) {
                case DNID:
                    if (id.getValue() != null) {
                        crit.setKey(eu.europa.ec.fisheries.schema.mobileterminal.types.v1.SearchKey.DNID);
                        crit.setValue(id.getValue());
                        criteria.getCriterias().add(crit);
                    }
                    break;
                case MEMBER_NUMBER:
                    if (id.getValue() != null) {
                        crit.setKey(eu.europa.ec.fisheries.schema.mobileterminal.types.v1.SearchKey.MEMBER_NUMBER);
                        crit.setValue(id.getValue());
                        criteria.getCriterias().add(crit);
                    }
                    break;
                case SERIAL_NUMBER:
                    if (id.getValue() != null) {
                        crit.setKey(eu.europa.ec.fisheries.schema.mobileterminal.types.v1.SearchKey.SERIAL_NUMBER);
                        crit.setValue(id.getValue());
                        criteria.getCriterias().add(crit);
                    }
                    break;
                case LES:
                default:
                    LOG.error("[ Unhandled Mobile Terminal id: {} ]", id.getType());
                    break;
            }
        }

        // If no valid criterias, don't look for a mobile terminal
        if (criteria.getCriterias().isEmpty()) {
            return null;
        }

        // If we know the transponder type from the source, use it in the search criteria
        eu.europa.ec.fisheries.schema.mobileterminal.types.v1.ListCriteria transponderTypeCrit = new eu.europa.ec.fisheries.schema.mobileterminal.types.v1.ListCriteria();
        switch (rawMovement.getSource()) {
            case INMARSAT_C:
                transponderTypeCrit.setKey(eu.europa.ec.fisheries.schema.mobileterminal.types.v1.SearchKey.TRANSPONDER_TYPE);
                transponderTypeCrit.setValue("INMARSAT_C");
                criteria.getCriterias().add(transponderTypeCrit);
                break;
            case IRIDIUM:
                transponderTypeCrit.setKey(eu.europa.ec.fisheries.schema.mobileterminal.types.v1.SearchKey.TRANSPONDER_TYPE);
                transponderTypeCrit.setValue("IRIDIUM");
                criteria.getCriterias().add(transponderTypeCrit);
                break;
        }

        query.setMobileTerminalSearchCriteria(criteria);
        eu.europa.ec.fisheries.schema.mobileterminal.types.v1.ListPagination pagination = new eu.europa.ec.fisheries.schema.mobileterminal.types.v1.ListPagination();
        // To leave room to find erroneous results - it must be only one in the list
        pagination.setListSize(2);
        pagination.setPage(1);
        query.setPagination(pagination);

        String getMobileTerminalListRequest = MobileTerminalModuleRequestMapper.createMobileTerminalListRequest(query);
        String getMobileTerminalMessageId = producer.sendDataSourceMessage(getMobileTerminalListRequest, DataSourceQueue.MOBILE_TERMINAL);
        TextMessage getMobileTerminalResponse = consumer.getMessage(getMobileTerminalMessageId, TextMessage.class);

        List<MobileTerminalType> resultList = MobileTerminalModuleResponseMapper.mapToMobileTerminalListResponse(getMobileTerminalResponse);

        MobileTerminalType mobileTerminal = resultList.size() != 1 ? null : resultList.get(0);

        return mobileTerminal;
    }

    // TODO: Implement for IRIDIUM as well (if needed)
    private String getChannelGuid(MobileTerminalType mobileTerminal, RawMovementType rawMovement) {
        String dnid = "";
        String memberNumber = "";
        String channelGuid = "";

        List<IdList> ids = rawMovement.getMobileTerminal().getMobileTerminalIdList();

        for (IdList id : ids) {
            switch (id.getType()) {
                case DNID:
                    if (id.getValue() != null) {
                        dnid = id.getValue();
                    }
                    break;
                case MEMBER_NUMBER:
                    if (id.getValue() != null) {
                        memberNumber = id.getValue();
                    }
                    break;
                case SERIAL_NUMBER:
                    // IRIDIUM
                case LES:
                default:
                    LOG.error("[ Unhandled Mobile Terminal id: {} ]", id.getType());
                    break;
            }
        }

        // Get the channel guid
        boolean correctDnid = false;
        boolean correctMemberNumber = false;
        List<ComChannelType> channels = mobileTerminal.getChannels();
        for (ComChannelType channel : channels) {

            List<ComChannelAttribute> attributes = channel.getAttributes();

            for (ComChannelAttribute attribute : attributes) {
                String type = attribute.getType();
                String value = attribute.getValue();

                if ("DNID".equals(type)) {
                    correctDnid = value.equals(dnid);
                }
                if ("MEMBER_NUMBER".equals(type)) {
                    correctMemberNumber = value.equals(memberNumber);
                }
            }

            if (correctDnid && correctMemberNumber) {
                channelGuid = channel.getGuid();
            }
        }

        return channelGuid;
    }

    private Date auditLog(String msg, Date lastTimestamp) {
        Date newTimestamp = new Date();
        long duration = newTimestamp.getTime() - lastTimestamp.getTime();
        LOG.info("--> AUDIT - {} {}ms", msg, duration);
        return newTimestamp;
    }

    private void sendAuditMessage(AuditObjectTypeEnum type, AuditOperationEnum operation, String affectedObject, String comment, String username) {
        try {
            String message = AuditLogMapper.mapToAuditLog(type.getValue(), operation.getValue(), affectedObject, username);
            producer.sendDataSourceMessage(message, DataSourceQueue.AUDIT);
        } catch (AuditModelMarshallException | MessageException e) {
            LOG.error("[ Error when sending message to Audit. ] {}", e.getMessage());
        }
    }

}
