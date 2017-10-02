package housing;

import java.util.ArrayList;
import java.util.Iterator;

import org.apache.commons.math3.random.MersenneTwister;

public class Demographics {

	//------------------//
	//----- Fields -----//
	//------------------//

	private Config	            config = Model.config; // Passes the Model's configuration parameters object to a private field
	private MersenneTwister     rand = Model.rand; // Passes the Model's random number generator to a private field
    private ArrayList<Region>   geography;
    private int                 totalPopulation;

    //------------------------//
    //----- Constructors -----//
    //------------------------//

    /**
     * Initialises the demographics with a reference to the whole geography of regions
     *
     * @param geography Geography of region where the demographic processes occur
     */
    public Demographics(ArrayList<Region> geography) {
        this.geography = geography;
    }

    //-------------------//
    //----- Methods -----//
    //-------------------//

    /**
     * Sets initial values for all relevant variables
     */
    public void init() {
        totalPopulation = 0;
    }


    /***
	 * Add newly 'born' households to the model and remove households that 'die'
	 */
	public void step() {
	    // For each region...
        for (Region region: geography) {
            // Birth: Add households in proportion to target population and monthly birth rate of first-time-buyers
            // TODO: Shouldn't this include also new renters? Review the whole method...
            int nBirths = (int)(region.getTargetPopulation()*config.FUTURE_BIRTH_RATE/config.constants.MONTHS_IN_YEAR
                    + 0.5);
            while(nBirths-- > 0) {
                region.households.add(new Household(data.Demographics.pdfHouseholdAgeAtBirth.nextDouble(), region));
                totalPopulation++;
            }
            // Death: Kill households with a probability dependent on their age and organise inheritance
            double pDeath;
            Iterator<Household> iterator = region.households.iterator();
            while(iterator.hasNext()) {
                Household h = iterator.next();
                pDeath = data.Demographics.probDeathGivenAge(h.getAge())/config.constants.MONTHS_IN_YEAR;
                if(rand.nextDouble() < pDeath) {
                    iterator.remove();
                    totalPopulation--;
                    // Inheritance
                    // TODO: This imposes inheritance within the same region!!!
                    h.transferAllWealthTo(region.households.get(rand.nextInt(region.households.size())));
                }
            }
        }
	}

    //----- Getter/setter methods -----//

    public int getTotalPopulation() { return totalPopulation; }
}
