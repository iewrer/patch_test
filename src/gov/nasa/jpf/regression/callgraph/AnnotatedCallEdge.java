package gov.nasa.jpf.regression.callgraph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * The class represents a call edge on a call graph which can be
 * annotated with information gathered during analysis that will be
 * useful in a graphical view of the call graph.
 *
 * @author Josh Branchaud
 *
 */

public class AnnotatedCallEdge {

	/*
	 * An AnnotatedCallEdge should be able to be uniquely identified
	 * based on matching both the source and the position.
	 */

	/*
	 * source An int that represents the source index of the
	 * callNode that is the source of this call edge.
	 */
	private int source;

	/*
	 * targetIndices An ArrayList of Integers that represent the
	 * target indices of the call nodes that are the possible targets
	 * of this call edge. I believe there could be multiple because
	 * of the potential of dynamic dispatch if polymorphism were to
	 * be incorporated.
	 */
	private ArrayList<Integer> targets;

	/*
	 * position An int that represents the bytecode position of the
	 * call site which is where the caller to callee relationship
	 * originates.
	 */
	private int position;

	/*
	 * annotations An ArrayList of the names of the arguments that
	 * may affect their call site which corresponds to this edge.
	 */
	private Map<String, ArrayList<String>> annotations;
	
	
	/*
	 * Annotations on the global variable maps that affect
	 * the call site that corresponds to the edge
	 */
	protected Map<String, ArrayList<String>> globalVarAnnotations;

	/*
	 * Constructor(s)
	 */
	public AnnotatedCallEdge(int source, int position,
			ArrayList<Integer> targets) {
		this.source = source;
		this.position = position;
		this.targets = targets;
		this.annotations = new HashMap<String, ArrayList<String>>();
		this.globalVarAnnotations = new HashMap<String, ArrayList<String>>();
	}

	/*
	 * Accessor Methods
	 */

	public int getSource() {
		return source;
	}

	public void setSource(int source) {
		this.source = source;
	}

	public ArrayList<Integer> getTargets() {
		return targets;
	}

	public void setTargets(ArrayList<Integer> targets) {
		this.targets = targets;
	}

	public int getPosition() {
		return position;
	}

	public void setPosition(int position) {
		this.position = position;
	}

	public ArrayList<String> getAnnotations(String index) {
		if(!annotations.containsKey(index)) {
			ArrayList<String> otherAnnotations = new ArrayList<String>();
			annotations.put(index, otherAnnotations);
		}
		return annotations.get(index);
	}
	
	public ArrayList<String> getGlobalVarAnnotations(String index) {
		if(!globalVarAnnotations.containsKey(index)) {
			ArrayList<String> globalAnnotations = new ArrayList<String>();
			globalVarAnnotations.put(index, globalAnnotations);
		} 
		return globalVarAnnotations.get(index);
	}

	//kindOfIndex is method_body, names of formal parameters,
	//names of global variables
	public void addAnnotations(ArrayList<String> varNames,
			String kindOfindex) {
		ArrayList<String> theNames = getAnnotations(kindOfindex);
		for(String varName : varNames) {
			if(!theNames.contains(varName))
				theNames.add(varName);
		}
	}

	
	public void addGlobalAnnotations(ArrayList<String> globalVars,
			String kindOfIndex) {
		ArrayList<String> theNames = getGlobalVarAnnotations(kindOfIndex);
		for(String globalVarName : globalVars) {
			if(!theNames.contains(globalVarName))
				theNames.add(globalVarName);
		}
		
	}
	
	public String toString() {
		String retVal = "";
		retVal = retVal.concat(Integer.toString(source)+"["+
				Integer.toString(position)+"] ==>" + this.targets.toString() +
				": Annotations ==>" + this.annotations.toString() + 
				": Global Annotations ==>" + this.globalVarAnnotations.toString());
		return retVal;
	}
}
