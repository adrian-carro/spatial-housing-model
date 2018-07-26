package housing;

import collectors.RegionalHouseholdStats;
import collectors.RegionalHousingMarketStats;
import collectors.RegionalRentalMarketStats;
import org.apache.commons.math3.random.MersenneTwister;

import utilities.PriorityQueue2D;

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
    public int                          regionID;
    private int                         housingStock;

    PriorityQueue2D<RegionQualityRecord>    regionsPQNewForSale; // PriorityQueue2D of regions (ordered by price and FSale)
    PriorityQueue2D<RegionQualityRecord>    regionsPQNewForRent; // PriorityQueue2D of regions (ordered by price and FRent)

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

        regionsPQNewForSale = new PriorityQueue2D<>(new RegionQualityRecord.PFComparator()); // Comparator based on price and F
        regionsPQNewForRent = new PriorityQueue2D<>(new RegionQualityRecord.PFComparator()); // Comparator based on price and F
    }

    //-------------------//
    //----- Methods -----//
    //-------------------//

    /**
     * Initialises the region by clearing the array of households and initialising the other internal variables
     */
    public void init() {
        households.clear();
        houseSaleMarket.init();
        houseRentalMarket.init();
        regionalHousingMarketStats.init();
        regionalRentalMarketStats.init();
        regionalHouseholdStats.init();
        housingStock = 0;
    }

    /**
     * Main method of the class: loops through the households updating their bids and then clears both markets,
     * recording data as appropriate
     */
    public void step() {
        // Updates regional households consumption, housing decisions, and corresponding regional bids and offers
        for (Household h : households) h.step();
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

    //----- Getter/setter methods -----//

    int getTargetPopulation() { return targetPopulation; }

    public int getHousingStock() { return housingStock; }

    void increaseHousingStock () { housingStock++; }
    
    public int getRegionID() {return regionID;}
}
