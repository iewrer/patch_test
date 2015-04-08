package gov.nasa.jpf.regression.ast;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MethodASTInfo {

	private String originalFileName;
	private String modifiedFileName;
	private String className;
	private String methodName;
	private MethodSignature origMethodSignature;
	private MethodSignature modMethodSignature;
	private Set<Integer> changedLinesOrig = new HashSet<Integer>();
	private Set<Integer> changedLinesMod = new HashSet<Integer>();
	private Set<Integer> origAssertLines = new HashSet<Integer>();
	private Set<Integer> modAssertLines = new HashSet<Integer>();

	private Set<Integer> addedLines = new HashSet<Integer>();
	private Set<Integer> removedLines = new HashSet<Integer>();
//	private Set<Integer> assertLines = new HashSet<Integer>();

	private boolean matched = false;
	private boolean equivalent = false;
	private boolean added = false;
	private boolean removed = false;
	private List<String> origModifiers = new ArrayList<String>();
	private List<String> modModifiers = new ArrayList<String>();
	private List<String> parameters = new ArrayList<String>();
	private List<String> paramTypes = new ArrayList<String>();
	private String returnType = "";
	private List<String> origParameters = new ArrayList<String>();
	private List<String> origParamTypes = new ArrayList<String>();
	private String origReturnType = "";
	private String defaultConstructorName = "";
	private String fileName = "";

	public MethodASTInfo() {

	}
	
	//make orig mod and mod orig
	public void reverse(){
		Set<Integer> tmpLines = changedLinesOrig;
		changedLinesOrig = changedLinesMod;
		changedLinesMod = tmpLines;
		tmpLines = addedLines;
		addedLines = removedLines;
		removedLines = tmpLines;
		List<String> tmpParams = parameters;
		parameters = origParameters;
		origParameters = tmpParams;
		tmpParams = paramTypes;
		paramTypes = origParamTypes;
		origParamTypes = tmpParams;
		String temp = returnType;
		returnType = origReturnType;
		origReturnType = temp;
		List<String> tmpMods = origModifiers;
		origModifiers = modModifiers;
		modModifiers = tmpMods;
		boolean tmpAR = added;
		added = removed;
		removed = tmpAR;
		temp = originalFileName;
		originalFileName = modifiedFileName;
		modifiedFileName=temp;
	}

	public MethodASTInfo(String className, String methodName) {
		this.className = className;
		this.methodName = methodName;

	}
	
	public Set<Integer> getAddedLines() {
		return addedLines;
	}
	public void setAddedLines(Set<Integer> addedLines) {
		this.addedLines = addedLines;
	}
	public String getUniqueMethodIndex(){
		String methodArgs = new String("");
		for(int argIndex = 0 ; argIndex <
								paramTypes.size(); argIndex++) {
			methodArgs = methodArgs.concat(paramTypes.get(argIndex));
		}
		if(defaultConstructorName.equals(""))
			return (methodName + methodArgs);
		return (defaultConstructorName+methodArgs);
	}

	public String getClassName(){
		return className;
	}

	public String getMethodName() {
		if(defaultConstructorName.equals(""))
			return methodName;
		return
			defaultConstructorName;
	}
	
	// includes method modifiers, method name, args and their respective types
	public String getOrigLongMethodName(){
		return this.getOrigMethodSig().toString();
	}
	
	// includes method modifiers, method name, args and their respective types
	public String getModLongMethodName(){
		return this.getModMethodSig().toString();
	}
	
	public MethodSignature getOrigMethodSig(){
		//create if not already instantiated
		if (origMethodSignature == null){
			origMethodSignature = new MethodSignature(className,origModifiers, methodName,
					origParameters,origParamTypes,origReturnType);
		}
		return origMethodSignature;
	}
	
	public MethodSignature getModMethodSig(){
		//create if not already instantiated
		if (modMethodSignature == null){
			modMethodSignature = new MethodSignature(className,modModifiers, methodName,
					parameters,paramTypes,returnType);
		}
		return modMethodSignature;
	}
	public void setOriginalFileName(String name){
		this.originalFileName = name;
	}
	public String getOriginalFileName(){
		return this.originalFileName;
	}
	public void setModifiedFileName(String name){
		this.modifiedFileName = name;
	}
	public String getModifiedFileName(){
		return this.modifiedFileName;
	}

	public void addChangedOrig(int line){
		changedLinesOrig.add(line);
	}
	
	public Set<Integer> getChangedLinesOrig() {
		return changedLinesOrig;
	}
	public void setChangedLinesOrig(Set<Integer> changedLinesOrig) {
		this.changedLinesOrig  = changedLinesOrig;
	}
	public int getNumberChangedLinesOrig(){
		return changedLinesOrig.size();
	}

	public int getNumberChangedLinesMod(){
		return changedLinesMod.size();
	}
	
	public Set<Integer> getChangedLinesMod() {
		return changedLinesMod;
	}

	public void setChangedLinesMod(Set<Integer> changedLinesMod) {
		this.changedLinesMod  = changedLinesMod;
	}

	public void addChangedMod(int line){
		changedLinesMod.add(line);
	}

	public void addAddedLine(int line){
		addedLines.add(line);
	}

	public void addRemovedLine(int line){
		removedLines.add(line);
	}
	public Set<Integer> getRemovedLines(){
		return removedLines;
	}
	public void setRemovedLines(Set<Integer> removedLines) {
		this.removedLines = removedLines;
	}
	public Set<Integer> getAssertLinesMod(){
		return modAssertLines;
	}
	
	public void addAssertLineMod(int line){
		modAssertLines.add(line);
	}
	
	public Set<Integer> getAssertLinesOrig(){
		return origAssertLines;
	}
	
	public void addAssertLineOrig(int line){
		origAssertLines.add(line);
	}
	
	public void setMatched(boolean mat){
		this.matched = mat;
	}

	public boolean getMatched(){
		return matched;
	}

	public void setEquivalent(boolean eq){
		this.equivalent = eq;
	}

	public boolean getEquivalent(){
		return equivalent;
	}

	public void setAdded(boolean added){
		this.added = added;
	}

	public boolean getAdded(){
		return added;
	}

	public void setRemoved(boolean removed){
		this.removed = removed;
	}

	public boolean getRemoved(){
		return removed;
	}
	
	public List<String> getOrigModifiers(){
		return origModifiers;
	}
	
	public List<String> getModModifiers(){
		return modModifiers;
	}
	
	public void setOrigModifiers(List<String> mods){
		origModifiers = mods;
	}
	
	public void setModModifiers(List<String> mods){
		modModifiers = mods;
	}

	public String getOrigLineType(int line){
		if (changedLinesOrig.contains(line))
			return "changed";
		else if(removedLines.contains(line))
			return "removed";
		else
			return "unchanged";
	}

	public String getModLineType(int line){
		if (changedLinesMod.contains(line))
			return "changed";
		else if(addedLines.contains(line))
			return "added";
		else
			return "unchanged";
	}

	public boolean ifAssertOrig(int line){
		return origAssertLines.contains(line);
	}
	
	public boolean ifAssertMod(int line){
		return modAssertLines.contains(line);
	}
	
	public List<String> getParameters(){
		return parameters;
	}

	public void addParameter(String param){
		this.parameters.add(param);
	}

	public List<String> getParamTypes(){
		return paramTypes;
	}

	public void addParamType(String type){
		this.paramTypes.add(type);
	}

	public void setReturnType(String ret){
		this.returnType = ret;
	}

	public String getReturnType(){
		return returnType;
	}

	public List<String> getOrigParameters(){
		return origParameters;
	}

	public void addOrigParameter(String param){
		this.origParameters.add(param);
	}

	public List<String> getOrigParamTypes(){
		return origParamTypes;
	}

	public void addOrigParamType(String type){
		this.origParamTypes.add(type);
	}

	public void setOrigReturnType(String ret){
		this.origReturnType = ret;
	}

	public String getOrigReturnType(){
		return origReturnType;
	}

	public void setDefaultConstructorName(String name){
		this.defaultConstructorName = name;
	}

	public String getDefaultConstructorName(){
		return defaultConstructorName;
	}
	
	public void setFileName(String fName){
		this.fileName = fName;
	}

	public String getFileName(){
		return this.fileName;
	}

	/*
	 * The rest of this may be obsolete, but will leave for now
	 */
	private Map<BigInteger,BlockASTInfo> originalBlocks =
		new HashMap<BigInteger,BlockASTInfo>();
	private Map<BigInteger,BlockASTInfo> modifiedBlocks =
		new HashMap<BigInteger,BlockASTInfo>();

	public void addOriginalBlock(BigInteger blockID, BlockASTInfo block){
		this.originalBlocks.put(blockID, block);
	}

	public void addModifiedBlock(BigInteger blockID, BlockASTInfo block){
		this.modifiedBlocks.put(blockID, block);
	}

	public Map<BigInteger,BlockASTInfo> getOriginalBlocks(){
		return originalBlocks;
	}

	public Map<BigInteger,BlockASTInfo> getModifiedBlocks(){
		return modifiedBlocks;
	}

	public String toString(){
		String temp = className + " " + methodName + "\n";
		temp = temp + "changed Lines in Orig version: " + changedLinesOrig.toString() +
			"\n changed Lines in Mod version: " + changedLinesMod.toString() +
			"\n added Lines: " + addedLines + "\n removed lines: " + removedLines +
			"\n matched: " + matched + " equivalent: " + equivalent +
			"\n added: " + added + " removed: " + removed;
		if (matched){
			if (parameters.size() > 0){
				temp = temp + "\n parameters:";
				for (int i=0; i< parameters.size(); i++){
					temp = temp + "(" + paramTypes.get(i) + ")" + parameters.get(i)
						+ ",";
				}
			}
			temp = temp + "\n return type: " + returnType;
			if (!equivalent){
				if (origParameters.size() > 0){
					temp = temp + "\n origParameters:";
					for (int i=0; i< origParameters.size(); i++){
						temp = temp + "(" + origParamTypes.get(i) + ")" + origParameters.get(i)
							+ ",";
					}
				}
				temp = temp + "\n orig return type: " + origReturnType;
			}
		}else{
			if (added){
				if (parameters.size() > 0){
					temp = temp + "\n parameters:";
					for (int i=0; i< parameters.size(); i++){
						temp = temp + "(" + paramTypes.get(i) + ")" + parameters.get(i)
							+ ",";
					}
				}
				temp = temp + "\n return type: " + returnType;

			}else{ //removed
				if (origParameters.size() > 0){
					temp = temp + "\n origParameters:";
					for (int i=0; i< origParameters.size(); i++){
						temp = temp + "(" + origParamTypes.get(i) + ")" + origParameters.get(i)
							+ ",";
					}
				}
				temp = temp + "\n orig return type: " + origReturnType;
			}
		}
		if (!defaultConstructorName.equalsIgnoreCase(""))
			temp = temp + " defaultConstructorName: " + defaultConstructorName;
		temp = temp +  "\n************************\n";

		return temp;
	}

}