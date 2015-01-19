package gov.nasa.jpf.regression.listener;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.JPF;
import gov.nasa.jpf.PropertyListenerAdapter;
import gov.nasa.jpf.jvm.bytecode.JVMFieldInstruction;
import gov.nasa.jpf.jvm.bytecode.GETFIELD;
import gov.nasa.jpf.jvm.bytecode.GETSTATIC;
import gov.nasa.jpf.jvm.bytecode.IfInstruction;
import gov.nasa.jpf.jvm.bytecode.JVMInvokeInstruction;
import gov.nasa.jpf.jvm.bytecode.JVMReturnInstruction;
import gov.nasa.jpf.regression.analysis.AffectedNodes;
import gov.nasa.jpf.regression.analysis.AnalyzeInterProceduralDiff;
import gov.nasa.jpf.regression.analysis.ComputeInterProceduralDiff;
import gov.nasa.jpf.regression.analysis.InterproceduralAnalysis;
import gov.nasa.jpf.regression.ast.ASTLoader;
import gov.nasa.jpf.regression.ast.MethodASTInfo;
import gov.nasa.jpf.regression.bytecode.AffectedMethodInfo;
import gov.nasa.jpf.regression.bytecode.IPAffectedBlocks;
import gov.nasa.jpf.regression.bytecode.StackNode;
import gov.nasa.jpf.regression.callgraph.AnnotatedCallEdge;
import gov.nasa.jpf.regression.output.PrintCallGraph;
import gov.nasa.jpf.regression.output.PrintInterProcResults;
import gov.nasa.jpf.regression.output.PrintToDot;
import gov.nasa.jpf.report.ConsolePublisher;
import gov.nasa.jpf.report.PublisherExtension;
import gov.nasa.jpf.search.Search;
import gov.nasa.jpf.symbc.numeric.PCChoiceGenerator;
import gov.nasa.jpf.vm.ChoiceGenerator;
import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.MethodInfo;
import gov.nasa.jpf.vm.StackFrame;
import gov.nasa.jpf.vm.SystemState;
import gov.nasa.jpf.vm.ThreadInfo;
import gov.nasa.jpf.vm.VM;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

