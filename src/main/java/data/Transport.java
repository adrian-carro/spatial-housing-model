package data;

import housing.Config;
import utilities.Matrix;

public class Transport {
	
	private Matrix travelTimeMatrix;  // a matrix storing the the travel time between any region
	private Matrix travelFeeMatrix; // a matrix storing the the travel fee between any region
	
	public Transport(Config config){
		readTravelTimeMatrix(config.DATA_TRAVEL_TIME);
		readTravelFeeMatrix(config.DATA_TRAVEL_FEE);
	}
	
	public void readTravelTimeMatrix(String inputFile){		
		this.travelTimeMatrix=new Matrix(inputFile);		
	}
	
	public void readTravelFeeMatrix(String inputFile){		
		this.travelFeeMatrix=new Matrix(inputFile);		
	}
	
	public Matrix getTravelTimeMatrix(){		
		return travelTimeMatrix;
	}
	
	public Matrix getTravelFeeMatrix(){		
		return travelFeeMatrix;
	}
	
	public void printTravelTimeMatrix(){
		travelTimeMatrix.printMatrixOnScreen();
	}
	public void printTravelFeeMatrix(){
		travelFeeMatrix.printMatrixOnScreen();
	}
	
	/**
	 * this method is only for test purpose
	 */
	public void adjustTransportForTest(){
		double newVal=100.0;
		double [][] matrixData=travelFeeMatrix.getMatrixData();
		for(int i=0;i<matrixData.length;i++){
			for(int j=0;j<matrixData[0].length;j++){
				if(i==j){
					matrixData[i][j]=0;
				}
				else{
					matrixData[i][j]=newVal;
				}
			}
		}	
	}
	
}
