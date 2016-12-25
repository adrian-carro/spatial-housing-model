package collectors;

import data.Transport;
import utilities.Id;
import housing.Config;
import housing.House;
import housing.Household;
import housing.Model;
import housing.Region;

/**************************************************************************************************
 * Class to collect regional household statistics
 *
 * @author Adrian Carro
 * @since 06/09/2017
 *
 *************************************************************************************************/
public class RegionalHouseholdStats extends CollectorBase {

    //------------------//
    //----- Fields -----//
    //------------------//

    // General fields
    private Config  config = Model.config; // Passes the Model's configuration parameters object to a private field
    private Region  region;
    private Transport transport;

    // Fields for counting numbers of the different types of households and household conditions
    private int     nBTL; // Number of buy-to-let (BTL) households, i.e., households with the BTL gene (includes both active and inactive)
    private int     nActiveBTL; // Number of BTL households with, at least, one BTL property
    private int     nBTLOwnerOccupier; // Number of BTL households owning their home but without any BTL property
    private int     nBTLHomeless; // Number of homeless BTL households
    private int     nNonBTLOwnerOccupier; // Number of non-BTL households owning their home
    private int     nRenting; // Number of (by definition, non-BTL) households renting their home
    private int     nNonBTLHomeless; // Number of homeless non-BTL households
    private int     nFailedBidder;// Number of failed bidders
    private double  nFailedBidTimes;// Average number of times bidders fail to get offers
    private int     nCommuter;// Number of commuter travling from one region to antother

    // Fields for summing annualised total incomes
    private double  activeBTLAnnualisedTotalIncome;
    private double  ownerOccupierAnnualisedTotalIncome;
    private double  rentingAnnualisedTotalIncome;
    private double  homelessAnnualisedTotalIncome;

    // Other fields
    private double  sumStockYield; // Sum of stock gross rental yields of all currently occupied rental properties
    private double  aveMonthlyTravelCost;  //Average monthly travel cost
    private double  aveMonthlyTravelFee;  //Average monthly travel fee

    //------------------------//
    //----- Constructors -----//
    //------------------------//

    /**
     * Initialises the regional household statistics collector with a reference to the region owning it
     *
     * @param region Reference to the region owning both the market and the regional collector
     */
    public RegionalHouseholdStats(Region region) {
        setActive(true);
        this.region = region;
    }
    
    public RegionalHouseholdStats(Region region,Transport transport) {
        setActive(true);
        this.region = region;
        this.transport=transport;
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
        nFailedBidder= 0;
        nFailedBidTimes = 0.0;
        nCommuter = 0;
        aveMonthlyTravelCost = 0.0;
        aveMonthlyTravelFee = 0.0;
    }

