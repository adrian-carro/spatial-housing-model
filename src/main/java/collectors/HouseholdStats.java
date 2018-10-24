package collectors;

import housing.Geography;
import housing.Model;
import housing.Region;

/**************************************************************************************************
 * Class to aggregate all regional household statistics
 *
 * @author daniel, Adrian Carro
 *
 *************************************************************************************************/
public class HouseholdStats extends CollectorBase {

    //------------------//
    //----- Fields -----//
    //------------------//

    // General fields
    private Geography           geography;

    // Fields for counting numbers of the different types of households and household conditions
    private int                 nBTL; // Number of buy-to-let (BTL) households, i.e., households with the BTL gene (includes both active and inactive)
    private int                 nActiveBTL; // Number of BTL households with, at least, one BTL property
    private int                 nBTLOwnerOccupier; // Number of BTL households owning their home but without any BTL property
    private int                 nBTLHomeless; // Number of homeless BTL households
    private int                 nBTLBankruptcies; // Number of BTL households going bankrupt in a given time step
    private int                 nNonBTLOwnerOccupier; // Number of non-BTL households owning their home
    private int                 nRenting; // Number of (by definition, non-BTL) households renting their home
    private int                 nNonBTLHomeless; // Number of homeless non-BTL households
    private int                 nNonBTLBankruptcies; // Number of non-BTL households going bankrupt in a given time step

    // Fields for summing annualised total incomes
    private double              activeBTLAnnualisedTotalIncome;
    private double              ownerOccupierAnnualisedTotalIncome;
    private double              rentingAnnualisedTotalIncome;
    private double              homelessAnnualisedTotalIncome;

    // Other fields
    private double              sumStockYield; // Sum of stock gross rental yields of all currently occupied rental properties
    private double              sumCommutingFees;
    private double              sumCommutingCost;
    private int                 nCommuters;
    private int                 nNonBTLBidsAboveExpAvSalePrice; // Number of normal (non-BTL) bids with desired housing expenditure above the exponential moving average sale price
    private int                 nBTLBidsAboveExpAvSalePrice; // Number of BTL bids with desired housing expenditure above the exponential moving average sale price

    //------------------------//
    //----- Constructors -----//
    //------------------------//

    /**
     * Initialises the national household statistics collector
     *
     * @param geography Reference to the whole geography of regions
     */
    public HouseholdStats(Geography geography) {
        setActive(true);
        this.geography = geography;
    }

    //-------------------//
    //----- Methods -----//
    //-------------------//

    /**
     * Sets initial values for all relevant variables to enforce a controlled first measure for statistics
     */
    public void init() {
        nBTL = 0;
        nActiveBTL = 0;
        nBTLOwnerOccupier = 0;
        nBTLHomeless = 0;
        nBTLBankruptcies = 0;
        nNonBTLOwnerOccupier = 0;
        nRenting = 0;
        nNonBTLHomeless = 0;
        nNonBTLBankruptcies = 0;
        activeBTLAnnualisedTotalIncome = 0.0;
        ownerOccupierAnnualisedTotalIncome = 0.0;
        rentingAnnualisedTotalIncome = 0.0;
        homelessAnnualisedTotalIncome = 0.0;
        sumStockYield = 0.0;
        sumCommutingFees = 0.0;
        sumCommutingCost = 0.0;
        nCommuters = 0;
        nNonBTLBidsAboveExpAvSalePrice = 0;
        nBTLBidsAboveExpAvSalePrice = 0;
    }

