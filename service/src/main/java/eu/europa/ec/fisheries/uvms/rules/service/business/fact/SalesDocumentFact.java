package eu.europa.ec.fisheries.uvms.rules.service.business.fact;

import eu.europa.ec.fisheries.schema.rules.template.v1.FactType;
import eu.europa.ec.fisheries.schema.sales.*;
import eu.europa.ec.fisheries.uvms.rules.service.business.AbstractFact;
import eu.europa.ec.fisheries.uvms.rules.service.business.helper.SalesFactHelper;

import java.util.Arrays;
import java.util.List;

public class SalesDocumentFact extends AbstractFact {

    private List<IdType> ids;
    private CodeType currencyCode;
    private List<IdType> transportDocumentIDs;
    private List<IdType> salesNoteIDs;
    private List<IdType> takeoverDocumentIDs;
    private List<SalesBatchType> specifiedSalesBatches;
    private List<SalesEventType> specifiedSalesEvents;
    private List<FishingActivityType> specifiedFishingActivities;
    private List<FLUXLocationType> specifiedFLUXLocations;
    private List<SalesPartyType> specifiedSalesParties;
    private VehicleTransportMeansType specifiedVehicleTransportMeans;
    private List<ValidationResultDocumentType> relatedValidationResultDocuments;
    private SalesPriceType totalSalesPrice;
    private FLUXLocationType departureSpecifiedFLUXLocation;
    private FLUXLocationType arrivalSpecifiedFLUXLocation;

    @Override
    public void setFactType() {
        this.factType = FactType.SALES_DOCUMENT;
    }

    public List<IdType> getIDS() {
        return this.ids;
    }

    public CodeType getCurrencyCode() {
        return this.currencyCode;
    }

    public List<IdType> getTransportDocumentIDs() {
        return this.transportDocumentIDs;
    }

    public List<IdType> getSalesNoteIDs() {
        return this.salesNoteIDs;
    }

    public List<IdType> getTakeoverDocumentIDs() {
        return this.takeoverDocumentIDs;
    }

    public List<SalesBatchType> getSpecifiedSalesBatches() {
        return this.specifiedSalesBatches;
    }

    public List<SalesEventType> getSpecifiedSalesEvents() {
        return this.specifiedSalesEvents;
    }

    public List<FishingActivityType> getSpecifiedFishingActivities() {
        return this.specifiedFishingActivities;
    }

    public List<FLUXLocationType> getSpecifiedFLUXLocations() {
        return this.specifiedFLUXLocations;
    }

    public List<SalesPartyType> getSpecifiedSalesParties() {
        return this.specifiedSalesParties;
    }

    public VehicleTransportMeansType getSpecifiedVehicleTransportMeans() {
        return this.specifiedVehicleTransportMeans;
    }

    public List<ValidationResultDocumentType> getRelatedValidationResultDocuments() {
        return this.relatedValidationResultDocuments;
    }

    public SalesPriceType getTotalSalesPrice() {
        return this.totalSalesPrice;
    }

    public FLUXLocationType getDepartureSpecifiedFLUXLocation() {
        return this.departureSpecifiedFLUXLocation;
    }

    public FLUXLocationType getArrivalSpecifiedFLUXLocation() {
        return this.arrivalSpecifiedFLUXLocation;
    }

    public void setIDS(List<IdType> ids) {
        this.ids = ids;
    }

    public void setCurrencyCode(CodeType currencyCode) {
        this.currencyCode = currencyCode;
    }

    public void setTransportDocumentIDs(List<IdType> transportDocumentIDs) {
        this.transportDocumentIDs = transportDocumentIDs;
    }

    public void setSalesNoteIDs(List<IdType> salesNoteIDs) {
        this.salesNoteIDs = salesNoteIDs;
    }

    public void setTakeoverDocumentIDs(List<IdType> takeoverDocumentIDs) {
        this.takeoverDocumentIDs = takeoverDocumentIDs;
    }

    public void setSpecifiedSalesBatches(List<SalesBatchType> specifiedSalesBatches) {
        this.specifiedSalesBatches = specifiedSalesBatches;
    }

    public void setSpecifiedSalesEvents(List<SalesEventType> specifiedSalesEvents) {
        this.specifiedSalesEvents = specifiedSalesEvents;
    }

    public void setSpecifiedFishingActivities(List<FishingActivityType> specifiedFishingActivities) {
        this.specifiedFishingActivities = specifiedFishingActivities;
    }

    public void setSpecifiedFLUXLocations(List<FLUXLocationType> specifiedFLUXLocations) {
        this.specifiedFLUXLocations = specifiedFLUXLocations;
    }

    public void setSpecifiedSalesParties(List<SalesPartyType> specifiedSalesParties) {
        this.specifiedSalesParties = specifiedSalesParties;
    }

    public void setSpecifiedVehicleTransportMeans(VehicleTransportMeansType specifiedVehicleTransportMeans) {
        this.specifiedVehicleTransportMeans = specifiedVehicleTransportMeans;
    }

    public void setRelatedValidationResultDocuments(List<ValidationResultDocumentType> relatedValidationResultDocuments) {
        this.relatedValidationResultDocuments = relatedValidationResultDocuments;
    }

