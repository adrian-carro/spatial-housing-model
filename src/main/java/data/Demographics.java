package data;

import housing.Config;
import housing.Model;

import utilities.BinnedDataDouble;
import utilities.Pdf;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

/**************************************************************************************************
 * Class to read and work with demographics data before passing it to the Demographics class. Note
 * that, throughout this class, we use "real population" to refer to actual numbers of individuals
 * while we leave the term "population" to refer to numbers of agents, i.e., numbers of households
 *
 * @author daniel, Adrian Carro
 *
 *************************************************************************************************/
public class Demographics {

    //------------------//
    //----- Fields -----//
    //------------------//

    private static Config config = Model.config; // Passes the Model's configuration parameters object to a private field

    private static int  totalRealPopulation = 0;

    /**
     * Probability density by age of the representative householder given that the household is newly formed. New
     * households can be formed by, e.g., children leaving home, divorce, separation, people leaving an HMO. Roughly
     * calibrated against "The changing living arrangements of young adults in the UK" ONS Population Trends winter 2009
     */
    private static BinnedDataDouble householdAgeAtBirth = new BinnedDataDouble(config.DATA_HOUSEHOLD_AGE_AT_BIRTH_PDF);
    public static Pdf pdfHouseholdAgeAtBirth = new Pdf(householdAgeAtBirth, 800);

	/**
	 * Probability that a household 'dies' per year given age of the representative householder
	 * Death of a household may occur by marriage, death of single occupant, moving together.
	 * As first order approx: we use female death rates, assuming singles live at home until marriage,
	 * there is no divorce and the male always dies first
	 */
    // Create a BinnedDataDouble object to keep bins and probabilities
    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection") // Warning due to data array not being queried (other variables are used)
    private static BinnedDataDouble probDeathGivenAgeData  = new BinnedDataDouble(config.DATA_DEATH_PROB_GIVEN_AGE);

    // Once data on household age at birth and on death probabilities has been loaded, compute birth rate
    private static double birthRate = computeBirthRate();

    /**
     * Target number of households for each region. Note that we are using Local Authority Districts as regions and that
     * we only have data on their population, not their number of households. Furthermore, we want to be able to set the
     * target total number of agents as a separate parameter. To solve this, we assume that each Local Authority
     * District contains the same fraction of the total number of households as their fraction of the total population.
     */
    private static ArrayList<Integer> targetPopulationPerRegion =
            computeTargetPopulationPerRegion(config.DATA_REAL_POPULATION_PER_REGION, config.TARGET_POPULATION);

    //-------------------//
    //----- Methods -----//
    //-------------------//

    /**
     * Compute monthly birth rate as a fraction of the target population, i.e., the number of births divided by the
     * target population
     */
    private static double computeBirthRate() {
        // First, compute total number of households with ages between the minimum possible age at birth and the maximum
        // possible age at birth, which is also the last age for which death probability is zero (birth area)
        double sum1 = 0.0;
        for (int i = (int)householdAgeAtBirth.getSupportLowerBound()*config.constants.MONTHS_IN_YEAR;
             i <= (int)householdAgeAtBirth.getSupportUpperBound()*config.constants.MONTHS_IN_YEAR - 2;
             i++) {
            sum1 += (householdAgeAtBirth.getSupportUpperBound()*config.constants.MONTHS_IN_YEAR - 1 - i)
                    * probHouseholdAgeAtBirthPerMonth(i);
        }
        // Second, compute total number of households with ages between the minimum age at which death probability is
        // non-zero and the maximum possible age, from which death probability is one (death area)
        double sum2 = 0.0;
        for (int j = (int)(probDeathGivenAgeData.getSupportLowerBound()*config.constants.MONTHS_IN_YEAR + 1);
             j <= (int)(probDeathGivenAgeData.getSupportUpperBound()*config.constants.MONTHS_IN_YEAR - 1); j++) {
            double product = 1;
            for (int i = (int)(probDeathGivenAgeData.getSupportLowerBound()*config.constants.MONTHS_IN_YEAR);
                 i <= j; i++) {
                product *= (1.0 - probDeathGivenAge(((double) i) / config.constants.MONTHS_IN_YEAR)
                        / config.constants.MONTHS_IN_YEAR);
            }
            sum2 += product;
        }
        return 1.0/(1 + sum1 + sum2);
    }

