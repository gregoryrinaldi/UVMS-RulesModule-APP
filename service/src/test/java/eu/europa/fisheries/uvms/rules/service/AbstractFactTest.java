/*
 Developed by the European Commission - Directorate General for Maritime Affairs and Fisheries @ European Union, 2015-2016.

 This file is part of the Integrated Fisheries Data Management (IFDM) Suite. The IFDM Suite is free software: you can redistribute it
 and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of
 the License, or any later version. The IFDM Suite is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 details. You should have received a copy of the GNU General Public License along with the IFDM Suite. If not, see <http://www.gnu.org/licenses/>.
 */

package eu.europa.fisheries.uvms.rules.service;

import eu.europa.ec.fisheries.schema.sales.SalesPartyType;
import eu.europa.ec.fisheries.uvms.rules.service.bean.RuleTestHelper;
import eu.europa.ec.fisheries.uvms.rules.service.business.AbstractFact;
import eu.europa.ec.fisheries.uvms.rules.service.business.fact.CodeType;
import eu.europa.ec.fisheries.uvms.rules.service.business.fact.FaArrivalFact;
import eu.europa.ec.fisheries.uvms.rules.service.business.fact.IdType;
import org.joda.time.DateTime;
import org.junit.Test;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Gregory Rinaldi
 */
public class AbstractFactTest {

    private AbstractFact fact = new FaArrivalFact();

    @Test
    public void testCheckDateNowHappy() {
        Date date = new DateTime(2005, 3, 26, 12, 0, 0, 0).toDate();
        assertTrue(date.before(fact.dateNow(1)));
    }

    @Test
    public void testListIdContainsAll() {
        List<CodeType> codeTypes = Arrays.asList(RuleTestHelper.getCodeType("val1", "AREA"), RuleTestHelper.getCodeType("val2", "AREA1"));
        assertTrue(fact.listIdContainsAll(codeTypes, "AREA", "AREA1", "BLA"));
    }

    @Test
    public void testValidateIDTypeHappy() {
        IdType idType = new IdType();
        idType.setSchemeId("53e3a36a-d6fa-4ac8-b061-7088327c7d81");
        IdType idType2 = new IdType();
        idType2.setSchemeId("53e36fab361-7338327c7d81");
        List<IdType> idTypes = Arrays.asList(idType, idType2);
        assertTrue(fact.schemeIdContainsAll(idTypes, "UUID"));
    }

    @Test
    public void testValidateIDType() {
        IdType idType = new IdType();
        idType.setSchemeId("53e3a36a-d6fa-4ac8-b061-7088327c7d81");
        IdType idType2 = new IdType();
        idType2.setSchemeId("53e3a36a-d6fa-4ac8-b061-7088327c7d81");
        List<IdType> idTypes = Arrays.asList(idType, idType2);
        assertTrue(fact.schemeIdContainsAll(idTypes, "UUID"));
    }

    @Test
    public void testContainsSchemeIdHappy() {
        IdType idType = new IdType();
        idType.setSchemeId("CFR");
        IdType idType2 = new IdType();
        idType2.setSchemeId("IRCS");
        IdType idType3 = new IdType();
        idType3.setSchemeId("EXT_MARK");
        List<IdType> idTypes = Arrays.asList(idType, idType2, idType3);
        boolean result = fact.schemeIdContainsAll(idTypes, "IRCS", "CFR");
        assertFalse(result);
    }

    @Test
    public void testContainsSchemeIdSad() {

        IdType idType = new IdType();
        idType.setSchemeId("CFR");
        IdType idType2 = new IdType();
        idType2.setSchemeId("IRCS");
        IdType idType3 = new IdType();
        idType3.setSchemeId("UUID");
        List<IdType> idTypes = Arrays.asList(idType, idType2, idType3);
        boolean result = fact.schemeIdContainsAll(idTypes, "UUID");
        assertFalse(result);
    }

    @Test
    public void testValidateFormatUUID_OK(){
        IdType uuidIdType = new IdType();
        uuidIdType.setSchemeId("UUID");
        uuidIdType.setValue(UUID.randomUUID().toString());
        List<IdType> idTypes = Arrays.asList(uuidIdType);
        boolean result = fact.validateFormat(idTypes);
        assertFalse(result);
    }

    @Test
    public void testValidateFormatUUID_NOT_OK(){
        IdType uuidIdType = new IdType();
        uuidIdType.setSchemeId("UUID");
        uuidIdType.setValue("ballshjshdhdfhsgfd");
        List<IdType> idTypes = Arrays.asList(uuidIdType);
        boolean result = fact.validateFormat(idTypes);
        assertTrue(result);
    }

    @Test
    public void testValidateFormatWhenPassingAStringAndResultIsOK(){
        boolean b = fact.validateFormat("aaa", "aaa");
        assertTrue(b);
    }

