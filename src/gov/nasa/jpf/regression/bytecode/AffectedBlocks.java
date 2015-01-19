package gov.nasa.jpf.regression.bytecode;

import gov.nasa.jpf.regression.analysis.AnalyzeIntraProceduralDiff;
import gov.nasa.jpf.regression.analysis.ComputeIntraProceduralDiff;
import gov.nasa.jpf.regression.analysis.DistanceMatrix;
import gov.nasa.jpf.regression.cfg.CFG;
import gov.nasa.jpf.regression.listener.PruningRSEListener;
import gov.nasa.jpf.regression.tasks.ClientTaskConfig;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class AffectedBlocks  {

	static Set<Integer> affectedCond = new HashSet<Integer>();
	static Set<Integer> reachedCond = new HashSet<Integer>();
	static Set<Integer> affectedWrite = new HashSet<Integer>();
	static Set<Integer> reachedWrite = new HashSet<Integer>();
	static HashMap<Integer, ArrayList<Integer>> branchToDependentWriteInsMap;
	static HashMap<Integer, ArrayList<Integer>> branchToDependentSecondaryInsMap;
	static String mName = "";

	public static void setAffectedBlock(String methodName) {
		AnalyzeIntraProceduralDiff semanticDiff = ComputeIntraProceduralDiff.
									getSemanticAnalysis(methodName);
		branchToDependentWriteInsMap = semanticDiff.branchToDependentWriteInsMap;
		branchToDependentSecondaryInsMap = semanticDiff.branchToDependentSecondaryInsMap;
		affectedCond.addAll(semanticDiff.getGlobalTrackCondition());
		affectedWrite.addAll(semanticDiff.getGlobalTrackWrite());
		mName = methodName;
	}

	public static int getShortestPathInfoCFG(String methodName, Integer pos,
			Integer ifPos) {
		if(!methodName.equals(mName)) return -1; //nothing to do
		if(affectedCond.contains(ifPos) || reachedCond.contains(ifPos)) return -1; //no need to do


		//check reachability to affected cond position
		AnalyzeIntraProceduralDiff semanticDiff = ComputeIntraProceduralDiff.
										getSemanticAnalysis(methodName);
		CFG cfg = semanticDiff.getCFG();
		DistanceMatrix dm = cfg.getDistanceMatrix();


		int posStartOffset = cfg.getStartOffset(pos);
		int posIndex = cfg.getIndexOfPosition(posStartOffset);

		Set<Integer> allVals = new HashSet<Integer>();
		allVals.addAll(affectedCond);
		allVals.addAll(affectedWrite);

		Iterator<Integer> valItr = allVals.iterator();
		int distance = Integer.MAX_VALUE;
		while(valItr.hasNext()) {
			Integer affPos = valItr.next();
			int affStartOffset = cfg.getStartOffset(affPos);
			int affIndex = cfg.getIndexOfPosition(affStartOffset);
			distance = Math.min(dm.getValue(posIndex, affIndex), distance);
		}
		return distance;
	}

	public static int getShortestPathInfoBytecode(String methodName, Integer pos,
			Integer ifPos) {
		if(!methodName.equals(mName)) return -1; //nothing to do
		if(affectedCond.contains(ifPos) || reachedCond.contains(ifPos)) return -1; //no need to do

		Set<Integer> allVals = new HashSet<Integer>();
		allVals.addAll(affectedCond);
		allVals.addAll(affectedWrite);

		Iterator<Integer> valItr = allVals.iterator();
		int distance = Integer.MAX_VALUE;
		while(valItr.hasNext()) {
			Integer affPos = valItr.next();
			distance = Math.min(Math.abs(affPos-pos), distance);
		}
		return distance;
	}
	
	// this is similar to the UpdateExploredSet method in the pseudo-code
	public static void checkPos(String methodName, Integer pos) {

		if(!methodName.equals(mName)) return;

		if(affectedCond.contains(pos))  {
			affectedCond.remove(pos);
			reachedCond.add(pos);
		}

		if(affectedWrite.contains(pos)) {
			affectedWrite.remove(pos);
			reachedWrite.add(pos);
		}
	}
	
	public static boolean impactedMethod(String methodName) {
		return methodName.equals(mName);
	}

	public static void checkCond(String methodName, Integer pos) {
		if(!methodName.equals(mName)) return;

		if(affectedCond.contains(pos))  {
			affectedCond.remove(pos);
			reachedCond.add(pos);
		}
	}

	public static boolean checkCondAffected(String methodName, Integer pos) {
		if(!methodName.equals(mName)) return false;
		
		if(affectedCond.contains(pos) || reachedCond.contains(pos))  {
			return true;
		}
		return false;
	}
	
	private static boolean isControlDependent(Integer pos, Integer dep, boolean isCond){
		if(isCond){
			if(!branchToDependentSecondaryInsMap.containsKey(pos)) return false;
			ArrayList<Integer> controllingBranches = branchToDependentSecondaryInsMap.get(pos);
			if(controllingBranches.contains(dep)) return true;
		}else{
			if(!branchToDependentWriteInsMap.containsKey(pos)) return false;
			ArrayList<Integer> writePos = branchToDependentWriteInsMap.get(pos);
			if(writePos.contains(dep)) return true;
		}
		return false;
	}
	
	private static boolean isReachableForImBrCoverage(CFG cfg, 
			DistanceMatrix dm, int pos, int posIndex, boolean post_dominance_check) {
		
		if(affectedCond.contains(pos)) return true;
		
		if(affectedCond.contains(pos) || 
								reachedCond.contains(pos)) {
		
//			boolean trueBranchTaken = 
//						PruningRSEListener.trueBranches.contains(pos);
//			boolean falseBranchTaken =
//						PruningRSEListener.falseBranhces.contains(pos);	
//			if(!trueBranchTaken || !falseBranchTaken)
				return true;
			
		}
		return AffectedBlocks.
				isReachableToAffectedCond(cfg, dm, pos, posIndex, post_dominance_check);
	}
	
	private static boolean isReachableFromImPathCoverage(CFG cfg,
			DistanceMatrix dm, int pos, int posIndex, boolean post_dominance_check) {
		if(affectedCond.contains(pos) || reachedCond.contains(pos)) return true;
		return AffectedBlocks.isReachableToAffectedCond(cfg, dm, pos, posIndex, post_dominance_check);
	}
	
	private static boolean isReachableFromBBCoverage(CFG cfg,
			DistanceMatrix dm, int pos, int posIndex, boolean post_dominance_check) {
		return (AffectedBlocks.isReachableForImBrCoverage(cfg, dm, pos, posIndex, post_dominance_check)) ||
				AffectedBlocks.isReachableToAffectedWrite(cfg, dm, posIndex);
	}

	public static boolean isReachable(String methodName, Integer pos, boolean post_dominance_check) {
		if(!methodName.equals(mName)) return true;


		//check reachability to affected cond position
		AnalyzeIntraProceduralDiff semanticDiff = ComputeIntraProceduralDiff.
										getSemanticAnalysis(methodName);
		CFG cfg = semanticDiff.getCFG();
		DistanceMatrix dm = cfg.getDistanceMatrix();


		int posStartOffset = cfg.getStartOffset(pos);
		int posIndex = cfg.getIndexOfPosition(posStartOffset);
		
		
		
		if(ClientTaskConfig.isImpactedBranchCoverage()) {
			return AffectedBlocks.isReachableForImBrCoverage(cfg, dm, pos,
																	posIndex, post_dominance_check);
		}
		
		if(ClientTaskConfig.isImpactedBasicBlockCoverage()) {
			return AffectedBlocks.isReachableFromBBCoverage(cfg, dm, pos,
																posIndex, post_dominance_check);
		}
		
		if(ClientTaskConfig.isImpactedPathFragCoverage()) {
			return AffectedBlocks.isReachableFromImPathCoverage(cfg, dm,
															pos, posIndex, post_dominance_check);
		}

		//if it is affected position we always want to generate both
		// choices.
		if(affectedCond.contains(pos) || reachedCond.contains(pos))
			return true;
				
				
		// loop always return true;
		// TODO: handle this more intelligently?
		if(ComputeIntraProceduralDiff.isPartOfLoop(pos, methodName))
			return true;


		//are the affected positions reachable from the pos?

		//System.out.println("affectedCond is" + affectedCond.toString());
		//System.out.println("affectedWrite is" + affectedWrite.toString());

		//System.out.println("reachedCond is" + reachedCond.toString());
		//System.out.println("reachedWrite is" + reachedWrite.toString());

		if(AffectedBlocks.isReachableToAffectedCond(cfg, dm, pos, posIndex, post_dominance_check))
			return true;
		
		// are the affected write positions reachable from the pos?
		boolean found = false;
		Set<Integer> tmpAffectedWrite = new HashSet<Integer>();
		tmpAffectedWrite.addAll(affectedWrite);
		Iterator<Integer> writeItr = tmpAffectedWrite.iterator();
		while(writeItr.hasNext()) {
			Integer writePos = writeItr.next();
			if(post_dominance_check){// check post dominance
				if (isControlDependent(pos, writePos, false)) continue;
			}
			
			int writeStartOffset = cfg.getStartOffset(writePos);
			int writeIndex = cfg.getIndexOfPosition(writeStartOffset);

			if(dm.getValue(posIndex, writeIndex) <
					Integer.MAX_VALUE) {
				checkReachability(writeIndex, cfg, dm);
				found = true;
			}
		}

		return found;
	}
	
	private static boolean isReachableToAffectedCond
							(CFG cfg, DistanceMatrix dm, int pos, int posIndex, boolean post_dominance_check) {
		Iterator<Integer> condItr = affectedCond.iterator();
		while(condItr.hasNext()) {
			Integer condPos = condItr.next();
			if(post_dominance_check){// check post dominance
				if (isControlDependent(pos, condPos, true)) continue;
			}
			
			int condStartOffset = cfg.getStartOffset(condPos);
			int condIndex = cfg.getIndexOfPosition(condStartOffset);

			if(dm.getValue(posIndex, condIndex) <
					Integer.MAX_VALUE) return true;
		}
		return false;
	}
	
	private static boolean isReachableToAffectedWrite
				(CFG cfg, DistanceMatrix dm, int posIndex) {
		Iterator<Integer> wrtItr = affectedWrite.iterator();
		while(wrtItr.hasNext()) {
			Integer wPos = wrtItr.next();
			int writeStartOffset = cfg.getStartOffset(wPos);
			int wrtIndex = cfg.getIndexOfPosition(writeStartOffset);

			if(dm.getValue(posIndex, wrtIndex) <
					Integer.MAX_VALUE) return true;
			}
		return false;
   }

	public static void resetCondPos(Integer pos,
											String methodName) {
		if(!methodName.equals(mName)) return;

		if (reachedCond.contains(pos))  {
			affectedCond.add(pos);
			reachedCond.remove(pos);
		}
	}

	private static void checkReachability(int writeIndex, CFG cfg,
												DistanceMatrix dm) {
		Iterator<Integer> condItr = reachedCond.iterator();
		Set<Integer> remove = new HashSet<Integer>();
		while(condItr.hasNext()) {
			Integer condPos = condItr.next();
			int condStartOffset = cfg.getStartOffset(condPos);
			int condIndex = cfg.getIndexOfPosition(condStartOffset);

			if(dm.getValue(writeIndex, condIndex) <
					Integer.MAX_VALUE)  {
				remove.add(condPos);
				affectedCond.add(condPos); // reset
			}
		}
		reachedCond.removeAll(remove);

		Iterator<Integer> writeItr = reachedWrite.iterator();
		Set<Integer> wRemove = new HashSet<Integer>();
		while(writeItr.hasNext()) {
			Integer wPos = writeItr.next();
			int writeStartOffset = cfg.getStartOffset(wPos);
			int wIndex = cfg.getIndexOfPosition(writeStartOffset);

			if(dm.getValue(writeIndex, wIndex) <
					Integer.MAX_VALUE) {
				wRemove.add(wPos);
				affectedWrite.add(wPos);
			}
		}
		reachedWrite.removeAll(wRemove);
	}


}