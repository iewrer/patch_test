package gov.nasa.jpf.regression.analysis;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.bcel.classfile.Field;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.LocalVariable;
import org.apache.bcel.classfile.LocalVariableTable;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.Type;
import org.apache.bcel.generic.ObjectType;

import edu.byu.cs.analysis.AnalyzeClass;
import edu.byu.cs.search.heuristic.pefca.ReducedCallGraph;
import edu.byu.cs.trace.AbstractMethodInfo;
import gov.nasa.jpf.regression.ast.MethodASTInfo;
import gov.nasa.jpf.regression.cfg.CFG;
import gov.nasa.jpf.regression.cfg.CFGBuilder;

public class ComputeInterProceduralDiff extends ComputeIntraProceduralDiff{
	
	private boolean verbose = false;

	protected AnalyzeInterProceduralDiff semanticDiff;
	protected AbstractMethodInfo absMethodInfo;
	protected ReducedCallGraph newCallGraph;
	protected Set<Integer> modifiedMethodIndices;
	protected MethodASTInfo astInfo;
	protected ArrayList<Integer> indices;
	protected String otherMethodIndex;
	protected ArrayList<String> allAffectedVars =
					new ArrayList<String>();
	protected HashMap<Integer, String> argIndexToVarName =
				new HashMap<Integer, String>();
	protected JavaClass jc = null;
	protected AnalyzeClass ac;
	
	protected Map<String,Map<Integer,Integer>> oldPosLineMap =
		new HashMap<String,Map<Integer,Integer>>();
	protected Map<String,Map<Integer,Integer>> newPosLineMap =
		new HashMap<String,Map<Integer,Integer>>();


	public ComputeInterProceduralDiff(CFGBuilder newBuilder,
			CFGBuilder oldBuilder, ReducedCallGraph newCG,
			AnalyzeClass ac,
			Set<Integer> modifiedMethodIndices,
			MethodASTInfo astInfo, String otherMethodIndex) {
		super(newBuilder, oldBuilder);

		this.otherMethodIndex = otherMethodIndex;

		// gives access to call graph so that edges can be annotated
		this.newCallGraph = newCG;
		
		this.ac = ac;

		this.modifiedMethodIndices = modifiedMethodIndices;
		this.astInfo = astInfo;
		this.indices = new ArrayList<Integer>();
		if (oldBuilder != null)
			this.oldPosLineMap = oldBuilder.getPosLineMap();
		if (newBuilder != null)
			this.newPosLineMap = newBuilder.getPosLineMap();
	}
	
	public ComputeInterProceduralDiff(CFGBuilder newBuilder,
			CFGBuilder oldBuilder, ReducedCallGraph newCG,
			Set<Integer> modifiedMethodIndices,
			MethodASTInfo astInfo, String otherMethodIndex,
			JavaClass jc) {
		super(newBuilder, oldBuilder);

		this.otherMethodIndex = otherMethodIndex;

		// gives access to call graph so that edges can be annotated
		this.newCallGraph = newCG;

		this.modifiedMethodIndices = modifiedMethodIndices;
		this.astInfo = astInfo;
		this.indices = new ArrayList<Integer>();
		
		if (oldBuilder != null)
			this.oldPosLineMap = oldBuilder.getPosLineMap();
		if (newBuilder != null)
			this.newPosLineMap = newBuilder.getPosLineMap();
		
		// Set the given JavaClass
		this.jc = jc;
	}

	public String getOtherMethodIndex ( ) {
		return otherMethodIndex;
	}
	
	public Map<String,Map<Integer,Integer>> getOldPosLineMap(){
		return oldPosLineMap;
	}
	
	public Map<String,Map<Integer,Integer>> getNewPosLineMap(){
		return newPosLineMap;
	}


	public HashMap<Integer, String> getArgIndexToVarName() {
		return argIndexToVarName;
	}

	public AffectedNodes getAffectedNode(String index) {
		if(semanticDiff.allAffectedNodes.containsKey(index))
			return semanticDiff.allAffectedNodes.get(index);
		return null;
	}

