package collectors;

import housing.*;

import java.awt.geom.Point2D;

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

    public double       sumBidPrices;
    public double       sumOfferPrices;
    public int          nSales, saleCount;
    public int	        nFTBSales, ftbSaleCount;	  // number of sales to first-time-buyers
    public int	        nBTLSales, btlSaleCount;	  // number of sales to first-time-buyers
    public int          nBuyers;
    public int          nSellers;
    public int 	        nNewBuild;
    public double [][]  priceData;
    public int 	        nEmpty;
    double []           offerPrices;
    double []           bidPrices;
    HouseSaleMarket     market;

    private Config config = Model.config; // Passes the Model's configuration parameters object to a private field

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
        priceData = new double[2][config.N_QUALITY];
        for(int i=0; i<config.N_QUALITY; ++i) {
            priceData[0][i] = market.referencePrice(i);
        }
    }

    //-------------------//
    //----- Methods -----//
    //-------------------//

    /**
     * Sets all relevant variables to zero to enforce a correct first measure even if record is not called at time 0
     */
    public void init() {
        saleCount = 0;
        ftbSaleCount = 0;
        btlSaleCount = 0;
        nFTBSales = 0;
        nBTLSales = 0;
        nSales = 0;
        nBuyers = 0;
        nSellers = 0;
        nNewBuild = 0;
        nEmpty = 0;
        for(int i=0; i<config.N_QUALITY; ++i) {
            priceData[1][i] = 0;
        }
    }

    public void record() {
        nSales = saleCount; saleCount = 0;
        nFTBSales = ftbSaleCount; ftbSaleCount = 0;
        nBTLSales = btlSaleCount; btlSaleCount = 0;
        nNewBuild = 0;
        nEmpty = 0;
        nSellers = market.getOffersPQ().size();
        nBuyers = market.getBids().size();

        // -- Record average bid price
        // ---------------------------
        sumBidPrices = 0.0;
        for(HouseBuyerRecord buyer : market.getBids()) {
            sumBidPrices += buyer.getPrice();
        }

        // -- Record average offer price
        // -----------------------------
        sumOfferPrices = 0.0;
        for(HousingMarketRecord sale : market.getOffersPQ()) {
            sumOfferPrices += sale.getPrice();
            if(((HouseSaleRecord)sale).house.owner == Model.construction) nNewBuild++;
            if(((HouseSaleRecord)sale).house.resident == null) nEmpty++;
        }
        recordOfferPrices();
        recordBidPrices();
    }

    public void recordSale(HouseBuyerRecord purchase, HouseSaleRecord sale) {
        saleCount += 1;
        MortgageAgreement mortgage = purchase.buyer.mortgageFor(sale.house);
        if(mortgage != null) {
            if(mortgage.isFirstTimeBuyer) {
                ftbSaleCount += 1;
            } else if(mortgage.isBuyToLet) {
                btlSaleCount += 1;
            }
        }
        Model.transactionRecorder.recordSale(purchase, sale, mortgage, market);
    }

    private void recordOfferPrices() {
        offerPrices = new double[nSellers];
        int i = 0;
        for(HousingMarketRecord sale : market.getOffersPQ()) {
            offerPrices[i] = sale.getPrice();
            ++i;
        }
    }

    private void recordBidPrices() {
        bidPrices = new double[nBuyers];
        int i = 0;

        for(HouseBuyerRecord bid : market.getBids()) {
            bidPrices[i] = bid.getPrice();
            ++i;
        }
    }

    public double [][] priceData() {
        // Simply copies the regional market's averageSalePrice array into the priceData array
        System.arraycopy(market.getAverageSalePrice(), 0, priceData[1], 0, config.N_QUALITY);
        return priceData;
    }

    ///////////////////////////////////////////////////////////////////////////////////////
    // Getters and setters for Mason
    ///////////////////////////////////////////////////////////////////////////////////////

    public double getAverageDaysOnMarket() {
        return market.averageDaysOnMarket;
    }

    public double[] getOfferPrices() {
        return(offerPrices);
    }

    public double[] getBidPrices() {
        return(bidPrices);
    }

    public double getAverageBidPrice() {
        if(nBuyers > 0) {
            return sumBidPrices/nBuyers;
        } else {
            return 0.0;
        }
    }

    public double getAverageOfferPrice() {
        if(nSellers > 0) {
            return sumOfferPrices/nSellers;
        } else {
            return 0.0;
        }
    }

    public int getnSales() {
        return nSales;
    }

    public int getnNewBuild() {
        return nNewBuild;
    }

    public int getnBuyers() {
        return nBuyers;
    }

    public int getNSellers() {
        return nSellers;
    }

    // Proportion of monthly sales that are to First-time-buyers
    public double getFTBSalesProportion() {
        return nFTBSales/(nSales+1e-8);
    }

    // Proportion of monthly sales that are to Buy-to-let investors
    public double getBTLSalesProportion() {
        return nBTLSales/(nSales+1e-8);
    }

    //House price growth year-on-year
    public double getHPA() {
        return(market.housePriceAppreciation(1));
    }

    public double getHPI() {
        return(market.housePriceIndex);
    }

    public double getnEmpty() {
        return nEmpty;
    }
}
