//TODO: needs to be simplified;
// summary printing broken

//
//Copyright (C) 2007 United States Government as represented by the
// Administrator of the National Aeronautics and Space Administration
// (NASA).  All Rights Reserved.
//
// This software is distributed under the NASA Open Source Agreement
// (NOSA), version 1.3.  The NOSA has been approved by the Open Source
// Initiative.  See the file NOSA-1.3-JPF at the top of the distribution
// directory tree for the complete NOSA document.
//
// THE SUBJECT SOFTWARE IS PROVIDED "AS IS" WITHOUT ANY WARRANTY OF ANY
// KIND, EITHER EXPRESSED, IMPLIED, OR STATUTORY, INCLUDING, BUT NOT
// LIMITED TO, ANY WARRANTY THAT THE SUBJECT SOFTWARE WILL CONFORM TO
// SPECIFICATIONS, ANY IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR
// A PARTICULAR PURPOSE, OR FREEDOM FROM INFRINGEMENT, ANY WARRANTY THAT
// THE SUBJECT SOFTWARE WILL BE ERROR FREE, OR ANY WARRANTY THAT
// DOCUMENTATION, IF PROVIDED, WILL CONFORM TO THE SUBJECT SOFTWARE.
package gov.nasa.jpf.regression.listener;

// does not work well for static methods:summary not printed for errors
import gov.nasa.jpf.Config;
import gov.nasa.jpf.JPF;
import gov.nasa.jpf.PropertyListenerAdapter;
import gov.nasa.jpf.vm.ChoiceGenerator;
import gov.nasa.jpf.vm.ClassInfo;
import gov.nasa.jpf.vm.DynamicElementInfo;
import gov.nasa.jpf.vm.VM;
import gov.nasa.jpf.vm.LocalVarInfo;
import gov.nasa.jpf.vm.MethodInfo;
import gov.nasa.jpf.vm.StackFrame;
import gov.nasa.jpf.vm.SystemState;
import gov.nasa.jpf.vm.ThreadInfo;
import gov.nasa.jpf.jvm.bytecode.ARETURN;
import gov.nasa.jpf.jvm.bytecode.DRETURN;
import gov.nasa.jpf.jvm.bytecode.FRETURN;
import gov.nasa.jpf.jvm.bytecode.IRETURN;
import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.jvm.bytecode.JVMInvokeInstruction;
import gov.nasa.jpf.jvm.bytecode.LRETURN;
import gov.nasa.jpf.jvm.bytecode.JVMReturnInstruction;
import gov.nasa.jpf.report.ConsolePublisher;
import gov.nasa.jpf.report.Publisher;
import gov.nasa.jpf.report.PublisherExtension;
import gov.nasa.jpf.search.Search;
import gov.nasa.jpf.symbc.SymbolicInstructionFactory;
import gov.nasa.jpf.symbc.bytecode.BytecodeUtils;
import gov.nasa.jpf.symbc.bytecode.INVOKESTATIC;
import gov.nasa.jpf.symbc.numeric.Comparator;
import gov.nasa.jpf.symbc.numeric.Constraint;
import gov.nasa.jpf.symbc.numeric.Expression;
import gov.nasa.jpf.symbc.numeric.IntegerConstant;
import gov.nasa.jpf.symbc.numeric.IntegerExpression;
import gov.nasa.jpf.symbc.numeric.PCChoiceGenerator;
import gov.nasa.jpf.symbc.numeric.PathCondition;
import gov.nasa.jpf.symbc.numeric.SymbolicInteger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.StringTokenizer;
import java.util.Vector;

import org.apache.commons.lang.StringUtils;

public class RSESymbolicListener extends PropertyListenerAdapter implements PublisherExtension {

	/* Locals to preserve the value that was held by JPF prior to changing it
	 * in order to turn off state matching during symbolic execution
	 */
	
	//the following option is useful when debugging - prints PC and return val
	//when a method in the target class returns
	private boolean printReturnValues = false;
	private boolean retainVal = false;
	private boolean forcedVal = false;
	private boolean printTestCases = false;
	private boolean htmlOutput = false;
	private boolean printStats = true;
	private boolean solving_at_end = false;
	String testFile = "";

	private Map<String,MethodSummary> allSummaries;
	private String currentMethodName = "";
	private Set<String> symMethods = new HashSet<String>();
	private boolean inSymbolicMethod = false;
	private Vector<Integer> byteCodes;
	private Vector<Integer> previousByteCodes;
	private Vector<Integer> lineNums;
	private Vector<Integer> previousLineNums;
	private int lastInstruction=0;
	private int lastByteCode=0;
	private String mainClassName = "";
	private boolean runningDiSE= false;
	private Stack<Vector<Integer>> bcLists = new Stack<Vector<Integer>>();
	private Stack<Vector<Integer>> lineNumLists = new Stack<Vector<Integer>>();
	
	public Map<String, MethodSummary> getAllMethodSummaries() {
		return allSummaries;
	}

