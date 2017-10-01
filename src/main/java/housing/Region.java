package housing;

import collectors.RegionalHouseholdStats;
import collectors.RegionalHousingMarketStats;
import collectors.RegionalRentalMarketStats;

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

    public ArrayList<Household>         households;
    public HouseSaleMarket              houseSaleMarket;
    public HouseRentalMarket            houseRentalMarket;
    public RegionalHouseholdStats       regionalHouseholdStats;
    public RegionalHousingMarketStats   regionalHousingMarketStats;
    public RegionalRentalMarketStats    regionalRentalMarketStats;
    public int                          targetPopulation;
    public int                          housingStock;

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
        regionalHousingMarketStats = new RegionalHousingMarketStats(this);
        regionalRentalMarketStats = new RegionalRentalMarketStats(this);
    }

    //-------------------//
    //----- Methods -----//
    //-------------------//

    public void init() {
        households.clear();
        houseSaleMarket.init();
        houseRentalMarket.init();
        regionalHousingMarketStats.init();
        regionalRentalMarketStats.init();
        regionalHouseholdStats.init();
        housingStock = 0;
    }

    public void step() {
        // Updates regional households consumption, housing decisions, and corresponding regional bids and offers
        for(Household h : households) h.step();
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
}
