package gov.nasa.jpf.regression.tasks;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

public class PathResults {
	String testInputs;
	String testResults;
	String pc;
	String scrubbedPC;
	int solverCalls;
	Vector<Integer> stmtsExecuted;
	Vector<Integer> posExecuted;
	
	public PathResults(){
		stmtsExecuted = new Vector<Integer>();
		posExecuted = new Vector<Integer>();
	}
	
	public String getTestInputs() {
		return testInputs;
	}
	public void setTestInputs(String testInputs) {
		this.testInputs = testInputs.trim();
	}
	public String getTestResults() {
		return testResults;
	}
	public void setTestResults(String testResults) {
		this.testResults = testResults.trim();
	}
	public String getPc() {
		return pc;
	}
	public String getScrubbedPC(){
		return this.scrubbedPC;
	}
	
	public void setPc(String pc) {
		this.pc = pc.trim();
    	String delims = "&&";
    	String[] constraints = this.pc.split(delims);
		String tmp = "";
    	for (int i=0; i< constraints.length; i++){
    		String[] constraint = constraints[i].split(" ");
    		for (int j=0; j<constraint.length; j++){
    			String c = constraint[j].trim();
    			if (c.contains("CONST"))
    				c = c.substring(c.indexOf("_") + 1);
    			else if (c.contains("[")){
    				String paren = "";
    				if (c.contains(")")){
    						paren = c.substring(c.indexOf(")"));
    						c = c.substring(0,c.indexOf("_")) + paren;
    				}else
    					c = c.substring(0,c.indexOf("_"));
    			}
    			tmp = tmp + " " + c;
    		}
    		if (i<constraints.length-1)
    			tmp = tmp + " &&";
    	}
		scrubbedPC = tmp;
	}
	
	public int getSolverCalls() {
		return solverCalls;
	}
	public void setSolverCalls(int solverCalls) {
		this.solverCalls = solverCalls;
	}
	public Vector<Integer> getStmtsExecuted() {
		return stmtsExecuted;
	}
	public void setStmtsExecuted(Vector<Integer> stmtsExecuted) {
		this.stmtsExecuted = stmtsExecuted;
	}
	public Vector<Integer> getPosExecuted() {
		return posExecuted;
	}
	public void setPosExecuted(Vector<Integer> posExecuted) {
		this.posExecuted = posExecuted;
	}
	
	public String toString(){
		String s = "testInputs=" + testInputs + "; testResults=" + testResults +
			//" pc= " + pc + " solverCalls=" + solverCalls +
			"; pc=" + scrubbedPC + "; solverCalls=" + solverCalls +
			"; stmtsExecuted=" + stmtsExecuted.toString() + 
			"; positionsExecuted=" + posExecuted.toString() + "\n";
		return s;
	}
	
	
}