	/* TODO: duplicate setting options for backwards compatibility
	 * Now using this listener for both symbc and dise
	 */
	public RSESymbolicListener(Config conf, JPF jpf) {
		System.out.println("------->Using RSESymbolicListener in 3/12/2012");
		jpf.addPublisherExtension(ConsolePublisher.class, this);
		allSummaries = new HashMap<String, MethodSummary>();
		if (conf.containsKey("rse.testCases"))
			printTestCases =conf.getBoolean("rse.testCases");
		if (conf.containsKey("symbc.testCases"))
			printTestCases =conf.getBoolean("symbc.testCases");
		if (conf.containsKey("rse.testFile"))
			testFile = conf.getString("rse.testFile");
		if (conf.containsKey("symbc.results"))
			testFile = conf.getString("symbc.results");
		if (conf.containsKey("rse.html"))
			htmlOutput = conf.getBoolean(("rse.html"));
		if(conf.containsKey("target"))
			mainClassName = conf.getString("target");
		if(conf.containsKey("symbolic.method"))
			processSymMethods(conf.getString("symbolic.method"));
		//need to distinguish between std symbc and dise for file naming purposes
		if (conf.containsKey("listener")){
			String tmp = conf.getString("listener");
			if (tmp.contains("Pruning"))
				runningDiSE = true;
		}
		if (conf.containsKey("symbc.solving_at_end")){
			String val = conf.getString("symbc.solving_at_end");
			if(val.equals("true")) {
				solving_at_end = true;
			}
		}
		if(solving_at_end) PathCondition.isReplay = true;  // turn off constraint solving
	}
	

	
	private void processSymMethods(String list){
		if (list.contains(",")){
			String[] methods = list.split(",");
			for(int i= 0; i<methods.length;i++){
				String lName = methods[i];
				lName = lName.substring(lName.lastIndexOf('.')+1,lName.indexOf('('));
				symMethods.add(lName);	
			}
		}else
			symMethods.add(list);
	}

	//not yet tested
	public void propertyViolated (Search search){
		VM vm = search.getVM();
		ChoiceGenerator <?>cg = vm.getChoiceGenerator();
		if (!(cg instanceof PCChoiceGenerator)){
			ChoiceGenerator <?> prev_cg = cg.getPreviousChoiceGenerator();
			while (!((prev_cg == null) || (prev_cg instanceof PCChoiceGenerator))) {
				prev_cg = prev_cg.getPreviousChoiceGenerator();
			}
			cg = prev_cg;
		}
		if ((cg instanceof PCChoiceGenerator) &&
			      ((PCChoiceGenerator) cg).getCurrentPC() != null){
			PathCondition pc = ((PCChoiceGenerator) cg).getCurrentPC();
			String error = search.getLastError().getDetails();
			error = "\"" + error.substring(0,error.indexOf("\n")) + "...\"";
			PathCondition result = new PathCondition();
			IntegerExpression sym_err = new SymbolicInteger("ERROR");
			IntegerExpression sym_value = new SymbolicInteger(error);
			result._addDet(Comparator.EQ, sym_err, sym_value);
			//solve the path condition, then print it
			pc.solve();
			Integer constraintCount = new Integer(pc.getSolverCalls());
			int termCount = pc.termCount();
			MethodSummary methodSummary = allSummaries.get(currentMethodName);
			if(InterProcPruningListener.getFunctionalCondNodes() != null) {
				ArrayList<String> funcNodes = InterProcPruningListener.getFunctionalCondNodes();
				methodSummary.addPathCondition(pc.getConstraintHeader(), result.getConstraintHeader(),
						pc.stringPC(), error, constraintCount, termCount, byteCodes,lineNums,funcNodes);
			}else
				methodSummary.addPathCondition(pc.getConstraintHeader(), result.getConstraintHeader(),
							pc.stringPC(), error, constraintCount, termCount, byteCodes,lineNums,null);
			allSummaries.put(currentMethodName,methodSummary);
			System.out.println("Property Violated: PC is "+pc.toString());
			System.out.println("Property Violated: result is  "+error);
			System.out.println("****************************");
		}
	}

	private void setSymbolicEnvironment(SystemState ss){
		//get the original values and save them for restoration after
		//we are done with symbolic execution
		retainVal = ss.getRetainAttributes();
		forcedVal = ss.isForced();
		//turn off state matching
		ss.setForced(true);
		//make sure it stays turned off when a new state is created
		ss.retainAttributes(true);
		inSymbolicMethod = true;
	}

