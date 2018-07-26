package data;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import housing.Config;
import housing.Model;

public class Distance {
	
	private static Config config = Model.config; 
	
	private static double[][] distanceMatrix = readDistanceMatrix(config.DISTANCE_BETWEEN_REGIONS);

	public static double[][] readDistanceMatrix(String fileName){

		distanceMatrix = new double[10][10]; // create 2D array large enough to hold the distance matrix
				
		File matrix = new File(fileName);
		
		try(BufferedReader br = new BufferedReader(new FileReader(matrix))){		
			int row = 0;		
			String str = null;		
			int firstDigit = br.read();
			while (firstDigit != -1) {
				str =  (char)firstDigit + br.readLine();
				String distance[] = str.split(" ");
				for (int i = 0; i < distance.length; i++) {
					distanceMatrix[row][i] = Double.parseDouble(distance[i]);
				}
				row++;
				firstDigit = br.read();
			}		
			br.close();		
		}catch (IOException ioe) {
			System.out.println("Exception " + ioe + " while trying to read file '" + fileName + "'");
			ioe.printStackTrace();
			}		
		return distanceMatrix;
		}

	public static double[][] getDistanceMatrix(){
		return distanceMatrix;
	}
}