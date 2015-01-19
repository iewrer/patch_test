package gov.nasa.jpf.regression.bytecode;

import gov.nasa.jpf.regression.analysis.AffectedNodes;
import gov.nasa.jpf.regression.analysis.AnalyzeInterProceduralDiff;
import gov.nasa.jpf.regression.analysis.ComputeInterProceduralDiff;
import gov.nasa.jpf.regression.analysis.ComputeIntraProceduralDiff;
import gov.nasa.jpf.regression.analysis.DistanceMatrix;
import gov.nasa.jpf.regression.cfg.CFG;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class IPAffectedBlocks {

	// TODO: Figure out how to best utilize the following Maps.
	// Unexplored ACN, AWN, ASN, ARN for each method
	static Map<String, Set<Integer>> affectedCondMap =
		new HashMap<String, Set<Integer>>();
	static Map<String, Set<Integer>> affectedWriteMap =
		new HashMap<String, Set<Integer>>();
	static Map<String, Set<Integer>> affectedInvocMap =
		new HashMap<String, Set<Integer>>();
	static Map<String, Set<Integer>> affectedReturnMap =
		new HashMap<String, Set<Integer>>();

	//Need to store the affected formal parameters of the method
	static Map<String, ArrayList<String>> affectedFormalParams =
		new HashMap<String, ArrayList<String>>();
	
	
	static Map<String, ArrayList<String>> affectedGlobalVariables =
			new HashMap<String, ArrayList<String>>();
	
	//the locations at which constraints generated can be
	//dropped before performing equivalence checking
	static Map<String, Set<Integer>> tobeDropped =
		new HashMap<String, Set<Integer>>();

	// Explored ACN, AWN, ASN, ARN for each method
	static Map<String, Set<Integer>> reachedCondMap =
		new HashMap<String, Set<Integer>>();
	static Map<String, Set<Integer>> reachedWriteMap =
		new HashMap<String, Set<Integer>>();
	static Map<String, Set<Integer>> reachedInvocMap =
		new HashMap<String, Set<Integer>>();
	static Map<String, Set<Integer>> reachedReturnMap =
		new HashMap<String, Set<Integer>>();

	// A mapping of method names to unique method names because there
	// can be multiple invocations of a method that need to be
	// differentiated between.
	// TODO: Consider changing this to a stack to make it easier to use.
	private static Map<String, ArrayList<String>> nameToUniqueMap =
		new HashMap<String, ArrayList<String>>();

	// not sure how this will play into the IP analysis here
	static String mName = "";

	public static boolean isFunctional(Integer pos, String methodName) {
		boolean isFunc = false;

		// if tte method is not even in the map then it cannot be an
		// affected conditional node
		if(!nameToUniqueMap.containsKey(methodName)) return isFunc;


		String uniqueName = nameToUniqueMap.get(methodName)
			.get(nameToUniqueMap.get(methodName).size()-1);

		if(affectedCondMap.get(uniqueName).contains(pos) ||
				reachedCondMap.get(uniqueName).contains(pos))  {
			if(!tobeDropped.get(uniqueName).contains(pos)) {
				isFunc = true;
			}
		}

		return isFunc;
	}

	public static void resetCondPos(Integer pos, String methodName) {
		if(!nameToUniqueMap.containsKey(methodName)) return;

		String uniqueName = nameToUniqueMap.get(methodName)
					.get(nameToUniqueMap.get(methodName).size()-1);

		if (reachedCondMap.get(uniqueName).contains(pos))  {
			affectedCondMap.get(uniqueName).add(pos);
			reachedCondMap.get(uniqueName).remove(pos);
		}

	}

	// this is similar to the UpdateExploredSet method in the pseudo-code
	public static void checkPos(Integer pos, String methodName) {

		if(!nameToUniqueMap.containsKey(methodName)) return;


		String uniqueName = nameToUniqueMap.get(methodName)
			.get(nameToUniqueMap.get(methodName).size()-1);


		//if(!methodName.equals(mName)) return;

		if(affectedCondMap.get(uniqueName).contains(pos))  {
			affectedCondMap.get(uniqueName).remove(pos);
			reachedCondMap.get(uniqueName).add(pos);
		}

		if(affectedWriteMap.get(uniqueName).contains(pos)) {
			affectedWriteMap.get(uniqueName).remove(pos);
			reachedWriteMap.get(uniqueName).add(pos);
		}

		if(affectedInvocMap.get(uniqueName).contains(pos)) {
			affectedInvocMap.get(uniqueName).remove(pos);
			reachedInvocMap.get(uniqueName).add(pos);
		}

		if(affectedReturnMap.get(uniqueName).contains(pos)) {
			affectedReturnMap.get(uniqueName).remove(pos);
			reachedReturnMap.get(uniqueName).add(pos);
		}
	} // end of checkPos

	/**
	 * addMethodToSets - gets a unique method name for the given
	 * method name and then sets up the affected and reached sets for
	 * that method in the affected and reached maps.
	 * @param methodName
	 * @param affNodes
	 */
	public static void addMethodToSets(String methodName,
			AffectedNodes affNodes, ArrayList<String> affectedFormalParams,
			ArrayList<String> affectedGlobalVariables) {


		// generate a unique method name for the given method name
		String uniqueName = genUniqueMethodName(methodName);
		System.out.println("adding method to sets :" + uniqueName);

		affectedCondMap.put(uniqueName, affNodes.getAffectedCondNodes());
		affectedWriteMap.put(uniqueName, affNodes.getAffectedWriteNodes());
		affectedInvocMap.put(uniqueName, affNodes.getAffectedCallNodes());
		affectedReturnMap.put(uniqueName, affNodes.getAffectedReturnNodes());

		// Store the affected FormalParams
		IPAffectedBlocks.affectedFormalParams.
							put(uniqueName, affectedFormalParams);
		IPAffectedBlocks.affectedGlobalVariables.put(uniqueName,
												affectedGlobalVariables);
		
		IPAffectedBlocks.tobeDropped.put(uniqueName,
										affNodes.getCanbeDropped());

		// initialize the reachedNodes
		reachedCondMap.put(uniqueName, new HashSet<Integer>());
		reachedWriteMap.put(uniqueName, new HashSet<Integer>());
		reachedInvocMap.put(uniqueName, new HashSet<Integer>());
		reachedReturnMap.put(uniqueName, new HashSet<Integer>());

	} // end of addMethodToSets

	public static ArrayList<String> getAffectedFormalVars(String methodName,
			ComputeInterProceduralDiff cipd) {
		//System.out.println(nameToUniqueMap.toString());
		if(!nameToUniqueMap.containsKey(methodName)){
			System.out.println(nameToUniqueMap.toString());
			return new ArrayList<String>();
		}

		String uniqueName = nameToUniqueMap.get(methodName)
			.get(nameToUniqueMap.get(methodName).size() - 1);

		return affectedFormalParams.get(uniqueName);

	}
	
	public static ArrayList<String> getAffectedGlobalVars(String methodName,
			ComputeInterProceduralDiff cipd) {
		//System.out.println(nameToUniqueMap.toString());
		if(!nameToUniqueMap.containsKey(methodName)){
			System.out.println(nameToUniqueMap.toString());
			return new ArrayList<String>();
		}

		String uniqueName = nameToUniqueMap.get(methodName)
			.get(nameToUniqueMap.get(methodName).size() - 1);

		return affectedGlobalVariables.get(uniqueName);

	}

	/**
	 * isReachable - determines if there is an affected node
	 * reachable from the given position. If so, then True
	 * is returned, otherwise False is returned.
	 * @param methodName
	 * @param pos
	 * @return boolean
	 */
	public static boolean isReachable(String methodName, Integer pos,
			ComputeInterProceduralDiff cipd) {

		//System.out.println("Entering isReachable with: " + methodName + " the pos is :" + pos);

		if(!nameToUniqueMap.containsKey(methodName)) return false;

		String uniqueName = nameToUniqueMap.get(methodName)
			.get(nameToUniqueMap.get(methodName).size() - 1);

		if(affectedCondMap.get(uniqueName) == null ||
				reachedCondMap.get(uniqueName) == null) {
			System.out.println("Something is null when it shouldn't be.");
		}

		// if it is an affect cond, then we always want to generate
		// both choices.
		if(affectedCondMap.get(uniqueName).contains(pos) ||
				reachedCondMap.get(uniqueName).contains(pos))
			return true;

		// loop always return true;
		// TODO: handle this more intelligently?
		if(ComputeIntraProceduralDiff.isPartOfLoop(pos, methodName))
			return true;

		// get the semantic diff, CFG, and distance matrix
		// these are used to check reachability
		AnalyzeInterProceduralDiff semanticDiff = cipd.getSemanticDiff();

		if(semanticDiff == null)
			System.out.println("SemanticDiff is null");
		else if(semanticDiff.getCFG() == null)
			System.out.println("the CFG is null");
		CFG cfg = semanticDiff.getCFG();
		DistanceMatrix dm = cfg.getDistanceMatrix();

		// get the index of the given position
		int posStartOffset = cfg.getStartOffset(pos);
		int posIndex = cfg.getIndexOfPosition(posStartOffset);

		// check if any unexplored ASN are reachable
		Set<Integer> tmpAffectedInvoc = new HashSet<Integer>();
		tmpAffectedInvoc.addAll(affectedInvocMap.get(uniqueName));
		Iterator<Integer> invocIter = tmpAffectedInvoc.iterator();
		while(invocIter.hasNext()) {
			Integer invocPos = invocIter.next();
			int invocStartOffset = cfg.getStartOffset(invocPos);
			int invocIndex = cfg.getIndexOfPosition(invocStartOffset);

			// when going back in call chain we will come the position 
			// that made the call (invocation) .. we cannot assume that 
			// this will always be in the reached invoc locations because
			// after having gone through one path in the callee method
			// we may have updated the reachablility of the invoke method 
			// neha: 01/13/2014
			if(pos != invocPos) {
				if(dm.getValue(posIndex, invocIndex) < Integer.MAX_VALUE) 
					return true;
			}
		}


		// check if any unexplored ARN are reachable
		Set<Integer> tmpAffectedReturn = new HashSet<Integer>();
		tmpAffectedReturn.addAll(affectedReturnMap.get(uniqueName));
		Iterator<Integer> returnIter = tmpAffectedReturn.iterator();
		while(returnIter.hasNext()) {
			Integer returnPos = returnIter.next();
			int returnStartOffset = cfg.getStartOffset(returnPos);
			int returnIndex = cfg.getIndexOfPosition(returnStartOffset);

			if(dm.getValue(posIndex, returnIndex) < Integer.MAX_VALUE){
				return true;
			}
		}

		boolean found = false;

		/*
		 * Intraprocedural DiSE:
		 * write -> affected cond
		 * write -> affected write
		 *
		 * Interprocedural DiSE:
		 * includes the two above scenarios
		 * write -> affected call site
		 * write -> affected return
		 * cond --> affected return
		 *
		 * Tricky Case not needed:
		 * cond --> affected call site
		 * This isn't needed because the conditional doesn't actually
		 * affect the behavior of the call site or what is passed to
		 * the call site, it is just a simple control dependence.
		 */

		// check if any unexplored ACN are reachable
		Iterator<Integer> UnExCondIter = affectedCondMap.get(uniqueName).iterator();
		while(UnExCondIter.hasNext()) {
			// get the index of the cond position
			Integer condPos = UnExCondIter.next();
			int condStartOffset = cfg.getStartOffset(condPos);
			int condIndex = cfg.getIndexOfPosition(condStartOffset);

			// check the reachability in the distance matrix
			if(dm.getValue(posIndex, condIndex) < Integer.MAX_VALUE) {
				checkCondReachability(uniqueName, condIndex, cfg, dm);
				found = true;
			}
		}

		// check if any unexplored AWN are reachable
		Set<Integer> tmpAffectedWrite = new HashSet<Integer>();
		tmpAffectedWrite.addAll(affectedWriteMap.get(uniqueName));
		Iterator<Integer> writeItr = tmpAffectedWrite.iterator();
		while(writeItr.hasNext()) {
			Integer writePos = writeItr.next();
			int writeStartOffset = cfg.getStartOffset(writePos);
			int writeIndex = cfg.getIndexOfPosition(writeStartOffset);

			if(dm.getValue(posIndex, writeIndex) <
					Integer.MAX_VALUE) {
				checkWriteReachability(uniqueName, writeIndex, cfg, dm);
				found = true;
			}
		}

		//printNonemptyCurrentSets();

		return found;
	}

	public static void checkCondReachability(String uniqueName,
			Integer index, CFG cfg, DistanceMatrix dm) {

		Iterator<Integer> returnIter = reachedReturnMap.get(uniqueName).iterator();
		Set<Integer> rRemove = new HashSet<Integer>();
		while(returnIter.hasNext()) {
			Integer rPos = returnIter.next();
			int returnStartOffset = cfg.getStartOffset(rPos);
			int rIndex = cfg.getIndexOfPosition(returnStartOffset);

			if(dm.getValue(index, rIndex) < Integer.MAX_VALUE) {
				rRemove.add(rPos);
				affectedReturnMap.get(uniqueName).add(rPos);
			}
		}
		reachedReturnMap.get(uniqueName).removeAll(rRemove);
	}

	public static void checkWriteReachability(String uniqueName,
			Integer index, CFG cfg, DistanceMatrix dm) {

		Iterator<Integer> condItr = reachedCondMap.get(uniqueName).iterator();
		Set<Integer> remove = new HashSet<Integer>();
		while(condItr.hasNext()) {
			Integer condPos = condItr.next();
			int condStartOffset = cfg.getStartOffset(condPos);
			int condIndex = cfg.getIndexOfPosition(condStartOffset);

			if(dm.getValue(index, condIndex) <
					Integer.MAX_VALUE)  {
				remove.add(condPos);
				affectedCondMap.get(uniqueName).add(condPos); // reset
			}
		}
		reachedCondMap.get(uniqueName).removeAll(remove);

		Iterator<Integer> writeItr = reachedWriteMap.get(uniqueName).iterator();
		Set<Integer> wRemove = new HashSet<Integer>();
		while(writeItr.hasNext()) {
			Integer wPos = writeItr.next();
			int writeStartOffset = cfg.getStartOffset(wPos);
			int wIndex = cfg.getIndexOfPosition(writeStartOffset);

			if(dm.getValue(index, wIndex) <
					Integer.MAX_VALUE) {
				wRemove.add(wPos);
				affectedWriteMap.get(uniqueName).add(wPos);
			}
		}
		reachedWriteMap.get(uniqueName).removeAll(wRemove);

		Iterator<Integer> invocIter = reachedInvocMap.get(uniqueName).iterator();
		Set<Integer> iRemove = new HashSet<Integer>();
		while(invocIter.hasNext()) {
			Integer iPos = invocIter.next();
			int invocStartOffset = cfg.getStartOffset(iPos);
			int iIndex = cfg.getIndexOfPosition(invocStartOffset);

			if(dm.getValue(index, iIndex) < Integer.MAX_VALUE) {
				iRemove.add(iPos);
				affectedInvocMap.get(uniqueName).add(iPos);
			}
		}
		reachedInvocMap.get(uniqueName).removeAll(iRemove);

		Iterator<Integer> returnIter = reachedReturnMap.get(uniqueName).iterator();
		Set<Integer> rRemove = new HashSet<Integer>();
		while(returnIter.hasNext()) {
			Integer rPos = returnIter.next();
			int returnStartOffset = cfg.getStartOffset(rPos);
			int rIndex = cfg.getIndexOfPosition(returnStartOffset);

			if(dm.getValue(index, rIndex) < Integer.MAX_VALUE) {
				rRemove.add(rPos);
				affectedReturnMap.get(uniqueName).add(rPos);
			}
		}
		reachedReturnMap.get(uniqueName).removeAll(rRemove);
	}

	/**
	 * genUniqueMethodName - given the name of a method, a unique
	 * method name is determined and returned based on what already
	 * exists in the NameToUniqueMap.
	 * @param mName
	 * @return String
	 */
	private static String genUniqueMethodName(String mName) {

		String uniqueName = "";
		boolean found = false;

		// check if the given method name exists
		Iterator<String> nameIter = nameToUniqueMap.keySet().iterator();
		while(nameIter.hasNext()) {
			String currName = nameIter.next();

			if(currName.equals(mName)) {
				// the name exists, generate the next uniqueName
				ArrayList<String> mNames = nameToUniqueMap.get(currName);
				uniqueName = mName + mNames.size();
				mNames.add(uniqueName);
				found = true;
				break;
			}
		}

		if(!found) {
			// the name doesn't exist, create a new hash item
			ArrayList<String> mNames = new ArrayList<String>();
			uniqueName = mName + "0";
			mNames.add(uniqueName);
			nameToUniqueMap.put(mName, mNames);
		}

		//System.out.println("Generated UniqueMethodName: " + uniqueName);

		return uniqueName;
	} // end of getUniqueMethodName

	public static void updateReachability(String methodName, Integer pos) {

		if(!nameToUniqueMap.containsKey(methodName)) return;

		String uniqueName = nameToUniqueMap.get(methodName)
						.get(nameToUniqueMap.get(methodName).size() - 1);

		if(reachedInvocMap.get(uniqueName).contains(pos)) {
			affectedInvocMap.get(uniqueName).add(pos);
			reachedInvocMap.get(uniqueName).remove(pos);
		}
	}

	public static boolean existsInMap(String methodName) {
		return (nameToUniqueMap.containsKey(methodName));
	}
	
	public static boolean unreachableLocsExist(String methodName) {
		//assert (!nameToUniqueMap.containsKey(methodName));
		
		ArrayList<String> mNames = nameToUniqueMap.get(methodName);
		int size = mNames.size();
		String uniqueName = mNames.get(size - 1);
		return allReachedCheck(uniqueName);
	}
	
	/**
	 * removeMethodSets - the given method name is used with the
	 * nameToUniqueMap to determine the most recent unique name for
	 * that method and then the affected and reached sets for that
	 * unique name are removed. The uniqueName is then subsequently
	 * removed from the nameToUniqueMap. If it is the only unique
	 * name, then that entire item is removed from the map.
	 * @param methodName
	 * @return boolean
	 */
	public static boolean removeMethodSets(String methodName) {

		assert (nameToUniqueMap.containsKey(methodName));
		ArrayList<String> mNames = nameToUniqueMap.get(methodName);
		int size = mNames.size();
		String uniqueName = mNames.get(size - 1);

		// remove the sets associated with the uniqueName
		boolean allReached = removeUniqueMethodSets(uniqueName);

		// remove the name from the map
		if(size > 1) {
			// just remove the unique name
			mNames.remove(size - 1);
		}
		else {
			// remove the entire hash item
			nameToUniqueMap.remove(methodName);
		}

		return allReached;
		//return found;
	} // end of removeMethodSets
	
	/*
	 * TODO: Refactor this so that false is returned inside each
	 * if condition, otherwise true is returned at the end.
	 */
	private static boolean allReachedCheck(String uniqueName) {
		boolean allReached = true;
		if(affectedCondMap.get(uniqueName).size() > 0) {
			//System.out.println("cond");
			allReached = false;
		}

		if(affectedWriteMap.get(uniqueName).size() > 0) {
			//System.out.println("write");
			allReached = false;
		}
		if(affectedInvocMap.get(uniqueName).size() > 0) {
			//System.out.println("invoc");
			allReached = false;
		}
		if(affectedReturnMap.get(uniqueName).size() > 0) {
			//System.out.println("return");
			allReached = false;
		}
		return allReached;
	}

	/**
	 * removeUniqueMethodSets - removes the affected and reached sets
	 * that are mapped to this unique method name.
	 * @param uniqueName
	 */
	private static boolean removeUniqueMethodSets(String uniqueName) {


		/**System.out.println(affectedCondMap.toString());
		System.out.println(affectedWriteMap.toString());
		System.out.println(affectedInvocMap.toString());
		System.out.println(affectedReturnMap.toString());**/

		boolean allReached = allReachedCheck(uniqueName);

		// TODO: This seems a bit inefficient, perhaps the affected
		// and reached sets can be combined into 2 objects similar to
		// Affected nodes so that there are only two calls to remove
		// rather than 8.
		affectedCondMap.remove(uniqueName);
		affectedWriteMap.remove(uniqueName);
		affectedInvocMap.remove(uniqueName);
		affectedReturnMap.remove(uniqueName);

		IPAffectedBlocks.affectedFormalParams.
							remove(uniqueName);

		IPAffectedBlocks.affectedGlobalVariables.
							remove(uniqueName);
		
		IPAffectedBlocks.tobeDropped.
							remove(uniqueName);

		reachedCondMap.remove(uniqueName);
		reachedWriteMap.remove(uniqueName);
		reachedInvocMap.remove(uniqueName);
		reachedReturnMap.remove(uniqueName);

		return allReached;
	} // end of removeUniqueMethodSets

	/**
	 * printCurrentSets - this method iterates over all the maps of
	 * block/location info and prints it all out in a readable form.
	 */
	public static void printCurrentSets() {
		Iterator<String> setIter;

		System.out.println("Affected Cond Sets [" + affectedCondMap.size() + "]:");
		setIter = affectedCondMap.keySet().iterator();
		while(setIter.hasNext()) {
			String uniqueName = setIter.next();
			Set<Integer> nodes = affectedCondMap.get(uniqueName);
			System.out.print("- " + uniqueName + " [");
			for(Integer node : nodes) {
				System.out.print("" + node + ",");
			}
			System.out.println("]");
		}

		System.out.println("Affected Write Sets [" + affectedWriteMap.size() + "]:");
		setIter = affectedWriteMap.keySet().iterator();
		while(setIter.hasNext()) {
			String uniqueName = setIter.next();
			Set<Integer> nodes = affectedWriteMap.get(uniqueName);
			System.out.print("- " + uniqueName + " [");
			for(Integer node : nodes) {
				System.out.print("" + node + ",");
			}
			System.out.println("]");
		}

		System.out.println("Affected Invoc Sets [" + affectedInvocMap.size() + "]:");
		setIter = affectedInvocMap.keySet().iterator();
		while(setIter.hasNext()) {
			String uniqueName = setIter.next();
			Set<Integer> nodes = affectedInvocMap.get(uniqueName);
			System.out.print("- " + uniqueName + " [");
			for(Integer node : nodes) {
				System.out.print("" + node + ",");
			}
			System.out.println("]");
		}

		System.out.println("Affected Return Sets [" + affectedReturnMap.size() + "]:");
		setIter = affectedReturnMap.keySet().iterator();
		while(setIter.hasNext()) {
			String uniqueName = setIter.next();
			Set<Integer> nodes = affectedReturnMap.get(uniqueName);
			System.out.print("- " + uniqueName + " [");
			for(Integer node : nodes) {
				System.out.print("" + node + ",");
			}
			System.out.println("]");
		}

		System.out.println("Reached Cond Sets [" + reachedCondMap.size() + "]:");
		setIter = reachedCondMap.keySet().iterator();
		while(setIter.hasNext()) {
			String uniqueName = setIter.next();
			Set<Integer> nodes = reachedCondMap.get(uniqueName);
			System.out.print("- " + uniqueName + " [");
			for(Integer node : nodes) {
				System.out.print("" + node + ",");
			}
			System.out.println("]");
		}

		System.out.println("Reached Write Sets [" + reachedWriteMap.size() + "]:");
		setIter = reachedWriteMap.keySet().iterator();
		while(setIter.hasNext()) {
			String uniqueName = setIter.next();
			Set<Integer> nodes = reachedWriteMap.get(uniqueName);
			System.out.print("- " + uniqueName + " [");
			for(Integer node : nodes) {
				System.out.print("" + node + ",");
			}
			System.out.println("]");
		}

		System.out.println("Reached Invoc Sets [" + reachedInvocMap.size() + "]:");
		setIter = reachedInvocMap.keySet().iterator();
		while(setIter.hasNext()) {
			String uniqueName = setIter.next();
			Set<Integer> nodes = reachedInvocMap.get(uniqueName);
			System.out.print("- " + uniqueName + " [");
			for(Integer node : nodes) {
				System.out.print("" + node + ",");
			}
			System.out.println("]");
		}

		System.out.println("Reached Return Sets [" + reachedReturnMap.size() + "]:");
		setIter = reachedReturnMap.keySet().iterator();
		while(setIter.hasNext()) {
			String uniqueName = setIter.next();
			Set<Integer> nodes = reachedReturnMap.get(uniqueName);
			System.out.print("- " + uniqueName + " [");
			for(Integer node : nodes) {
				System.out.print("" + node + ",");
			}
			System.out.println("]");
		}
	}

	/**
	 * printNonemptyCurrentSets - this method goes through all the
	 * sets like printCurrentSets, but only prints out the info if
	 * that set is non-empty. Empty sets are ignored.
	 */
	public static void printNonemptyCurrentSets() {
		Iterator<String> setIter;

		System.out.println("Affected Cond Sets [" + affectedCondMap.size() + "]:");
		setIter = affectedCondMap.keySet().iterator();
		while(setIter.hasNext()) {
			String uniqueName = setIter.next();
			Set<Integer> nodes = affectedCondMap.get(uniqueName);
			if(nodes.size() > 0) {
				System.out.print("- " + uniqueName + " [");
				for(Integer node : nodes) {
					System.out.print("" + node + ",");
				}
				System.out.println("]");
			}
		}

		System.out.println("Affected Write Sets [" + affectedWriteMap.size() + "]:");
		setIter = affectedWriteMap.keySet().iterator();
		while(setIter.hasNext()) {
			String uniqueName = setIter.next();
			Set<Integer> nodes = affectedWriteMap.get(uniqueName);
			if(nodes.size() > 0) {
				System.out.print("- " + uniqueName + " [");
				for(Integer node : nodes) {
					System.out.print("" + node + ",");
				}
				System.out.println("]");
			}
		}

		System.out.println("Affected Invoc Sets [" + affectedInvocMap.size() + "]:");
		setIter = affectedInvocMap.keySet().iterator();
		while(setIter.hasNext()) {
			String uniqueName = setIter.next();
			Set<Integer> nodes = affectedInvocMap.get(uniqueName);
			if(nodes.size() > 0) {
				System.out.print("- " + uniqueName + " [");
				for(Integer node : nodes) {
					System.out.print("" + node + ",");
				}
				System.out.println("]");
			}
		}

		System.out.println("Affected Return Sets [" + affectedReturnMap.size() + "]:");
		setIter = affectedReturnMap.keySet().iterator();
		while(setIter.hasNext()) {
			String uniqueName = setIter.next();
			Set<Integer> nodes = affectedReturnMap.get(uniqueName);
			if(nodes.size() > 0) {
				System.out.print("- " + uniqueName + " [");
				for(Integer node : nodes) {
					System.out.print("" + node + ",");
				}
				System.out.println("]");
			}
		}

		System.out.println("Reached Cond Sets [" + reachedCondMap.size() + "]:");
		setIter = reachedCondMap.keySet().iterator();
		while(setIter.hasNext()) {
			String uniqueName = setIter.next();
			Set<Integer> nodes = reachedCondMap.get(uniqueName);
			if(nodes.size() > 0) {
				System.out.print("- " + uniqueName + " [");
				for(Integer node : nodes) {
					System.out.print("" + node + ",");
				}
				System.out.println("]");
			}
		}

		System.out.println("Reached Write Sets [" + reachedWriteMap.size() + "]:");
		setIter = reachedWriteMap.keySet().iterator();
		while(setIter.hasNext()) {
			String uniqueName = setIter.next();
			Set<Integer> nodes = reachedWriteMap.get(uniqueName);
			if(nodes.size() > 0) {
				System.out.print("- " + uniqueName + " [");
				for(Integer node : nodes) {
					System.out.print("" + node + ",");
				}
				System.out.println("]");
			}
		}

		System.out.println("Reached Invoc Sets [" + reachedInvocMap.size() + "]:");
		setIter = reachedInvocMap.keySet().iterator();
		while(setIter.hasNext()) {
			String uniqueName = setIter.next();
			Set<Integer> nodes = reachedInvocMap.get(uniqueName);
			if(nodes.size() > 0) {
				System.out.print("- " + uniqueName + " [");
				for(Integer node : nodes) {
					System.out.print("" + node + ",");
				}
				System.out.println("]");
			}
		}

		System.out.println("Reached Return Sets [" + reachedReturnMap.size() + "]:");
		setIter = reachedReturnMap.keySet().iterator();
		while(setIter.hasNext()) {
			String uniqueName = setIter.next();
			Set<Integer> nodes = reachedReturnMap.get(uniqueName);
			if(nodes.size() > 0) {
				System.out.print("- " + uniqueName + " [");
				for(Integer node : nodes) {
					System.out.print("" + node + ",");
				}
				System.out.println("]");
			}
		}
	}
}