	private MethodSummary createMethodSummary(String methodName, MethodInfo mi,
			ThreadInfo ti, JVMInvokeInstruction md){
		MethodSummary ms = new MethodSummary();
		String shortName = methodName;
		if (methodName.contains("("))
			shortName = methodName.substring(0,methodName.indexOf("("));
		ms.setMethodName(shortName);
		Object [] args = md.getArgumentValues(ti);
		String argValues = "";
		for (int i=0; i<args.length; i++){
			argValues = argValues + args[i];
			if ((i+1) < args.length)
				argValues = argValues + ",";
		}
		ms.setArgValues(argValues);
		String [] argTypeNames = md.getInvokedMethod(ti).getArgumentTypeNames();
		String argTypes = "";
		for (int j=0; j<argTypeNames.length; j++){
			argTypes = argTypes + argTypeNames[j];
			if ((j+1) < argTypeNames.length)
				argTypes = argTypes + ",";
		}
		ms.setArgTypes(argTypes);
		return ms;
	}

	private String getSymValues(ThreadInfo ti, MethodInfo mi, JVMInvokeInstruction md,
			int numberOfArgs){
		String vals = "";
		//get the symbolic values (changed from constructing them here)
		StackFrame sf = ti.getTopFrame();
		LocalVarInfo[] varInfo = mi.getLocalVars();

		//if debug option was not used when compiling the class,
		//then we do not have names of the locals and need to
		//use a different naming scheme; previous code was broken
		if (varInfo == null)
			throw new RuntimeException("ERROR: you need to turn debug option on");

		int sfIndex;
		if (md instanceof INVOKESTATIC)
				sfIndex=0;
		else
			sfIndex=1; // do not consider implicit parameter "this"
		String symValues = "";
		String symVarName = "";
		for(int i=sfIndex; i < numberOfArgs; i++){
			Expression expLocal = null;
			if(sf.hasAttrs())
			    expLocal = (Expression)sf.getLocalAttr(sfIndex, Expression.class);
			if (expLocal != null){ // symbolic
				symVarName = expLocal.toString();
				symValues = symValues + symVarName + ",";
			}
			else
				symVarName = varInfo[sfIndex].getName() + "_CONCRETE" + ",";
			String [] argTypeNames = md.getInvokedMethod(ti).getArgumentTypeNames();
			if(argTypeNames[i].equals("double") || argTypeNames[i].equals("long"))
				sfIndex=sfIndex+2;
			else
			    sfIndex++;
		}

		// get rid of last ","
		if (symValues.endsWith(",")) {
			symValues = symValues.substring(0,symValues.length()-1);
		}
		return vals;
	}

	private String processReturn(PathCondition result) {
		SymbolicInteger ret = new SymbolicInteger("RETURN");
		SymbolicInteger val = new SymbolicInteger("VOID");
		result._addDet(Comparator.EQ, ret, val);
		return "RETURN==VOID";
	}
	
	private String processIReturn(ThreadInfo ti, PathCondition pc, 
			PathCondition result, Instruction insn, IntegerExpression sym_result){
		String returnString = "RETURN==";
		IRETURN ireturn = (IRETURN)insn;
		int returnValue = ireturn.getReturnValue();
		IntegerExpression returnAttr = (IntegerExpression) ireturn.getReturnAttr(ti, Expression.class);
		if (returnAttr != null){
				returnString = returnString + returnAttr.sExpressionPC();
				//returnString = returnString + String.valueOf(returnAttr.sExpressionPC());
		}else // concrete
			returnString = returnString + String.valueOf(returnValue);
		if (returnAttr != null){
			pc._addDet(Comparator.EQ, sym_result, returnAttr);
		}else{ // concrete
			pc._addDet(Comparator.EQ, sym_result, new IntegerConstant(returnValue));
		}
		return returnString;
	}

	private String processAReturn(ThreadInfo ti, PathCondition pc, 
			PathCondition result, Instruction insn, IntegerExpression sym_result){
		String returnString = "";
		ARETURN areturn = (ARETURN)insn;
		IntegerExpression returnAttr = (IntegerExpression) areturn.getReturnAttr(ti, Expression.class);
		if (returnAttr != null)
			returnString = String.valueOf(returnAttr.solution());
		else {// concrete
			Object val = areturn.getReturnValue(ti);
			returnString = String.valueOf(val.toString());
		}
		if (returnAttr != null){
			System.out.println("is it not null");
			pc._addDet(Comparator.EQ, sym_result, returnAttr);
			System.out.println(pc.toString());
		}else{ // concrete
			System.out.println("it is null");
			DynamicElementInfo val = (DynamicElementInfo)areturn.getReturnValue(ti);
				String tmp = val.toString();
				tmp = tmp.substring(tmp.lastIndexOf('.')+1);
				pc._addDet(Comparator.EQ, sym_result,  new SymbolicInteger(tmp));
				System.out.println(pc.toString());
		}
		return returnString;
	}

