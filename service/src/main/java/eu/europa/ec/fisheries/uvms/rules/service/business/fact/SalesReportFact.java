package eu.europa.ec.fisheries.uvms.rules.service.business.fact;

import eu.europa.ec.fisheries.schema.rules.template.v1.FactType;
import eu.europa.ec.fisheries.schema.sales.*;
import eu.europa.ec.fisheries.uvms.rules.service.business.SalesAbstractFact;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SalesReportFact extends SalesAbstractFact {

    public static final String MDR_FISH_FRESHNESS = "FISH_FRESHNESS";

    private IdType id;
    private CodeType itemTypeCode;
    private List<SalesDocumentFact> includedSalesDocuments;
    private List<ValidationResultDocumentType> includedValidationResultDocuments;

    @Override
    public void setFactType() {
        this.factType = FactType.SALES_REPORT;
    }

    public IdType getID() {
        return this.id;
    }

    public CodeType getItemTypeCode() {
        return this.itemTypeCode;
    }

    public List<SalesDocumentFact> getIncludedSalesDocuments() {
        return this.includedSalesDocuments;
    }

    public List<ValidationResultDocumentType> getIncludedValidationResultDocuments() {
        return this.includedValidationResultDocuments;
    }

    public void setID(IdType id) {
        this.id = id;
    }

    public void setItemTypeCode(CodeType itemTypeCode) {
        this.itemTypeCode = itemTypeCode;
    }

    public void setIncludedSalesDocuments(List<SalesDocumentFact> includedSalesDocuments) {
        this.includedSalesDocuments = includedSalesDocuments;
    }

    public void setIncludedValidationResultDocuments(List<ValidationResultDocumentType> includedValidationResultDocuments) {
        this.includedValidationResultDocuments = includedValidationResultDocuments;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SalesReportFact)) return false;
        SalesReportFact that = (SalesReportFact) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(itemTypeCode, that.itemTypeCode) &&
                Objects.equals(includedSalesDocuments, that.includedSalesDocuments) &&
                Objects.equals(includedValidationResultDocuments, that.includedValidationResultDocuments);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, itemTypeCode, includedSalesDocuments, includedValidationResultDocuments);
    }

    public boolean isSellerRoleNotSpecifiedForSalesNote(){
        if(isItemTypeEqualTo("SN") && !isEmpty(includedSalesDocuments))
        {
            for (SalesDocumentFact salesDocument:includedSalesDocuments) {
                if (salesDocument == null || salesDocument.getSpecifiedSalesParties() == null){
                    return true;
                }

                boolean sellerAvailable = false;

                for (SalesPartyFact salesParty:salesDocument.getSpecifiedSalesParties()) {
                    if (salesParty != null && !valueDoesNotContainAll(salesParty.getRoleCodes(), "SELLER")) {
                        sellerAvailable = true;
                    }
                }

                if (!sellerAvailable) {
                    return true;
                }
            }
        }

        return false;
    }

    public boolean isSellerRoleOrBuyerNotSpecifiedForSalesNoteWithPurchase(){
        if(isItemTypeEqualTo("SN") && !isEmpty(includedSalesDocuments)) {
            for (SalesDocumentFact salesDocument : includedSalesDocuments) {
                // If the document does not have a price greater than zero it can not pass the test since it is not considered a purchase
                if (isTotalZero()) {
                    return false;
                }

                if (salesDocument.getSpecifiedSalesParties() == null) {
                    return true;
                }

                boolean sellerAvailable = false;
                boolean buyerAvailable = false;
                for (SalesPartyFact salesParty : salesDocument.getSpecifiedSalesParties()) {
                    if (salesParty != null && !valueDoesNotContainAll(salesParty.getRoleCodes(), "SELLER")) {
                        sellerAvailable = true;
                    }
                    if (salesParty != null && !valueDoesNotContainAll(salesParty.getRoleCodes(), "BUYER")) {
                        buyerAvailable = true;
                    }
                }

                return !sellerAvailable || !buyerAvailable;
            }
        }
        return false;
    }

    public boolean isRecipientRoleNotSpecifiedForTakeOverDocument(){
        if(isItemTypeEqualTo("TOD") && !isEmpty(includedSalesDocuments))
        {
            for (SalesDocumentFact salesDocument:includedSalesDocuments) {
                if (salesDocument == null || salesDocument.getSpecifiedSalesParties() == null){
                    return true;
                }

                boolean recipientAvailable = false;
                for (SalesPartyFact salesParty:salesDocument.getSpecifiedSalesParties()) {
                    if (salesParty != null && !valueCodeTypeContainsAny(salesParty.getRoleCodes(), "RECIPIENT")) {
                        recipientAvailable = true;
                    }
                }

                if (!recipientAvailable) {
                    return true;
                }
            }
        }

        return false;
    }

    public boolean isFluxOrganizationNotSpecifiedOnAllSalesPartiesForTakeOverDocument(){
        if(isItemTypeEqualTo("TOD") && !isEmpty(includedSalesDocuments))
        {
            for (SalesDocumentFact salesDocument:includedSalesDocuments) {
                if (salesDocument == null || salesDocument.getSpecifiedSalesParties() == null){
                    continue;
                }

                for (SalesPartyFact salesParty:salesDocument.getSpecifiedSalesParties()) {
                    if (salesParty != null && salesParty.getSpecifiedFLUXOrganization() == null) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public boolean isSalesNoteIdentifierNotSpecifiedForTakeOverDocumentWithStoredProducts(){
        if(isItemTypeEqualTo("TOD") && !isEmpty(includedSalesDocuments))
        {
            for (SalesDocumentFact salesDocument:includedSalesDocuments) {
                if (isAnyProductSetWithStorageAsUsage(salesDocument)){
                    if (isEmpty(salesDocument.getSalesNoteIDs()) || isEmpty(salesDocument.getSalesNoteIDs().get(0).getValue())){
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public boolean isSalesNoteAndIsFreshnessNotPresent() {
        return isItemTypeEqualTo("SN") && isFreshnessNotPresent();
    }

    public boolean isTakeOverDocumentAndDoesAFLUXOrganizationNotHaveAName() {
        return isItemTypeEqualTo("TOD") && doesAFLUXOrganizationNotHaveAName();
    }

    public boolean isSalesNoteAndNotAllChargeAmountsAreGreaterThanOrEqualToZero() {
        return isItemTypeEqualTo("SN") && notAllChargeAmountsAreGreaterThanOrEqualToZero();
    }

    public boolean isSalesNoteAndAtLeastOneChargeAmountIsNull() {
        return isItemTypeEqualTo("SN") && isAChargeAmountNull();
    }

    public boolean isSalesNoteAndAnyChargeAmountIsEqualToZero() {
        return isItemTypeEqualTo("SN") && isAChargeAmountZero();
    }

    private boolean isAChargeAmountNull() {
        List<AmountType> chargeAmounts = getAllChargeAmounts();
        for (AmountType amount : chargeAmounts) {
            if (amount == null || amount.getValue() == null) {
                return true;
            }
        }
        return false;
    }

    private boolean isAChargeAmountZero() {
        List<AmountType> chargeAmounts = getAllChargeAmounts();
        for (AmountType amount : chargeAmounts) {
            if (amount != null && amount.getValue() != null && amount.getValue().compareTo(BigDecimal.ZERO) == 0) {
                return true;
            }
        }
        return false;
    }

    private boolean notAllChargeAmountsAreGreaterThanOrEqualToZero() {
        List<AmountType> chargeAmounts = getAllChargeAmounts();
        for (AmountType amount : chargeAmounts) {
            if (amount != null && amount.getValue() != null && amount.getValue().compareTo(BigDecimal.ZERO) < 0) {
                return true;
            }
        }
        return false;
    }

    private List<AmountType> getAllChargeAmounts() {
        List<AAPProductType> products = getProducts();
        SalesPriceType totalSalesPrice = getTotalSalesPrice();

        List<AmountType> chargeAmounts = new ArrayList<>();
        for (AAPProductType product: products) {
            if (product != null && product.getTotalSalesPrice() != null) {
                if (isEmpty(product.getTotalSalesPrice().getChargeAmounts())) {
                    chargeAmounts.add(null);
                } else{
                    chargeAmounts.add(product.getTotalSalesPrice().getChargeAmounts().get(0));
                }
            }
        }
        if (totalSalesPrice != null) {
            if (isEmpty(totalSalesPrice.getChargeAmounts())) {
                chargeAmounts.add(null);
            } else {
                chargeAmounts.add(totalSalesPrice.getChargeAmounts().get(0));
            }
        }
        return chargeAmounts;
    }

    private SalesPriceType getTotalSalesPrice() {
        if (isEmpty(includedSalesDocuments) || includedSalesDocuments.get(0) == null) {
            return null;
        } else{
            return includedSalesDocuments.get(0).getTotalSalesPrice();
        }
    }

    private boolean doesAFLUXOrganizationNotHaveAName() {
        if (!isEmpty(includedSalesDocuments) && includedSalesDocuments.get(0) != null) {
            return doesAFLUXOrganizationNotHaveANameInSalesParties(includedSalesDocuments.get(0).getSpecifiedSalesParties()) ||
                    doesAFLUXOrganizationNotHaveANameInVehicleTransportMeans(includedSalesDocuments.get(0).getSpecifiedVehicleTransportMeans());
        } else {
            return false;
        }
    }

    private boolean doesAFLUXOrganizationNotHaveANameInVehicleTransportMeans(VehicleTransportMeansType specifiedVehicleTransportMeans) {
        return specifiedVehicleTransportMeans != null &&
                doesAFLUXOrganizationNotHaveAName(specifiedVehicleTransportMeans.getOwnerSalesParty());
        }

    private boolean doesAFLUXOrganizationNotHaveAName(SalesPartyType ownerSalesParty) {
        if (ownerSalesParty != null) {
            FLUXOrganizationType specifiedFLUXOrganization = ownerSalesParty.getSpecifiedFLUXOrganization();
            return doesTheFLUXOrganizationNotHaveAName(specifiedFLUXOrganization);
        }
        return false;
    }

    private boolean doesTheFLUXOrganizationNotHaveAName(FLUXOrganizationType specifiedFLUXOrganization) {
        return specifiedFLUXOrganization != null &&
                (specifiedFLUXOrganization.getName() == null || isEmpty(specifiedFLUXOrganization.getName().getValue()));
    }

    private boolean doesAFLUXOrganizationNotHaveANameInSalesParties(List<SalesPartyFact> specifiedSalesParties) {
        if (!isEmpty(specifiedSalesParties)) {
            for (SalesPartyFact salesPartyFact : specifiedSalesParties) {
                if (salesPartyFact != null) {
                    if (doesTheFLUXOrganizationNotHaveAName(salesPartyFact.getSpecifiedFLUXOrganization())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean isFreshnessNotPresent() {
        return isFreshnessNotPresent(getProducts());
    }

    private boolean isFreshnessNotPresent(List<AAPProductType> specifiedAAPProducts) {
        if (!isEmpty(specifiedAAPProducts)) {
            for (AAPProductType aapProductType : specifiedAAPProducts) {
                List<AAPProcessType> appliedAAPProcesses = aapProductType.getAppliedAAPProcesses();
                if (!isEmpty(appliedAAPProcesses)) {
                    for (AAPProcessType aapProcessType : aapProductType.getAppliedAAPProcesses()) {
                        if (isFreshnessNotPresent(aapProcessType)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private boolean isFreshnessNotPresent(AAPProcessType aapProcessType) {
        boolean foundFreshnessForThisAAPProcessType = false;
        for (eu.europa.ec.fisheries.schema.sales.CodeType codeType : aapProcessType.getTypeCodes()) {
            if (MDR_FISH_FRESHNESS.equals(codeType.getListID())) {
                foundFreshnessForThisAAPProcessType = true;
            }
        }
        return !foundFreshnessForThisAAPProcessType;
    }

    private boolean isItemTypeEqualTo(String type) {
        return itemTypeCode != null && Objects.equals(itemTypeCode.getValue(), type);
    }

    private boolean isAnyProductSetWithStorageAsUsage(SalesDocumentFact salesDocument){
        for (AAPProductType product : getProducts()) {
            if (product != null && product.getUsageCode() != null && product.getUsageCode().getValue() == "STO") {
                return true;
            }
        }

        return false;
    }

    private boolean isTotalZero(){
        for (AAPProductType product: getProducts()) {
            if (product != null && product.getTotalSalesPrice() != null
                    && !isEmpty(product.getTotalSalesPrice().getChargeAmounts())){
                for (AmountType amount :product.getTotalSalesPrice().getChargeAmounts()) {
                    if (amount != null && BigDecimal.ZERO.compareTo(amount.getValue()) == -1){
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private List<AAPProductType> getProducts() {
        if (isEmpty(includedSalesDocuments) ||
                includedSalesDocuments.get(0) == null ||
                isEmpty(includedSalesDocuments.get(0).getSpecifiedSalesBatches())){
            return new ArrayList<>();
        }

        List<AAPProductType> products = new ArrayList<>();
        for (SalesBatchType salesBatch : includedSalesDocuments.get(0).getSpecifiedSalesBatches()) {
            if (!isEmpty(salesBatch.getSpecifiedAAPProducts())) {
                products.addAll(salesBatch.getSpecifiedAAPProducts());
            }
        }
        return products;
    }
}
