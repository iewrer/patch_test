package gov.nasa.jpf.regression.analysis;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InvokeInstruction;
import org.apache.bcel.verifier.structurals.InstructionContext;

import edu.byu.cs.search.heuristic.pefca.ReducedCallGraph;
import edu.byu.cs.search.heuristic.pefca.callNode;
import edu.byu.cs.trace.AbstractMethodInfo;

public class AugmentCallGraph {
	
	HashMap<String, ComputeInterProceduralDiff> differences; 

	public AugmentCallGraph(HashMap<String,
					ComputeInterProceduralDiff> differences) {
		this.differences = differences;
	}
	
	public void setup() {
//		System.out.println("Edge Count (1): " + countCallEdges(callGraph.getCallEdges()));
//		callGraph = new ReducedCallGraph
//								(rta, "/Users/jbranchaud/Documents/git/deval/subjects/voldemort/VoldemortOnlyCallEdges.csv");
//		System.out.println("Edge Count (2): " + countCallEdges(callGraph.getCallEdges()));
		
		//System.exit(1);
		
		System.out.println("**********");
		//System.out.println(callGraph.getLiveNodes().toString());
//		for(String node : callGraph.getLiveNodes().keySet()) {
//			System.out.println(node);
//		}
//		for(Integer callerIndex : callGraph.getCallEdges().keySet()) {
//			callGraph.getCallEdges();
//		}
		//this.augmentCallGraph(callGraph, getPreciseEdges(""));

	}
	
	/*
	 * augmentCallGraph
	 * 
	 * given the DiSE generated call graph and a map of precise call graph
	 * edge information from an external source, this method will augment
	 * the given call graph by removing the over-approximations. That is,
	 * for each edge in the given call graph, if the equivalent edge does
	 * not exist in the preciseEdges map, then that edge is removed from
	 * the call graph. The resulting reduced call graph is returned.
	 */
	public void augmentCallGraph(ReducedCallGraph callGraph, Map<String, ArrayList<String>> preciseEdges) {
		
		Map<Integer, HashMap<Integer, ArrayList<Integer>>> callEdges = callGraph.getCallEdges();
		
		Map<Integer, HashMap<Integer, ArrayList<Integer>>> callEdgesCopy = deepCopyCallEdges(callEdges);
		
		System.out.println(preciseEdges.toString());
		System.out.println("Precise Edges: " + preciseEdges.keySet().size());
		
		// count call edges before
		System.out.println("CallEdges Before: " + countCallEdges(callEdges));
		
		for(Integer callerIndex : callEdges.keySet()) {
			Map<Integer, ArrayList<Integer>> callPositionMap = callEdges.get(callerIndex);
			callNode callerNode = callGraph.getCallNode(callerIndex);
			for(Integer callPosition : callPositionMap.keySet()) {
				ArrayList<Integer> targets = callPositionMap.get(callPosition);
				for(Integer calleeIndex : targets) {
					callNode calleeNode = callGraph.getCallNode(calleeIndex);
					String callerSignature = callerNode.getClassName() + "." + callerNode.getMethodName();
					String calleeSignature = calleeNode.getClassName() + "." + calleeNode.getMethodName();
//					System.out.println(callerNode.getClassName() + "." + callerNode.getMethodName() + " => "
//							+ calleeNode.getClassName() + "." + calleeNode.getMethodName());
					
					// check for the current caller-callee pair in the preciseEdges map
					if(preciseEdges.get(callerSignature) != null) {
						if(preciseEdges.get(callerSignature).contains(calleeSignature)) {
							continue;
						}
					}
					
					// this edge wasn't found, time to remove it
					removeCallEdge(callEdgesCopy, callerIndex, calleeIndex);
					
				}
			}
		}
		
		// count call edges after
		System.out.println("CallEdges After: " + countCallEdges(callEdgesCopy));
		
		// TODO: don't forget to update the call graph call edges with the
		// new additions before the method returns.
	}
	
	public void getInvocationPosition(ReducedCallGraph callGraph, String callerSignature, String calleeSignature) {
		
		callNode callerNode = getCallNode(callGraph, callerSignature);
		callNode calleeNode = getCallNode(callGraph, calleeSignature);
		
//		MethodASTInfo callerInfo = this.methodASTInfo.get(callerSignature);
//		MethodASTInfo calleeInfo = this.methodASTInfo.get(calleeSignature);
		
		AbstractMethodInfo callerInfo = this.differences.get(callerSignature).absMethodInfo;
		AbstractMethodInfo calleeInfo = this.differences.get(calleeSignature).absMethodInfo;
		
		Map<Integer, InstructionContext> ics = callerInfo.getInstructionContexts();
		for(Integer instructionPosition : ics.keySet()) {
			InstructionContext ic = ics.get(instructionPosition);
			Instruction currInstruction = ic.getInstruction().getInstruction();
			ArrayList<Integer> someList = new ArrayList<Integer>();
			if(currInstruction instanceof org.apache.bcel.generic.InvokeInstruction) {
				InvokeInstruction invocation = (InvokeInstruction)currInstruction;
				
			}
		}
		callerInfo.getMethodGen().getInstructionList().getInstructionHandles();
		for(Instruction ins : callerInfo.getMethodGen().getInstructionList().getInstructions()) {
			
		}
		
		//callerInfo.getMethodGen().getInstructionList().get
	}
	
	/*
	 * getCallNode: ReducedCallGraph, String -> callNode
	 * 
	 * given a ReducedCallGraph object for the call graph of interest and
	 * a String representing the method signature of a CallNode in the call
	 * graph, this method will find the associated callNode and return it.
	 */
	public static callNode getCallNode(ReducedCallGraph callGraph, String methodSignature) {
		
		callNode caller = callGraph.getLiveNodes().get(methodSignature);
		
		if(caller == null) {
			throw new RuntimeException("Bad methodSignature, doesn't match anything in map of call nodes.");
		}
		
		return caller;
	}
	
