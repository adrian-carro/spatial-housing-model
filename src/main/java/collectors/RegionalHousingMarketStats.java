package collectors;

import housing.*;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

/**************************************************************************************************
 * Class to collect regional sale market statistics
 *
 * @author daniel, Adrian Carro
 * @since 06/09/2017
 *
 *************************************************************************************************/
public class RegionalHousingMarketStats extends CollectorBase {

    //------------------//
    //----- Fields -----//
    //------------------//

    // General fields
    private HouseSaleMarket         market;
    private Config                  config = Model.config; // Passes the Model's configuration parameters object to a private field

    // Variables computed at initialisation
    private double []               referencePricePerQuality;

    // Variables computed before market clearing
    private int                     nBuyers;
    private int                     nSellers;
    private int 	                nNewBuild;
    private int 	                nEmpty;
    private double                  sumBidPrices;
    private double                  sumOfferPrices;
    private double []               offerPrices;
    private double []               bidPrices;

    // Variables computed during market clearing, counters
    private int                     salesCount; // Dummy variable to count sales
    private int                     ftbSalesCount; // Dummy variable to count sales to first-time buyers
    private int                     btlSalesCount; // Dummy variable to count sales to buy-to-let investors
    private double                  sumSoldReferencePriceCount; // Dummy counter
    private double                  sumSoldPriceCount; // Dummy counter
    private double                  avDaysOnMarketCount; // Dummy counter
    private double []               avSalePricePerQualityCount; // Dummy counter
    private double []               nSalesPerQualityCount; // Dummy counter (only used for computing avSalePricePerQuality)

    // Variables computed after market clearing to keep the previous values during the clearing
    private int                     nSales; // Number of sales
    private int	                    nFTBSales; // Number of sales to first-time buyers
    private int	                    nBTLSales; // Number of sales to buy-to-let investors
    private double                  sumSoldReferencePrice; // Sum of reference prices for the qualities of properties sold this month
    private double                  sumSoldPrice; // Sum of prices of properties sold this month
    private double                  avDaysOnMarket; // Normal average of the number of days on the market for properties sold this month
    private double []               avSalePricePerQuality; // Normal average of the price for each quality band for properties sold this month

    // Other variables computed after market clearing
    private double                  expAvDaysOnMarket; // Exponential moving average of the number of days on the market
    private double []               expAvSalePricePerQuality; // Exponential moving average of the price for each quality band
    private double                  housePriceIndex;
    private DescriptiveStatistics   HPIRecord;
    private double                  annualHousePriceAppreciation;
    private double                  longTermHousePriceAppreciation;


    //------------------------//
    //----- Constructors -----//
    //------------------------//

    /**
     * Initialises the regional sale market statistics collector
     *
     * @param region Reference to the region owning both the market and the regional collector
     */
    public RegionalHousingMarketStats(Region region) {
        setActive(true);
        this.market = region.houseSaleMarket;
        referencePricePerQuality = new double[config.N_QUALITY];
        // TODO: Attention, this is passing the national reference prices for each region! Each region should have its own!
        System.arraycopy(data.HouseSaleMarket.getReferencePricePerQuality(), 0, referencePricePerQuality, 0,
                config.N_QUALITY); // Copies reference prices from data/HouseSaleMarket into referencePricePerQuality
        HPIRecord = new DescriptiveStatistics(config.derivedParams.HPI_RECORD_LENGTH);
    }

    //-------------------//
    //----- Methods -----//
    //-------------------//

    //----- Initialisation methods -----//

    /**
     * Sets initial values for all relevant variables to enforce a controlled first measure for statistics
     */
    public void init() {
        // Set zero initial value for variables computed before market clearing
        nBuyers = 0;
        nSellers = 0;
        nNewBuild = 0;
        nEmpty = 0;
        sumBidPrices = 0.0;
        sumOfferPrices = 0.0;
        offerPrices = new double[nSellers];
        bidPrices = new double[nBuyers];

        // Set zero initial value for persistent variables whose count is computed during market clearing
        nSales = 0;
        nFTBSales = 0;
        nBTLSales = 0;
        sumSoldReferencePrice = 0;
        sumSoldPrice = 0;
        avDaysOnMarket = 0;
        avSalePricePerQuality = new double[config.N_QUALITY];

        // Set initial values for other variables computed after market clearing
        expAvDaysOnMarket = config.constants.DAYS_IN_MONTH; // TODO: Make this initialisation explicit in the paper! Is 30 days similar to the final simulated value?
        expAvSalePricePerQuality = new double[config.N_QUALITY];
        System.arraycopy(referencePricePerQuality, 0, expAvSalePricePerQuality, 0,
                config.N_QUALITY); // Exponential averaging of prices is initialised from reference prices
        housePriceIndex = 1.0;
        for (int i = 0; i < config.derivedParams.HPI_RECORD_LENGTH; ++i) HPIRecord.addValue(1.0);
        annualHousePriceAppreciation = housePriceAppreciation(1);
        longTermHousePriceAppreciation = housePriceAppreciation(config.HPA_YEARS_TO_CHECK);
    }