    /**
     * Collects current values, apart from updating those to be computed, for all relevant variables from the regional
     * household statistics objects
     */
    public void collectRegionalRecords() {
        // Re-initiate to zero variables to sum over regions
        nBTL = 0;
        nActiveBTL = 0;
        nBTLOwnerOccupier = 0;
        nBTLHomeless = 0;
        nBTLBankruptcies = 0;
        nNonBTLOwnerOccupier = 0;
        nRenting = 0;
        nNonBTLHomeless = 0;
        nNonBTLBankruptcies = 0;
        activeBTLAnnualisedTotalIncome = 0.0;
        ownerOccupierAnnualisedTotalIncome = 0.0;
        rentingAnnualisedTotalIncome = 0.0;
        homelessAnnualisedTotalIncome = 0.0;
        sumStockYield = 0.0;
        sumCommutingFees = 0.0;
        sumCommutingCost = 0.0;
        nCommuters = 0;
        nNonBTLBidsAboveExpAvSalePrice = 0;
        nBTLBidsAboveExpAvSalePrice = 0;
        // Run through regions summing
        for (Region region : geography.getRegions()) {
            nBTL += region.regionalHouseholdStats.getnBTL();
            nActiveBTL += region.regionalHouseholdStats.getnActiveBTL();
            nBTLOwnerOccupier += region.regionalHouseholdStats.getnBTLOwnerOccupier();
            nBTLHomeless += region.regionalHouseholdStats.getnBTLHomeless();
            nBTLBankruptcies += region.regionalHouseholdStats.getnBTLBankruptcies();
            nNonBTLOwnerOccupier += region.regionalHouseholdStats.getnNonBTLOwnerOccupier();
            nRenting += region.regionalHouseholdStats.getnRenting();
            nNonBTLHomeless += region.regionalHouseholdStats.getnNonBTLHomeless();
            nNonBTLBankruptcies += region.regionalHouseholdStats.getnNonBTLBankruptcies();
            activeBTLAnnualisedTotalIncome += region.regionalHouseholdStats.getActiveBTLAnnualisedTotalIncome();
            ownerOccupierAnnualisedTotalIncome += region.regionalHouseholdStats.getOwnerOccupierAnnualisedTotalIncome();
            rentingAnnualisedTotalIncome += region.regionalHouseholdStats.getRentingAnnualisedTotalIncome();
            homelessAnnualisedTotalIncome += region.regionalHouseholdStats.getHomelessAnnualisedTotalIncome();
            sumStockYield += region.regionalHouseholdStats.getSumStockYield();
            sumCommutingFees += region.regionalHouseholdStats.getSumCommutingFees();
            sumCommutingCost += region.regionalHouseholdStats.getSumCommutingCost();
            nCommuters += region.regionalHouseholdStats.getnCommuters();
            nNonBTLBidsAboveExpAvSalePrice += region.regionalHouseholdStats.getnNonBTLBidsAboveExpAvSalePrice();
            nBTLBidsAboveExpAvSalePrice += region.regionalHouseholdStats.getnBTLBidsAboveExpAvSalePrice();
        }
    }

    //----- Getter/setter methods -----//

    // Getters for numbers of households variables
    int getnBTL() { return nBTL; }
    int getnActiveBTL() { return nActiveBTL; }
    int getnBTLOwnerOccupier() { return nBTLOwnerOccupier; }
    int getnBTLHomeless() { return nBTLHomeless; }
    int getnBTLBankruptcies() { return nBTLBankruptcies; }
    int getnNonBTLOwnerOccupier() { return nNonBTLOwnerOccupier; }
    int getnRenting() { return nRenting; }
    int getnNonBTLHomeless() { return nNonBTLHomeless; }
    int getnNonBTLBankruptcies() { return nNonBTLBankruptcies; }
    int getnOwnerOccupier() { return nBTLOwnerOccupier + nNonBTLOwnerOccupier; }
    int getnHomeless() { return nBTLHomeless + nNonBTLHomeless; }
    int getnNonOwner() { return nRenting + getnHomeless(); }

    // Getters for annualised income variables
    double getActiveBTLAnnualisedTotalIncome() { return activeBTLAnnualisedTotalIncome; }
    double getOwnerOccupierAnnualisedTotalIncome() { return ownerOccupierAnnualisedTotalIncome; }
    double getRentingAnnualisedTotalIncome() { return rentingAnnualisedTotalIncome; }
    double getHomelessAnnualisedTotalIncome() { return homelessAnnualisedTotalIncome; }
    double getNonOwnerAnnualisedTotalIncome() {
        return rentingAnnualisedTotalIncome + homelessAnnualisedTotalIncome;
    }

    // Getters for yield variables
    double getSumStockYield() { return sumStockYield; }
    double getAvStockYield() {
        if(nRenting > 0) {
            return sumStockYield/nRenting;
        } else {
            return 0.0;
        }
    }

    // Getters for other variables...
    // ... number of empty houses
    int getnEmptyHouses() {
        return Model.construction.getHousingStock() + nBTLHomeless + nNonBTLHomeless
                - Model.demographics.getTotalPopulation();
    }
    // ... proportion of housing stock owned by buy-to-let investors (all rental properties, plus all empty houses not
    // owned by the construction sector)
    double getBTLStockFraction() {
        return ((double)(getnEmptyHouses() - Model.housingMarketStats.getnUnsoldNewBuild()
                + nRenting))/Model.construction.getHousingStock();
    }
    // ... number of normal (non-BTL) bidders with desired housing expenditure above the exponential moving average sale price
    int getnNonBTLBidsAboveExpAvSalePrice() { return nNonBTLBidsAboveExpAvSalePrice; }
    // ... number of BTL bidders with desired housing expenditure above the exponential moving average sale price
    int getnBTLBidsAboveExpAvSalePrice() { return nBTLBidsAboveExpAvSalePrice; }
    int getnCommuters() { return nCommuters; }
    double getSumCommutingFees() { return sumCommutingFees; }
    double getSumCommutingCost() { return sumCommutingCost; }
}
