package collectors;

import housing.*;
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

    private Config              config = Model.config; // Passes the Model's configuration parameters object to a private field
    private ArrayList<Region>   geography;

    public double sumBidPrices;
    public double sumOfferPrices;
    public int    nSales, saleCount;
    public int	  nFTBSales, ftbSaleCount;	  // number of sales to first-time-buyers
    public int	  nBTLSales, btlSaleCount;	  // number of sales to first-time-buyers
    public int    nBuyers;
    public int    nSellers;
    public int 	  nNewBuild;
    public double [][]    priceData;
    double [] offerPrices;
    double [] bidPrices;
    HousingMarket market;

    public int 	nEmpty;

    //------------------------//
    //----- Constructors -----//
    //------------------------//


    public HousingMarketStats(ArrayList<Region> geography) {
        setActive(true);
        this.geography = geography;
        priceData = new double[2][config.N_QUALITY];
        for(int i=0; i<config.N_QUALITY; ++i) {
            priceData[0][i] = referencePrice(i);
        }
    }

    //-------------------//
    //----- Methods -----//
    //-------------------//

    public void record() {
        nSales = 0;
        nFTBSales = 0;
        nBTLSales = 0;
        nNewBuild = 0;
        nEmpty = 0;
        nSellers = 0;
        nBuyers = 0;
        sumBidPrices = 0;
        sumOfferPrices = 0;
        for (Region region: geography) {
            // TODO: All this should probably be done via getters
            nSales += region.regionalHousingMarketStats.nSales;
            nFTBSales += region.regionalHousingMarketStats.nFTBSales;
            nBTLSales += region.regionalHousingMarketStats.nBTLSales;
            nNewBuild += region.regionalHousingMarketStats.nNewBuild;
            nEmpty += region.regionalHousingMarketStats.nEmpty;
            nSellers += region.regionalHousingMarketStats.nSellers;
            nBuyers += region.regionalHousingMarketStats.nBuyers;
            sumBidPrices += region.regionalHousingMarketStats.sumBidPrices;
            sumOfferPrices += region.regionalHousingMarketStats.sumOfferPrices;
        }

        recordOfferPrices();
        recordBidPrices();
    }

	private void recordOfferPrices() {
		offerPrices = new double[nSellers];
		int i = 0;
		for (Region region: geography) {
		    for (double price: region.regionalHousingMarketStats.offerPrices) {
                offerPrices[i] = price;
                ++i;
            }
        }
	}

	private void recordBidPrices() {
		bidPrices = new double[nBuyers];
		int i = 0;
        for (Region region: geography) {
            for(double price: region.regionalHousingMarketStats.bidPrices) {
                bidPrices[i] = price;
                ++i;
            }
        }
	}

    private double referencePrice(int quality) {
        return data.HouseSaleMarket.referencePrice(quality);
    }

    public double [][] priceData() {
        for (Region region: geography) {
            for(int i = 0; i < config.N_QUALITY; ++i) {
                priceData[1][i] += region.regionalHousingMarketStats.priceData[1][i];
            }
        }
        for(int i = 0; i < config.N_QUALITY; ++i) {
            priceData[1][i] /= geography.size();
        }
        return priceData;
    }

    ///////////////////////////////////////////////////////////////////////////////////////
    // Getters and setters for Mason
    ///////////////////////////////////////////////////////////////////////////////////////

    public double getAverageDaysOnMarket() {
        return market.averageDaysOnMarket;
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

    public int getNSales() {
        return nSales;
    }

    public int getNNewBuild() {
        return nNewBuild;
    }

    public int getNBuyers() {
        return nBuyers;
    }

    /**
     * @return Current total number of sellers aggregated through all the sale markets
     */
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
        double hpa = 0;
        for (Region region: geography) {
            hpa += region.regionalHousingMarketStats.getHPA();
        }
        return(hpa/geography.size());
    }

    public double getHPI() {
        return(market.housePriceIndex);
    }

    public double getNEmpty() {
        return nEmpty;
    }
}
