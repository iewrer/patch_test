package gov.nasa.jpf.regression.tasks;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

public class DiSEResultsProcessor {
		
	private String seResultsFile=""; 
	private String cfgInfoFile=""; 
	private String seOldResultsFile="";
	
	private Vector<PathResults> newPaths;
	private Vector<PathResults> oldPaths;
	private Vector<CFGNodeInfo> oldNodes;
	private Vector<CFGNodeInfo> newNodes;
	private Map<Integer,Integer> oldToNewNodeMap;
	
	private int minSolverCallsOld = 0;
	private int maxSolverCallsOld = 0;
	private int totalSolverCallsOld = 0;
	private int minSolverCallsNew = 0;
	private int maxSolverCallsNew = 0;
	private int totalSolverCallsNew = 0;
	
	private static boolean debug = true;
	
	public DiSEResultsProcessor() {
		newPaths = new Vector<PathResults>();
		oldPaths = new Vector<PathResults>();
		oldNodes = new Vector<CFGNodeInfo>();
		newNodes = new Vector<CFGNodeInfo>();
		oldToNewNodeMap = new HashMap<Integer,Integer>();
	}
	
	public int getMinSolverCallsOld() {
		return minSolverCallsOld;
	}

	public int getMaxSolverCallsOld() {
		return maxSolverCallsOld;
	}

	public int getTotalSolverCallsOld() {
		return totalSolverCallsOld;
	}

	public int getMinSolverCallsNew() {
		return minSolverCallsNew;
	}

	public int getMaxSolverCallsNew() {
		return maxSolverCallsNew;
	}

	public int getTotalSolverCallsNew() {
		return totalSolverCallsNew;
	}

	public Vector<PathResults> getNewPathResults(){
		return this.newPaths;
	}
	
	public Vector<PathResults> getOldPathResults(){
		return this.oldPaths;
	}
	
	public Vector<CFGNodeInfo> getOldCFGInfo(){
		return oldNodes;
	}
	
	public Vector<CFGNodeInfo> getNewCFGInfo(){
		return newNodes;
	}
	
	public void printPathResults(String whichFile){
		Iterator<PathResults> it;
		if (whichFile.equalsIgnoreCase("new")){
			System.out.println("Paths in new version...");
			it = newPaths.iterator();
			while (it.hasNext()){
				System.out.println(it.next().toString());
			}
			System.out.println("Min Solver Calls: " + minSolverCallsNew);
			System.out.println("Max Solver Calls: " + maxSolverCallsNew);
			System.out.println("Total Solver Calls: " + totalSolverCallsNew);
			
		}else{
			System.out.println("Paths in old version...");
			it = oldPaths.iterator();
			while (it.hasNext()){
				System.out.println(it.next().toString());
			}
			System.out.println("Min Solver Calls: " + minSolverCallsOld);
			System.out.println("Max Solver Calls: " + maxSolverCallsOld);
			System.out.println("Total Solver Calls: " + totalSolverCallsOld);
		}

	}
	
	//TODO update to print all CFG info (both cfgs)
	public void printCFGInfo(){
		System.out.println("original CFG");
		Iterator<CFGNodeInfo> it = oldNodes.iterator();
		while (it.hasNext()){
			System.out.println(it.next().toString());
		}
		System.out.println("modified CFG");
		Iterator<CFGNodeInfo> it2 = newNodes.iterator();
		while (it2.hasNext()){
			System.out.println(it2.next().toString());
		}
	}
	
	public void loadOldSEResults(String fileName){
		this.seOldResultsFile = fileName;
		loadSEResults("old");
	}
	
	public void loadNewSEResults(String fileName){
		this.seResultsFile = fileName;
		loadSEResults("new");
	}
	
	public void loadCFGFile(String fileName){
		this.cfgInfoFile = fileName;
		loadCFGInfo();
	}
	