    public void setTotalSalesPrice(SalesPriceType totalSalesPrice) {
        this.totalSalesPrice = totalSalesPrice;
    }

    public void setDepartureSpecifiedFLUXLocation(FLUXLocationType departureSpecifiedFLUXLocation) {
        this.departureSpecifiedFLUXLocation = departureSpecifiedFLUXLocation;
    }

    public void setArrivalSpecifiedFLUXLocation(FLUXLocationType arrivalSpecifiedFLUXLocation) {
        this.arrivalSpecifiedFLUXLocation = arrivalSpecifiedFLUXLocation;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SalesDocumentFact that = (SalesDocumentFact) o;

        if (ids != null ? !ids.equals(that.ids) : that.ids != null) return false;
        if (currencyCode != null ? !currencyCode.equals(that.currencyCode) : that.currencyCode != null) return false;
        if (transportDocumentIDs != null ? !transportDocumentIDs.equals(that.transportDocumentIDs) : that.transportDocumentIDs != null)
            return false;
        if (salesNoteIDs != null ? !salesNoteIDs.equals(that.salesNoteIDs) : that.salesNoteIDs != null) return false;
        if (takeoverDocumentIDs != null ? !takeoverDocumentIDs.equals(that.takeoverDocumentIDs) : that.takeoverDocumentIDs != null)
            return false;
        if (specifiedSalesBatches != null ? !specifiedSalesBatches.equals(that.specifiedSalesBatches) : that.specifiedSalesBatches != null)
            return false;
        if (specifiedSalesEvents != null ? !specifiedSalesEvents.equals(that.specifiedSalesEvents) : that.specifiedSalesEvents != null)
            return false;
        if (specifiedFishingActivities != null ? !specifiedFishingActivities.equals(that.specifiedFishingActivities) : that.specifiedFishingActivities != null)
            return false;
        if (specifiedFLUXLocations != null ? !specifiedFLUXLocations.equals(that.specifiedFLUXLocations) : that.specifiedFLUXLocations != null)
            return false;
        if (specifiedSalesParties != null ? !specifiedSalesParties.equals(that.specifiedSalesParties) : that.specifiedSalesParties != null)
            return false;
        if (specifiedVehicleTransportMeans != null ? !specifiedVehicleTransportMeans.equals(that.specifiedVehicleTransportMeans) : that.specifiedVehicleTransportMeans != null)
            return false;
        if (relatedValidationResultDocuments != null ? !relatedValidationResultDocuments.equals(that.relatedValidationResultDocuments) : that.relatedValidationResultDocuments != null)
            return false;
        if (totalSalesPrice != null ? !totalSalesPrice.equals(that.totalSalesPrice) : that.totalSalesPrice != null)
            return false;
        if (departureSpecifiedFLUXLocation != null ? !departureSpecifiedFLUXLocation.equals(that.departureSpecifiedFLUXLocation) : that.departureSpecifiedFLUXLocation != null)
            return false;
        return arrivalSpecifiedFLUXLocation != null ? arrivalSpecifiedFLUXLocation.equals(that.arrivalSpecifiedFLUXLocation) : that.arrivalSpecifiedFLUXLocation == null;
    }

    @Override
    public int hashCode() {
        int result = ids != null ? ids.hashCode() : 0;
        result = 31 * result + (currencyCode != null ? currencyCode.hashCode() : 0);
        result = 31 * result + (transportDocumentIDs != null ? transportDocumentIDs.hashCode() : 0);
        result = 31 * result + (salesNoteIDs != null ? salesNoteIDs.hashCode() : 0);
        result = 31 * result + (takeoverDocumentIDs != null ? takeoverDocumentIDs.hashCode() : 0);
        result = 31 * result + (specifiedSalesBatches != null ? specifiedSalesBatches.hashCode() : 0);
        result = 31 * result + (specifiedSalesEvents != null ? specifiedSalesEvents.hashCode() : 0);
        result = 31 * result + (specifiedFishingActivities != null ? specifiedFishingActivities.hashCode() : 0);
        result = 31 * result + (specifiedFLUXLocations != null ? specifiedFLUXLocations.hashCode() : 0);
        result = 31 * result + (specifiedSalesParties != null ? specifiedSalesParties.hashCode() : 0);
        result = 31 * result + (specifiedVehicleTransportMeans != null ? specifiedVehicleTransportMeans.hashCode() : 0);
        result = 31 * result + (relatedValidationResultDocuments != null ? relatedValidationResultDocuments.hashCode() : 0);
        result = 31 * result + (totalSalesPrice != null ? totalSalesPrice.hashCode() : 0);
        result = 31 * result + (departureSpecifiedFLUXLocation != null ? departureSpecifiedFLUXLocation.hashCode() : 0);
        result = 31 * result + (arrivalSpecifiedFLUXLocation != null ? arrivalSpecifiedFLUXLocation.hashCode() : 0);
        return result;
    }

    protected boolean canEqual(Object other) {
        return other instanceof SalesDocumentFact;
    }

    public boolean isInvalidCurrencyCode(){
        return !SalesFactHelper.doesSetContainAnyValue(Arrays.asList(currencyCode.getValue()), SalesFactHelper.getValidCurrencies());
    }
}
