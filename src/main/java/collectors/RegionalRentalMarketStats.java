package collectors;

import housing.*;

import java.awt.geom.Point2D;

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
    private Config              config = Model.config; // Passes the Model's configuration parameters object to a private field
    private HouseRentalMarket   market;

    // Rental-specific variables computed during market clearing, counters
    private double []           sumMonthsOnMarketPerQualityCount; // Dummy counter

    // Rental-specific variables computed after market clearing to keep the previous values during the clearing
    private double []           sumMonthsOnMarketPerQuality; // Normal average of the price for each quality band for properties sold this month

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
        this.market = region.houseRentalMarket;
    }

    //-------------------//
    //----- Methods -----//
    //-------------------//

    //----- Rental-specific pre-market-clearing methods -----//

    /**
     * This method extends the corresponding one at the RegionalHousingMarketStats class with some rental-specific
     * variables. Computes pre-clearing statistics and resets counters to zero.
     */
    @Override
    public void preClearingRecord() {
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
        // TODO: Also on Regional- and HousingMarketStats, it should be months, not days being counted!
        sumMonthsOnMarketPerQualityCount[sale.getQuality()] += (Model.getTime() - sale.tInitialListing);
    }

    //----- Post-market-clearing methods -----//

    /**
     * This method extends the corresponding one at the RegionalHousingMarketStats class with some rental-specific
     * variables. Updates several statistic records after bids have been matched by clearing the market.
     */
    public void postClearingRecord() {
        super.postClearingRecord();
        // Pass count value obtained during market clearing to persistent variables
        for (int q = 0; q < config.N_QUALITY; q++) {
            sumMonthsOnMarketPerQuality[q] = sumMonthsOnMarketPerQualityCount[q];
        }
    }

    //----- Getter/setter methods -----//

    // Note that, for security reasons, getters should never give or use counter variables, as their value changes
    // during market clearing

    // Getters for derived variables

    /**
     * Computes the fraction of time that a house of a given quality is expected to be occupied, based on the average
     * tenancy length in months and the average number of months that houses of this quality are currently spending on
     * the rental market
     *
     * @param quality Quality of the house
     */
    public double getExpectedOccupancyForQuality(int quality) {
        return config.AVERAGE_TENANCY_LENGTH/(config.AVERAGE_TENANCY_LENGTH
                + sumMonthsOnMarketPerQuality[quality]/getnSalesForQuality(quality));
    }

    public double getExpectedGrossYieldForQuality(int quality) {
        return getAvSalePriceForQuality(quality)*config.constants.MONTHS_IN_YEAR*getExpectedOccupancyForQuality(quality)
                /market.getAverageSalePrice(quality);
    }

    public double getAverageSoldGrossYield() {
        return(market.averageSoldGrossYield);
    }
}
