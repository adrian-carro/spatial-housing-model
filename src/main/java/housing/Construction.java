package housing;

import org.apache.commons.math3.random.MersenneTwister;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;


public class Construction implements IHouseOwner, Serializable {
    private static final long serialVersionUID = -6288390048595500248L;

    //------------------//
    //----- Fields -----//
    //------------------//

    private HashMap<Region, Integer>    nNewBuildPerRegion;
    private int                         housingStock; // Total number of houses in the whole model
    private int                         nNewBuild; // Number of houses built this month

    private Config	                    config = Model.config; // Passes the Model's configuration parameters object to a private field
    private MersenneTwister             rand = Model.rand; // Passes the Model's random number generator to a private field
    private ArrayList<Region>           geography;
    private HashSet<House>              onMarket;

    //------------------------//
    //----- Constructors -----//
    //------------------------//

	public Construction(ArrayList<Region> geography) {
	    this.geography = geography;
        nNewBuildPerRegion = new HashMap<>();
        onMarket = new HashSet<>();
	}

    //-------------------//
    //----- Methods -----//
    //-------------------//

	public void init() {
        housingStock = 0;
        for (Region region: geography) nNewBuildPerRegion.put(region, 0);
		onMarket.clear();
	}

	public void step() {
	    // Initialise to zero the number of houses built this month
	    nNewBuild = 0;
        // First update prices of properties put on the market on previous time steps and still unsold
	    // Tony: why do we compute the price by multiplying the selling price by 0.95? should 0.95 be
	    // a parameter to be set in the config file?
        for(House h : onMarket) {
            h.region.houseSaleMarket.updateOffer(h.getSaleRecord(), h.getSaleRecord().getPrice()*0.95);
        }
	    // Then, for each region...
        for (Region region: geography) {
            // ...compute target housing stock dependent on current and target population for the region
            int targetStock;
            if(region.households.size() < region.targetPopulation) {
                targetStock = (int)(region.households.size()*config.CONSTRUCTION_HOUSES_PER_HOUSEHOLD);
            } else {
                targetStock = (int)(region.targetPopulation*config.CONSTRUCTION_HOUSES_PER_HOUSEHOLD);
            }
            // ...compute the shortfall of houses
            int shortFall = targetStock - region.getHousingStock();
            // ...if shortfall is positive...
            if (shortFall > 0) {
                // ...add this regional shortfall to the number of houses built this month in the region and nationally
                nNewBuildPerRegion.put(region, shortFall);
                nNewBuild += shortFall;
            } else {
                // ...otherwise add zero newBuilds to the region
                nNewBuildPerRegion.put(region, 0);
            }
            // ...and while there is any shortfall...
            House newHouse;
            while(shortFall > 0) {
                // ...create a new house with a random quality and with the construction sector as the owner
                newHouse = new House(region, (int)(rand.nextDouble()*config.N_QUALITY));
                newHouse.owner = this;
                // ...put the house for sale in the regional house sale market at the reference price for that quality
                region.houseSaleMarket.offer(newHouse,
                        region.regionalHousingMarketStats.getReferencePriceForQuality(newHouse.getQuality()));
                // ...add the house to the portfolio of construction sector properties
                onMarket.add(newHouse);
                // ...and finally increase both regional and general housing stocks, and decrease shortfall
                region.increaseHousingStock();
                ++housingStock;
                --shortFall;
            }
        }
	}

	@Override
	public void completeHouseSale(HouseSaleRecord sale) { onMarket.remove(sale.house); }

	@Override
	public void endOfLettingAgreement(House h, PaymentAgreement p) {
        System.out.println("Strange: a tenant is moving out of a house owned by the construction sector!");
	}

	@Override
	public void completeHouseLet(HouseSaleRecord sale) {
        System.out.println("Strange: the construction sector is trying to let a house!");
	}

    //----- Getter/setter methods -----//

    public int getHousingStock() { return housingStock; }

    public HashMap<Region, Integer> getnNewBuildPerRegion() { return nNewBuildPerRegion; }

    public int getnNewBuildForRegion(Region region) { return nNewBuildPerRegion.get(region); }

    public int getnNewBuild() { return nNewBuild; }
}
