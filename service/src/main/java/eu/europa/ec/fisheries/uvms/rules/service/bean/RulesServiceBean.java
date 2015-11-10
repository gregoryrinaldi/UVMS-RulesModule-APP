package eu.europa.ec.fisheries.uvms.rules.service.bean;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.jms.JMSException;
import javax.jms.TextMessage;

import eu.europa.ec.fisheries.schema.exchange.movement.v1.MovementComChannelType;
import eu.europa.ec.fisheries.schema.mobileterminal.types.v1.*;
import eu.europa.ec.fisheries.schema.movement.v1.MovementType;
import eu.europa.ec.fisheries.schema.rules.asset.v1.AssetId;
import eu.europa.ec.fisheries.schema.rules.asset.v1.AssetIdList;
import eu.europa.ec.fisheries.schema.rules.mobileterminal.v1.IdList;
import eu.europa.ec.fisheries.schema.rules.movement.v1.MovementRefType;
import eu.europa.ec.fisheries.schema.rules.movement.v1.RawMovementType;
import eu.europa.ec.fisheries.schema.rules.search.v1.*;
import eu.europa.ec.fisheries.schema.rules.search.v1.ListCriteria;
import eu.europa.ec.fisheries.schema.rules.search.v1.ListPagination;
import eu.europa.ec.fisheries.schema.rules.search.v1.SearchKey;
import eu.europa.ec.fisheries.uvms.mobileterminal.model.exception.MobileTerminalModelMapperException;
import eu.europa.ec.fisheries.uvms.mobileterminal.model.exception.MobileTerminalUnmarshallException;
import eu.europa.ec.fisheries.uvms.mobileterminal.model.mapper.MobileTerminalModuleRequestMapper;
import eu.europa.ec.fisheries.uvms.mobileterminal.model.mapper.MobileTerminalModuleResponseMapper;
import eu.europa.ec.fisheries.uvms.rules.model.exception.RulesFaultException;
import eu.europa.ec.fisheries.uvms.rules.model.exception.RulesModelMarshallException;
import eu.europa.ec.fisheries.uvms.rules.model.mapper.JAXBMarshaller;
import eu.europa.ec.fisheries.uvms.rules.service.business.*;
import eu.europa.ec.fisheries.uvms.vessel.model.exception.VesselModelMapperException;
import eu.europa.ec.fisheries.uvms.vessel.model.mapper.VesselModuleRequestMapper;
import eu.europa.ec.fisheries.uvms.vessel.model.mapper.VesselModuleResponseMapper;
import eu.europa.ec.fisheries.wsdl.vessel.types.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.europa.ec.fisheries.schema.movement.v1.MovementBaseType;
import eu.europa.ec.fisheries.schema.rules.alarm.v1.AlarmReportType;
import eu.europa.ec.fisheries.schema.rules.alarm.v1.AlarmStatusType;
import eu.europa.ec.fisheries.schema.rules.customrule.v1.CustomRuleType;
import eu.europa.ec.fisheries.schema.rules.previous.v1.PreviousReportType;
import eu.europa.ec.fisheries.schema.rules.source.v1.GetAlarmListByQueryResponse;
import eu.europa.ec.fisheries.schema.rules.source.v1.GetTicketListByQueryResponse;
import eu.europa.ec.fisheries.schema.rules.source.v1.SingleAlarmResponse;
import eu.europa.ec.fisheries.schema.rules.source.v1.SingleTicketResponse;
import eu.europa.ec.fisheries.schema.rules.ticket.v1.TicketStatusType;
import eu.europa.ec.fisheries.schema.rules.ticket.v1.TicketType;
import eu.europa.ec.fisheries.uvms.config.service.ParameterService;
import eu.europa.ec.fisheries.uvms.movement.model.exception.ModelMarshallException;
import eu.europa.ec.fisheries.uvms.movement.model.mapper.MovementModuleRequestMapper;
import eu.europa.ec.fisheries.uvms.notifications.NotificationMessage;
import eu.europa.ec.fisheries.uvms.rules.message.constants.DataSourceQueue;
import eu.europa.ec.fisheries.uvms.rules.message.consumer.RulesResponseConsumer;
import eu.europa.ec.fisheries.uvms.rules.message.exception.MessageException;
import eu.europa.ec.fisheries.uvms.rules.message.producer.RulesMessageProducer;
import eu.europa.ec.fisheries.uvms.rules.model.exception.RulesModelMapperException;
import eu.europa.ec.fisheries.uvms.rules.model.mapper.RulesDataSourceRequestMapper;
import eu.europa.ec.fisheries.uvms.rules.model.mapper.RulesDataSourceResponseMapper;
import eu.europa.ec.fisheries.uvms.rules.service.RulesService;
import eu.europa.ec.fisheries.uvms.rules.service.event.AlarmReportEvent;
import eu.europa.ec.fisheries.uvms.rules.service.event.TicketEvent;
import eu.europa.ec.fisheries.uvms.rules.service.exception.RulesServiceException;
import eu.europa.ec.fisheries.uvms.rules.service.mapper.RulesMapper;

