//
// Copyright (C) 2007 United States Government as represented by the
// Administrator of the National Aeronautics and Space Administration
// (NASA).  All Rights Reserved.
//
// This software is distributed under the NASA Open Source Agreement
// (NOSA), version 1.3.  The NOSA has been approved by the Open Source
// Initiative.  See the file NOSA-1.3-JPF at the top of the distribution
// directory tree for the complete NOSA document.
//
// THE SUBJECT SOFTWARE IS PROVIDED "AS IS" WITHOUT ANY WARRANTY OF ANY
// KIND, EITHER EXPRESSED, IMPLIED, OR STATUTORY, INCLUDING, BUT NOT
// LIMITED TO, ANY WARRANTY THAT THE SUBJECT SOFTWARE WILL CONFORM TO
// SPECIFICATIONS, ANY IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR
// A PARTICULAR PURPOSE, OR FREEDOM FROM INFRINGEMENT, ANY WARRANTY THAT
// THE SUBJECT SOFTWARE WILL BE ERROR FREE, OR ANY WARRANTY THAT
// DOCUMENTATION, IF PROVIDED, WILL CONFORM TO THE SUBJECT SOFTWARE.
//

package gov.nasa.jpf.regression.output;

//import gov.nasa.jpf.Config;
import gov.nasa.jpf.regression.analysis.AnalyzeInterProceduralDiff;
import gov.nasa.jpf.regression.analysis.AnalyzeIntraProceduralDiff;
import gov.nasa.jpf.regression.analysis.ComputeInterProceduralDiff;
import gov.nasa.jpf.regression.analysis.ComputeIntraProceduralDiff;
import gov.nasa.jpf.regression.ast.MethodASTInfo;
import gov.nasa.jpf.regression.cfg.CFG;
import gov.nasa.jpf.regression.cfg.Edge;
import gov.nasa.jpf.regression.cfg.Node;
import gov.nasa.jpf.regression.data.GlobalOperation;
import gov.nasa.jpf.regression.listener.VisualizationData;
import gov.nasa.jpf.util.Pair;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import jpf_diff.Control;
import jpf_diff.Data;
import jpf_diff.Dependency;

public class PrintToDot {

	String outputDir;
	Writer output;
	FileWriter fileWriter;
	String outputDebug;
	int nodeNum = 0;


	HashMap<String, Integer> dNodes;
	HashMap<Integer, GlobalOperation> operations;
	HashMap<Integer, ArrayList<Integer>> dEdges;
	ArrayList<Integer> changedNodes = new ArrayList<Integer>();
	ArrayList<Integer> condNodes = new ArrayList<Integer>();
	ArrayList<Integer> writeNodes = new ArrayList<Integer>();
	ArrayList<Integer> assertNodes = new ArrayList<Integer>();
	ArrayList<Integer> callNodes = new ArrayList<Integer>();
	ArrayList<Integer> retNodes = new ArrayList<Integer>();

	/* Interprocedural Data Structures */
	// Each MethodName is mapped to its set of CFGNodes
	Map<String, Map<Integer, String>> allCFGNodes =
		new HashMap<String, Map<Integer, String>>();
	// Each MethodName is mapped to its ArrayList of ChangedNodes
	Map<String, ArrayList<Integer>> allChangedNodes =
		new HashMap<String, ArrayList<Integer>>();
	// Each MethodName is mapped to its ArrayList of CondNodes
	Map<String, ArrayList<Integer>> allCondNodes =
		new HashMap<String, ArrayList<Integer>>();
	// Each MethodName is mapped to its ArrayList of WriteNodes
	Map<String, ArrayList<Integer>> allWriteNodes =
		new HashMap<String, ArrayList<Integer>>();
	//Each MethodName is mapped to its ArrayList of CallNodes
	Map<String, ArrayList<Integer>> allCallNodes =
		new HashMap<String, ArrayList<Integer>>();
	//Each MethodName is mapped to its ArrayList of ReturnNodes
	Map<String, ArrayList<Integer>> allRetNodes =
		new HashMap<String, ArrayList<Integer>>();

	// Each MethodName is mapped to its array of Edges
	Map<String, Edge[]> allEdges = new HashMap<String, Edge[]>();

	//Map to track node ids to unique IDs used in dot file
	Map<Integer,Integer> nodeIDMap = new HashMap<Integer,Integer>();

