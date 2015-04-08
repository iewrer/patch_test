package gov.nasa.jpf.regression.analysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

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

		 Set<Integer> writePositions = semanticDiff.checkModifiedWriteStatement
		 				(modifiedWritesAndIfs, new HashSet<Integer>());


		 HashMap<String, ArrayList<Integer>> writeVarsMod =
			 					new HashMap<String, ArrayList<Integer>>();
		 //生成用到这些变量的对应写语句map，即writeVarsMod
		 semanticDiff.genWriteInsUsingModifiedWriteVals(writePositions,
		 												  writeVarsMod);

		 //获取到使用了modified write中变量的写语句可达的cond语句位置
		 Set<Integer> additionalPos = semanticDiff.getCondBranchesWithVars(writeVarsMod);
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