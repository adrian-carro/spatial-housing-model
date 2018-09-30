package housing;

import org.apache.commons.math3.random.MersenneTwister;

import java.util.ArrayList;

/**************************************************************************************************
 * Class to encapsulate the geography of regions and the commuting costs between them, as well as
 * to keep track of some national aggregate variables and deal with the priority queues for
 * households to choose where to bid for housing
 *
 * @author Adrian Carro
 * @since 05/02/2018
 *
 *************************************************************************************************/
public class Geography {

    //------------------//
    //----- Fields -----//
    //------------------//

    private static ArrayList<Region>        regions;
    private ArrayList<ArrayList<Double>>    distanceMatrix;
    private Config	                        config; // Private field to receive the Model's configuration parameters object

    //------------------------//
    //----- Constructors -----//
    //------------------------//

    /**
     * Constructs the geography with its regions and respective target populations and distance between them
     */
    Geography(Config config, MersenneTwister rand) {
        this.config = config;
        regions = new ArrayList<>(); 
        int regionID = 0;
        // Read target population for each real region from file and create a region accordingly
        for (int targetPopulation: data.Demographics.getTargetPopulationPerRegion()) {        		
            regions.add(new Region(this.config, rand, targetPopulation, regionID));
            regionID++;
        }
        // Read matrix of distances between regions, pass the number of regions to check if it is the same as in the distances file
        distanceMatrix = data.Distance.getDistanceMatrix(regions.size());
    }

    //-------------------//
    //----- Methods -----//
    //-------------------//

    /**
     * Initialises the geography by initialising its regions
     */
    public void init() { for (Region r : regions) r.init(); }

    /**
     * Main method of the class: first, it loops through the regions updating household's bids, and then again clearing
     * both markets and recording data as appropriate
     */
    public void step() {
        // Update, for each region, its households, collecting bids at the corresponding markets
        for (Region r : regions) r.stepHouseholds();
        // Update, for each region, its market statistics collectors and markets
        for (Region r : regions) r.stepMarkets();
    }

    //----- Getter/setter methods -----//

    public ArrayList<Region> getRegions() { return regions; }
}
