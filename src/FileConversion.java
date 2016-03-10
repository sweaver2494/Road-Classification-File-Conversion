/*
 *
 * @author Scott Weaver
 */
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

/******A new feature file is created using files in RawData folder.*****/

public class FileConversion {
    
	public static void main(String[] args) {
		
		String featureFolderPath = "Data/FeatureFiles/";
    	(new File(featureFolderPath)).mkdirs();
    	
    	String rawDataFolderPath = "Data/RawData/";
    	File rawDataFolder = new File(rawDataFolderPath);

		//Ensure files have unique names by appending an integer after conflicting filenames.
		int i=0;
		String featureFilePath  = featureFolderPath + "features_0.csv";
		while ((new File(featureFilePath)).exists()) {
			i++;
			featureFilePath  = featureFolderPath + "features_" + i + ".csv";
		}

    	//values contains the values of each sensor type
        HashMap<String, ArrayList<Double>> values = new HashMap<String, ArrayList<Double>>();
        //rms is the root-means-squared of all values of each sensor type
        HashMap<String, Double> rms = new HashMap<String, Double>();
        //avg is the average of all values of each sensor type
        HashMap<String, Double> avg = new HashMap<String, Double>();
        //sdv is the standard deviation of all values of each sensor type
        HashMap<String, Double> sdv = new HashMap<String, Double>();
        
        HashMap<String, Double> maxValues = new HashMap<String, Double>();
        HashMap<String, Double> minValues = new HashMap<String, Double>();
        
        
    	//Calculate sensor average & variance for each feature, then write to features file
		try {
	    	BufferedWriter featureFileBuffer = new BufferedWriter(new FileWriter(featureFilePath, true));
	    	
	    	int row = 0;
	    	if (rawDataFolder.isDirectory()) {
	    		for (File rawDataFile : rawDataFolder.listFiles()) {
	    			
	    			//read sensor data file and list all values for each feature
	    	        String classification = readRawData(rawDataFile, values, minValues, maxValues);
	    	        
	    	        //calculate the average, root-means-squared, and standard deviation of all values for each feature
	    	        calculateStatistics(values, minValues, maxValues, avg, rms, sdv);
	    	        
	    	        //write feature column headers only once
	    	        if (row == 0) {
	    	        	writeFeatureFileHeaders(featureFileBuffer, avg, rms, sdv);
	    	        }
	    	        
	    	        //write single row of feature data, one column per feature
	    	        writeFeatureFile(featureFileBuffer, avg, rms, sdv, classification);
	    	        
	    	        //set all default values for each key to reuse containers with the same key ordering
	    	        for (String key : values.keySet()) {
	    	        	ArrayList<Double> tempArrayList = values.get(key);
	    	        	tempArrayList.clear();
	    	        	
	    	        	avg.replace(key, (double)0);
	    	        	rms.replace(key, (double)0);
	    	        	sdv.replace(key, (double)0);
	    	        }
	    	        
	    	        row++;
	    		}
	    	}
	    	featureFileBuffer.close();
	    	
		} catch (IOException e) {
			System.err.println("IO Exception: " + e.getMessage());
		}
		
		System.out.println("Feature File Path " + featureFilePath);
	}
	
    //Read rawData files and record all values for each feature type
    public static String readRawData(File rawDataFile, HashMap<String, ArrayList<Double>> values, HashMap<String, Double> minValues, HashMap<String, Double> maxValues) {
    	String classification = "";
    	try {
	        BufferedReader rawDataBuffer = new BufferedReader(new FileReader(rawDataFile));
	        String line = rawDataBuffer.readLine();
	        //read classification from first row
	        if (line != null) {
	        	classification = line;
	        	line = rawDataBuffer.readLine();
	        }
	        //read all data from file
	        while (line != null) {
	        	String dataCompsStr[] = line.split(",");
	        	String key = dataCompsStr[0];
	        	double val = Double.parseDouble(dataCompsStr[2]);
	        	
	        	if (values.containsKey(key)) {
	        		ArrayList<Double> temp = values.get(key);
	        		temp.add(val);
	        		
	        		if (val < minValues.get(key)) {
	        			minValues.put(key, val);
	        		}
	        		if (val > maxValues.get(key)) {
	        			maxValues.put(key, val);
	        		}
	        	} else {
	        		ArrayList<Double> temp = new ArrayList<Double>();
	        		temp.add(val);
	        		values.put(key, temp);
	        		
	        		minValues.put(key, val);
	        		maxValues.put(key, val);
	        	}
	        	
	        	line = rawDataBuffer.readLine();
	        }
	        rawDataBuffer.close();
		} catch (IOException e) {
			System.err.println("Cannot read raw data file: " + rawDataFile.getAbsolutePath());
		}
    	return classification;
    }
    
