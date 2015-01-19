package gov.nasa.jpf.regression.tasks;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class RegressionTesting {
	
	ArrayList<String> oldTestListing;
	ArrayList<String> newTestListing;
	
	String file1; 
	String file2; 
	
	int addedTests = 0;
	int selectedTests = 0;
	
	public RegressionTesting(String file1, String file2) {
		oldTestListing = new ArrayList<String>();
		newTestListing = new ArrayList<String>();
		this.file1 = file1;
		this.file2 = file2;
	}
	
	public void checkTests() {
		String curDir =  System.getProperty("user.dir");
		String fileSeparator = System.getProperty("file.separator");
		//.out.println("file1 : " + file1);
		//System.out.println("file2 :" + file2);
		String firstPath = curDir.concat(fileSeparator.concat("testCases").
				concat(fileSeparator).concat(file1));
		String secondPath = curDir.concat(fileSeparator.concat("testCases").
				concat(fileSeparator).concat(file2));
		//System.out.println(firstPath);
		//System.out.println(secondPath);
		readFile(file1, oldTestListing);
		readFile(file2, newTestListing);
		//readFile(firstPath, testInputOne);
		//readFile(secondPath, testInputTwo);
		compareTests();
	}
	
	public void readFile(String fileName, ArrayList<String> tests) {
		BufferedReader bufferedReader = null;
		  try {
			 bufferedReader = new BufferedReader
			  									(new FileReader(fileName));

		} catch (FileNotFoundException e) {
			System.err.println("file not found");
			e.printStackTrace();
		}
		
		String line;
		try {
			while ((line = bufferedReader.readLine()) != null)
			  {
			    //System.out.println(line);
			    if(!line.equals("")) {
			    	//strip off count of dp calls
			    	if (line.contains("-->"))
			    		line = line.substring(0, line.indexOf("-->"));
			    	tests.add(line);
			    }
			  }
		} catch (IOException e) {
			System.err.println("io exception");
			e.printStackTrace();
		}
		  
		  // close the BufferedReader when we're done
		  try {
			bufferedReader.close();
		} catch (IOException e) {
			System.err.println("close file error");
			e.printStackTrace();
		}
	}
	
	public void compareTests() {
		for(int secondIndex = 0 ; secondIndex < newTestListing.size(); 
											secondIndex++) {
			String test = newTestListing.get(secondIndex);
			if(oldTestListing.contains(test)) {
				System.out.println("Selected :" + test);
				this.selectedTests++;
			} else{
				System.out.println("Added :" + test);
				this.addedTests++;
			}
		}
		System.out.println("Tests Selected: " + this.selectedTests);
		System.out.println("Tests Added: " + this.addedTests);

	}
	
	
	public static void main(String [] args) {
		if(args.length < 2) {
			System.out.println("usage: <file listing old tests> <file listing new tests> ");
			System.exit(1);
		}
		
		RegressionTesting rt = new 
					RegressionTesting(args[0], args[1]);
		rt.checkTests();
		
		
	}
	
}