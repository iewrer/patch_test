package gov.nasa.jpf.regression.output;

import gov.nasa.jpf.regression.cfg.CFG;
import gov.nasa.jpf.regression.cfg.Node;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

public class PrintStatistics {
	
	//each entry contains information about a node in the CFG
	private Vector<String> oldCFGInfo;
	private Vector<String> modCFGInfo;
	
	private String trackCondLoc = "";
	private String trackWriteLoc = "";
	
	public PrintStatistics(){
		oldCFGInfo = new Vector<String>();
		modCFGInfo = new Vector<String>();
	}
	
	public void computeStats(CFG originalCfg,
			CFG modifiedCfg, Set<Integer> trackCondLoc,	
			Set<Integer> trackWriteLoc){
		this.trackCondLoc = trackCondLoc.toString() + "\n";
		this.trackWriteLoc = trackWriteLoc.toString() + "\n";
		processCFG(originalCfg, "orig");
		processCFG(modifiedCfg, "mod");
		
	}
	
	private void processCFG(CFG cfg, String flag){
		Vector<String> info = null;
		if (flag.equalsIgnoreCase("mod"))
			info = modCFGInfo;
		else
			info = oldCFGInfo;
		Node[] nodes = cfg.getNodes();
		for (int i=0; i<nodes.length;i++){
			if (i == 0 || i== nodes.length-2 || i == nodes.length-1){
				//skip these nodes; nothing to see here
			}else{
				boolean controlNode = false;
				if (nodes[i].getSuccessorList().size() > 1)
					controlNode = true;
				String nodeInfo =
					"nodeID=" + nodes[i].getID() + ";" +
					"startLine=" + nodes[i].getStartLineNumber() + ";" +
					"endLine=" + nodes[i].getEndLineNumber() + ";" +
					"controlNode=" + controlNode + ";" +
					"changeTypes=" + nodes[i].getChangeTypes().toString() + "\n";
				info.add(nodeInfo);
			}
		}
	}
	
	public void printCFGInfo(String fName){
		Writer output = null;
		try {
			output = new BufferedWriter(new FileWriter(new File(fName)));
		} catch (IOException e) {
			System.err.println("error while creating the stat file to write");
			e.printStackTrace();
		}
		try {
			output.write("trackCondLoc=" + trackCondLoc + "\n");
			output.write("trackWriteLoc=" + trackWriteLoc + "\n");
			output.write("modified CFG Info \n");
			Iterator<String> itN = modCFGInfo.iterator();
			while (itN.hasNext()){
				output.write(itN.next());
			}
			output.write("\n");
			output.write("original CFG Info \n");
			Iterator<String> itO = oldCFGInfo.iterator();
			while (itO.hasNext()){
				output.write(itO.next());
			}
			output.close();
		} catch (IOException e) {
			System.err.println("Error while writing to the stat file");
			e.printStackTrace();
		}
	}
}
