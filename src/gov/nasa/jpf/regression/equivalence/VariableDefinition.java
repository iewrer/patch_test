package gov.nasa.jpf.regression.equivalence;

public class VariableDefinition {
	
	private String name = "";
	private String type = "";
	
	public VariableDefinition(String n, String t){
		this.name = n;
		this.type = t;
	}
	
	public void setName(String n){
		this.name = n;
	}
	
	public void setType(String t){
		this.type = t;
	}
	
	public String getName(){
		return name;
	}
	
	public String getType(){
		return type;
	}

}
