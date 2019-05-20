package housing;

import java.util.*;

import org.apache.commons.math3.random.MersenneTwister;

public class Demographics {

	//------------------//
	//----- Fields -----//
	//------------------//

    private Config	            config; // Private field to receive the Model's configuration parameters object
    private MersenneTwister     rand; // Private field to receive the Model's random number generator
    private Random              altRand; // Alternative random number generator to use with Collections.shuffle()
    private Geography           geography;
    private int                 totalPopulation;
    private double              firstBinMin = data.Demographics.getMonthlyAgeDistributionMinimum();
    private double              binWidth = data.Demographics.getMonthlyAgeDistributionBinWidth();
    private int                 ageDistSize = data.Demographics.getMonthlyAgeDistributionSize();
    private int []              householdsPerAgeBand = new int[data.Demographics.getMonthlyAgeDistributionSize()];
    private int []              birthsAndDeaths = new int[data.Demographics.getMonthlyAgeDistributionSize()];

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
        this.altRand = new Random(this.rand.nextLong());
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
     * Add newly born households to the model and remove households that die. Given a distribution of age (with monthly
     * age bins), the expected number of households per age band is used to compute how many new households are to be
     * added or removed from each age band. Then, households are added to each region in proportion to the region's
     * target population, which is a measure of the availability of employment opportunities (number of jobs) in that
     * region. Furthermore, households get permanently assigned this region as their job region.
     */
	public void step() {
        // Increase age of households and create a histogram with the number of households in each age band
        updateHouseholdsPerAgeBand();
        // Update the list of births (positive) and deaths (negative) to be implemented
        updateBirthsAndDeaths();
        // Implement births in each age bin by adding new households with random ages between the corresponding edges
        implementBirths();
        // Implement deaths according to the probabilities calculated above
        implementDeaths();
    }


    /**
     * After increasing the age of all currently existing households by one month, create a histogram with the actual
     * number of households in each age band. Note that there might be households older than the maximum bin edge, which
     * will be ignored here, but they will be assigned a death probability equal to one later on
     */
    private void updateHouseholdsPerAgeBand() {
        // The array must be reset to zero every time step
        Arrays.fill(householdsPerAgeBand, 0);
        // For each region...
        for (Region region : geography.getRegions()) {
            for (Household h : region.households) {
                // ...first, households age
                h.ageOneMonth();
                // ...then find the bin at which the age of the household falls...
                int i = (int) ((h.getAge() - firstBinMin) / binWidth);
                // ...and increase the number of households in that bin by one (ignoring ages beyond the maximum edge)
                if (i < householdsPerAgeBand.length) householdsPerAgeBand[i]++;
            }
        }
    }

    /**
     * Update the array of births (positive) and deaths (negative) to be implemented by comparing the actual number of
     * households with the expected number of households for each age bin
     */
    private void updateBirthsAndDeaths() {
        for (int i = 0; i < householdsPerAgeBand.length; i++) {
            birthsAndDeaths[i] = data.Demographics.getExpectedHouseholdsForAgeBand(i) - householdsPerAgeBand[i];
        }
    }

    /**
     * Implement births in each age bin by adding new households with random ages drawn from a uniform distribution
     * between the corresponding bin edges
     */
    private void implementBirths() {
        // For each age band...
        for (int i = 0; i < birthsAndDeaths.length; i++) {
            // ...implement all required births...
            while (birthsAndDeaths[i] > 0) {
                // ...by choosing a random age within the band
                double age = (rand.nextDouble() + i) * binWidth + firstBinMin;
                if (age >= firstBinMin + (i + 1) * binWidth) { // To correct for possible rounding errors
                    age = Math.nextDown(firstBinMin + (i + 1) * binWidth);
                }
                // ...and a random job region, with probability proportional to the target population of the region
                Region jobRegion =
                        geography.getRegions().get(data.Demographics.getProbDistOfRegionsByPopulation().sample());
                jobRegion.households.add(new Household(config, rand, age, geography, jobRegion));
                birthsAndDeaths[i]--;
                totalPopulation++;
            }
        }
    }

    /**
     * First, all households are collected in an ArrayList and shuffled so as to randomise deaths. Then, a run through
     * these households is used to store for killing as many households per age band as needed according to the
     * birthsAndDeaths array previously created. Thus, this method is not probabilistic and it rather kills the exact
     * number of households per age band as needed to obtain the expected number of households per age band. Then, a
     * first run through the households to kill is used to remove them from their home region and, finally, another run
     * through the households to kill is used to implement inheritance, which might mean moving some households between
     * regions if they inherit a new home in a different region from the one they had so far been living at.
     */
    private void implementDeaths() {
        // First, run through the regions collecting all households in a single ArrayList...
        ArrayList<Household> allHouseholds = new ArrayList<>();
        for (Region region: geography.getRegions()) {
            allHouseholds.addAll(region.households);
        }
        // ...and shuffle this new ArrayList to randomise deaths
        Collections.shuffle(allHouseholds, altRand);
        // Then, run through households...
        ArrayList<Household> householdsToKill = new ArrayList<>();
        for (Household h : allHouseholds) {
            // ...finding, for each of them, its age bin...
            int i = (int) ((h.getAge() - firstBinMin) / binWidth);
            // ...and killing it if more deaths still to be implemented in this age bin (controlling for out of bounds)
            if (i >= ageDistSize) {
                householdsToKill.add(h);
                totalPopulation--;
            } else if (birthsAndDeaths[i] < 0) {
                householdsToKill.add(h);
                birthsAndDeaths[i]++;
                householdsPerAgeBand[i]--;
                totalPopulation--;
            }
        }
        // Then, remove all households to be killed from their respective regions
        for (Household h : householdsToKill) {
            h.getHomeRegion().households.remove(h);
        }
        // And, finally, implement inheritance with a randomly chosen heir within the same region (preventing
        // self-inheritance)
        for (Household h : householdsToKill) {
            Household beneficiary = h.getHomeRegion().households.get(rand.nextInt(h.getHomeRegion().households.size()));
            while (beneficiary == h) {
                beneficiary = h.getHomeRegion().households.get(rand.nextInt(h.getHomeRegion().households.size()));
            }
            h.transferAllWealthTo(beneficiary);
        }
    }

    //----- Getter/setter methods -----//

    public int getTotalPopulation() { return totalPopulation; }
}
