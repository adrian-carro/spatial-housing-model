package housing;

import collectors.RegionalHouseholdStats;
import collectors.RegionalHousingMarketStats;
import collectors.RegionalRentalMarketStats;

import org.apache.commons.math3.random.MersenneTwister;

import java.util.ArrayList;

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

    public ArrayList<Household>             households;
    public RegionalHouseholdStats           regionalHouseholdStats;
    public RegionalHousingMarketStats       regionalHousingMarketStats;
    public RegionalRentalMarketStats        regionalRentalMarketStats;

    HouseSaleMarket                         houseSaleMarket;
    HouseRentalMarket                       houseRentalMarket;
    int                                     targetPopulation;

    private int                             regionID;
    private int                             housingStock;

    //------------------------//
    //----- Constructors -----//
    //------------------------//

    /**
     * Constructs the region with a sales market, a rental market, and space for storing households
     */
    public Region(Config config, MersenneTwister rand, int targetPopulation, int regionID) {
        this.targetPopulation = targetPopulation;
        this.regionID = regionID;
        households = new ArrayList<>(targetPopulation*2);
        houseSaleMarket = new HouseSaleMarket(config, rand, this);
        houseRentalMarket = new HouseRentalMarket(config, rand, this);
        regionalHouseholdStats = new RegionalHouseholdStats(config, this);
        regionalHousingMarketStats = new RegionalHousingMarketStats(config, houseSaleMarket);
        regionalRentalMarketStats = new RegionalRentalMarketStats(config, regionalHousingMarketStats,
                                                                  houseRentalMarket);
    }

    //-------------------//
    //----- Methods -----//
    //-------------------//

    /**
     * Initialises the region by clearing the array of households and initialising the other internal variables
     */
    public void init() {
        housingStock = 0;
        households.clear();
        houseSaleMarket.init();
        houseRentalMarket.init();
        regionalHouseholdStats.init();
        regionalHousingMarketStats.init();
        regionalRentalMarketStats.init();
    }

    /**
     * One of the two main methods of the class: loops through the households updating their bids
     */
    void stepHouseholds() {
        // Update regional households' consumption, housing decisions, and corresponding regional bids and offers
        for (Household h : households) h.step();
    }

    /**
     * One of the two main methods of the class: clears both markets, recording data as appropriate
     */
    void stepMarkets() {
        // Store regional sale market bid and offer prices and averages before bids are matched by clearing the market
        regionalHousingMarketStats.preClearingRecord();
        // Clear regional sale market and updates the HPI
        houseSaleMarket.clearMarket();
        // Compute and stores several regional housing market statistics after bids are matched by clearing the market (such as HPI, HPA)
        regionalHousingMarketStats.postClearingRecord();
        // Store regional rental market bid and offer prices and averages before bids are matched by clearing the market
        regionalRentalMarketStats.preClearingRecord();
        // Clear regional rental market
        houseRentalMarket.clearMarket();
        // Compute and stores several regional rental market statistics after bids are matched by clearing the market (such as HPI, HPA)
        regionalRentalMarketStats.postClearingRecord();
        // Store regional household statistics after both regional markets have been cleared
        regionalHouseholdStats.record();
    }

    //----- Getter/setter methods -----//

    int getTargetPopulation() { return targetPopulation; }

    public int getHousingStock() { return housingStock; }

    void increaseHousingStock () { housingStock++; }
    
    int getRegionID() { return regionID; }
}