    //----- Pre-market-clearing methods -----//

    /**
     * Computes pre-clearing statistics and resets counters to zero
     */
    public void preClearingRecord() {
        // Re-initialise to zero variables to be computed later on, during market clearing, counters
        salesCount = 0;
        ftbSalesCount = 0;
        btlSalesCount = 0;
        sumSoldReferencePriceCount = 0;
        sumSoldPriceCount = 0;
        avDaysOnMarketCount = 0;
        avSalePricePerQualityCount = new double[config.N_QUALITY];
        nSalesPerQualityCount = new double[config.N_QUALITY];

        // Re-initialise to zero variables computed before market clearing
        nBuyers = market.getBids().size();
        nSellers = market.getOffersPQ().size();
        nNewBuild = 0;
        nEmpty = 0;
        sumBidPrices = 0.0;
        sumOfferPrices = 0.0;
        offerPrices = new double[nSellers];
        bidPrices = new double[nBuyers];


        // Record bid prices and their average
        int i = 0;
        for(HouseBuyerRecord bid : market.getBids()) {
            sumBidPrices += bid.getPrice();
            bidPrices[i] = bid.getPrice();
            ++i;
        }

        // Record offer prices, their average, and the number of empty and new houses
        i = 0;
        for(HousingMarketRecord sale : market.getOffersPQ()) {
            if(((HouseSaleRecord)sale).house.owner == Model.construction) nNewBuild++;
            if(((HouseSaleRecord)sale).house.resident == null) nEmpty++;
            sumOfferPrices += sale.getPrice();
            offerPrices[i] = sale.getPrice();
            ++i;
        }
    }

    //----- During-market-clearing methods -----//

    /**
     * This method updates the values of several counters every time a buyer and a seller are matched and the
     * transaction is completed. Note that only counter variables can be modified within this method
     *
     * @param purchase The HouseBuyerRecord of the buyer
     * @param sale The HouseSaleRecord of the house being sold
     */
    // TODO: Need to think if this method and recordTransaction can be joined in a single method!
    public void recordSale(HouseBuyerRecord purchase, HouseSaleRecord sale) {
        salesCount += 1;
        MortgageAgreement mortgage = purchase.buyer.mortgageFor(sale.house);
        if(mortgage != null) {
            if(mortgage.isFirstTimeBuyer) {
                ftbSalesCount += 1;
            } else if(mortgage.isBuyToLet) {
                btlSalesCount += 1;
            }
        }
        // TODO: Attention, call to model from regional class: need to build regional recorders!
        Model.transactionRecorder.recordSale(purchase, sale, mortgage, market);
    }

    /**
     * This method updates the values of several counters every time a buyer and a seller are matched and the
     * transaction is completed. Note that only counter variables can be modified within this method
     *
     * @param sale The HouseSaleRecord of the house being sold
     */
    public void recordTransaction(HouseSaleRecord sale) {
        avDaysOnMarketCount += config.constants.DAYS_IN_MONTH*(Model.getTime() - sale.tInitialListing);
        avSalePricePerQualityCount[sale.getQuality()] += sale.getPrice();
        nSalesPerQualityCount[sale.getQuality()]++;
        sumSoldReferencePriceCount += referencePricePerQuality[sale.getQuality()];
        sumSoldPriceCount += sale.getPrice();
    }

    //----- Post-market-clearing methods -----//

    /**
     * This method updates several statistic records after bids have been matched by clearing the market. The
     * computation of the HPI is included here. Note that reference prices from data are used for computing the HPI, and
     * thus the value for t=1 is not 1
     */
    public void postClearingRecord() {
        // Pass count value obtained during market clearing to persistent variables
        nSales = salesCount;
        nFTBSales = ftbSalesCount;
        nBTLSales = btlSalesCount;
        sumSoldReferencePrice = sumSoldReferencePriceCount;
        sumSoldPrice = sumSoldPriceCount;
        avDaysOnMarket = avDaysOnMarketCount/nSales;
        for (int q = 0; q < config.N_QUALITY; q++) {
            avSalePricePerQuality[q] = avSalePricePerQualityCount[q]/nSalesPerQualityCount[q];
        }
        // Compute the rest of variables after market clearing...
        // ... exponential averages of days in the market and prices per quality band
        expAvDaysOnMarket = config.derivedParams.E*expAvDaysOnMarket + (1.0 - config.derivedParams.E)*avDaysOnMarket;
        for (int q = 0; q < config.N_QUALITY; q++) {
            expAvSalePricePerQuality[q] = config.derivedParams.G * expAvSalePricePerQuality[q]
                    + (1.0 - config.derivedParams.G)*avSalePricePerQuality[q];
        }
        // ... current house price index (only if there has been any transaction, leading to sumSoldPrice > 0)
        if(sumSoldPrice > 0.0) {
            housePriceIndex = sumSoldPrice/sumSoldReferencePrice;
        }
        // ... HPIRecord with the new house price index value
        HPIRecord.addValue(housePriceIndex);
        // ... current house price appreciation values (both annual and long term value)
        annualHousePriceAppreciation = housePriceAppreciation(1);
        longTermHousePriceAppreciation = housePriceAppreciation(config.HPA_YEARS_TO_CHECK);
        // ... relaxation of the price distribution towards the reference price distribution (described in appendix A3)
        for(int q = 0; q < config.N_QUALITY; q++) {
            expAvSalePricePerQuality[q] = config.MARKET_AVERAGE_PRICE_DECAY*expAvSalePricePerQuality[q]
                    + (1.0-config.MARKET_AVERAGE_PRICE_DECAY)*(housePriceIndex*referencePricePerQuality[q]);
        }
    }

