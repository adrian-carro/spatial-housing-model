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

    public ArrayList<Household>     households;
    public HouseSaleMarket          houseSaleMarket;
    public HouseRentalMarket        houseRentalMarket;
    public int                      targetPopulation;
    public int                      housingStock;

    RegionalHouseholdStats          regionalHouseholdStats;
    RegionalHousingMarketStats      regionalHousingMarketStats;
    RegionalRentalMarketStats       regionalRentalMarketStats;

    private Config                  config = Model.config; // Passes the Model's configuration parameters object to a private field
    private MersenneTwister         rand = Model.rand; // Passes the Model's random number generator to a private field

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
        housingStock = 0;
    }

    public void step() {
        // Updates regional households consumption, housing decisions, and corresponding regional bids and offers
        for(Household h : households) h.step();
        // Stores regional sale market bid and offer prices and averages before bids are matched by clearing the market
        regionalHousingMarketStats.record();
        // Clears regional sale market and updates the HPI
        houseSaleMarket.clearMarket();
        // Stores regional rental market bid and offer prices and averages before bids are matched by clearing the market
        regionalRentalMarketStats.record();
        // Clears regional rental market
        houseRentalMarket.clearMarket();
        // Stores regional household statistics after both regional markets have been cleared
        regionalHouseholdStats.step();
    }
}
