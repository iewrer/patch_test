package gov.nasa.jpf.regression.analysis;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.bcel.classfile.LineNumberTable;
import org.apache.bcel.generic.MethodGen;
import edu.byu.cs.trace.AbstractMethodInfo;
import gov.nasa.jpf.regression.ast.BlockASTInfo;
import gov.nasa.jpf.regression.ast.MethodASTInfo;
import gov.nasa.jpf.regression.cfg.ByteSourceHandler;
import gov.nasa.jpf.regression.cfg.CFG;
import gov.nasa.jpf.regression.cfg.CFGBuilder;
import gov.nasa.jpf.regression.analysis.SCC.Loop;

public class ComputeIntraProceduralDiff extends ComputeDifferences {

    protected CFGBuilder cfgbNew;
    protected CFGBuilder cfgbOld;
    
    private boolean verbose = false;

    static HashMap<String, AnalyzeIntraProceduralDiff> analysis
    					= new HashMap<String, AnalyzeIntraProceduralDiff>();
    public String classname;


    /**
     * Constructor only initializes data structures for
     * the new class.
     * @param String classNameNew
     * @throws Exception
     */
    public ComputeIntraProceduralDiff(String classNameNew,
    						String classNameOld) throws Exception {
    	ByteSourceHandler bsHandlerNew = new ByteSourceHandler();
        bsHandlerNew.readSourceFile(classNameNew);
       	cfgbNew = new CFGBuilder();
	   	cfgbNew.parseClass(bsHandlerNew.classFile);

	   	ByteSourceHandler bsHandlerOld = new ByteSourceHandler();
	   	bsHandlerOld.readSourceFile(classNameOld);
	   	cfgbOld = new CFGBuilder();
	   	cfgbOld.parseClass(bsHandlerOld.classFile);
	   	
	   	this.classname = cfgbOld.className;

    }

    public ComputeIntraProceduralDiff(CFGBuilder newBuilder,
    							CFGBuilder oldBuilder) {
    	this.cfgbNew = newBuilder;
    	this.cfgbOld = oldBuilder;
    }

