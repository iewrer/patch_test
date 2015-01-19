package gov.nasa.jpf.regression.ast;

import java.math.BigInteger;
import java.util.HashSet;
import java.util.Set;

public class BlockASTInfo {

	private int startLine;
	private int endLine;
	private String changeType;
	private BigInteger matchedBlock;
	private BigInteger parent;
	private Set<Integer> assertStatements = new HashSet<Integer>();

	public int getStartLine() {
		return startLine;
	}
	public void setStartLine(int startLine) {
		this.startLine = startLine;
	}
	public int getEndLine() {
		return endLine;
	}
	public void setEndLine(int endLine) {
		this.endLine = endLine;
	}
	public String getChangeType() {
		return changeType;
	}
	public void setChangeType(String changeType) {
		this.changeType = changeType;
	}

	public BigInteger getMatchedBlock(){
		return this.matchedBlock;
	}

	public void setMatchedBlock(BigInteger matchedBlock){
		this.matchedBlock = matchedBlock;
	}

	public BigInteger getParent(){
		return this.parent;
	}

	public void setParent(BigInteger parent){
		this.parent = parent;
	}
	
	public Set<Integer> getAssertStatement(){
		return assertStatements;
	}
}