    public void record() {
        // Initialise variables to sum
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
        nFailedBidder= 0;
        nFailedBidTimes= 0.0;
        nCommuter = 0;
        aveMonthlyTravelCost = 0.0;
        aveMonthlyTravelFee = 0.0;
        double sumMonthlyTravelCost = 0.0;
        double sumMonthlyTravelFee = 0.0;
        int cntVaildMonthlyTravelCost=0;
        int cntVaildMonthlyTravelFee=0;
        // TODO: Print to screen and check all these numbers!
        // Run through all households counting population in each type and summing their gross incomes
        for (Household h : region.households) {
            if (h.behaviour.isPropertyInvestor()) {
                ++nBTL;
                // Active BTL investors
                if (h.nInvestmentProperties() > 0) {
                    ++nActiveBTL;
                    activeBTLAnnualisedTotalIncome += h.getMonthlyPreTaxIncome();
                // Inactive BTL investors who own their house
                } else if (h.nInvestmentProperties() == 0) {
                    ++nBTLOwnerOccupier;
                    ownerOccupierAnnualisedTotalIncome += h.getMonthlyPreTaxIncome();
                // Inactive BTL investors in social housing
                } else {
                    ++nBTLHomeless;
                    homelessAnnualisedTotalIncome += h.getMonthlyPreTaxIncome();
                }
            } else {
                // Non-BTL investors who own their house
                if (h.isHomeowner()) {
                    ++nNonBTLOwnerOccupier;
                    ownerOccupierAnnualisedTotalIncome += h.getMonthlyPreTaxIncome();
                // Non-BTL investors renting
                } else if (h.isRenting()) {
                    ++nRenting;
                    rentingAnnualisedTotalIncome += h.getMonthlyPreTaxIncome();
                    if (region.regionalHousingMarketStats.getExpAvSalePriceForQuality(h.getHome().getQuality()) > 0) {
                        sumStockYield += h.getHousePayments().get(h.getHome()).monthlyPayment
                                *config.constants.MONTHS_IN_YEAR
                                /region.regionalHousingMarketStats.getExpAvSalePriceForQuality(h.getHome().getQuality());
                    }
                // Non-BTL investors in social housing
                } else if (h.isInSocialHousing()) {
                    // TODO: Once numbers are checked, this "else if" can be replaced by an "else"
                    ++nNonBTLHomeless;
                    homelessAnnualisedTotalIncome += h.getMonthlyPreTaxIncome();
                }
            }
            // check if the househld is a commuter travelling from one region to another
            House home=h.getHome();
            Region region=h.getRegion();
            if(region!=null && home !=null){
            	if(home.getRegion().getRegionId()!=region.getRegionId()){
            		this.nCommuter++;
            	}
            }
            // sum the monthly travl cost of each household who need to commute
            double monthlyTravelCost=h.getMonthlyTravelCost(transport);
            if(monthlyTravelCost>0){
            	sumMonthlyTravelCost+=monthlyTravelCost;
            	cntVaildMonthlyTravelCost++;
            }
            // sum the monthly travl fee of each household who need to commute
            double monthlyTravelFee=h.getMonthlyTravelFee(transport);
            if(monthlyTravelFee>0){
            	sumMonthlyTravelFee+=monthlyTravelFee;
            	cntVaildMonthlyTravelFee++;
            }           
        }
        // Monthly Travel Cost;
        if(cntVaildMonthlyTravelCost>0){
        	this.aveMonthlyTravelCost=sumMonthlyTravelCost*1.0/cntVaildMonthlyTravelCost;
        }
        // Monthly Travel Fee;
        if(cntVaildMonthlyTravelFee>0){
        	this.aveMonthlyTravelFee=sumMonthlyTravelFee*1.0/cntVaildMonthlyTravelFee;
        }
        // Failed Bidders
        nFailedBidder=region.getFailedBidderCounter().size();
        double sum=0;
        for(Id<Household> householdId:region.getFailedBidderCounter().keySet()){
        	sum+=region.getFailedBidderCounter().get(householdId);
        }
        nFailedBidTimes=0;
        if(nFailedBidder>0){
        	nFailedBidTimes=sum*1.0/nFailedBidder;
        }        
        // Annualise monthly income data
        activeBTLAnnualisedTotalIncome *= config.constants.MONTHS_IN_YEAR;
        ownerOccupierAnnualisedTotalIncome *= config.constants.MONTHS_IN_YEAR;
        rentingAnnualisedTotalIncome *= config.constants.MONTHS_IN_YEAR;
        homelessAnnualisedTotalIncome *= config.constants.MONTHS_IN_YEAR;
    }

    //----- Getter/setter methods -----//

    // Getters for numbers of households variables
    public int getnBTL() { return nBTL; }
    public int getnActiveBTL() { return nActiveBTL; }
    public int getnBTLOwnerOccupier() { return nBTLOwnerOccupier; }
    public int getnBTLHomeless() { return nBTLHomeless; }
    public int getnNonBTLOwnerOccupier() { return nNonBTLOwnerOccupier; }
    public int getnRenting() { return nRenting; }
    public int getnNonBTLHomeless() { return nNonBTLHomeless; }
    public int getnOwnerOccupier() { return nBTLOwnerOccupier + nNonBTLOwnerOccupier; }
    public int getnHomeless() { return nBTLHomeless + nNonBTLHomeless; }
    public int getnNonOwner() { return nRenting + getnHomeless(); }
    public int getnFailedBidder() { return nFailedBidder; }
    public double getnFailedBidTimes() { return nFailedBidTimes; }
    public int getnCommuter() { return nCommuter; }
    

    // Getters for annualised income variables
    public double getActiveBTLAnnualisedTotalIncome() { return activeBTLAnnualisedTotalIncome; }
    public double getOwnerOccupierAnnualisedTotalIncome() { return ownerOccupierAnnualisedTotalIncome; }
    public double getRentingAnnualisedTotalIncome() { return rentingAnnualisedTotalIncome; }
    public double getHomelessAnnualisedTotalIncome() { return homelessAnnualisedTotalIncome; }
    public double getNonOwnerAnnualisedTotalIncome() {
        return rentingAnnualisedTotalIncome + homelessAnnualisedTotalIncome;
    }

    // Getters for yield variables
    public double getSumStockYield() { return sumStockYield; }
    public double getAvStockYield() {
        if(nRenting > 0) {
            return sumStockYield/nRenting;
        } else {
            return 0.0;
        }
    }

    // Getters for other variables...
    // ... number of empty houses
    public int getnEmptyHouses() {
        return region.getHousingStock() + nBTLHomeless + nNonBTLHomeless - region.households.size();
    }
    // ... proportion of housing stock owned by buy-to-let investors (all rental properties, plus all empty houses not
    // owned by the construction sector)
    public double getBTLStockFraction() {
        return ((double)(getnEmptyHouses() - region.regionalHousingMarketStats.getnUnsoldNewBuild()
                + nRenting))/region.getHousingStock();
    }
    
    public double getAveMonthlyTravelCost() { return aveMonthlyTravelCost; }
    public double getAveMonthlyTravelFee() { return aveMonthlyTravelFee; }

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