	public void instructionExecuted(VM vm, ThreadInfo currentThread,
			  Instruction nextInstruction, Instruction executedInstruction)  {

		if (!vm.getSystemState().isIgnored()) {
			Instruction insn = executedInstruction;
			SystemState ss = vm.getSystemState();
			ThreadInfo ti = currentThread;
			Config conf = vm.getConfig();

			if (insn instanceof JVMInvokeInstruction) {
				JVMInvokeInstruction md = (JVMInvokeInstruction) insn;
				String methodName = md.getInvokedMethodName();
				int numberOfArgs = md.getArgumentValues(ti).length;
				MethodInfo mi = md.getInvokedMethod();
				String className = mi.getClassInfo().getName();
				//neha:changed invoked method to full name method
				//System.out.println("!!!!!!!!!! full name "+mi.getFullName());
				//if we were in a symbolic method, save the information before
				//continuing in this method

				if (!ti.isFirstStepInsn()){
					//add call to the invoked method, then push the
					//lists onto the stacks
					//if (inSymbolicMethod){
					//	if (insn.getLineNumber() != lastInstruction){
					//		lineNums.add(new Integer(insn.getLineNumber()));
					//		lastInstruction = insn.getLineNumber();
					//	}
					//	if (insn.getPosition() != lastByteCode){
					//		byteCodes.add(new Integer(insn.getPosition()));
					//		lastByteCode = insn.getPosition();
					//	}
					//	bcLists.push(byteCodes);
					//	lineNumLists.push(lineNums);
					//}
					
					if ((BytecodeUtils.isClassSymbolic(conf, className, mi, methodName))
							|| BytecodeUtils.isMethodSymbolic(conf, mi.getFullName(), numberOfArgs, null)){
						setSymbolicEnvironment(ss);
						MethodSummary methodSummary = createMethodSummary(methodName,mi,ti,md);
						methodSummary.setSymValues(getSymValues(ti, mi, md, numberOfArgs));
						currentMethodName = mi.getLongName();
						allSummaries.put(currentMethodName,methodSummary);
					//	lastByteCode=0;
					//	lastInstruction = 0;
					//	byteCodes = new Vector<Integer>();
					//	lineNums = new Vector<Integer>();
					}else{
						inSymbolicMethod = false;
					}
				}

			}else if (insn instanceof JVMReturnInstruction){
				MethodInfo mi = insn.getMethodInfo();
				String mName = mi.getFullName();
				String cName = mi.getClassName();
				if (printReturnValues && cName.contains(mainClassName) && !mName.contains("<init>")){
					System.out.println("^^^^^^RETURN FROM : " + mName);
					ChoiceGenerator <?>cg1 = vm.getChoiceGenerator();
					if (!(cg1 instanceof PCChoiceGenerator)){
						ChoiceGenerator <?> prev_cg = cg1.getPreviousChoiceGenerator();
						while (!((prev_cg == null) || (prev_cg instanceof PCChoiceGenerator))) {
							prev_cg = prev_cg.getPreviousChoiceGenerator();
						}
						cg1 = prev_cg;
					}
					if ((cg1 instanceof PCChoiceGenerator) &&(
							(PCChoiceGenerator) cg1).getCurrentPC() != null){
						PathCondition pc = ((PCChoiceGenerator) cg1).getCurrentPC();
						System.out.println("Current PC: " + pc.toString());
					}
					if (insn instanceof IRETURN){
						IRETURN ireturn = (IRETURN)insn;
						IntegerExpression returnAttr = (IntegerExpression) ireturn.getReturnAttr(ti, Expression.class);
						if (returnAttr != null)
							System.out.println("RETURN==" + returnAttr.toString());
						else
							System.out.println("RETURN==" + ireturn.getReturnValue());
					}else if (insn instanceof DRETURN){
						System.out.println("IMPLEMENT ME: DRETURN");
					}else if (insn instanceof LRETURN){
						System.out.println("IMPLEMENT ME: LRETURN");
					}else if (insn instanceof FRETURN){
						System.out.println("IMPLEMENT ME: FRETURN");
					}else if (insn instanceof ARETURN){
						System.out.println("IMPLEMENT ME: ARETURN");
					}else
						System.out.println("RETURN=VOID");
				}
				ClassInfo ci = mi.getClassInfo();
				if (inSymbolicMethod){
					//if (insn.getLineNumber() != lastInstruction){
					//	lineNums.add(new Integer(insn.getLineNumber()));
					//	lastInstruction = insn.getLineNumber();
					//}
					//if (insn.getPosition() != lastByteCode){
					//	byteCodes.add(new Integer(insn.getPosition()));
					//	lastByteCode = insn.getPosition();
					//}
				}
				if (null != ci){
					String className = ci.getName();
					String methodName = mi.getName();
					int numberOfArgs = mi.getNumberOfArguments();
					//neha: changed invoked method to full name
					if (((BytecodeUtils.isClassSymbolic(conf, className, mi, methodName))
							|| BytecodeUtils.isMethodSymbolic(conf, mi.getFullName(), numberOfArgs, null))){
						//at the end of symbolic execution, set the values back
						//to their original value
						ss.retainAttributes(retainVal);
						ss.setForced(forcedVal);
						ChoiceGenerator <?>cg = vm.getChoiceGenerator();
						if (!(cg instanceof PCChoiceGenerator)){
							ChoiceGenerator <?> prev_cg = cg.getPreviousChoiceGenerator();
							while (!((prev_cg == null) || (prev_cg instanceof PCChoiceGenerator))) {
								prev_cg = prev_cg.getPreviousChoiceGenerator();
							}
							cg = prev_cg;
						}
						if ((cg instanceof PCChoiceGenerator) &&(
								(PCChoiceGenerator) cg).getCurrentPC() != null){
							PathCondition pc = ((PCChoiceGenerator) cg).getCurrentPC();
							pc.solve(); //we only solve the pc
							PathCondition result = new PathCondition();
							//after the following statement is executed, the pc loses its solution
							IntegerExpression sym_result = new SymbolicInteger("RETURN");
							//changed to get SMTLib format
							String pcString = pc.sExpressionPC();
							//after the following statement is executed, the pc loses its solution
							String returnString = "";
							Integer constraintCount = new Integer(pc.getSolverCalls()); //also # of constraints in a path condition
							
							
							if (insn instanceof IRETURN){
								returnString = processIReturn(ti, pc, result, insn, sym_result);
							}else if (insn instanceof DRETURN){
								System.out.println("IMPLEMENT ME: DRETURN");
							}else if (insn instanceof LRETURN){
								System.out.println("IMPLEMENT ME: LRETURN");
							}else if (insn instanceof FRETURN){
								System.out.println("IMPLEMENT ME: FRETURN");
							}else if (insn instanceof ARETURN){
								returnString = processAReturn(ti,pc, result,insn,sym_result);
							} else {
								returnString = processReturn(result);
							}
							System.out.println("returnString :" + returnString);
							
							if(solving_at_end) PathCondition.isReplay = false; //turn on constraint solving
							pc.solve();
							if(solving_at_end) PathCondition.isReplay = true; //turn off constraint solving
							
							//changed to get SMTLib format
							pcString = pc.sExpressionPC();
							int termCount = pc.termCount();
							
							String longName = mi.getLongName();
							MethodSummary methodSummary = allSummaries.get(longName);
							Vector<String> pcs = methodSummary.getPathConditions();
							//check on the first condition of the following if statement
							//sp: added a check for an empty pc string
							if (!pcs.contains(pcString) && !pcString.equalsIgnoreCase("# = 0")) {
							//if ((!pcs.contains(pcString)) && (pcString.contains("SYM"))) {
								//Vector<Integer> bcs = (Vector<Integer>)byteCodes.clone();
								//Vector<Integer> lns = (Vector<Integer>)lineNums.clone();
								Vector<Integer> bcs = new Vector<Integer>();
								Vector<Integer> lns = new Vector<Integer>();
								if (returnString.equalsIgnoreCase("")) {
									returnString = "IMPLEMENT ME?";
								}
								if(InterProcPruningListener.getDroppedCondNodes() != null) {
									ArrayList<String> funcNodes = InterProcPruningListener.getDroppedCondNodes();
									methodSummary.addPathCondition(pc.getConstraintHeader(), result.getConstraintHeader(),
											pcString, returnString, constraintCount, termCount, bcs, lns,funcNodes);
								}else
									methodSummary.addPathCondition(pc.getConstraintHeader(),  result.getConstraintHeader(),
												pcString, returnString, constraintCount, termCount, bcs, lns,null);
							}
							allSummaries.put(longName,methodSummary);
							//System.out.println(allSummaries.toString());
						}
					}
				}
				if (inSymbolicMethod){ //return from a symbolic method
					inSymbolicMethod = false;
					//lastByteCode=0;
					//lastInstruction = 0;
					//previousByteCodes = byteCodes;
					//byteCodes = new Vector<Integer>();
					//previousLineNums = lineNums;
					//lineNums = new Vector<Integer>();
				}
			}else{ //not an invoke or return instruction
				if (!inSymbolicMethod) {
					MethodInfo mi = insn.getMethodInfo();
					String cName = mi.getClassName();
					String mName = mi.getFullName();
					int numberOfArgs = mi.getNumberOfArguments();
					//make sure the method is not symbolic
					if ((BytecodeUtils.isClassSymbolic(conf, cName, mi, mName))
							|| BytecodeUtils.isMethodSymbolic(conf, mi.getFullName(), numberOfArgs, null)){
						setSymbolicEnvironment(ss);
						//if (lineNumLists.size()>0){
						//	lineNums = lineNumLists.pop();
						//	updateLineNums(insn);
						//	byteCodes = bcLists.pop();
						//	updateByteCodePositions(insn);
						//}else{
						//	lineNums = new Vector<Integer>();
						//	byteCodes = new Vector<Integer>();
						//}
					}
				}
				if (inSymbolicMethod){
					//if (insn.getLineNumber() != lastInstruction){
					//	lineNums.add(new Integer(insn.getLineNumber()));
					//	lastInstruction = insn.getLineNumber();
					//}
					//if (insn.getPosition() != lastByteCode){
					//	byteCodes.add(new Integer(insn.getPosition()));
					//	lastByteCode = insn.getPosition();
					//}
				}
			}
		}
	}

