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

    private static ArrayList<Region>    regions;
    private Config	                    config; // Private field to receive the Model's configuration parameters object
    // TODO: Bring commuting costs data into this class to be used for computing prices and filling in the priority queues

    //------------------------//
    //----- Constructors -----//
    //------------------------//

    /**
     * Constructs the geography with its regions and respective target populations
     */
    Geography(Config config, MersenneTwister rand) {
        this.config = config;
        regions = new ArrayList<>();
        for (int targetPopulation: data.Demographics.getTargetPopulationPerRegion()) {
            regions.add(new Region(this.config, rand, targetPopulation));
        }
    }

    //-------------------//
    //----- Methods -----//
    //-------------------//

    /**
     * Initialises the geography by initialising its regions
     */
    public void init() {
        // First initialise all regions
        for (Region r : regions) r.init();
    }

    /**
     * Main method of the class: first, it loops through the regions updating their priority queues of region-quality
     * bands, then, it loops again through the regions updating household's bids, clearing both markets, and recording
     * data as appropriate. Note that, since every record at the priority queue of regions-quality bands would need to
     * be removed and re-introduced, it is more efficient to simply clear the priority queue altogether and fill it in
     * again from scratch
     */
    public void step() {
        // Update, for each region, the priority queue of region-quality bands using current market data
        // For each region...
        for (Region origin : regions) {
            // ...first, clear the priority queue of region-quality bands
            origin.regionsPQ.clear();
            // ...then, fill it in using current (exponential moving average) prices
            for (Region destination: regions) {
                for(int quality = 0; quality < config.N_QUALITY; ++quality) {
                    // TODO: Add here commuting costs!
                    double price = destination.regionalHousingMarketStats.getExpAvSalePriceForQuality(quality);
                    RegionQualityRecord record = new RegionQualityRecord(destination, quality, price);
                    origin.regionsPQ.add(record);
                }
            }
            // ...finally, sort priorities before any use
            origin.regionsPQ.sortPriorities();
        }
        // Update, for each region, its households, market statistics collectors and markets
        for (Region r : regions) r.step();
    }

    //----- Getter/setter methods -----//

    public ArrayList<Region> getRegions() { return regions; }
}
