package housing;

import collectors.Collectors;
import collectors.MicroDataRecorder;
import collectors.Recorder;
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

    public static HouseSaleMarket       houseSaleMarket;
    public static HouseRentalMarket     houseRentalMarket;
    public static ArrayList<Household>  households;

    private Config                      config = Model.config; // Passes the Model's configuration parameters object to a private field
    private MersenneTwister             rand = Model.rand; // Passes the Model's random number generator to a private field

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
    public Region(int expectedPopulation) {
        households = new ArrayList<>(expectedPopulation*2);
        houseSaleMarket = new HouseSaleMarket();
        houseRentalMarket = new HouseRentalMarket();
    }

    //-------------------//
    //----- Methods -----//
    //-------------------//

    public static void init() {
        houseSaleMarket.init();
        houseRentalMarket.init();
        households.clear();
    }

    public static void step() {
        for(Household h : households) h.step();
        // TODO: Add regional collector and national collector (or aggregate nationally somehow...)
        // Stores ownership market bid and offer prices, and their averages, into their respective variables
        //collectors.housingMarketStats.record();
        // Clears market and updates the HPI
        houseSaleMarket.clearMarket();
        // Stores rental market bid and offer prices, and their averages, into their respective variables
        //collectors.rentalMarketStats.record();
        houseRentalMarket.clearMarket();
    }
}
