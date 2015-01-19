package gov.nasa.jpf.regression.listener;

import gov.nasa.jpf.vm.Instruction;


public class LocationData {
	
	protected String className;
	protected String methodName;
	protected String fullMethodName;
	protected Integer programLoc;
	protected String fileLoc;
	protected String srcLine;
	protected int lineNum;
	protected String bCode;
	protected String constraint ="";
	
	// An optional field for the actual instruction at a program location
    private Instruction instruction;
	
	public LocationData(String className, String mName, String fullMName,
			Integer programLoc, String fLoc, String sLine, int lNum, String byteCode){
		this.className = className;
		this.methodName = mName;
		this.fullMethodName = fullMName;
		this.programLoc = programLoc;
		this.fileLoc = fLoc;
		this.srcLine = sLine;
		this.lineNum = lNum;
		this.bCode = byteCode;
	}
	
	public String getClassName(){
		return className;
	}
	
	public String getMethodName() {
		return methodName;
	}
	
	public String getFullMethodName(){
		return fullMethodName;
	}
	
	public Integer getLocation() {
		return programLoc;
	}
	
	public String getFileLoc(){
		return fileLoc;
	}
	
	public String getSrcLine(){
		return srcLine;
	}
	
	public int getLineNum(){
		return lineNum;
	}
	
	public String getBCode(){
		return bCode;
	}
	
    public void setInstruction(Instruction instruction) {
        this.instruction = instruction;
    }
    
    public String getConstraint(){
    	return constraint;
    }
    
    public void addConstraint(String c){
    	constraint = c;
    }
    
    public String toString(){
    	return "insn: " + programLoc + ", cName: " + className + 
				", mName: " + methodName + ", fullMethodName: " + fullMethodName + 
				", loc: " + programLoc + ", fileLoc: " + fileLoc + ", srcLine: " + 
				srcLine + ", lineNum: " + lineNum + ", bytecode: " + bCode +
				", constraint: " + constraint;
    }

}
