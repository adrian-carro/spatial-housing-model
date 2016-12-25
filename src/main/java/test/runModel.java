package test;

import housing.Model;


public class runModel {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
	   String configFileName="src/main/resources/config.properties";
	   String outputFolder="./Test/output/";
       Model model=new Model(configFileName,outputFolder);
       model.run();


	}

}