	public AnalyzeInterProceduralDiff getSemanticDiff() {
		return semanticDiff;
	}



	public void updateAffectedAffectedIndices() {
		MethodGen mg = absMethodInfo.getMethodGen();
		int argNum = 0;
		int startIndex = 0;
		if(mg.getArgumentNames() != null) {
			argNum = mg.getArgumentNames().length;
		}

		LocalVariableTable lvt = mg.getLocalVariableTable(mg.getConstantPool());
		LocalVariable[] lv = lvt.getLocalVariableTable();

		for(int lIndex = startIndex; lIndex < lv.length; lIndex++) {
			LocalVariable var = lv[lIndex];
			if(var.getIndex() < argNum) {
				 if(this.allAffectedVars.contains(var.getName()) &&
						 !indices.contains(lIndex)) {
				    	indices.add(lIndex);
				  }
			}
		}
	}

	public ArrayList<Integer> getAffectedFormalParameterIndices() {
		return indices;
	}

	protected void genAffectedLocationsDueToOtherModMethods
						(HashMap<Integer, ArrayList<Integer>> callsAndVars) {
		Iterator<Integer> callSites = callsAndVars.keySet().iterator();
		while(callSites.hasNext()) {
			Integer callSite = callSites.next();
			ArrayList<Integer> callVarLocs = callsAndVars.get(callSite);
			if(callVarLocs != null && callVarLocs.size() > 0)
				semanticDiff.analyzeVarsFromModifiedMethods
											(callSite, callVarLocs);
		}
	}

	/**
	 * preciseAnalysis - this method performs a relatively precise
	 * control and data flow analysis on the given method.
	 * @param methodName
	 * 				the name of the method under analysis
	 * @param cfg
	 * 				the CFG for the method which contains all the
	 * 				control and data flow info
	 * @param mg
	 * 				the MethodGen for other info about the method
	 * @param callGraphIndex
	 * 				index of this method in the program call graph
	 */
	 protected void preciseAnalysis(String methodName, CFG cfg, MethodGen mg,
			 			Integer callGraphIndex, boolean precise) {
		 this.absMethodInfo = new AbstractMethodInfo(mg);
		 absMethodInfo.runAnalysis(false);
		 absMethodInfo.anlayzeInvokeStatements();

		 semanticDiff = new AnalyzeInterProceduralDiff(absMethodInfo, cfg, mg);
		 semanticDiff.setPreciseHeap(precise);
		 
		 // this is a bunch of heap stuff
		 if(semanticDiff.useHeapReductions) {
			 
			 // Map Field Variables to their positions of first use
			 semanticDiff.generatePosnToFieldInsnMaps();
			 semanticDiff.buildFieldFirstUsesMap();
			 
			 semanticDiff.buildGetToPutMapping();
			 
			 //buildGlobalVariableSet();
			 buildGlobalVariableSets();
		 }

		 semanticDiff.initializeAnnotatedCallEdges(newCallGraph.getCallEdges().
				 					get(callGraphIndex), callGraphIndex);

		 setupComputeInterProcSemanticDiff();
		 genFixPoint("method_body", cfg.getModifiedWritesAndIfs());
		 
		 buildFormalParametersAffectedSet();
		 
		 if(semanticDiff.useHeapReductions) { 
			 buildGlobalVariableAffectedSet();
		 }
		 
		 if (verbose)
			 System.out.println("Affected Field References: " + 
				 this.semanticDiff.affectedGlobals.getAffectedFieldReferences());
		 
	 }

	 /**
	  * buildGlobalVariableSet - this method simply goes through the
	  * set of Field objects for class under analysis and adds the
	  * name of each to the globalVariables set for the current 
	  * method's analysis.
	  * The Field name is of the form: <class-name>.<field-name>
	  */
	 protected void buildGlobalVariableSet() {
		 for(Field f : ac.getFields()) {
			 String fieldName = ac.getClassName()+"."+f.getName();
			 semanticDiff.globalVariables.add(fieldName);
		 }
	 }
	 
