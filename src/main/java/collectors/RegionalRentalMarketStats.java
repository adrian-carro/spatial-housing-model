package collectors;

import housing.Config;
import housing.HouseRentalMarket;
import housing.Model;
import housing.Region;

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

    private Config              config = Model.config; // Passes the Model's configuration parameters object to a private field
    private HouseRentalMarket   market;

    //------------------------//
    //----- Constructors -----//
    //------------------------//

    /**
     * Initialises the regional rental market statistics collector
     *
     * @param region Reference to the region owning both the market and the regional collector
     */
    public RegionalRentalMarketStats(Region region) {
        // TODO: Review both national and regional versions of the market stats for both sale and rental markets, as
        // TODO: rental market seems to include by inheritance methods and variables not needed!
        super(region);
        this.market = region.houseRentalMarket;
    }

    //-------------------//
    //----- Methods -----//
    //-------------------//

    public Point2D[] getExpectedGrossYieldByQuality() {
        Point2D [] data = new Point2D[config.N_QUALITY];
        for(int i=0; i<config.N_QUALITY; ++i) {
            data[i] = new Point2D.Double(i, market.getExpectedGrossYield(i));
        }
        return data;
    }

    public Point2D [] getExpectedOccupancyByQuality() {
        Point2D [] data = new Point2D[config.N_QUALITY];
        for(int i=0; i<config.N_QUALITY; ++i) {
            data[i] = new Point2D.Double(i, market.expectedOccupancy(i));
        }
        return data;
    }

    public double getAverageSoldGrossYield() {
        return(market.averageSoldGrossYield);
    }
}
