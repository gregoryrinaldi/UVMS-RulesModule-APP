/*
 Developed by the European Commission - Directorate General for Maritime Affairs and Fisheries @ European Union, 2015-2016.

 This file is part of the Integrated Fisheries Data Management (IFDM) Suite. The IFDM Suite is free software: you can redistribute it
 and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of
 the License, or any later version. The IFDM Suite is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 details. You should have received a copy of the GNU General Public License along with the IFDM Suite. If not, see <http://www.gnu.org/licenses/>.
 */

package eu.europa.ec.fisheries.uvms.rules.service.bean;

import static java.util.concurrent.TimeUnit.SECONDS;

import javax.annotation.PostConstruct;
import javax.ejb.AccessTimeout;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.base.Stopwatch;
import eu.europa.ec.fisheries.remote.RulesDomainModel;
import eu.europa.ec.fisheries.schema.rules.rule.v1.ExternalRuleType;
import eu.europa.ec.fisheries.schema.rules.rule.v1.RuleType;
import eu.europa.ec.fisheries.schema.rules.template.v1.FactType;
import eu.europa.ec.fisheries.uvms.rules.model.dto.TemplateRuleMapDto;
import eu.europa.ec.fisheries.uvms.rules.model.exception.RulesModelException;
import eu.europa.ec.fisheries.uvms.rules.service.MDRCacheRuleService;
import eu.europa.ec.fisheries.uvms.rules.service.business.EnrichedBRMessage;
import eu.europa.ec.fisheries.uvms.rules.service.business.TemplateFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.drools.template.parser.DefaultTemplateContainer;
import org.drools.template.parser.TemplateContainer;
import org.drools.template.parser.TemplateDataListener;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.Message;
import org.kie.api.builder.Results;
import org.kie.api.definition.KiePackage;
import org.kie.api.definition.rule.Rule;
import org.kie.api.runtime.KieContainer;

@Singleton
@Startup
@Slf4j
public class RulesKieContainerInitializer {

    @EJB
    private RulesDomainModel rulesDb;

    @EJB
    private MDRCacheRuleService cacheService;

    private Map<ContainerType, KieContainer> containers;

    @PostConstruct
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void init() {
        try {
            Stopwatch stopwatch = Stopwatch.createStarted();
            List<TemplateRuleMapDto> allTemplates = rulesDb.getAllFactTemplatesAndRules();
            enrichRulesWithMDR(allTemplates);

            List<TemplateRuleMapDto> faResponseTemplatesAndRules = getFaResponseRules(allTemplates);
            List<TemplateRuleMapDto> faTemplatesAndRules = getFaMessageRules(allTemplates);
            List<TemplateRuleMapDto> salesTemplatesAndRules = getSalesRules(allTemplates);
            List<TemplateRuleMapDto> faQueryTemplatesAndRules = getFaQueryRules(allTemplates);

            log.info("Initializing templates and rules for FA-Report facts. Nr. of Rules : {}", faTemplatesAndRules.size());
            KieContainer faReportContainer = createContainer(faTemplatesAndRules);

            log.info("Initializing templates and rules for FA-Response facts. Nr. of Rules : {}", faResponseTemplatesAndRules.size());
            KieContainer faRespContainer = createContainer(faResponseTemplatesAndRules);

            log.info("Initializing templates and rules for FA-Query facts. Nr. of Rules : {}", faQueryTemplatesAndRules.size());
            KieContainer faQueryContainer =  createContainer(faQueryTemplatesAndRules);

            log.info("Initializing templates and rules forSales facts. Nr. of Rules : {}", salesTemplatesAndRules.size());
            KieContainer salesContainer = createContainer(salesTemplatesAndRules);

            containers = new EnumMap<>(ContainerType.class);
            containers.put(ContainerType.FA_REPORT, faReportContainer);
            containers.put(ContainerType.FA_RESPONSE, faRespContainer);
            containers.put(ContainerType.FA_QUERY, faQueryContainer);
            containers.put(ContainerType.SALES, salesContainer);

            // To make sure that we have deployed all the templates!
            if (!allTemplates.isEmpty()) {
                throw new RuntimeException("Please include all the <code>FactType</code> in the KieContainers");
            }
            log.info("It took " + stopwatch + " to initialize the rules.");
        } catch (RulesModelException e) {
            log.error(e.getMessage(), e);
        }
    }