	public void loadSEResults(String whichFile){
		BufferedReader bufferedReader = null;
		  try {
			  if (whichFile.equalsIgnoreCase("new"))
				  bufferedReader = new BufferedReader(new FileReader(seResultsFile));
			  else
				  bufferedReader = new BufferedReader(new FileReader(seOldResultsFile));
		} catch (FileNotFoundException e) {
			System.err.println("Symbolic Execution results file not found");
			e.printStackTrace();
		}
		String line;
		try {
			while ((line = bufferedReader.readLine()) != null){
			    if(!line.equals("")) {
			    	String delims = "[;]";
			    	String[] tokens = line.split(delims);
			    	PathResults path = new PathResults();
			    	path.setTestInputs(tokens[0].substring(tokens[0].indexOf("=")+1));
			    	path.setPc(tokens[1].substring(tokens[1].indexOf("=")+1));
			    	path.setTestResults(tokens[2].substring(tokens[2].indexOf("=")+1));
			    	String solverCalls = (tokens[3].substring(tokens[3].indexOf("=")+1));
			    	Integer count = Integer.parseInt(solverCalls);
			    	path.setSolverCalls(count);
			    	if (whichFile.equalsIgnoreCase("new")){
			    		if (count > 0){
						  if (count > maxSolverCallsNew)
							  maxSolverCallsNew = count;
						  if (minSolverCallsNew == 0 || count < minSolverCallsNew)
							  minSolverCallsNew = count;
						  totalSolverCallsNew = totalSolverCallsNew + count;
					  }
			    	}else{
			    		if (count > 0){
						  if (count > maxSolverCallsOld)
							  maxSolverCallsOld = count;
						  if (minSolverCallsOld == 0 || count < minSolverCallsOld)
							  minSolverCallsOld = count;
						  totalSolverCallsOld = totalSolverCallsOld + count;
					  }
			    	}
			    	String linesExecuted = tokens[4].substring(tokens[4].indexOf("=")+2,tokens[4].length()-1);
			    	String delims2 = "[,]";
			    	String[] numbers = linesExecuted.split(delims2);
			    	Vector<Integer> lines = new Vector<Integer>();
			    	for (int i=0; i<numbers.length; i++){
			    		lines.add(new Integer(numbers[i].trim()));
			    	}
			    	path.setStmtsExecuted(lines);
			    	String posExecuted = tokens[5].substring(tokens[5].indexOf("=")+2,tokens[5].length()-1);
			    	numbers = posExecuted.split(delims2);
			    	Vector<Integer> pos = new Vector<Integer>();
			    	for (int i=0; i<numbers.length; i++){
			    		pos.add(new Integer(numbers[i].trim()));
			    	}
			    	path.setPosExecuted(pos);
			    	if (whichFile.equalsIgnoreCase("new"))
			    		newPaths.add(path);
			    	else
			    		oldPaths.add(path);
			    }
			  }
		} catch (IOException e) {
			e.printStackTrace();
		}
		// close the BufferedReader when we're done
		try {
			bufferedReader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private Set<Integer> processAffectedControl(String line){
		Set<Integer> affectedPos = new HashSet<Integer>();
		line = line.substring(line.indexOf("=")+2,line.length()-1);
		String[] positions = line.split("[,]");
		for (int i=0; i<positions.length; i++){
			affectedPos.add(Integer.parseInt(positions[i].trim()));
		}
		return affectedPos;
	}
	
	private Set<Integer> processAffectedWrite(String line){
		Set<Integer> affectedPos = new HashSet<Integer>();
		line = line.substring(line.indexOf("=")+2,line.length()-1);
		String[] positions = line.split("[,]");
		for (int i=0; i<positions.length; i++){
			affectedPos.add(Integer.parseInt(positions[i].trim()));
		}
		return affectedPos;
	}
	
	private void processCfgEntry(CFGNodeInfo cfg, Map <Integer,String> posMap, 
					Set<Integer> affectedControlPos,
					Set<Integer> affectedWritePos, String line){
		
		String delims = "[;]";
		String[] tokens = line.split(delims);
    	String id = (tokens[0].substring(tokens[0].indexOf("=")+1));
    	cfg.setId(Integer.parseInt(id));
    	String startLine = (tokens[1].substring(tokens[1].indexOf("=")+1));
    	cfg.setStartLine(Integer.parseInt(startLine));			    	
    	String endLine = (tokens[2].substring(tokens[2].indexOf("=")+1));
    	cfg.setEndLine(Integer.parseInt(endLine));	
    	String cNode = (tokens[3].substring(tokens[3].indexOf("=")+1));
    	if (cNode.equalsIgnoreCase("true"))
    		cfg.setControlNode(true);
    	else
    		cfg.setControlNode(false);
    	String changeTypes = tokens[4].substring(tokens[4].indexOf("=")+2,tokens[4].length()-1);
    	String delims2 = "[,]";
    	String[] pairs = changeTypes.split(delims2);	
    	for (int i=0; i<pairs.length; i++){
    		String[] pair = pairs[i].split("[=]");
    		Integer pos = Integer.parseInt(pair[0].trim());
    		if (affectedControlPos.contains(pos))
    			cfg.addControlPos(pos);
    		if (affectedWritePos.contains(pos))
    			cfg.addWritePos(pos);
    		int type = Integer.parseInt(pair[1]);
    		String chgType = "";
    		if (type == 0)
    			chgType = "unchanged";
    		else if (type == 1)
    			chgType = "changed";
    		else if (type == 2)
    			chgType = "added";
    		else if (type == 3)
    			chgType = "removed";
    		else
    			chgType = "unknown";
    		posMap.put(pos,chgType);
    	}
	}
	
	public void loadCFGInfo(){
		BufferedReader bufferedReader = null;
		Set<Integer> affectedControlPos = new HashSet<Integer>();
		Set<Integer> affectedWritePos = new HashSet<Integer>();
		  try {
			 bufferedReader = new BufferedReader(new FileReader(cfgInfoFile));
		} catch (FileNotFoundException e) {
			System.err.println("Symbolic Execution results file not found");
			e.printStackTrace();
		}
		String line;
		try {
			while ((line = bufferedReader.readLine()) != null){
			    if(!line.equals("")) {
			    	if (line.startsWith("trackCondLoc"))
			    		affectedControlPos = processAffectedControl(line);
			    	else if (line.startsWith("trackWriteLoc"))
			    		affectedWritePos = processAffectedWrite(line);
			    	else if (line.startsWith("modified")){
			    		line = bufferedReader.readLine(); //get first line
			    		while(!line.equals("")){
			    			CFGNodeInfo cfg = new CFGNodeInfo();
				    		Map <Integer,String> posMap = new HashMap<Integer,String>();
			    			processCfgEntry(cfg, posMap, affectedControlPos,
			    					affectedWritePos,line);
				    		cfg.setPosToChangeTypeMap(posMap);
					    	newNodes.add(cfg);
					    	line = bufferedReader.readLine();
			    		}
			    	}else if (line.startsWith("original")){
			    		line = bufferedReader.readLine(); //get first line
			    		while(!line.equals("")){
			    			CFGNodeInfo cfg = new CFGNodeInfo();
				    		Map <Integer,String> posMap = new HashMap<Integer,String>();
			    			processCfgEntry(cfg, posMap, affectedControlPos,
			    					affectedWritePos,line);
				    		cfg.setPosToChangeTypeMap(posMap);
					    	oldNodes.add(cfg);
			    			line = bufferedReader.readLine();
			    		}

			    	}
			    }
			  }
		} catch (IOException e) {
			e.printStackTrace();
		}
		// close the BufferedReader when we're done
		try {
			bufferedReader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private boolean checkArgs(String[] args){
		boolean valid = true;
		if (args.length < 2){
			System.out.println("usage: -n <path to new SE results> -c <path to CFG info>" +
			" -o <path to previous SE results>");
			valid=false;
		}else{
			for (int i=0; i<args.length; i++){
				if (args[i].startsWith("-c")) {
					cfgInfoFile = args[i+1];
					i++;
				}else if (args[i].startsWith("-o")) {
					seOldResultsFile = args[i+1];
					i++;
				}else if (args[i].startsWith("-n")) {
					seResultsFile = args[i+1];
					i++;
				}else if (args[i].startsWith("-d"))
					debug = true;
			}
		}
		return valid;
	}
	
	
	public static void main(String [] args) {
		DiSEResultsProcessor processor = new DiSEResultsProcessor();
		if (!processor.checkArgs(args))
			System.exit(1);
		
		if (!processor.seResultsFile.equalsIgnoreCase("")){
			processor.loadSEResults("new");
			if (debug)
				processor.printPathResults("new");
		}
		if (!processor.seOldResultsFile.equalsIgnoreCase("")){
			processor.loadSEResults("old");
			if (debug)
				processor.printPathResults("old");
		}
		if (!processor.cfgInfoFile.equalsIgnoreCase("")){
			processor.loadCFGInfo();
			if (debug)
				processor.printCFGInfo();
		}
	}
	
}