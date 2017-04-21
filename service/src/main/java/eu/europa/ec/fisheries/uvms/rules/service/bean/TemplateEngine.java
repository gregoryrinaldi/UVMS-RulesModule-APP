/*
 *
 * Developed by the European Commission - Directorate General for Maritime Affairs and Fisheries European Union, 2015-2016.
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

import java.util.List;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Singleton;
import javax.inject.Inject;

import eu.europa.ec.fisheries.remote.RulesDomainModel;
import eu.europa.ec.fisheries.uvms.rules.model.dto.TemplateRuleMapDto;
import eu.europa.ec.fisheries.uvms.rules.model.exception.RulesModelException;
import eu.europa.ec.fisheries.uvms.rules.service.business.AbstractFact;
import org.apache.commons.collections.CollectionUtils;

@Singleton
@LocalBean
public class TemplateEngine {

    @EJB
    private RulesDomainModel rulesDb;

    private FactRuleEvaluator ruleEvaluator = FactRuleEvaluator.getInstance();

    @PostConstruct
    public void initialize() {
        ruleEvaluator.initializeRules(getAllTemplates());
        updateFailedRules(ruleEvaluator.getFailedRules());
    }

    public void evaluateFacts(List<AbstractFact> facts) throws RulesModelException {
        if (!CollectionUtils.isEmpty(facts)) {
            ruleEvaluator.validateFact(facts);
        }
    }

    private List<TemplateRuleMapDto> getAllTemplates() {
        try {
            return rulesDb.getAllFactTemplatesAndRules();
        } catch (RulesModelException e) {
            throw new IllegalStateException(e);
        }
    }

    private void updateFailedRules(List<String> failedBrIds) {
        try {
            rulesDb.updateFailedRules(failedBrIds);
        } catch (RulesModelException e) {
            throw new IllegalStateException(e);
        }
    }
}
