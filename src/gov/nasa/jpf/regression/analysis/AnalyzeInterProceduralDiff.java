package gov.nasa.jpf.regression.analysis;

import edu.byu.cs.analysis.InstructionNode;
import edu.byu.cs.analysis.VariableInfo;
import edu.byu.cs.search.heuristic.pefca.ReducedCallGraph;
import edu.byu.cs.trace.AbstractMethodInfo;
import gov.nasa.jpf.regression.callgraph.AnnotatedCallEdge;
import gov.nasa.jpf.regression.cfg.CFG;
import gov.nasa.jpf.regression.cfg.Node;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.bcel.generic.ArithmeticInstruction;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.DCMPG;
import org.apache.bcel.generic.DCMPL;
import org.apache.bcel.generic.FCMPG;
import org.apache.bcel.generic.FCMPL;
import org.apache.bcel.generic.FieldInstruction;
import org.apache.bcel.generic.GETFIELD;
import org.apache.bcel.generic.GETSTATIC;
import org.apache.bcel.generic.IINC;
import org.apache.bcel.generic.IfInstruction;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InvokeInstruction;
import org.apache.bcel.generic.LCMP;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.POP;
import org.apache.bcel.generic.PUTFIELD;
import org.apache.bcel.generic.PUTSTATIC;
import org.apache.bcel.generic.ReturnInstruction;
import org.apache.bcel.generic.StoreInstruction;
import org.apache.bcel.generic.Type;
import org.apache.bcel.verifier.structurals.InstructionContext;

import com.sun.tools.javac.util.Pair;


