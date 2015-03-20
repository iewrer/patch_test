//
//Copyright (C) 2007 United States Government as represented by the
//Administrator of the National Aeronautics and Space Administration
//(NASA).  All Rights Reserved.
//
//This software is distributed under the NASA Open Source Agreement
//(NOSA), version 1.3.  The NOSA has been approved by the Open Source
//Initiative.  See the file NOSA-1.3-JPF at the top of the distribution
//directory tree for the complete NOSA document.
//
//THE SUBJECT SOFTWARE IS PROVIDED "AS IS" WITHOUT ANY WARRANTY OF ANY
//KIND, EITHER EXPRESSED, IMPLIED, OR STATUTORY, INCLUDING, BUT NOT
//LIMITED TO, ANY WARRANTY THAT THE SUBJECT SOFTWARE WILL CONFORM TO
//SPECIFICATIONS, ANY IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR
//A PARTICULAR PURPOSE, OR FREEDOM FROM INFRINGEMENT, ANY WARRANTY THAT
//THE SUBJECT SOFTWARE WILL BE ERROR FREE, OR ANY WARRANTY THAT
//DOCUMENTATION, IF PROVIDED, WILL CONFORM TO THE SUBJECT SOFTWARE.

package gov.nasa.jpf.regression.listener;

//import edu.byu.cs.guided.jpf.GuidedListener;
//import edu.byu.cs.guided.jpf.TrackLocations;
//import edu.byu.cs.guided.jpf.TrackOutputInstructionVisitor;
//import edu.byu.cs.guided.search.heuristic.GuidedDFSearch;
//import edu.byu.cs.search.heuristic.location.ProgramLocation;
//import edu.byu.cs.trace.sets.KeyLocation;
import gov.nasa.jpf.Config;
//import gov.nasa.jpf.JPF;
//import gov.nasa.jpf.PropertyListenerAdapter;
//import gov.nasa.jpf.vm.ChoiceGenerator;
//import gov.nasa.jpf.vm.ClassInfo;
//import gov.nasa.jpf.vm.VM;
//import gov.nasa.jpf.vm.MethodInfo;
//import gov.nasa.jpf.vm.SystemState;
//import gov.nasa.jpf.vm.ThreadInfo;
//import gov.nasa.jpf.jvm.bytecode.IfInstruction;
//import gov.nasa.jpf.vm.Instruction;
//import gov.nasa.jpf.jvm.bytecode.JVMInvokeInstruction;
//import gov.nasa.jpf.vm.choice.IntIntervalGenerator;
import gov.nasa.jpf.regression.analysis.ComputeIntraProceduralDiff;
import gov.nasa.jpf.regression.ast.ASTLoader;
import gov.nasa.jpf.regression.ast.MethodASTInfo;
import gov.nasa.jpf.regression.bytecode.AffectedBlocks;
import gov.nasa.jpf.regression.cfg.CFG;
import gov.nasa.jpf.regression.cfg.Node;
import gov.nasa.jpf.regression.output.PrintStatistics;
import gov.nasa.jpf.regression.output.PrintToDot;
import gov.nasa.jpf.regression.output.PrintToXML;
import gov.nasa.jpf.regression.tasks.ClientTaskConfig;
//import gov.nasa.jpf.report.ConsolePublisher;
//import gov.nasa.jpf.report.PublisherExtension;
//import gov.nasa.jpf.search.Search;
//import gov.nasa.jpf.symbc.numeric.PCChoiceGenerator;
//import gov.nasa.jpf.symbc.numeric.PathCondition;



















import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.security.SignatureSpi;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

import javax.swing.text.html.HTMLDocument.Iterator;

import org.apache.bcel.classfile.Utility;
import org.eclipse.jdt.core.*;;

//public class PruningRSEListener extends PropertyListenerAdapter implements
//		PublisherExtension {

public class PruningRSEListener {
	private static final boolean debug = false;
//	private static HashMap<String, Integer> shortestPathInfo =
//			new HashMap<String, Integer>();
	
