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

package eu.europa.ec.fisheries.uvms.rules.service.business;

import eu.europa.ec.fisheries.uvms.rules.service.business.generator.*;
import eu.europa.ec.fisheries.uvms.rules.service.config.BusinessObjectType;

/**
 * Created by padhyad on 4/19/2017.
 */
public class BusinessObjectFactory {

    public static final AbstractGenerator getBusinessObjFactGenerator(BusinessObjectType businessObjectType) {
        switch (businessObjectType) {
            case FLUX_ACTIVITY_REQUEST_MSG:
                return new ActivityRequestFactGenerator();
            case FLUX_ACTIVITY_RESPONSE_MSG:
                return new ActivityResponseFactGenerator();
            case FLUX_ACTIVITY_QUERY_MSG:
                return new ActivityQueryFactGenerator();
            case FLUX_SALES_QUERY_MSG:
                return new SalesQueryFactGenerator();
            case FLUX_SALES_REPORT_MSG:
                return new SalesReportFactGenerator();
            case FLUX_SALES_RESPONSE_MSG:
                return new SalesResponseFactGenerator();
        }
        return null;
    }
}