	 /**
	  * buildGlobalVariableSets - this method is very similar to the
	  * similarly named buildGlobalVariableSet. Rather than just
	  * building the globalVariables set, this one also builds the
	  * globalObjectVariables set used for heap analysis.
	  * This can replace buildGlobalVariableSet()
	  */
	 protected void buildGlobalVariableSets() {
		 
		 // this class's name to be used in the for loop
		 String className = this.ac.getClassName();
		 
		 /*
		  * go through all of the fields for this class and add them
		  * all to the globalVariables set. Those that are objects can
		  * also be added to the globalObjectVariables set.
		  */
		 for(Field f : ac.getFields()) {
			 String fieldName = className + "." + f.getName();
			 semanticDiff.globalVariables.add(fieldName);
			 
			 // now check if it is an object
			 if(Type.OBJECT.equals(f.getType())) {
				 semanticDiff.globalObjectVariables.add(fieldName);
			 }
		 }
	 }
	 
	 /**
	  * buildGlobalVariableAffectedSet - this method is supposed to
	  * be somewhat similar to the buildFormalParamatersAffectedSet.
	  */
	 public void buildGlobalVariableAffectedSet() {
		 /*
		  * Need to have a for loop that iterates over all the fields
		  * in the class.
		  * Then initialize a modifiedSet like buildFormalParams
		  * Consider pulling out common pieces for buildFormalParams
		  * and here to avoid too much duplicate code.
		  */
		 for(Field f : ac.getFields()) {
			 String fieldName = ac.getClassName()+"."+f.getName();
			 
			 /*
			  * This is going to update the affected sets and then
			  * propagate the affected sets to a fixed point.
			  */
			 this.buildAffectedSetsForField(fieldName);
			 
			 /*
			  * if the current Field object was marked as affected,
			  * then it should be added to the set of affectedGlobals.
			  */
			
			 if(this.allAffectedVars.contains(fieldName)) {
		    	semanticDiff.affectedGlobals.addAffectedGlobal(f);
			 }
		 }
	 }
	 
	 /**
	  * buildGlobalObjectAffectedSet - this method goes through
	  * the fields for the class of this method and builds a set of
	  * them that are affected/impacted.
	  */
	 public void buildGlobalObjectAffectedSet() {
		 
		 for(Field f : ac.getFields()) {
			 
			 Type fieldType = f.getType();
			 if(!(fieldType instanceof ObjectType)) return;
			 
			 // those fields that are of type CLASS are complex
			 // objects that we are interested in.
			 
		 }
	 }
	 
	 /**
	  * buildAffectedSetsForSubfields - this method acts similarly to
	  * the buildAffectedSetsForName method by 
	  */

	 public void buildFormalParametersAffectedSet() {

	 	MethodGen mg = absMethodInfo.getMethodGen();

	 	// no need to map the arguments to affected set if
	 	// since it is always String[] args for main
		if(mg.getName().equals("main")) return;

		int argNum = 0;
		int startIndex = 0;
		if(mg.getArgumentNames() != null) {
			argNum = mg.getArgumentNames().length;
		} else {
			return;
		}

		if(!mg.isStatic()) {
			argNum = argNum+1; //account for "this"
			startIndex = 1; // skip analyzing the "this"
		}
		//TODO handle the case if the method is an interface


		LocalVariableTable lvt = mg.getLocalVariableTable(mg.getConstantPool());
		LocalVariable[] lv = lvt.getLocalVariableTable();

		/*
		 * Essentially iterating over the formal params in this method.
		 */
		for(int lIndex = startIndex; lIndex < lv.length; lIndex++) {
			LocalVariable var = lv[lIndex];
			if(var.getIndex() < argNum) {

				this.buildAffectedSetsForName(var.getName());
				
			    if(this.allAffectedVars.contains(var.getName())) {
			    	indices.add(lIndex);
			    }
			    argIndexToVarName.put(lIndex, var.getName());
			}
		}

	}
	 
