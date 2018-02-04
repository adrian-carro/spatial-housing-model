package housing;

import org.apache.commons.math3.random.MersenneTwister;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

/**************************************************************************************************
 * Class to represent the building sector in the aggregate and encapsulate its decisions
 *
 * @author daniel, Adrian Carro
 *
 *************************************************************************************************/
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

    //#################################################################//
    //##### HARDCODED PARAMETERS ##### TO BE MOVED TO CONFIG FILE #####//
    //#################################################################//

    private boolean     SMART_CONSTRUCTION = false;
    private double      BUILDING_CAPACITY_PER_HOUSEHOLD = 0.001; // This sets the speed (per household) at which houses are built (UKnewHomesAYear/(UKhouseholds*12)) = 0.00044;
    private double      BUILDING_COST_OVER_REFERENCE_PRICE = 0.8; // This sets the minimum level of HPI below which the construction sector stops building
    private double []   LOCAL_AUTHORITY_POLICY = new double[]{0.33, 0.66, 1.0}; // Success rate of planning applications for each region

    //#################################################################//
    //#################################################################//
    //#################################################################//

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
	    if (SMART_CONSTRUCTION) {
	        smartStep();
        } else {
            normalStep();
        }
    }

    private void smartStep() {
        // Update prices of properties put on the market on previous time steps and still unsold
        for(House h : onMarket) {
            h.region.houseSaleMarket.updateOffer(h.getSaleRecord(), h.getSaleRecord().getPrice()*0.95);
        }
        // Find the maximum number of houses the construction sector can build this month, given available resources (minimum set to 1)
        int maxnNewBuild = Math.max(1, (int)(Model.demographics.getTotalPopulation()*BUILDING_CAPACITY_PER_HOUSEHOLD));
        // Find the number of houses the construction sector would be willing to build in each region (assuming no
        // resource constraint), looking at different economic and demographic variables
        int [] nHousesToBuildPerRegion = new int[geography.size()];
        int nHousesToBuild = 0;
        for (int i = 0; i < geography.size(); i++) {
            // TODO: Rethink this equation and discuss it with Doyne
            // TODO: Another possibility would be to build always at full capacity but distribute houses according to HPI
            double profitabilityIndex = geography.get(i).regionalHousingMarketStats.getHPI()
                    - BUILDING_COST_OVER_REFERENCE_PRICE;
            // This is the actual supply gap for the region to have the "expected" number of houses given its population
//            int supplyGap = (int)(geography.get(i).households.size()*config.CONSTRUCTION_HOUSES_PER_HOUSEHOLD)
//                    - geography.get(i).getHousingStock();
            // Not supply gap, but number of houses that would be built this month in this region if profitabilityIndex would be 1 (RegionPopulation*UKnewHouses/(12*UKpopulation))
            int supplyGap = 5;
            // Build only if it is profitable!
            if (profitabilityIndex > 0.0 && supplyGap > 0) {
//                supplyGap = (int)(5*supplyGap/(geography.get(i).households.size()*config.CONSTRUCTION_HOUSES_PER_HOUSEHOLD));
                nHousesToBuildPerRegion[i] = nextBinomial((int)(profitabilityIndex*supplyGap + 0.5),
                        LOCAL_AUTHORITY_POLICY[i]);
            } else {
                nHousesToBuildPerRegion[i] = 0;
            }
            nHousesToBuild += nHousesToBuildPerRegion[i];
        }
        // If too many houses are to be built...
        if (nHousesToBuild > maxnNewBuild) {
            nNewBuild = 0;
            // ...cap proportionally to get a maximum of maxnNewBuild, write to nNewBuildPerRegion
            for (int i = 0; i < geography.size(); i++) {
                nNewBuildPerRegion.put(geography.get(i), (int)((double)(nHousesToBuildPerRegion[i]*maxnNewBuild)
                        /nHousesToBuild+0.5));
                // ...and set the number of units to be actually build during this month
                nNewBuild += (int)((double)(nHousesToBuildPerRegion[i]*maxnNewBuild)/nHousesToBuild+0.5);
            }
        // Otherwise...
        } else {
            // ...keep number of units to be built, write to nNewBuildPerRegion
            for (int i = 0; i < geography.size(); i++) {
                nNewBuildPerRegion.put(geography.get(i), nHousesToBuildPerRegion[i]);
            }
            // ...and set the number of units to be actually build during this month
            nNewBuild = nHousesToBuild;
        }
        // Finally, for each region, build the promised houses
        for (Region region: geography) {
            House newHouse;
            for (int i = 0; i < nNewBuildPerRegion.get(region); i++) {
                // ...create a new house with a random quality and with the construction sector as the owner
                newHouse = new House(region, (int)(rand.nextDouble()*config.N_QUALITY));
                newHouse.owner = this;
                // ...put the house for sale in the regional house sale market at the reference price for that quality
                region.houseSaleMarket.offer(newHouse,
                        region.regionalHousingMarketStats.getExpAvSalePriceForQuality(newHouse.getQuality()));
//                        region.regionalHousingMarketStats.getReferencePriceForQuality(newHouse.getQuality()));
                // ...add the house to the portfolio of construction sector properties
                onMarket.add(newHouse);
                // ...and finally increase both regional and general housing stocks
                region.increaseHousingStock();
                ++housingStock;
            }
        }
    }

	private void normalStep() {
	    // Initialise to zero the number of houses built this month
	    nNewBuild = 0;
        // First update prices of properties put on the market on previous time steps and still unsold
        for (House h : onMarket) {
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
            while (shortFall > 0) {
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

    //##### Binomial random numbers... #####// Todo: Replace with a proper implementation of this!
    private int nextBinomial(int trials, double probability) {
        int x = 0;
        for (int i = 0; i < trials; i++) {
            if (Math.random() < probability) {
                x++;
            }
        }
        return x;
    }
}
