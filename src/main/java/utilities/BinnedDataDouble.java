package utilities;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Iterator;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;


public class BinnedDataDouble extends BinnedData<Double> {

	/***
	 * Loads data from a .csv file. The file should be in the format
	 * 
	 * bin min, min max, value
	 * 
	 * The first row should be the titles of the columns.
	 * 
	 * @param filename
	 * @throws IOException
	 */
	public BinnedDataDouble(String filename) throws IOException {
		super(0.0,0.0);
		FileReader in = new FileReader(filename);
        BufferedReader buffReader = new BufferedReader(in);
        // Skip initial comment lines
        String line = buffReader.readLine();
        while (line.charAt(0) == '#') {
            line = buffReader.readLine();
        }
        // Pass advanced buffered reader to CSVFormat parser
		Iterator<CSVRecord> records = CSVFormat.EXCEL.parse(buffReader).iterator();
		CSVRecord record;
		if(records.hasNext()) {
			record = records.next();
		    this.setFirstBinMin(Double.valueOf(record.get(0)));
		    this.setBinWidth(Double.valueOf(record.get(1))-firstBinMin);
		    add(Double.valueOf(record.get(2)));
			while(records.hasNext()) {
				record = records.next();
			    add(Double.valueOf(record.get(2)));
			}
		}
	}
	
	public BinnedDataDouble(double firstBinMin, double binWidth) {
		super(firstBinMin, binWidth);
	}
}
