package gov.nasa.jpf.regression.listener;

import static gov.nasa.jpf.regression.data.Operator.MONITORENTER;
import static gov.nasa.jpf.regression.data.Operator.MONITOREXIT;
import static gov.nasa.jpf.regression.data.Operator.READ;
import static gov.nasa.jpf.regression.data.Operator.WRITE;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.JPF;
import gov.nasa.jpf.PropertyListenerAdapter;
import gov.nasa.jpf.jvm.bytecode.JVMInstruction;
import gov.nasa.jpf.vm.ElementInfo;
import gov.nasa.jpf.vm.VM;
import gov.nasa.jpf.vm.ThreadInfo;
import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.regression.data.GlobalOperation;
import gov.nasa.jpf.regression.data.GlobalOperationInstructionVisitor;
import gov.nasa.jpf.regression.data.StaticDataStructures;
import gov.nasa.jpf.regression.output.PrintToDot;
import gov.nasa.jpf.regression.output.PrintToXML;
import gov.nasa.jpf.report.ConsolePublisher;
import gov.nasa.jpf.report.PublisherExtension;
import gov.nasa.jpf.search.Search;
public class DependenceListener extends PropertyListenerAdapter 
										implements PublisherExtension {
	
	Vector<GlobalOperation> nameLst; // this is the stack
	HashMap<String, Integer> dNodes;
	HashMap<Integer, GlobalOperation> operations;
	HashMap<Integer, ArrayList<Integer>> dEdges;
	ElementInfo lastLocked;
	static int counter = 0;
		
	public DependenceListener(Config conf, JPF jpf) {
		jpf.addPublisherExtension(ConsolePublisher.class, this);
	}
	

	public void stateBacktracked(Search search) {
		int size = nameLst.size();
		if(size > 0) {
			int index = size-1;
			nameLst.remove(index);
		}
	}
	
	public void stateProcessed(Search search) {
	}
	
	public void stateAdvanced (Search search) {
		VM vm = search.getVM();
		ThreadInfo current = vm.getCurrentThread();
		GlobalOperation gOp;
		if(current.getPC() != null) {// && current.isRunnable()) {
			Instruction ins = current.getPC();
			gOp = new GlobalOperation(current.getClassInfo().getName(),
							current.getTopFrameMethodInfo().getUniqueName(), 
							Integer.toString(current.getPC().getPosition()),
							current.getId());
			StaticDataStructures.setVals(search, gOp);
			((JVMInstruction)ins).accept(new GlobalOperationInstructionVisitor());
			nameLst.add(gOp);
			
			if(!current.holdsLock(current.getElementInfo(gOp.getObjRef()))) {
				//the locking operations will be handled
				// after the lock has been acquired or released
				if(gOp.getObjRef() != -1) 
					addDependencyEdge(gOp);
			}
		} else {
			gOp = new GlobalOperation(null,null,null,-1);
			nameLst.add(gOp);
		}	
	}
	
	private void addDependencyEdge(GlobalOperation currOp) {
		int size = nameLst.size() - 2; //skip the first element
		int objRef = currOp.getObjRef();
		for(int lstIndex = size; lstIndex >= 0; lstIndex--) {
			GlobalOperation firstOp = nameLst.get(lstIndex);
			//TODO: this could be a list
			GlobalOperation firstOpNext = firstOp.getSecondaryOperation(); 
			if(firstOpNext != null) {
				boolean edge = addingEdge(firstOpNext, currOp, objRef);
				if(edge)  {return;}
			}
			boolean edge = addingEdge(firstOp, currOp,  objRef);
			if(edge) {return;}
		}
	}
	
	private boolean addingEdge(GlobalOperation firstOp, GlobalOperation currOp, int objRef) {
	
		int fObjRef = firstOp.getObjRef();
		if(fObjRef == objRef) {

			//if the objrefs are the same and we found the same 
			// threads ids. If there are other threads that affect
			// the same references, it should affect the previous
			// operation before affecting the current one. 
			if(currOp.getThreadId() == firstOp.getThreadId()) {
				return true;
			}
			if(checkEdgeValidity(currOp, firstOp)) {
				//System.out.println(firstOp.getUniqueId() + " ==>" + currOp.getUniqueId());
				if(!dNodes.containsKey(firstOp.getUniqueId())) {
					dNodes.put(firstOp.getUniqueId(),
							DependenceListener.counter);
					DependenceListener.counter++;
				}
				if(!dNodes.containsKey(currOp.getUniqueId())) {
					dNodes.put(currOp.getUniqueId(),
							DependenceListener.counter);
					DependenceListener.counter++;
				}
				int parentVal = dNodes.get(firstOp.getUniqueId());
				int childVal = dNodes.get(currOp.getUniqueId());

				if(!operations.containsKey(parentVal)) {
					operations.put(parentVal, firstOp);
				}
				if(!operations.containsKey(childVal)) {
					operations.put(childVal, currOp);
				}

				ArrayList<Integer> childNodes;
				if(dEdges.containsKey(parentVal)) {
					childNodes = dEdges.get(parentVal);
				} else {
					childNodes = new ArrayList<Integer>();
				}
				if(!childNodes.contains(childVal) && parentVal != childVal) {
					childNodes.add(childVal);
				}
				dEdges.put(parentVal, childNodes);
				return true;
			}	
		}
		return false;	
	}
	
	private boolean checkEdgeValidity(GlobalOperation op1, GlobalOperation op2) {
		
		if((op1.getOperator() == MONITOREXIT || op1.getOperator() == MONITORENTER) &&
				(op1.getOperator() == MONITOREXIT || op2.getOperator() == MONITORENTER)) {
			return true;
		} else if((op1.getOperator() == MONITORENTER || op1.getOperator() == MONITOREXIT) &&
				(op2.getOperator() == READ || op2.getOperator() == WRITE)) {
			return true;
		} else if((op1.getOperator() == READ || op1.getOperator() == WRITE) &&
				(op2.getOperator() == MONITORENTER || op2.getOperator() == MONITOREXIT)) {
			return true;
		}
		else if(op1.getOperator() != READ || op2.getOperator() != READ) {
			return true;
		}
		return false;
	}
	

	 
	public void searchStarted(Search search) {
		nameLst = new Vector<GlobalOperation>();
		dNodes = new HashMap<String, Integer>();
		operations = new HashMap<Integer, GlobalOperation>();
		dEdges = new HashMap<Integer, ArrayList<Integer>>();
	}
	
	public void searchFinished(Search search) {
		System.out.println("The search is finished");
		System.out.println(dNodes.toString());
		System.out.println(dEdges.toString());
		Config conf = search.getConfig();
		String outputDir = conf.getString("output.loc", "");
		if(!outputDir.equals("")) {
			outputDir = DependenceListener.replaceWithFileSeperator(outputDir);
			PrintToXML xml = new PrintToXML();
			String xmlOut = outputDir.concat(File.separator).concat("out.xml");
			xml.printOutput(dNodes, operations, dEdges, xmlOut);
			PrintToDot dot = new PrintToDot();
			String dotOut = outputDir.concat(File.separator).concat("out.dot");
			dot.printOutput(dNodes, operations, dEdges, dotOut);
		}
	}
	
	public void objectLocked (VM vm, ThreadInfo currentThread, ElementInfo lockedObject) {
		addLockingInformation(lockedObject.getObjectRef(),
							"monitorenter", vm,currentThread);
	}
	
	public static String replaceWithFileSeperator(String orgString) {
		if(orgString.contains("/")) {
			return orgString.replace("/", File.separator);
		} else if(orgString.contains("\\")) {
			return orgString.replace("\\", File.separator);
		}
			return orgString;
	}
	
	
	public void objectUnlocked (VM vm, ThreadInfo currentThread, 
			ElementInfo unlockedObject) {
		
		int objRef = unlockedObject.getObjectRef();
		
		boolean lockAdded = addLockingInformation(objRef,
							"monitorexit", vm, currentThread);
		if(lockAdded) return;
		//System.out.println("the lock was not added");
		if(nameLst.size() <= 0) return;

		//find a corresponding monitorenter for the objRef
		//this is to handle the case where there was no 
		// break point at the monitorexit for some reason
		ThreadInfo last = currentThread;

		int lstSize = nameLst.size() - 1;
		for(int lstIndex = lstSize; lstIndex >= 0; lstIndex--) {
			GlobalOperation gOp = nameLst.get(lstIndex);
			if(gOp.getObjRef() != objRef ||
					gOp.getOperator() != MONITORENTER) continue;
			if(gOp.getThreadId() != last.getId()) break;
			GlobalOperation prevOp = nameLst.get(lstSize);
			prevOp.initializeNextOperation(last.getPC().getMethodInfo().getClassName(),
					last.getPC().getMethodInfo().getUniqueName(),
					Integer.toString(last.getPC().getPosition()),
					last.getId());
			GlobalOperation nextPrevOp = prevOp.getSecondaryOperation();
			nextPrevOp.setUnlockOperation();
			nextPrevOp.setObjRef(objRef);
			break;
		} 
	}
	
	
	
	private boolean addLockingInformation(int objRef, String val, VM vm,
			ThreadInfo currentThread) {
		if(nameLst.size() <= 0) return false;

		int index = nameLst.size() - 1;
		GlobalOperation gOp = nameLst.get(index);
		ThreadInfo last = currentThread;

		if(last.getPC().getMethodInfo().getClassName().
				equals(gOp.getClassName()) &&
				last.getPC().getMethodInfo().getUniqueName().
				equals(gOp.getMethodName()) &&
				Integer.toString(last.getPC().getPosition()).
				equals(gOp.getPosition())) {
			gOp.setObjRef(objRef);
			gOp.setReadWriteVal(objRef + ":=" + val + "(" + objRef + ")");
			if(val.equals("monitorenter")) {
				gOp.setLockOperation();
			} else if (val.equals("monitorexit")) {
				gOp.setUnlockOperation();
			}
			addDependencyEdge(gOp);
			return true;

		} 
		return false;
	}
		 
	public void objectWait (VM vm, ThreadInfo ti, ElementInfo ei){}
	  
	public void objectNotify (VM vm) {}
	
	public void objectNotifyAll (VM vm) {}
	
}