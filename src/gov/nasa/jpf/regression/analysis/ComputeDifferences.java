package gov.nasa.jpf.regression.analysis;

import java.security.spec.MGF1ParameterSpec;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import jpf_diff.Dependency;
import gov.nasa.jpf.regression.cfg.CFG;

public class ComputeDifferences {

	static protected boolean debug = false;

	/**
	 * default constructor
	 */
	public ComputeDifferences() {}

	protected void initializeDataStructures(AnalyzeIntraProceduralDiff
												semanticDiff) {
		 // Explore all the arithmetic instructions and find the variables
		 // involved in the operation. Also explore all the store operations
		 // and detect the variables that are involved in the operation.
		 semanticDiff.analyzeArithmeticAndWriteOperations();
		 semanticDiff.analyzeConditionalBranchStatements();

	}

	protected HashMap<String, ArrayList<Integer>>
					setupCheckingSemanticDifferences(CFG cfg,
				AnalyzeIntraProceduralDiff semanticDiff,
				Set<Integer> modifiedWritesAndIfs,
				boolean genTransitiveControlDep) {


         //	check the conditional branches that are affected by modified write contained
		 // in the set of modifiedWritesAndIfs
		//	即应用了规则3

		Iterator<Integer> modItr = modifiedWritesAndIfs.iterator();
		while(modItr.hasNext()) {
			Integer pos = modItr.next();
			//添加初始追踪依赖关系，即Modified语句不依赖于任何语句
			if (!semanticDiff.depend.containsKey(pos)) {
				Dependency dependency = new Dependency(pos, -1);
				Set<Dependency> dependencies = new HashSet<>();
				dependencies.add(dependency);
				semanticDiff.depend.put(pos, dependencies);
			}
		}
		 /*
		  * 0.抽取出变更语句集合中的写语句并返回之，同时找到用到这些写语句中出现过变量的所有写语句w，若有依赖于w的cond语句，放入到track cond中
		  * 	->checkModifiedWriteStatement
		  * 1.再找用到这些变量的写语句
		  * 	->genWriteInsUsingModifiedWriteVals
		  * 2.再找用到这些变量的cond语句，并检查这些写语句和cond语句之间是否可达，并返回可达的cond语句,将可达的写语句放入global track write
		  * 	->getCondBranchesWithVars
		  * 3.再找控制依赖于1中找到的写语句和2中找到的cond语句的那些cond语句，并放入到track cond
		  * 	->generateSetOfAffectCondBranches
		  * 总结：
		  * 	--->最终效果即找到那些被变更写语句影响到的cond语句，并放入临时的impact set
		  * 	--->并将变更写语句放入impact set
		  * 	--->并将用到这些变量的，且到被变更写语句影响到的cond语句可达的那些写语句放入impact set
		  */
		 Set<Integer> writePositions = semanticDiff.checkModifiedWriteStatement
		 				(modifiedWritesAndIfs, new HashSet<Integer>());
		 
		 HashMap<String, ArrayList<Integer>> writeVarsMod =
			 					new HashMap<String, ArrayList<Integer>>();
		 //生成用到这些变量的对应写语句map，即writeVarsMod
		 semanticDiff.genWriteInsUsingModifiedWriteVals(writePositions,
		 												  writeVarsMod);

		 //获取到使用了modified write中变量的写语句可达的cond语句位置
		 Set<Integer> additionalPos = semanticDiff.getCondBranchesWithVars(writeVarsMod, "modified write");
		 //再把用了modified write中变量的写语句语句位置也添加进来
		 additionalPos.addAll(extractWriteLocations(writeVarsMod));


		 //check the conditional branches that are modified and anly conditional
		 // branches that are control dependent on the modified conditional branches
		 // conditional branches that use variables from the modified write statements
		 // and branches control dependent on them.

		 Set<Integer> allVals = new HashSet<Integer>();
		 allVals.addAll(modifiedWritesAndIfs);
		 allVals.addAll(additionalPos);
		 //包括了write语句中用到的其他的变量相关的写位置
		 allVals.addAll(writePositions);

		 semanticDiff.globalTrackWrite.addAll(writePositions);
		 semanticDiff.generateSetOfAffectCondBranches
		 					(allVals, genTransitiveControlDep);
		 return writeVarsMod;
	}

	/**
	 * extractWriteLocations - the given HashMap maps variable names
	 * to sets of bytecode positions where those variables are found
	 * and this method extracts all the positions out from all the
	 * ArrayLists in the HashMap and collects them all in one set of
	 * Integers which gets returned.
	 *
	 * @param writeVars
	 * @return Set<Integer>
	 */
	protected Set<Integer> extractWriteLocations(HashMap<String,
			ArrayList<Integer>> writeVars) {
		Set<Integer> varLocations = new HashSet<Integer>();
		Iterator<String> varItr = writeVars.keySet().iterator();
		while(varItr.hasNext()) {
			String varId = varItr.next();
			varLocations.addAll(writeVars.get(varId));
		}
		return varLocations;
	}

	/**
	 * extractVariableNames - this method acts similarly to the above
	 * method (extractWriteLocations), but rather than extracting the
	 * positions into a single set, it is extracting all the variable
	 * names into an ArrayList which is returned.
	 *
	 * @param writeVars
	 * @return ArrayList<String>
	 */
	protected ArrayList<String> extractVariableNames (HashMap<String,
			ArrayList<Integer>> writeVars) {
		ArrayList<String> vars = new ArrayList<String>();
		Iterator<String> varItr = writeVars.keySet().iterator();
		while(varItr.hasNext()) {
			String varId = varItr.next();
			vars.add(varId);
		}
		return vars;
	}
}