	/*
	 * getCallNodeIndex: ReducedCallGraph, String -> int
	 * 
	 * given a ReducedCallGraph object for the call graph of interest and
	 * a String representing the method signature of a CallNode in the call
	 * graph, this method will get the callNode from getCallNode() and then
	 * return the index value associated with that callNode.
	 */
	public static int getCallNodeIndex(ReducedCallGraph callGraph, String methodSignature) {
		
		return getCallNode(callGraph, methodSignature).getUniqueIdentifier();
	}
	
	/*
	 * removeCallEdge
	 * 
	 * given a mapping of caller indices to callee indices that represents
	 * call edges as well as two ints representing a caller index and a
	 * callee index, this method will remove the caller-callee pairing
	 * from the call edges map.
	 */
	public static void removeCallEdge(Map<Integer, HashMap<Integer, ArrayList<Integer>>> callEdges, int callerIndex, int calleeIndex) {
		
		// if the caller index isn't even in the call graph edges
		// or if the list of callee is empty
		// or if the callee index isn't in the list of callees,
		// then simply return
		if(callEdges.get(callerIndex) == null
				|| callEdges.get(callerIndex).size() == 0
				|| callEdges.get(callerIndex).containsKey(calleeIndex) == false) {
			return;
		}
		
		if(callEdges.get(callerIndex) == null) return;
		
		for(ArrayList<Integer> callees : callEdges.get(callerIndex).values()) {
			callees.remove(new Integer(calleeIndex));
		}
	}
	
	public static Map<Integer, HashMap<Integer, ArrayList<Integer>>> deepCopyCallEdges(
			Map<Integer, HashMap<Integer, ArrayList<Integer>>> callEdges) {
		
		Map<Integer, HashMap<Integer, ArrayList<Integer>>> callEdgesCopy = new HashMap<Integer, HashMap<Integer, ArrayList<Integer>>>();
		
		for(Integer callerIndex : callEdges.keySet()) {
			callEdgesCopy.put(callerIndex, new HashMap<Integer, ArrayList<Integer>>());
			for(Integer callerPosition : callEdges.get(callerIndex).keySet()) {
				ArrayList<Integer> calleeIndices = new ArrayList<Integer>();
				for(Integer calleeIndex : callEdges.get(callerIndex).get(callerPosition)) {
					calleeIndices.add(calleeIndex);
				}
				callEdgesCopy.get(callerIndex).put(callerPosition, calleeIndices);
			}
		}
		
		return callEdgesCopy;
	}
	
	/*
	 * countCallEdges
	 * 
	 * given a ReducedCallGraph object, this method will go through the call
	 * edges map and count up all the call edges that exist within this
	 * call graph. The total will be returned.
	 */
	public static int countCallEdges(Map<Integer, HashMap<Integer, ArrayList<Integer>>> callEdges) {
		
		int count = 0;
		
		for(Integer callerIndex : callEdges.keySet()) {
			Map<Integer, ArrayList<Integer>> currCaller = callEdges.get(callerIndex);
			for(Integer callerPosition : currCaller.keySet()) {
				count = count + currCaller.get(callerPosition).size();
			}
		}
		
		return count;
	}
	
	/*
	 * getPreciseEdges
	 * 
	 * given a filename to a CSV file that contains more precise call edge
	 * info from running Soot, this method will read in the content of the
	 * CSV and translate it to a HashMap which will then be returned.
	 */
	public Map<String, ArrayList<String>> getPreciseEdges(String filename) {
		
		Map<String, ArrayList<String>> preciseEdges = new HashMap<String, ArrayList<String>>();
		
		if(filename.equals("")) {
			filename = "/Users/jbranchaud/Documents/git/deval/subjects/voldemort/VoldemortCallEdges.csv";
		}
		
		File csvFile = new File(filename);
		try {
			BufferedReader bf = new BufferedReader(new FileReader(csvFile));
			String currLine;
			while((currLine = bf.readLine()) != null) {
				String[] methodSignatures = currLine.split(",");
				String caller = methodSignatures[0];
				String callee = methodSignatures[1];
				
				// add them to the map
				if(preciseEdges.get(caller) == null) {
					preciseEdges.put(caller, new ArrayList<String>());
				}
				preciseEdges.get(caller).add(callee);
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			System.out.println(e.getMessage());
		} catch (IOException e) {
			System.out.println(e.getMessage());
		}
		
		return preciseEdges;
	}
	
    /*
     * compareMethodSignatures
     * 
     * given two Strings that represent method signatures, this method will
     * determine if they are equivalent method signatures. The simplest check
     * is to see if the two strings are completely identical. If they are
     * identical, then the first signature should be returned. If they are
     * not identical, then they may still be the same if they have parameters
     * that are in the same type hierarchy (e.g. java.util.List and
     * java.util.Collection). If a match can be found by swapping in and out
     * the various parameters with the items in their same class hierarchy,
     * then return the first method signature. If no direct or indirect
     * match can be found, then an empty string should be returned.
     */
    public String compareMethodSignatures(String signature1, String signature2) {
    	
    	// the given signatures may contain '$' when enums and inner classes
    	// are involved, this changes them to '.' for consistency.
    	signature1 = signature1.replaceAll("\\$", ".");
    	signature2 = signature2.replaceAll("\\$", ".");
    	
    	if(signature1.equals(signature2)) {
    		return signature1;
    	}
    	
    	if(signature1.endsWith(signature2)) {
    		return signature1;
    	}
    	else if(signature2.endsWith(signature1)) {
    		return signature2;
    	}
    
    	return "";
    }
    
    

}