    private KieContainer createContainer(Collection<TemplateRuleMapDto> templates) {
        Map<String, String> drlsAndRules = new HashMap<>();
        for (TemplateRuleMapDto template : templates) {
            String templateFile = TemplateFactory.getTemplateFileName(template.getTemplateType().getType());
            String templateName = template.getTemplateType().getTemplateName();
            drlsAndRules.putAll(generateRulesFromTemplate(templateName, templateFile, template.getRules()));
            drlsAndRules.putAll(generateExternalRulesFromTemplate(template.getExternalRules()));
        }

        KieServices kieServices = KieServices.Factory.get();
        KieFileSystem kieFileSystem = kieServices.newKieFileSystem();
        for (Map.Entry<String, String> ruleEntrySet : drlsAndRules.entrySet()) {
            String rule = ruleEntrySet.getKey();
            String templateName = ruleEntrySet.getValue();
            String systemPackage = "src/main/resources/rule/" + templateName + ".drl";
            kieFileSystem.write(systemPackage, rule);
        }
        KieBuilder kieBuilder = kieServices.newKieBuilder(kieFileSystem).buildAll();

        Results results = kieBuilder.getResults();
        if (results.hasMessages(Message.Level.ERROR)) {
            for (Message result : results.getMessages()) {
                log.debug(result.getText());
            }
            throw new RuntimeException("COMPILATION ERROR IN RULES. PLEASE ADAPT THE FAILING EXPRESSIONS AND TRY AGAIN");
        }
        return kieServices.newKieContainer(kieServices.getRepository().getDefaultReleaseId());
    }

    private Map<String, String> generateRulesFromTemplate(String templateName, String templateFile, List<RuleType> rules) {
        if (CollectionUtils.isEmpty(rules)) {
            return Collections.emptyMap();
        }
        InputStream templateStream = this.getClass().getResourceAsStream(templateFile);
        TemplateContainer tc = new DefaultTemplateContainer(templateStream);
        Map<String, String> drlsAndBrId = new HashMap<>();
        TemplateDataListener listener = new TemplateDataListener(tc);
        int rowNum = 0;
        for (RuleType ruleDto : rules) {
            listener.newRow(rowNum, 0);
            listener.newCell(rowNum, 0, templateName, 0);
            listener.newCell(rowNum, 1, ruleDto.getExpression(), 0);
            listener.newCell(rowNum, 2, ruleDto.getBrId(), 0);
            listener.newCell(rowNum, 3, ruleDto.getMessage(), 0);
            listener.newCell(rowNum, 4, ruleDto.getErrorType().value(), 0);
            listener.newCell(rowNum, 5, ruleDto.getLevel(), 0);
            listener.newCell(rowNum, 6, ruleDto.getPropertyNames(), 0);
            rowNum++;
        }
        listener.finishSheet();
        String drl = listener.renderDRL();
        log.debug(drl);
        drlsAndBrId.put(drl, templateName);
        return drlsAndBrId;
    }

    private Map<String, String> generateExternalRulesFromTemplate(List<ExternalRuleType> externalRules) {
        if (CollectionUtils.isEmpty(externalRules)) {
            return Collections.emptyMap();
        }
        Map<String, String> drlsAndBrId = new HashMap<>();
        for (ExternalRuleType extRuleType : externalRules) {
            String drl = extRuleType.getDrl();
            log.debug("DRL for BR Id {} : {} ", extRuleType.getBrId(), drl);
            drlsAndBrId.put(drl, extRuleType.getBrId());
        }
        return drlsAndBrId;
    }

