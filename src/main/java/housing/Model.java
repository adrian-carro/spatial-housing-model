package housing;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Scanner;
import java.time.Instant;

import collectors.*;

import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.cli.*;
import org.apache.commons.io.FileUtils;

/**************************************************************************************************
 * This is the root object of the simulation. Upon creation it creates and initialises all the
 * agents in the model.
 *
 * The project is prepared to be run with maven, and it takes the following command line input
 * arguments:
 *
 * -configFile <arg>    Configuration file to be used (address within project folder). By default,
 *                      'src/main/resources/config.properties' is used.
 * -outputFolder <arg>  Folder in which to collect all results (address within project folder). By
 *                      default, 'Results/<current date and time>/' is used. The folder will be
 *                      created if it does not exist.
 * -dev                 Removes security question before erasing the content inside output folder
 *                      (if the folder already exists).
 * -help                Print input arguments usage information.
 *
 * Note that the seed for random number generation is set from the config file.
 *
 * @author daniel, Adrian Carro
 *
 *************************************************************************************************/

@SuppressWarnings("serial")

public class Model {

    //------------------//
    //----- Fields -----//
    //------------------//

    public static Config                config;
    public static Demographics		    demographics;
    public static Construction		    construction;
    public static CentralBank		    centralBank;
    public static Bank 				    bank;
    public static ArrayList<Region>     geography;
    public static MersenneTwister	    rand;
    public static CreditSupply          creditSupply;
    public static CoreIndicators        coreIndicators;
    public static HouseholdStats        householdStats;
    public static HousingMarketStats    housingMarketStats;
    public static RentalMarketStats     rentalMarketStats;
    public static MicroDataRecorder     transactionRecorder;
    public static int	                nSimulation; // To keep track of the simulation number
    public static int	                t; // To keep track of time (in months)

    static Government		            government;

    private static Recorder             recorder;
    private static String               configFileName;
    private static String               outputFolder;

    // Temporary stuff
//    static long startTime;
//    static long endTime;
//    static long durationDemo = 0;

    //------------------------//
    //----- Constructors -----//
    //------------------------//

    /**
     * @param configFileName String with the address of the configuration file
     * @param outputFolder String with the address of the folder for storing results
     */
    public Model(String configFileName, String outputFolder) {
        // TODO: Check that random numbers are working properly!
        config = new Config(configFileName);
        rand = new MersenneTwister(config.SEED);
        geography = new ArrayList<>();

        for (int targetPopulation: data.Demographics.targetPopulationPerRegion) {
            geography.add(new Region(targetPopulation));
        }

        government = new Government();
        demographics = new Demographics(geography);
        construction = new Construction(geography);
        centralBank = new CentralBank();
        bank = new Bank();

        recorder = new collectors.Recorder(outputFolder);
        transactionRecorder = new collectors.MicroDataRecorder(outputFolder);
        creditSupply = new collectors.CreditSupply(outputFolder);
        coreIndicators = new collectors.CoreIndicators();
        householdStats = new collectors.HouseholdStats(geography);
        housingMarketStats = new collectors.HousingMarketStats(geography);
        rentalMarketStats = new collectors.RentalMarketStats(geography);

        nSimulation = 0;
    }

    //-------------------//
    //----- Methods -----//
    //-------------------//

	public static void main(String[] args) {

	    // Handle input arguments from command line
        handleInputArguments(args);

        // Create an instance of Model in order to initialise it (reading config file)
        new Model(configFileName, outputFolder);

        // Start data recorders for output
        setupStatics();

        // Open files for writing multiple runs results
        recorder.openMultiRunFiles(config.recordCoreIndicators);

        // Perform config.N_SIMS simulations
		for (nSimulation = 1; nSimulation <= config.N_SIMS; nSimulation += 1) {

            // For each simulation, open files for writing single-run results
            recorder.openSingleRunFiles(nSimulation);

		    // For each simulation, initialise both houseSaleMarket and houseRentalMarket variables (including HPI)
            init();

            // For each simulation, run config.N_STEPS time steps
			for (t = 0; t <= config.N_STEPS; t += 1) {

                // Steps model and stores sale and rental markets bid and offer prices, and their averages, into their
                // respective variables
                modelStep();

//                if (t >= config.TIME_TO_START_RECORDING) {
                    // Write results of this time step and run to both multi- and single-run files
                    recorder.writeTimeStampResults(config.recordCoreIndicators, t);
//                }

                // Print time information to screen
                if (t % 100 == 0) {
                    System.out.println("Simulation: " + nSimulation + ", time: " + t);
                }
            }

			// Finish each simulation within the recorders (closing single-run files, changing line in multi-run files)
            recorder.finishRun(config.recordCoreIndicators);
            // TODO: Check what this is actually doing and if it is necessary
            if(config.recordMicroData) transactionRecorder.endOfSim();
		}

        // After the last simulation, clean up
        recorder.finish(config.recordCoreIndicators);
        if(config.recordMicroData) transactionRecorder.finish();

        //Stop the program when finished
//        System.out.println("Demographics: " + durationDemo/(double)1000000000);

		System.exit(0);
	}

	private static void setupStatics() {
        setRecordGeneral();
		setRecordCoreIndicators(config.recordCoreIndicators);
		setRecordMicroData(config.recordMicroData);
	}

	private static void init() {
        demographics.init();
		construction.init();
		bank.init();
		housingMarketStats.init();
		rentalMarketStats.init();
        for(Region r : geography) r.init();
	}