@Stateless
public class RulesServiceBean implements RulesService {

    final static Logger LOG = LoggerFactory.getLogger(RulesServiceBean.class);
    public static final String REF_TYPE_MOVEMENT = "MOVEMENT";
    public static final String REF_TYPE_ALARM = "ALARM";

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

    @EJB
    RulesValidator rulesValidator;

    /**
     * {@inheritDoc}
     *
     * @param customRule
     * @throws RulesServiceException
     */
    @Override
    public CustomRuleType createCustomRule(CustomRuleType customRule) throws RulesServiceException {
        LOG.info("Create invoked in service layer");
        try {
            String request = RulesDataSourceRequestMapper.mapCreateCustomRule(customRule);
            String messageId = producer.sendDataSourceMessage(request, DataSourceQueue.INTERNAL);
            TextMessage response = consumer.getMessage(messageId, TextMessage.class);

//            rulesValidator.init();

            return RulesDataSourceResponseMapper.mapToCreateCustomRuleFromResponse(response);
        } catch (RulesModelMapperException | MessageException ex) {
            throw new RulesServiceException(ex.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return
     * @throws RulesServiceException
     */
    @Override
    public GetAlarmListByQueryResponse getAlarmList(AlarmQuery query) throws RulesServiceException {
        LOG.info("Get alarm list invoked in service layer");
        try {
            String request = RulesDataSourceRequestMapper.mapAlarmList(query);
            String messageId = producer.sendDataSourceMessage(request, DataSourceQueue.INTERNAL);
            TextMessage response = consumer.getMessage(messageId, TextMessage.class);

            if (response == null) {
                LOG.error("[ Error when getting list, response from JMS Queue is null ]");
                throw new RulesServiceException("[ Error when getting list, response from JMS Queue is null ]");
            }
            return RulesDataSourceResponseMapper.mapToAlarmListFromResponse(response);
        } catch (RulesModelMapperException | MessageException ex) {
            throw new RulesServiceException(ex.getMessage());
        }
    }

    @Override
    public GetTicketListByQueryResponse getTicketList(TicketQuery query) throws RulesServiceException {
        LOG.info("Get ticket list invoked in service layer");
        try {
            String request = RulesDataSourceRequestMapper.mapTicketList(query);
            String messageId = producer.sendDataSourceMessage(request, DataSourceQueue.INTERNAL);
            TextMessage response = consumer.getMessage(messageId, TextMessage.class);
            if (response == null) {
                LOG.error("[ Error when getting list, response from JMS Queue is null ]");
                throw new RulesServiceException("[ Error when getting list, response from JMS Queue is null ]");
            }
            return RulesDataSourceResponseMapper.mapToTicketListFromResponse(response);
        } catch (RulesModelMapperException | MessageException ex) {
            throw new RulesServiceException(ex.getMessage());
        }
    }

    @Override
    public TicketType updateTicketStatus(TicketType ticket) throws RulesServiceException {
        LOG.info("Update ticket status invoked in service layer");
        try {
            String request = RulesDataSourceRequestMapper.mapUpdateTicketStatus(ticket);
            String messageId = producer.sendDataSourceMessage(request, DataSourceQueue.INTERNAL);
            TextMessage response = consumer.getMessage(messageId, TextMessage.class);
            TicketType updatedTicket = RulesDataSourceResponseMapper.mapToSetTicketStatusFromResponse(response);

            // Notify long-polling clients of the update
            ticketEvent.fire(new NotificationMessage("guid", updatedTicket.getGuid()));

            return updatedTicket;

        } catch (RulesModelMapperException | MessageException ex) {
            throw new RulesServiceException(ex.getMessage());
        }
    }

    @Override
    public AlarmReportType updateAlarmStatus(AlarmReportType alarm) throws RulesServiceException {
        LOG.info("Update alarm status invoked in service layer");
        try {
            String request = RulesDataSourceRequestMapper.mapUpdateAlarmStatus(alarm);
            String messageId = producer.sendDataSourceMessage(request, DataSourceQueue.INTERNAL);
            TextMessage response = consumer.getMessage(messageId, TextMessage.class);

            AlarmReportType result = RulesDataSourceResponseMapper.mapToSetAlarmStatusFromResponse(response);

            // Notify long-polling clients of the change
            alarmReportEvent.fire(new NotificationMessage("guid", result.getGuid()));

            return result;
        } catch (RulesModelMapperException | MessageException e) {
            throw new RulesServiceException(e.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param id
     * @return
     * @throws RulesServiceException
     */
    @Override
    public CustomRuleType getById(Long id) throws RulesServiceException {
        LOG.info("Update invoked in service layer");
        throw new RulesServiceException("Update not implemented in service layer");
    }

    /**
     * {@inheritDoc}
     *
     * @param guid
     * @return
     * @throws RulesServiceException
     */
    @Override
    public CustomRuleType getByGuid(String guid) throws RulesServiceException, RulesModelMapperException, RulesFaultException {
        LOG.info("Get by id invoked in service layer");
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
     * @param customRule
     * @throws RulesServiceException
     */
    @Override
    public CustomRuleType updateCustomRule(CustomRuleType customRule) throws RulesServiceException {
        LOG.info("Update custom rule invoked in service layer");
        try {
            String request = RulesDataSourceRequestMapper.mapUpdateCustomRule(customRule);
            String messageId = producer.sendDataSourceMessage(request, DataSourceQueue.INTERNAL);
            TextMessage response = consumer.getMessage(messageId, TextMessage.class);
            return RulesDataSourceResponseMapper.mapToUpdateCustomRuleFromResponse(response);
        } catch (RulesModelMapperException | MessageException ex) {
            throw new RulesServiceException(ex.getMessage());
        }
    }

    // Triggered by RulesTimerBean
    @Override
    public List<PreviousReportType> getPreviousMovementReports() throws RulesServiceException {
        LOG.info("Get previous movement reports invoked in service layer");
        try {
            String request = RulesDataSourceRequestMapper.mapGetPreviousReport();
            String messageId = producer.sendDataSourceMessage(request, DataSourceQueue.INTERNAL);
            TextMessage response = consumer.getMessage(messageId, TextMessage.class);
            return RulesDataSourceResponseMapper.mapToGetPreviousReportResponse(response);
        } catch (RulesModelMapperException | MessageException ex) {
            throw new RulesServiceException(ex.getMessage());
        }
    }

    // Triggered by timer rule
    @Override
    public void timerRuleTriggered(String ruleName, String ruleGuid, PreviousReportFact fact) throws RulesServiceException {
        LOG.info("Timer rule triggered invoked in service layer");
        try {
            // Check if ticket already is created for this vessel
            String getTicketRequest = RulesDataSourceRequestMapper.mapGetTicketByVesselGuid(fact.getVesselGuid());
            String messageId = producer.sendDataSourceMessage(getTicketRequest, DataSourceQueue.INTERNAL);
            TextMessage response = consumer.getMessage(messageId, TextMessage.class);

            boolean noTicketCreated = RulesDataSourceResponseMapper.mapToGetTicketByVesselGuidFromResponse(response).getTicket() == null;

            if (noTicketCreated) {
                TicketType ticket = new TicketType();

                ticket.setVesselGuid(fact.getVesselGuid());
                ticket.setOpenDate(RulesUtil.dateToString(new Date()));
                ticket.setRuleName(ruleName);
                ticket.setRuleGuid(ruleGuid);
                ticket.setUpdatedBy("UVMS");
                ticket.setStatus(TicketStatusType.OPEN);
                ticket.setMovementGuid(fact.getMovementGuid());
                ticket.setGuid(UUID.randomUUID().toString());

                String createTicketRequest = RulesDataSourceRequestMapper.mapCreateTicket(ticket);
                producer.sendDataSourceMessage(createTicketRequest, DataSourceQueue.INTERNAL);
                TextMessage ticketResponse = consumer.getMessage(messageId, TextMessage.class);

                // TODO: Do something with the response???
            }
        } catch (RulesModelMapperException | MessageException ex) {
            throw new RulesServiceException(ex.getMessage());
        }
    }

    @Override
    public String reprocessAlarm(List<String> alarmGuids) throws RulesServiceException {
        LOG.info("Reprocess alarms invoked in service layer");
        try {
            AlarmQuery query = new AlarmQuery();
            ListPagination pagination = new ListPagination();
            pagination.setListSize(alarmGuids.size());
            pagination.setPage(1);
            query.setPagination(pagination);

            for (String alarmGuid : alarmGuids) {
                ListCriteria criteria = new ListCriteria();
                criteria.setKey(SearchKey.ALARM_GUID);
                criteria.setValue(alarmGuid);

                query.getAlarmSearchCriteria().add(criteria);
            }

            // We only want open alarms
            ListCriteria openCrit = new ListCriteria();
            openCrit.setKey(SearchKey.STATUS);
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

            List<AlarmReportType> alarms = RulesDataSourceResponseMapper.mapToAlarmListFromResponse(response).getAlarms();

            for (AlarmReportType alarm : alarms) {
                RawMovementType rawMovementType = alarm.getRawMovement();

                // TODO: Use better type (some variaqtion of PluginType...)
                String pluginType = alarm.getPluginType();
                MovementRefType refType = setMovementReportReceived(rawMovementType, pluginType);

                // Close ok reprocessed alarms
                if (refType.getType().equals(REF_TYPE_MOVEMENT)) {
                    alarm.setStatus(AlarmStatusType.CLOSED);
                    updateAlarmStatus(alarm);
                }
            }

            // TODO: Better......................
            return "OK";

        } catch (RulesModelMapperException | MessageException e) {
            throw new RulesServiceException(e.getMessage());
        }
    }

    @Override
    public MovementRefType setMovementReportReceived(RawMovementType rawMovement, String pluginType) throws RulesServiceException {
        try {
            Date auditTimestamp = new Date();

            MobileTerminalType mobileTerminal = null;
            Vessel vessel = null;

            LOG.debug("Incoming comChannelType {}", rawMovement.getComChannelType().name());
            if (rawMovement.getComChannelType().name().equals(MovementComChannelType.MOBILE_TERMINAL.name())) {
                // Get Mobile Terminal
                mobileTerminal = getMobileTerminalByRawMovement(rawMovement);
                auditTimestamp = auditLog("Time to fetch from Mobile Terminal Module:", auditTimestamp);

                // Get Vessel
                if (mobileTerminal != null) {
                    String connectId = mobileTerminal.getConnectId();
                    vessel = getVesselByConnectId(connectId);
                    auditTimestamp = auditLog("Time to fetch from Vessel Module:", auditTimestamp);
                }
            } else if (rawMovement.getComChannelType().name().equals(MovementComChannelType.FLUX.name()) || rawMovement.getComChannelType().name().equals(MovementComChannelType.MANUAL.name())) {
                // Get Vessel
                vessel = getVesselByAssetId(rawMovement.getAssetId());
            } else {
                LOG.error("[ Unknown type {} ]", rawMovement.getComChannelType().name());
            }

            RawMovementFact rawMovementFact = RulesUtil.mapRawMovementFact(rawMovement, mobileTerminal, vessel, pluginType);
            LOG.debug("rawMovementFact:{}", rawMovementFact);

            rulesValidator.evaluate(rawMovementFact);
            auditTimestamp = auditLog("Time to validate sanity:", auditTimestamp);

            if (rawMovementFact.isOk()) {
                LOG.info("Send the validated raw position to Movement");

                MovementBaseType movementBaseType = RulesMapper.getInstance().getMapper().map(rawMovement, MovementBaseType.class);

                movementBaseType.setConnectId(rawMovementFact.getVesselConnectId());

                String createMovementRequest = MovementModuleRequestMapper.mapToCreateMovementRequest(movementBaseType);

                // Send to Movement
                String messageId = producer.sendDataSourceMessage(createMovementRequest, DataSourceQueue.MOVEMENT);
                TextMessage response = consumer.getMessage(messageId, TextMessage.class);
                auditTimestamp = auditLog("Time to get movement from Movement Module:", auditTimestamp);

                if (response != null) {
                    MovementType createdMovement = RulesMapper.mapCreateMovementToMovementType(response);
                    validateCreatedMovement(createdMovement, rawMovementFact, vessel);

                    // Tell Exchange that a movement was persisted in Movement
                    MovementRefType ref = new MovementRefType();
                    ref.setMovementRefGuid(createdMovement.getGuid());
                    ref.setType(REF_TYPE_MOVEMENT);
                    return ref;
                } else {
                    LOG.error("[ Error when getting movement from Movement , response from JMS Queue is null ]");
                    throw new RulesServiceException("[ Error when getting movement from Movement , response from JMS Queue is null ]");
                }
            } else {
                // Tell Exchange that the report caused an alarm
                MovementRefType ref = new MovementRefType();
                ref.setMovementRefGuid(rawMovementFact.getMovementGuid());
                ref.setType(REF_TYPE_ALARM);
                return ref;
            }
        } catch (MessageException |MobileTerminalModelMapperException | MobileTerminalUnmarshallException | JMSException |  ModelMarshallException | VesselModelMapperException e) {
            throw new RulesServiceException(e.getMessage());
        }
    }

    private void validateCreatedMovement(MovementType movement, RawMovementFact rawFact, Vessel vessel) {
        Date auditTimestamp = new Date();

        // Mobile Terminal
        String mobileTerminalDnid = rawFact.getMobileTerminalDnid();
        String mobileTerminalMemberNumber = rawFact.getMobileTerminalMemberNumber();
        String mobileTerminalSerialNumber = rawFact.getMobileTerminalSerialNumber();

        // Vessel
        String assetGroup = null;
        String vesselGuid = null;
        if (vessel != null) {
            // TODO: get assetGroup to validate on
            assetGroup = "GO_GET_IT!!!";

            vesselGuid = vessel.getVesselId().getGuid();
        }

        try {
            LOG.info("Validating movement from Movement Module");

            // TODO Decide if we really want to verify stuff from MobileTerminal
            // Enrich with extra mobile terminal data

            // TODO: Get vecinityOf to validate on
            // ??? Maybe Movement?
            String vicinityOf = "GO_GET_IT!!!";

            MovementFact movementFact = RulesUtil.mapMovementFact(movement, vessel, mobileTerminalDnid, mobileTerminalMemberNumber, mobileTerminalSerialNumber);
            LOG.debug("movementFact:{}", movementFact);

            rulesValidator.evaluate(movementFact);
            auditTimestamp = auditLog("Time to validate rules:", auditTimestamp);

            persistThisMovementReport(vesselGuid, movement);
            auditTimestamp = auditLog("Time to persist the position time:", auditTimestamp);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Vessel getVesselByConnectId(String connectId) throws VesselModelMapperException, MessageException {
        LOG.info("Fetch vessel by connectId");

        VesselListQuery query = new VesselListQuery();
        VesselListCriteria criteria = new VesselListCriteria();
        VesselListCriteriaPair criteriaPair = new VesselListCriteriaPair();
        criteriaPair.setKey(ConfigSearchField.GUID);
        criteriaPair.setValue(connectId);
        criteria.getCriterias().add(criteriaPair);
        criteria.setIsDynamic(true);

        query.setVesselSearchCriteria(criteria);

        VesselListPagination pagination = new VesselListPagination();
        pagination.setListSize(1);
        pagination.setPage(1);
        query.setPagination(pagination);

        String getVesselRequest = VesselModuleRequestMapper.createVesselListModuleRequest(query);
        String getVesselMessageId = producer.sendDataSourceMessage(getVesselRequest, DataSourceQueue.VESSEL);
        TextMessage getVesselResponse = consumer.getMessage(getVesselMessageId, TextMessage.class);

        List<Vessel> resultList = VesselModuleResponseMapper.mapToVesselListFromResponse(getVesselResponse, getVesselMessageId);
        Vessel result = resultList.isEmpty()?null:resultList.get(0);

        return  result;
    }

    private Vessel getVesselByAssetId(AssetId assetId) throws VesselModelMapperException, MessageException {
        LOG.info("Fetch vessel by assetId");
        VesselListQuery query = new VesselListQuery();

        List<AssetIdList> ids = assetId.getAssetIdList();
        VesselListCriteria criteria = new VesselListCriteria();
        for (AssetIdList id : ids) {

            VesselListCriteriaPair crit = new VesselListCriteriaPair();
            switch (id.getIdType()) {
                case CFR:
                    crit.setKey(ConfigSearchField.CFR);
                    crit.setValue(id.getValue());
                    criteria.getCriterias().add(crit);
                    break;
                case IRCS:
                    crit.setKey(ConfigSearchField.IRCS);
                    crit.setValue(id.getValue());
                    criteria.getCriterias().add(crit);
                    break;
                case IMO:
                    crit.setKey(ConfigSearchField.IMO);
                    crit.setValue(id.getValue());
                    criteria.getCriterias().add(crit);
                    break;
                case MMSI:
                    crit.setKey(ConfigSearchField.MMSI);
                    crit.setValue(id.getValue());
                    criteria.getCriterias().add(crit);
                    break;
                case GUID:
                case ID:
                default:
                    LOG.error("[ Unhandled AssetId: {} ]", id.getIdType());
                    break;
            }

        }

        criteria.setIsDynamic(true);
        query.setVesselSearchCriteria(criteria);
        VesselListPagination pagination = new VesselListPagination();
        pagination.setListSize(1);
        pagination.setPage(1);
        query.setPagination(pagination);

        String getVesselListRequest = VesselModuleRequestMapper.createVesselListModuleRequest(query);
        String getVesselMessageId = producer.sendDataSourceMessage(getVesselListRequest, DataSourceQueue.VESSEL);
        TextMessage getMobileTerminalResponse = consumer.getMessage(getVesselMessageId, TextMessage.class);

        List<Vessel> resultList = VesselModuleResponseMapper.mapToVesselListFromResponse(getMobileTerminalResponse, getVesselMessageId);
        Vessel result = resultList.isEmpty()?null:resultList.get(0);

        return result;
    }

    private MobileTerminalType getMobileTerminalByRawMovement(RawMovementType rawMovement) throws MessageException, MobileTerminalModelMapperException, MobileTerminalUnmarshallException, JMSException {
        LOG.info("Fetch mobile terminal");
        MobileTerminalListQuery query = new MobileTerminalListQuery();

        List<IdList> ids = rawMovement.getMobileTerminal().getMobileTerminalIdList();

        MobileTerminalSearchCriteria criteria = new MobileTerminalSearchCriteria();
        for (IdList id : ids) {
            eu.europa.ec.fisheries.schema.mobileterminal.types.v1.ListCriteria crit = new eu.europa.ec.fisheries.schema.mobileterminal.types.v1.ListCriteria();
            switch (id.getType()) {
                case DNID:
                    crit.setKey(eu.europa.ec.fisheries.schema.mobileterminal.types.v1.SearchKey.DNID);
                    crit.setValue(id.getValue());
                    criteria.getCriterias().add(crit);
                    break;
                case MEMBER_NUMBER:
                    crit.setKey(eu.europa.ec.fisheries.schema.mobileterminal.types.v1.SearchKey.MEMBER_NUMBER);
                    crit.setValue(id.getValue());
                    criteria.getCriterias().add(crit);
                    break;
                case SERIAL_NUMBER:
                    crit.setKey(eu.europa.ec.fisheries.schema.mobileterminal.types.v1.SearchKey.SERIAL_NUMBER);
                    crit.setValue(id.getValue());
                    criteria.getCriterias().add(crit);
                    break;
                case LES:
                default:
                    LOG.error("[ Unhandled Mobile Terminal id: {} ]", id.getType());
                    break;
            }
        }

        // If we know the transponder type from the source, use it in the search criteria
        eu.europa.ec.fisheries.schema.mobileterminal.types.v1.ListCriteria transponderTypeCrit = new eu.europa.ec.fisheries.schema.mobileterminal.types.v1.ListCriteria();
        switch(rawMovement.getSource()) {
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
        pagination.setListSize(1);
        pagination.setPage(1);
        query.setPagination(pagination);

        String getMobileTerminalListRequest = MobileTerminalModuleRequestMapper.createMobileTerminalListRequest(query);
        String getMobileTerminalMessageId = producer.sendDataSourceMessage(getMobileTerminalListRequest, DataSourceQueue.MOBILE_TERMINAL);
        TextMessage getMobileTerminalResponse = consumer.getMessage(getMobileTerminalMessageId, TextMessage.class);

        List<MobileTerminalType> resultList = MobileTerminalModuleResponseMapper.mapToMobileTerminalListResponse(getMobileTerminalResponse);
        MobileTerminalType result = resultList.isEmpty()?null:resultList.get(0);

        return result;
    }

    private void persistThisMovementReport(String vesselGuid, MovementType movement) throws MessageException, RulesModelMapperException {
        // I assume we will aways have a guid for vessel
        PreviousReportType thisReport = new PreviousReportType();
        thisReport.setMovementGuid(movement.getGuid());
        thisReport.setPositionTime(movement.getPositionTime());
        thisReport.setVesselGuid(vesselGuid);

        String upsertPreviousReportequest = RulesDataSourceRequestMapper.mapUpsertPreviousReport(thisReport);
        producer.sendDataSourceMessage(upsertPreviousReportequest, DataSourceQueue.INTERNAL);
    }

    private Date auditLog(String msg, Date lastTimestamp) {
        Date newTimestamp = new Date();
        long duration = newTimestamp.getTime() - lastTimestamp.getTime();
        LOG.info("--> AUDIT - {} {}ms", msg, duration);
        return newTimestamp;
    }

    @Override
    public AlarmReportType getAlarmReportByGuid(String guid) throws RulesServiceException {
        try {
            String getAlarmReportRequest = RulesDataSourceRequestMapper.mapGetAlarmByGuid(guid);
            String messageId = producer.sendDataSourceMessage(getAlarmReportRequest, DataSourceQueue.INTERNAL);
            TextMessage response = consumer.getMessage(messageId, TextMessage.class);
            SingleAlarmResponse singleAlarmReportResponse = JAXBMarshaller.unmarshallTextMessage(response, SingleAlarmResponse.class);
            return singleAlarmReportResponse.getAlarm();
        } catch (MessageException | RulesModelMarshallException e) {
            LOG.error("[ Error when getting alarm by GUID ] {}", e.getMessage());
            throw new RulesServiceException("[ Error when getting alarm by GUID. ]");
        }
    }

    @Override
    public TicketType getTicketByGuid(String guid) throws RulesServiceException {
        try {
            String getTicketReportRequest = RulesDataSourceRequestMapper.mapGetTicketByGuid(guid);
            String messageId = producer.sendDataSourceMessage(getTicketReportRequest, DataSourceQueue.INTERNAL);
            TextMessage response = consumer.getMessage(messageId, TextMessage.class);
            SingleTicketResponse singleTicketResponse = JAXBMarshaller.unmarshallTextMessage(response, SingleTicketResponse.class);
            return singleTicketResponse.getTicket();
        } catch (MessageException | RulesModelMarshallException e) {
            LOG.error("[ Error when getting ticket by GUID ] {}", e.getMessage());
            throw new RulesServiceException("[ Error when getting ticket by GUID. ]");
        }
    }

}
