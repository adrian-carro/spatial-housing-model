package utilities;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Matrix {
	
	
	private Map<String,Integer> rowIndices;
	private Map<String,Integer> columnIndices;
	private double [][] matrixData;
	
	public Matrix(){
		
	}
	
	public Matrix(Map<String,Integer> rowIndices,Map<String,Integer> columnIndices,double [][] matrixData){
		this.rowIndices=rowIndices;
		this.columnIndices=columnIndices;
		this.matrixData=matrixData;
	}
	
//	/**
//	 * this method is only for test purpose. 
//	 * @param rowIndices
//	 * @param columnIndices
//	 * @param lowerLimit
//	 * @param upperLimit
//	 */
//	public Matrix(Map<String,Integer> rowIndices,Map<String,Integer> columnIndices, double lowerLimit,double upperLimit){
//		this.rowIndices=rowIndices;
//		this.columnIndices=columnIndices;
//		MersenneTwister rand=Model.rand;
//		this.matrixData=new double[rowIndices.size()][columnIndices.size()];
//		for(int i=0;i<rowIndices.size();i++){
//			for(int j=0;j<columnIndices.size();j++){
//				double data=lowerLimit+rand.nextDouble()*(upperLimit-lowerLimit);
//				this.matrixData[i][j]=data;
//			}
//		}
//	}
	
//	public void printMatrix(){
//		
	
//	}
	
//	public static void main(String[] args) {
//		String inputFile="src/main/resources/travelFee.csv";
//		new Matrix().readTypcialMatrix(inputFile);
//	}
	
	
	public Matrix(String inputFile){
		readTypcialMatrix(inputFile);
	}

	
	//TODO
	public void readTypcialMatrix(String inputFile){
		
		this.rowIndices=new HashMap<String,Integer>();
		this.columnIndices=new HashMap<String,Integer>();		
		List<double[]> dataList=new ArrayList<double[]>();
		
		int lineId=0;
        try (BufferedReader buffReader = new BufferedReader(new FileReader(inputFile))) {
            String line = buffReader.readLine();
            while (line != null) {
                if (line.charAt(0) != '#') {
                    try {
                    	lineId++;
                    	String [] arr=line.split(",");
                    	if(lineId==1){                   		
                    		for(int i=1;i<arr.length;i++){
                    			this.rowIndices.put(arr[i], i-1);
                    		}
                    	}
                    	else{
                    		this.columnIndices.put(arr[0], lineId-2);
                    		double [] dataArr=new double[arr.length-1];
                    		for(int i=1;i<arr.length;i++){
                    			double data=Double.parseDouble(arr[i]);   
                    			dataArr[i-1]=data;
                    		}
                    		dataList.add(dataArr);
                    	}                       
                        
                    } catch (NumberFormatException nfe) {
                        System.out.println("Exception " + nfe + " while trying to parse " +
                                line.split(",")[0] + " for an double");
                        nfe.printStackTrace();
                    }
                }
                line = buffReader.readLine();
            }
        } catch (IOException ioe) {
            System.out.println("Exception " + ioe + " while trying to read file '" + inputFile + "'");
            ioe.printStackTrace();
        }
        
        // coverting dataList into matrixData
        this.matrixData =new double [this.columnIndices.size()][this.rowIndices.size()];
        for(int i=0;i<dataList.size();i++){
        	double [] dataArr=dataList.get(i);
        	for(int j=0;j<dataArr.length;j++){
        		matrixData[i][j]=dataArr[j];
        	}
        }         
	}
	
	/**
	 * This method is used to get the specific data with the given row and column indices. 
	 * @param rowIndexStr: e.g., regionID (region1)
	 * @param columnIndexStr: e.g., regionID (region2)
	 * @return
	 */
	public double getData(String rowIndexStr,String columnIndexStr){
		int rowIndexInt=rowIndices.get(rowIndexStr);
		int columnIndexInt=columnIndices.get(columnIndexStr); 
		double data=matrixData[rowIndexInt][columnIndexInt];
		return data;
	}
	
	

	

}