    @Test
    public void testValidateFormatWhenPassingAStringAndResultIsNOKBecauseArgumentIsNull(){
        boolean b = fact.validateFormat(null, null);
        assertFalse(b);
    }

    @Test
    public void testValidateFormatWhenPassingAStringAndResultIsNOKBecauseArgumentDoesNotApplyToTheFormat(){
        boolean b = fact.validateFormat("aap", "paa");
        assertFalse(b);
    }


    @Test
    public void testValidateFormatWhenSalesSpecificIDAndResultIsOK() {
        boolean b = fact.validateFormat("BEL-SN-2017-123456", AbstractFact.FORMATS.EU_SALES_ID_SPECIFIC.getFormatStr());
        assertTrue(b);
    }

    @Test
    public void testListIdDoesNotContainAllWhenListIdDoesNotContainAllValues() {
        CodeType codeType1 = getCodeTypeWithListID("bla");
        CodeType codeType2 = getCodeTypeWithListID("alb");

        List<CodeType> codeTypeList = Arrays.asList(codeType1, codeType2);

        assertTrue(fact.listIdDoesNotContainAll(codeTypeList, "bla", "notbla"));
    }

    @Test
    public void testListIdDoesNotContainAllWhenListIdDoesContainAllValues() {
        CodeType codeType1 = getCodeTypeWithListID("bla");
        CodeType codeType2 = getCodeTypeWithListID("alb");

        List<CodeType> codeTypeList = Arrays.asList(codeType1, codeType2);

        assertFalse(fact.listIdDoesNotContainAll(codeTypeList, "bla", "alb"));
    }


    @Test
    public void testAnyValueDoesNotContainAllWhenValueDoesNotContainAnyValue() {
        eu.europa.ec.fisheries.schema.sales.CodeType codeType1 = getCodeTypeWithValue("BUYER");
        eu.europa.ec.fisheries.schema.sales.CodeType codeType2 = getCodeTypeWithValue("SELLER");

        SalesPartyType salesPartyType1 = new SalesPartyType();
        salesPartyType1.withRoleCodes(codeType1);

        SalesPartyType salesPartyType2 = new SalesPartyType();
        salesPartyType2.withRoleCodes(codeType2);


        assertTrue(fact.salesPartiesValueDoesNotContainAny(Arrays.asList(salesPartyType1, salesPartyType2),"SENDER"));
    }

    @Test
    public void testAnyValueDoesNotContainAllWhenValueContainsAnyValue() {
        eu.europa.ec.fisheries.schema.sales.CodeType codeType1 = getCodeTypeWithValue("BUYER");
        eu.europa.ec.fisheries.schema.sales.CodeType codeType2 = getCodeTypeWithValue("SELLER");
        eu.europa.ec.fisheries.schema.sales.CodeType codeType3 = getCodeTypeWithValue("SENDER");

        SalesPartyType salesPartyType1 = new SalesPartyType();
        salesPartyType1.withRoleCodes(codeType1);

        SalesPartyType salesPartyType2 = new SalesPartyType();
        salesPartyType2.withRoleCodes(codeType2);

        SalesPartyType salesPartyType3 = new SalesPartyType();
        salesPartyType3.withRoleCodes(codeType3);

        assertFalse(fact.salesPartiesValueDoesNotContainAny(Arrays.asList(salesPartyType1, salesPartyType2, salesPartyType3),"SENDER"));
    }

    @Test
    public void testValueIdTypeContainsAnyWhenValueIsPresent() {
        IdType idType1 = new IdType();
        idType1.setValue("value");
        IdType idType2 = new IdType();
        idType2.setValue("MASTER");

        List<IdType> idTypes = Arrays.asList(idType1, idType2);

        assertFalse(fact.valueIdTypeContainsAny(idTypes, "MASTER", "AGENT", "OWNER", "OPERATOR"));
    }

    @Test
    public void testValueIdTypeContainsAnyWhenValueIsNotPresent() {
        IdType idType1 = new IdType();
        idType1.setValue("value");
        IdType idType2 = new IdType();
        idType2.setValue("eulav");

        List<IdType> idTypes = Arrays.asList(idType1, idType2);

        assertTrue(fact.valueIdTypeContainsAny(idTypes, "MASTER", "AGENT", "OWNER", "OPERATOR"));
    }


    private CodeType getCodeTypeWithListID(String listId) {
        CodeType codeType = new CodeType();
        codeType.setListId(listId);
        return codeType;
    }

    private eu.europa.ec.fisheries.schema.sales.CodeType getCodeTypeWithValue(String value) {
        eu.europa.ec.fisheries.schema.sales.CodeType codeType = new eu.europa.ec.fisheries.schema.sales.CodeType();
        codeType.setValue(value);
        return codeType;
    }

}