	private void updateLineNums(Instruction insn){
		int currentInstruction = insn.getLineNumber();
		boolean found = false;
		if (lineNums.size() == 0){
			int index = previousLineNums.size() - 1;
			while (!found && index >= 0){
				if (previousLineNums.get(index).intValue() != currentInstruction)
					previousLineNums.remove(index);
				else{
					previousLineNums.remove(index);
					found = true;
					lineNums.addAll(previousLineNums);
				}
				index--;
			}
		}else{
			int index = lineNums.size() - 1;
			while (!found && index >= 0){
				if (lineNums.get(index).intValue() != currentInstruction)
					lineNums.remove(index);
				else{
					lineNums.remove(index);
					found = true;
				}
				index--;
			}
		}
	}

	/*
	 * same thing for bytecode positions
	 */
	private void updateByteCodePositions(Instruction insn){
		int currentPos = insn.getPosition();
		boolean found = false;
		if (byteCodes.size() == 0){
			int index = previousByteCodes.size() - 1;
			while (!found && index >= 0){
				if (previousByteCodes.get(index).intValue() != currentPos)
					previousByteCodes.remove(index);
				else{
					previousByteCodes.remove(index);
					found = true;
					byteCodes.addAll(previousByteCodes);
				}
				index--;
			}
		}else{
			int index = byteCodes.size() - 1;
			while (!found && index >= 0){
				if (byteCodes.get(index).intValue() != currentPos)
					byteCodes.remove(index);
				else{
					byteCodes.remove(index);
					found = true;
				}
				index--;
			}
		}
	  }

