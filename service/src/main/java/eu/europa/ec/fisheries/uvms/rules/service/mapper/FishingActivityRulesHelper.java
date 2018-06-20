/*
 Developed by the European Commission - Directorate General for Maritime Affairs and Fisheries @ European Union, 2015-2016.

 This file is part of the Integrated Fisheries Data Management (IFDM) Suite. The IFDM Suite is free software: you can redistribute it
 and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of
 the License, or any later version. The IFDM Suite is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 details. You should have received a copy of the GNU General Public License along with the IFDM Suite. If not, see <http://www.gnu.org/licenses/>.
 */

package eu.europa.ec.fisheries.uvms.rules.service.mapper;

import eu.europa.ec.fisheries.uvms.rules.entity.FADocumentID;
import eu.europa.ec.fisheries.uvms.rules.entity.FAUUIDType;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import un.unece.uncefact.data.standard.fluxfareportmessage._3.FLUXFAReportMessage;
import un.unece.uncefact.data.standard.reusableaggregatebusinessinformationentity._20.FAReportDocument;
import un.unece.uncefact.data.standard.reusableaggregatebusinessinformationentity._20.FLUXReportDocument;
import un.unece.uncefact.data.standard.reusableaggregatebusinessinformationentity._20.FishingActivity;
import un.unece.uncefact.data.standard.reusableaggregatebusinessinformationentity._20.FishingTrip;
import un.unece.uncefact.data.standard.unqualifieddatatype._20.CodeType;
import un.unece.uncefact.data.standard.unqualifieddatatype._20.IDType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FishingActivityRulesHelper {

    private static final String DASH = "-";

    public Set<FADocumentID> collectReportIds(FLUXFAReportMessage fluxfaReportMessage) {
        Set<FADocumentID> ids = new HashSet<>();
        if (fluxfaReportMessage != null){
            FLUXReportDocument fluxReportDocument = fluxfaReportMessage.getFLUXReportDocument();
            if (fluxReportDocument != null){
                mapFluxReportDocumentIDS(ids, fluxReportDocument, FAUUIDType.FLUX_FA_REPORT_MESSAGE_ID);
            }
            List<FAReportDocument> faReportDocuments = fluxfaReportMessage.getFAReportDocuments();
            if (CollectionUtils.isNotEmpty(faReportDocuments)){
                mapFaReportDocuments(ids, faReportDocuments);
            }
        }
        return ids;
    }

    private void mapFaReportDocuments(Set<FADocumentID> ids, List<FAReportDocument> faReportDocuments) {
        if (CollectionUtils.isNotEmpty(faReportDocuments)){
            for (FAReportDocument faReportDocument : faReportDocuments) {
                if (faReportDocument != null){
                    mapFluxReportDocumentIDS(ids, faReportDocument.getRelatedFLUXReportDocument(), FAUUIDType.FA_FLUX_REPORT_ID);
                }
            }
        }
    }

    private void mapFluxReportDocumentIDS(Set<FADocumentID> ids, FLUXReportDocument fluxReportDocument, FAUUIDType faFluxMessageId) {
        if (fluxReportDocument != null){
            List<IDType> fluxReportDocumentIDS = fluxReportDocument.getIDS();
            if (CollectionUtils.isNotEmpty(fluxReportDocumentIDS)){
                IDType idType = fluxReportDocumentIDS.get(0);
                if (idType != null){
                    ids.add(new FADocumentID(idType.getValue(), faFluxMessageId));
                }
            }
        }
    }

    public List<String> collectFaIdsAndTripIds(FLUXFAReportMessage fluxFaRepMessage) {
        List<String> idsReqList = new ArrayList<>();
        FLUXReportDocument fluxReportDocument = fluxFaRepMessage.getFLUXReportDocument();
        List<FAReportDocument> faReportDocuments = fluxFaRepMessage.getFAReportDocuments();
        if (fluxReportDocument == null || CollectionUtils.isEmpty(faReportDocuments)) {
            return idsReqList;
        }
        // Purpose code
        //CodeType purposeCode = fluxReportDocument.getPurposeCode();
        // Check if we need the purpose codes! For now it seems not!
        //List<String> purposeCodes = purposeCode != null && StringUtils.isNotEmpty(purposeCode.getValue()) ?
        //Arrays.asList(purposeCode.getValue()) : Arrays.asList("1", "3", "5", "9");

        // FishinActivity type, tripId, tripSchemeId
        for (FAReportDocument faRepDoc : faReportDocuments) {
            collectFromActivityList(idsReqList, faRepDoc.getTypeCode(), faRepDoc.getSpecifiedFishingActivities());
        }
        return idsReqList;
    }

    private void collectFromActivityList(List<String> aIdsPerTripEntityList, CodeType faReportTypeCode, List<FishingActivity> specifiedFishingActivities) {
        if (CollectionUtils.isEmpty(specifiedFishingActivities) || faReportTypeCode == null || StringUtils.isEmpty(faReportTypeCode.getValue())) {
            return;
        }
        String faReportTypeCodeStr = faReportTypeCode.getValue();
        for (FishingActivity fishAct : specifiedFishingActivities) {
            CodeType typeCode = fishAct.getTypeCode();
            FishingTrip fishTrip = fishAct.getSpecifiedFishingTrip();
            if (typeCode == null || StringUtils.isEmpty(typeCode.getValue()) || fishTrip == null || CollectionUtils.isEmpty(fishTrip.getIDS())) {
                continue;
            }
            for (IDType tripId : fishTrip.getIDS()) {
                aIdsPerTripEntityList.add(tripId.getValue() +DASH+ tripId.getSchemeID() +DASH+ typeCode.getValue() +DASH+ faReportTypeCodeStr);
            }
        }
    }

}