	//FIXME: tracking the branches of executed affected conditional
	// branches. 
	public static Set<Integer> trueBranches = new HashSet<Integer>();
	public static Set<Integer> falseBranhces = new HashSet<Integer>();

	//the following are used for reguression testing purposes
	protected int chgAffectedWCNodes = 0;
	protected int affectedWCNodes = 0;
	protected int chgAffectedWNodes = 0;
	protected int affectedWNodes = 0;
	protected int chgAffectedCNodes = 0;
	protected int affectedCNodes = 0;
	protected int chgNodes = 0;
	protected int chgCodeNodes = 0;
	protected int chgAssertNodes = 0;
	protected int unAffectedNodes = 0;
	protected int totalNodes = 0;

//	private boolean printCounts = false;
	private boolean checkIf = false;
	private boolean filterAffMode = true; //the filtering is turned on by default
	
//	private int pruningCount = 0; // keep track of pruning 
	
	
	
	Config conf;
	
	// currently executed method
	String methodName;

	// name of the class under execution
	String className;

	//support for using AST information
//	String shortMethodName = "";
	String astInfo="";
	//key=className+methodName value=MethodASTInfo
	Map<String,MethodASTInfo> methodASTInfo =
		new HashMap<String,MethodASTInfo>();
	String dotFile = "";
	String statFile = "";
//	boolean shortestPathCFG = false;
//	boolean shortestPathBytecode = false;
//	boolean randomChoice = false;
//	boolean post_dominance_check = false;

	Random random;
	CFG cfg;
	
//	public PruningRSEListener(Config conf, JPF jpf) {
	public PruningRSEListener(Config conf) {
		System.out.println("---------->Using PruningRSE Listener 6/3/2011");
		className = "";
		methodName="";
		this.conf = conf;
//		jpf.addPublisherExtension(ConsolePublisher.class, this);
		if (conf.containsKey("rse.ASTResults")){ //path to XML file
			astInfo = conf.getString("rse.ASTResults");
			System.out.println("astFile: "+astInfo);
			ASTLoader loader = new ASTLoader();
			methodASTInfo = loader.loadAST(astInfo);
		}

		if (conf.containsKey("rse.dotFile"))
			dotFile = conf.getString("rse.dotFile");
//		if (conf.containsKey("rse.printCounts"))
//			printCounts = conf.getBoolean("rse.printCounts");
		if (conf.containsKey("rse.statFile"))
			statFile = conf.getString("rse.statFile");
//		if(conf.containsKey("rse.shortest_path")) {
//			String val = conf.getString("rse.shortest_path");
//			if(val.equals("true")) {
//				shortestPathCFG = true;
//			}
//		}
//		if(conf.containsKey("rse.shortest_path_bytecode")) {
//			String val = conf.getString("rse.shortest_path_bytecode");
//			if(val.equals("true")) {
//				shortestPathBytecode = true;
//			}
//		}		
//		if(conf.containsKey("rse.random_choice")) {
//			String val = conf.getString("rse.random_choice");
//			if(val.equals("true")) {
//				randomChoice = true;
//			}
//		}
//		if(conf.containsKey("rse.post_dominance_check")) {
//			String val = conf.getString("rse.post_dominance_check");
//			if(val.equals("true")) {
//				post_dominance_check = true;
//			}
//		}
		if(conf.containsKey("rse.filterAffMode")) {
			String val = conf.getString("rse.filterAffMode");
			if(val.equals("true")) {
				filterAffMode = true;
			}
			if(val.equals("false")) {
				filterAffMode = false;
			}
		}
		
		String clientAnalysis = "ImpactDataFlowCoverage";
		if(conf.containsKey("client.analysis")) {
			System.out.println("the client analysis");
			clientAnalysis = conf.getString("client.analysis");
		}
		ClientTaskConfig taskConfig = new ClientTaskConfig(clientAnalysis);
		taskConfig.initialize();
		System.out.println(ClientTaskConfig.isImpactedBranchCoverage() + "branch coverage");
		
		random = new Random(System.currentTimeMillis());
	}

