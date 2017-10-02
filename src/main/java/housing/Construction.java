package housing;

import org.apache.commons.math3.random.MersenneTwister;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;


public class Construction implements IHouseOwner, Serializable {
    private static final long serialVersionUID = -6288390048595500248L;

    //------------------//
    //----- Fields -----//
    //------------------//

    public int                  housingStock; // total number of houses built

    private Config	            config = Model.config; // Passes the Model's configuration parameters object to a private field
    private MersenneTwister     rand = Model.rand; // Passes the Model's random number generator to a private field
    private ArrayList<Region>   geography;
    private HashSet<House>      onMarket;

    //------------------------//
    //----- Constructors -----//
    //------------------------//

	public Construction(ArrayList<Region> geography) {
	    this.geography = geography;
		housingStock = 0;
		onMarket = new HashSet<>();
	}

    //-------------------//
    //----- Methods -----//
    //-------------------//

	public void init() {
		housingStock = 0;
		onMarket.clear();
	}

	public void step() {
        // First update prices of properties put on the market on previous time steps and still unsold
        for(House h : onMarket) {
            h.region.houseSaleMarket.updateOffer(h.getSaleRecord(), h.getSaleRecord().getPrice()*0.95);
        }
	    // Then, for each region...
        for (Region region: geography) {
            // ... compute target housing stock dependent on current and target population for the region
            int targetStock;
            if(region.households.size() < config.TARGET_POPULATION) {
                targetStock = (int)(region.households.size()*config.CONSTRUCTION_HOUSES_PER_HOUSEHOLD);
            } else {
                targetStock = (int)(config.TARGET_POPULATION*config.CONSTRUCTION_HOUSES_PER_HOUSEHOLD);
            }
            // ... compute the shortfall of houses
            int shortFall = targetStock - region.getHousingStock();
            // ... and while there is any shortfall...
            House newBuild;
            while(shortFall > 0) {
                // ... create a new house with a random quality and with the construction sector as the owner
                newBuild = new House(region, (int)(rand.nextDouble()*config.N_QUALITY));
                newBuild.owner = this;
                // ... put the house for sale in the regional house sale market at the reference price for that quality
                region.houseSaleMarket.offer(newBuild,
                        region.regionalHousingMarketStats.getReferencePriceForQuality(newBuild.getQuality()));
                // ... add the house to the portfolio of construction sector properties
                onMarket.add(newBuild);
                // ... and finally increase both regional and general housing stocks, and decrease shortfall
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
}
