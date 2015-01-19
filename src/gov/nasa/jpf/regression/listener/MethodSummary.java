package gov.nasa.jpf.regression.listener;

//import gov.nasa.jpf.symbc.numeric.Constraint;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

public class MethodSummary{
	private String methodName = "";
	private String argTypes = "";
	private String argValues = "";
	private String symValues = "";
	
	//this is used for test case generation
	private Vector<String> pathConditions;

	//this is the input output constraints
//	protected Vector<inputAndOutputConstraint> inOut;
	
	private Map<String,String> pcToReturnValMap;
	private Map<String,Integer> pcToSolverCallCountMap;
	private Map<String,Integer> pcToTermCountMap;
	private Map<String,Vector<Integer>> pcToBytecodesMap;
	private Map<String,Vector<Integer>> pcToLineNumMap;
	//holds the list of constraints that can be dropped during eq checking
	private Map<String,List<String>> pcToConstraintsMap;

	public MethodSummary(){
	 	pathConditions = new Vector<String>();
//	 	inOut = new Vector<inputAndOutputConstraint>();
	 	
	 	pcToReturnValMap = new HashMap<String,String>();
	 	pcToSolverCallCountMap = new HashMap<String,Integer>();
	 	pcToBytecodesMap = new HashMap<String,Vector<Integer>>();
	 	pcToLineNumMap = new HashMap<String,Vector<Integer>>();
	 	pcToConstraintsMap = new HashMap<String,List<String>>();
	 	pcToTermCountMap = new HashMap<String,Integer>();
	}

	public void setMethodName(String mName){
		this.methodName = mName;
	}

	public String getMethodName(){
		return this.methodName;
	}

	public void setArgTypes(String args){
		this.argTypes = args;
	}

	public String getArgTypes(){
		return this.argTypes;
	}

	public void setArgValues(String vals){
		this.argValues = vals;
	}

	public String getArgValues(){
		return this.argValues;
	}

	public void setSymValues(String sym){
		this.symValues = sym;
	}

	public String getSymValues(){
		return this.symValues;
	}

//	public void addPathCondition(Constraint input, Constraint output, String pc, String retVal,
//			Integer count, Integer termCount, Vector<Integer> bytecodes,
//			Vector<Integer> lineNums,List<String> constraints){
//		
//		pathConditions.add(pc);
//		
//		inputAndOutputConstraint ioC = new inputAndOutputConstraint(input, output);
//		inOut.add(ioC);
//		
//		pcToReturnValMap.put(pc,retVal);
//		pcToSolverCallCountMap.put(pc, count);
//		pcToTermCountMap.put(pc, termCount);
//		pcToBytecodesMap.put(pc, bytecodes);
//		pcToLineNumMap.put(pc,lineNums);
//		pcToConstraintsMap.put(pc, constraints);
//	}

	public Vector<String> getPathConditions(){
		return this.pathConditions;
	}
	
//	public Vector<inputAndOutputConstraint> getActualPathConditions() {
//		return this.inOut;
//	}

	public String getRetVal(String pc){
		return this.pcToReturnValMap.get(pc);
	}

	public Integer getSolverCallCount(String pc){
		return this.pcToSolverCallCountMap.get(pc);
	}
	
	public Integer getTermCount(String pc){
		return this.pcToTermCountMap.get(pc);
	}
	
	public List<String> getListOfConstraints(String pc){
		return this.pcToConstraintsMap.get(pc);
	}

	public Vector<Integer> getBytecodes(String pc){
		return this.pcToBytecodesMap.get(pc);
	}

	public Vector<Integer>getLineNums(String pc){
		return this.pcToLineNumMap.get(pc);
	}

	public String toString(){
		String s = "";
		Iterator<String> it = pathConditions.iterator();
		while (it.hasNext()){
			String pc = it.next();
			String bytecodes = pcToBytecodesMap.get(pc).toString();
			String lineNums = pcToLineNumMap.get(pc).toString();
			Integer count = pcToSolverCallCountMap.get(pc);
			String retVal = pcToReturnValMap.get(pc);
			List<String> constraints = pcToConstraintsMap.get(pc);
			Iterator<String> itC = constraints.iterator();
			String dropped = "";
			while (itC.hasNext()){
				dropped = dropped + itC.next();
				if (itC.hasNext())
					dropped = dropped + ",";
			}
			s = s + pc + ";" + retVal + ";" + bytecodes + ";" +
				lineNums + ";" + count + ";" + dropped + "\n";
		}
		return s;
	}

//	public class inputAndOutputConstraint {
//		Constraint inputConstraintHeader;
//		Constraint outputConstraintHeader;
//		
//		public inputAndOutputConstraint() {}
//		
//		
//		public inputAndOutputConstraint(Constraint input,
//				Constraint output) {
//			this.inputConstraintHeader = input;
//			this.outputConstraintHeader = output;
//		}
//		
//		public void setInputConstraint(Constraint input) {
//			this.inputConstraintHeader = input;
//		}
//		
//		public void setOutputConstraint(Constraint output) {
//			this.outputConstraintHeader = output;
//		}
//		
//		public Constraint getInputConstraint() {
//			return this.inputConstraintHeader;
//		}
//		
//		public Constraint getOutputConstraint() {
//			return this.outputConstraintHeader;
//		}
//	}


}

