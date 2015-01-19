package gov.nasa.jpf.regression.bytecode;


public class AffectedMethodInfo {

	private Integer invokePosition;
	private String calleeMethodIndex;
	private String callerMethodIndex;

	public AffectedMethodInfo(String calleeMethodIndex,
			String callerMethodIndex,
			Integer invokePosition) {
		this.calleeMethodIndex = calleeMethodIndex;
		this.callerMethodIndex = callerMethodIndex;
		this.invokePosition = invokePosition;
	}

	// Accessor Methods
	public String getCalleeMethodIndex() {
		return calleeMethodIndex;
	}

	public String getCallerMethodIndex() {
		return callerMethodIndex;
	}

	public void setUniqueMethodIndex(String uniqueMethodIndex) {
		this.calleeMethodIndex = uniqueMethodIndex;
	}

	public void setInvokePositions(Integer invkPosition) {
		invokePosition = invkPosition;
	}

	public Integer getInvokePosition() {
		return invokePosition;
	}

	public String toString() {
		String retVal = "";
		retVal = retVal.concat(calleeMethodIndex + " -->"
					+ Integer.toString(invokePosition) + " ===>"
					+ callerMethodIndex);
		return retVal;
	}
}