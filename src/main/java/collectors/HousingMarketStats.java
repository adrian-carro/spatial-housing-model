package collectors;

import housing.*;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import javax.jws.WebParam;
import java.util.ArrayList;

/**************************************************************************************************
 * Class to aggregate all regional sale market statistics
 *
 * @author daniel, Adrian Carro
 * @since 16/09/2017
 *
 *************************************************************************************************/
public class HousingMarketStats extends CollectorBase {
    private static final long serialVersionUID = -535310555732796139L;

    //------------------//
    //----- Fields -----//
    //------------------//

    // General fields
    private ArrayList<Region>       geography;
    private Config                  config = Model.config; // Passes the Model's configuration parameters object to a private field

    // Variables computed at initialisation
    private double []               referencePricePerQuality;

    // Variables computed before market clearing
    int                     nBuyers;
    int                     nSellers;
    double                  sumBidPrices;
    double                  sumOfferPrices;
    double []               offerPrices;
    double []               bidPrices;

    // Variables computed after market clearing to keep the previous values during the clearing
    int                     nSales; // Number of sales
    private int	                    nFTBSales; // Number of sales to first-time buyers
    private int	                    nBTLSales; // Number of sales to buy-to-let investors
    int 	                nUnsoldNewBuild; // Accumulated number of new built properties still unsold after market clearing
    double                  sumSoldReferencePrice; // Sum of reference prices for the qualities of properties sold this month
    double                  sumSoldPrice; // Sum of prices of properties sold this month
    double                  sumDaysOnMarket; // Normal average of the number of days on the market for properties sold this month
    double []               sumSalePricePerQuality; // Normal average of the price for each quality band for properties sold this month
    int []                  nSalesPerQuality; // Number of sales for each quality band for properties sold this month

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
     * Initialises the national sale market statistics collector
     *
     * @param geography Reference to the whole geography of regions
     */
    public HousingMarketStats(ArrayList<Region> geography) {
        setActive(true);
        this.geography = geography;
        referencePricePerQuality = new double[config.N_QUALITY];
        System.arraycopy(data.HouseSaleMarket.getReferencePricePerQuality(), 0, referencePricePerQuality, 0,
                config.N_QUALITY); // Copies reference prices from data/HouseSaleMarket into referencePricePerQuality
        HPIRecord = new DescriptiveStatistics(config.derivedParams.HPI_RECORD_LENGTH);
    }

    //-------------------//
    //----- Methods -----//
    //-------------------//

    /**
     * Sets initial values for all relevant variables to enforce a controlled first measure for statistics
     */
    public void init() {
        // Set zero initial value for variables computed (regionally) before market clearing
        nBuyers = 0;
        nSellers = 0;
        nUnsoldNewBuild = 0;
        sumBidPrices = 0.0;
        sumOfferPrices = 0.0;
        offerPrices = new double[nSellers];
        bidPrices = new double[nBuyers];

        // Set zero initial value for persistent variables whose count is computed (regionally) during market clearing
        nSales = 0;
        nFTBSales = 0;
        nBTLSales = 0;
        sumSoldReferencePrice = 0;
        sumSoldPrice = 0;
        sumDaysOnMarket = 0;
        sumSalePricePerQuality = new double[config.N_QUALITY];
        nSalesPerQuality = new int[config.N_QUALITY];

        // Set initial values for other variables computed (regionally) after market clearing
        expAvDaysOnMarket = config.constants.DAYS_IN_MONTH; // TODO: Make this initialisation explicit in the paper! Is 30 days similar to the final simulated value?
        expAvSalePricePerQuality = new double[config.N_QUALITY];
        System.arraycopy(referencePricePerQuality, 0, expAvSalePricePerQuality, 0,
                config.N_QUALITY); // Exponential averaging of prices is initialised from reference prices
        housePriceIndex = 1.0;
        for (int i = 0; i < config.derivedParams.HPI_RECORD_LENGTH; ++i) HPIRecord.addValue(1.0);
        annualHousePriceAppreciation = housePriceAppreciation(1);
        longTermHousePriceAppreciation = housePriceAppreciation(config.HPA_YEARS_TO_CHECK);
    }