	public void printStatsToStdOut(){
		System.out.println("Number of changed AW/CN=" + chgAffectedWCNodes);
		System.out.println("Number of AW/CN=" + affectedWCNodes);
		System.out.println("Number of changed AWN=" + chgAffectedWNodes);
		System.out.println("Number of AWN=" + affectedWNodes);
		System.out.println("Number of changed ACN=" + chgAffectedCNodes);
		System.out.println("Number of ACN=" + affectedCNodes);
		System.out.println("Number of changed nodes=" + chgNodes);
		System.out.println("Number of changed code nodes=" + chgCodeNodes);
		System.out.println("Number of changed assert nodes=" + chgAssertNodes);
		System.out.println("Number of unaffected nodes=" + unAffectedNodes);
		System.out.println("Total number of nodes=" + totalNodes);
	}

	//supports regression testing
	private void setCounts(PrintToDot dot){
		if (dot != null){
			chgAffectedWCNodes = dot.chgAffectedWCNodes;
			affectedWCNodes = dot.affectedWCNodes;
			chgAffectedWNodes = dot.chgAffectedWNodes;
			affectedWNodes = dot.affectedWNodes;
			chgAffectedCNodes = dot.chgAffectedCNodes;
			chgCodeNodes = dot.chgCodeNodes;
			chgAssertNodes = dot.chgAssertNodes;
			affectedCNodes = dot.affectedCNodes;
			chgNodes = dot.chgNodes;
			unAffectedNodes = dot.unAffectedNodes;
			totalNodes = dot.totalNodes;
		}else
			System.out.println("WARNING: dot is null");
	}

	
	public void ComputeDiff() {
		String oldClass = conf.getString("rse.oldClass");
		String newClass = conf.getString("rse.newClass");
		java.util.Iterator<Entry<String, MethodASTInfo>> entries = methodASTInfo.entrySet().iterator();
		int index = 0;
		while (entries.hasNext()) {
			Map.Entry entry = (Map.Entry) entries.next();
			
			MethodASTInfo info = (MethodASTInfo) entry.getValue();
			String method = info.getClassName() + "." + info.getMethodName();
			String signature = Utility.methodTypeToSignature(info.getReturnType(), info.getParamTypes().toArray(new String[0]));
//			System.out.println(method);
//			System.out.println(signature);
			methodName = method + signature;
//			if (method.endsWith("resolve")) {
//				method.toString();
//			}
			
			if (	!info.getEquivalent()
//					!method.contains("init")&&
////					!method.contains("extractInt")&&
////					!method.contains("consumeDigits")&&
////					!method.contains("scanEscapeCharacter")&&
//					!method.contains("checkTaskTag")&&//runanalysis erro
//					!method.contains("getNextToken")&&
//					!method.contains("internalScanIdentifierOrKeyword")&&
//					//no such method
//					!method.contains("atTypeAnnotation")&&
//					!method.contains("ungetToken")&&
//					!method.contains("disambiguatedToken")&&
//					!method.contains("hasBeenReached")&&
//					!method.contains("getIdentityComparisonLines")&&
//					!method.contains("followSetOfCast")&&
//					!method.contains("setActiveParser")&&
//					!method.contains("VanguardParser")&&
//					!method.contains("isAtAssistIdentifier")&&
//					!method.contains("maybeAtReferenceExpression")&&
//					!method.contains("maybeAtEllipsisAnnotationsStart")&&
//					!method.contains("fastForward")&&
//					!method.contains("maybeAtLambdaOrCast")
//					!method.endsWith("accept") && 
//					!method.endsWith("resolve") && 
//					!method.endsWith("internalBeginToCompile") && 
//					!method.endsWith("compile")
					) {
//				System.out.println("computing:"+ methodName);
				ComputeIntraProceduralDiff cpd = null;
				try {
					/*
					 * 变更newclass和oldclass的位置，以修正获得的受影响的代码版本
					 */
					cpd = new ComputeIntraProceduralDiff(newClass, oldClass);
//					cpd = new ComputeIntraProceduralDiff(oldClass, newClass);
					cpd.computeChangedInfo(methodName, info, filterAffMode);
					
					AffectedBlocks.setAffectedBlock(methodName);
					
					//debug code for shortest_path
					cfg = cpd.getCFG(methodName, info);
//					
//					// debug
//					HashMap map = cfg.getNodeOffsetMap();
//					for(Object i: map.keySet()){
//						Object v = map.get(i);
//						System.out.println((Integer) i + ": " + ((Node)v).getID() + (Node)v);
//					}
				} catch (Exception e) {
					System.err.println("Exception during computing precise differences");
					System.exit(1);
				}
				if (!dotFile.equalsIgnoreCase("")){
					PrintToDot dot = new PrintToDot();
					System.out.println("printing dot...");
					System.out.println(info.getEquivalent());
					System.out.println(method);
					System.out.println(signature);
					try {
						printImpacted(dotFile + "." + (index++) + "." + info.getMethodName() + ".txt",
								cpd.getCFG(methodName,info),
								ComputeIntraProceduralDiff.
								getSemanticAnalysis(methodName).
								getGlobalTrackCondition(),
								ComputeIntraProceduralDiff.
								getSemanticAnalysis(methodName).
								getGlobalTrackWrite());
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					dot.printCFG(dotFile + "." + (index++) + "." + info.getMethodName() + ".dot",
								cpd.getCFG(methodName,info),
								ComputeIntraProceduralDiff.
								getSemanticAnalysis(methodName).
								getGlobalTrackCondition(),
								ComputeIntraProceduralDiff.
								getSemanticAnalysis(methodName).
								getGlobalTrackWrite());
					
					setCounts(dot);
					System.out.println("printing dot over!");
					
					Set<Integer> addedLines = info.getAddedLines();
					Set<Integer> chgedLines = info.getChangedLinesMod();
					VisualizationData.addAddededLocs(methodName, addedLines);
					VisualizationData.addChangedLocs(methodName, chgedLines);
					//end of visualization code
				}				
			}
		}
	}

	private void printImpacted(String file, CFG cfg,
			Set<Integer> trackCondLoc, Set<Integer> trackWriteLoc) throws IOException {
		// TODO Auto-generated method stub
		BufferedWriter writer = new BufferedWriter(new FileWriter(new File(file)));
		Node[] nodes = cfg.getNodes();
		Set<Integer> cond = new HashSet<Integer>();
		Set<Integer> write = new HashSet<Integer>();
		Map<Integer, Node> condNode = new HashMap<>();
		Map<Integer, Node> writeNode = new HashMap<>();
		for (int i=0; i<nodes.length;i++) {
			writer.write(nodes[i].getStartLineNumber() + "," + nodes[i].getEndLineNumber() + "\n");
			if (trackCondLoc != null){
				java.util.Iterator<Integer> it1 = trackCondLoc.iterator();
				while (it1.hasNext()){
					Integer loc = it1.next();
					if (loc >= nodes[i].getStartOffset() && loc <= nodes[i].getEndOffset()) {
						cond.add(loc);
						condNode.put(loc, nodes[i]);
					}
					
				}
			}
			if (trackWriteLoc != null){
				java.util.Iterator<Integer> it2 = trackWriteLoc.iterator();
				while (it2.hasNext()){
					Integer loc = it2.next();
					if (loc >= nodes[i].getStartOffset() && loc <= nodes[i].getEndOffset()) {
						write.add(loc);
						writeNode.put(loc, nodes[i]);
					}
				}
			}			
		}
		if (cond != null){
			java.util.Iterator<Integer> it1 = cond.iterator();
			while (it1.hasNext()){
				Integer loc = it1.next();
				writer.write("Condition Loc:" + loc + "\n");
				writer.write(condNode.get(loc).getStartLineNumber() + "," + condNode.get(loc).getEndLineNumber() + "\n");
			}
		}
		if (write != null){
			java.util.Iterator<Integer> it2 = write.iterator();
			while (it2.hasNext()){
				Integer loc = it2.next();
				writer.write("Write Loc:" + loc + "\n");
				writer.write(writeNode.get(loc).getStartLineNumber() + "," + writeNode.get(loc).getEndLineNumber() + "\n");
			}
		}		
		writer.close();
	}
			
	
	/*
	@Override
	public void instructionExecuted(VM vm, ThreadInfo currentThread,
			  Instruction nextInstruction, Instruction executedInstruction) {
//		Instruction lastInsn = executedInstruction;
//		ThreadInfo ti = currentThread;
//		Config conf = vm.getConfig();
//		
//		Instruction currPC = currentThread.getPC();
//		if(shortestPathCFG)
//			checkShortestPathCFG(currPC);
//		if(shortestPathBytecode)
//			checkShortestPathBytecode(currPC);
		
		//for debug
//		if(cfg!=null){
//			MethodInfo mi = lastInsn.getMethodInfo();
//			if(mi.getUniqueName().contains("alt_sep_test")){
//				Node n = (Node)cfg.getNodeOffsetMap().get(lastInsn.getPosition());
//				if(n!=null)
//					System.out.println(n.getID());
//			}
//		}
		
		if (lastInsn instanceof JVMInvokeInstruction) {
			JVMInvokeInstruction invInst = (JVMInvokeInstruction) lastInsn;

			MethodInfo m = invInst.getInvokedMethod();
			ClassInfo ci = m.getClassInfo();
			String cName = ci.getName();

			String oldClass = conf.getString("rse.oldClass");
			String newClass = conf.getString("rse.newClass");
			
			if(!methodName.equals(cName + "." + m.getUniqueName())){
				className = cName;

				methodName = cName + "." + m.getUniqueName();
				shortMethodName = cName + "." + m.getName();
				String[] argNames = m.getArgumentTypeNames();
				String argId  = "";
				//building the custom method signature identifier based on
				// the arguments passed to the method.
				if(argNames != null) {
					for(int argIndex =0; argIndex <
					argNames.length; argIndex++) {
						argId = argId.concat(argNames[argIndex]);
					}
				}
				String alternateMethodName = shortMethodName.concat(argId);

				//FIXME -- right now this is a hack to get the existing examples to work
				// with AST files that have short method names as the lookup key
				// they should all be using the full qualified method name that contains
				// the method signature
				if(methodASTInfo.containsKey(shortMethodName) ||
						methodASTInfo.containsKey(alternateMethodName)) {
					ComputeIntraProceduralDiff cpd = null;
					try {
						cpd = new ComputeIntraProceduralDiff(newClass, oldClass);
						if(methodASTInfo.containsKey(alternateMethodName))
							cpd.computeChangedInfo(methodName, methodASTInfo.get(alternateMethodName), filterAffMode);
						else
							cpd.computeChangedInfo(methodName, methodASTInfo.get(shortMethodName), filterAffMode);
						AffectedBlocks.setAffectedBlock(methodName);
						
						//debug code for shortest_path
						cfg = cpd.getCFG(methodName, methodASTInfo.get(shortMethodName));
//						
//						// debug
//						HashMap map = cfg.getNodeOffsetMap();
//						for(Object i: map.keySet()){
//							Object v = map.get(i);
//							System.out.println((Integer) i + ": " + ((Node)v).getID() + (Node)v);
//						}
					} catch (Exception e) {
						System.err.println("Exception during computing precise differences");
						System.exit(1);
					}
			/*		if (!statFile.equalsIgnoreCase("")){
						PrintToXML xml = new PrintToXML();
						xml.printXMLFile(statFile,srcPath,bcPath,shortMethodName,
								cName,
								methodASTInfo.get(shortMethodName),
								cpd.getCFG(methodName,
										methodASTInfo.get(shortMethodName)),
										ComputeIntraProceduralDiff.
										getSemanticAnalysis(methodName).
										getGlobalTrackCondition(),
										ComputeIntraProceduralDiff.
										getSemanticAnalysis(methodName).
										getGlobalTrackWrite());
					}/*
					if (!dotFile.equalsIgnoreCase("")){
						PrintToDot dot = new PrintToDot();
						dot.printCFG(dotFile,
								cpd.getCFG(methodName,
										methodASTInfo.get(shortMethodName)),
										ComputeIntraProceduralDiff.
										getSemanticAnalysis(methodName).
										getGlobalTrackCondition(),
										ComputeIntraProceduralDiff.
										getSemanticAnalysis(methodName).
										getGlobalTrackWrite());
						setCounts(dot);
						//visualization code
						String mName = shortMethodName;
						MethodASTInfo minfo = methodASTInfo.get(mName);
						if (minfo == null)
							mName = alternateMethodName;
						minfo = methodASTInfo.get(mName);
						Set<Integer> addedLines = methodASTInfo.get(mName).getAddedLines();
						Set<Integer> chgedLines = methodASTInfo.get(mName).getChangedLinesMod();
						VisualizationData.addAddededLocs(mName, addedLines);
						VisualizationData.addChangedLocs(mName, chgedLines);
						//end of visualization code
					}
					if (printCounts || !statFile.equalsIgnoreCase("")){
						PrintStatistics stats = new PrintStatistics();
						stats.computeStats(cpd.getCFG(methodName,
								methodASTInfo.get(shortMethodName)),
								cpd.getOriginalCFG(methodName,
										methodASTInfo.get(shortMethodName)),
										ComputeIntraProceduralDiff.
										getSemanticAnalysis(methodName).
										getGlobalTrackCondition(),
										ComputeIntraProceduralDiff.
										getSemanticAnalysis(methodName).
										getGlobalTrackWrite());
						if (printCounts)
							printStatsToStdOut();
						if (!statFile.equalsIgnoreCase(""))
							stats.printCFGInfo(statFile);
					}
				} else {
					if (debug)
						System.out.println("no AST diff info for " + shortMethodName);
				}
			}
		}
		//add to the position
		if(!ti.isFirstStepInsn()) {
		AffectedBlocks.checkPos(lastInsn.getMethodInfo().getFullName(),
				lastInsn.getPosition());
		addingAdditionalInfo(lastInsn);
		}

	}
	*/
	
	/*
	protected void addingAdditionalInfo(Instruction lastInsn) {
		String methodName = lastInsn.getMethodInfo().getFullName();
		if (!AffectedBlocks.impactedMethod(methodName)) return;
		if(!(lastInsn instanceof IfInstruction)) return;
		IfInstruction ifInsn = (IfInstruction) lastInsn;
		if(ifInsn.getConditionValue()) {
			// false branch taken
			PruningRSEListener.falseBranhces.add(ifInsn.getPosition());
		} else {
			PruningRSEListener.trueBranches.add(ifInsn.getPosition());
		}
	}
	*/
/*
	protected void checkShortestPathCFG(Instruction insn) {
		if(insn == null || !(insn instanceof IfInstruction))
				return;
		String mName = insn.getMethodInfo().getFullName();
		IfInstruction ifInsn = (IfInstruction) insn;

		Instruction target = ifInsn.getTarget();
		Instruction next = ifInsn.getNext();
		int targetDist = AffectedBlocks.getShortestPathInfoCFG
									(mName, target.getPosition(), ifInsn.getPosition());
		if(targetDist == -1) return;
		int nextDist = AffectedBlocks.getShortestPathInfoCFG
									(mName, next.getPosition(), ifInsn.getPosition());

		if(targetDist < nextDist && targetDist != Integer.MAX_VALUE) {
			String index = mName.concat(Integer.
									toString(ifInsn.getPosition()));
			//reverse order of choice generator
			shortestPathInfo.put(index, nextDist);
		}

	}

	protected void checkShortestPathBytecode(Instruction insn) {
		if(insn == null || !(insn instanceof IfInstruction))
				return;
		String mName = insn.getMethodInfo().getFullName();
		IfInstruction ifInsn = (IfInstruction) insn;

		Instruction target = ifInsn.getTarget();
		Instruction next = ifInsn.getNext();
		int targetDist = AffectedBlocks.getShortestPathInfoBytecode
									(mName, target.getPosition(), ifInsn.getPosition());
		if(targetDist == -1) return;
		int nextDist = AffectedBlocks.getShortestPathInfoBytecode
									(mName, next.getPosition(), ifInsn.getPosition());

		if(targetDist < nextDist && targetDist != Integer.MAX_VALUE) {
			String index = mName.concat(Integer.
									toString(ifInsn.getPosition()));
			//reverse order of choice generator
			shortestPathInfo.put(index, nextDist);
		}

	}
*/

	/*
	@Override
	public void searchFinished(Search search) {
		if (debug)
			System.out.println("searchFinished");
		System.out.println("---->Total number of pruning: " + pruningCount);
//		System.out.println("---->Total number of infeasible paths: " + PathCondition.infeasibleCount);
	}

	@Override
	public void stateAdvanced(Search search) {
//		System.out.println(">>> advanced");
		
		VM vm = search.getVM();
		SystemState ss = vm.getSystemState();
		// if the new state is not feasible (the constraint is unsat)
		if(ss.isIgnored()) {
			Instruction insn = vm.getCurrentThread().getPC();
			assert (insn != null);
			AffectedBlocks.resetCondPos(insn.getPosition(),
						insn.getMethodInfo().getFullName());
		}
	}


	@Override
	public void choiceGeneratorRegistered (VM vm, ChoiceGenerator<?> nextCG, 
			ThreadInfo currentThread, Instruction executedInstruction){
		 ChoiceGenerator<?> cg = vm.getNextChoiceGenerator();
		 Instruction insn = cg.getInsn();

		 // randomly pick the branch to execute
		 if(randomChoice){
			 if(cg instanceof IntIntervalGenerator && cg instanceof PCChoiceGenerator) {
				 if(random.nextBoolean()){
					 IntIntervalGenerator interval = (IntIntervalGenerator) cg;
					 interval.reverse();
				 }
			 }
		 }
	
		 // pick the branch on the shortest path to execute
		 if(shortestPathCFG || shortestPathBytecode){
			 String index = insn.getMethodInfo().getFullName().concat(Integer.
					 toString(insn.getPosition()));
			 
			 if(!PruningRSEListener.shortestPathInfo.containsKey(index)) return;
	
			 if(cg instanceof IntIntervalGenerator && cg instanceof PCChoiceGenerator) {
				 IntIntervalGenerator interval = (IntIntervalGenerator) cg;
				 interval.reverse();
			 }
			 PruningRSEListener.shortestPathInfo.remove(index);
		 }
	}

	@Override
	public void stateBacktracked(Search search) {
//		System.out.println(">>> backtracked!");
		
		ThreadInfo current = search.getVM().getCurrentThread();
		Integer position = current.getPC().getPosition();
		String methodName = current.getPC().getMethodInfo().
										getFullName();
		boolean reachable = AffectedBlocks.isReachable(methodName,
												position, post_dominance_check);
		if(!reachable) {
			SystemState ss = search.getVM().getSystemState();
			ChoiceGenerator<?> cg = ss.getChoiceGenerator();
			assert (cg != null);
			cg.setDone();
			pruningCount++;
	
		}
	}
	*/
}

