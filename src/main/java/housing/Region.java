package housing;

import collectors.HouseholdStats;
import collectors.HousingMarketStats;
import collectors.RegionalHouseholdStats;
import collectors.RegionalHousingMarketStats;
import collectors.RegionalRentalMarketStats;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import data.Transport;
import utilities.Id;

/**************************************************************************************************
 * Class to encapsulate everything contained in a region, including its houses, its house sale and
 * rental markets, and the households participating in those markets.
 *
 * @author Adrian Carro
 * @since 04/09/2017
 *
 *************************************************************************************************/
public class Region {

    //------------------//
    //----- Fields -----//
    //------------------//

    public ArrayList<Household>         households;
    public HouseSaleMarket              houseSaleMarket;
    public HouseRentalMarket            houseRentalMarket;
    public RegionalHouseholdStats       regionalHouseholdStats;
    public RegionalHousingMarketStats   regionalHousingMarketStats;
    public RegionalRentalMarketStats    regionalRentalMarketStats;
    public int                          targetPopulation;
    private int                         housingStock;   
    private Id<Region>                  regionId;//region ID or Name
    // Variables for Biding Accoss Regions
    private Map<Id<Household>,Integer>  failedBidderCounter;//<HouseholdId,Count>: used to count how many times households fail to get offers


    // Temporary stuff
//    static long startTime;
//    static long endTime;
//    static long durationDemo = 0;

    //------------------------//
    //----- Constructors -----//
    //------------------------//

    /**
     * Initialises the region with a sales market, a rental market, and space for storing
     * households
     */
    public Region(int targetPopulation) {
        this.targetPopulation = targetPopulation;
        households = new ArrayList<>(targetPopulation*2);
        houseSaleMarket = new HouseSaleMarket(this);
        houseRentalMarket = new HouseRentalMarket(this);
        regionalHouseholdStats = new RegionalHouseholdStats(this);
        regionalHousingMarketStats = new RegionalHousingMarketStats(houseSaleMarket);
        regionalRentalMarketStats = new RegionalRentalMarketStats(regionalHousingMarketStats, houseRentalMarket);
        failedBidderCounter=new HashMap<Id<Household>,Integer>();
    }
    
    /**
     * @author HiTony: we need a ID for each region in order to make them distinguishable. 
     */
    public Region(int targetPopulation,Id<Region> regionId,Transport transport) {
        this.targetPopulation = targetPopulation;
        households = new ArrayList<>(targetPopulation*2);
        houseSaleMarket = new HouseSaleMarket(this);
        houseRentalMarket = new HouseRentalMarket(this);
        regionalHouseholdStats = new RegionalHouseholdStats(this,transport);
        regionalHousingMarketStats = new RegionalHousingMarketStats(houseSaleMarket);
        regionalRentalMarketStats = new RegionalRentalMarketStats(regionalHousingMarketStats, houseRentalMarket);
        this.regionId=regionId;
    }

    //-------------------//
    //----- Methods -----//
    //-------------------//

    public void init() {
        households.clear();
        failedBidderCounter=new HashMap<Id<Household>,Integer>();
        houseSaleMarket.init();
        houseRentalMarket.init();
        regionalHousingMarketStats.init();
        regionalRentalMarketStats.init();
        regionalHouseholdStats.init();
        housingStock = 0;
    }

//    public void step() {
//        // Updates regional households consumption, housing decisions, and corresponding regional bids and offers
//        for(Household h : households) h.step();
//        // Stores regional sale market bid and offer prices and averages before bids are matched by clearing the market
//        regionalHousingMarketStats.preClearingRecord();
//        // Clears regional sale market and updates the HPI
//        houseSaleMarket.clearMarket();
//        // Computes and stores several regional housing market statistics after bids are matched by clearing the market (such as HPI, HPA)
//        regionalHousingMarketStats.postClearingRecord();
//        // Stores regional rental market bid and offer prices and averages before bids are matched by clearing the market
//        regionalRentalMarketStats.preClearingRecord();
//        // Clears regional rental market
//        houseRentalMarket.clearMarket();
//        // Computes and stores several regional rental market statistics after bids are matched by clearing the market (such as HPI, HPA)
//        regionalRentalMarketStats.postClearingRecord();
//        // Stores regional household statistics after both regional markets have been cleared
//        regionalHouseholdStats.record();
//    }
    
    public void step(ArrayList<Region> geography,Transport transport) {
        // Updates regional households consumption, housing decisions, and corresponding regional bids and offers
        for(Household h : households) h.step(geography,transport);
        // Stores regional sale market bid and offer prices and averages before bids are matched by clearing the market
        regionalHousingMarketStats.preClearingRecord();
        // Clears regional sale market and updates the HPI
        houseSaleMarket.clearMarket();
        // Computes and stores several regional housing market statistics after bids are matched by clearing the market (such as HPI, HPA)
        regionalHousingMarketStats.postClearingRecord();
        // Stores regional rental market bid and offer prices and averages before bids are matched by clearing the market
        regionalRentalMarketStats.preClearingRecord();
        // Clears regional rental market
        houseRentalMarket.clearMarket();
        // Computes and stores several regional rental market statistics after bids are matched by clearing the market (such as HPI, HPA)
        regionalRentalMarketStats.postClearingRecord();
        // Stores regional household statistics after both regional markets have been cleared
        regionalHouseholdStats.record();
    }
    
    //******** Methods for Failed Bidders ******//   
    public Map<Id<Household>,Integer> getFailedBidderCounter(){
    	return this.failedBidderCounter;
    }
    
    public void countFailedBidder(ArrayList<HouseBuyerRecord> failedBids){   
    	for(HouseBuyerRecord failedBid:failedBids){
    		Id<Household> householdId=failedBid.getBuyer().getHouseholdId();
    		countFailedBidder(householdId);    		
    	}
    }
    
    public void countFailedBidder(Id<Household> householdId){   
    	int cnt=0;
    	if(failedBidderCounter.containsKey(householdId)){
    		cnt=failedBidderCounter.get(householdId)+1;
    	}
    	else{
    		cnt=1;
    	}
    	failedBidderCounter.put(householdId, cnt);
    }
    
    public void removeSuccessfulBidder(Id<Household> householdId){
    	if(failedBidderCounter.containsKey(householdId)){
    		failedBidderCounter.remove(householdId);
    	}
    }
    /**
     * it is assuemd that a household will start trying to bid in neighbouring regions after failling to get a offer in its present 
     * region for a specfic number of steps that is maxNumOfStepsInPresentRegion
     * @param householdId
     * @param maxNumOfStepsInPresentRegion: the maximum number of steps that households can bid in the present region
     */
    public boolean decideToTryNeighbouringRegion(Id<Household> householdId,int maxNumOfStepsInPresentRegion){
    	boolean toTry=false;//to try its neighbouringRegion
    	if(this.failedBidderCounter.containsKey(householdId)){
    		int numOfSteps=failedBidderCounter.get(householdId);
    		if(numOfSteps>=maxNumOfStepsInPresentRegion){
    			toTry=true;
    		}
    	}
    	return toTry;
    }

    //----- Getter/setter methods -----//

    public int getTargetPopulation() { return targetPopulation; }

    public int getHousingStock() { return housingStock; }

    void increaseHousingStock () { housingStock++; }
    
    public Id<Region> getRegionId() {return regionId;}
    
    public void setRegionId(Id<Region> regionId) {this.regionId=regionId;}
}
