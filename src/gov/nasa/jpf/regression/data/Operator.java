package gov.nasa.jpf.regression.data;

public enum Operator{
	
	READ(0),
	WRITE(1),
	MONITORENTER(2),
	MONITOREXIT(3);
	
	private final int op;
	
    Operator(int op){
		this.op= op;
	}
    
    public int getOp() {
    	return op;
    }
	
	
}