	//key=id, value=line numbers
	Map<Integer,String> cfgNodes;
	Edge[] edges;
	String fileName;

	public int chgAffectedWCNodes = 0;
	public int affectedWCNodes = 0;
	public int chgAffectedWNodes = 0;
	public int affectedWNodes = 0;
	public int chgAffectedCNodes = 0;
	public int affectedCNodes = 0;
	public int chgNodes = 0;
	public int chgCodeNodes = 0;
	public int chgAssertNodes = 0;
	public int unAffectedNodes = 0;
	public int totalNodes = 0;

	//Go through each of the nodes, if a node matches trackCondLoc then mark
	// it as a conditional branch with color red and if a node matches
	// a write location then mark it with color blue
	public void printCFG(String fName, CFG cfg, AnalyzeIntraProceduralDiff analyzeIntraProceduralDiff, Set<Integer> trackCondLoc,
			Set<Integer> trackWriteLoc) {
		//save the impact sets for the visualization
		String mName = cfg.getMethodName();
		VisualizationData.addTrackCondLoc(mName,trackCondLoc);
		VisualizationData.addTrackWriteLoc(mName,trackWriteLoc);
		fileName = fName;
		Node[] nodes = cfg.getNodes();
		// TODO: Get rid of this variable because it isn't used.
		totalNodes = nodes.length-1;
		cfgNodes = new HashMap<Integer,String>();
		for (int i=0; i<nodes.length;i++){
			String lines ="";
			if (i == 0){
				//lines = Integer.toString(nodes[i].getID());
				lines = "root";
				cfgNodes.put(nodes[i].getID(),lines);
			}else if (i== nodes.length-2){
				lines = "exit";
				cfgNodes.put(nodes[i].getID(),lines);
			}else if (i == nodes.length-1){
				//lines = "";
				//cfgNodes.put(nodes[i].getID(),lines);
			}else{
				lines = nodes[i].getStartLineNumber() + "-" + nodes[i].getEndLineNumber();
				//用cfgnodes记录从node id到其行号的映射
				cfgNodes.put(nodes[i].getID(),lines);
				if (nodes[i].getNodeIsChanged())
					changedNodes.add(nodes[i].getID());
				if (nodes[i].isAssert())
					assertNodes.add(nodes[i].getID());
				if (trackCondLoc != null){
					Iterator<Integer> it1 = trackCondLoc.iterator();
					//如果该condloc的位置在node对应block的offset范围内，则添加之
					//该位置是pos还是linenumber？
					while (it1.hasNext()){
						Integer loc = it1.next();
						if (loc >= nodes[i].getStartOffset() && loc <= nodes[i].getEndOffset())
							condNodes.add(nodes[i].getID());
					}
				}
				if (trackWriteLoc != null){
					Iterator<Integer> it2 = trackWriteLoc.iterator();
					while (it2.hasNext()){
						Integer loc = it2.next();
						if (loc >= nodes[i].getStartOffset() && loc <= nodes[i].getEndOffset())
							writeNodes.add(nodes[i].getID());
					}
				}
			}
		}
		Map<Integer, Set<Dependency> > depend = new HashMap<>();
		Iterator<Integer> iterator = analyzeIntraProceduralDiff.depend.keySet().iterator();
		while (iterator.hasNext()) {
			Integer pos = (Integer) iterator.next();
			Set<Dependency> dependencies = analyzeIntraProceduralDiff.depend.get(pos);
			for (Dependency dependency : dependencies) {
				if (!cfg.posToID.containsKey(pos) || !cfg.posToID.containsKey(dependency.depend._2)) {
					System.err.println("can not find node ID for pos!");
					continue;
				}
				dependency.depend  = new Pair<Integer, Integer>(cfg.posToID.get(pos), cfg.posToID.get(dependency.depend._2));
				if (!depend.containsKey(cfg.posToID.get(pos))) {
					Set<Dependency> newDependencies = new HashSet<>();
					newDependencies.add(dependency);
					depend.put(cfg.posToID.get(pos), newDependencies);
				}
				else {
					depend.get(cfg.posToID.get(pos)).add(dependency);
				}
			}
		}
		edges = new Edge[cfg.getEdgeCount()];
		edges = cfg.getEdges(edges);
		depend.putAll(cfg.oldDepend);
		writeToFile(depend);
	}