    /**
     * This method computes the annualised appreciation in house price index by comparing the most recent quarter
     * (previous 3 months, to smooth changes) to the quarter nYears years before (full years to avoid seasonal effects)
     * and computing the geometric mean over that period
     *
     * @param nYears Integer with the number of years over which to average house price growth
     * @return Annualised house price appreciation over nYears years
     */
    public double housePriceAppreciation(int nYears) {
        double HPI = (HPIRecord.getElement(config.derivedParams.HPI_RECORD_LENGTH - 1)
                + HPIRecord.getElement(config.derivedParams.HPI_RECORD_LENGTH - 2)
                + HPIRecord.getElement(config.derivedParams.HPI_RECORD_LENGTH - 3));
        double oldHPI = (HPIRecord.getElement(config.derivedParams.HPI_RECORD_LENGTH
                - nYears*config.constants.MONTHS_IN_YEAR - 1)
                + HPIRecord.getElement(config.derivedParams.HPI_RECORD_LENGTH
                - nYears*config.constants.MONTHS_IN_YEAR - 2)
                + HPIRecord.getElement(config.derivedParams.HPI_RECORD_LENGTH
                - nYears*config.constants.MONTHS_IN_YEAR - 3));
        return(Math.pow(HPI/oldHPI, 1.0/nYears) - 1.0);
    }

    //----- Getter/setter methods -----//

    // Note that, for security reasons, getters should never give counter variables, as their value changes during
    // market clearing

    // Getters for variables computed at initialisation
    public double [] getReferencePricePerQuality() { return referencePricePerQuality; }

    // Getters for variables computed before market clearing
    public int getnBuyers() { return nBuyers; }
    public int getnSellers() { return nSellers; }
    public int getnNewBuild() { return nNewBuild; }
    public int getnEmpty() { return nEmpty; }
    public double getSumBidPrices() { return sumBidPrices; }
    public double getSumOfferPrices() { return sumOfferPrices; }
    public double [] getOfferPrices() { return offerPrices; }
    public double [] getBidPrices() { return bidPrices; }

    // Getters for variables computed after market clearing to keep the previous values during the clearing
    public int getnSales() { return nSales; }
    public int getnFTBSales() { return nFTBSales; }
    public int getnBTLSales() { return nBTLSales; }
    public double getSumSoldReferencePrice() { return sumSoldReferencePrice; }
    public double getSumSoldPrice() { return sumSoldPrice; }
    public double getAvDaysOnMarket() { return avDaysOnMarket; }
    public double [] getAvSalePricePerQuality() { return avSalePricePerQuality; }

    // Getters for other variables computed after market clearing
    public double getExpAvDaysOnMarket() { return expAvDaysOnMarket; }
    public double [] getExpAvSalePricePerQuality() { return expAvSalePricePerQuality; }
    public double getHPI() { return housePriceIndex; }
    public DescriptiveStatistics getHPIRecord() { return HPIRecord; }
    public double getAnnualHPA() { return annualHousePriceAppreciation; }
    public double getLongTermHPA() {return longTermHousePriceAppreciation; }

    // Getters for derived variables
    // TODO: Check whether to keep ifs or remove them and always add an infinitesimal amount
    public double getAverageBidPrice() {
        if (nBuyers > 0) {
            return sumBidPrices/nBuyers;
        } else {
            return 0.0;
        }
    }
    public double getAverageOfferPrice() {
        if (nSellers > 0) {
            return sumOfferPrices/nSellers;
        } else {
            return 0.0;
        }
    }
    // Proportion of monthly sales that are to first-time buyers
    public double getFTBSalesProportion() {
        if (nSales > 0) {
            return nFTBSales/nSales;
        } else {
            return 0.0;
        }
    }
    // Proportion of monthly sales that are to buy-to-let investors
    public double getBTLSalesProportion() {
        if (nSales > 0) {
            return nBTLSales/nSales;
        } else {
            return 0.0;
        }
    }
}
