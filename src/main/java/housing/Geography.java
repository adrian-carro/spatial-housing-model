package housing;

import data.Transport;
import org.apache.commons.math3.random.MersenneTwister;

import java.util.ArrayList;

/**************************************************************************************************
 * Class to encapsulate the geography of regions and the commuting times and fees between them
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
    private ArrayList<ArrayList<Double>>    commutingTimeMatrix;
    private ArrayList<ArrayList<Double>>    commutingFeeMatrix;

    //------------------------//
    //----- Constructors -----//
    //------------------------//

    /**
     * Constructs the geography with its regions and respective target populations and distance between them
     */
    Geography(Config config, MersenneTwister rand) {
        regions = new ArrayList<>(); 
        int regionID = 0;
        // Read target population for each real region from file and create a region accordingly
        for (int targetPopulation: data.Demographics.getTargetPopulationPerRegion()) {        		
            regions.add(new Region(config, rand, targetPopulation, regionID));
            regionID++;
        }
        // Read matrix of commuting times between regions, pass the number of regions to check if it is the same as in
        // the commuting times file
        commutingTimeMatrix = Transport.getCommutingTimeMatrix(regions.size());
        // Read matrix of commuting times between regions, pass the number of regions to check if it is the same as in
        // the commuting times file
        commutingFeeMatrix = Transport.getCommutingFeeMatrix(regions.size());
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
        // Update, for each region, its household statistics collectors, after all markets have been cleared
        for (Region r : regions) r.regionalHouseholdStats.record();
    }

    //----- Getter/setter methods -----//

    public ArrayList<Region> getRegions() { return regions; }

    double getCommutingTimeBetween(Region region1, Region region2) {
        return commutingTimeMatrix.get(region1.getRegionID()).get(region2.getRegionID());
    }

    double getCommutingFeeBetween(Region region1, Region region2) {
        return commutingFeeMatrix.get(region1.getRegionID()).get(region2.getRegionID());
    }
}
