package collectors;

import housing.Config;
import housing.Model;
import housing.Region;

import java.util.ArrayList;
import java.util.Arrays;

/**************************************************************************************************
 * Class to aggregate all regional rental market statistics
 *
 * @author daniel, Adrian Carro
 * @since 16/09/2017
 *
 *************************************************************************************************/

public class RentalMarketStats extends HousingMarketStats {

    //------------------//
    //----- Fields -----//
    //------------------//

    // General fields
    private ArrayList<Region>   geography;
    private HousingMarketStats  housingMarketStats;
    private Config              config = Model.config; // Passes the Model's configuration parameters object to a private field

    // Rental-specific variables computed after market clearing to keep the previous values during the clearing
    private double []           sumMonthsOnMarketPerQuality; // Sum of the months on market for each quality band for properties rented this month
    private double []           expAvMonthsOnMarketPerQuality; // Exponential moving average of the months on market for each quality band
    private double []           avOccupancyPerQuality; // Average fraction of time a rental property stays rented for each quality band
    private double []           avFlowYieldPerQuality; // Average gross rental yield for each quality band for properties rented out this month
    private double              avFlowYield; // Average gross rental yield for properties rented out this month
    private double              expAvFlowYield; // Exponential moving average (fast decay) of the average flow gross rental yield
    private double              longTermExpAvFlowYield; // Exponential moving average (slow decay) of the average flow gross rental yield

    //------------------------//
    //----- Constructors -----//
    //------------------------//

    /**
     * Initialises the national rental market statistics collector
     *
     * @param geography Reference to the whole geography of regions
     */
    public RentalMarketStats(ArrayList<Region> geography) {
        super(geography);
        setActive(true);
        this.geography = geography;
        // TODO: The model's housingMarketStats object should be passed as a parameter or with a setter (in case of mutual dependence)
        this.housingMarketStats = Model.housingMarketStats;
    }

    //-------------------//
    //----- Methods -----//
    //-------------------//

    /**
     * This method extends the corresponding one at the HousingMarketStats class with some rental-specific variables.
     * Sets initial values for all relevant variables to enforce a controlled first measure for statistics
     */
    @Override
    public void init() {
        super.init();
        // Set initial value for all rental-specific variables
        sumMonthsOnMarketPerQuality = new double[config.N_QUALITY];
        expAvMonthsOnMarketPerQuality  = new double[config.N_QUALITY];
        Arrays.fill(expAvMonthsOnMarketPerQuality, 1.0);
        avOccupancyPerQuality = new double[config.N_QUALITY];
        Arrays.fill(avOccupancyPerQuality, 1.0);
        avFlowYieldPerQuality = new double[config.N_QUALITY];
        Arrays.fill(avFlowYieldPerQuality, config.RENT_GROSS_YIELD);
        avFlowYield = config.RENT_GROSS_YIELD;
        expAvFlowYield = config.RENT_GROSS_YIELD;
        longTermExpAvFlowYield = config.RENT_GROSS_YIELD;
    }

    /**
     * This method extends the corresponding one at the HousingMarketStats class with some rental-specific variables.
     * Collects current values, apart from updating those to be computed, for all relevant variables from the regional
     * rental market statistics objects
     */
    @Override
    public void collectRegionalRecords() {
        super.collectRegionalRecords();
        // Re-initiate to zero variables to sum over regions
        sumMonthsOnMarketPerQuality = new double[config.N_QUALITY];
        // Run through regions summing
        for (Region region: geography) {
            for (int q = 0; q < config.N_QUALITY; q++) {
                sumMonthsOnMarketPerQuality[q] += region.regionalRentalMarketStats.getSumMonthsOnMarketForQuality(q);
            }
        }
        // Compute the rest of aggregate variables...
        avFlowYield = 0.0;
        for (int q = 0; q < config.N_QUALITY; q++) {
            // ... exponential average of months in the market per quality band (only if there have been sales)
            if (getnSalesForQuality(q) > 0) {
                expAvMonthsOnMarketPerQuality[q] = config.derivedParams.E*expAvMonthsOnMarketPerQuality[q]
                        + (1.0 - config.derivedParams.E)*sumMonthsOnMarketPerQuality[q]/getnSalesForQuality(q);
            }
            // ... average fraction of time that a house of a given quality is occupied, based on average tenancy length
            // and exponential moving average of months that houses of this quality spend on the rental market
            avOccupancyPerQuality[q] = config.AVERAGE_TENANCY_LENGTH/(config.AVERAGE_TENANCY_LENGTH
                    + expAvMonthsOnMarketPerQuality[q]);
            // ... average flow gross rental yield per quality band
            avFlowYieldPerQuality[q] = getAvSalePriceForQuality(q)*config.constants.MONTHS_IN_YEAR
                    *avOccupancyPerQuality[q]/housingMarketStats.getAvSalePriceForQuality(q);
            // ... average flow gross rental yield (for all quality bands)
            avFlowYield += avFlowYieldPerQuality[q]*getnSalesForQuality(q);
        }
        avFlowYield /= getnSales();
        // ... a short and a long term exponential moving average of the average flow gross rental yield
        expAvFlowYield = expAvFlowYield *config.derivedParams.K + (1.0 - config.derivedParams.K)* avFlowYield;
        longTermExpAvFlowYield = longTermExpAvFlowYield *config.derivedParams.KL
                + (1.0 - config.derivedParams.KL)* avFlowYield;
    }

    //----- Getter/setter methods -----//

    public double [] getSumMonthsOnMarketPerQuality() { return sumMonthsOnMarketPerQuality; }
    public double getSumMonthsOnMarketForQuality(int quality) { return sumMonthsOnMarketPerQuality[quality]; }
    public double [] getExpAvMonthsOnMarketPerQuality() { return expAvMonthsOnMarketPerQuality; }
    public double getExpAvMonthsOnMarketForQuality(int quality) { return expAvMonthsOnMarketPerQuality[quality]; }
    public double [] getAvOccupancyPerQuality() { return avOccupancyPerQuality; }
    public double getAvOccupancyForQuality(int quality) { return avOccupancyPerQuality[quality]; }
    public double [] getAvFlowYieldPerQuality() { return avFlowYieldPerQuality; }
    public double getAvFlowYieldForQuality(int quality) { return avFlowYieldPerQuality[quality]; }
    public double getAvFlowYield() { return avFlowYield; }
    public double getExpAvFlowYield() { return expAvFlowYield; }
    public double getLongTermExpAvFlowYield() { return longTermExpAvFlowYield; }
}
