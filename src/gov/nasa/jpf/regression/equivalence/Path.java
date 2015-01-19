package gov.nasa.jpf.regression.equivalence;

public class Path {
	//stored as S-expressions
	private String droppedConstraints;
	private String pc;
	private String effects;
	
	public Path(String pc, String e, String dropped){
		this.pc = pc;
		this.effects = e;
		this.droppedConstraints = dropped;
	}
	
	public void setPC(String pc){
		this.pc = pc;
	}
	
	public void setEffects(String e){
		this.effects = e;
	}
	
	public void setDropped(String d){
		this.droppedConstraints = d;
	}
	
	public String getPC(){
		return pc;
	}
	
	public String getEffects(){
		return effects;
	}
	
	public String getDropped(){
		return droppedConstraints;
	}
	
	public String toString(){
		return ">>>PC: " + pc + "\n     returns: " + effects + " (dropped: " + droppedConstraints + ")";
	}
}
