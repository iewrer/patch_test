package gov.nasa.jpf.regression.analysis;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import jpf_diff.Block;
import jpf_diff.Control;
import jpf_diff.ControlBlock;
import jpf_diff.Data;
import jpf_diff.DataBlock;
import jpf_diff.Dependency;
import jpf_diff.ErrorCount;

import org.apache.bcel.classfile.LineNumberTable;
import org.apache.bcel.generic.MethodGen;

import edu.byu.cs.trace.AbstractMethodInfo;
import gov.nasa.jpf.regression.ast.BlockASTInfo;
import gov.nasa.jpf.regression.ast.MethodASTInfo;
import gov.nasa.jpf.regression.cfg.ByteSourceHandler;
import gov.nasa.jpf.regression.cfg.CFG;
import gov.nasa.jpf.regression.cfg.CFGBuilder;
import gov.nasa.jpf.regression.analysis.SCC.Loop;
import gov.nasa.jpf.util.Pair;

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
     * @param error 
     * @param String classNameNew
     * @throws Exception
     */
    public ComputeIntraProceduralDiff(String classNameNew,
    						String classNameOld, ErrorCount error) throws Exception {
    	ByteSourceHandler bsHandlerNew = new ByteSourceHandler();
        bsHandlerNew.readSourceFile(classNameNew, error);
       	cfgbNew = new CFGBuilder();
	   	cfgbNew.parseClass(bsHandlerNew.classFile, error);

	   	ByteSourceHandler bsHandlerOld = new ByteSourceHandler();
	   	bsHandlerOld.readSourceFile(classNameOld, error);
	   	cfgbOld = new CFGBuilder();
	   	cfgbOld.parseClass(bsHandlerOld.classFile, error);
	   	
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

    public int computeChangedInfo(String methodName, MethodASTInfo methodASTInfo, boolean filterAffMode) {
    	CFG newCfg;
		CFG oldCfg;
		Map<BigInteger, Set<Block>> oldDepend = new HashMap<BigInteger, Set<Block> >();
		try {
	    	if(analysis.containsKey(methodName)) return 0;
			oldCfg = cfgbOld.getCFG(methodName,"original", methodASTInfo);

			if(oldCfg !=null && oldCfg.getRemovedInstructions().size() > 0) {
				AnalyzeIntraProceduralDiff diff =
					checkOldInformation(methodName, oldCfg, cfgbOld.getMethodGen(methodName));

				oldDepend = mapRemovedNode(methodASTInfo, cfgbOld.getMethodGen(methodName), diff);
			}

			newCfg = cfgbNew.getCFG(methodName, "modified", methodASTInfo);
			if (oldCfg !=null) {
				cfgbNew.addOldDepend(oldDepend, newCfg, methodASTInfo, cfgbOld.getMethodGen(methodName));
			}
			//System.out.println(newCfg.modifiedWritesAndIfs.toString());
			if (newCfg != null) {
				preciseAnalysis(methodName, newCfg, cfgbNew.getMethodGen(methodName), filterAffMode);
			}
			


		} catch (Exception e) {
			System.err.println("exception raised in precise analysis");
			e.printStackTrace();
//			System.exit(1);
			return -1;
		}
		return 0;
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
    protected Map<BigInteger, Set<Block>> mapRemovedNode(MethodASTInfo methodASTInfo, MethodGen mg,
    		AnalyzeIntraProceduralDiff oldSemantic) {
    	Set<BigInteger> matched =  new HashSet<BigInteger>();
    	Map<BigInteger, BlockASTInfo> orig = methodASTInfo.getOriginalBlocks();
    	LineNumberTable lnt = mg.getLineNumberTable(mg.getConstantPool());
    	Set<Integer> global = oldSemantic.globalTrackCond;
    	global.addAll(oldSemantic.globalTrackWrite);
    	System.err.println("oldsemantic: " + global.size());
    	
    	Map<Integer, BigInteger> origPosToModBlock = new HashMap<>();
    	
    	//先获得original block，对每一个block找到他的match block，如果包括globalTrackCond、globalTrackWrite，则记录之
    	Iterator<BigInteger> itrOrg = orig.keySet().iterator();
    	while(itrOrg.hasNext()) {
    		BigInteger bi = itrOrg.next();
    		BlockASTInfo bo = orig.get(bi);
    		Iterator<Integer> gItr = global.iterator();
    		while(gItr.hasNext()) {
    			Integer gPos = gItr.next();
    			int lineNum = lnt.getSourceLine(gPos);
    			if(bo.getStartLine() <= lineNum &&
    					bo.getEndLine() >= lineNum) {
    				System.err.println("old tracked: " + lineNum);
    				matched.add(bo.getMatchedBlock());    				
    				if (!origPosToModBlock.containsKey(gPos)) {
						origPosToModBlock.put(gPos, bo.getMatchedBlock());
					}
    				break;
    			}
//    			else {
//    				System.err.println("old tracked not mapped: " + lineNum);
//    			}
    		}
    	}
    	//遍历matched block，将match上的modified block添加到methodASTInfo的changedModifiedLines里面
    	Map<BigInteger, BlockASTInfo> modified = methodASTInfo.
    	getModifiedBlocks();
    	Iterator<BigInteger> matchItr = matched.iterator();
    	while(matchItr.hasNext()) {
    		BigInteger mIndex = matchItr.next();
    		BlockASTInfo mod = modified.get(mIndex);
    		if (mod == null) {
				continue;
			}
    		int startLine = mod.getStartLine();
    		int endLine = mod.getEndLine();
    		for(int lnIndex = startLine; lnIndex <= endLine; lnIndex++) {
    			System.err.println("old tracked map to : " + lnIndex);
    			methodASTInfo.addChangedMod(lnIndex);
    		}
    	}
		Iterator<Integer> iterator = oldSemantic.depend.keySet().iterator();
		Map<BigInteger, Set<Block> > depend = new HashMap<>();
		System.err.println("old dependengcy begin----");
		while (iterator.hasNext()) {
			Integer pos = (Integer) iterator.next();
			Set<Dependency> dependencies = oldSemantic.depend.get(pos);
			for (Dependency dependency : dependencies) {
				System.err.println("	" + lnt.getSourceLine(pos) + " depends on " + lnt.getSourceLine(dependency.depend._2));
				Block dependBlock;
				if (dependency instanceof Data) {
					dependBlock = new DataBlock(origPosToModBlock.get(pos), origPosToModBlock.get(dependency.depend._2));
				}
				else if (dependency instanceof Control) {
					dependBlock = new ControlBlock(origPosToModBlock.get(pos), origPosToModBlock.get(dependency.depend._2));
				}
				else {
					dependBlock = new Block(origPosToModBlock.get(pos), origPosToModBlock.get(dependency.depend._2));
				}
				if (!depend.containsKey(origPosToModBlock.get(pos))) {
					Set<Block> newDependencies = new HashSet<>();
					newDependencies.add(dependBlock);
					depend.put(origPosToModBlock.get(pos), newDependencies);
				}
				else {
					depend.get(origPosToModBlock.get(pos)).add(dependBlock);
				}				
			}
		}
		System.err.println("old dependengcy end----");
    	return depend;
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

	   for (Integer integer : oldCFG.getRemovedInstructions()) {
		   Set<Dependency> dependencies = new HashSet<>();
		   dependencies.add(new Dependency(integer, -1));
		   semanticDiffOld.depend.put(integer, dependencies);
	   }
	   
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
			 											newCondPositions, true, "check write");

	   semanticDiffOld.clearTrackedCondAndUpdateGlobal();



	   //System.out.println("writeVars :"  + writeVars.toString());

	   ArrayList<String> varNames = semanticDiffOld.getRelevantVarNamesUsedInCondInsn
	   											(semanticDiffOld.globalTrackCond);
	   //final mapping between hte variables

	   semanticDiffOld.generateFinalMapBetweenVarsAndCond(varNames);
	   
	   
	   
	   
	   LineNumberTable lnt = oldMg.getLineNumberTable(oldMg.getConstantPool());

	   System.err.println("semanticDiffOld.globalTrackCond: "+ semanticDiffOld.globalTrackCond.size());
	   for (Integer cond : semanticDiffOld.globalTrackCond) {
		   Integer line = lnt.getSourceLine(cond);
		   System.err.println("	" + line);
	   }
	   
	   System.err.println("semanticDiffOld.globalTrackWrite: "+ semanticDiffOld.globalTrackWrite.size());
	   for (Integer cond : semanticDiffOld.globalTrackWrite) {
		   Integer line = lnt.getSourceLine(cond);
		   System.err.println("	" + line);
	   }
	   
	   
	   semanticDiffOld.globalTrackCond.removeAll(oldCFG.getRemovedInstructions());

	   semanticDiffOld.globalTrackWrite.removeAll(oldCFG.getRemovedInstructions());

	   //System.out.println("final old (remove - affected) globalTrackCond :" + semanticDiffOld.globalTrackCond);
	   //System.out.println("final old (remove - affected) globalTrackWrite :" + semanticDiffOld.globalTrackWrite);
	   return semanticDiffOld;
   }

   protected void preciseAnalysis(String methodName, CFG cfg, MethodGen mg, boolean filterAffMode) {
	   	if (methodName.contains("org.eclipse.jdt.internal.compiler.Compiler.compile")) {
			System.err.println(methodName);
		}
    	//System.out.println("methodName is :" + methodName);
    	AbstractMethodInfo absMethodInfo = new AbstractMethodInfo(mg);
    	absMethodInfo.runAnalysis(false);
    	//System.out.println("coming to the precise analysis");
    	AnalyzeIntraProceduralDiff semanticDiff
    	 				= new AnalyzeIntraProceduralDiff(absMethodInfo, cfg, mg);
    	initializeDataStructures(semanticDiff);
    	boolean genTransitiveControlDepStmts = true;
    	boolean genFinalWriteStmts = false;
    	
    	System.err.println("----modified instructions in modified block begin----");
    	for (Integer integer : cfg.getModifiedWritesAndIfs()) {
			System.err.println("modified: " + mg.getLineNumberTable(mg.getConstantPool()).getSourceLine(integer));
		}
    	System.err.println("----modified instructions in modified block end----");
    	
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
    	
//    	LineNumberTable lnt = mg.getLineNumberTable(mg.getConstantPool());
//    	System.err.println(lnt.getSourceLine(43) + " depends on " + lnt.getSourceLine(25));

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
		 /*
		  * 0.先用变更语句集合计算一遍，获取到受影响的cond语句集合并存入track cond,并返回用到这些变量的写语句map
		  * 	->setupCheckingSemanticDifferences
		  * 
		  * 接下来进入循环：
		  * 1.找到用到受影响的cond语句中变量s的那些写语句
		  * 	-->genWriteInsCDOnBranches
		  * 2.再找用到这些变量s的写语句
		  * 	->genWriteInsUsingModifiedWriteVals
		  * 3.再找用到这些变量的cond语句，并检查这些写语句和cond语句之间是否可达，并返回可达的cond语句,将可达的写语句放入global track write
		  * 	->getCondBranchesWithVars
		  * 4.再找控制依赖于1中找到的写语句和2中找到的cond语句的那些cond语句，并放入到track cond
		  * 	->generateSetOfAffectCondBranches
		  * 5.若没有找到更多的语句了，则结束循环，否则继续
		  * 总结：
		  * 	--->最终效果即找到那些被变更写语句影响到的cond语句，并放入临时的impact set
		  * 	--->并将变更写语句放入impact set
		  * 	--->并将用到这些变量的，且到被变更写语句影响到的cond语句可达的那些写语句放入impact set
		  */
		 if (cfg.getMethodName().contains("Compile.compile")) {
			int x = 0;
		}
		 while(!done) {

			//1. check whether there exists conditional branch statements that use the variables
			// in the writeVarsMod,
			HashMap<String, ArrayList<Integer>> writeVars = semanticDiff.
	 								genWriteInsCDOnBranches(semanticDiff.getTrackedBranches(),
	 										genTransitiveCondDep);

			writeVarsMod.clear();
			
			
			 /*
			  * 先找用到这些变量的写语句：genWriteInsUsingModifiedWriteVals
			  * 再找用到这些变量的cond语句，并检查这些写语句和cond语句之间是否可达，并返回可达的cond语句：getCondBranchesWithVars
			  */
			
			//将Modified的write语句用到的变量名映射到对应的Instructions列表上，该map即为writeVarsMod
			semanticDiff.genWriteInsUsingModifiedWriteVals
										(extractWriteLocations(writeVars),writeVarsMod);
			writeVars.putAll(writeVarsMod);

		    //2. check whether there exists a path in the CFG from the location where the variable
		    //being written to conditional branch position where the variable is used

			//相当于通过check reachable应用了规则3，且部分应用了规则4?
			Set<Integer> newCondPositions = semanticDiff.getCondBranchesWithVars(writeVars, "check write");
			//将这些Modified的write语句用到的变量的具体位置做成一个Set，并加入进来
			newCondPositions.addAll(extractWriteLocations(writeVarsMod));


			semanticDiff.clearTrackedCondAndUpdateGlobal();


			//3. Get all the conditional branches that satisfy 2 and any conditional branch locations
			// that are control dependent on ones that satisfy 2.
			//应用了规则1
			//也相当于应用了规则2，因为之前将Modified的write语句用到的变量位置也添加到了newCondPositions中来
			semanticDiff.generateSetOfAffectCondBranches(newCondPositions, genTransitiveCondDep);

			//System.out.println("generateSetOfAffectCondBranches only modifies trackCond");
			
			if (verbose)
				System.out.println("newCondPosns1: " + newCondPositions.toString());

			//generateSetOfAffectCondBranches方法中将这些cond的位置记录到了TrackedBranches中
			newCondPositions.addAll(semanticDiff.getTrackedBranches());
			//因为这里是
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