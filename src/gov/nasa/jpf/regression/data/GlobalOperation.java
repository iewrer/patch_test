package gov.nasa.jpf.regression.data;

import static gov.nasa.jpf.regression.data.Operator.*;

public class GlobalOperation {
	
	String className;
	String methodName;
	String pos;
	String readWriteVal ="";
	String objName;
	Operator op;
	int objRef = -1;
	int threadId = -1;
	GlobalOperation next = null;
	
	public GlobalOperation(String className, String methodName,
			String pos, int threadId){ 
		this.className = className;
		this.methodName = methodName;
		this.pos = pos;
		this.threadId = threadId;
	}
	
	public GlobalOperation(String className, String methodName,
			String pos, int objRef, String objName) {
		this.className = className;
		this.methodName = methodName;
		this.pos = pos;
		this.objRef = objRef;
		this.objName = objName;
		
	}
	
	public void resetVals(String className, String methodName,
			String pos, int threadId, int objRef) {
		this.className = className;
		this.methodName = methodName;
		this.pos = pos;
		this.threadId = threadId;
		this.objRef = objRef;
	}
	
	public void initializeNextOperation(String className, String methodName,
			String pos, int threadId) { 
		next = new GlobalOperation(className, methodName, pos, threadId);
	}
	
	public GlobalOperation getSecondaryOperation(){
		return next;
	}
	
	public void setReadWriteVal(String rwVal) {
		this.readWriteVal = rwVal;
	}
	
	public void setObjRef(int objRef) {
		this.objRef = objRef;
	}
	
	public int getObjRef() {
		return objRef;
	}
	
	public void setReadOperation() {
		op = READ;
	}
	
	public void setWriteOperation() {
		op = WRITE;
	}
	
	//monitor enter
	public void setLockOperation() {
		op = MONITORENTER;
	}
	
	//monitor exit
	public void setUnlockOperation() {
		op = MONITOREXIT;
	}
	
	
	public String getUniqueId() {
		return className.concat(methodName.concat(pos));
	}
	
	public String getClassName() {
		return className;
	}
	
	public String getMethodName() {
		return methodName;
	}
	
	public String getPosition() {
		return pos;
	}
	
	public int getThreadId() {
		return threadId;
	}
	
	public Operator getOperator() {
		return op;
	}
	
	
	public String toString() {
		if(className == null) {
			return "_null_";
		}
		String retVal;
		
		retVal = "Thread Id:" + getThreadId()+ ","+ getUniqueId().
				concat("[" + readWriteVal +"]");
		if(next != null) {
			retVal = retVal.concat("next: " + next.getUniqueId());
		}
		return retVal;
	}
	
}