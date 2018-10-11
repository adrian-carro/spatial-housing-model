package housing;

import java.util.Iterator;

import org.apache.commons.math3.random.MersenneTwister;

public class Demographics {

	//------------------//
	//----- Fields -----//
	//------------------//

    private Config	            config; // Private field to receive the Model's configuration parameters object
    private MersenneTwister     rand; // Private field to receive the Model's random number generator
    private Geography           geography;
    private int                 totalPopulation;

    //------------------------//
    //----- Constructors -----//
    //------------------------//

    /**
     * Initialises the demographics with a reference to the whole geography of regions
     *
     * @param geography Geography of region where the demographic processes occur
     */
    public Demographics(Config config, MersenneTwister rand, Geography geography) {
        this.config = config;
        this.rand = rand;
        this.geography = geography;
    }

    //-------------------//
    //----- Methods -----//
    //-------------------//

    /**
     * Sets initial values for all relevant variables
     */
    public void init() { totalPopulation = 0; }


    /**
	 * Adds newly "born" households to the model and removes households that "die". In particular, households are added
     * to each region in proportion to the region's target population, which is a measure of the availability of
     * employment opportunities (number of jobs) in that region. Furthermore, households get permanently assigned this
     * region as their job region.
	 */
	public void step() {
	    // For each region...
        for (Region region: geography.getRegions()) {
            // Birth: Add new households at a rate compatible with the age at birth distribution, the probability of
            // death dependent on age, and the target population
            int nBirths = (int) (region.getTargetPopulation() * data.Demographics.getBirthRate() + rand.nextDouble());
            // Finally, add the households, with random ages drawn from the corresponding distribution
            while(nBirths-- > 0) {
                region.households.add(new Household(config, rand,
                        data.Demographics.pdfHouseholdAgeAtBirth.nextDouble(rand), geography, region));
                totalPopulation++;
            }
            // Death: Kill households with a probability dependent on their age and organise inheritance
            double pDeath;
            Iterator<Household> iterator = region.households.iterator();
            while(iterator.hasNext()) {
                Household h = iterator.next();
                pDeath = data.Demographics.probDeathGivenAge(h.getAge())/config.constants.MONTHS_IN_YEAR;
                if (rand.nextDouble() < pDeath) {
                    iterator.remove();
                    totalPopulation--;
                    // Inheritance
                    // TODO: This imposes inheritance within the same region, which is the job region of the dead household!!!
                    h.transferAllWealthTo(region.households.get(rand.nextInt(region.households.size())));
                }
            }
        }
	}

    //----- Getter/setter methods -----//

    public int getTotalPopulation() { return totalPopulation; }
}