	private static void modelStep() {
        // Update population with births and deaths in each region
        demographics.step();
        // Update number of houses in each region
        construction.step();
        // Update, for each region, its households, market statistics collectors and markets
        for(Region r : geography) r.step();
        // Update all sale market statistics by collecting and aggregating results from the regions
        housingMarketStats.collectRegionalRecords();
        // Update all rental market statistics by collecting and aggregating results from the regions
        rentalMarketStats.collectRegionalRecords();
        // Update all household statistics by collecting and aggregating results from the regions
        householdStats.collectRegionalRecords();
        // Update all credit supply statistics // TODO: Check what this actually does and if it should go elsewhere!
        creditSupply.step();
		// Update bank and interest rate for new mortgages
		bank.step(demographics.getTotalPopulation());
        // Update central bank policies (currently empty!)
		centralBank.step(coreIndicators);
	}

    /**
     * This method handles command line input arguments to
     * determine the address of the input config file and
     * the folder for outputs
     *
     * @param args String with the command line arguments
     */
	private static void handleInputArguments(String[] args) {

        // Create Options object
        Options options = new Options();

        // Add configFile and outputFolder options
        options.addOption("configFile", true, "Configuration file to be used (address within " +
                "project folder). By default, 'src/main/resources/config.properties' is used.");
        options.addOption("outputFolder", true, "Folder in which to collect all results " +
                "(address within project folder). By default, 'Results/<current date and time>/' is used. The " +
                "folder will be created if it does not exist.");
        options.addOption("dev", false, "Removes security question before erasing the content" +
                "inside output folder (if the folder already exists).");
        options.addOption("help", false, "Print input arguments usage information.");

        // Create help formatter in case it will be needed
        HelpFormatter formatter = new HelpFormatter();

        // Parse command line arguments and perform appropriate actions
        // Create a parser and a boolean variable for later control
        CommandLineParser parser = new DefaultParser();
        boolean devBoolean = false;
        try {
            // Parse command line arguments into a CommandLine instance
            CommandLine cmd = parser.parse(options, args);
            // Check if help argument has been passed
            if(cmd.hasOption("help")) {
                // If it has, then print formatted help to screen and stop program
                formatter.printHelp( "spatial-housing-model", options );
                System.exit(0);
            }
            // Check if dev argument has been passed
            if(cmd.hasOption("dev")) {
                // If it has, then activate boolean variable for later control
                devBoolean = true;
            }
            // Check if configFile argument has been passed
            if(cmd.hasOption("configFile")) {
                // If it has, then use its value to initialise the respective member variable
                configFileName = cmd.getOptionValue("configFile");
            } else {
                // If not, use the default value to initialise the respective member variable
                configFileName = "src/main/resources/config.properties";
            }
            // Check if outputFolder argument has been passed
            if(cmd.hasOption("outputFolder")) {
                // If it has, then use its value to initialise the respective member variable
                outputFolder = cmd.getOptionValue("outputFolder");
                // If outputFolder does not end with "/", add it
                if (!outputFolder.endsWith("/")) { outputFolder += "/"; }
            } else {
                // If not, use the default value to initialise the respective member variable
                outputFolder = "Results/" + Instant.now().toString().replace(":", "-") + "/";
            }
        }
        catch(ParseException pex) {
            // Catch possible parsing errors
            System.err.println("Parsing failed. Reason: " + pex.getMessage());
            // And print input arguments usage information
            formatter.printHelp( "spatial-housing-model", options );
        }

        // Check if outputFolder directory already exists
        File f = new File(outputFolder);
        if (f.exists() && !devBoolean) {
            // If it does, try removing everything inside (with a warning that requests approval!)
            Scanner reader = new Scanner(System.in);
            System.out.println("\nATTENTION:\n\nThe folder chosen for output, '" + outputFolder + "', already exists and " +
                    "might contain relevant files.\nDo you still want to proceed and erase all content?");
            String reply = reader.next();
            if (!reply.equalsIgnoreCase("yes") && !reply.equalsIgnoreCase("y")) {
                // If user does not clearly reply "yes", then stop the program
                System.exit(0);
            } else {
                // Otherwise, try to erase everything inside the folder
                try {
                    FileUtils.cleanDirectory(f);
                } catch (IOException ioe) {
                    // Catch possible folder cleaning errors
                    System.err.println("Folder cleaning failed. Reason: " + ioe.getMessage());
                }
            }
        } else {
            // If it doesn't, simply create it
            f.mkdirs();
        }

        // Copy config file to output folder
        try {
            FileUtils.copyFileToDirectory(new File(configFileName), new File(outputFolder));
        } catch (IOException ioe) {
            System.err.println("Copying config file to output folder failed. Reason: " + ioe.getMessage());
        }
    }

	/**
	 * @return Simulated time in months
	 */
	static public int getTime() {
		return t;
	}

    /**
     * @return Current month of the simulation
     */
	static public int getMonth() {
		return t%12 + 1;
	}

    private static void setRecordGeneral() {
        creditSupply.setActive(true);
        householdStats.setActive(true);
        housingMarketStats.setActive(true);
        rentalMarketStats.setActive(true);
    }

	private static void setRecordCoreIndicators(boolean recordCoreIndicators) {
	    coreIndicators.setActive(recordCoreIndicators);
	}

	private static void setRecordMicroData(boolean record) { transactionRecorder.setActive(record); }

}