	  public void stateBacktracked(Search search) {
		  VM vm = search.getVM();
		  Config conf = vm.getConfig();

		  Instruction insn = vm.getChoiceGenerator().getInsn();
		  SystemState ss = vm.getSystemState();
		  MethodInfo mi = insn.getMethodInfo();
		  String className = mi.getClassName();
		  //neha: changed the method name to full name
		  String methodName = mi.getFullName();
		  //this method returns the number of slots for the arguments, including "this"
		  int numberOfArgs = mi.getNumberOfArguments();

		  if ((BytecodeUtils.isClassSymbolic(conf, className, mi, methodName))
					|| BytecodeUtils.isMethodSymbolic(conf, methodName, numberOfArgs, null)){
			//get the original values and save them for restoration after
			//we are done with symbolic execution
			retainVal = ss.getRetainAttributes();
			forcedVal = ss.isForced();
			//turn off state matching
			ss.setForced(true);
			//make sure it stays turned off when a new state is created
			ss.retainAttributes(true);
			inSymbolicMethod = true;
		  }
	  }


	  /*
	   *  todo: needs to be implemented if we are going to support heuristic search
	   */
	  public void stateRestored(Search search) {
		  System.err.println("Warning: State restored - heuristic search not supported");
	  }
	  /*
	   * Save the method summaries to a file for use by others
	   */
	  public void searchFinished(Search search) {
//		  System.out.println("# of infeasible paths: " + PathCondition.infeasibleCount);
	  }

	  /*
	   * The way this method works is specific to the format of the methodSummary
	   * data structure
	   */