    /**
     * Collects current values, apart from updating those to be computed, for all relevant variables from the regional
     * housing market statistics objects
     */
    public void collectRegionalRecords() {
        // Re-initiate to zero variables to sum over regions
        nBuyers = 0;
        nSellers = 0;
        nUnsoldNewBuild = 0;
        sumBidPrices = 0.0;
        sumOfferPrices = 0.0;
        nSales = 0;
        nFTBSales = 0;
        nBTLSales = 0;
        sumSoldReferencePrice = 0;
        sumSoldPrice = 0;
        sumDaysOnMarket = 0;
        sumSalePricePerQuality = new double[config.N_QUALITY];
        nSalesPerQuality = new int[config.N_QUALITY];

        // Run through regions summing
        runThroughRegionsSumming();

        // Once we have total nSellers and nBuyers, we can allocate and collect offerPrices and bidPrices arrays
        offerPrices = new double[nSellers];
        bidPrices = new double[nBuyers];
        // TODO: Check efficiency of methods 1 and 2 and decide for one or the other.
        // METHOD 1
        // Run through regions collecting regional offer and bid prices arrays into corresponding national arrays
//        int i = 0;
//        int j = 0;
//        for (Region region: geography) {
//            System.arraycopy(region.regionalHousingMarketStats.offerPrices, 0, offerPrices, i,
//                    i + region.regionalHousingMarketStats.nSellers); // Copies regional offerPrices into national offerPrices
//            System.arraycopy(region.regionalHousingMarketStats.bidPrices, 0, bidPrices, j,
//                    j + region.regionalHousingMarketStats.nBuyers); // Copies regional bidPrices into national bidPrices
//            i += region.regionalHousingMarketStats.nSellers;
//            j += region.regionalHousingMarketStats.nBuyers;
//        }
        // METHOD 2
        collectOfferPrices();
        collectBidPrices();

        // Compute all derived variables...
        // ... exponential averages of days in the market and prices per quality band (only if there have been sales)
        if (nSales > 0) {
            expAvDaysOnMarket = config.derivedParams.E*expAvDaysOnMarket
                    + (1.0 - config.derivedParams.E)*sumDaysOnMarket/nSales;
        }
        for (int q = 0; q < config.N_QUALITY; q++) {
            if (nSalesPerQuality[q] > 0) {
                expAvSalePricePerQuality[q] = config.derivedParams.G*expAvSalePricePerQuality[q]
                        + (1.0 - config.derivedParams.G)*sumSalePricePerQuality[q]/nSalesPerQuality[q];
            }
        }
        // ... current house price index (only if there have been sales)
        if(nSales > 0) {
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
                    + (1.0 - config.MARKET_AVERAGE_PRICE_DECAY)*(housePriceIndex*referencePricePerQuality[q]);
        }
    }

    /**
     * Runs through the regions aggregating regional results. This operation has been extracted from method
     * collectRegionalRecords so as to allow for it to be overridden at RentalMarketStats
     * Note: To be overridden at RentalMarketStats
     */
    void runThroughRegionsSumming() {
        // Run through regions summing
        for (Region region : geography) {
            nBuyers += region.regionalHousingMarketStats.getnBuyers();
            nSellers += region.regionalHousingMarketStats.getnSellers();
            nUnsoldNewBuild += region.regionalHousingMarketStats.getnUnsoldNewBuild();
            sumBidPrices += region.regionalHousingMarketStats.getSumBidPrices();
            sumOfferPrices += region.regionalHousingMarketStats.getSumOfferPrices();
            nSales += region.regionalHousingMarketStats.getnSales();
            nFTBSales += region.regionalHousingMarketStats.getnFTBSales();
            nBTLSales += region.regionalHousingMarketStats.getnBTLSales();
            sumSoldReferencePrice += region.regionalHousingMarketStats.getSumSoldReferencePrice();
            sumSoldPrice += region.regionalHousingMarketStats.getSumSoldPrice();
            sumDaysOnMarket += region.regionalHousingMarketStats.getSumDaysOnMarket();
            for (int q = 0; q < config.N_QUALITY; q++) {
                sumSalePricePerQuality[q] += region.regionalHousingMarketStats.getSumSalePriceForQuality(q);
                nSalesPerQuality[q] += region.regionalHousingMarketStats.getnSalesForQuality(q);
            }
        }
    }

