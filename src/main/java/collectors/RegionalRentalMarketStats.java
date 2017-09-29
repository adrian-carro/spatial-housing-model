package collectors;

import housing.*;

import java.util.Arrays;

/**************************************************************************************************
 * Class to collect regional rental market statistics
 *
 * @author Adrian Carro
 * @since 06/09/2017
 *
 *************************************************************************************************/
public class RegionalRentalMarketStats extends RegionalHousingMarketStats {

    //------------------//
    //----- Fields -----//
    //------------------//

    // General fields
    private RegionalHousingMarketStats  regHousingMarketStats;
    private Config                      config = Model.config; // Passes the Model's configuration parameters object to a private field

    // Rental-specific variables computed during market clearing, counters
    private double []                   sumMonthsOnMarketPerQualityCount; // Dummy counter

    // Rental-specific variables computed after market clearing to keep the previous values during the clearing
    private double []                   sumMonthsOnMarketPerQuality; // Sum of the months on market for each quality band for properties rented this month
    private double []                   expAvMonthsOnMarketPerQuality; // Exponential moving average of the months on market for each quality band
    private double []                   avOccupancyPerQuality; // Average fraction of time a rental property stays rented for each quality band
    private double []                   avFlowYieldPerQuality; // Average gross rental yield for each quality band for properties rented out this month
    private double                      avFlowYield; // Average gross rental yield for properties rented out this month
    private double                      expAvFlowYield; // Exponential moving average (fast decay) of the average flow gross rental yield
    private double                      longTermExpAvFlowYield; // Exponential moving average (slow decay) of the average flow gross rental yield

    //------------------------//
    //----- Constructors -----//
    //------------------------//

    /**
     * Initialises the regional rental market statistics collector
     *
     * @param region Reference to the region owning both the market and the regional collector
     */
    public RegionalRentalMarketStats(Region region) {
        super(region);
        setActive(true);
        this.regHousingMarketStats = region.regionalHousingMarketStats;
    }

    //-------------------//
    //----- Methods -----//
    //-------------------//

    //----- Rental-specific initialisation methods -----//

    /**
     * This method extends the corresponding one at the RegionalHousingMarketStats class with some rental-specific
     * variables. Sets initial values for all relevant variables to enforce a controlled first measure for statistics
     */
    @Override
    public void init() {
        super.init();
        // Set initial value for all rental specific variables
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

    //----- Rental-specific pre-market-clearing methods -----//

    /**
     * This method extends the corresponding one at the RegionalHousingMarketStats class with some rental-specific
     * variables. Computes pre-clearing statistics and resets counters to zero.
     */
    @Override
    public void preClearingRecord() {
        super.preClearingRecord();
        // Re-initialise to zero variables to be computed later on, during market clearing, counters
        sumMonthsOnMarketPerQualityCount = new double[config.N_QUALITY];
    }

    //----- Rental-specific during-market-clearing methods -----//

    /**
     * This method extends the corresponding one at the RegionalHousingMarketStats class with some rental-specific
     * variables. Updates the values of several counters every time a buyer and a seller are matched and the transaction
     * is completed. Note that only counter variables can be modified within this method
     *
     * @param sale The HouseSaleRecord of the house being sold
     */
    @Override
    public void recordTransaction(HouseSaleRecord sale) {
        super.recordTransaction(sale);
        sumMonthsOnMarketPerQualityCount[sale.getQuality()] += (Model.getTime() - sale.tInitialListing);
    }

    //----- Post-market-clearing methods -----//

    /**
     * This method extends the corresponding one at the RegionalHousingMarketStats class with some rental-specific
     * variables. Updates several statistic records after bids have been matched by clearing the market.
     */
    @Override
    public void postClearingRecord() {
        super.postClearingRecord();
        // Pass count value obtained during market clearing to persistent variables
        System.arraycopy(sumMonthsOnMarketPerQualityCount, 0, sumMonthsOnMarketPerQuality, 0, config.N_QUALITY);
        // Compute the rest of variables after market clearing...
        avFlowYield = 0;
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
                    *avOccupancyPerQuality[q]/regHousingMarketStats.getAvSalePriceForQuality(q);
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

    // Note that, for security reasons, getters should never give or use counter variables, as their value changes
    // during market clearing

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