	 /**
	  * buildAffectedSetsForName - this method updates the initial
	  * affected sets and then propagates those effects throughout
	  * the program until a fixed point is reached. 
	  * The purpose of this method was to pull out some common
	  * functionality between the buildFormalParametersAffectedSet
	  * and buildGlobalVariableAffectedSet.
	  * @param name
	  * 		The name of the variable being propagated.
	  */
	 protected void buildAffectedSetsForName(String name) {
		 
		 Set<Integer> modifiedSet = new HashSet<Integer>();
		 

		// If there are Cond positions for this var, then add them to modified set.
	    if(semanticDiff.varNameToCondBranchPosMap.containsKey(name)) {
	    	modifiedSet.addAll(semanticDiff.varNameToCondBranchPosMap.get(
	    									name));
	    }
	    
	    // If there are Write positions for this var, then add them to the modified set.
	    if(semanticDiff.varNamesToWriteIns.containsKey(name)) {
	    	modifiedSet.addAll(semanticDiff.varNamesToWriteIns.get(name));
	    }
	    
	    
	    semanticDiff.markAffectedCallSitesAndReturnStmtsBasedOnMethodParams
	    										(name);
	    genFixPoint(name, modifiedSet);
	 }
	 
	 /**
	  * buildAffectedSetsForField: String -> void
	  * this method is given the qualified name of a field of this class, if
	  * it is a primitive field, then it will simply build the affected sets
	  * for that field, however if it is an object field, then in addition to
	  * building the affected sets for the field, it will also have to do so
	  * for any fields of that object.
	  * 
	  * @param fieldName
	  * 			the qualified name of the field of interest.
	  */
	 protected void buildAffectedSetsForField(String fieldName) {
		 
		 Set<Integer> modifiedSet = new HashSet<Integer>();
		 
		 // if there are conditional positions that use the given field,
		 // then add them to the modified set
		 if(semanticDiff.varNameToCondBranchPosMap.containsKey(fieldName)) {
			 modifiedSet.addAll(semanticDiff.varNameToCondBranchPosMap.get(fieldName));
		 }
		 
		 // if there are write positions that use the given field, then add
		 // them to the modified set
		 if(semanticDiff.varNamesToWriteIns.containsKey(fieldName)) {
			 modifiedSet.addAll(semanticDiff.varNamesToWriteIns.get(fieldName));
		 }
		 
		 // figure out how to access the names of the fields of a field object
		 // -- this can be done by using the qualifiedObjectNameToFieldPosnMap
		 // XXX: Does reachability need to be checked here? Not going to check
		 // it to start.
		 if(semanticDiff.qualifiedObjectNameToFieldPosnMap.containsKey(fieldName)) {
			 modifiedSet.addAll(
					 semanticDiff.qualifiedObjectNameToFieldPosnMap.get(fieldName));
			 // try instead to add them to the semanticDiff.affectedGlobals or
			 // likewise
			 // TODO: Need to make one or the other of these final and remove the
			 // other. This will involve removing fields from AnalyzeInter...
			 semanticDiff.affectedObjectFields.addAll(
					 semanticDiff.qualifiedObjectNameToFieldPosnMap.get(fieldName));
			 semanticDiff.affectedGlobals.getAffectedFieldReferences().addAll(
					 semanticDiff.qualifiedObjectNameToFieldPosnMap.get(fieldName));
		 }
		 
		 this.genFixPoint(fieldName, modifiedSet);
	 }