public class InterProcPruningListener extends
				PropertyListenerAdapter implements
						 	PublisherExtension {

	@SuppressWarnings("unused")
	private static final boolean debug = false;
	private static boolean test = false;
	
	private static int dropCount = 0;
	private static int impCount = 0;

	Config config;
	
	//the following are used for regression testing purposes
	protected int chgAffectedWCNodes = 0;
	protected int affectedWCNodes = 0;
	protected int chgAffectedWNodes = 0;
	protected int affectedWNodes = 0;
	protected int chgAffectedCNodes = 0;
	protected int affectedCNodes = 0;
	protected int chgNodes = 0;
	protected int chgCodeNodes = 0;
	protected int chgAssertNodes = 0;
	protected int unAffectedNodes = 0;
	protected int totalNodes = 0;
	//format of these is <mname>:<loc>:<loc>,<mname>:<loc>,<loc>...
	protected String impactedWriteLocs="";
	protected String impactedCondLocs="";
	protected String impactedRetLocs="";
	protected String impactedInvLocs="";

	Map<String,MethodASTInfo> methodASTInfo =
					new HashMap<String,MethodASTInfo>();
	InterproceduralAnalysis analyses;
	String[] astInfo = new String[0];
	String[] oldClassInfo = new String[0];
	String[] newClassInfo = new String[0];
	String direction = "regular";
	String dotFile = "";
	String statFile = "";
	boolean printCounts = false;
	String mainClassName = "";
	PrintCallGraph printCallGraphs;
	PrintInterProcResults printInterProcResults;
	static Stack<StackNode> stack;
	static int unsatCounter = 0;
	
	boolean preciseHeap = false;
	
	public InterProcPruningListener(Config conf, JPF jpf) {
		System.out.println("---------->Using Interprocedural PruningRSE Listener 07/19/2013");
		jpf.addPublisherExtension(ConsolePublisher.class, this);

		this.config = conf;
		stack = new Stack<StackNode>();
		if (conf.containsKey("rse.direction")) {
			direction = conf.getString("rse.direction");
		}
		System.out.println("Direction: " + direction);
		//added and removed methods will also be included
		//added will have changed lines in modified version and matched=false
		//removed will have changed line in original version and matched=false
		if (conf.containsKey("rse.ASTResults")){ //path to XML file
			astInfo = conf.getStringArray("rse.ASTResults");
			ASTLoader loader = new ASTLoader();
			methodASTInfo = loader.loadAST(astInfo,direction);
		}
		if (debug)
			System.out.println(methodASTInfo.toString());
		if (conf.containsKey("rse.test"))
			test = conf.getBoolean("rse.test");
		if (conf.containsKey("rse.dotFile"))
			dotFile = conf.getString("rse.dotFile");
		if (conf.containsKey("rse.printCounts"))
			printCounts = conf.getBoolean("rse.printCounts");
		if (conf.containsKey("rse.statFile"))
			statFile = conf.getString("rse.statFile");
		if (conf.containsKey("target"))
			mainClassName = conf.getString("target");
		if (direction.equalsIgnoreCase("regular")){
			oldClassInfo = conf.getStringArray("rse.oldClassInfo");
			newClassInfo = conf.getStringArray("rse.newClassInfo");
		}else{ //"reverse"
			newClassInfo = conf.getStringArray("rse.oldClassInfo");
			oldClassInfo = conf.getStringArray("rse.newClassInfo");
		}
		
		// check if the precise heap analysis is turned on
		if(conf.containsKey("rse.heap.precise")) {
			preciseHeap = conf.getBoolean("rse.heap.precise");
		}
	}
	
	public void printStatsToStdOut(){
		System.out.println("Number of changed AW/CN=" + chgAffectedWCNodes);
		System.out.println("Number of AW/CN=" + affectedWCNodes);
		System.out.println("Number of changed AWN=" + chgAffectedWNodes);
		System.out.println("Number of AWN=" + affectedWNodes);
		System.out.println("Number of changed ACN=" + chgAffectedCNodes);
		System.out.println("Number of ACN=" + affectedCNodes);
		System.out.println("Number of changed nodes=" + chgNodes);
		System.out.println("Number of changed code nodes=" + chgCodeNodes);
		System.out.println("Number of changed assert nodes=" + chgAssertNodes);
		System.out.println("Number of unaffected nodes=" + unAffectedNodes);
		System.out.println("Total number of nodes=" + totalNodes);
		System.out.println("Impacted Write Locations: " + impactedWriteLocs);
		System.out.println("Impacted Condition Locations: " + impactedCondLocs);
		System.out.println("Impacted Return Locations: " + impactedRetLocs);
		System.out.println("Impacted Invoke Locations: " + impactedInvLocs);
	}

	//supports regression testing
	private void setCounts(PrintToDot dot){
		if (dot != null){
			chgAffectedWCNodes = dot.chgAffectedWCNodes;
			affectedWCNodes = dot.affectedWCNodes;
			chgAffectedWNodes = dot.chgAffectedWNodes;
			affectedWNodes = dot.affectedWNodes;
			chgAffectedCNodes = dot.chgAffectedCNodes;
			chgCodeNodes = dot.chgCodeNodes;
			chgAssertNodes = dot.chgAssertNodes;
			affectedCNodes = dot.affectedCNodes;
			chgNodes = dot.chgNodes;
			unAffectedNodes = dot.unAffectedNodes;
			totalNodes = dot.totalNodes;
		}else
			System.out.println("WARNING: dot is null");
	}

	/*
	 * Helper method to collect the class files
	 */
	 static private List<File> getSortedFileListing(File aStartingDir)
		throws FileNotFoundException {
		 validateDirectory(aStartingDir);
		 List<File> result = new ArrayList<File>();
		 File[] filesAndDirs = aStartingDir.listFiles();
		 Arrays.sort(filesAndDirs);
		 List<File> filesDirs = Arrays.asList(filesAndDirs);
		 for(File file : filesDirs) {
			 result.add(file); //always add, even if directory
			 if ( ! file.isFile() ) {
				 //must be a directory
				 //recursive call!
				 List<File> deeperList = getSortedFileListing(file);
				 result.addAll(deeperList);
			 }
		 }
		 return result;
	 }

	/*
	 * Helper method to collect the class files
	 */
	 static private void validateDirectory (File aDirectory) throws FileNotFoundException {
		 if (aDirectory == null) {
			 throw new IllegalArgumentException("Directory should not be null.");
		 }
		 if (!aDirectory.exists()) {
			 throw new FileNotFoundException("Directory does not exist: " + aDirectory);
		 }
		 if (!aDirectory.isDirectory()) {
			 throw new IllegalArgumentException("Is not a directory: " + aDirectory);
		 }
		 if (!aDirectory.canRead()) {
			 throw new IllegalArgumentException("Directory cannot be read: " + aDirectory);
		 }
	 }

	@Override
	public void searchStarted(Search search) {

		List<File> oldClasses = new ArrayList<File>();
		List<File> newClasses = new ArrayList<File>();

		try{
			if(InterProcPruningListener.debug) {
				System.out.println("search started with interprocedural pruning");
			}
			for (int i=0; i< oldClassInfo.length; i++){
				File f = new File(oldClassInfo[i]);
				if (f.isDirectory()){
					oldClasses.addAll(getSortedFileListing(f));
				} else { // its a file
					oldClasses.add(f);
				}
			}
			for (int i=0; i< newClassInfo.length; i++){
				File f = new File(newClassInfo[i]);
				if (f.isDirectory()){
					newClasses.addAll(getSortedFileListing(f));
				} else { //its a file
					newClasses.add(f);
				}
			}

		}catch (Exception e){
			e.printStackTrace();
			System.exit(1);
		}

		//now do something with the old classes & new classes
		analyses = new InterproceduralAnalysis(oldClasses, newClasses,
				methodASTInfo, mainClassName);
		analyses.setPreciseHeap(this.preciseHeap);
		printCallGraphs = new PrintCallGraph(dotFile, mainClassName,
				analyses.getOldCallGraph(),
				analyses.getNewCallGraph(),
				analyses.getModifiedCallNodes());
		// Print the old and new call graphs
		printCallGraphs.printOldCallGraph();
		printCallGraphs.printNewCallGraph();
		// assuming that "0" is the top of the call graph
		analyses.searchCallGraph(0);
		printInterProcResults = new PrintInterProcResults(config,
				analyses.getNewCallGraph(), analyses.getDifferences());
		PrintToDot dot = new PrintToDot();
		dot.printCFG(dotFile, mainClassName, methodASTInfo, analyses.getDifferences());
		if (test){
			setCounts(dot);
			computeImpactedLocs(analyses);
		}
		ThreadInfo current = search.getVM().getCurrentThread();
		createAndPushStackNode(current);
	}
	
	private void computeImpactedLocs(InterproceduralAnalysis analyses){
		Map<String, ComputeInterProceduralDiff> res = 
			analyses.getDifferences();
		Iterator<Map.Entry<String,ComputeInterProceduralDiff>> it = 
			res.entrySet().iterator();
	    while (it.hasNext()) {
	        Map.Entry<String,ComputeInterProceduralDiff> pairs = it.next();
	        String mName = pairs.getKey();
	        ComputeInterProceduralDiff resultPair = pairs.getValue();
	        AnalyzeInterProceduralDiff diff = resultPair.getSemanticDiff();
	        Map<String,Map<Integer,Integer>> posLineMap = resultPair.getNewPosLineMap();
        	Map<Integer,Integer> pToLineMap = posLineMap.get(mName);
	        String tmp = mName.trim() + ":";
	        //extract impacted condition locs
	        Set<Integer> iCond = diff.getGlobalTrackCondition();
	        if (iCond.size()>0){
		        Iterator<Integer> itC = iCond.iterator();
		        while (itC.hasNext()){
		        	Integer loc = itC.next();
		        	loc = pToLineMap.get(loc);
		        	tmp = tmp + loc.toString();
		        	if (itC.hasNext())
		        		tmp = tmp + ":";
		        }
		        impactedCondLocs = impactedCondLocs + tmp + ",";
	        }
	        tmp = mName.trim() + ":";
	        //extract impacted invoke locations
	        Set<Integer> iInvoke = diff.getGlobalTrackInvoke();
	        if (iInvoke.size()>0){
	        	Iterator<Integer> itI = iInvoke.iterator();
	        	while (itI.hasNext()){
	        		Integer loc = itI.next();
	        		loc = pToLineMap.get(loc);
		        	tmp = tmp + loc.toString();
	        		if (itI.hasNext())
	        			tmp = tmp + ":";
	        	}
	        	impactedInvLocs = impactedInvLocs + tmp + ",";
	        }
	        tmp = mName.trim() + ":";
	        //extract impacted return locations
	        Set<Integer> iReturn = diff.getGlobalTrackReturn();
	        if (iReturn.size()>0){
	        	Iterator<Integer> itR = iReturn.iterator();
	        	while (itR.hasNext()){
	        		Integer loc = itR.next();
	        		loc = pToLineMap.get(loc);
		        	tmp = tmp + loc.toString();
	        		if (itR.hasNext())
	        			tmp = tmp + ":";
	        	}
	        	impactedRetLocs = impactedRetLocs + tmp + ",";
	        }
	        tmp = mName.trim() + ":";    
	        //extract impacted write locations
	        Set<Integer> iWrite = diff.getGlobalTrackWrite();
	        if (iWrite.size()>0){
	        	Iterator<Integer> itW = iWrite.iterator();
	        	while (itW.hasNext()){
	        		Integer loc = itW.next();
	        		loc = pToLineMap.get(loc);
		        	tmp = tmp + loc.toString();
	        		if (itW.hasNext())
	        			tmp = tmp + ":";
	        	}
	        	impactedWriteLocs = impactedWriteLocs + tmp + ",";
	        }
	    }
	    if (impactedCondLocs.endsWith(","))
        	impactedCondLocs = impactedCondLocs.substring(0,
        		impactedCondLocs.length()-1);
        if (impactedInvLocs.endsWith(","))
        	impactedInvLocs = impactedInvLocs.substring(0,
        		impactedInvLocs.length()-1);
        if (impactedRetLocs.endsWith(","))
        	impactedRetLocs = impactedRetLocs.substring(0,
        		impactedRetLocs.length()-1);
        if (impactedWriteLocs.endsWith(","))
        	impactedWriteLocs = impactedWriteLocs.substring(0,
        		impactedWriteLocs.length()-1);
	}


	private void createAndPushStackNode(ThreadInfo current) {
	
		if(current.getPC() != null) {
			Integer position = current.getPC().getPosition();
			String methodName = current.getPC().getMethodInfo().getFullName();
			StackNode stackNode = new StackNode(methodName, position);
			stack.push(stackNode);
		}
		else {
			stack.push(null);
		}
	}

	private boolean popStackNode(Search search) {
		boolean popped = false;
		
		assert (stack.size() > 0);
		
		ThreadInfo current = search.getVM().getCurrentThread();
		Integer position = current.getPC().getPosition();
		String methodName = current.getPC().getMethodInfo().
										getFullName();
		
		StackNode top = stack.peek();

		if(top == null) {
			stack.pop();
			return true;
		}
		if(!(top.getMethodIndex().equals(methodName) &&
				position.intValue() == top.getPosition().intValue())) {
			stack.pop();
			popped = true;
		}

		if(stack.size() <= 0) return false;
		top = stack.peek();

		// need to go through and update the sets in IPAffectedBlocks
		List<AffectedMethodInfo> methodCalls = top.getMethodCalls();

		for(int i = methodCalls.size() - 1; i >= 0; i--) {
			String mName = methodCalls.get(i).getCalleeMethodIndex();
			boolean allReached = IPAffectedBlocks.removeMethodSets(mName);
			if(!allReached) {
				String callerName = methodCalls.get(i).getCallerMethodIndex();
				IPAffectedBlocks.updateReachability(callerName,
						methodCalls.get(i).getInvokePosition());
				//TODO save the information about the caller and the positions from
				// which there are unreachable in a data structure
			}
		}
		methodCalls.clear();
		top.clearNodes();

		return popped;

	}

	/**
	 * Checks if the instruction at the point of the program counter
	 * is a choice point (if instruction). If it is, then it gets the
	 * choice generator.
	 * TODO: The last if statement doesn't seem to do anything, it
	 * returns whether true or false. Is this intended?
	 * @param vm
	 */
	private void checkInstruction(VM vm) {
		Instruction insn = vm.getCurrentThread().getPC();
		if(insn == null) return;
		if(!vm.getCurrentThread().isFirstStepInsn()) return;
		if(!(insn instanceof IfInstruction)) return;
		if(vm.getSystemState().getChoiceGenerator() != null) return;
		ChoiceGenerator<?> cg = vm.getSystemState().getChoiceGenerator();
		if (!(cg instanceof PCChoiceGenerator)) return;
	}

	public void checkAtMethodExits(VM vm) {
		MethodInfo mi = vm.getCurrentThread().getTopFrameMethodInfo();
		if(mi == null) return;
		String returnedMethodName  = mi.getFullName();
		if(!IPAffectedBlocks.existsInMap(returnedMethodName)) 
			return;
		boolean allReached = IPAffectedBlocks.
								unreachableLocsExist(returnedMethodName);
		if(allReached) return;

		StackFrame sf = vm.getCurrentThread().getCallerStackFrame();
		String callerMethodName = sf.getMethodInfo().getFullName();
		int invokePos = sf.getPC().getPosition();
		boolean find = IPAffectedBlocks.existsInMap(callerMethodName);
		//assert(find);
		IPAffectedBlocks.updateReachability(callerMethodName, invokePos);
	} 
	
	@Override
	  public void instructionExecuted(VM vm, ThreadInfo currentThread,
			  Instruction nextInstruction, Instruction executedInstruction) {

		checkInstruction(vm);
		Instruction insn = executedInstruction;
		ThreadInfo ti = currentThread;
		//System.out.println("instructionExecuted : " + insn.toString());
		if(insn instanceof IfInstruction && ti.isFirstStepInsn()) {
			buildConstraintPositionMap(vm);
		}
		//System.out.println("after the buildconstraint position map");

		//if a constraint was generated by the previous instruction,
		// it will be used to update the constraint map and the
		// corresponding affected vs. unaffected constraints.
		// empty the cache that holds the previously generated
		// constraint
		//TrackConstraintsListener.resetConstraintStr();
		
		/*
		 * Find the getField and getStatic instructions, this can be
		 * done by finding Instructions that extend JVMFieldInstruction.
		 * 
		 */
		if(insn instanceof JVMFieldInstruction) {
			JVMFieldInstruction fieldInsn = (JVMFieldInstruction)insn;
			if(fieldInsn instanceof GETFIELD) {
				GETFIELD getField = (GETFIELD)fieldInsn;
			}
			else if(fieldInsn instanceof GETSTATIC) {
				GETSTATIC getStatic = (GETSTATIC)fieldInsn;
			}
			
			
		}

		if(insn instanceof JVMInvokeInstruction && !ti.isFirstStepInsn()) {
			JVMInvokeInstruction ivk = (JVMInvokeInstruction) insn;
			MethodInfo caller = insn.getMethodInfo();
			MethodInfo callee = ivk.getInvokedMethod(vm.getCurrentThread());
			ComputeInterProceduralDiff diffCaller = getDifferenceInfo
													(caller.getFullName());
			ComputeInterProceduralDiff diffCallee = getDifferenceInfo
													(callee.getFullName());

			//Needs to be a fresh copy
			AffectedNodes affNodes = new AffectedNodes();
			AffectedMethodInfo am = new AffectedMethodInfo
					(callee.getFullName(), caller.getFullName(),
												ivk.getPosition());
			Set<Integer> dropA = new HashSet<Integer>();
			Set<Integer> condB = new HashSet<Integer>();
			Set<Integer> keep = new HashSet<Integer>();
			boolean crossCheck = false;

			ArrayList<String> affectedFormalVars = new ArrayList<String>();
			ArrayList<String> affectedGlobalVars = new ArrayList<String>();
			//get the affected nodes of the callee
			if(diffCallee != null) {
				assert (stack.peek() != null);
				AffectedNodes calleeAffNodes = diffCallee.
										getAffectedNode("method_body");
				/***System.out.println("*** callee Name: " + callee.getFullName());
				if(!calleeAffNodes.isEmpty())
					System.out.println("*** "+calleeAffNodes.getAffectedCondNodes().toString() + " " +
							calleeAffNodes.getAffectedWriteNodes().toString() + " " +
							calleeAffNodes.getAffectedCallNodes().toString() + " " +
							calleeAffNodes.getAffectedReturnNodes().toString());
				else
					System.out.println("*** callAffNodes: appears to be empty.");  ***/
				if(!calleeAffNodes.isEmpty()) {
					dropA.addAll(calleeAffNodes.getCanbeDropped());
					condB.addAll(calleeAffNodes.getAffectedCondNodes());
					affNodes.addAllAffectedNodes(calleeAffNodes);
				}
			}
			if(diffCaller != null) {
				//get the affected call edges from the caller and then
				// get the correct affected nodes such that the variables
				// in the call edge map to the arguments in the callee
				// the affected nodes for those arguments/parameters

				AnnotatedCallEdge callerEdge = diffCaller.getSemanticDiff().
											getAnnotatedCallEdge(insn.getPosition());
				if(callerEdge != null) {
					
					ArrayList<String> indices = new ArrayList<String>();
					indices.add("method_body");
					//System.out.println(insn.getMethodInfo().getFullName() + "------?");
					
					
					indices.addAll(IPAffectedBlocks.getAffectedFormalVars
							(insn.getMethodInfo().getFullName(), diffCaller));

					indices.addAll(IPAffectedBlocks.getAffectedGlobalVars
								(insn.getMethodInfo().getFullName(), diffCaller));
					
		
					//affectedGlobalVars.addAll(IPAffectedBlocks.getAffectedGlobalVars
					//				(insn.getMethodInfo().getFullName(), diffCaller));
					
					if (InterProcPruningListener.debug)
						System.out.println("indices :" + indices.toString());
					Set<String> exploredVars = new HashSet<String>();

					for(int inIndex = 0; inIndex < indices.size(); inIndex++) {
						ArrayList<String> annotations = callerEdge.
								getAnnotations(indices.get(inIndex));
						ArrayList<String> globalVarAnnotations = callerEdge.
								getGlobalVarAnnotations(indices.get(inIndex));

						if(annotations.isEmpty() && 
								globalVarAnnotations.isEmpty()) continue;

						if(!annotations.isEmpty()) {
							annotations.removeAll(exploredVars);
							exploredVars.addAll(annotations);
						}
						ArrayList<Integer> positions = diffCaller.getSemanticDiff().
								mapAnnotatedVarsToIndices
								(insn.getPosition(), annotations);

						// now map it back to the callee
						HashMap<Integer, String> argIndexToVars =
								diffCallee.getArgIndexToVarName();
						for(int posIndex =0 ; posIndex < positions.size(); posIndex++) {
							Integer pos = positions.get(posIndex);
							assert (argIndexToVars.containsKey(pos));
							String varName = argIndexToVars.get(pos);
							AffectedNodes calleeVarAffNodes = diffCallee.
									getAffectedNode(varName);
							affectedFormalVars.add(varName);
							if(calleeVarAffNodes == null || calleeVarAffNodes.isEmpty()) 
								continue;


							affNodes.addAllAffectedNodes(calleeVarAffNodes);
							this.computeToKeepSet(dropA, condB, calleeVarAffNodes.getCanbeDropped(),
									calleeVarAffNodes.getAffectedCondNodes(), keep);
							crossCheck = true;

						}

						for(int globalIndex = 0; globalIndex < globalVarAnnotations.size();
								globalIndex++) {
							String globalVarName = globalVarAnnotations.get(globalIndex);
							AffectedNodes calleeGlobalVarAffNodes = diffCallee.
									getAffectedNode(globalVarName);
							if(!affectedGlobalVars.contains(globalVarName))
								affectedGlobalVars.add(globalVarName);

							if(calleeGlobalVarAffNodes == null || 
									calleeGlobalVarAffNodes.isEmpty()) continue;

							affNodes.addAllAffectedNodes(calleeGlobalVarAffNodes);
							computeToKeepSet(dropA, condB, calleeGlobalVarAffNodes.getCanbeDropped(),
									calleeGlobalVarAffNodes.getAffectedCondNodes(), keep);
							crossCheck = true;

						}

					}
				}
			if(!crossCheck) {
				if(dropA.size() > 0) {
					affNodes.addCanBeDropped(dropA);
					//System.out.println("affNodes:" + affNodes.toString());
				}
			} else{
				if(keep.size() > 0) {
					affNodes.addCanBeDropped(keep);
					//System.out.println("affNodes:" + affNodes.toString());
				}
			}
		}
		if(!affNodes.isEmpty()) {
			StackNode sn = stack.peek();
			sn.addAffectedMethodInfo(am);
				//add the affectedMethodInfo to the stack
				/*
				 * Sets up a unique mapping of this invocation instance
				 * to the affected nodes.
				 */
			//System.out.println(affectedGlobalVars.toString() + " *************************");
			IPAffectedBlocks.addMethodToSets(callee.getFullName(),
						affNodes, affectedFormalVars, affectedGlobalVars);
		
		}


		}

		// Update the affected and reached sets
		if(!ti.isFirstStepInsn()) {
			IPAffectedBlocks.checkPos(insn.getPosition(), insn.getMethodInfo().
															getFullName());
		}
		
		if(insn instanceof JVMReturnInstruction) {
			checkAtMethodExits(vm);
		}

	}

	private void computeToKeepSet(Set<Integer> A, Set<Integer> B,
			Set<Integer> C, Set<Integer> D, Set<Integer> Keep) {
		Iterator<Integer> dropCItr = C.iterator();
		while(dropCItr.hasNext()) {
			Integer cElem = dropCItr.next();
			if(A.contains(cElem))
				Keep.add(cElem);
			else if(!B.contains(cElem)) {
				Keep.add(cElem);
			}
		}
		Iterator<Integer> dropAItr = A.iterator();
		while(dropAItr.hasNext()) {
			Integer aElem = dropAItr.next();
			if (!D.contains(aElem)) {
				Keep.add(aElem);
			}
		}
	}

	private ComputeInterProceduralDiff
				getDifferenceInfo(String uniqueMethodIndex) {
		if(analyses.getDifferences().containsKey(uniqueMethodIndex))
			return analyses.getDifferences().get(uniqueMethodIndex);
		return null;
	}


	@Override
	public void stateAdvanced(Search search) {
		VM vm = search.getVM();
		ThreadInfo current = vm.getCurrentThread();
		//createAndPushStackNode(current);

		// if the new state is feasible (the constraint is satisfiable)
		if(!search.getVM().getSystemState().isIgnored()) {
			createAndPushStackNode(current);
		} else {
			InterProcPruningListener.unsatCounter++;
			/**System.out.println("is ignored***************************");
			System.out.println("checkign value :" + TrackConstraintsListener.getConstraintStr());
			Constraint c = TrackConstraintsListener.getLastConstraint();
			Map<String, Object> varsVals = new HashMap<String, Object>();
			if(c != null) {
			c.getVarVals(varsVals);
			System.out.println(varsVals.toString());
			}
			System.out.println(vm.getLastInstruction().toString());**/
			// if the new state lies along an infeasible path (the constraint
			// is unsatisfiable). mark the conditional
			IPAffectedBlocks.resetCondPos(current.getPC().getPosition(),
					current.getPC().getMethodInfo().getFullName());
			//createAndPushStackNode(null);
		}
	}

	@Override
	public void searchFinished(Search search) {
		if (InterProcPruningListener.debug){
			System.out.println("searchFinished");
			System.out.println("Unimpacted Constraints: " + InterProcPruningListener.dropCount);
			System.out.println("Impacted Constraints: " + InterProcPruningListener.impCount);
		}
	}

	private void buildConstraintPositionMap(VM vm) {
		ThreadInfo current = vm.getCurrentThread();
		if(current.getPC() == null) return;
		Integer position = current.getPC().getPosition();
		String methodName = current.getPC().getMethodInfo().
										getFullName();
		//System.out.println(methodName +" -%%> "+ position+ " -&&> :" + "before the beginning");

		String strConstraint = TrackConstraintsListener.getConstraintStr();
		if(!strConstraint.equals("")) {
			StackNode sn = stack.peek();
			assert (sn != null);
			if(IPAffectedBlocks.isFunctional(position, methodName)) {
				//System.out.println(methodName +" --> "+ position+ " --> :" + strConstraint);
				sn.addFunctionalConditional(strConstraint);
				InterProcPruningListener.dropCount++;
			} else { // these are the dropped constraints
				//System.out.println(methodName +" **> "+ position+ " **> :" + strConstraint);
				sn.addNonFunctionalConditional(strConstraint);
				InterProcPruningListener.impCount++;
			}
		}
	}

	public static ArrayList<String> getDroppedCondNodes() {
		ArrayList<String> droppedConstraints =
							new ArrayList<String>();
		if(stack == null) return null;
		ArrayList<String> functionalConstraints =
								getFunctionalCondNodes();
		Iterator<StackNode> snItr = stack.iterator();
		while(snItr.hasNext()) {
			StackNode sn = snItr.next();
			droppedConstraints.addAll(sn.getFeasibleUnimpactedConds());
		}
		//in different methods the same constraints may have been generated
		// that is affected. The same constraint if is also contained within
		// the dropped constraints then it should be be removed
		if(functionalConstraints != null) {
			droppedConstraints.removeAll(functionalConstraints);
		}
		return droppedConstraints;
	}

	public static ArrayList<String> getFunctionalCondNodes() {
		ArrayList<String> functionalCondNodes =
							new ArrayList<String>();
		if(stack == null) return null;
		Iterator<StackNode> snItr = stack.iterator();
		while(snItr.hasNext()) {
			StackNode sn = snItr.next();
			//System.out.println("sn.getCurrentConds:" + sn.getCurrentConds().toString());
			functionalCondNodes.addAll(sn.getCurrentConds());
		}
		return functionalCondNodes;
	}

	private boolean checkReachabilityAlongCallChain(StackFrame caller) {
		// check if there is anything reachable in the caller
		
		String callerMethodName = caller.getMethodInfo().getFullName();
		Integer callPos = caller.getPC().getPosition();
		ComputeInterProceduralDiff callerCipd = this.
								getDifferenceInfo(callerMethodName);
		boolean callReachable = IPAffectedBlocks.
					isReachable(callerMethodName, callPos, callerCipd);
		return callReachable;
	
	}
	
	@Override
	public void stateBacktracked(Search search) {

		ThreadInfo current = search.getVM().getCurrentThread();
		Integer position = current.getPC().getPosition();
		String methodName = current.getPC().getMethodInfo().
										getFullName();

		boolean popped = popStackNode(search);

		ComputeInterProceduralDiff cipd = this.getDifferenceInfo(methodName);
		boolean reachable = IPAffectedBlocks.
			isReachable(methodName, position, cipd);

			
		//this goes along the whole calling chain
		StackFrame caller = current.getCallerStackFrame();
		
		while(!reachable && caller != null) {
			reachable = reachable | 
							checkReachabilityAlongCallChain(caller);
			caller = caller.getPrevious();
		}

		// if it isn't reachable, then you have gone as far as you can
		// on this path, so prune the rest.
		if(!reachable) {
			//System.out.println("not reachable ***** :" + methodName + " -->" + position);
			SystemState ss = search.getVM().getSystemState();
			ChoiceGenerator<?> cg = ss.getChoiceGenerator();
			assert (cg != null);
			cg.setDone();
			if(!popped && stack.size() > 0) {
				stack.pop();
			}
		}
	}

}