    /**
     * Method that gives, for a given age in years, its corresponding probability of death
     *
     * @param ageInYears Age in years (double)
     * @return probability Probability of death for the given age in years (double)
     */
    public static double probDeathGivenAge(double ageInYears) {
        if (ageInYears < probDeathGivenAgeData.getSupportLowerBound()) {
            return 0.0;
        } else if (ageInYears >= probDeathGivenAgeData.getSupportUpperBound()) {
            return config.constants.MONTHS_IN_YEAR;
        } else {
            return probDeathGivenAgeData.getBinAt(ageInYears);
        }
    }

    /**
     * Probability that a new household would be assigned a given age (in months)
     *
     * @param ageInMonths Age in months (int)
     */
    private static double probHouseholdAgeAtBirthPerMonth(int ageInMonths) {
        if (ageInMonths / config.constants.MONTHS_IN_YEAR < householdAgeAtBirth.getSupportLowerBound()
                || ageInMonths / config.constants.MONTHS_IN_YEAR > householdAgeAtBirth.getSupportUpperBound()) {
            return 0.0;
        } else {
            return householdAgeAtBirth.getBinAt((double) ageInMonths / config.constants.MONTHS_IN_YEAR)
                    / (config.constants.MONTHS_IN_YEAR * householdAgeAtBirth.getBinWidth());
        }
    }

    /**
     * Method to compute target numbers of households for each region. It makes use of data on the actual population of
     * each region, the target total number of households in the model set by the user, and the assumption that regions
     * contain the same fraction of households as the fraction they contain of the total population.
     *
     * @param fileName String with name of file (address inside source folder)
     * @param totalTargetPopulation Integer with the total target number of households set by the user
     * @return targetPopulationPerRegion ArrayList of integers with the target number of households for each region
     */
    private static ArrayList<Integer> computeTargetPopulationPerRegion(String fileName, int totalTargetPopulation) {
        ArrayList<Integer> targetPopulationPerRegion = new ArrayList<>();
        for (Integer realPopulation: readRealPopulationPerRegion(fileName)) {
            targetPopulationPerRegion.add(totalTargetPopulation*realPopulation/totalRealPopulation);
        }
        return targetPopulationPerRegion;
    }

    /**
     * Method to read the real population of each region from a data file.
     *
     * @param fileName String with name of file (address inside source folder)
     * @return realPopulationPerRegion ArrayList of integers with the real population of each region
     */
    private static ArrayList<Integer> readRealPopulationPerRegion(String fileName) {
        int realPopulation;
        ArrayList<Integer> realPopulationPerRegion = new ArrayList<>();
        // Try-with-resources statement
        try (BufferedReader buffReader = new BufferedReader(new FileReader(fileName))) {
            String line = buffReader.readLine();
            while (line != null) {
                if (line.charAt(0) != '#') {
                    try {
                        realPopulation = Integer.parseInt(line.split(",")[1].trim());
                        realPopulationPerRegion.add(realPopulation);
                        totalRealPopulation += realPopulation;
                    } catch (NumberFormatException nfe) {
                        System.out.println("Exception " + nfe + " while trying to parse " +
                                line.split(",")[1] + " for an integer");
                        nfe.printStackTrace();
                    }
                }
                line = buffReader.readLine();
            }
        } catch (IOException ioe) {
            System.out.println("Exception " + ioe + " while trying to read file '" + fileName + "'");
            ioe.printStackTrace();
        }
        return realPopulationPerRegion;
    }

    //----- Getter/setter methods -----//

    public static ArrayList<Integer> getTargetPopulationPerRegion() { return targetPopulationPerRegion; }

    public static double getBirthRate() { return birthRate; }

}
