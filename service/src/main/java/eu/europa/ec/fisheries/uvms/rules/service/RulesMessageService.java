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

package eu.europa.ec.fisheries.uvms.rules.service;

import eu.europa.ec.fisheries.schema.rules.exchange.v1.PluginType;
import eu.europa.ec.fisheries.schema.rules.module.v1.ReceiveSalesQueryRequest;
import eu.europa.ec.fisheries.schema.rules.module.v1.ReceiveSalesReportRequest;
import eu.europa.ec.fisheries.schema.rules.module.v1.ReceiveSalesResponseRequest;
import eu.europa.ec.fisheries.schema.rules.module.v1.RulesBaseRequest;
import eu.europa.ec.fisheries.schema.rules.module.v1.SendSalesReportRequest;
import eu.europa.ec.fisheries.schema.rules.module.v1.SendSalesResponseRequest;
import eu.europa.ec.fisheries.schema.rules.module.v1.SetFLUXFAReportMessageRequest;
import eu.europa.ec.fisheries.schema.rules.module.v1.SetFaQueryMessageRequest;
import eu.europa.ec.fisheries.uvms.rules.model.dto.ValidationResultDto;
import eu.europa.ec.fisheries.uvms.rules.model.exception.RulesModelMarshallException;
import javax.ejb.Local;
import un.unece.uncefact.data.standard.fluxfaquerymessage._3.FLUXFAQueryMessage;
import un.unece.uncefact.data.standard.fluxfareportmessage._3.FLUXFAReportMessage;
import un.unece.uncefact.data.standard.fluxresponsemessage._6.FLUXResponseMessage;

/**
 * Created by padhyad on 5/9/2017.
 */
@Local
public interface RulesMessageService {

    void evaluateFLUXFAReportRequest(SetFLUXFAReportMessageRequest request) throws RulesModelMarshallException;

    FLUXResponseMessage generateFluxResponseMessage(ValidationResultDto faReportValidationResult, FLUXFAReportMessage fluxfaReportMessage);

    FLUXResponseMessage generateFluxResponseMessage(ValidationResultDto faReportValidationResult);

    FLUXResponseMessage generateFluxResponseMessage(ValidationResultDto faReportValidationResult, FLUXFAQueryMessage fluxfaQueryMessage);

    FLUXResponseMessage generateFluxResponseMessage(ValidationResultDto faReportValidationResult, FLUXResponseMessage fluxResponseMessage);

    void validateAndSendResponseToExchange(FLUXResponseMessage fluxResponseMessageType, RulesBaseRequest request, PluginType pluginType);

    void mapAndSendFLUXMdrRequestToExchange(String request);

    void mapAndSendFLUXMdrResponseToMdrModule(String request);

    void receiveSalesQueryRequest(ReceiveSalesQueryRequest receiveSalesQueryRequest);

    void receiveSalesReportRequest(ReceiveSalesReportRequest receiveSalesReportRequest);

    void sendSalesReportRequest(SendSalesReportRequest rulesRequest);

    void receiveSalesResponseRequest(ReceiveSalesResponseRequest rulesRequest);

    void sendSalesResponseRequest(SendSalesResponseRequest rulesRequest);

    String getValidationsForRawMessageGuid(String guid, String type);

    void evaluateFaQueryRequest(SetFaQueryMessageRequest request);
}