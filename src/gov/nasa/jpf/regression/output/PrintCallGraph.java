package gov.nasa.jpf.regression.output;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import edu.byu.cs.search.heuristic.pefca.ReducedCallGraph;
import edu.byu.cs.search.heuristic.pefca.callNode;
import gov.nasa.jpf.Config;

public class PrintCallGraph {
	
	private HashMap<String, callNode> oldCGNodes;
	
	private HashMap<String, callNode> newCGNodes;
	
	private HashMap<Integer, HashMap<Integer, ArrayList<Integer>>> oldCGEdges;
	
	private HashMap<Integer, HashMap<Integer, ArrayList<Integer>>> newCGEdges;
	
	private Set<Integer> modNodes;
	
	private String filename;
	
	public PrintCallGraph(String dotDir, String target, ReducedCallGraph oldGraph,
			ReducedCallGraph newGraph, Set<Integer> modNodes) {
		
		/*
		 * TODO: Figure out a naming scheming for these files so that
		 * they can be uniquely identified.
		 */
		target = target.substring(target.lastIndexOf(".")+1);
		this.filename = dotDir + File.separator + target;
		
		
		this.oldCGNodes = oldGraph.getLiveNodes();
		this.newCGNodes = newGraph.getLiveNodes();
		
		this.oldCGEdges = oldGraph.getCallEdges();
		this.newCGEdges = newGraph.getCallEdges();
		
		this.modNodes = modNodes;
	}
	
	/**
	 * This method takes the oldGraph ReducedCallGraph information
	 * given to the constructor to write the dot file for the Old
	 * Call Graph.
	 */
	public void printOldCallGraph() {
		
		/* Modeled after writeToFile() in PrintToDot.java */
		Writer output = null;
		File file = new File(this.filename + ".OldCG.dot");
		try {
			output = new BufferedWriter(new FileWriter(file));
		} catch (IOException e) {
			System.err.println("error while creating the file to write");
			e.printStackTrace();
		}
		
		try {
			// writes the opening line of the file
			output.write("digraph \"\" {\n");
			
			// write the Old Call Graph nodes to the file
			this.writeCGNodes(output, this.oldCGNodes, false);
			
			// write the Old Call Graph edges to the file
			this.writeCGEdges(output, this.oldCGEdges);
			
			// writes the closing line of the file
			output.write("}");
		}
		catch(IOException e) {
			System.err.println("Error while writing to the dot file");
			e.printStackTrace();
		}
		
		try {
			output.close();
		} catch (IOException e) {
			System.err.print("IOException while trying the close the FileWriter.");
			e.printStackTrace();
		}
		
	} // end of printOldCallGraph
	
	/**
	 * This method takes the newGraph ReducedCallGraph information
	 * given to the constructor to write the dot file for the New
	 * Call Graph.
	 */
	public void printNewCallGraph() {
		
		/* Modeled after writeToFile() in PrintToDot.java */
		Writer output = null;
		File file = new File(this.filename + ".NewCG.dot");
		try {
			output = new BufferedWriter(new FileWriter(file));
		} catch (IOException e) {
			System.err.println("error while creating the file to write");
			e.printStackTrace();
		}
		
		try {
			// writes the opening line of the file
			output.write("digraph \"\" {\n");
			
			// write the Call Graph nodes to the file
			this.writeCGNodes(output, this.newCGNodes, true);
			
			// write the Call Graph edges to the file
			this.writeCGEdges(output, this.newCGEdges);
			
			// writes the closing line of the file
			output.write("}");
		}
		catch(IOException e) {
			System.err.println("Error while writing to the dot file");
			e.printStackTrace();
		}
		
		try {
			output.close();
		} catch (IOException e) {
			System.err.print("IOException while trying the close the FileWriter.");
			e.printStackTrace();
		}
		
	} // end of printNewCallGraph
	
	/**
	 * Writes the given nodes of the Call Graph to the given FileWriter 
	 */
	public void writeCGNodes(Writer output, HashMap<String, callNode> nodes, boolean isNew) 
		throws IOException {
		
		// write the nodes to the output dot file
		
		// Iterate over the nodes in this call graph
		Iterator<String> iter = nodes.keySet().iterator();
		
		while(iter.hasNext())
		{
			// get the next item from the iterator
			/*
			 * the key is a concatenation of the ClassName and the
			 * MethodName.
			 */
			String nodeKey = iter.next();
			
			// get this node's callNode
			callNode cn = nodes.get(nodeKey);
			
			int unique = cn.getUniqueIdentifier();
			
			String id = Integer.toString(unique);
			
			String label = cn.getClassName() + "\\n" + cn.getMethodName();
			
			if(isNew)
			{
				if(this.modNodes.contains(unique))
				{
					label = "**" + label;
				}
			}
			
			output.write(id+"[ label=\"" + label +"\"];\n");
		}
		
	} // end of writeCGNodes
	
	/**
	 * Writes the given edges of the Call Graph to the given FileWriter 
	 */
	public void writeCGEdges(Writer output, HashMap<Integer, HashMap<Integer, ArrayList<Integer>>> edges) 
		throws IOException {
		
		// write the edges to the output dot file
		
		Iterator<Integer> sourceIter = edges.keySet().iterator();
		
		while(sourceIter.hasNext())
		{
			// get the next item from the iterator
			/* 
			 * this is the unique id of the method from where the
			 * method call originates.
			 */
			int sourceIndex = sourceIter.next();
			
			/* 
			 * this is the Map of all method invocations within the
			 * source method.
			 */
			HashMap<Integer, ArrayList<Integer>> successors = edges.get(sourceIndex);
			
			Iterator<Integer> targetIter = successors.keySet().iterator();
			
			while(targetIter.hasNext())
			{
				/* 
				 * this is the bytecode position within the source
				 * method where the call originated.
				 */
				int position = targetIter.next();
				
				/* 
				 * this is the list of unique ids for target methods 
				 * which have been invoked by the source method
				 */
				ArrayList<Integer> targets = successors.get(position);
				
				//in the presence of polymorphism there can be multiple
				// targets at a call site
				for(int tIndex = 0; tIndex < targets.size(); tIndex++) {
					int targetIndex = targets.get(tIndex);
				
					// write the directed edge to the output dot file
					output.write(Integer.toString(sourceIndex) + "->" + 
						Integer.toString(targetIndex) +
						"[ label=\"bcp(" + position + ")\"];\n");
				}
			}
		}
		
	} // end of writeCGEdges
	
}