    //normalize value from 0 to 1
    private static double normalizeValue(double value, double min, double max) {
    	double normalized = (value - min) / (max - min);

    	return normalized;
    }
    
    public static void calculateStatistics(HashMap<String, ArrayList<Double>> values, HashMap<String, Double> minValues, HashMap<String, Double> maxValues, HashMap<String, Double> avg, HashMap<String, Double> rms, HashMap<String, Double> sdv) {
        for (String key : values.keySet()) {
        	//calculate average for each reading (value) for each sensor type (key)
        	for (double val : values.get(key)) {
        		
        		//normalize val from 0 to 1
        		val = normalizeValue(val, minValues.get(key), maxValues.get(key));
        		
        		if (avg.containsKey(key)) {
                	avg.put(key, avg.get(key) + val);
                } else {
                	avg.put(key, val);
                }
        	}
        	double keyAvg = avg.get(key) / values.get(key).size();
        	avg.put(key, keyAvg);
        	
        	//calculate root-means-squared for each reading (value) for each sensor type (key)
        	//root-means-squared is square root of the average of squared values
        	for (double val : values.get(key)) {
        		
        		//normalize val from 0 to 1
        		val = normalizeValue(val, minValues.get(key), maxValues.get(key));
        		
        		if (rms.containsKey(key)) {
        			rms.put(key, rms.get(key) + Math.pow(val, 2));
        		} else {
                   	rms.put(key, Math.pow(keyAvg, 2));
                }
        	}
        	double keyRms = rms.get(key) / values.get(key).size();
        	keyRms = Math.sqrt(keyRms);
        	rms.put(key, keyRms);
        	
        	//calculate standard deviation for each reading (value) for each sensor type (key)
        	//standard deviation is the square root of the average of the squared differences from the mean
        	for (double val : values.get(key)) {
        		
        		//normalize val from 0 to 1
        		val = normalizeValue(val, minValues.get(key), maxValues.get(key));
        		
        		if (sdv.containsKey(key)) {
        			sdv.put(key, sdv.get(key) + Math.pow(val - keyAvg, 2));
        		} else {
                   	sdv.put(key, Math.pow(val - keyAvg, 2));
                }
        	}
        	double keySdv = sdv.get(key) / values.get(key).size();
        	keySdv = Math.sqrt(keySdv);
        	sdv.put(key, keySdv);
        }
    }
    
    //assumes values exist for sum, avg, and var
    public static void writeFeatureFileHeaders(BufferedWriter featureFileBuffer, HashMap<String, Double> avg, HashMap<String, Double> rms, HashMap<String, Double> sdv) {
		try {
			featureFileBuffer.write("classification,");
			
	    	//Write column headers.
			
			for (String key : avg.keySet()) {
				String header = key + "_avg";
				featureFileBuffer.write(header + ",");
			}
			
			
			for (String key : rms.keySet()) {
				String header = key + "_rms";
				featureFileBuffer.write(header + ",");
			}
			
			int i = 0;
			for (String key : sdv.keySet()) {
				i++;
				String header = key + "_sdv";
				featureFileBuffer.write(header);
				
				//don't write comma after last feature
				if (i < sdv.keySet().size()) {
					featureFileBuffer.write(",");
				}
			}
			featureFileBuffer.newLine();
			featureFileBuffer.flush();
		} catch (IOException e) {
			System.err.println("Cannot write feature file headers.");
		}
    }
    
    public static void writeFeatureFile(BufferedWriter featureFileBuffer, HashMap<String, Double> avg, HashMap<String, Double> rms, HashMap<String, Double> sdv, String classification) {
    	try {
    		featureFileBuffer.write(classification + ",");
    		
	    	//Write all values for single row.
    		
			for (String key : avg.keySet()) {
				double val = avg.get(key);
				featureFileBuffer.write(Double.toString(val) + ",");
			}
			
			for (String key : rms.keySet()) {
				double val = rms.get(key);
				featureFileBuffer.write(Double.toString(val) + ",");
			}
			int i = 0;
			for (String key : sdv.keySet()) {
				i++;
				double val = sdv.get(key);
				featureFileBuffer.write(Double.toString(val));
				
				//don't write comma after last feature
				if (i < sdv.keySet().size()) {
					featureFileBuffer.write(",");
				}
			}
			featureFileBuffer.newLine();
			featureFileBuffer.flush();
    	} catch (IOException e) {
    		System.err.println("Cannot write to feature file.");
    	}
    }

}