	 /**
	  * genFixPoint - ...
	  * @param index
	  * @param modifiedSet
	  */
	 protected void genFixPoint (String index,
			 								Set<Integer> modifiedSet) {
		 boolean genTransitiveControlDLocs = true;
		 boolean genFinalWriteStmts = true;
		 
		 computeSemanticDiff(semanticDiff.cfg, semanticDiff,
				 			modifiedSet, genTransitiveControlDLocs,
				 			genFinalWriteStmts);
		 this.computeInterProceduralDiSE(semanticDiff.cfg, index);

		 if(ComputeDifferences.debug)  {
			 System.out.println("genFixPoint :" + semanticDiff.globalTrackCond.toString() +
					 " --> " + semanticDiff.ACNPrime.toString()
					 + semanticDiff.globalTrackWrite.toString() +
					 "--> " + semanticDiff.AWNPrime.toString());
		 }

		 while(!reachedFixedPoint(semanticDiff.globalTrackCond, semanticDiff.ACNPrime,
				 semanticDiff.globalTrackWrite, semanticDiff.AWNPrime)) {
			 modifiedSet.clear();
			 modifiedSet.addAll(semanticDiff.ACNPrime);
			 modifiedSet.addAll(semanticDiff.AWNPrime);
			 modifiedSet.removeAll(semanticDiff.globalTrackCond);
			 modifiedSet.removeAll(semanticDiff.globalTrackWrite);

			 if(ComputeDifferences.debug) {
				 System.out.println("coming here **********");
			 }

			 computeSemanticDiff(semanticDiff.cfg, semanticDiff, modifiedSet,
					 		genTransitiveControlDLocs, genFinalWriteStmts);
			 this.computeInterProceduralDiSE(semanticDiff.cfg, index);

		 }

		 HashSet<Integer> newCondPositions = new HashSet<Integer>();
		 /*** --- rule for control dependence of affected write statements **/
		 newCondPositions.addAll(semanticDiff.markOtherConditionalBranches(null, false));
		 semanticDiff.updateToBeDroppedSet(newCondPositions, semanticDiff.canbeDropped,
				 			semanticDiff.globalTrackCond);
		 semanticDiff.globalTrackCond.addAll(newCondPositions);
		 /** // --- end of control dependence **/
		 
		 /** --- rule to mark write statements that dominate affected variables 
		  * 		that is, the backwards flow rule                            --- **/
		 if(semanticDiff.useHeapReductions) {
			 semanticDiff.updateBackwardFlowOfAffectedFields();
		 }
		 /** --- end of rule --- **/

		 if(ComputeDifferences.debug) {
			 System.out.println(semanticDiff.globalTrackCond.toString()  +
					 " Final Conditional statements");
			 System.out.println(semanticDiff.globalTrackWrite.toString() +
					 "  final write statements");
			 System.out.println(semanticDiff.globalTrackInvoke.toString() +
					 " final invoke statements");
		 }

		 //method wont' do anything if the index is not a global variable
		 //this allows us to track forward flow of global variables through
		 //different methods calls
		 semanticDiff.markAffectedCallSitesForGlobalVariableFlow(index);
		 
		 semanticDiff.addToAffectedNodeSet(index);
		 if(index.equals("method_body")) {
			 generateForwardMap(semanticDiff.globalTrackCond,
					 			semanticDiff.globalTrackWrite,
					 			semanticDiff.globalTrackReturn);
		 }

		 semanticDiff.clearAllAffectedDataStructures();
	 }

	 protected void generateForwardMap(Set<Integer> affCond, Set<Integer> affWrite,
			 Set<Integer> affReturn) {
			 allAffectedVars = semanticDiff.
					 getAllAffectedVariables(affCond, affWrite, affReturn);
	 }

	 protected boolean reachedFixedPoint(Set<Integer> ACN, Set<Integer> ACNPrime,
			 Set<Integer> AWN, Set<Integer> AWNPrime) {
		 
		 if(ACN.containsAll(ACNPrime) && AWN.containsAll(AWNPrime)) {
			 return true;
		 }
		 
		 return false;
	 }

