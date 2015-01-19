package gov.nasa.jpf.regression.bytecode;

import java.util.ArrayList;
import java.util.List;

public class StackNode {

	String methodIndex;
	Integer position;
	List<AffectedMethodInfo>
					methodCalls;

	List<String> functionalConds;
	List<String> feasibleUnimpactedConds;

	public StackNode(String methodIndex,
				Integer position) {
		this.methodIndex = methodIndex;
		this.position = position;
		methodCalls = new ArrayList<AffectedMethodInfo>();
		functionalConds = new ArrayList<String>();
		feasibleUnimpactedConds = new ArrayList<String>();
	}

	public void clearNodes() {
		functionalConds.clear();
		feasibleUnimpactedConds.clear();
	}

	public List<String> getFunctionalConditional() {
		return functionalConds;
	}

	public List<String> getFeasibleUnimpactedConds() {
		return feasibleUnimpactedConds;
	}

	public List<String> getCurrentConds() {
		ArrayList<String> fConds = new ArrayList<String>();
		for(int condIndex = 0; condIndex < functionalConds.size();
							condIndex++) {
			String fStr = functionalConds.get(condIndex);
			//if(!feasibleUnimpactedConds.contains(fStr)) {
				fConds.add(fStr);
			//}
		}
		return fConds;
	}

	public void addFunctionalConditional(String constraint) {
		functionalConds.add(constraint);
	}

	public void addNonFunctionalConditional(String constraint) {
		feasibleUnimpactedConds.add(constraint);
	}

	public void addAffectedMethodInfo
							(AffectedMethodInfo ami) {
		methodCalls.add(ami);
	}

	public String getMethodIndex() {
		return methodIndex;
	}

	public Integer getPosition(){
		return this.position;
	}

	public String getUniqueId() {
		return this.methodIndex
					.concat(Integer.toString(position));
	}

	public List<AffectedMethodInfo> getMethodCalls() {
		return this.methodCalls;
	}

	public String toString () {
		String retVal = "";
		retVal = retVal.concat(methodIndex + " ==> "
					+ Integer.toString(position) + "\n"+
				"AffectedMethodInfo: " + methodCalls.toString() +"\n");
		return retVal;
	}
}