package eu.europa.ec.fisheries.uvms.rules.service.business.fact;

import eu.europa.ec.fisheries.schema.sales.*;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

public class SalesAAPProductFactTest {

    private SalesAAPProductFact fact;

    @Before
    public void setUp() {
        fact = new SalesAAPProductFact();
    }

    @Test
    public void isBMSSpeciesAndUsageIsNotForNonDirectHumanConsumptionWhenUsageIsHCN() throws Exception {
        fact.setSpecifiedSizeDistribution(new SizeDistributionType().withClassCodes(new eu.europa.ec.fisheries.schema.sales.CodeType().withValue("BMS")));
        fact.setUsageCode(new CodeType("HCN"));

        assertTrue(fact.isBMSSpeciesAndUsageIsNotForNonDirectHumanConsumption());
    }

    @Test
    public void isBMSSpeciesAndUsageIsNotForNonDirectHumanConsumptionWhenUsageIsHCNIndirect() throws Exception {
        fact.setSpecifiedSizeDistribution(new SizeDistributionType().withClassCodes(new eu.europa.ec.fisheries.schema.sales.CodeType().withValue("BMS")));
        fact.setUsageCode(new CodeType("HCN-INDIRECT"));

        assertFalse(fact.isBMSSpeciesAndUsageIsNotForNonDirectHumanConsumption());
    }

    @Test
    public void isBMSSpeciesAndUsageIsNotForNonDirectHumanConsumptionWhenSizeDistributionIsNull() throws Exception {
        fact.setUsageCode(new CodeType("HCN-INDIRECT"));

        assertFalse(fact.isBMSSpeciesAndUsageIsNotForNonDirectHumanConsumption());
    }

    @Test
    public void isBMSSpeciesAndUsageIsNotForNonDirectHumanConsumptionWhenSizeDistributionHasNoClassCodes() throws Exception {
        fact.setSpecifiedSizeDistribution(new SizeDistributionType());
        fact.setUsageCode(new CodeType("HCN-INDIRECT"));

        assertFalse(fact.isBMSSpeciesAndUsageIsNotForNonDirectHumanConsumption());
    }

    @Test
    public void isBMSSpeciesAndUsageIsNotForNonDirectHumanConsumptionWhenTheClassCodeIsNull() throws Exception {
        fact.setSpecifiedSizeDistribution(new SizeDistributionType().withClassCodes((eu.europa.ec.fisheries.schema.sales.CodeType) null));
        fact.setUsageCode(new CodeType("HCN-INDIRECT"));

        assertFalse(fact.isBMSSpeciesAndUsageIsNotForNonDirectHumanConsumption());
    }

    @Test
    public void isBMSSpeciesAndUsageIsNotForNonDirectHumanConsumptionWhenTheClassCodeValueIsNull() throws Exception {
        fact.setSpecifiedSizeDistribution(new SizeDistributionType().withClassCodes(new eu.europa.ec.fisheries.schema.sales.CodeType()));
        fact.setUsageCode(new CodeType("HCN-INDIRECT"));

        assertFalse(fact.isBMSSpeciesAndUsageIsNotForNonDirectHumanConsumption());
    }

    @Test
    public void isBMSSpeciesAndUsageIsNotForNonDirectHumanConsumptionWhenTheUsageCodeIsNull() throws Exception {
        fact.setSpecifiedSizeDistribution(new SizeDistributionType().withClassCodes(new eu.europa.ec.fisheries.schema.sales.CodeType().withValue("BMS")));

        assertTrue(fact.isBMSSpeciesAndUsageIsNotForNonDirectHumanConsumption());
    }

    @Test
    public void isOriginFLUXLocationEmptyOrTypeNotLocationWhenTypeIsNotLocation() throws Exception {
        fact.setOriginFLUXLocations(Collections.singletonList(new FLUXLocationType().withTypeCode(new eu.europa.ec.fisheries.schema.sales.CodeType().withValue("NOT LOCATION"))));

        assertTrue(fact.isOriginFLUXLocationEmptyOrTypeNotLocation());
    }