	public void printCFG(String dotDir, String target, Map<String,MethodASTInfo> methodASTInfo,
			Map<String,ComputeInterProceduralDiff> differences) {

		File f = new File(dotDir);
		if (f.isDirectory()){
			target = target.substring(target.lastIndexOf(".")+1);
			fileName = dotDir + File.separator + target + ".dot";
		}else
			fileName = dotDir;

		// Iterate over the map of ComputeInterProceduralDiffs
		Iterator<String> cipdIter = differences.keySet().iterator();

		while(cipdIter.hasNext())
		{
			// Get the method name for the next cipd
			String methodName = cipdIter.next();

			//System.out.println("***** " + methodName + " *****");

			// Get the cipd mapped to the method name
			ComputeInterProceduralDiff cipd = differences.get(methodName);

			MethodASTInfo mai = methodASTInfo.get(cipd.getOtherMethodIndex());
			if(mai == null)
				System.out.println("The MethodASTInfo is null, fix it!");

			// Get the CFG for this cipd
			CFG cfg = cipd.getCFG(methodName, mai);
			if(cfg == null)
				System.out.println("The CFG is null, fix it!");


			AnalyzeIntraProceduralDiff sa;

			if(cipd instanceof ComputeInterProceduralDiff ) {
				 sa = ((ComputeInterProceduralDiff) cipd).getSemanticDiff();
			} else {
				sa = ComputeIntraProceduralDiff.getSemanticAnalysis(methodName);
			}

			Set<Integer> trackCondLoc = sa.getGlobalTrackCondition();
			Set<Integer> trackWriteLoc = sa.getGlobalTrackWrite();

			Set<Integer> trackInvokeLoc = null;
			if(cipd instanceof ComputeInterProceduralDiff) {
				trackInvokeLoc = ((ComputeInterProceduralDiff) cipd)
							.getSemanticDiff().getGlobalTrackInvoke();
			}

			Set<Integer> trackReturnLoc = null;
			if(cipd instanceof ComputeInterProceduralDiff) {
				trackReturnLoc = ((ComputeInterProceduralDiff) cipd).
								getSemanticDiff().getGlobalTrackReturn();
			}

			/*
			 * At this point, everything needed to construct the dot
			 * file for this particular method is ready.
			 * - method name
			 * - CFG
			 * - ACNs
			 * - AWNs
			 */
			Node[] nodes = cfg.getNodes();
			Map<Integer,String> cfgNodes = new HashMap<Integer,String>();
			ArrayList<Integer> changedNodes = new ArrayList<Integer>();
			ArrayList<Integer> condNodes = new ArrayList<Integer>();
			ArrayList<Integer> writeNodes = new ArrayList<Integer>();
			ArrayList<Integer> callNodes = new ArrayList<Integer>();
			ArrayList<Integer> retNodes = new ArrayList<Integer>();

			// loop through all the nodes in the CFG to build the
			// lists of nodes that will be used in the dot file.
			for (int i=0; i<nodes.length; i++) {
				String line = "";
				if (i == 0) {
					line = "root";
					cfgNodes.put(nodes[i].getID(),line);
				}
				else if (i== nodes.length-2) {
					line = "exit";
					cfgNodes.put(nodes[i].getID(),line);
				}
				else if (i == nodes.length-1) {
					// do nothing
				}
				else {
					line = nodes[i].getStartLineNumber() + "-" + nodes[i].getEndLineNumber();
					cfgNodes.put(nodes[i].getID(),line);

					if (nodes[i].getNodeIsChanged())
						changedNodes.add(nodes[i].getID());

					if (trackCondLoc != null) {
						Iterator<Integer> it1 = trackCondLoc.iterator();
						while (it1.hasNext()) {
							Integer loc = it1.next();
							if (loc >= nodes[i].getStartOffset() && loc <= nodes[i].getEndOffset())
								condNodes.add(nodes[i].getID());
						}
					}

					if (trackWriteLoc != null) {
						Iterator<Integer> it2 = trackWriteLoc.iterator();
						while (it2.hasNext()) {
							Integer loc = it2.next();
							if (loc >= nodes[i].getStartOffset() && loc <= nodes[i].getEndOffset())
								writeNodes.add(nodes[i].getID());
						}
					}
					if(trackInvokeLoc != null) {
						Iterator<Integer> it3 = trackInvokeLoc.iterator();
						while(it3.hasNext()) {
							Integer loc = it3.next();
							if (loc >= nodes[i].getStartOffset() && loc <= nodes[i].getEndOffset())
								callNodes.add(nodes[i].getID());
						}
					}
					if(trackReturnLoc != null) {
						Iterator<Integer> it4 = trackReturnLoc.iterator();
						while(it4.hasNext()) {
							Integer loc = it4.next();
							if (loc >= nodes[i].getStartOffset() && loc <= nodes[i].getEndOffset())
								retNodes.add(nodes[i].getID());
						}
					}
				}
			}

			// The nodes have been broken up into the appropriate lists
			// They can be put in the global maps for later access.
			this.allCFGNodes.put(methodName, cfgNodes);
			this.allChangedNodes.put(methodName, changedNodes);
			this.allCondNodes.put(methodName, condNodes);
			this.allWriteNodes.put(methodName, writeNodes);
			this.allCallNodes.put(methodName, callNodes);
			this.allRetNodes.put(methodName, retNodes);

			// The edges need to be stored for later too
			Edge[] edges = new Edge[cfg.getEdgeCount()];
			edges = cfg.getEdges(edges);
			this.allEdges.put(methodName, edges);
		}

		/*
		 * The data structures are all setup, so the CFG(s) can be
		 * written to the dot file now.
		 */
		// count the number of methods as you go
		int mCount = 0;

		Writer output = null;
		File file = new File(fileName);
		try {
			output = new BufferedWriter(new FileWriter(file));
		} catch (IOException e) {
			System.err.println("error while creating the file to write");
			e.printStackTrace();
		}
		try {
			// This is the first line of the dot file
			output.write("digraph \"\" { \n");
		} catch (IOException e) {
			System.err.println("Error while writing to the XML file");
			e.printStackTrace();
		}

		// Iterate over the CFGNodes for each method writing it to file
		Iterator<String> methodIter = this.allCFGNodes.keySet().iterator();

		while(methodIter.hasNext())
		{
			// This is the name of the current method being written
			String methodName = methodIter.next();

			// This is the Map of CFG nodes for the current method
			Map<Integer, String> cfgNodes = this.allCFGNodes.get(methodName);

			// only write the CFG if the cfgNodes aren't null
			if(cfgNodes != null)
			{
				// This is the ArrayList of the changed nodes for the current method
				ArrayList<Integer> changedNodes = this.allChangedNodes.get(methodName);

				// This is the ArrayList of cond nodes for the current method
				ArrayList<Integer> condNodes = this.allCondNodes.get(methodName);

				// This is the ArrayList of write nodes for the current method
				ArrayList<Integer> writeNodes = this.allWriteNodes.get(methodName);

				// This is the ArrayList of call nodes for the current method
				ArrayList<Integer> callNodes = this.allCallNodes.get(methodName);

				// This is the ArrayList of ret nodes for the current method
				ArrayList<Integer> retNodes = this.allRetNodes.get(methodName);

				// This is the array of edges for the current method
				Edge[] edges = this.allEdges.get(methodName);

				try
				{
					// these are the first two lines of a subgraph
					output.write("subgraph \"cluster_method" + mCount + "\" {\n");
					output.write("label=\"" + methodName + "\"\n");

					// write the nodes and edges to the file
					nodeIDMap = new HashMap<Integer,Integer>();

					this.printCFGNodes(output, cfgNodes, changedNodes,
							condNodes, writeNodes, callNodes, retNodes);
					this.printCFGEdges(output, edges);

					// this is the closing line of a subgraph
					output.write("}\n\n");
				}
				catch (IOException e)
				{
					System.out.println(e.getMessage());
					System.out.println("Issue when writing subgraph for " + methodName);
				}

				// increment the method count after each method written
				mCount++;
			}
		}

		try {
			// This is the last line of the dot file
			output.write("}");
		} catch (IOException e) {
			System.err.println("Error while writing to the XML file");
			e.printStackTrace();
		}

		// Close the Writer
		try {
			output.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void printCFGNodes(Writer output) throws IOException {
		if(cfgNodes == null) return;
		Iterator<Integer> nodeItr = cfgNodes.keySet().iterator();
		while(nodeItr.hasNext()) {
			Integer id = nodeItr.next();
//			Integer uniqueID = new Integer(this.nodeNum);
			Integer uniqueID = id;
			nodeIDMap.put(id, uniqueID);
			
			String lineNumbers = cfgNodes.get(id);
			if (writeNodes.contains(id) && condNodes.contains(id)){
				if (changedNodes.contains(id)){
					chgAffectedWCNodes++;
					chgNodes++;
					if(assertNodes.contains(id)){
						chgAssertNodes++;
						output.write(uniqueID +"[ label=\"(" + uniqueID + "**)" +
								lineNumbers + "\",color=purple,shape=doubleoctagon];\n");
					}else{
						chgCodeNodes++;
						output.write(uniqueID +"[ label=\"(" + uniqueID + "**)" +
							lineNumbers + "\",color=purple];\n");
					}
				}else{
					affectedWCNodes++;
					if(assertNodes.contains(id)){
						output.write(uniqueID +"[ label=\"(" + uniqueID + ")" +
							lineNumbers + "\",color=purple,shape=doubleoctagon];\n");
					}else{
						output.write(uniqueID +"[ label=\"(" + uniqueID + ")" +
								lineNumbers + "\",color=purple];\n");
					}
				}
			}else if (writeNodes.contains(id)){
				if (changedNodes.contains(id)){
					chgAffectedWNodes++;
					chgNodes++;
					if(assertNodes.contains(id)){
						chgAssertNodes++;
						output.write(uniqueID +"[ label=\"(" + uniqueID + "**)" +
							lineNumbers + "\",color=blue,shape=doubleoctagon];\n");
					}else{
						chgCodeNodes++;
						output.write(uniqueID +"[ label=\"(" + uniqueID + "**)" +
								lineNumbers + "\",color=blue];\n");						
					}
				}else{
					affectedWNodes++;
					if(assertNodes.contains(id)){
						output.write(uniqueID +"[ label=\"(" + uniqueID + ")" +
							lineNumbers + "\",color=blue,shape=doubleoctagon];\n");
					}else{
						output.write(uniqueID +"[ label=\"(" + uniqueID + ")" +
								lineNumbers + "\",color=blue];\n");
					}
				}
			}else if (condNodes.contains(id)){
				if (changedNodes.contains(id)){
					chgAffectedCNodes++;
					chgNodes++;
					if(assertNodes.contains(id)){
						chgAssertNodes++;
						output.write(uniqueID +"[ label=\"(" + uniqueID + "**)" +
							lineNumbers + "\",color=red,shape=doubleoctagon];\n");
					}else{
						chgCodeNodes++;
						output.write(uniqueID +"[ label=\"(" + uniqueID + "**)" +
							lineNumbers + "\",color=red];\n");
					}
				}else{
					affectedCNodes++;
					if(assertNodes.contains(id)){
						output.write(uniqueID +"[ label=\"(" + uniqueID + ")" +
							lineNumbers + "\",color=red,shape=doubleoctagon];\n");
					}else{
						output.write(uniqueID +"[ label=\"(" + uniqueID + ")" +
							lineNumbers + "\",color=red];\n");
					}
				}
			}
			else{
				unAffectedNodes++;
				if (changedNodes.contains(id)){
					chgNodes++;
					if(assertNodes.contains(id)){
						chgAssertNodes++;
						output.write(uniqueID +"[ label=\"(" + uniqueID + "**)" +
							lineNumbers + "\",shape=doubleoctagon];\n");
					}else{
						chgCodeNodes++;
						output.write(uniqueID +"[ label=\"(" + uniqueID + "**)" +
							lineNumbers + "\"];\n");
					}
				}else{
					if(assertNodes.contains(id)){
						output.write(uniqueID +"[ label=\"(" + uniqueID + ")" +
							lineNumbers +"\",shape=doubleoctagon];\n");
					}else{
						output.write(uniqueID +"[ label=\"(" + uniqueID + ")" +
							lineNumbers +"\"];\n");
					}
				}
			}
			this.nodeNum++;
		}
	}

	public void printCFGNodes(Writer output, Map<Integer, String> cfgNodes,
			ArrayList<Integer> changedNodes, ArrayList<Integer> condNodes,
			ArrayList<Integer> writeNodes, ArrayList<Integer> callNodes,
			ArrayList<Integer> retNodes) throws IOException
	{
		if(cfgNodes == null) return;

		Iterator<Integer> nodeItr = cfgNodes.keySet().iterator();
		while(nodeItr.hasNext()) {
			Integer id = nodeItr.next();
			Integer uniqueID = new Integer(this.nodeNum);
			nodeIDMap.put(id, uniqueID);
			String lineNumbers = cfgNodes.get(id);
			if (writeNodes.contains(id) && condNodes.contains(id) &&
					callNodes.contains(id) && retNodes.contains(id)) {
				if (changedNodes.contains(id)) {
					output.write(uniqueID +"[ label=\"(" + uniqueID + "**)" +
							lineNumbers + "\",color=grey10, shape=tripleoctagon," +
									" style=filled, fillcolor=darksalmon];\n");
				} else {
					output.write(uniqueID +"[ label=\"(" + uniqueID + ")" +
							lineNumbers + "\",color=grey10, shape=tripleoctagon, " +
									"	style=filled, fillcolor=darksalmon];\n");
				}
			}
			else if(writeNodes.contains(id) && condNodes.contains(id) &&
					callNodes.contains(id))  {
				if (changedNodes.contains(id)) {
					output.write(uniqueID +"[ label=\"(" + uniqueID + "**)" +
							lineNumbers + "\",color=grey10, shape=tripleoctagon];\n");
				} else {
					output.write(uniqueID +"[ label=\"(" + uniqueID + ")" +
							lineNumbers + "\",color=grey10, shape=tripleoctagon];\n");
				}
			}
			else if (writeNodes.contains(id) && callNodes.contains(id) &&
					retNodes.contains(id)) {
				if (changedNodes.contains(id)) {
					output.write(uniqueID +"[ label=\"(" + uniqueID + "**)" +
							lineNumbers + "\",color=cyan3, shape=doubleoctagon," +
									" style=filled, fillcolor=darksalmon];\n");
				} else {
					output.write(uniqueID +"[ label=\"(" + uniqueID + ")" +
							lineNumbers + "\",color=cyan3, shape=doubleoctagon," +
									" style=filled, fillcolor=darksalmon];\n");
				}
			}
			else if (writeNodes.contains(id) && callNodes.contains(id)) {
				if (changedNodes.contains(id)) {
					output.write(uniqueID +"[ label=\"(" + uniqueID + "**)" +
							lineNumbers + "\",color=cyan3, shape=doubleoctagon];\n");
				} else {
					output.write(uniqueID +"[ label=\"(" + uniqueID + ")" +
							lineNumbers + "\",color=cyan3, shape=doubleoctagon];\n");
				}
			}
			else if(condNodes.contains(id) && callNodes.contains(id) &&
					retNodes.contains(id)) {
				if (changedNodes.contains(id)) {
					output.write(uniqueID +"[ label=\"(" + uniqueID + "**)" +
							lineNumbers + "\",color=darkorange, shape=doubleoctagon," +
									" style=filled, fillcolor=darksalmon];\n");
				} else {
					output.write(uniqueID +"[ label=\"(" + uniqueID + ")" +
							lineNumbers + "\",color=darkorange, shape=doubleoctagon," +
									" style=filled, fillcolor=darksalmon];\n");
				}
			}
			else if(condNodes.contains(id) && callNodes.contains(id)) {
				if (changedNodes.contains(id)) {
					output.write(uniqueID +"[ label=\"(" + uniqueID + "**)" +
							lineNumbers + "\",color=darkorange, shape=doubleoctagon];\n");
				} else {
					output.write(uniqueID +"[ label=\"(" + uniqueID + ")" +
							lineNumbers + "\",color=darkorange, shape=doubleoctagon];\n");
				}
			}
			else if (writeNodes.contains(id) && condNodes.contains(id) &&
					retNodes.contains(id)){
				if (changedNodes.contains(id)){
					chgAffectedWCNodes++;
					chgNodes++;
					output.write(uniqueID +"[ label=\"(" + uniqueID + "**)" +
							lineNumbers + "\",color=purple,shape=doubleoctagon," +
									" style=filled, fillcolor=darksalmon];\n");
				}else{
					affectedWCNodes++;
					output.write(uniqueID +"[ label=\"(" + uniqueID + ")" +
							lineNumbers + "\",color=purple,shape=doubleoctagon," +
									" style=filled, fillcolor=darksalmon];\n");
				}
			}
			else if (writeNodes.contains(id) && condNodes.contains(id)){
				if (changedNodes.contains(id)){
					chgAffectedWCNodes++;
					chgNodes++;
					output.write(uniqueID +"[ label=\"(" + uniqueID + "**)" +
							lineNumbers + "\",color=purple,shape=doubleoctagon];\n");
				}else{
					affectedWCNodes++;
					output.write(uniqueID +"[ label=\"(" + uniqueID + ")" +
							lineNumbers + "\",color=purple,shape=doubleoctagon];\n");
				}
			}
			else if (callNodes.contains(id) && retNodes.contains(id)) {
				if(changedNodes.contains(id)) {
					output.write(uniqueID +"[ label=\"(" + uniqueID + "**)" +
							lineNumbers + "\",color=green4,shape=doubleoctagon," +
									" style=filled, fillcolor=darksalmon];\n");
				} else {
					output.write(uniqueID +"[ label=\"(" + uniqueID + ")" +
							lineNumbers + "\",color=green4,shape=doubleoctagon," +
									" style=filled, fillcolor=darksalmon];\n");
				}
			}
			else if (writeNodes.contains(id)){
				if (changedNodes.contains(id)){
					chgAffectedWNodes++;
					chgNodes++;
					output.write(uniqueID +"[ label=\"(" + uniqueID + "**)" +
							lineNumbers + "\",color=blue];\n");
				}else{
					affectedWNodes++;
					output.write(uniqueID +"[ label=\"(" + uniqueID + ")" +
							lineNumbers + "\",color=blue];\n");
				}
			}else if (condNodes.contains(id)){
				if (changedNodes.contains(id)){
					chgAffectedCNodes++;
					chgNodes++;
					output.write(uniqueID +"[ label=\"(" + uniqueID + "**)" +
							lineNumbers + "\",color=red];\n");
				}else{
					affectedCNodes++;
					output.write(uniqueID +"[ label=\"(" + uniqueID + ")" +
							lineNumbers + "\",color=red];\n");
				}
			} else if (callNodes.contains(id)) {
				if(changedNodes.contains(id)) {
					output.write(uniqueID +"[ label=\"(" + uniqueID + "**)" +
							lineNumbers + "\",color=green4];\n");
				} else {
					output.write(uniqueID +"[ label=\"(" + uniqueID + ")" +
							lineNumbers + "\",color=green4];\n");
				}
			} else if (retNodes.contains(id)) {
				if(changedNodes.contains(id)) {
					output.write(uniqueID +"[ label=\"(" + uniqueID + "**)" +
							lineNumbers + "\",color=deeppink2];\n");
				} else {
				output.write(uniqueID +"[ label=\"(" + uniqueID + ")" +
						lineNumbers + "\",color=deeppink2];\n");
				}
			}
			else{
				unAffectedNodes++;
				if (changedNodes.contains(id)){
					chgNodes++;
					output.write(uniqueID +"[ label=\"(" + uniqueID + "**)" +
							lineNumbers + "\"];\n");
				}else{
					output.write(uniqueID +"[ label=\"(" + uniqueID + ")" +
							lineNumbers +"\"];\n");
				}
			}
			this.nodeNum++;
		}
	}

	public void printCFGEdges(Writer output) throws IOException {
		if(edges == null) return;
		for (int i=0; i<edges.length;i++){
			Edge e = edges[i];
			String label = e.getLabel();
			boolean dangerous = e.getDangerous();
			if (dangerous)
				output.write(nodeIDMap.get(e.getPredNodeID()) + "->" +
						nodeIDMap.get(e.getSuccNodeID()) +
					"[ color=\"red\" label=\"" + label +	"\"];\n");
			else
				output.write(nodeIDMap.get(e.getPredNodeID()) + "->" +
						nodeIDMap.get(e.getSuccNodeID()) +
						"[ label=\"" + label +	"\"];\n");
		}
	}

	public void printCFGEdges(Writer output, Edge[] edges) throws IOException {
		if(edges == null) return;
		for (int i=0; i<edges.length;i++){
			Edge e = edges[i];
			String label = e.getLabel();
			boolean dangerous = e.getDangerous();
			if (dangerous)
				output.write(nodeIDMap.get(e.getPredNodeID()) + "->" +
						nodeIDMap.get(e.getSuccNodeID()) +
					"[ color=\"red\" label=\"" + label +	"\"];\n");
			else
				output.write(nodeIDMap.get(e.getPredNodeID()) + "->" +
						nodeIDMap.get(e.getSuccNodeID()) +
						"[ label=\"" + label +	"\"];\n");
		}
	}

//	public void printOutput(HashMap<String, Integer> dNodes,
//			HashMap<Integer, GlobalOperation> operations,
//			HashMap<Integer, ArrayList<Integer>> dEdges,
//			String fileName) {
//		this.dNodes = dNodes;
//		this.operations = operations;
//		this.dEdges = dEdges;
//		this.fileName = fileName;
//		writeToFile();
//	}

	public void writeToFile(Map<Integer, Set<Dependency>> depend) {
		Writer output = null;
		File file = new File(fileName);
		try {
			output = new BufferedWriter(new FileWriter(file));
		} catch (IOException e) {
			System.err.println("error while creating the file to write");
			e.printStackTrace();
		}
		try {
			output.write("digraph \"\" { \n");
			//output.write("\t size=\"6,6\"; \n " +
			//		"\t node [ shape = record, style = filled ]; \n" +
			//		"\t edge [ fontsize=18, size=24, color=magenta, style=solid]; \n");
			//printNodes(output);
			nodeIDMap = new HashMap<Integer,Integer>();
			printCFGNodes(output);
			printCFGEdges(output);
			printCFGDepend(output, depend);
			//printEdges(output);
			output.write("}");
		} catch (IOException e) {
			System.err.println("Error while writing to the XML file");
			e.printStackTrace();
		}

		try {
			output.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private void printCFGDepend(Writer output, Map<Integer, Set<Dependency>> depend) throws IOException {
		// TODO Auto-generated method stub
		if(cfgNodes == null) return;
		Iterator<Integer> nodeItr = cfgNodes.keySet().iterator();
		Set<Pair<Integer, Integer>> used = new HashSet<>();
		while(nodeItr.hasNext()) {
			Integer id = nodeItr.next();
//			Integer uniqueID = new Integer(this.nodeNum);
			Integer uniqueID = id;
			nodeIDMap.put(id, uniqueID);
			
			String lineNumbers = cfgNodes.get(id);
			if (depend.containsKey(id)) {
				Set<Dependency> dependencies = depend.get(id);
				for (Dependency dependency : dependencies) {
					Integer des = dependency.depend._2;
					Pair<Integer, Integer> now = new Pair<Integer, Integer>(id, des);
					System.err.println(nodeIDMap.get(id) + "->" + nodeIDMap.get(des));
					if (des == null || used.contains(now)) {
						continue;
					}
					if (dependency instanceof Data) {
						output.write(nodeIDMap.get(id) + "->" +
								nodeIDMap.get(des) +
							"[ color=\"red\" label=\"" + "Data Depends on" +	"\" style = dotted ];\n");
					}
					if (dependency instanceof Control) {
						output.write(nodeIDMap.get(id) + "->" +
								nodeIDMap.get(des) +
							"[ color=\"blue\" label=\"" + "Control Depends on" +	"\" style = dotted ];\n");
					}
					used.add(now);
				}
			}
		}		
	}

	public void printNodes(Writer output) throws IOException {
		Iterator<Integer> nodeItr = operations.keySet().iterator();
		while(nodeItr.hasNext()) {
			Integer id = nodeItr.next();
			GlobalOperation gOp = operations.get(id);
			String label = gOp.getClassName().concat(gOp.getMethodName()).
											concat(gOp.getPosition());
			output.write(id+"[ label=" + getModifiedString(label) +"];\n");
		}
	}

	private String getModifiedString(String index) {
		// Replace all non-alphanumeric characters with empty string
		String finalStr = "";
		for(int i = 0; i < index.length(); i++) {
			if(Character.isDigit(index.charAt(i)) ||
					Character.isLetter(index.charAt(i))) {
			finalStr = finalStr.concat(index.substring(i, i+1));
			}
		}
		return finalStr;
	}

	public void printEdges(Writer output) throws IOException  {
		Iterator<Integer> edgeItr = dEdges.keySet().iterator();
		while(edgeItr.hasNext()) {
			Integer head = edgeItr.next();
			ArrayList<Integer> child = dEdges.get(head);
			for(int childIdx = 0; childIdx < child.size(); childIdx++) {
				output.write(head + "->" + child.get(childIdx)+";\n");
			}
		}
	}

}