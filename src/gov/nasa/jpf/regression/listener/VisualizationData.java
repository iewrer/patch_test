package gov.nasa.jpf.regression.listener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class VisualizationData {
	
	public static ArrayList<LocationData> locations = new ArrayList<LocationData>();
	public static HashMap<String, Boolean> branchConditions = new HashMap<String,Boolean>(); //not currently used
	
	public static Map<String,Set<Integer>> trackCondLoc = new HashMap<String,Set<Integer>>();
	public static Map<String,Set<Integer>> trackWriteLoc = new HashMap<String,Set<Integer>>();
	
	public static Map<String,Set<Integer>> changedLocs = new HashMap<String,Set<Integer>>();
	public static Map<String,Set<Integer>> addedLocs = new HashMap<String,Set<Integer>>();
	
	public static void initializeLocations() {
		locations = new ArrayList<LocationData>();
		branchConditions = new HashMap<String,Boolean>();
		trackCondLoc = new HashMap<String,Set<Integer>>();
		trackWriteLoc = new HashMap<String,Set<Integer>>();
		changedLocs = new HashMap<String,Set<Integer>>();
		addedLocs = new HashMap<String,Set<Integer>>();
	}
	
    public static void addLocation(LocationData loc) {
        locations.add(loc);
    }
    
	public static ArrayList<LocationData> getExecutedInstructions() {
		return locations;
	}
	
	public static HashMap<String, Boolean> getBranchConditions(){
		return branchConditions;
	}
	
	public static void addBranchCondition(LocationData loc, Boolean branchCondition) {
		String index = loc.getClassName().concat(loc.getMethodName()).concat(loc.getLocation().toString());
		branchConditions.put( index , branchCondition);
	}
	
    public static void addTrackCondLoc(String mName, Set<Integer> locs){
    	trackCondLoc.put(mName, locs);
    }
    
    public static Set<Integer> getTrackCondLoc(String mName){
    	return trackCondLoc.get(mName);
    }
    
    public static Map<String,Set<Integer>> getTrackCondLocMap(){
    	return trackCondLoc;
    }
    
    public static void addTrackWriteLoc(String mName,Set<Integer> locs){
    	trackWriteLoc.put(mName,locs);
    }
    
    public static Set<Integer> getTrackWriteLoc(String mName){
    	return trackWriteLoc.get(mName);
    }
    
    public static Map<String,Set<Integer>> getTrackWriteLocMap(){
    	return trackWriteLoc;
    }
    
    public static Map<String,Set<Integer>> getChangedLocsMap(){
    	return changedLocs;
    }
    
    public static void addChangedLocs(String mName, Set<Integer> locs){
    	changedLocs.put(mName, locs);
    }
    
    public static Set<Integer> getChangedLocs(String mName){
    	return changedLocs.get(mName);
    }
    
    public static Map<String,Set<Integer>> getAddedLocsMap(){
    	return addedLocs;
    }
    
    public static void addAddededLocs(String mName, Set<Integer> locs){
    	addedLocs.put(mName, locs);
    }
    
    public static Set<Integer> getAddedLocs(String mName){
    	return addedLocs.get(mName);
    }
}
