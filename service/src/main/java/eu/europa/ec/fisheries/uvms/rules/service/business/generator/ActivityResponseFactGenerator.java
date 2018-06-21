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

package eu.europa.ec.fisheries.uvms.rules.service.business.generator;

import eu.europa.ec.fisheries.uvms.commons.date.XMLDateUtils;
import eu.europa.ec.fisheries.uvms.rules.service.business.AbstractFact;
import eu.europa.ec.fisheries.uvms.rules.service.business.fact.IdType;
import eu.europa.ec.fisheries.uvms.rules.service.exception.RulesValidationException;
import eu.europa.ec.fisheries.uvms.rules.service.mapper.fact.ActivityFactMapper;
import eu.europa.ec.fisheries.uvms.rules.service.mapper.xpath.util.XPathStringWrapper;
import org.apache.commons.collections.CollectionUtils;
import org.joda.time.DateTime;
import un.unece.uncefact.data.standard.fluxresponsemessage._6.FLUXResponseMessage;
import un.unece.uncefact.data.standard.reusableaggregatebusinessinformationentity._20.FLUXResponseDocument;
import un.unece.uncefact.data.standard.reusableaggregatebusinessinformationentity._20.ValidationQualityAnalysis;
import un.unece.uncefact.data.standard.reusableaggregatebusinessinformationentity._20.ValidationResultDocument;
import un.unece.uncefact.data.standard.unqualifieddatatype._20.DateTimeType;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static eu.europa.ec.fisheries.uvms.rules.service.config.ExtraValueType.RESPONSE_IDS;
import static eu.europa.ec.fisheries.uvms.rules.service.config.ExtraValueType.SENDER_RECEIVER;
import static eu.europa.ec.fisheries.uvms.rules.service.constants.XPathConstants.*;

/**
 * @author padhyad
 * @author Gregory Rinaldi
 */
public class ActivityResponseFactGenerator extends AbstractGenerator {

    private FLUXResponseMessage fluxResponseMessage;

    private XPathStringWrapper xPathUtil;

    private ActivityFactMapper activityFactMapper;

    public ActivityResponseFactGenerator() {
        xPathUtil = new XPathStringWrapper();
        activityFactMapper = new ActivityFactMapper(xPathUtil);
    }

    @Override
    public void setAdditionalValidationObject() {
        String senderReceiver = (String) extraValueMap.get(SENDER_RECEIVER);
        List<IdType> idsFromDb = (List<IdType>) extraValueMap.get(RESPONSE_IDS);
        activityFactMapper.setSenderReceiver(senderReceiver);
        activityFactMapper.setFaResponseIds(idsFromDb);
    }

    @Override
    public List<AbstractFact> generateAllFacts() {
        List<AbstractFact> facts = new ArrayList<>();
        xPathUtil.append(FLUX_RESPONSE_MESSAGE).append(FLUX_RESPONSE_DOCUMENT);
        facts.add(activityFactMapper.generateFactsForFaResponse(fluxResponseMessage));

        FLUXResponseDocument fluxResponseDocument = fluxResponseMessage.getFLUXResponseDocument();
        if (fluxResponseDocument != null) {
            List<ValidationResultDocument> validationResultDocuments = fluxResponseDocument.getRelatedValidationResultDocuments();
            int index = 1;
            String partialXpath = xPathUtil.getValue();
            for (ValidationResultDocument validationResultDocument : validationResultDocuments) {
                String partialSpecFishActXpath = xPathUtil.appendWithoutWrapping(partialXpath).appendWithIndex(RELATED_VALIDATION_QUALITY_ANALYSIS, index).getValue();

                xPathUtil.appendWithoutWrapping(partialSpecFishActXpath);
                facts.addAll(addFactsForValidationQualityAnalysis(validationResultDocument.getRelatedValidationQualityAnalysises()));

                index++;
            }
        }

        if (fluxResponseMessage.getFLUXResponseDocument() != null){
            DateTimeType creationDateTime = fluxResponseMessage.getFLUXResponseDocument().getCreationDateTime();
            for (AbstractFact fact : facts) {
                if(creationDateTime != null){
                    Date repDat = XMLDateUtils.xmlGregorianCalendarToDate(creationDateTime.getDateTime());
                    if(repDat != null){
                        fact.setCreationDateOfMessage(new DateTime(repDat));
                    }
                }
            }
        }

        return facts;
    }

    private List<AbstractFact> addFactsForValidationQualityAnalysis(List<ValidationQualityAnalysis> validationResultDocuments) {
        List<AbstractFact> facts = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(validationResultDocuments)) {
            int index = 1;
            String partialXpath = xPathUtil.getValue();
            for (ValidationQualityAnalysis validationQualityAnalysis : validationResultDocuments) {

                xPathUtil.appendWithoutWrapping(partialXpath).appendWithIndex(RELATED_VALIDATION_QUALITY_ANALYSIS, index);
                facts.add(activityFactMapper.generateFactsForValidationQualityAnalysis(validationQualityAnalysis));

                index++;
            }
        }

        return facts;
    }

    @Override
    public void setBusinessObjectMessage(Object businessObject) throws RulesValidationException {
        if (!(businessObject instanceof FLUXResponseMessage)) {
            throw new RulesValidationException("Business object does not match required type");
        }
        this.fluxResponseMessage = (FLUXResponseMessage) businessObject;
    }


}