	 /**
	  * setupComputeInterProcSemanticDiff - this method does the setup
	  * of the data structures for the interprocedural part of the
	  * analysis. This is not only the sets and maps of instruction
	  * info, but also the annotation of call edges.
	  * -- not entirely sure that all setup is completely contained
	  *    to this method. Most seems to happen here though.
	  */
	 protected void setupComputeInterProcSemanticDiff() {

		 // this does setup of the data structures for
		 // arithmetic/write insns as well as for branch insns.
		 initializeDataStructures(semanticDiff);
		 
		 // updates the insnToVarNames, insnToComplex, and insnToRetVal
		 // maps with the Invoke and Return instructions.
		 semanticDiff.analyzeInvokeAndReturnInstructions();
		 
		 // annotate modified call edges with affected variables
		 // and put all the modified callsites in globalTrackInvoke
		 semanticDiff.markModifiedCallSites();

		 // generating the branchToDependentInvokeInsMap has to come
		 // after the vanilla DiSE because data structures it is
		 // dependent on get built in it.
		 // build the branch to invoke map
		 semanticDiff.genInvokeInsCDOnBranches();
		 
		 /*
		  * generating the branchToDependentGETInstructionsMap will
		  * happen right after the genInvokeInsCDOnBranches call because
		  * they seem like very similar methods.
		  * Build the branch to GET FieldInstructions map
		  */
		 if(semanticDiff.useHeapReductions) { 
			 semanticDiff.genGETInstructionsCDOnBranches();
		 }

		 // As part of the setup, compute the distance matrix
		 // Now, anyone with the CallGraph should be able to determine
		 // the reachability of the callgraph.
		 this.newCallGraph.computeDistanceEstimates();

		 // if a call site is invoking affected methods (even transitively),
		 // then we conservatively mark it as affected as well.
		 semanticDiff.findInterprocedurallyAffectedCallSites(
				 this.modifiedMethodIndices, this.newCallGraph);

		 // goes through all the positions of return statements and those
		 // that are affected are added to the globalTrackReturn set.
		 semanticDiff.markModifiedReturnStatements();
	 }

	 /**
	  * initializeAffectedGlobals: none -> void
	  * use the ac to initialize the AffectedGlobals object for the
	  * semanticDiff with the names of the field objects.
	  */
	 public void initializeAffectedGlobals() {
		 
		 String className = ac.getClassName();
		 
		 for(Field field : ac.getFields()) {
			 String fieldName = className + "." + field.getName();
			 semanticDiff.affectedGlobals.allFieldNames.add(fieldName);
		 }
	 }

	 /**
	  * computeInterProceduralDiSE - this is the method that should get
	  * called after vanilla DiSE is executed in genFixPoint of the
	  * pseudo code. This is the part of the interprocedural analysis
	  * that builds upon the info gathered by vanilla DiSE.
	  */
	 public void computeInterProceduralDiSE(CFG cfg, String index) {

		 semanticDiff.setIndex(index);

		 // this marks call sites control dependent on affected
		 // conditional statements as affected and all the parameters
		 // of the call are annotated on
		 //if(!InterproceduralAnalysis.ChCK)
		 //	 semanticDiff.findAffectedCallSiteInstructions();

		 //need to update the affected write statements because
		 // there might be write instructions that are not used
		 // in a conditional statement but may be used in the ret
		 // statements.

		 semanticDiff.updateAffectedWriteStatements();
		 semanticDiff.markCallSitesAffectedByAWN();

		 semanticDiff.markAffectedUseOfReturnValues();
		 //mark the return statements
		 semanticDiff.markAffectedReturnStatements();

		 // This is the code that does impact propagation for the Heap part
		 // of the analysis.
		 if(semanticDiff.useHeapReductions) {
			 // this will get the allFieldNames initialized and potentially
			 // other data structures in that class
			 this.initializeAffectedGlobals();
			 
			 semanticDiff.markAffectedFieldReferences();
			 semanticDiff.markInsAffectedByFieldReferences();
			 
			 //updateAffectedGlobalsWithAWN();
		 }
	 }
	 
	 /**
	  * updateAffectedGlobalsWithAWN - 
	  */
//	 public void updateAffectedGlobalsWithAWN() {
//		 
//		 //System.out.println(semanticDiff.affectedGlobals.toString());
//		 
//		 /*
//		  * Now we need to be able to find the relationship between a
//		  * Field object and the Integers that the other affected
//		  * locations are tracked with.
//		  */
//		 semanticDiff.AWNPrime
//	 }
}