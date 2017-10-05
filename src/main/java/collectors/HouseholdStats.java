package collectors;

import housing.Model;
import housing.Region;

import java.util.ArrayList;

/**************************************************************************************************
 * Class to aggregate all regional household statistics
 *
 * @author danial, Adrian Carro
 *
 *************************************************************************************************/
public class HouseholdStats extends CollectorBase {
    private static final long serialVersionUID = -402486195880710795L;

    //------------------//
    //----- Fields -----//
    //------------------//

    // General fields
    private ArrayList<Region>   geography;

    // Fields for counting numbers of the different types of households and household conditions
    private int                 nBTL; // Number of buy-to-let (BTL) households, i.e., households with the BTL gene (includes both active and inactive)
    private int                 nActiveBTL; // Number of BTL households with, at least, one BTL property
    private int                 nBTLOwnerOccupier; // Number of BTL households owning their home but without any BTL property
    private int                 nBTLHomeless; // Number of homeless BTL households
    private int                 nNonBTLOwnerOccupier; // Number of non-BTL households owning their home
    private int                 nRenting; // Number of (by definition, non-BTL) households renting their home
    private int                 nNonBTLHomeless; // Number of homeless non-BTL households

    // Fields for summing annualised total incomes
    private double              activeBTLAnnualisedTotalIncome;
    private double              ownerOccupierAnnualisedTotalIncome;
    private double              rentingAnnualisedTotalIncome;
    private double              homelessAnnualisedTotalIncome;

    // Other fields
    private double              sumStockYield; // Sum of stock gross rental yields of all currently occupied rental properties

    //------------------------//
    //----- Constructors -----//
    //------------------------//

    /**
     * Initialises the national household statistics collector
     *
     * @param geography Reference to the whole geography of regions
     */
    public HouseholdStats(ArrayList<Region> geography) {
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
        nNonBTLOwnerOccupier = 0;
        nRenting = 0;
        nNonBTLHomeless = 0;
        activeBTLAnnualisedTotalIncome = 0.0;
        ownerOccupierAnnualisedTotalIncome = 0.0;
        rentingAnnualisedTotalIncome = 0.0;
        homelessAnnualisedTotalIncome = 0.0;
        sumStockYield = 0.0;
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
        nNonBTLOwnerOccupier = 0;
        nRenting = 0;
        nNonBTLHomeless = 0;
        activeBTLAnnualisedTotalIncome = 0.0;
        ownerOccupierAnnualisedTotalIncome = 0.0;
        rentingAnnualisedTotalIncome = 0.0;
        homelessAnnualisedTotalIncome = 0.0;
        sumStockYield = 0.0;
        // Run through regions summing
        for (Region region : geography) {
            nBTL += region.regionalHouseholdStats.getnBTL();
            nActiveBTL += region.regionalHouseholdStats.getnActiveBTL();
            nBTLOwnerOccupier += region.regionalHouseholdStats.getnBTLOwnerOccupier();
            nBTLHomeless += region.regionalHouseholdStats.getnBTLHomeless();
            nNonBTLOwnerOccupier += region.regionalHouseholdStats.getnNonBTLOwnerOccupier();
            nRenting += region.regionalHouseholdStats.getnRenting();
            nNonBTLHomeless += region.regionalHouseholdStats.getnNonBTLHomeless();
            activeBTLAnnualisedTotalIncome += region.regionalHouseholdStats.getActiveBTLAnnualisedTotalIncome();
            ownerOccupierAnnualisedTotalIncome += region.regionalHouseholdStats.getOwnerOccupierAnnualisedTotalIncome();
            rentingAnnualisedTotalIncome += region.regionalHouseholdStats.getRentingAnnualisedTotalIncome();
            homelessAnnualisedTotalIncome += region.regionalHouseholdStats.getHomelessAnnualisedTotalIncome();
            sumStockYield += region.regionalHouseholdStats.getSumStockYield();
        }
    }

    //----- Getter/setter methods -----//

    // Getters for numbers of households variables
    int getnBTL() { return nBTL; }
    int getnActiveBTL() { return nActiveBTL; }
    int getnBTLOwnerOccupier() { return nBTLOwnerOccupier; }
    int getnBTLHomeless() { return nBTLHomeless; }
    int getnNonBTLOwnerOccupier() { return nNonBTLOwnerOccupier; }
    int getnRenting() { return nRenting; }
    int getnNonBTLHomeless() { return nNonBTLHomeless; }
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

//    // Array with ages of all households
//    public double [] getAgeDistribution() {
//        double [] result = new double[region.households.size()];
//        int i = 0;
//        for(Household h : region.households) {
//            result[i] = h.getAge();
//            ++i;
//        }
//        return(result);
//    }
//
//    // Array with ages of renters and households in social housing
//    public double [] getNonOwnerAges() {
//        double [] result = new double[getnNonOwner()];
//        int i = 0;
//        for(Household h : region.households) {
//            if(!h.isHomeowner() && i < getnNonOwner()) {
//                result[i++] = h.getAge();
//            }
//        }
//        while(i < getnNonOwner()) {
//            result[i++] = 0.0;
//        }
//        return(result);
//    }
//
//    // Array with ages of owner-occupiers
//    public double [] getOwnerOccupierAges() {
//        double [] result = new double[getnNonOwner()];
//        int i = 0;
//        for(Household h : region.households) {
//            if(!h.isHomeowner() && i < getnNonOwner()) {
//                result[i] = h.getAge();
//                ++i;
//            }
//        }
//        while(i < getnNonOwner()) {
//            result[i++] = 0.0;
//        }
//        return(result);
//    }
//
//    // Distribution of the number of properties owned by BTL investors
//    public double [] getBTLNProperties() {
//        if(isActive() && nBTL > 0) {
//            double [] result = new double[(int)nBTL];
//            int i = 0;
//            for(Household h : region.households) {
//                if(h.behaviour.isPropertyInvestor() && i<nBTL) {
//                    result[i] = h.nInvestmentProperties();
//                    ++i;
//                }
//            }
//            return(result);
//        }
//        return null;
//    }
//
//    public double [] getLogIncomes() {
//        double [] result = new double[region.households.size()];
//        int i = 0;
//        for(Household h : region.households) {
//            result[i++] = Math.log(h.annualEmploymentIncome());
//        }
//        return(result);
//    }
//
//    public double [] getLogBankBalances() {
//        double [] result = new double[region.households.size()];
//        int i = 0;
//        for(Household h : region.households) {
//            result[i++] = Math.log(Math.max(0.0, h.getBankBalance()));
//        }
//        return(result);
//    }
}
