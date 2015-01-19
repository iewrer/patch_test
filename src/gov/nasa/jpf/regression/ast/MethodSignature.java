package gov.nasa.jpf.regression.ast;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class MethodSignature {
	
	private String className;
	private List<String> modifiers;
	private String methodName;
	private List<String> paramNames;
	private List<String> paramTypes;
	private String returnType;
	
	public MethodSignature(){
		className = "";
		modifiers = new ArrayList<String>();
		methodName = "";
		paramNames = new ArrayList<String>();
		paramTypes = new ArrayList<String>();
		returnType = "";
	}
	
	public MethodSignature(String clsName, List<String> mods, String name, 
			List<String> parNm,	List<String> parTyp, String returnTyp){
		className = clsName;
		modifiers = mods;
		methodName = name;
		paramNames = parNm;
		paramTypes = parTyp;
		returnType = returnTyp;
	}
	
	public String getClassName(){
		return className;
	}

	public List<String> getModifiers() {
		return modifiers;
	}

	public String getMethodName() {
		return methodName;
	}

	public List<String> getParamNames() {
		return paramNames;
	}

	public List<String> getParamTypes() {
		return paramTypes;
	}

	public String getReturnType() {
		return returnType;
	}
	
	//does not include class name
	public String toString(){
		String str="";
		if (modifiers.size()>0){
			Iterator<String> it = modifiers.iterator();
			while (it.hasNext()){
				str = str + it.next();
				if (it.hasNext())
					str = str + " ";
			}
			str = str + " " ;
		}
		str = str + returnType + " " + methodName + "(";
		if (paramNames.size()>0){
			int index = 0;
			while(index < paramNames.size()){
				str = str + paramTypes.get(index) + " " + paramNames.get(index);
				if (index+1 < paramNames.size())
					str = str + ", ";
				index++;
			}
		}
		str = str + ")";
		return str;
	}

}
