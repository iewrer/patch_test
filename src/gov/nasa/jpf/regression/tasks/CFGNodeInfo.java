package gov.nasa.jpf.regression.tasks;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class CFGNodeInfo {
	
	private int id;
	private int startLine;
	private int endLine;
	private boolean controlNode;
	//list of all positions in the node and their change types
	private Map<Integer,String> posToChangeTypeMap;
	//set of affected control positions in this node
	private Set<Integer> affectedControlPos;
	//set of affected write positions in this node
	private Set<Integer> affectedWritePos;
	
	public CFGNodeInfo(){
		posToChangeTypeMap = new HashMap<Integer,String>();
		affectedControlPos = new HashSet<Integer>();
		affectedWritePos = new HashSet<Integer>();
	}
	
	public int getId(){
		return this.id;
	}
	public void setId(int id){
		this.id = id;
	}
	
	public int getStartLine() {
		return startLine;
	}
	
	public void setStartLine(int line){
		this.startLine = line;
	}

	public int getEndLine() {
		return endLine;
	}
	
	public void setEndLine(int line){
		this.endLine = line;
	}
	
	public boolean getControlNode(){
		return this.controlNode;
	}
	
	public void setControlNode(boolean b){
		this.controlNode = b;
	}

	public Map<Integer, String> getPosToChangeTypeMap() {
		return posToChangeTypeMap;
	}
	
	public void setPosToChangeTypeMap(Map<Integer,String> map){
		this.posToChangeTypeMap = map;
	}

	public Set<Integer> getAffectedWritePos(){
		return this.affectedWritePos;
	}
	
	public void addWritePos(Integer pos){
		this.affectedWritePos.add(pos);
	}
	
	public Set<Integer> getAffectedControlPos(){
		return this.affectedControlPos;
	}
	
	public void addControlPos(Integer pos){
		this.affectedControlPos.add(pos);
	}
	
	public String toString(){
		String s = "id=" + id + " startLine=" + startLine + " endLine=" + endLine +
		" controlNode= " + controlNode + 
		" positionToChangeMap= " + posToChangeTypeMap.toString() + 
		" affectedContPos=" + affectedControlPos.toString() + 
		" affectedWritePos=" + affectedWritePos.toString() + "\n";
	return s;
	}

}