public class AnalyzeInterProceduralDiff extends
				AnalyzeIntraProceduralDiff {

	// A map that contains for each conditional branch position the
	// set of invoke(INVOKESPECIAL, INVOKESTATIC, ... etc) instructions
	// that are control dependent on the conditional branch (either
	// true or false).
	Map<Integer, ArrayList<Integer>> branchToDependentInvokeInsMap;
	
	/*
	 * A map that contains a mapping for each conditional branch to
	 * the set of GET instructions (GETFIELD and GETSTATIC) that are
	 * control dependent on it.
	 */
	Map<Integer, ArrayList<Integer>> branchToDependentGETInstructionsMap;
	
	/*
	 * A map that contains a mapping for each conditional branch to
	 * the set of PUT instructions (PUTFIELD and PUTSTATIC) that are
	 * control dependent on it.
	 */
	Map<Integer, ArrayList<Integer>> branchToDependentPUTInstructionsMap;

	/*
	 * the set of invoke instruction positions that are affected
	 */
	Set<Integer> globalTrackInvoke;

	//set of return instruction positions that are affected
	Set<Integer> globalTrackReturn;
	
	//set of GET FieldInstruction positions that are affected
	Set<Integer> affectedGETInstructions;

	Set<Integer> AWNPrime;
	Set<Integer> ACNPrime;
	
	// AffectedGlobals object for tracking the affected globals and affected references
	AffectedGlobals affectedGlobals;

	HashMap<Integer, ArrayList<String>> insnToVarNames;
	HashMap<Integer, ArrayList<Integer>> insnToComplex;
	HashMap<Integer, ArrayList<Integer>> insnToRetVal;

	/*
	 * CallEdges An ArrayList of AnnotatedCallEdges which will be
	 * annotated with information about affected arguments.
	 */
	ArrayList<AnnotatedCallEdge> callEdges;

	/* TODO: This may be removed now.
	 * A distance matrix based on the program's call graph
	 */
	DistanceMatrix callGraphDM;


	protected HashMap<Integer, ArrayList<Integer>>
											forwardLookupMap;

	protected HashMap<String, AffectedNodes> allAffectedNodes;

	/**
	 * the set of all invocation positions in the current method
	 */
	Set<Integer> allInvocationPositions;

	/**
	 * the set of all return positions in the current method
	 */
	Set<Integer> allReturnPositions;
	
	/**
	 * the set of all GET instruction positions in the current method
	 */
	Set<Integer> allGETInsnPositions;
	
	/**
	 * the set of all PUT instruction positions in the current method
	 */
	Set<Integer> allPUTInsnPositions;
	
	/**
	 * the set of all global variables in the class
	 */
	Set<String> globalVariables;
	
	/**
	 * the set of all global variables that are objects
	 * (as opposed to primitives)
	 */
	Set<String> globalObjectVariables;
	
	/**
	 * the map of positions to GET instructions, this is built when the
	 * constructor is called.
	 */
	Map<Integer, FieldInstruction> posnToGETInsnMap;
	
	/**
	 * the map of positions to PUT instructions, this is built when the
	 * constructor is called.
	 */
	Map<Integer, FieldInstruction> posnToPUTInsnMap;
	
	/**
	 * this maps global variable names to the first uses of them in
	 * the CFG (basically the GET FieldInstructions).
	 */
	Map<String, ArrayList<Integer>> fieldFirstUsesMap;
	
	/**
	 * this maps object names (e.g. this, e1, e2, etc.) to the set
	 * of GET FieldInstruction positions that use that object as a
	 * reference. If e1.a is at position 23, then you will add position
	 * 23 to the ArrayList mapped to e1.
	 * Note: this will need to be constructed before the
	 * qualifiedObjectNameToFieldPosnMap can be constructed.
	 */
	Map<String, ArrayList<Integer>> objectNameToFieldPosnMap;
	
	/**
	 * this maps qualified object names (e.g. heap.Example02.e1) to the set
	 * of GET FieldInstruction positions that use that object as a reference.
	 * If heap.Example02.e1.a is at position 23, then you will add position
	 * 23 to the ArrayList mapped to heap.Example02.e1.
	 * Note: this will be constructed after the objectNameToFieldPosnMap has
	 * been completely constructed.
	 */
	Map<String, ArrayList<Integer>> qualifiedObjectNameToFieldPosnMap;
	
	/**
	 * this maps field names (e.g. this, e1, e2, etc.) to the set
	 * of GET FieldInstruction positions that they are referenced at.
	 * For instance, if you have a GET instruction for e1 at position 12,
	 * then a mapping between e1 and the list containing 12 will be added
	 * to the map.
	 */
	Map<String, ArrayList<Integer>> fieldNameToUsesMap;
	
	/**
	 * affectedObjectFields - this is the set of positions at which
	 * there is a field of an object that may be affected.
	 */
	Set<Integer> affectedObjectFields;
	
	/**
	 * getsToDominatingPutsMap - this is a mapping of GETFIELD instructions
	 * to the set of PUTFIELD instructions that dominate that GET.
	 */
	Map<Integer, ArrayList<Integer>> getsToDominatingPutsMap;
	
	/**
	 * this is a flag that dictates whether or not we are clever with the heap
	 * analysis in order to get reductions. The flag defaults to false. Switch
	 * it to true in order to use the heap stuff.
	 */
	public boolean useHeapReductions = true;

	String kindOfIndex = "method_body";

	static boolean debug = false;

	public AnalyzeInterProceduralDiff(
			AbstractMethodInfo absMethodInfo, CFG cfg, MethodGen mg) {
		super(absMethodInfo, cfg, mg);
		branchToDependentInvokeInsMap = new
							HashMap<Integer, ArrayList<Integer>>();
		branchToDependentGETInstructionsMap =
			new HashMap<Integer, ArrayList<Integer>>();
		branchToDependentPUTInstructionsMap =
			new HashMap<Integer, ArrayList<Integer>>();
		globalTrackInvoke = new HashSet<Integer>();
		globalTrackReturn = new HashSet<Integer>();
		insnToVarNames = new HashMap<Integer, ArrayList<String>>();
		insnToComplex = new HashMap<Integer, ArrayList<Integer>>();
		insnToRetVal = new HashMap<Integer, ArrayList<Integer>>();
		affectedGlobals = new AffectedGlobals();

		AWNPrime = new HashSet<Integer>();
		ACNPrime = new HashSet<Integer>();
		
		this.affectedGETInstructions = new HashSet<Integer>();

		this.callEdges = new ArrayList<AnnotatedCallEdge>();

		allInvocationPositions = new HashSet<Integer>();
		allReturnPositions = new HashSet<Integer>();

		this.globalVariables = new HashSet<String>();
		this.globalObjectVariables = new HashSet<String>();
		
		forwardLookupMap = new HashMap<Integer, ArrayList<Integer>>();
		allAffectedNodes = new HashMap<String, AffectedNodes>();
		
		this.posnToGETInsnMap = new HashMap<Integer, FieldInstruction>();
		this.posnToPUTInsnMap = new HashMap<Integer, FieldInstruction>();
		
		this.allGETInsnPositions = new HashSet<Integer>();
		this.allPUTInsnPositions = new HashSet<Integer>();
		
		this.fieldFirstUsesMap = new HashMap<String, ArrayList<Integer>>();
		this.objectNameToFieldPosnMap = new HashMap<String, ArrayList<Integer>>();
		this.qualifiedObjectNameToFieldPosnMap =
				new HashMap<String, ArrayList<Integer>>();
		this.fieldNameToUsesMap = new HashMap<String, ArrayList<Integer>>();
		
		this.affectedObjectFields = new HashSet<Integer>();
		
		this.getsToDominatingPutsMap = new HashMap<Integer, ArrayList<Integer>>();

		this.buildInvocationAndReturnSet();
		this.buildPutAndGetSets();
		this.buildForwardLookup();
		
		// This is the initialization for the Heap analysis
		// It sets up the data structures that are needed
		// - put together maps based on GET and PUT sets
		if(this.useHeapReductions) { 
			this.buildFieldNameToUsesMap();
			
			// map object names to positions that reference them
			this.buildObjectNameToFieldPosnMap();
			// map qualified object names to positions that reference them
			this.buildQualifiedObjectNameToFieldPosnMap();
		}
	}

	public ArrayList<AnnotatedCallEdge> getAnnotatedCallEdges() {
		return callEdges;
	}

	public AnnotatedCallEdge getAnnotatedCallEdge(int callSite) {
		for(AnnotatedCallEdge edge : callEdges) {
			if(edge.getPosition() == callSite) {
				return edge;
			}
		}
		return null;
	}

	public void setIndex(String theIndex) {
		kindOfIndex = theIndex;
	}

	public void addToAffectedNodeSet(String index) {
		AffectedNodes affNodes = new AffectedNodes();
		
		affNodes.addNodes(globalTrackCond, globalTrackWrite,
								globalTrackInvoke, globalTrackReturn,
								canbeDropped);
		checkVariables();
		allAffectedNodes.put(index, affNodes);
	}
	
	protected void checkVariables() {
		ArrayList<String> affVars = getAllAffectedVariables(globalTrackCond, 
						 	globalTrackWrite, globalTrackReturn);
		//System.out.println(affVars.toString());
	}

	protected void clearAllAffectedDataStructures() {
		globalTrackCond.clear();
		globalTrackWrite.clear();
		globalTrackInvoke.clear();
		globalTrackReturn.clear();
		trackCond.clear();
		AWNPrime.clear();
		ACNPrime.clear();
		canbeDropped.clear();
	}

	public Set<Integer> getGlobalTrackInvoke() {
		assert (allAffectedNodes.containsKey("method_body"));
		return allAffectedNodes.get("method_body").
					getAffectedCallNodes();
	}

	public Set<Integer> getGlobalTrackWrite() {
		assert (allAffectedNodes.containsKey("method_body"));
		return allAffectedNodes.get("method_body").
						getAffectedWriteNodes();
	}

	public Set<Integer> getGlobalTrackCondition() {
		assert (allAffectedNodes.containsKey("method_body"));
		return allAffectedNodes.get("method_body").
						getAffectedCondNodes();
	}

	public Set<Integer> getGlobalTrackReturn() {
		assert (allAffectedNodes.containsKey("method_body"));
		return allAffectedNodes.get("method_body").
						getAffectedReturnNodes();
	}

	/**
	 * initializeAnnotatedCallEdges - initializes this semanticDiff's
	 * set of AnnotatedCallEdges with information from the callEdges
	 * data structure from the ReducedCallGraph. Once these are setup
	 * then they can be annotated by the interprocedural analysis.
	 * @param callEdges
	 */
	public void initializeAnnotatedCallEdges(
			HashMap<Integer, ArrayList<Integer>> sourceEdges, Integer callGraphIndex) {

			if(sourceEdges == null) return;
			// iterate over the sourceEdges, each is a different callEdge
			// integer is the position of the call site
			// ArrayList<integer> are the target indices
			Iterator<Integer> posIter = sourceEdges.keySet().iterator();

			while(posIter.hasNext()) {
				Integer position = posIter.next();
				ArrayList<Integer> targets = sourceEdges.get(position);

				// Create the new AnnotatedCallEdge and add it to the list
				this.callEdges.add(
						new AnnotatedCallEdge(callGraphIndex, position, targets));
			}

	} // end of initializeAnnotatedCallEdges

	public void mapInputParameters(ArrayList<Pair<String, String>> actualParam) {
		//input parameters to the method

		HashMap<Integer, ArrayList<VariableInfo>> args = absMethodInfo.getArguments();
		Iterator<Integer> argItr = args.keySet().iterator();
		while(argItr.hasNext()) {
			Integer argIndex = argItr.next();
			System.out.println("callSite Position : " + argIndex);
			ArrayList<VariableInfo> vars = args.get(argIndex);
			System.out.println("arguments " + vars.toString());
		}
	}

	/**
	 * analyzeInvokeAndReturnInstructions - finds all the instructions
	 * from the instruction contexts that are either invoke or return
	 * instructions. It then makes a call to checkVariablesUsedInIns
	 * which looks at the variables involved and does bookkeeping on
	 * them by storing them in the insnToVarNames and insnToComplex
	 * data structures which are conceptually similar to opToVarNames
	 * and opToOtherOp data structures used in the intraprocedural
	 * analysis.
	 */
	public void analyzeInvokeAndReturnInstructions() {
		HashMap<Integer, InstructionContext> inContext = absMethodInfo.
			getInstructionContexts();

		Iterator<Integer> contextItr = inContext.keySet().iterator();

		while(contextItr.hasNext()) {
			Integer position = contextItr.next();
			InstructionContext ic = inContext.get(position);
			Instruction insn = ic.getInstruction().getInstruction();
			if(insn instanceof InvokeInstruction ||
					insn instanceof ReturnInstruction) {
				checkVariablesUsedInInstructions(position, ic,
									insnToVarNames, insnToComplex, insnToRetVal);
			}
		}
	}
	
	/**
	 * buildFieldFirstUsesMap - this method performs a depth-first
	 * traversal of the CFG for the current method to find the first
	 * use(s) of fields in the method. It looks for getField insns
	 * and then checks the variable name against a map of fields to
	 * get their first positions.
	 */
	public void buildFieldFirstUsesMap() {
		
		Map<String, ArrayList<Integer>> fieldFirstUses =
			new HashMap<String, ArrayList<Integer>>();
		
		// grab the root node and look at its positions
		if(!this.kindOfIndex.equals("method_body")) return;
		Node rootNode = this.cfg.getRootNode();
		
		// the fieldFirstUses map will be populated with all the
		// positions that fields are first used
		this.processNodeForFieldFirstUses(rootNode, fieldFirstUses,
				new HashSet<Integer>());
		
		this.fieldFirstUsesMap = fieldFirstUses;
	}
	
	/**
	 * buildFieldNameToUsesMap: none -> void
	 * this method will go through the set of all GET FieldInstructions
	 * in order to map the name of the Field to the list of positions that
	 * the particular instructions appear.
	 * For instance, if there is a GET field instruction for 'e1' at posn
	 * 12 and 28, then there will be a key-value pair in the map:
	 * 	'e1' -> {12,28}
	 * 
	 * Temporal assertion: this method needs to be called after the 
	 * allGETInsnPositions set is built in this.buildPutAndGetSets().
	 */
	public void buildFieldNameToUsesMap() {
		
		// the map has already been initialized in the constructor
		// this.fieldNameToUsesMap
		
		// need to get at the stack for the field instructions, which can
		// be accessed from the Instruction nodes located in the
		// ControlPathAnalysis HashMap provided by AbstractMethodInfo.
		HashMap<Integer, InstructionNode> controlPathAnalysis =
				this.absMethodInfo.getControlPathAnalysisInfo();
		
		// iterate over each of the positions in the set of GETs
		for(Integer getInsnPosn : this.allGETInsnPositions) {
			
			// TODO: Remove this, it isn't needed here
			InstructionContext ic =
					this.absMethodInfo.getInstructionContexts().get(getInsnPosn);
			
			// Get the instruction node and StackFrame for this instruction
//			InstructionNode currInsnNode = controlPathAnalysis.get(getInsnPosn);
//			ArrayList<VariableInfo> stackFrame =
//					currInsnNode.getNodeInfo().getStackFrame();
//			
//			
//			String variableName = stackFrame.get(0).getVariableName().toString();
			
			String variableName = this.getUnqualifiedVariableName(getInsnPosn);
			
			if(!this.fieldNameToUsesMap.containsKey(variableName)) {
				this.fieldNameToUsesMap.put(variableName, new ArrayList<Integer>());
			}
			this.fieldNameToUsesMap.get(variableName).add(getInsnPosn);
		}
	}
	
	/**
	 * buildObjectNameToFieldPosnMap: none -> void
	 * this method will go through the set of all Get FieldInstructions
	 * and for each that is not of a primitive type, the name its owner/
	 * parent will be the key of the map (e.g. 'this', e1, etc.) and the
	 * array list of instruction positions where fields of that object
	 * are accessed (GET FieldInstructions) will be the value.
	 * That is, Map<String, ArrayList<Integer>>
	 */
	public void buildObjectNameToFieldPosnMap() {
		
		// initialize the map that is being built in this method
		Map<String, ArrayList<Integer>> mapping =
				new HashMap<String, ArrayList<Integer>>();
		
		// go through the set of GET Field Instructions to look at
		// how the getFieldReferenceString works on them.
		for(Integer getInsnPosn : this.allGETInsnPositions) {
			
			InstructionContext ic =
					this.absMethodInfo.getInstructionContexts().get(getInsnPosn);
			
			/*
			 * Information Needed:
			 * - Current Instruction Position
			 * - Field Reference Variable Name
			 */
			// already have posn -> getInsnPosn
			String fieldRefVarName = this.getFieldReferenceVariableName(ic);
			
			if(mapping.containsKey(fieldRefVarName)) {
				mapping.get(fieldRefVarName).add(getInsnPosn);
			}
			else {
				mapping.put(fieldRefVarName, new ArrayList<Integer>());
				mapping.get(fieldRefVarName).add(getInsnPosn);
			}
		}
		
		// lastly, need to set the global as the produced mapping
		this.objectNameToFieldPosnMap = mapping;
	}
	
	/**
	 * buildQualifiedObjectNameToFieldPosnMap: none -> void
	 * this method maps fully qualified object names to the positions of GET
	 * field instructions that reference them. This method will call the
	 * getQualifiedFieldReference() method and map the resulting string to
	 * an ArrayList of GET FieldInstruction positions. Once the mapping has
	 * been built, this.qualifiedObjectNameToFieldPosnMap will be set to that
	 * mapping.
	 */
	public void buildQualifiedObjectNameToFieldPosnMap() {
		
		// initialize the map that is being built in this method
		Map<String, ArrayList<Integer>> mapping =
				new HashMap<String, ArrayList<Integer>>();
		
		// go through the set of GET Field Instructions
		for(Integer getInsnPosn : this.allGETInsnPositions) {

			String qualifiedFieldRef =
					this.constructQualifiedFieldReference(getInsnPosn);
			
			if(mapping.containsKey(qualifiedFieldRef)) {
				mapping.get(qualifiedFieldRef).add(getInsnPosn);
			}
			else {
				mapping.put(qualifiedFieldRef, new ArrayList<Integer>());
				mapping.get(qualifiedFieldRef).add(getInsnPosn);
			}
		}
		
		// lastly, need to set the global as the produced mapping
		this.qualifiedObjectNameToFieldPosnMap = mapping;
	}
	
	/**
	 * getFieldReferenceVariableName: Integer -> String
	 * this does exactly the same thing as the other
	 * getFieldReferenceVariableName, except that it takes an Integer as
	 * an argument which represents the position of the field reference.
	 * This is just a convenience, so that callers don't have to do the
	 * extra lookup of the InstructionContext themselves.
	 * 
	 * @param position
	 * 			the position of the Field reference instruction
	 * @return String
	 * 			the qualified variable name of the field
	 */
	public String getFieldReferenceVariableName(Integer position) {
		
		InstructionContext ic =
				this.absMethodInfo.getInstructionContexts().get(position);
		return this.getFieldReferenceVariableName(ic);
	}
	
	
	/**
	 * getFieldReferenceVariableName: InstructionContext -> String
	 * given an InstructionContext object for a FieldInstruction (namely a
	 * GET FieldInstruction), this function will look at one of the
	 * predecessors to see how this Field was referenced. It will then
	 * get the name of the variable that referenced this field and return
	 * that name.
	 * 
	 * @param ic
	 * 			the InstructionContext object for the field of interest
	 * @return String
	 * 			the name of the variable that referenced the field
	 */
	public String getFieldReferenceVariableName(InstructionContext ic) {
		
		HashMap<Integer, InstructionNode> controlPathAnalysis =
				absMethodInfo.getControlPathAnalysisInfo();
		HashMap<Integer, ArrayList<Integer>> allPreds =
						absMethodInfo.getPredecessors();
		
		Integer posn = ic.getInstruction().getPosition();
		int toPop = ic.getInstruction().getInstruction().
								consumeStack(mg.getConstantPool());
		ArrayList<Integer> preds = allPreds.get(posn);
		String variableName = "";
		
		if(preds == null || preds.size() == 0) {
			return variableName;
		}
		
		int predPosition = preds.get(0);
		InstructionNode predIn = controlPathAnalysis.get(predPosition);
		ArrayList<VariableInfo> stackFrame =
						predIn.getNodeInfo().getStackFrame();
		int startIndex = stackFrame.size() - 1;

		for(int varIndex = startIndex;
			(varIndex >=0 && varIndex > (startIndex - toPop));
			varIndex--) {
			
			VariableInfo vi = stackFrame.get(varIndex);
			if(variableName != "") {
				variableName += "." + vi.getVariableName();
			}
			else {
				variableName = (String)vi.getVariableName();
			}
		}
		
		return variableName;
	}
	
	/**
	 * processNodeForFieldFirstUses: Node, Map<String, ArrayList<Integer>> -> void
	 * given a Node and a Map of the getFields discovered along this
	 * path so far, this method will continue the depth-first
	 * traversal of the CFG after first processing this node's insn
	 * positions for more *new* getFields.
	 * This is intended to be invoked only by the
	 * buildFieldFirstUsesMap() method.
	 * 
	 * @param currNode
	 * 			the Node being currently visited on this traversal
	 * @param fieldFirstUses
	 * 			the map of Field varNames to the posn of first use
	 */
	public void processNodeForFieldFirstUses(Node currNode,
			Map<String, ArrayList<Integer>> fieldFirstUses,
			Set<Integer> visited) {
		
		/*
		 * this method is called recursively on successors nodes that
		 * could be part of a loop, so we have a visited set to keep
		 * track of what we have already seen. If the currNode is
		 * contained in visited, then simply return.
		 */
		if(visited.contains(currNode.getID())) return;
		// now that we are visiting currNode, add its ID to visited.
		visited.add(currNode.getID());
				
		// first, process all the positions for this node
		for(Integer currPosn : currNode.getPositions()) {
		
			/*
			 * try to get a FieldInstruction for the currPosn from the
			 * posnToGETInsnMap, if null is returned (which is most
			 * likely) then we will simply move onto the next insn.
			 * However, if a FieldInstruction is returned, then we
			 * can update the fieldFirstUses map accordingly.
			 */
			FieldInstruction insn = this.posnToGETInsnMap.get(currPosn);
			if(insn != null) {
				
				// qualified field name
				String qualifiedFieldName =
						this.constructQualifiedFieldName(currPosn);
				
				/*
				 * Since we are building a map of the possible first uses of a
				 * field, we only want fields that are being visited for the
				 * first time along this path. Because of branching, there may
				 * be multiple paths with first uses of a particular field,
				 * this is handled by the map union that happens in the lower
				 * for loop.
				 */
				if(fieldFirstUses.get(qualifiedFieldName) == null) {
					ArrayList<Integer> posnList = new ArrayList<Integer>();
					posnList.add(currPosn);
					fieldFirstUses.put(qualifiedFieldName, posnList);
				}
			}
		}
		
		// now process this nodes successors //
		
		// go through all the successors and make recursive calls
		for(Node succNode : currNode.getSuccessorList()) {
			
			// create a deep copy that can be used to make subsequent copies
			// the deep copy ensures that when there is a _first_ use along one
			// the path down from successor and another _first_ use along the path
			// down the other successor, then both the uses are detected and marked
			
			// create copy for the recursive call
			Map<String, ArrayList<Integer>> currMapCopy = 
							new HashMap<String, ArrayList<Integer>>();
			currMapCopy.putAll(fieldFirstUses);
			
			
			// only process the successor if it hasn't already been visited
			this.processNodeForFieldFirstUses(succNode, currMapCopy, visited);
			
			fieldFirstUses.putAll(currMapCopy);
		}
	}
	
	/**
	 * generatePosnToFieldInsnMaps - using the instruction contexts
	 * from the abstract method info, collect all the occurrences of
	 * GETs and PUTs for fields and map them to their position. This
	 * is a preliminary data structure to capture the available
	 * FieldInstructions that may be of interest to the analysis.
	 */
	public void generatePosnToFieldInsnMaps() {
		HashMap<Integer, InstructionContext> inContext =
			absMethodInfo.getInstructionContexts();
		HashMap<Integer, InstructionNode> controlPathAnalysis =
				this.absMethodInfo.getControlPathAnalysisInfo();
		
		Iterator<Integer> contextItr = inContext.keySet().iterator();
		
		while(contextItr.hasNext()) {
			Integer posn = contextItr.next();
			InstructionContext ic = inContext.get(posn);
			Instruction insn = ic.getInstruction().getInstruction();
			if(insn instanceof FieldInstruction) {
				// a FieldInstruction can be one of 4 things...
				// if it is a PUT instruction, then put it in posnToPUTInsnMap
				if(insn instanceof PUTFIELD || insn instanceof PUTSTATIC) {
					FieldInstruction fieldInsn = (FieldInstruction)insn;
					Type t1 = fieldInsn.getType(absMethodInfo.getMethodGen().getConstantPool());
					
					this.posnToPUTInsnMap.put(posn, fieldInsn);
				}
				// otherwise it must be a GET, so put it in posnToGETInsnMap
				else {
					assert(insn instanceof GETFIELD || insn instanceof GETSTATIC);
					FieldInstruction fieldInsn = (FieldInstruction)insn;
					this.posnToGETInsnMap.put(posn, fieldInsn);
					
					// Look at the stackframe for the variable
//					ArrayList<VariableInfo> stackFrame =
//							controlPathAnalysis.get(posn).getNodeInfo().getStackFrame();
				}
			}
		}
	}
	
	/**
	 * TODO: Not fully-implemented, may need removal
	 * mapObjectsToFieldAccesses - given a set of qualified object
	 * names, this method goes through them comparing against the
	 * GET FieldInstructions. Those that match are mapped to the
	 * Object name.
	 * @param objectNames
	 */
	public void mapObjectsToFieldAccesses(Set<String> objectNames) {
		
		ConstantPoolGen cpGen = this.mg.getConstantPool();
		
		for(String objectName : objectNames) {
			
			for(Integer getPosn : this.allGETInsnPositions) {
				FieldInstruction getInsn = this.posnToGETInsnMap.get(getPosn);
				
				String fieldName = getInsn.getLoadClassType(cpGen).getClassName()
					+ "." + getInsn.getFieldName(cpGen);
				
				if(fieldName.startsWith(objectName)) {
					if(fieldName.equals(objectName)) {
						// this object field is what is being accessed by the GET FieldInstruction
						
					}
					else {
						// a field of this object is being accessed by the GET FieldInstruction
						
					}
				}
			}
		}
	}
	
	/**
	 * buildPutAndGetSets - using the map of instruction contexts,
	 * this method finds each FieldInstruction and then for those
	 * that are PUT instructions, it stores the integer positions in
	 * a set and likewise for the GET instructions.
	 */
	public void buildPutAndGetSets() {
		HashMap<Integer, InstructionContext> inContext =
			absMethodInfo.getInstructionContexts();
		
		Iterator<Integer> contextItr = inContext.keySet().iterator();
		
		while(contextItr.hasNext()) {
			Integer posn = contextItr.next();
			InstructionContext ic = inContext.get(posn);
			Instruction insn = ic.getInstruction().getInstruction();
			if(insn instanceof FieldInstruction) {
				// a FieldInstruction can be one of 4 things...
				// if it is PUTFIELD or PUTSTATIC, then add it to the PUT set
				if(insn instanceof PUTFIELD || insn instanceof PUTSTATIC) {
					this.allPUTInsnPositions.add(posn);
				}
				// if it is GETFIELD or GETSTATIC, then add it to the GET set
				else if(insn instanceof GETFIELD || insn instanceof GETSTATIC) {
					this.allGETInsnPositions.add(posn);
				}
			}
		}
	}
	
	/*
	 * TODO: the methods generateGETInstructionsControlDependentOnBranches
	 * and generatePUTInstructionsControlDependentOnBranches (and possibly
	 * other similar methods) can be factored into a single method that
	 * will then only iterate the map of secondary positions once, thus
	 * reducing some unnecessary cost.
	 */
	// TODO: Remove this method, it isn't being used in any way.
	/**
	 * generateInstructionSetsControlDependentOnBranches - this
	 * method goes through the map of secondary instruction positions
	 * and sifts them into the appropriate maps for InvokeInstructions,
	 * GET Instructions, PUT Instructions. These maps are global.
	 */
//	public void generateInstructionSetsControlDependentOnBranches() {
//		
//		// Iterate over the mapping of secondary instructions
//		Iterator<Integer> secondaryIter =
//			this.branchToDependentSecondaryInsMap.keySet().iterator();
//
//		while(secondaryIter.hasNext()) {
//			
//			// get the position of the current branch instruction
//			Integer branchPosn = secondaryIter.next();
//			
//			/*
//			 * get the list of secondary instruction positions
//			 * related to the branch instruction.
//			 */
//			ArrayList<Integer> secondaries =
//				this.branchToDependentSecondaryInsMap.get(branchPosn);
//			
//			//*** GET Instructions ***//
//			// get the secondary positions that are GET instructions
//			ArrayList<Integer> GETInstructions =
//				filterGETInstructionPositions(secondaries);
//			
//			this.branchToDependentGETInstructionsMap.put(
//					branchPosn, GETInstructions);
//			
//			//*** PUT Instructions ***//
//			// get the secondary positions that are PUT instructions
//			ArrayList<Integer> PUTInstructions =
//				filterPUTInstructionPositions(secondaries);
//			
//			this.branchToDependentPUTInstructionsMap.put(
//					branchPosn, PUTInstructions);
//		}
//	}
	
	/**
	 * filterFieldInstructionPositions - given a list of bytecode
	 * positions, this method will sift through them aggregating and
	 * returning those that are positions of FieldInstructions.
	 * @param positions
	 * 			An ArrayList of bytecode positions for filtering
	 * @return ArrayList<Integer>
	 * 			The resulting list of FieldInstruction positions
	 */
	public ArrayList<Integer> filterFieldInstructionPositions(ArrayList<Integer> positions) {
		
		ArrayList<Integer> fieldInstructionPositions = new ArrayList<Integer>();
		
		/*
		 * iterate over the positions and check the type of associated
		 * instruction, adding to fieldInstructionPositions only when
		 * it is a FieldInstruction
		 */
		for(Integer position : positions) {
			
			// get the actual instruction
			InstructionContext ic = this.absMethodInfo.getInstructionContexts().get(position);
			Instruction instruction = ic.getInstruction().getInstruction();
			
			// check if that instruction is a FieldInstruction
			if(instruction instanceof FieldInstruction) {
				// to avoid passing the same Integer objects around,
				// use the .intValue() to get a new Integer
				fieldInstructionPositions.add(position.intValue());
			}
		}
		
		return fieldInstructionPositions;
	}
	
	/**
	 * filterGETInstructionPositions - given a list of bytecode
	 * positions, this method will sift through them aggregating and
	 * returning those that are positions of GET FieldInstructions.
	 * @param positions
	 * 			An ArrayList of bytecode positions for filtering
	 * @return ArrayList<Integer>
	 * 			The resulting list of GET FieldInstruction positions
	 */
	public ArrayList<Integer> filterGETInstructionPositions(ArrayList<Integer> positions) {
		
		ArrayList<Integer> GETInstructionPositions = new ArrayList<Integer>();
		
		/*
		 * iterate over the positions and check the type of associated
		 * instruction, adding to GETInstructionPositions only when
		 * it is a GET Instruction
		 */
		for(Integer position : positions) {
			
			// get the actual instruction
			InstructionContext ic = this.absMethodInfo.getInstructionContexts().get(position);
			Instruction instruction = ic.getInstruction().getInstruction();
			
			// check if that instruction is a GET instruction
			if(instruction instanceof GETFIELD || instruction instanceof GETSTATIC) {
				// to avoid passing the same Integer objects around,
				// use the .intValue() to get a new Integer
				GETInstructionPositions.add(position.intValue());
			}
		}
		
		return GETInstructionPositions;
	}
	
	/**
	 * filterPUTInstructionPositions - given a list of bytecode
	 * positions, this method will sift through them aggregating and
	 * returning those that are positions of PUT FieldInstructions.
	 * @param positions
	 * 			An ArrayList of bytecode positions for filtering
	 * @return ArrayList<Integer>
	 * 			The resulting list of PUT FieldInstruction positions
	 */
	public ArrayList<Integer> filterPUTInstructionPositions(ArrayList<Integer> positions) {
		
		ArrayList<Integer> PUTInstructionPositions = new ArrayList<Integer>();
		
		/*
		 * iterate over the positions and check the type of associated
		 * instruction, adding to PUTInstructionPositions only when
		 * it is a PUT Instruction
		 */
		for(Integer position : positions) {
			
			// get the actual instruction
			InstructionContext ic = this.absMethodInfo.getInstructionContexts().get(position);
			Instruction instruction = ic.getInstruction().getInstruction();
			
			// check if that instruction is a PUT instruction
			if(instruction instanceof PUTFIELD || instruction instanceof PUTSTATIC) {
				// to avoid passing the same Integer objects around,
				// use the .intValue() to get a new Integer
				PUTInstructionPositions.add(position.intValue());
			}
		}
		
		return PUTInstructionPositions;
	}
	
	/**
	 * genGETInstructionsCDOnBranches - this method finds the sets of GET
	 * FieldInstructions that are control dependent on a conditional branch
	 * and maps the position of that branch instruction to the list of
	 * positions of those GET FieldInstructions. THis list will be used as
	 * a reference throughout the propagation of impact in iDiSE.
	 */
	public void genGETInstructionsCDOnBranches() {
		
		// What do we have:
		// - mapping of varNames to set of branch posn
		// - list of all GET instruction positions
		// So we can look at the field reference name of a get instruction
		// and see if it is a key in the aforementioned map, if so, then
		// for each branch posn, map that branch posn to the list of GET
		// FieldInstruction positions.
		if (debug)
			System.out.println(this.allGETInsnPositions.toString());
		
		for(Integer getInsnPosn : this.allGETInsnPositions) {
			
			String varName = this.getFieldReferenceVariableName(getInsnPosn);
			InstructionContext ic = this.absMethodInfo.getInstructionContexts().get(getInsnPosn);
			String varString = this.getFieldReferenceString(ic, false);
			String qualifiedVarName = this.getQualifiedVariableName(getInsnPosn);
			
//			System.out.println("Looking at: " + getInsnPosn + "--" + varName + " -- " + varString + " -- " + qualifiedVarName);
//			System.out.println("          : " + this.stringTogetherFieldReferences(getInsnPosn));
//			System.out.println("          : " + this.getFullFieldReference(getInsnPosn, false));
			
			varName = this.constructQualifiedFieldName(getInsnPosn);
			
			if(this.varNameToCondBranchPosMap.keySet().contains(varName)) {
				
				for(Integer conditionalPosn :
					this.varNameToCondBranchPosMap.get(varName)) {
					
					if(this.branchToDependentGETInstructionsMap.get(conditionalPosn) == null) {
						this.branchToDependentGETInstructionsMap.put(conditionalPosn, new ArrayList<Integer>());
					}
					
					this.branchToDependentGETInstructionsMap.get(conditionalPosn).add(getInsnPosn);
				}
			}
		}
		
//		System.out.println("======%%%%======");
//		for(Integer condPosn : this.branchToDependentGETInstructionsMap.keySet()) {
//			System.out.println(" " + condPosn + " - " + this.branchToDependentGETInstructionsMap.get(condPosn).toString());
//		}
		
//		for(String varName : this.varNameToCondBranchPosMap.keySet()) {
//			System.out.println(varName + " - " +
//					this.varNameToCondBranchPosMap.get(varName).toString());
//		}
//		
//		// Iterate over the branch instructions and the lists of secondary
//		// instructions that are dependent on them
//		for(Integer currBranchPosn :
//			this.branchToDependentSecondaryInsMap.keySet()) {
//			
//			// Just printing some stuff to see what is data is available
////			ArrayList<String> varNames = new ArrayList<String>();
////			this.getVarsAssociatedWithIns(varNames, currBranchPosn);
////			System.out.println("%%%%%%%%%%%%");
////			System.out.println("- " + currBranchPosn + " - " + varNames.toString());
//			
//			// the list of secondary instructions dependent on the curr
//			// branch instruction posn
//			ArrayList<Integer> secondaries =
//					this.branchToDependentSecondaryInsMap.get(currBranchPosn);
//			
//			// loop through the secondaries, checking against the list of GET
//			// FieldInstructions that we have already accumulated.
//			for(Integer secondary : secondaries) {
//				
//				if(this.allGETInsnPositions.contains(secondary)) {
//					
//					// if the secondary is a GET FieldInstruction,
//					// then add it to the map
//					
//					// check if we can add it to an existing list or
//					// if we need to make a new list.
//					if(this.branchToDependentGETInstructionsMap.get(currBranchPosn) == null) {
//						this.branchToDependentGETInstructionsMap.put(currBranchPosn, new ArrayList<Integer>());
//					}
//					
//					this.branchToDependentGETInstructionsMap.get(currBranchPosn).add(secondary);
//				}
//			}
//		}
	}

	/**
	 * genInvokeInsCDOnBranches - finds the sets of invoke ins that
	 * are control dependent on a conditional branch and maps the
	 * position of that branch ins to the list of positions of those
	 * invoke ins. This list will be used as a reference throughout
	 * iDiSE.
	 */
	public void genInvokeInsCDOnBranches() {

		// Iterate over the mapping of secondary ins
		Iterator<Integer> secIter =
			this.branchToDependentSecondaryInsMap.keySet().iterator();

		while(secIter.hasNext()) {

			// get the position of the curr branch ins
			Integer bPos = secIter.next();

			// get the list of secondary ins for bPos
			ArrayList<Integer> secondaries = this.branchToDependentSecondaryInsMap.get(bPos);

			// loop through the secondary ins to find invoke ins
			for(int i = 0; i < secondaries.size(); i++) {

				// look at curr secondary ins position
				Integer sPos = secondaries.get(i);

				// get the actual instruction to check if it is invoke ins
				InstructionContext ic = this.absMethodInfo.getInstructionContexts().get(sPos);
				Instruction insn = ic.getInstruction().getInstruction();
				if(insn instanceof InvokeInstruction) {

					// check if we can add it to an existing list or
					// if we need to make a new list.
					if(this.branchToDependentInvokeInsMap.get(bPos) != null) {

						this.branchToDependentInvokeInsMap.get(bPos).add(sPos);
					}
					else {

						ArrayList<Integer> invokes = new ArrayList<Integer>();
						invokes.add(sPos);
						this.branchToDependentInvokeInsMap.put(bPos, invokes);
					}
				}
			}
		}

		if(ComputeDifferences.debug)
			System.out.println("Created branchToDependentInvokeInsMap: "
					+ this.branchToDependentInvokeInsMap.toString());

	} // end of genInvokeInsCDOnBranches

	public ArrayList<Integer> mapAnnotatedVarsToIndices(Integer callSite,
							ArrayList<String> annotatedVars) {
		ArrayList<Integer> argPositions = new ArrayList<Integer>();
		HashMap<Integer, InstructionContext> inContext =
			absMethodInfo.getInstructionContexts();
		HashMap<Integer, InstructionNode> controlPathAnalysis =
			absMethodInfo.getControlPathAnalysisInfo();
		HashMap<Integer, ArrayList<Integer>> allPreds =
			absMethodInfo.getPredecessors();


		InstructionContext ic = inContext.get(callSite);
		Instruction insn = ic.getInstruction().getInstruction();


		int toPop = insn.consumeStack(mg.getConstantPool());

		if(!allPreds.containsKey(callSite)) return argPositions;
		ArrayList<Integer> preds = allPreds.get(callSite);


		for(int predIndex = 0; predIndex < preds.size(); predIndex++) {
			int predPosition = preds.get(predIndex);
			InstructionNode predIn = controlPathAnalysis.get(predPosition);
			int counter = toPop;
			//System.out.println(predIn.getNodeInfo().getStackFrame().toString() + " ---->");
			ArrayList<VariableInfo> stack = predIn.getNodeInfo().getStackFrame();
			int startIndex =stack.size() -1;
			for(int stackIndex = startIndex;
			(stackIndex >= 0 && stackIndex > (startIndex - toPop));
			stackIndex--) {
				ArrayList<String> varNames = new ArrayList<String>();
				counter--;
				assert (counter >= 0);

				assert (stack.size() >= stackIndex);
				VariableInfo vi = stack.get(stackIndex);
				Integer varPos = vi.getPositionOfVariable();
				if((vi.getVariableName().toString().equals("__UNKNOWN__") ||
						vi.getVariableName().toString().equals("RETURN_ELEMENT")) &&
						!(vi.getVariableName().toString().equals("__CONSTANT_VAL__"))) {
					getVar(varPos, varNames);
					//System.out.println(varNames.toString() + " varnames %%%%%%%%");
				} else {
					varNames.add(vi.getVariableName().toString());
				}
				for(int annVarIndex = 0; annVarIndex < annotatedVars.size(); annVarIndex++) {
					String annVar = annotatedVars.get(annVarIndex);
					if(varNames.contains(annVar)) {
						argPositions.add(counter);
					}
				}
			}
		}
		return argPositions;
	}

	public void analyzeVarsFromModifiedMethods(Integer callSite,
			ArrayList<Integer> argPositions) {
		HashMap<Integer, InstructionContext> inContext =
							absMethodInfo.getInstructionContexts();
		HashMap<Integer, InstructionNode> controlPathAnalysis =
							absMethodInfo.getControlPathAnalysisInfo();
		HashMap<Integer, ArrayList<Integer>> allPreds =
							absMethodInfo.getPredecessors();


		InstructionContext ic = inContext.get(callSite);
		Instruction insn = ic.getInstruction().getInstruction();


		int toPop = insn.consumeStack(mg.getConstantPool());

		if(!allPreds.containsKey(callSite)) return;
		ArrayList<Integer> preds = allPreds.get(callSite);

		ArrayList<String> varNames = new ArrayList<String>();

		for(int predIndex = 0; predIndex < preds.size(); predIndex++) {
			int predPosition = preds.get(predIndex);
			InstructionNode predIn = controlPathAnalysis.get(predPosition);
			int counter = toPop;
			//System.out.println(predIn.getNodeInfo().getStackFrame().toString() + " ---->");
			ArrayList<VariableInfo> stack = predIn.getNodeInfo().getStackFrame();
			int startIndex =stack.size() -1;
			for(int stackIndex = startIndex;
			(stackIndex >= 0 && stackIndex > (startIndex - toPop));
			stackIndex--) {
				counter--;
				assert (counter >= 0);
				if(!argPositions.contains(counter)) {
					continue;
				}
				if(stack.size() < stackIndex) return;
				VariableInfo vi = stack.get(stackIndex);
				Integer varPos = vi.getPositionOfVariable();
				if((vi.getVariableName().toString().equals("__UNKNOWN__") ||
						vi.getVariableName().toString().equals("RETURN_ELEMENT")) &&
					!(vi.getVariableName().toString().equals("__CONSTANT_VAL__"))) {
				getVar(varPos, varNames);
				//System.out.println(varNames.toString() + " varnames %%%%%%%%");
				} else {
					varNames.add(vi.getVariableName().toString());
				}

			}
		}
		computeAffectedWriteVarLocs(varNames, callSite, new HashSet<String>());

	}

	private void getVar(int position, ArrayList<String> vars) {
		 getVar(position, vars, new HashSet<Integer>());
	}

	private void getVar(int position, ArrayList<String> vars,
											Set<Integer> visited) {
		visited.add(position);
		if(insnToVarNames.containsKey(position)) {
			vars.addAll(insnToVarNames.get(position));
		}
		if(opToVarNames.containsKey(position)) {
			vars.addAll(opToVarNames.get(position));
		}
		if(opToOtherOp.containsKey(position)) {
			ArrayList<Integer> otherOps = opToOtherOp.get(position);
			for(int other : otherOps) {
				if(visited.contains(other)) continue;
				getVar(other, vars, visited);
			}
		}
		if(opToRetValOfCallSites.containsKey(position)) {
			ArrayList<Integer> callSites =
						opToRetValOfCallSites.get(position);
			//do stuff with call sites
			for(int callSite : callSites) {
				ArrayList<String> callVars = new ArrayList<String>();
				this.getVarsAssociatedWithInvocAndReturn
							(callVars, callSite, new HashSet<Integer>());
				if(callVars.contains("this")) callVars.remove("this");
				AffectedNodes affNode = allAffectedNodes.get("method_body");
				affNode.affectedCallN.add(callSite);
				annotateCallEdge(callSite, callVars, "method_body");
				vars.addAll(callVars);
			}
		}
		if(insnToComplex.containsKey(position)) {
			ArrayList<Integer> others = insnToComplex.get(position);
			for(int other : others) {
				if(!visited.contains(other))
					getVar(other, vars, visited);
			}
		}
	}

	private void computeAffectedWriteVarLocs(ArrayList<String> varNames,
				Integer reachableCallSite, Set<String> exploredVarNames) {
		AffectedNodes affNodes = allAffectedNodes.get("method_body");


		ArrayList<String> newVars = new ArrayList<String>();

		// find all write statements that write to a variable in var name
		for(int varIndex = 0; varIndex < varNames.size(); varIndex++) {
			String varName = varNames.get(varIndex);
			if(!varNamesToWriteIns.containsKey(varName)) continue;
			ArrayList<Integer> writePoses = varNamesToWriteIns.get(varName);
			Iterator<Integer> writeItr = writePoses.iterator();
			while(writeItr.hasNext()) {
				Integer wPos = writeItr.next();
				boolean canbeReached = isReachable(wPos, reachableCallSite);
				if(canbeReached) {
					affNodes.affectedWriteN.add(wPos);
					getVar(wPos, newVars);
				}
			}
			exploredVarNames.add(varName);
		}
		newVars.removeAll(exploredVarNames);
		if(newVars.size() > 0) {
			computeAffectedWriteVarLocs(newVars, reachableCallSite, exploredVarNames);
		}
	}



	/**
	 * getVarsAssociatedWithInv - utilizes the insnToVarNames and
	 * insnToComplex maps to determine the names of the variables
	 * involved in the invoke instruction at a given position.
	 * FIXME: 'this' gets included in the list of varNames which is
	 * not one of the desired elements. Look into why this is happening
	 * and how to avoid it. Shouldn't cause problems in the meantime.
	 * @param varNames
	 * @param iPos
	 */
	public void getVarsAssociatedWithInvocAndReturn(ArrayList<String> varNames,
			Integer iPos, Set<Integer> visitedPos) {


		// check if iPos corresponds to anything in insnToVarNames
		if(this.insnToVarNames.containsKey(iPos)) {
			// it does, so add them to varNames
			varNames.addAll(this.insnToVarNames.get(iPos));
		}
		// check if iPos corresponds to anything in insnToCompl
		 if(this.insnToComplex.containsKey(iPos)) {
			// it does, so find the var names associated with these
			ArrayList<Integer> complexes = insnToComplex.get(iPos);

			// if you have already visited a complex, don't visit again
			for(Integer complex : complexes) {

				if(visitedPos.contains(complex)) continue;
				else {
					visitedPos.add(complex);
					this.getVarsAssociatedWithInvocAndReturn(varNames, complex,
							visitedPos);
				}
			}
		}

		// when the operation gets to a write operation or an arithmetic
		// operation then we need to look up in the parent data structures --neha
		else if (opToOtherOp.containsKey(iPos) ||
				opToVarNames.containsKey(iPos)) {
			varNames.addAll(getVarsUsedInWriteAndArthInsn(iPos));
		}
	} // end of getVarsAssociatedWithInv
	
	/**
	 * getVarsAssociatedWithFieldInstructions - given an ArrayList of Strings
	 * for variable names, the position of an instruction, and a set
	 * of positions already visited, this method will assume the given
	 * position corresponds to a FieldInstruction (some list of GET/PUT
	 * instructions will have already been gathered) and then dig through
	 * the mappings of instructions to vars/complex to find all the
	 * associated var names.
	 * @param varNames
	 * @param insnPosn
	 * @param visitedPosns
	 */
	public void getVarsAssociatedWithFieldInstructions(
			ArrayList<String> varNames, Integer insnPosn, Set<Integer> visitedPosns) {
		
		// if the given insn position exists in any of our maps, we
		// are going to get the corresponding variable names and add
		// them to varNames.
		
		// check the straightforward mapping of positions to varnames first
		if(this.insnToVarNames.containsKey(insnPosn)) {
			varNames.addAll(this.insnToVarNames.get(insnPosn));
		}
		
		// for more complex statements, check the insnToComplex mapping
		if(this.insnToComplex.containsKey(insnPosn)) {
			// grab the complexes and call yourself on them
			ArrayList<Integer> complexes = insnToComplex.get(insnPosn);
			
			// only visit complex positions once by adding to vistedPosns
			for(Integer complex : complexes) {
				
				if(visitedPosns.contains(complex)) {
					continue;
				}
				else {
					visitedPosns.add(complex);
					this.getVarsAssociatedWithFieldInstructions(
							varNames, complex, visitedPosns);
				}
			}
		}
		// when the operation gets to a write operation or an arithmetic
		// operation then we need to look up in the parent data structures --neha
		else if (opToOtherOp.containsKey(insnPosn) ||
				opToVarNames.containsKey(insnPosn)) {
			varNames.addAll(getVarsUsedInWriteAndArthInsn(insnPosn));
		}
	}

	/**
	 * markModifedCallSites - this method goes through the modified
	 * Calls and Returns from the CFG, looking at those that are
	 * call sites. We then grab the variable names used at each
	 * call site which is used to annotate the call edge.
	 * Lastly, all the modified are call sites are added to the
	 * globalTrackInvokes.
	 */
	public void markModifiedCallSites() {
		Set<Integer> modCallAndRet = cfg.
							getModifiedCallsAndReturns();
		Iterator<Integer> modItr = modCallAndRet.
							iterator();
		ArrayList<Integer> asn = new ArrayList<Integer>();
		while(modItr.hasNext()) {
			Integer modPos = modItr.next();
			if(this.allInvocationPositions.contains(modPos)) {
				ArrayList<String> varNames = new ArrayList<String>();
				this.getVarsAssociatedWithInvocAndReturn(varNames, modPos,
						new HashSet<Integer>());
				this.annotateCallEdge(modPos, varNames, kindOfIndex);
				asn.add(modPos);
			}
		}
		updateAffectedCallSites(asn);
	}

	/**
	 * findAffectedCallSiteInstructions - iterate over the HashMap
	 * branchToDependentInvokeInsMap to look at the positions of all
	 * the invoke statements. If the corresponding branch statement
	 * is affected then the list of invoke ins positions it is mapped
	 * to need to be added to the globalTrackInvoke by calling
	 * updateAffectedCallSites.
	 */
	public void findAffectedCallSiteInstructions() {

		// Iterate over the mapping of invoke instructions
		Iterator<Integer> invIter =
			this.branchToDependentInvokeInsMap.keySet().iterator();

		//System.out.println("branchToDependentInvokeInsMap: " + this.branchToDependentInvokeInsMap.toString());

		while(invIter.hasNext()) {

			// get the current branch position
			Integer bPos = invIter.next();

			//System.out.println("Iterating on this pos: " + bPos);

			// we only care about the invoke ins if the current
			// branch instruction is affected (in globalTrackCond)
			if(this.globalTrackCond.contains(bPos)) {

				// get the ArrayList mapped to the current branch ins
				ArrayList<Integer> invokes =
					this.branchToDependentInvokeInsMap.get(bPos);

				this.updateAffectedCallSites(invokes);

				// TODO: Consider putting this in separate method
				// now update the call edge based on arguments
				for(Integer invPos : invokes) {

					ArrayList<String> varNames = new ArrayList<String>();

					this.getVarsAssociatedWithInvocAndReturn(varNames, invPos,
							new HashSet<Integer>());

					// checking that varNames is populated correctly
					//System.out.println("Arguments at this Invoke: " + varNames.toString());

					// annotate the call edge
					this.annotateCallEdge(invPos, varNames, kindOfIndex);
				}
			}
		}

		//System.out.println("End of findAffectedCallSiteInstructions:");
		//System.out.println("BranchToInvokeMap: " + this.branchToDependentInvokeInsMap.toString());
		//System.out.println("globalTrackCond: " + this.globalTrackCond.toString());
		//System.out.println("pre-ASN: " + preASN);
		//System.out.println("post-ASN: " + this.globalTrackInvoke.toString());

	} // end of findAffectedCallSiteInstructions

	/**
	 * buildInvocationSet - looks through all instructions in order
	 * to identify those that are invoke instructions and then puts
	 * in the invocations set and does the same for return
	 * instructions
	 */
	public void buildInvocationAndReturnSet() {

		HashMap<Integer, InstructionContext> ics =
			this.absMethodInfo.getInstructionContexts();

		// iterate over the instruction contexts to look at each one
		Iterator<Integer> icIter = ics.keySet().iterator();

		while(icIter.hasNext()) {

			Integer currPos = icIter.next();

			InstructionContext ic = ics.get(currPos);
			Instruction insn = ic.getInstruction().getInstruction();

			if(insn instanceof InvokeInstruction) {
				allInvocationPositions.add(currPos);
			}
			if(insn instanceof ReturnInstruction) {
				allReturnPositions.add(currPos);
			}
		}

	} // end of buildInvocationSet

	/**
	 * annotateCallEdge - finds the call edge that originates from
	 * the given call site position and then adds the given variable
	 * names to its annotations.
	 * @param position
	 * @param varNames
	 */
	public void annotateCallEdge(Integer position,
			ArrayList<String> varNames, String kindOfIndex) {
		if(varNames.contains("this")) varNames.remove("this");
		for(AnnotatedCallEdge edge : this.callEdges) {
			if(edge.getPosition() == position) {
				edge.addAnnotations(varNames, kindOfIndex);
			}
		}
	} // end of annotateCallEdge
	
	public void annotateGlobalVarCallEdge(Integer position,
			ArrayList<String> globalVars, String kindOfIndex) {
		for(AnnotatedCallEdge edge : this.callEdges) {
			if(edge.getPosition() == position) {
				edge.addGlobalAnnotations(globalVars, kindOfIndex);
			}
		}
	}
	
	/**
	 * TODO: Finish implementing this method
	 * markGETsAffectedByAWN: none -> void
	 * this method looks through the set of affected write nodes
	 * (as found in this.globalTrackWrite) and checks for their
	 * reachability to GET instructions that use the defined variable.
	 */
	public void markGETsAffectedByAWN() {
		
		HashMap<Integer, InstructionContext> insnContexts =
			this.absMethodInfo.getInstructionContexts();
		
		// initialize an empty list of affected GET insn locations
		Set<Integer> affectedGETs = new HashSet<Integer>();
		
		for(Integer writePosn : this.globalTrackWrite) {
			// check the reachability of writePosn to 
		}
	}
	
	/**
	 * markCallSitesAffectedByAWN - marks call sites as affected if
	 * at least one of their arguments is a variable written to in
	 * an affected write node (globalTrackWrite).
	 */
	public void markCallSitesAffectedByAWN() {

		//System.out.println("Entering markCallSitesAffectedByAWN");

		HashMap<Integer, InstructionContext> ics = this.absMethodInfo.getInstructionContexts();

		// the list of affected call sites discovered
		ArrayList<Integer> asn = new ArrayList<Integer>();

		// the list of affected variable names discovered
		ArrayList<String> affectedVarNames = new ArrayList<String>();

		Iterator<Integer> awnIter = this.globalTrackWrite.iterator();

		while(awnIter.hasNext()) {

			Integer wPos = awnIter.next();

			// determine the variable name stored to at wPos
			InstructionContext ic = ics.get(wPos);
			Instruction insn = ic.getInstruction().getInstruction();
			String varName = this.getWriteVariableName(insn, ic);
			//System.out.println("varName :" + varName);

			// iterate over the invoke instructions in this method
			Iterator<Integer> invIter = this.allInvocationPositions.iterator();

			while(invIter.hasNext()) {
				Integer invPos = invIter.next();

				// Create argNames ArrayList that will get the args
				// for the current invPos
				ArrayList<String> argNames = new ArrayList<String>();
				this.getVarsAssociatedWithInvocAndReturn(argNames,
									invPos, new HashSet<Integer>());

				affectedVarNames.clear();

				
				// if argName equals varName then the call site
				// is affected if it is reachable
				if(argNames.contains(varName) &&  
						isReachable(wPos, invPos)) {
					asn.add(invPos);
					affectedVarNames.add(varName);
					this.annotateCallEdge(invPos, affectedVarNames, kindOfIndex);
				} 
				
				if(globalVariables.contains(varName) &&
						isReachable(wPos, invPos)) {
					asn.add(invPos);
					affectedVarNames.add(varName);
					annotateGlobalVarCallEdge(invPos, affectedVarNames, kindOfIndex);
				}
			}
		}

		//System.out.println("New Affected Call Sites: " + asn.toString());

		this.updateAffectedCallSites(asn);

	} // end of markCallSitesAffectedByAWN

	

	/**
	 * findInterprocedurallyAffectedCallSites - if the target of an
	 * invocation has been marked as modified, then we conservatively
	 * assume the return value has been affected and mark the call
	 * site as affected with updateAffectedCallSites
	 * @param modifiedMethodIndices
	 * @param newCG
	 */
	public void findInterprocedurallyAffectedCallSites(
			Set<Integer> modifiedMethodIndices, ReducedCallGraph newCG) {

		if(ComputeDifferences.debug)
			System.out.println("Entering findInterprocedurallyAffectedCallSites");

		ArrayList<Integer> invokes = new ArrayList<Integer>();

		String className = this.mg.getClassName();
		String uniqueMethodName = this.getUniqueMethodIndex();
		Integer currMethod = newCG.getCallNodeIndex
							(className.concat(uniqueMethodName));
		HashMap<Integer, ArrayList<Integer>> callEdgesInCurrMethod =
							newCG.getCallEdges().get(currMethod);
		if(callEdgesInCurrMethod == null) return; // no call
		Iterator<Integer> callItr = callEdgesInCurrMethod.
										keySet().iterator();
		while(callItr.hasNext()) {
			Integer callSite = callItr.next();
			ArrayList<Integer> targets = callEdgesInCurrMethod
										.get(callSite);
			for(int target : targets) {
				// look through all the Modified Methods checking for reachability
				Iterator<Integer> methodIter =
							modifiedMethodIndices.iterator();

				while(methodIter.hasNext()) {
					Integer methodPos = methodIter.next();

					// check reachability from target to modified
					if(newCG.getDistance(target, methodPos)
							== Integer.MAX_VALUE) continue;

					// if at least 1 modified method is found, then break
					invokes.add(callSite);
					break;
				}
			}
		}

	//System.out.println("Interprocedurally Affected Invokes: " + invokes.toString());

	// update the affected call sites with invokes
	this.updateAffectedCallSites(invokes);

	} // end of findInterprocedurallyAffectedCallSites

	/**
	 * updateAffectedCallSites - given the positions of a set of
	 * invoke instructions, these will each be added to the set
	 * globalTrackInvoke.
	 * @param invokes The list of invoke ins positions to be updated
	 */
	public void updateAffectedCallSites(ArrayList<Integer> invokes) {

		if(ComputeDifferences.debug) {
			System.out.println("Entering updateAffectedCallSites");
		}

		// add the affected call sites to globalTrackInvoke
		for(int i = 0; i < invokes.size(); i++) {

			Integer sPos = invokes.get(i);

			this.globalTrackInvoke.add(sPos);

			if(ComputeDifferences.debug)
				System.out.println("Invokes: " + this.globalTrackInvoke.toString());

		}

		if(ComputeDifferences.debug) {
			System.out.println("End of updateAffectedCallSites:");
			System.out.println("ASN: " + globalTrackInvoke.toString());
		}

	} // end of updateAffectedCallSites

	/**
	 * markAffectedUseOfReturnValues -
	 *
	 * A (affected) call site can be the subject of:
	 * - a conditional (if instruction)
	 * - a write (some store instruction)
	 * - another call site (invoke instruction)
	 * - nothing (pop instruction)
	 * - return instruction
	 * - complex (then becomes subject of above)
	 */
	protected void markAffectedUseOfReturnValues() {
		Set<Integer> tmpCall = new HashSet<Integer>();
		tmpCall.addAll(globalTrackInvoke);
		Iterator<Integer> callItr = tmpCall.iterator();
		while(callItr.hasNext()) {
			Integer affectedCallSite = callItr.next();
			// to check whether the return value of a function call is used
			// in the invocation of another function call or a return stmt
			Iterator<Integer> retVarItr = this.insnToRetVal.
											keySet().iterator();
			while(retVarItr.hasNext()) {
				Integer location = retVarItr.next();
				ArrayList<Integer> usesOfRetVar = insnToRetVal.get(location);
				if(!usesOfRetVar.contains(affectedCallSite)) continue;
					this.findInstructionTermination(location, affectedCallSite);
			}

			//to check whether the return value of a function call is used
			// in an arithmetic operation or a write operation
			retVarItr = this.opToRetValOfCallSites.
											keySet().iterator();
			while(retVarItr.hasNext()) {
				Integer location = retVarItr.next();
				ArrayList<Integer> usesOfRetVar = opToRetValOfCallSites.
														get(location);
				if(!usesOfRetVar.contains(affectedCallSite)) continue;
					this.findInstructionTermination(location, -1);
			}
		}
		if(ComputeDifferences.debug) {
			System.out.println(AWNPrime.toString() + " ****** AWNPrime");
			System.out.println(ACNPrime.toString() + " ******* ACNPrime");
		}
	} // end of markAffectedUseOfReturnValues
	
	/**
	 * findInstructionTermination - this is used by markAffectedUse-
	 * OfReturnValues to keep digging through a complex instruction
	 * until one of the termination instructions is found which are
	 * IfInstruction, Store Instructions, Another Invocation, or a
	 * Pop. If none of these are reached, then this method is called
	 * recursively on the next instruction.
	 */
	public void findInstructionTermination(Integer nextPos, Integer affInv) {

		InstructionContext ic =
			this.absMethodInfo.getInstructionContexts().get(nextPos);
		Instruction insn = ic.getInstruction().getInstruction();

		if(insn instanceof IfInstruction) {
			ACNPrime.add(ic.getInstruction().getPosition());
		}
		else if (insn instanceof StoreInstruction || insn instanceof PUTFIELD
				|| insn instanceof PUTSTATIC || insn instanceof IINC) {
			AWNPrime.add(ic.getInstruction().getPosition());
		}
		else if (insn instanceof ReturnInstruction) {
			globalTrackReturn.add(ic.getInstruction().getPosition());
		}
		else if (insn instanceof InvokeInstruction) {
			ArrayList<Integer> invokes = new ArrayList<Integer>();
			invokes.add(nextPos);
			this.updateAffectedCallSites(invokes);
		    ArrayList<String> allVars = new ArrayList<String>();
			getVarsAssociatedWithInvocAndReturn(allVars, affInv,
								new HashSet<Integer>());
			this.annotateCallEdge(nextPos, allVars, kindOfIndex);
			System.out.println("allVars :" + allVars.toString());
		}
		else if (insn instanceof POP)
		{
			// return value isn't handled, do nothing

		}
		else if(insn instanceof ArithmeticInstruction ||
				insn instanceof DCMPG || insn instanceof DCMPL ||
				insn instanceof FCMPG || insn instanceof FCMPL ||
				insn instanceof LCMP || insn instanceof GETFIELD ||
				insn instanceof GETSTATIC) {

			for(Integer nextInsn : this.forwardLookupMap.get(nextPos)) {
				this.findInstructionTermination(nextInsn, affInv);
			}

		}
	} // end of findInstructionTermination

	public void buildForwardLookup() {
		HashMap<Integer, InstructionContext> inContext =
							absMethodInfo.getInstructionContexts();
		Iterator<Integer> contextItr = inContext.keySet().iterator();

		HashMap<Integer, InstructionNode> controlPathAnalysis =
							absMethodInfo.getControlPathAnalysisInfo();
		HashMap<Integer, ArrayList<Integer>> allPreds =
							absMethodInfo.getPredecessors();

		while(contextItr.hasNext()) {
			Integer position = contextItr.next();
			assert (inContext.containsKey(position));
			InstructionContext ic = inContext.get(position);
			Instruction insn = ic.getInstruction().getInstruction();

			if(!(isArithmethicOrCompareInstruction(insn)
					|| isWriteInstruction(insn)
					|| insn instanceof InvokeInstruction
					|| insn instanceof ReturnInstruction
					||insn instanceof IfInstruction))
				continue;
			// if the instruction is an arthmethic instruction, store
			// or a conditional instruction
			int toPop = insn.consumeStack(mg.getConstantPool());
			ArrayList<Integer> preds = allPreds.get(position);
			if(preds == null) continue; // the node has no predecessors
			for(int predIndex = 0; predIndex < preds.size(); predIndex++) {
				int predPosition = preds.get(predIndex);
				InstructionNode predIn = controlPathAnalysis.get(predPosition);
				ArrayList<VariableInfo> stack = predIn.getNodeInfo().
												getStackFrame();
				int startIndex =stack.size() -1;
				for(int stackIndex = startIndex;
				(stackIndex >= 0 && stackIndex > (startIndex - toPop));
				stackIndex--) {
					if(stack.size() < stackIndex) return;
					VariableInfo vi = stack.get(stackIndex);
					Integer varPos = vi.getPositionOfVariable();
					if(!inContext.containsKey(varPos)) continue;
					ArrayList<Integer> usingLocs;
					if(forwardLookupMap.containsKey(varPos)) {
						usingLocs = forwardLookupMap.get(varPos);
					} else {
						usingLocs = new ArrayList<Integer>();
					}
					usingLocs.add(position);
					forwardLookupMap.put(varPos, usingLocs);

				}
			}
		}
		if(ComputeDifferences.debug) {
			System.out.println(forwardLookupMap.toString() + " forwardLookupMap");
		}
	}
	
	/**
	 * updateBackwardFlowOfAffectedFields: none -> void
	 * this method will iterate over the positions of affected GETFIELD objects
	 * and for each it will find the write positions or object GETFIELDs that
	 * dominate it, marking each as affected.
	 */
	public void updateBackwardFlowOfAffectedFields() {
		
		// create a mapping, just in case you want to be able to determine
		// which GETs are related to what other instructions.
		Map<Integer, ArrayList<Integer>> getsToDominatorsMap =
				new HashMap<Integer, ArrayList<Integer>>();
		
		for(Integer affectedGET : this.affectedObjectFields) {
			
			Set<Integer> dominatingPUTs = new HashSet<Integer>();
			
			dominatingPUTs.addAll(this.getsToDominatingPutsMap.get(affectedGET));
			
			if(this.preciseHeap) {
				// TODO:
				// find uses of the object of the field reference (affected
				// GET) that dominate the GETFIELD at the current position.
				// These locations should be marked as impacted during the
				// precise heap analysis.
			}
			
			this.globalTrackWrite.addAll(dominatingPUTs);
		}
	}
	
	/**
	 * buildGetToPutMapping: none -> void
	 * this method will go through the set of GETFIELD instructions and then
	 * through the PUTFIELD instructions mapping the GETs to the set of PUTs
	 * that are for the same field.
	 */
	public void buildGetToPutMapping() {
		
		// we are building and returning a mapping of GETFIELD instruction
		// positions to the set of PUTFIELD instruction positions that are
		// for the same variable and have reachability to that GET.
		Map<Integer, ArrayList<Integer>> getsToDominatingPuts =
							new HashMap<Integer, ArrayList<Integer>>();
		
		
		Map<Integer, InstructionContext> ics =
							this.absMethodInfo.getInstructionContexts();
		
		for(Integer getPosn : this.allGETInsnPositions) {
			Instruction getInsn =
					ics.get(getPosn).getInstruction().getInstruction();
			if(getInsn instanceof GETFIELD) {
				
				String qualifiedGetName = this.constructQualifiedFieldName(getPosn);
				// create an entry for this GET
				getsToDominatingPuts.put(getPosn, new ArrayList<Integer>());
				
				// iterate over the put instructions, finding those with matching names
				for(Integer putPosn : this.allPUTInsnPositions) {
					Instruction putInsn =
							ics.get(putPosn).getInstruction().getInstruction();
					if(putInsn instanceof PUTFIELD) {

						String qualifiedPutName = getFullyQualifiedFieldBeingWritten(putPosn);
						
						if(debug) { 
							System.out.println("GET Insn: " + getPosn + " - " + qualifiedGetName);
							System.out.println("PUT Insn: " + putPosn + " - " + qualifiedPutName);
						}
						
						//System.out.println("qualifiedPutName :" + qualifiedPutName);
						// check that the names match and then that there is reachability
						if(qualifiedGetName.equals(qualifiedPutName)) {
							if(this.isReachable(putPosn, getPosn)) {
								getsToDominatingPuts.get(getPosn).add(putPosn);
							}
						}
					}
				}
			}
		}
		
		this.getsToDominatingPutsMap = getsToDominatingPuts;
	}
	
	
	/**
	 * markInsAffectedByFieldReferences: none -> void
	 * 
	 */
	public void markInsAffectedByFieldReferences() {
		
		for(Integer affectedGET : this.affectedObjectFields) {
			
			// mark the writes affected by the affectedObjectFields set
			if(debug) {
				System.out.println("-- checking affected GET: " + affectedGET);
			}
			this.findInstructionTermination(affectedGET, -1);
		}
	}
	
	/**
	 * markAffectedFieldReferences: none -> void
	 * this method will look through the fieldFirstUsesMap as compared to the
	 * sets of impacted nodes to see if the first uses of any fields have been
	 * marked as impacted. If they have been, then we know the Object fields
	 * will (through lazy initialization during symbolic execution) get
	 * concretized. The fields of these objects, however will take on symbolic
	 * values which we have determined to be impacted at this point. Because
	 * of this impact, the reachable positions that reference those fields
	 * (in the form of GET field instructions) will be added a set of
	 * affectedFieldReferences which will be used to further propagate the
	 * impact throughout the program.
	 */
	public void markAffectedFieldReferences() {
		
		if(debug) { 
			System.out.println("marking affected field references - " + this.kindOfIndex);
			System.out.println(this.fieldFirstUsesMap.toString());
			System.out.println(this.posnToGETInsnMap.toString());
			System.out.println(this.branchToDependentGETInstructionsMap.toString());
			System.out.println(this.ACNPrime.toString());
			System.out.println(this.allAffectedNodes.toString());
			System.out.println(this.qualifiedObjectNameToFieldPosnMap.toString());
			System.out.println(this.globalTrackCond.toString());
		}
		
		/**
		 * Why only first uses of fields?
		 * 
		 * This is because it is only after the first use of a field that
		 * it will become concretized. Only after being concretized can we
		 * consider the object's fields to be symbolic. So if the first
		 * occurrence of an object is affected, then we will want to add
		 * its fields to a set of affectedFieldReferences.
		 */
		
		// go through this map to check each field (and its positions)
		// individually
		for(String fieldName : this.fieldFirstUsesMap.keySet()) {
			
			// then go through, the positions for that field to see if any
			// of them are impacted
			for(Integer fieldPosition : this.fieldFirstUsesMap.get(fieldName)) {
				
				// iterate over the set of impacted conditional so that we know
				// which records from branchToDependentGETInstructionsMap to
				// grab.
				for(Integer affectedCond : this.globalTrackCond) {
					
					if(!this.branchToDependentGETInstructionsMap.containsKey(affectedCond)) {
						continue;
					}
					
					if(this.branchToDependentGETInstructionsMap.get(affectedCond).contains(fieldPosition)) {
						
						// at this point, there is a GET instruction that is
						// the first use of a particular field object and we
						// have determined that it appears at an affected
						// conditional statement.
						// Now, it is time to compute the reachability of this
						// GET instruction to the uses of its fields to
						// determine which of those GET instructions should be
						// added to the set of affected field references.
						Integer affectedFieldPosition = fieldPosition;
						
						// figure out whether the position is for a primitive or an object
//						InstructionNode in =
//								this.absMethodInfo.getControlPathAnalysisInfo().get(affectedFieldPosition);
//						ArrayList<VariableInfo> stackFrame = in.getNodeInfo().getStackFrame();
//						// TODO: may need to clean up the way the stackFrame
//						// is accessed, but it may not matter since the
//						// positions we are looking at are already vetted.
//						String fieldType = 
//								stackFrame.get(0).getVariableType().toString();
//						boolean primitive = false;
//						if(fieldType.equals("int") ||
//								fieldType.equals("boolean") ||
//								fieldType.equals("byte") ||
//								fieldType.equals("short") ||
//								fieldType.equals("long") ||
//								fieldType.equals("float") ||
//								fieldType.equals("double") ||
//								fieldType.equals("char")) {
//							primitive = true;
//						}
						
						//System.out.println("*** fieldName: " + fieldName);
						//System.out.println("*** fieldMap: " + qualifiedObjectNameToFieldPosnMap.toString());
						
						if(!this.qualifiedObjectNameToFieldPosnMap.containsKey(fieldName)) {
							continue;
						}
						
						// at this point, I should either get the list of
						// positions associated with the fieldName or I will
						// need a similar map of positions to position lists
						for(Integer referencingPosition : this.qualifiedObjectNameToFieldPosnMap.get(fieldName)) {
							
							if(this.isReachable(affectedFieldPosition, referencingPosition)) {
								
								if(debug) {
									System.out.println("marking " + referencingPosition);
								}
								
								// if it is primitive, then we don't want
								// to add it to the set of affected field objects
								if(!this.isPrimitive(referencingPosition)) {
								
									// it is reachable, so add it to the set of
									// affected field references
									this.affectedObjectFields.add(referencingPosition);
									
								}
								
								// to get this incorporated into the updateAffectedWriteStatements()
								//this.globalTrackWrite.add(referencingPosition);
								
								// starting to use the AffectedGlobals object now
								this.affectedGlobals.getAffectedFieldReferences().
									add(referencingPosition);
							}
						}
					}
				}
			}
		}
	}
	
	/**
	 * isPrimitive: int -> boolean
	 * given an int that represents a bytecode position of a variable,
	 * this method will look at the StackFrame/VariableInfo and determine
	 * if the variable refers to a primitive value (int, boolean, float,
	 * etc.) or to a complex value (object). If it is primitive, return
	 * true, otherwise return false. 
	 */
	public boolean isPrimitive(int position) {
		
		// figure out whether the position is for a primitive or an object
		InstructionNode in =
				this.absMethodInfo.getControlPathAnalysisInfo().get(position);
		ArrayList<VariableInfo> stackFrame = in.getNodeInfo().getStackFrame();
		// TODO: may need to clean up the way the stackFrame
		// is accessed, but it may not matter since the
		// positions we are looking at are already vetted.
		String fieldType = 
				stackFrame.get(0).getVariableType().toString();
		boolean primitive = false;
		if(fieldType.equals("int") ||
				fieldType.equals("boolean") ||
				fieldType.equals("byte") ||
				fieldType.equals("short") ||
				fieldType.equals("long") ||
				fieldType.equals("float") ||
				fieldType.equals("double") ||
				fieldType.equals("char")) {
			primitive = true;
		}
		
		return primitive;
	}
	
	/**
	 * markAffectedCallSitesForGlobalVariableFlow: String -> void
	 * @param varName
	 */
	public void markAffectedCallSitesForGlobalVariableFlow(String varName) {
		if(!globalVariables.contains(varName)) return;
		
		ArrayList<String> globalVars = new ArrayList<String>();
		globalVars.add(varName);

		Iterator<Integer> inkPos = allInvocationPositions.iterator();

		while(inkPos.hasNext()) {
			Integer callLoc = inkPos.next();
			this.globalTrackInvoke.add(callLoc);
			this.annotateGlobalVarCallEdge(callLoc, globalVars, varName);
		}
	}

	public void markAffectedCallSitesAndReturnStmtsBasedOnMethodParams
								(String varName) {
		//get all the invocations
		Iterator<Integer> inkPos = allInvocationPositions.iterator();
		while(inkPos.hasNext()) {
			Integer callLoc = inkPos.next();
			updateAffCallOrRet(globalTrackInvoke,
					callLoc, varName, true);
		}
		// get all return statements
		Iterator<Integer> retPos = allReturnPositions.iterator();
		while(retPos.hasNext()) {
			Integer retLoc = retPos.next();
			updateAffCallOrRet(globalTrackReturn,
					retLoc, varName, false);
		}
	}
	
	private void updateAffCallOrRet(Set<Integer> affSet, Integer position,
			String varName, boolean isCall) {
		ArrayList<String> allVars = new ArrayList<String>();
		getVarsAssociatedWithInvocAndReturn(allVars, position,
							new HashSet<Integer>());
		if(allVars.contains(varName)) {
			affSet.add(position);
			if(!isCall) return;
			ArrayList<String> varNames = new ArrayList<String>();
			varNames.add(varName);
			this.annotateCallEdge(position, varNames, varName);
		}
	}

	/**
	 * markModifiedReturnStatements - this method goes through the
	 * set of allReturnPositions and checks to see if each is also
	 * contained in the modifiedCallsAndReturns from this method's
	 * CFG. Those that are will then be added to the globalTrackReturn
	 * set for later use by the analysis.
	 */
	public void markModifiedReturnStatements() {
		Iterator<Integer> retItr = this.allReturnPositions.iterator();

		while(retItr.hasNext()) {
			Integer retPos = retItr.next();
			if(cfg.getModifiedCallsAndReturns().contains(retPos)) {
				// this only adds it to the set if it doesn't already
				// exist in the set.
				this.globalTrackReturn.add(retPos);
			}
		}
	}

	/**
	 * updateAffectedWriteStatements: none -> void
	 * 
	 */
	public void updateAffectedWriteStatements() {
		HashMap<String, ArrayList<Integer>> writeVarsMod = new
							HashMap<String, ArrayList<Integer>>();
		this.genWriteInsUsingModifiedWriteVals(globalTrackWrite,
													writeVarsMod);
		Iterator<String> writeVarItr = writeVarsMod.keySet().iterator();
		while(writeVarItr.hasNext()) {
			String writeVar = writeVarItr.next();
			ArrayList<Integer> writePoses =
									writeVarsMod.get(writeVar);
			AWNPrime.addAll(writePoses);
		}
	}

	public void markAffectedReturnStatements() {
		HashMap<String, Integer> writeVarAndPos =
								new HashMap<String, Integer>();
		Iterator<Integer> writeItr = globalTrackWrite.iterator();
		while(writeItr.hasNext()) {
			Integer wPos = writeItr.next();
			String varName = this.getWriteVariable(wPos);
			writeVarAndPos.put(varName, wPos);
		}

		Iterator<Integer> retItr = this.allReturnPositions.iterator();

		while(retItr.hasNext()) {
			Integer retPos = retItr.next();

			//check whether a variable returned has
			//been written in a write instruction such
			// that the return is reachable from the write
			//instruction

			ArrayList<String> varTrack = new ArrayList<String>();

			ArrayList<String> allVars = new ArrayList<String>();
			getVarsAssociatedWithInvocAndReturn(allVars, retPos,
												new HashSet<Integer>());
			boolean found = false;
			for(int varIndex = 0; varIndex < allVars.size(); varIndex++) {
				String currVarName = allVars.get(varIndex);
				if(!writeVarAndPos.containsKey(currVarName)) continue;
				Integer wPos = writeVarAndPos.get(currVarName);
				varTrack.add(currVarName);
				if(isReachable(wPos, retPos)) {
					if(!globalTrackReturn.contains(retPos))
						globalTrackReturn.add(retPos);
					found = true;
				}
			}
			if(!found) continue;
			for(int varIndex = 0; varIndex < allVars.size(); varIndex++) {
				String currVarName = allVars.get(varIndex);
				checkingOtherVars(writeVarAndPos, currVarName, retPos, varTrack);
			}
		}
	}

	private void checkingOtherVars(HashMap<String, Integer> writeVarAndPos,
				String currVarName, Integer retPos, ArrayList<String> varTrack) {
		ArrayList<Integer> reachablePoses = new ArrayList<Integer>();
		if(varNamesToWriteIns.containsKey(currVarName) ) {
			ArrayList<Integer> wPoses =
						varNamesToWriteIns.get(currVarName);
			for(int wIndex = 0; wIndex < wPoses.size(); wIndex++) {
				Integer wPos = wPoses.get(wIndex);
				if(isReachable(wPos, retPos)) {
					reachablePoses.add(wPos);
					AWNPrime.add(wPos);
				}
			}
		}
		if(!reachablePoses.isEmpty()) {
			varTrack.add(currVarName);
		}
		for(int reachIndex = 0; reachIndex < reachablePoses.size(); reachIndex++) {
			ArrayList<String> allOtherVarNames = 
						getVarsUsedInWriteAndArthInsn(reachablePoses.get(reachIndex));
			for(int allIndex = 0 ; allIndex < allOtherVarNames.size(); allIndex++) {
				String allOtherName = allOtherVarNames.get(allIndex);
				if(varTrack.contains(allOtherName)) continue;
				checkingOtherVars(writeVarAndPos, allOtherName, retPos, varTrack);
			}
		}
	}

	/**
	 * getUniqueMethodIndex - this method utilizes the information
	 * provided by this.mg to build the uniqueMethodName which can be
	 * used for accessing data indexed by the uniqueMethodName.
	 * @return String
	 */
	public String getUniqueMethodIndex() {

		String methodName = this.mg.getName();
		String methodArgs = new String("");
		Type[] t = this.mg.getArgumentTypes();
		for(Type type : t) {
			String strType = type.toString();
			methodArgs = methodArgs.concat(strType);
		}
		return (methodName + methodArgs);
	} // end of getUniqueMethodIndex



	public ArrayList<String> getAllAffectedVariables(
					Set<Integer> affCond, Set<Integer> affWrite,
					Set<Integer> affRet) {
		ArrayList<String> allVars = new ArrayList<String>();
		//get all variables used in affected conditional branches

		ArrayList<String> varsUsedCond = getRelevantVarNamesUsedInCondInsn
												(affCond);
		allVars.addAll(varsUsedCond);

		// get all variables used in affected write instructions

		Iterator<Integer> wrtItr = affWrite.iterator();
		while(wrtItr.hasNext()) {
			Integer wrtPos = wrtItr.next();
			ArrayList<String> varsUsedWrite = getVarsUsedInWriteAndArthInsn(wrtPos);
			allVars.addAll(varsUsedWrite);
		}

		// get all variables used in affected invoke instructions
		// we only get about those variables in the invoke instructions
		// with which the call graph edges are annotated with
		for(int callEdgeIndex = 0; callEdgeIndex < callEdges.size(); callEdgeIndex++) {
			AnnotatedCallEdge ace = callEdges.get(callEdgeIndex);
			ArrayList<String> varsUsedCalls = ace.getAnnotations("method_body");
			allVars.addAll(varsUsedCalls);
		}


		// get all variables used in affected return instructions

		Iterator<Integer> retItr = affRet.iterator();
		while(retItr.hasNext()) {
			Integer retPos = retItr.next();
			ArrayList<String> varsReturned = new ArrayList<String>();
			this.getVarsAssociatedWithInvocAndReturn(varsReturned, retPos,
										new HashSet<Integer>());
		}
		return allVars;
	}
}