	  //TODO:  needs to be changed not to use String representations
	  private void printMethodSummary(PrintWriter pw, MethodSummary methodSummary,
			  boolean firstTime){
		  //System.out.println("Symbolic values: " +methodSummary.getSymValues());
		  int totalConstraints = 0;
		  int totalDropped = 0;
		  int solverCalls = 0;
		  int totalTermCount = 0;
		  int PCs = 0;
		  int totalTC = 0;
		  int min = 0;
		  int max = 0;
		  
		  Vector<String> pathConditions = methodSummary.getPathConditions();
		  String mName = methodSummary.getMethodName();
		  PCs = pathConditions.size();
		  if (PCs > 0){
			  Iterator<String> it = pathConditions.iterator();
			  String allTestCases = "";
			  while(it.hasNext()){
				  String testCaseString = methodSummary.getMethodName() + "(";
				  String pc = it.next();
				  String symValues = methodSummary.getSymValues();
				  String argValues = methodSummary.getArgValues();
				  String argTypes = methodSummary.getArgTypes();
				  StringTokenizer st = new StringTokenizer(symValues, ",");
				  StringTokenizer st2 = new StringTokenizer(argValues, ",");
				  StringTokenizer st3 = new StringTokenizer(argTypes, ",");
				  while(st2.hasMoreTokens()){
					  String token = "";
					  String actualValue = st2.nextToken();
					  String actualType = st3.nextToken();
					  if (st.hasMoreTokens())
						  token = st.nextToken();
					  if (pc.contains(token)){
						  String temp = pc.substring(pc.indexOf(token));
						  String val = temp.substring(temp.indexOf("[")+1,temp.indexOf("]"));
						  if (actualType.equalsIgnoreCase("int") ||
								  actualType.equalsIgnoreCase("float") ||
								  actualType.equalsIgnoreCase("long") ||
								  actualType.equalsIgnoreCase("double"))
							  testCaseString = testCaseString + val + ",";
						  else{ //translate boolean values represented as ints
							  //to "true" or "false"
							  if (val.equalsIgnoreCase("0"))
								  testCaseString = testCaseString + "false" + ",";
							  else
								  testCaseString = testCaseString + "true" + ",";
						  }
					  }else{
						  //need to check if value is concrete
						  if (token.contains("CONCRETE"))
							  testCaseString = testCaseString + actualValue + ",";
						  else
							  testCaseString = testCaseString + "don't care,";
					  }
				  }
				  if (testCaseString.endsWith(","))
					  testCaseString = testCaseString.substring(0,testCaseString.length()-1);
				  testCaseString = testCaseString + ")";
				  
				  String s1 = pc.substring(pc.indexOf("\n")+1);
				  int terms = s1.split("&&").length;

				  testCaseString = "testCase=" + testCaseString + ";" +
				  	"pc=" + pc.substring(pc.indexOf("\n")+1) + ";" +
				  	"effects=" + methodSummary.getRetVal(pc) + ";";

				  Integer count = methodSummary.getSolverCallCount(pc);
				  if (count > 0){
					  if (count > max)
						  max = count;
					  if (min == 0 || count < min)
						  min = count;
					  solverCalls = solverCalls + count;
					  testCaseString = testCaseString + "solverCalls=" + count + ";";
				  }
				  
				  Integer termCount = methodSummary.getTermCount(pc);
				  totalTermCount = totalTermCount + termCount;
				  
				  testCaseString = testCaseString + "statementsExecuted=" +
				  "[];"+
				  	//methodSummary.getLineNums(pc) + ";" +
				  	//"positionsExecuted=" + methodSummary.getBytecodes(pc);
				  "positionsExecuted=[]";
				  
				  List<String> constraints = methodSummary.getListOfConstraints(pc);
				  if (constraints != null){
					  String s = constraints.toString();
					  testCaseString = testCaseString + ";" + "constraints=" +
					  	s;
				  }else
					  testCaseString = testCaseString + ";" + "constraints=" +
					  	"none";
				  int drops = 0;
				  if(constraints != null)
					  drops = constraints.size();
				  totalDropped = totalDropped + drops;
				  totalConstraints = totalConstraints + terms - drops;

				  //do not add duplicate test case
				  // System.out.println("TESTCASE: " + testCase);
				  if (!allTestCases.contains(testCaseString)){
					  allTestCases = allTestCases + "\n" + testCaseString;
					  totalTC++;
				  }else
					  System.out.println("Dropping test: " + testCaseString);
			  }
			  
			  if (printStats){
				  DecimalFormat twoDForm = new DecimalFormat("#.##");
			        
				  //pw.println(allTestCases);
				  pw.println("Stats for method..." + mName + ":");
				  pw.println("Total Number of solver calls: " + solverCalls);
				  pw.println("Total Number of constraints: " + totalConstraints);
				  pw.println("Total Number of dropped constraints: " + totalDropped);
				  
				  pw.println("----->Min Number of solver calls/PC =" + min);
				  pw.println("----->Max Number of solver calls/PC =" + max);
				  pw.println("---->Total Number of solver calls = " + solverCalls);
				  double averageSolverCalls = (double)solverCalls / PCs;
				  pw.println("---->Average number of solver calls = " + Double.valueOf(twoDForm.format(averageSolverCalls)));
				  pw.println("---->Total Number of terms: " + totalTermCount);
				  double averageTermCount = (double)totalTermCount / PCs;
				  pw.println("---->Average Number of terms: " + Double.valueOf(twoDForm.format(averageTermCount)));
				  pw.println("---->Total Number of test cases = " + totalTC + "(" + PCs + ")");
				  pw.println("---->Total Number of infeasible paths = " + PathCondition.infeasibleCount);
			  }

			  if(!testFile.equals("")) {
				  //only save the path conditions for the target method for now
				  if (symMethods.contains(mName));
					  writeToFile(testFile, allTestCases,firstTime);
			  }
		  }else{
			  pw.println("No path conditions for " + methodSummary.getMethodName() +
					  "(" + methodSummary.getArgValues() + ")");
		  }
	  }