    @Test
    public void isOriginFLUXLocationEmptyOrTypeNotLocationWhenTypeIsLocation() throws Exception {
        fact.setOriginFLUXLocations(Collections.singletonList(new FLUXLocationType()
                .withTypeCode(new eu.europa.ec.fisheries.schema.sales.CodeType().withValue("LOCATION"))));

        assertFalse(fact.isOriginFLUXLocationEmptyOrTypeNotLocation());
    }

    @Test
    public void countFAOAreaCodesWhen2() {
        // data set
        FLUXLocationType fluxLocation1 = new FLUXLocationType().withID(new IDType().withSchemeID("FAO_AREA"));
        FLUXLocationType fluxLocation2 = new FLUXLocationType().withID(new IDType().withSchemeID("SOMETHING_ELSE"));
        FLUXLocationType fluxLocation3 = new FLUXLocationType().withID(new IDType().withSchemeID("FAO_AREA"));
        List<FLUXLocationType> originFluxLocations = Arrays.asList(fluxLocation1, fluxLocation2, fluxLocation3);

        SalesAAPProductFact fact = new SalesAAPProductFact();
        fact.setOriginFLUXLocations(originFluxLocations);

        // execute and assert
        assertEquals(2, fact.countFAOAreaCodes());
    }

    @Test
    public void countFAOAreaCodesWhen1() {
        // data set
        FLUXLocationType fluxLocation1 = new FLUXLocationType().withID(new IDType().withSchemeID("FAO_AREA"));
        FLUXLocationType fluxLocation2 = new FLUXLocationType().withID(new IDType().withSchemeID("SOMETHING_ELSE"));
        List<FLUXLocationType> originFluxLocations = Arrays.asList(fluxLocation1, fluxLocation2);

        SalesAAPProductFact fact = new SalesAAPProductFact();
        fact.setOriginFLUXLocations(originFluxLocations);

        // execute and assert
        assertEquals(1, fact.countFAOAreaCodes());
    }

    @Test
    public void countFAOAreaCodesWhen0() {
        // data set
        FLUXLocationType fluxLocation1 = new FLUXLocationType().withID(new IDType().withSchemeID("ANOTHER_ONE"));
        FLUXLocationType fluxLocation2 = new FLUXLocationType().withID(new IDType().withSchemeID("SOMETHING_ELSE"));
        List<FLUXLocationType> originFluxLocations = Arrays.asList(fluxLocation1, fluxLocation2);

        SalesAAPProductFact fact = new SalesAAPProductFact();
        fact.setOriginFLUXLocations(originFluxLocations);

        // execute and assert
        assertEquals(0, fact.countFAOAreaCodes());
    }

    @Test
    public void containsMultipleFAOAreaCodesWhenNoOriginFLUXLocations() {
        //data set
        SalesAAPProductFact fact = new SalesAAPProductFact();

        // execute and assert
        assertEquals(0, fact.countFAOAreaCodes());
    }

    @Test
    public void countFAOAreaCodesWhen2AndOneOfTheIDsIsNull(){
        // data set
        FLUXLocationType fluxLocation1 = new FLUXLocationType().withID(new IDType().withSchemeID("FAO_AREA"));
        FLUXLocationType fluxLocation2 = new FLUXLocationType().withID(null);
        FLUXLocationType fluxLocation3 = new FLUXLocationType().withID(new IDType().withSchemeID("FAO_AREA"));
        List<FLUXLocationType> originFluxLocations = Arrays.asList(fluxLocation1, fluxLocation2, fluxLocation3);

        SalesAAPProductFact fact = new SalesAAPProductFact();
        fact.setOriginFLUXLocations(originFluxLocations);

        // execute and assert
        assertEquals(2, fact.countFAOAreaCodes());
    }

    @Test
    public void countFAOAreaCodesWhen1AndOneOfTheIDsIsNull() {
        // data set
        FLUXLocationType fluxLocation1 = new FLUXLocationType().withID(new IDType().withSchemeID("FAO_AREA"));
        FLUXLocationType fluxLocation2 = new FLUXLocationType().withID(new IDType().withSchemeID("SOMETHING_ELSE"));
        FLUXLocationType fluxLocation3 = new FLUXLocationType().withID(null);
        List<FLUXLocationType> originFluxLocations = Arrays.asList(fluxLocation1, fluxLocation2, fluxLocation3);

        SalesAAPProductFact fact = new SalesAAPProductFact();
        fact.setOriginFLUXLocations(originFluxLocations);

        // execute and assert
        assertEquals(1, fact.countFAOAreaCodes());
    }