    @AccessTimeout(value = 180, unit = SECONDS)
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void reload() {
        init();
    }

    private List<TemplateRuleMapDto> getSalesRules(List<TemplateRuleMapDto> templatesAndRules) {
        List<TemplateRuleMapDto> responseTemplates = new ArrayList<>();
        List<FactType> salesFactsTypes = getSalesFactsTypes();
        for (TemplateRuleMapDto templatesAndRule : templatesAndRules) {
            if (salesFactsTypes.contains(templatesAndRule.getTemplateType().getType())) {
                responseTemplates.add(templatesAndRule);
            }
        }
        templatesAndRules.removeAll(responseTemplates);
        return responseTemplates;
    }

    private List<TemplateRuleMapDto> getFaMessageRules(List<TemplateRuleMapDto> templatesAndRules) {
        List<TemplateRuleMapDto> faTemplates = new ArrayList<>();
        List<FactType> faReportFacts = getFaReportFactsTypes();
        for (TemplateRuleMapDto templatesAndRule : templatesAndRules) {
            if (faReportFacts.contains(templatesAndRule.getTemplateType().getType())) {
                faTemplates.add(templatesAndRule);
            }
        }
        templatesAndRules.removeAll(faTemplates);
        return faTemplates;
    }

    private List<TemplateRuleMapDto> getFaResponseRules(List<TemplateRuleMapDto> templatesAndRules) {
        List<TemplateRuleMapDto> responseTemplates = new ArrayList<>();
        for (TemplateRuleMapDto templatesAndRule : templatesAndRules) {
            if (FactType.FA_RESPONSE.equals(templatesAndRule.getTemplateType().getType()) ||
                    FactType.FA_VALIDATION_QUALITY_ANALYSIS.equals(templatesAndRule.getTemplateType().getType())) {
                responseTemplates.add(templatesAndRule);
            }
        }
        templatesAndRules.removeAll(responseTemplates);
        return responseTemplates;
    }

    private List<TemplateRuleMapDto> getFaQueryRules(List<TemplateRuleMapDto> allTemplates) {
        List<TemplateRuleMapDto> faQueryTemplates = new ArrayList<>();
        for (TemplateRuleMapDto actualTemplate : allTemplates) {
            if (FactType.FA_QUERY.equals(actualTemplate.getTemplateType().getType()) ||
                    FactType.FA_QUERY_PARAMETER.equals(actualTemplate.getTemplateType().getType())) {
                faQueryTemplates.add(actualTemplate);
            }
        }
        allTemplates.removeAll(faQueryTemplates);
        return faQueryTemplates;
    }

    private void enrichRulesWithMDR(List<TemplateRuleMapDto> templatesAndRules) {
        cacheService.loadCacheForFailureMessages();
        for (TemplateRuleMapDto templatesAndRule : templatesAndRules) {
            for (RuleType ruleType : templatesAndRule.getRules()) {
                EnrichedBRMessage enrichedBRMessage = cacheService.getErrorMessageForBrId(ruleType.getBrId());
                if (enrichedBRMessage != null) {
                    String errorMessageForBrId = enrichedBRMessage.getMessage();
                    if (StringUtils.isNotEmpty(errorMessageForBrId)) {
                        ruleType.setMessage(errorMessageForBrId.replaceAll("\"", "&quot;"));
                        enrichedBRMessage.setTemplateEntityName(templatesAndRule.getTemplateType().getType().name());
                        enrichedBRMessage.setExpression(ruleType.getExpression());
                    }
                }
            }
        }
    }

    public KieContainer getContainerByType(ContainerType containerType) {
        return containers.get(containerType);
    }