    /**
     * Collects all offer prices from the regional housing market statistics objects
     * Note: To be overridden at RentalMarketStats
     */
	void collectOfferPrices() {
		int i = 0;
		for (Region region: geography) {
		    for (double price: region.regionalHousingMarketStats.getOfferPrices()) {
                offerPrices[i] = price;
                ++i;
            }
        }
	}

    /**
     * Collects all bid prices from the regional housing market statistics objects
     * Note: To be overridden at RentalMarketStats
     */
	void collectBidPrices() {
		int i = 0;
        for (Region region: geography) {
            for(double price: region.regionalHousingMarketStats.getBidPrices()) {
                bidPrices[i] = price;
                ++i;
            }
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

    /**
     * This method computes the quarter on quarter appreciation in house price index by comparing the most recent
     * quarter (previous 3 months, to smooth changes) to the previous one and computing the percentage change
     *
     * @return Quarter on quarter house price growth
     */
    public double getQoQHousePriceGrowth() {
        double HPI = HPIRecord.getElement(config.derivedParams.getHPIRecordLength() - 1)
                + HPIRecord.getElement(config.derivedParams.getHPIRecordLength() - 2)
                + HPIRecord.getElement(config.derivedParams.getHPIRecordLength() - 3);
        double oldHPI = HPIRecord.getElement(config.derivedParams.getHPIRecordLength() - 4)
                + HPIRecord.getElement(config.derivedParams.getHPIRecordLength() - 5)
                + HPIRecord.getElement(config.derivedParams.getHPIRecordLength() - 6);
        return(100.0*(HPI - oldHPI)/oldHPI);
    }

    //----- Getter/setter methods -----//

    // Getters for variables computed at initialisation
    public double [] getReferencePricePerQuality() { return referencePricePerQuality; }

    // Getters for variables computed before market clearing
    public int getnBuyers() { return nBuyers; }
    public int getnSellers() { return nSellers; }
    public int getnUnsoldNewBuild() { return nUnsoldNewBuild; }
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
    public double getSumDaysOnMarket() { return sumDaysOnMarket; }
    public double [] getSumSalePricePerQuality() { return sumSalePricePerQuality; }
    public double getSumSalePriceForQuality(int quality) { return sumSalePricePerQuality[quality]; }
    public int [] getnSalesPerQuality() { return nSalesPerQuality; }
    public int getnSalesForQuality(int quality) { return nSalesPerQuality[quality]; }

    // Getters for other variables computed after market clearing
    public double getExpAvDaysOnMarket() { return expAvDaysOnMarket; }
    public double [] getExpAvSalePricePerQuality() { return expAvSalePricePerQuality; }
    public double getHPI() { return housePriceIndex; }
    public DescriptiveStatistics getHPIRecord() { return HPIRecord; }
    public double getAnnualHPA() { return annualHousePriceAppreciation; }
    public double getLongTermHPA() {return longTermHousePriceAppreciation; }

    // Getters for derived variables
    public double getAvBidPrice() {
        if (nBuyers > 0) {
            return sumBidPrices/nBuyers;
        } else {
            return 0.0;
        }
    }
    public double getAvOfferPrice() {
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
    public double getAvDaysOnMarket() {
        if (nSales > 0) {
            return sumDaysOnMarket/nSales;
        } else {
            return 0.0;
        }
    }
    public double [] getAvSalePricePerQuality() {
        double [] avSalePricePerQuality;
        avSalePricePerQuality = new double[config.N_QUALITY];
        for (int q = 0; q < config.N_QUALITY; q++) {
            if (nSalesPerQuality[q] > 0) {
                avSalePricePerQuality[q] = sumSalePricePerQuality[q]/nSalesPerQuality[q];
            } else {
                avSalePricePerQuality[q] = 0.0;
            }
        }
        return avSalePricePerQuality;
    }

    public double getAvSalePriceForQuality(int quality) {
        if (nSalesPerQuality[quality] > 0) {
            return sumSalePricePerQuality[quality]/nSalesPerQuality[quality];
        } else {
            return 0.0;
        }
    }

}
