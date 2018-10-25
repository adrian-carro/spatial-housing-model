package data;

import housing.Config;
import housing.Model;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

/**************************************************************************************************
 * Class to read data on transport: a matrix of commuting times, and a matrix of commuting fees
 * between the regions
 *
 * @author Adrian Carro
 *
 *************************************************************************************************/

public class Transport {

    //------------------//
    //----- Fields -----//
    //------------------//

    private static Config config = Model.config; // Passes the Model's configuration parameters object to a private field
    // Reads and stores the matrix of commuting times between regions as a static 2D ArrayList of doubles
	private static ArrayList<ArrayList<Double>> commutingTimeMatrix = readMatrix(config.DATA_COMMUTING_TIMES);
    // Reads and stores the matrix of commuting fees between regions as a static 2D ArrayList of doubles
    private static ArrayList<ArrayList<Double>> commutingFeeMatrix = readMatrix(config.DATA_COMMUTING_FEES);

    //-------------------//
    //----- Methods -----//
    //-------------------//

	/**
	 * Method to read a matrix of doubles from a file.
	 *
	 * @param fileName String with name of file (address inside source folder)
	 * @return matrix 2D ArrayList of doubles with the values read from the file
	 */
	private static ArrayList<ArrayList<Double>> readMatrix(String fileName) {
		ArrayList<ArrayList<Double>> matrix = new ArrayList<>();
		// Try-with-resources statement
		try (BufferedReader buffReader = new BufferedReader(new FileReader(fileName))) {
			String line = buffReader.readLine();
			while (line != null) {
				if (line.charAt(0) != '#') {
					ArrayList<Double> lineValues = new ArrayList<>();
					for (String str: line.split(",")) {
						try {
							lineValues.add(Double.parseDouble(str.trim()));
						} catch (NumberFormatException nfe) {
							System.out.println("Exception " + nfe + " while trying to parse " +
                                    line.split(",")[1] + " for an double");
							nfe.printStackTrace();
						}
					}
					matrix.add(lineValues);
				}
				line = buffReader.readLine();
			}
		} catch (IOException ioe) {
			System.out.println("Exception " + ioe + " while trying to read file '" + fileName + "'");
			ioe.printStackTrace();
		}
		// Check that the matrix is squared
        if (matrix.size() != matrix.get(0).size()) {
            System.out.println("Matrix at " + fileName + " is not squared");
            System.exit(0);
        }
		return matrix;
	}

    //----- Getter/setter methods -----//

	public static ArrayList<ArrayList<Double>> getCommutingTimeMatrix(int numberOfRegions) {
	    // First check if the number of regions passed as input (derived from reading the population per region file) is
        // the same as the number of regions read from the commuting times file
	    if (numberOfRegions != commutingTimeMatrix.size()) {
            System.out.println("Number of regions at population file, " + numberOfRegions +
                    ", incoherent with the number of regions at the commuting times file, "
                    + commutingTimeMatrix.size());
            System.exit(0);
        }
        return commutingTimeMatrix;
    }

    public static ArrayList<ArrayList<Double>> getCommutingFeeMatrix(int numberOfRegions) {
        // First check if the number of regions passed as input (derived from reading the population per region file) is
        // the same as the number of regions read from the commuting fees file
        if (numberOfRegions != commutingFeeMatrix.size()) {
            System.out.println("Number of regions at population file, " + numberOfRegions +
                    ", incoherent with the number of regions at the commuting fees file, "
                    + commutingFeeMatrix.size());
            System.exit(0);
        }
        return commutingFeeMatrix;
    }
}