    public boolean isRulesLoaded() {
        List<Rule> deployedRules = new ArrayList<>();
        KieContainer container = containers.get(ContainerType.FA_REPORT);
        Collection<KiePackage> kiePackages = container.getKieBase().getKiePackages();
        for (KiePackage kiePackage : kiePackages) {
            deployedRules.addAll(kiePackage.getRules());
        }
        return CollectionUtils.isNotEmpty(deployedRules);
    }

    private List<FactType> getFaReportFactsTypes() {
        return new ArrayList<FactType>() {{
            add(FactType.FA_REPORT_DOCUMENT);
            add(FactType.FLUX_FA_REPORT_MESSAGE);
            add(FactType.VESSEL_TRANSPORT_MEANS);
            add(FactType.STRUCTURED_ADDRESS);
            add(FactType.FISHING_GEAR);
            add(FactType.GEAR_CHARACTERISTIC);
            add(FactType.GEAR_PROBLEM);
            add(FactType.FA_CATCH);
            add(FactType.FISHING_TRIP);
            add(FactType.FLUX_LOCATION);
            add(FactType.FLUX_CHARACTERISTIC);
            add(FactType.VESSEL_STORAGE_CHARACTERISTIC);
            add(FactType.FISHING_ACTIVITY);
            add(FactType.FA_DEPARTURE);
            add(FactType.FA_ENTRY_TO_SEA);
            add(FactType.FA_FISHING_OPERATION);
            add(FactType.FA_JOINT_FISHING_OPERATION);
            add(FactType.FA_RELOCATION);
            add(FactType.FA_DISCARD);
            add(FactType.FA_EXIT_FROM_SEA);
            add(FactType.FA_NOTIFICATION_OF_ARRIVAL);
            add(FactType.FA_ARRIVAL);
            add(FactType.FA_LANDING);
            add(FactType.FA_TRANSHIPMENT);
            add(FactType.FA_NOTIFICATION_OF_TRANSHIPMENT);
        }};
    }

    private List<FactType> getSalesFactsTypes() {
        return new ArrayList<FactType>() {{
            add(FactType.SALES_FLUX_SALES_REPORT_MESSAGE);
            add(FactType.SALES_FLUX_REPORT_DOCUMENT);
            add(FactType.SALES_FLUX_PARTY);
            add(FactType.SALES_REPORT);
            add(FactType.SALES_DOCUMENT);
            add(FactType.SALES_PARTY);
            add(FactType.SALES_EVENT);
            add(FactType.SALES_BATCH);
            add(FactType.SALES_AAP_PRODUCT);
            add(FactType.SALES_AAP_PROCESS);
            add(FactType.SALES_SIZE_DISTRIBUTION);
            add(FactType.SALES_PRICE);
            add(FactType.SALES_FLUX_ORGANIZATION);
            add(FactType.SALES_FISHING_ACTIVITY);
            add(FactType.SALES_DELIMITED_PERIOD);
            add(FactType.SALES_VESSEL_TRANSPORT_MEANS);
            add(FactType.SALES_VESSEL_COUNTRY);
            add(FactType.SALES_CONTACT_PARTY);
            add(FactType.SALES_CONTACT_PERSON);
            add(FactType.SALES_FISHING_TRIP);
            add(FactType.SALES_FLUX_LOCATION);
            add(FactType.SALES_FLUX_GEOGRAPHICAL_COORDINATE);
            add(FactType.SALES_STRUCTURED_ADDRESS);
            add(FactType.SALES_QUERY);
            add(FactType.SALES_FLUX_RESPONSE_DOCUMENT);
            add(FactType.SALES_VALIDATION_RESULT_DOCUMENT);
            add(FactType.SALES_VALIDATION_QUALITY_ANALYSIS);
            add(FactType.SALES_REPORT_WRAPPER);
            add(FactType.SALES_AUCTION_SALE);
            add(FactType.SALES_FLUX_SALES_QUERY_MESSAGE);
            add(FactType.SALES_QUERY_PARAMETER);
            add(FactType.SALES_FLUX_SALES_RESPONSE_MESSAGE);
        }};
    }
}