	  /*
	   * Saves the test cases (solved path conditions) to the specified file
	   * (Just one file for now)
	   */
	  public void writeToFile(String fileName, String allTestCases, boolean firstTime) {
		  FileWriter out = null;
		  File file = new File(fileName);
		  //this is kludgy at best (may not be V0V1 and may overwrite V0 w/ V1)
		  if (file.isDirectory()) {
			  String tmpName = mainClassName.substring(mainClassName.lastIndexOf(".")+1);
			  if (runningDiSE)
				  tmpName = fileName + File.separator + tmpName + "TestV0V1.txt";
			  else
				  tmpName = fileName + File.separator + tmpName + "TestSymbc.txt";
			  file = new File(tmpName);
		  }
		  try {
			  	if (firstTime)
			  		out = new FileWriter(fileName,false);
			  	else
			  		out = new FileWriter(fileName,true);
				//output = new BufferedWriter(new FileWriter(file));

			} catch (IOException e) {
				System.err.println("error while creating the file to write");
				e.printStackTrace();
			}

			try {
				out.write(allTestCases);
				//output.write(allTestCases);
			} catch (IOException e) {
				System.err.println("Error while writing to the XML file");
				e.printStackTrace();
			}

			try {
				out.close();
				//output.close();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	  }

	  /*
	   * Not recently tested
	   */
	  private void printMethodSummaryHTML(PrintWriter pw, MethodSummary methodSummary){
		  pw.println("<h1>Test Cases Generated by Symbolic Java Path Finder for " +
				  methodSummary.getMethodName() + " (Path Coverage) </h1>");

		  Vector<String> pathConditions = methodSummary.getPathConditions();
		  if (pathConditions.size() > 0){
			  Iterator<String> it = pathConditions.iterator();
			  String allTestCases = "";
			  String symValues = methodSummary.getSymValues();
			  StringTokenizer st = new StringTokenizer(symValues, ",");
			  while(st.hasMoreTokens())
				  allTestCases = allTestCases + "<td>" + st.nextToken() + "</td>";
			  allTestCases = "<tr>" + allTestCases + "</tr>\n";
			  while(it.hasNext()){
				  String testCase = "<tr>";
				  String pc = it.next();
				  String retVal = methodSummary.getRetVal(pc);
				  String argValues = methodSummary.getArgValues();
				  String argTypes = methodSummary.getArgTypes();
				  st = new StringTokenizer(symValues, ",");
				  StringTokenizer st2 = new StringTokenizer(argValues, ",");
				  StringTokenizer st3 = new StringTokenizer(argTypes, ",");
				  while(st2.hasMoreTokens()){
					  String token = "";
					  String actualValue = st2.nextToken();
					  String actualType = st3.nextToken();
					  if (st.hasMoreTokens())
						  token = st.nextToken();
					  if (pc.contains(token)){
						  String temp = pc.substring(pc.indexOf(token));
						  String val = temp.substring(temp.indexOf("[")+1,temp.indexOf("]"));
						  if (actualType.equalsIgnoreCase("int") ||
								  actualType.equalsIgnoreCase("float") ||
								  actualType.equalsIgnoreCase("long") ||
								  actualType.equalsIgnoreCase("double"))
							  testCase = testCase + "<td>" + val + "</td>";
						  else{ //translate boolean values represented as ints
							  //to "true" or "false"
							  if (val.equalsIgnoreCase("0"))
								  testCase = testCase + "<td>false</td>";
							  else
								  testCase = testCase + "<td>true</td>";
						  }
					  }else{
						  //need to check if value is concrete
						  if (token.contains("CONCRETE"))
							  testCase = testCase + "<td>" + actualValue + "</td>";
						  else
							  testCase = testCase + "<td>don't care</td>";
					  }
				  }

				  if (!retVal.equalsIgnoreCase(""))
					  testCase = testCase + "<td>" + retVal + "</td>";
				  //do not add duplicate test case
				  if (!allTestCases.contains(testCase))
					  allTestCases = allTestCases + testCase + "</tr>\n";
			  }
			  pw.println("<table border=1>");
			  pw.print(allTestCases);
			  pw.println("</table>");
		  }else{
			  pw.println("No path conditions for " + methodSummary.getMethodName() +
					  "(" + methodSummary.getArgValues() + ")");
		  }

	  }

      //	-------- the publisher interface
	  public void publishFinished (Publisher publisher) {
		String[] dp = SymbolicInstructionFactory.dp;
		if (dp[0].equalsIgnoreCase("no_solver") || dp[0].equalsIgnoreCase("cvc3bitvec"))
				return;

	    PrintWriter pw = publisher.getOut();
	    if (printTestCases){
		    publisher.publishTopicStart("Method Summaries");
		    Iterator it = allSummaries.entrySet().iterator();
		    boolean firstTime = true;
		    while (it.hasNext()){
		    	Map.Entry me = (Map.Entry)it.next();
		    	MethodSummary methodSummary = (MethodSummary)me.getValue();
			    printMethodSummary(pw, methodSummary,firstTime);
			    firstTime = false;
		    }
		    if (htmlOutput){
		    	publisher.publishTopicStart("Method Summaries (HTML)");
		    	it = allSummaries.entrySet().iterator();
			    while (it.hasNext()){
			    	Map.Entry me = (Map.Entry)it.next();
			    	MethodSummary methodSummary = (MethodSummary)me.getValue();
			    	printMethodSummaryHTML(pw, methodSummary);
			    }
		    }
	    }
	  }
}
	 