    @Test
    public void countFAOAreaCodesWhen2AndOneOfTheSchemeIDsIsNull(){
        // data set
        FLUXLocationType fluxLocation1 = new FLUXLocationType().withID(new IDType().withSchemeID("FAO_AREA"));
        FLUXLocationType fluxLocation2 = new FLUXLocationType().withID(new IDType().withSchemeID(null));
        FLUXLocationType fluxLocation3 = new FLUXLocationType().withID(new IDType().withSchemeID("FAO_AREA"));
        List<FLUXLocationType> originFluxLocations = Arrays.asList(fluxLocation1, fluxLocation2, fluxLocation3);

        SalesAAPProductFact fact = new SalesAAPProductFact();
        fact.setOriginFLUXLocations(originFluxLocations);

        // execute and assert
        assertEquals(2, fact.countFAOAreaCodes());
    }

    @Test
    public void countFAOAreaCodesWhen1AndOneOfTheSchemeIDsIsNull() {
        // data set
        FLUXLocationType fluxLocation1 = new FLUXLocationType().withID(new IDType().withSchemeID("FAO_AREA"));
        FLUXLocationType fluxLocation2 = new FLUXLocationType().withID(new IDType().withSchemeID("SOMETHING_ELSE"));
        FLUXLocationType fluxLocation3 = new FLUXLocationType().withID(new IDType().withSchemeID(null));
        List<FLUXLocationType> originFluxLocations = Arrays.asList(fluxLocation1, fluxLocation2, fluxLocation3);

        SalesAAPProductFact fact = new SalesAAPProductFact();
        fact.setOriginFLUXLocations(originFluxLocations);

        // execute and assert
        assertEquals(1, fact.countFAOAreaCodes());
    }

    @Test
    public void countFAOAreaCodesWhen0AndOneOfTheSchemeIDsIsNull() {
        // data set
        FLUXLocationType fluxLocation1 = new FLUXLocationType().withID(new IDType().withSchemeID("ANOTHER_ONE"));
        FLUXLocationType fluxLocation2 = new FLUXLocationType().withID(new IDType().withSchemeID("SOMETHING_ELSE"));
        FLUXLocationType fluxLocation3 = new FLUXLocationType().withID(new IDType().withSchemeID(null));
        List<FLUXLocationType> originFluxLocations = Arrays.asList(fluxLocation1, fluxLocation2, fluxLocation3);

        SalesAAPProductFact fact = new SalesAAPProductFact();
        fact.setOriginFLUXLocations(originFluxLocations);

        // execute and assert
        assertEquals(0, fact.countFAOAreaCodes());
    }

    @Test
    public void equalsAndHashCode() {
        EqualsVerifier.forClass(SalesAAPProductFact.class)
                .suppress(Warning.STRICT_INHERITANCE)
                .suppress(Warning.NONFINAL_FIELDS)
                .withPrefabValues(FACatchType.class, new FACatchType().withTypeCode(new eu.europa.ec.fisheries.schema.sales.CodeType().withValue("a")), new FACatchType().withTypeCode(new eu.europa.ec.fisheries.schema.sales.CodeType().withValue("b")))
                .withPrefabValues(AAPProductType.class, new AAPProductType().withSpeciesCode(new eu.europa.ec.fisheries.schema.sales.CodeType().withValue("a")), new AAPProductType().withSpeciesCode(new eu.europa.ec.fisheries.schema.sales.CodeType().withValue("b")))
                .withPrefabValues(FLUXLocationType.class, new FLUXLocationType().withID(new IDType().withValue("BE")), new FLUXLocationType().withID(new IDType().withValue("SWE")))
                .withPrefabValues(FishingActivityType.class, new FishingActivityType().withIDS(new IDType().withValue("BE")), new FishingActivityType().withIDS(new IDType().withValue("SWE")))
                .withRedefinedSuperclass()
                .withIgnoredFields("messageType", "factType", "warnings", "errors", "uniqueIds", "ok", "sequence", "source", "senderOrReceiver", "salesCategoryType", "originatingPlugin")
                .verify();
    }

}