    public CFG getOriginalCFG(String methodName, MethodASTInfo methodASTInfo){
    	try{
    		return cfgbOld.getCFG(methodName, "original", methodASTInfo);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
    }


    public CFG getCFG(String methodName, MethodASTInfo methodASTInfo){
    	try{
    		return cfgbNew.getCFG(methodName, "modified", methodASTInfo);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
    }

    public static AnalyzeIntraProceduralDiff getSemanticAnalysis(String mName){
    	return analysis.get(mName);
    }


    public static boolean checkIfCondition(Integer position, String methodName) {
    	//System.out.println("check if condition" + methodName);
    	if(analysis.containsKey(methodName)) {
    		AnalyzeIntraProceduralDiff analyze = analysis.get(methodName);
    		if(analyze.globalTrackCond.contains(position)) {
    			return true;
    		}
    	}
    	return false;

    }

    public static boolean checkDistanceToWriteIns(Integer condPos,
    									Integer position, String methodName) {
    	if(analysis.containsKey(methodName)) {
    		AnalyzeIntraProceduralDiff analyze = analysis.get(methodName);
    		if(!analyze.branchToDependentWriteInsMap.
    						containsKey(condPos)) return false;
    		ArrayList<Integer> dWritePos = analyze.
    						branchToDependentWriteInsMap.get(condPos);
    		Set<Integer> writePos = analyze.globalTrackWrite;

    		Iterator<Integer> wItr = writePos.iterator();
    		while(wItr.hasNext()) {
    			Integer wPos = wItr.next();

    			if(dWritePos.contains(wPos) && ComputeIntraProceduralDiff.
    					isReachable(analyze.cfg, position, wPos))
    				return true;
    			else {
    				Set<Integer> dBranch = analyze.dependentTrack;
    				Iterator<Integer> bItr = dBranch.iterator();
    				while(bItr.hasNext()) {
    					Integer bPos = bItr.next();
    					int condPosIndex = analyze.condToIntMap.get(condPos);
    					int bPosIndex = analyze.condToIntMap.get(bPos);
    					if(analyze.dm.getValue(condPosIndex, bPosIndex)
    							== Integer.MAX_VALUE) continue;

    					if(ComputeIntraProceduralDiff.
    							isReachable(analyze.cfg, bPos, wPos))
    						return true;
    				}
    			}
    		}
    	}
    	return false;
    }

    private static boolean isReachable(CFG cfg, int start, int end) {
    	int startOffset = cfg.getStartOffset(start);
    	int endOffset = cfg.getStartOffset(end);
		//	  if there exits a path from wPos, cPos
		int reachable = cfg.getDistanceMatrix()
						.getValue(cfg.getIndexOfPosition(startOffset),
				cfg.getIndexOfPosition(endOffset));
		if(reachable < Integer.MAX_VALUE ) {
			return true;
		}
		return false;
    }

    public static int getDistanceToConditionalIns(Integer pos, String methodName) {
    	int min = Integer.MAX_VALUE;
    	if(analysis.containsKey(methodName)) {
    		AnalyzeIntraProceduralDiff analyze = analysis.get(methodName);
    		Set<Integer> condPos = analyze.globalTrackCond;
    		Iterator<Integer> cItr = condPos.iterator();
    		CFG cfg = analyze.cfg;
    		while(cItr.hasNext()) {
    			Integer cPos = cItr.next();
    			int posOffset = cfg.getStartOffset(pos);
    			int condPosOffset = cfg.getStartOffset(cPos);
    			int reachable = cfg.getDistanceMatrix().
    						getValue(cfg.getIndexOfPosition(posOffset),
    					cfg.getIndexOfPosition(condPosOffset));
    			if(min > reachable) {
    				min = reachable;
    			}

    		}
    	}
    	return min;
    }

    public static Loop checkLoops(Integer pos, String methodName, Integer next,
    																Integer target) {
    	if(analysis.containsKey(methodName)) {
    		AnalyzeIntraProceduralDiff analyze = analysis.get(methodName);
    		if(analyze.scc.allSCC.contains
    					(analyze.cfg.getStartOffset(pos))) {
    			boolean foundNext = false;
    			boolean foundTarget = false;
    			if(analyze.scc.allSCC.contains
    					(analyze.cfg.getStartOffset(next))) foundNext = true;
    			if(analyze.scc.allSCC.contains
    					(analyze.cfg.getStartOffset(target))) foundTarget = true;
    			if(foundNext) {
    				if(foundTarget) {
    					return Loop.TARGET_AND_NEXT;
    				} else{
    					return Loop.NEXT;
    				}
    			}
    			if(foundTarget) {
    				return Loop.TARGET;
    			}
    		}
    	}
    	return Loop.NONE;
    }

    public static boolean isPartOfLoop(Integer pos, String methodName) {
	    	if(analysis.containsKey(methodName)) {
	    		AnalyzeIntraProceduralDiff analyze = analysis.get(methodName);
	    		if(analyze.scc.allSCC.contains
	    					(analyze.cfg.getStartOffset(pos))) {
	    			return true;
	    		}
	    	}
	    	return false;
    }

    public void computeChangedInfo(String methodName, MethodASTInfo methodASTInfo, boolean filterAffMode) {
    	CFG newCfg;
		CFG oldCfg;
		try {
	    	if(analysis.containsKey(methodName)) return;
			oldCfg = cfgbOld.getCFG(methodName,"original", methodASTInfo);

			if(oldCfg.getRemovedInstructions().size() > 0) {
				AnalyzeIntraProceduralDiff diff =
					checkOldInformation(methodName, oldCfg, cfgbOld.getMethodGen(methodName));

				mapRemovedNode(methodASTInfo, cfgbOld.getMethodGen(methodName), diff);
			}

			newCfg = cfgbNew.getCFG(methodName, "modified", methodASTInfo);
			//System.out.println(newCfg.modifiedWritesAndIfs.toString());
			preciseAnalysis(methodName, newCfg, cfgbNew.getMethodGen(methodName), filterAffMode);


		} catch (Exception e) {
			System.err.println("exception raised in precise analysis");
			e.printStackTrace();
			System.exit(1);
		}
    }

    protected void addFinalControlDependentNodes(AnalyzeIntraProceduralDiff semanticDiff) {
    	Set<Integer> newCondPositions = new HashSet<Integer>();
    	// TODO: this breaks the regression tests in DiSE and needs to be _FIXED_ **/
    	// TODO: test it in the DiSE framework as well
    	newCondPositions.addAll(semanticDiff.markOtherConditionalBranches(null, false));
    	semanticDiff.clearTrackedCondAndUpdateGlobal();

    }
    //如果有original block中的内容是受到影响的
    //将其对应的modified block中的内容添加到methodASTInfo的changedModifiedLines里面
    protected void mapRemovedNode(MethodASTInfo methodASTInfo, MethodGen mg,
    		AnalyzeIntraProceduralDiff oldSemantic) {
    	Set<BigInteger> matched =  new HashSet<BigInteger>();
    	Map<BigInteger, BlockASTInfo> orig = methodASTInfo.getOriginalBlocks();
    	LineNumberTable lnt = mg.getLineNumberTable(mg.getConstantPool());
    	Set<Integer> global = oldSemantic.globalTrackCond;
    	global.addAll(oldSemantic.globalTrackWrite);
    	
    	//先获得original block，对每一个block找到他的match block，如果包括globalTrackCond、globalTrackWrite，则记录之
    	Iterator<BigInteger> itrOrg = orig.keySet().iterator();
    	while(itrOrg.hasNext()) {
    		BigInteger bi = itrOrg.next();
    		BlockASTInfo bo = orig.get(bi);
    		Iterator<Integer> gItr = global.iterator();
    		while(gItr.hasNext()) {
    			Integer gPos = gItr.next();
    			int lineNum = lnt.getSourceLine(gPos);
    			if(bo.getStartLine() >= lineNum &&
    					bo.getEndLine() <= lineNum) {
    				matched.add(bo.getMatchedBlock());
    				break;
    			}
    		}
    	}
    	//遍历matched block，将match上的modified block添加到methodASTInfo的changedModifiedLines里面
    	Map<BigInteger, BlockASTInfo> modified = methodASTInfo.
    	getModifiedBlocks();
    	Iterator<BigInteger> matchItr = matched.iterator();
    	while(matchItr.hasNext()) {
    		BigInteger mIndex = matchItr.next();
    		BlockASTInfo mod = modified.get(mIndex);
    		int startLine = mod.getStartLine();
    		int endLine = mod.getEndLine();
    		for(int lnIndex = startLine; lnIndex <= endLine; lnIndex++) {
    			methodASTInfo.addChangedMod(lnIndex);
    		}
    	}
    }

   protected AnalyzeIntraProceduralDiff checkOldInformation
   					(String methodName, CFG oldCFG, MethodGen oldMg) {
	   AbstractMethodInfo absMethodInfoOld = new AbstractMethodInfo(oldMg);
	   absMethodInfoOld.runAnalysis(false);
	   AnalyzeIntraProceduralDiff semanticDiffOld = new
	   				AnalyzeIntraProceduralDiff(absMethodInfoOld, oldCFG, oldMg);
	   semanticDiffOld.analyzeArithmeticAndWriteOperations();
	   semanticDiffOld.analyzeConditionalBranchStatements();
		 //System.out.println("cfg.modifiedWritesAndIfs :" + cfg.modifiedWritesAndIfs);

	   semanticDiffOld.checkModifiedWriteStatement(oldCFG.getRemovedInstructions(),
			   										new HashSet<Integer>());

	   Set<Integer> allVals = new HashSet<Integer>();
	   allVals.addAll(oldCFG.getRemovedInstructions());
	   allVals.addAll(semanticDiffOld.trackCond);

	   semanticDiffOld.generateSetOfAffectCondBranches(allVals, true);
	   //System.out.println(semanticDiffOld.globalTrackCond + " globalTrackCond in old");
	   //System.out.println(semanticDiffOld.globalTrackWrite + " globalTrackWrite in old");


	   HashMap<String, ArrayList<Integer>> writeVars = semanticDiffOld.
			genWriteInsCDOnBranches(semanticDiffOld.getTrackedBranches(), true);
	   //System.out.println("trackCond is: " + semanticDiffOld.trackCond.toString());


	   Set<Integer> newCondPositions = semanticDiffOld.
	 					getCondBranchesWithVars(extractVariableNames(writeVars));

	   semanticDiffOld.checkReachability(this.extractWriteLocations(writeVars),
			 											newCondPositions, true);

	   semanticDiffOld.clearTrackedCondAndUpdateGlobal();



	   //System.out.println("writeVars :"  + writeVars.toString());

	   ArrayList<String> varNames = semanticDiffOld.getRelevantVarNamesUsedInCondInsn
	   											(semanticDiffOld.globalTrackCond);
	   //final mapping between hte variables

	   semanticDiffOld.generateFinalMapBetweenVarsAndCond(varNames);

	   semanticDiffOld.globalTrackCond.removeAll(oldCFG.getRemovedInstructions());
	   semanticDiffOld.globalTrackWrite.removeAll(oldCFG.getRemovedInstructions());

	   //System.out.println("final old (remove - affected) globalTrackCond :" + semanticDiffOld.globalTrackCond);
	   //System.out.println("final old (remove - affected) globalTrackWrite :" + semanticDiffOld.globalTrackWrite);
	   return semanticDiffOld;
   }

   protected void preciseAnalysis(String methodName, CFG cfg, MethodGen mg, boolean filterAffMode) {
    	//System.out.println("methodName is :" + methodName);
    	AbstractMethodInfo absMethodInfo = new AbstractMethodInfo(mg);
    	absMethodInfo.runAnalysis(false);
    	//System.out.println("coming to the precise analysis");
    	AnalyzeIntraProceduralDiff semanticDiff
    	 				= new AnalyzeIntraProceduralDiff(absMethodInfo, cfg, mg);
    	initializeDataStructures(semanticDiff);
    	boolean genTransitiveControlDepStmts = true;
    	boolean genFinalWriteStmts = false;
    	computeSemanticDiff(cfg, semanticDiff, cfg.getModifiedWritesAndIfs(),
    			genTransitiveControlDepStmts, genFinalWriteStmts);
    	
    	HashSet<Integer> newCondPositions = new HashSet<Integer>();
		 /*** --- rule for control dependence of affected write statements **/
    	newCondPositions.addAll(semanticDiff.markOtherConditionalBranches(null, false));
    	semanticDiff.updateToBeDroppedSet(newCondPositions, semanticDiff.canbeDropped,
    			semanticDiff.globalTrackCond);
    	semanticDiff.globalTrackCond.addAll(newCondPositions);
		
    	if(filterAffMode){
    		filterAff(cfg, semanticDiff);
    	}
    	
    	if (verbose)
    		System.out.println("end of the precise analysis");
    	analysis.put(methodName, semanticDiff);
    	//System.exit(1);

    }

   	
   	// filter affected locations which are not reachable from or can't reach changes
    protected void filterAff(CFG cfg, AnalyzeIntraProceduralDiff semanticDiff){
    	Set<Integer> modifiedWritesAndIfs = cfg.getModifiedWritesAndIfs();
    	Set<Integer> affectedCond = semanticDiff.getGlobalTrackCondition();
    	Set<Integer> affectedWrite = semanticDiff.getGlobalTrackWrite();
    	Set<Integer> notOnAPath = new HashSet<Integer>();
    	for(Integer i: affectedCond){
    		boolean ifOnAPath = false;
    		for(Integer j: modifiedWritesAndIfs){
    			if(semanticDiff.isReachable(i, j) || semanticDiff.isReachable(j, i)){
    				ifOnAPath = true;
    				break;
    			}
    		}
    		if(!ifOnAPath) notOnAPath.add(i);
    	}
    	
    	for(Integer i: affectedWrite){
    		boolean ifOnAPath = false;
    		for(Integer j: modifiedWritesAndIfs){
    			if(semanticDiff.isReachable(i, j) || semanticDiff.isReachable(j, i)){
    				ifOnAPath = true;
    				break;
    			}
    		}
    		if(!ifOnAPath) notOnAPath.add(i);
    	}
    	
    	affectedCond.removeAll(notOnAPath);
    	affectedWrite.removeAll(notOnAPath);
    }    

   
	 protected void computeSemanticDiff(CFG cfg,
			 					AnalyzeIntraProceduralDiff semanticDiff,
			 					Set<Integer> modifiedWritesAndIfs,
			 					boolean genTransitiveCondDep,
			 					boolean genFinalWriteStmts) {
		 //System.out.println("Coming to generate control dependence info");
		 HashMap<String, ArrayList<Integer>> writeVarsMod =
			 			setupCheckingSemanticDifferences(cfg, semanticDiff,
			 						modifiedWritesAndIfs, genTransitiveCondDep);
		 boolean done = false;
		 while(!done) {

			//1. check whether there exists conditional branch statements that use the variables
			// in the writeVarsMod,
			HashMap<String, ArrayList<Integer>> writeVars = semanticDiff.
	 								genWriteInsCDOnBranches(semanticDiff.getTrackedBranches(),
	 										genTransitiveCondDep);

			writeVarsMod.clear();
			semanticDiff.genWriteInsUsingModifiedWriteVals
										(extractWriteLocations(writeVars),writeVarsMod);
			writeVars.putAll(writeVarsMod);

		    //2. check whether there exists a path in the CFG from the location where the variable
		    //being written to conditional branch position where the variable is used

			Set<Integer> newCondPositions = semanticDiff.getCondBranchesWithVars(writeVars);
			newCondPositions.addAll(extractWriteLocations(writeVarsMod));


			semanticDiff.clearTrackedCondAndUpdateGlobal();


			//3. Get all the conditional branches that satisfy 2 and any conditional branch locations
			// that are control dependent on ones that satisfy 2.
			semanticDiff.generateSetOfAffectCondBranches(newCondPositions, genTransitiveCondDep);

			//System.out.println("generateSetOfAffectCondBranches only modifies trackCond");
			
			if (verbose)
				System.out.println("newCondPosns1: " + newCondPositions.toString());

			newCondPositions.addAll(semanticDiff.getTrackedBranches());
			newCondPositions.removeAll(extractWriteLocations(writeVarsMod));
			
			if (verbose)
				System.out.println("newCondPosns1: " + newCondPositions.toString());
			
			//System.out.println("After removeAll writeVarsMod from newCondPositions");
			//System.out.println("newCondPositions: " + newCondPositions.toString());

			/** uncomment this if you want to add this affected write node **/
			/** it will only change the number "affected write node numbers"
			 * in DiSE we don't use it **/
			/**semanticDiff.globalTrackWrite.addAll(extractWriteLocations(writeVarsMod));**/

			//TODO: this is not pretty at all, change to something a bit more elegant
			if(genFinalWriteStmts) {
				semanticDiff.globalTrackWrite.addAll(extractWriteLocations(writeVarsMod));
			}


			// 4. If the conditional branches that are generated already are part of the "trackedBranhces"
			// we have reached a fixed point.
			// Otherwise continue the loop with the trackBranches \ generatedConditionalBranches

			 semanticDiff.checkGlobalTrackCond(newCondPositions);
			 done = (newCondPositions.size() == 0);

		 }

		 //5. Generate the set of store instructions that affect the generated write instruction.
		 //The newly generated instructions should not have (I think need to be sure about it)
		 // any affect on any of conditional branches.

		 ArrayList<String> varNames = semanticDiff.getRelevantVarNamesUsedInCondInsn
		 													(semanticDiff.globalTrackCond);


		 //any variables that are used in the conditional branches, including
		 // the one in modified write statements should be encompassed here

		 //6. Check the use of variables in return statements. If the variable in the
		 //return statement ?

		 //final mapping between hte variables

		 semanticDiff.generateFinalMapBetweenVarsAndCond(varNames);

	 }
	 
}