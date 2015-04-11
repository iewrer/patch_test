package gov.nasa.jpf.regression.analysis;

import edu.byu.cs.analysis.AnalyzeClass;
import edu.byu.cs.analysis.AnalyzeMethod;
import edu.byu.cs.analysis.ParseFiles;
import edu.byu.cs.analysis.StaticAnalysis;
import edu.byu.cs.search.heuristic.SuperAndSubClasses;
import edu.byu.cs.search.heuristic.pefca.PolymorphicCallGraph;
import edu.byu.cs.search.heuristic.pefca.RapidTypeAnalysis;
import edu.byu.cs.search.heuristic.pefca.ReducedCallGraph;
import edu.byu.cs.search.heuristic.pefca.callNode;
import edu.byu.cs.trace.AbstractMethodInfo;
import gov.nasa.jpf.regression.ast.MethodASTInfo;
import gov.nasa.jpf.regression.cfg.ByteSourceHandler;
import gov.nasa.jpf.regression.cfg.CFG;
import gov.nasa.jpf.regression.cfg.CFGBuilder;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jpf_diff.ErrorCount;

import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InvokeInstruction;
import org.apache.bcel.verifier.structurals.InstructionContext;

//this assumes there is a single class being analyzed
public class InterproceduralAnalysis {

	private boolean verbose = false;


	Map<String,MethodASTInfo> methodASTInfo =
		new HashMap<String,MethodASTInfo>();

	// This maps a qualified Method name to the CIPD that has been analyzed
	Map<String, ComputeInterProceduralDiff> differences;
	
	// This maps a class's name to the affected fields/globals
	Map<String, AffectedGlobals> globalsMap;

	Map<String, CFG> allCFG = null;
	Map<String, CFGBuilder> cfgBuilderLookup;
	Map<String, String> oldClassPathLookup;
	Map<String, String> newClassPathLookup;
	String mainMethodName;
	ArrayList<JavaClass> allNewClasses;
	ArrayList<JavaClass> allOldClasses;
	String mainClassName;
	
	Map<String, JavaClass> newClassesMap;
	Map<String, JavaClass> oldClassesMap;

	Set<Integer> modifiedMethodIndices;
	StaticAnalysis newSA, oldSA;
	ReducedCallGraph newCallGraph, oldCallGraph;
	SuperAndSubClasses newHierarchy, oldHierarchy;
	
	boolean preciseHeap = false;


	public InterproceduralAnalysis(List<File> oldClasses,
			List<File> newClasses, Map<String,MethodASTInfo> methodASTInfo,
			String mainClassName) {
		this.methodASTInfo = methodASTInfo;
		allNewClasses = new ArrayList<JavaClass>();
		allOldClasses = new ArrayList<JavaClass>();
		this.mainClassName = mainClassName;
		modifiedMethodIndices = new HashSet<Integer>();
		cfgBuilderLookup = new HashMap<String, CFGBuilder>();
		oldClassPathLookup = new HashMap<String, String>();
		newClassPathLookup = new HashMap<String, String>();
		differences = new HashMap<String, ComputeInterProceduralDiff>();
		
		// Initialize the new and old classes maps
		this.newClassesMap = new HashMap<String, JavaClass>();
		this.oldClassesMap = new HashMap<String, JavaClass>();
		
		// Initialize the affected globals HashMap
		this.globalsMap = new HashMap<String, AffectedGlobals>();
		
		// populate the classes map and globals map
		this.buildClassesMaps();

		Iterator<File> newClass = newClasses.iterator();
		while(newClass.hasNext()) {
			File newClassFile = newClass.next();
			initializeClassFiles(newClassFile, allNewClasses);
		}
		// for the new version
		boolean isNewProgram = true;
		initializeStaticAnalysis(isNewProgram);

		Iterator<File> oldClass = oldClasses.iterator();
		while(oldClass.hasNext()) {
			File oldClassFile = oldClass.next();
			initializeClassFiles(oldClassFile, allOldClasses);
		}

		isNewProgram = false;
		initializeStaticAnalysis(isNewProgram);
	}
	
	/**
	 * buildClassesMaps - this method populates the newClassesMap and
	 * the oldClassesMap with the JavaClass's classname as the key and
	 * the JavaClass as the value. This will allow for quick lookup
	 * of the JavaClass objects. The assumption is that the filename
	 * used will be unique from all other filenames.
	 * TODO: This method needs to be called by someone, probably the
	 *       constructor will call it when after data structure initialization.
	 * TODO: Need to determine the format of the String returned by
	 *       JavaClass.getFileName() and JavaClass.getClassName().
	 */
	public void buildClassesMaps() {
		
		if (verbose)
			System.out.println("** New Classes:");
		
		/*
		 * For each JavaClass in allNewClasses, get its filename and
		 * then map it to the JavaClass object.
		 */
		for(JavaClass jc : this.allNewClasses) {
			/*
			 * Format of the following String:
			 * <package>.<classname>
			 * e.g. interprocedural.ExampleIP04
			 */
			String name = jc.getPackageName() + "." + jc.getClassName();
			//System.out.println("   - " + name);
			//String name = jc.getFileName();
			this.newClassesMap.put(name, jc);
			
			/*
			 * I am only adding AffectedGlobal objects to the globalsMap
			 * for classes in the 'new' version because these are the ones
			 * that exist and can be affected.
			 */
			this.globalsMap.put(name, new AffectedGlobals(jc.getFields()));
		}
		
		/*
		 * For each JavaClass in allOldClasses, get its classname and
		 * packagename, then map it to the JavaClass object.
		 */
		for(JavaClass jc : this.allOldClasses) {
			String name = jc.getPackageName() + "." + jc.getClassName();
			//String name = jc.getFileName();
			this.oldClassesMap.put(name, jc);
		}
	}

	public ReducedCallGraph getNewCallGraph() {
		return newCallGraph;
	}

	public ReducedCallGraph getOldCallGraph() {
		return oldCallGraph;
	}

	public Set<Integer> getModifiedCallNodes() {
		return modifiedMethodIndices;
	}

	public Map<String, CFG> getAllCFGs() {
		return allCFG;
	}

	public StaticAnalysis getStaticAnalysisResults() {
		return newSA;
	}

	public Map<String, ComputeInterProceduralDiff> getDifferences () {
		return differences;
	}

	private void initializeClassFiles(File f,
								ArrayList<JavaClass> allClasses) {
		try {
			if(f.isFile()){
				String name = f.getName();
				if (name.endsWith(".class")) {
					Collection<JavaClass> clazz = ParseFiles.parseClass(f);
					allClasses.addAll(clazz);
				}//otherwise ignore it, it is not a class file
			}
		} catch (IOException e) {
			System.err.println("error initializing class files");
			e.printStackTrace();
		}
	}

	public ArrayList<JavaClass> getNewClasses() {
		return this.allNewClasses;
	}
	
	public ArrayList<JavaClass> getOldClasses() {
		return this.allOldClasses;
	}
	
	private void initializeStaticAnalysis(boolean isNewProgram) {
		ArrayList<JavaClass> allClasses = null;
		StaticAnalysis sa;
		ReducedCallGraph callGraph;
		if(isNewProgram) {
			allClasses = allNewClasses;
		} else {
			allClasses = allOldClasses;
		}


		sa = new StaticAnalysis(allClasses);

		ArrayList<String> classNames = new ArrayList<String>();
		for(int classIndex = 0; classIndex < allClasses.size();
														classIndex++) {
			String className = allClasses.get(classIndex).getClassName();
			String fileName = allClasses.get(classIndex).getFileName();
			classNames.add(className);
			if(isNewProgram)
				newClassPathLookup.put(className, fileName);
			else
				oldClassPathLookup.put(className, fileName);
		}
		
	
		SuperAndSubClasses superAndSubClasses = new SuperAndSubClasses(allClasses);

		PolymorphicCallGraph polymorphicCallGraph =
				new PolymorphicCallGraph(superAndSubClasses, sa.getClassAnalysis(),
											classNames, mainClassName);

		RapidTypeAnalysis rta = new RapidTypeAnalysis
								(polymorphicCallGraph, mainClassName);
		callGraph = new ReducedCallGraph(rta);
		callGraph.computeDistanceEstimates(null);
		

		if(isNewProgram) {
			markChangedCallGraph(sa, callGraph);
			newCallGraph = callGraph;
			this.newHierarchy = superAndSubClasses;
			newSA = sa;
		} else {
			oldCallGraph = callGraph;
			this.oldHierarchy = superAndSubClasses;
			oldSA = sa;
		}
	}
	
	
	private void markChangedCallGraph(StaticAnalysis sa,
													ReducedCallGraph callGraph) {
		Set<Integer> toExplore = new HashSet<Integer>();
		HashMap<String, AnalyzeClass> classAnalysis =
										sa.getClassAnalysis();
		Iterator<String> classItr = classAnalysis
											.keySet().iterator();
		while(classItr.hasNext()) {
			String classIndex = classItr.next();
			AnalyzeClass ac = classAnalysis.get(classIndex);
			HashMap<String, AnalyzeMethod> methods = ac.getMethods();
			Iterator<String> methodItr = methods.keySet().iterator();
			while(methodItr.hasNext()) {
				String methodIndex = methodItr.next();
				AnalyzeMethod am = methods.get(methodIndex);
				String uniqueMethodName = am.getUniqueMethodIndex();
				String index = classIndex.concat("."+uniqueMethodName);
				if(methodASTInfo.containsKey(index)) {
					MethodASTInfo ast = methodASTInfo.get(index);
					if(ast != null && !(ast.getEquivalent()) ) {
						int cIndex = callGraph.getCallNodeIndex
								(classIndex.concat(uniqueMethodName));
						modifiedMethodIndices.add(cIndex);
						toExplore.add(cIndex);
					}
				}
			}
		}
	}

    private String getMethodStringIdentifier(int index) {
    	callNode cn = newCallGraph.getCallNode(index);
    	String className = cn.getClassName();
    	String methodName = cn.getMethodName();
    	
    	AnalyzeClass ac = newSA.getClassAnalysis().get(className);
    	
    	String finalClsName; 
    	AnalyzeMethod am = null;
    	am = ac.getMethods().get(methodName);
    	finalClsName = className;
    
    	return finalClsName.concat("." + am.getMethodName() + 
    						am.getMethodGen().getSignature());
    }
    
   
  
    
    private CFGBuilder getCFGBuilder(String className) {
    	if(!cfgBuilderLookup.containsKey(className)) {
			assert (newClassPathLookup.containsKey
											(className) == true);
			String fileName = newClassPathLookup.get(className);
			return generateCFGsForClass(className, fileName);

		} else {
			return cfgBuilderLookup.get(className);
		}
    }
    
	public void searchCallGraph(int index) {

		//need this check to handle the polymorphism call graph
		if(!methodExists(index)) return;
		
		/*
		 * this works similarly to methodExists, except that it returns a
		 * method signature when true and returns an empty string when
		 * false. If it actually returns a method signature, then we can
		 * use that method signature as the methodName value rather than
		 * the one from callNode. The callNode method signatures are
		 * equivalent, but they do not contain the generics information.
		 * This allows us access to a method signature with generics info
		 * throughout the rest of this method.
		 */
//		String methodSig = this.getMethodSignatureIfExists(index);
//		if(methodSig.equals("")) return;
		
		callNode cn = newCallGraph.getCallNode(index);
		
		//The className is fully qualified 
		String className = cn.getClassName();
		/*
		 * for now, this is essentially being overwritten with an equivalent
		 * method signature that actually contains the generics information.
		 * If there is not an equivalent method signature, then we will have
		 * already returned out of this method above.
		 */
		String methodName = cn.getMethodName();
	
		ComputeInterProceduralDiff cipd = null;
		try {
			
			String shortMethodName;		
			String signatureMethodIndex;
			String lookupIndex;
			CFGBuilder currCFGBuilder;

			AnalyzeClass currClass = newSA.getClassAnalysis().get(className);
			HashMap<String, AnalyzeMethod> methods = currClass.getMethods();
			
			AnalyzeMethod am = methods.get(methodName);
			shortMethodName = am.getMethodName();
			signatureMethodIndex = className.concat("."+shortMethodName+
					am.getMethodGen().getSignature());
			lookupIndex = className.concat("."+methodName);
			currCFGBuilder = getCFGBuilder(className);
			
			/*
			 * Enums as arguments:
			 * lookupIndex which ultimately is derived from the callNode
			 * representation of the method signature uses a '$' when
			 * referring to an enum that is a method argument. The
			 * methodASTInfo map stores method signatures with enums
			 * using a '.' instead. As a quick fix, we replace the '$'
			 * with '.' for now.
			 */
			// removing $ from the lookup index, replacing with .
			lookupIndex = lookupIndex.replaceAll("\\$", ".");
			
		
			if(differences.containsKey(signatureMethodIndex)) return;
			
			//if(methodInfo == null) throw new RuntimeException("could not find AST method");
			//keep going if no ASTInfo - this *should* only happen if
			//there are no differences in the versions
			MethodASTInfo methodInfo = methodASTInfo.get(lookupIndex);
			if (methodInfo == null){
				if (verbose)
					System.out.println("creating methodASTInfo on the fly for " + className + "." + methodName);
				methodInfo = new MethodASTInfo(className,shortMethodName);
				methodInfo.setEquivalent(true);
				methodInfo.setMatched(true);
				methodASTInfo.put(lookupIndex, methodInfo);
			}
			
			CFG newCFG = currCFGBuilder.getCFG(signatureMethodIndex, "modified",
					methodInfo);
			// TODO: Need to modify the constructor for CIPD so that
			//       the JavaClass object is included.
			cipd = new ComputeInterProceduralDiff
							(currCFGBuilder, null, this.newCallGraph,
							currClass, this.modifiedMethodIndices,
							methodInfo, lookupIndex);
			
			//cipd.getSemanticDiff().setPreciseHeap(this.preciseHeap);
			
			cipd.preciseAnalysis(signatureMethodIndex, newCFG,
					currCFGBuilder.getMethodGen(signatureMethodIndex), index, this.preciseHeap);
			differences.put(signatureMethodIndex, cipd);


		} catch (Exception e) {
			System.err.println("error encountered traversing the call graph");
			e.printStackTrace();
		}

		// this is the end of the call graph
		if(!newCallGraph.getCallEdges().containsKey(index)) {
			if (verbose)
				printAffectedInfo(cipd);
			return;
		}

		HashMap<Integer, ArrayList<Integer>> edges =
							newCallGraph.getCallEdges().get(index);

		//System.out.println("edges : " + edges.toString());
		HashMap<Integer, ArrayList<Integer>> affectedFormal =
				new HashMap<Integer, ArrayList<Integer>>();

		Iterator<Integer> edgeItr = edges.keySet().iterator();
		while(edgeItr.hasNext()) {
			Integer parent = edgeItr.next();
			ArrayList<Integer> succ = edges.get(parent);
			for(int succIndex = 0; succIndex < succ.size(); succIndex++) {
				int succId = succ.get(succIndex);
				// Depth-first traversal
				searchCallGraph(succId);
				
				//need this check to handle the polymorphism call graph
				if(!methodExists(succId)) continue;
				
				ComputeInterProceduralDiff cipdSucc =
					differences.get(getMethodStringIdentifier(succId));
				ArrayList<Integer> affectedVarPositions =
					cipdSucc.getAffectedFormalParameterIndices();
				/**System.out.println("begin ***");
				System.out.println("prev parent ===> " + parent  + " " +
						"affected Variables :" + cipdSucc.allAffectedVars.toString());
				System.out.println("parent --> " + parent + " " +
						"affectedVarPositions :" + affectedVarPositions);**/
				affectedFormal.put(parent, affectedVarPositions);
						//System.out.println("end ***");
			}
		}
		assert (cipd != null); {
			cipd.genAffectedLocationsDueToOtherModMethods(affectedFormal);
			AffectedNodes cipdAN = cipd.getAffectedNode("method_body");
			Set<Integer> affectedCondNodes = cipdAN.getAffectedCondNodes();
			Set<Integer> affectedWriteNodes = cipdAN.getAffectedWriteNodes();
			Set<Integer> affectedReturnNodes = cipdAN.getAffectedReturnNodes();
			cipd.generateForwardMap(affectedCondNodes,affectedWriteNodes,affectedReturnNodes);

			cipd.updateAffectedAffectedIndices();
			Set<Integer> otherConds = cipd.semanticDiff.markOtherConditionalBranches(
									cipd.getAffectedNode("method_body").getAffectedWriteNodes(), true);
			cipd.semanticDiff.updateToBeDroppedSet(otherConds, cipd.getAffectedNode("method_body").getCanbeDropped(),
					cipd.getAffectedNode("method_body").getAffectedCondNodes());
			cipd.getAffectedNode("method_body").getAffectedCondNodes().addAll(otherConds);
			if (verbose)
				printAffectedInfo(cipd);
		}
	}

	//the current implementation of the call graph may have an edge to a method
	// whose implementation does not exist in the class which is contained in
	// this can go away once the implementation of the call graph is updated
	// to remove potential edges to methods that do no have an implementation
	private boolean methodExists(int callIndex) {
		callNode cn = newCallGraph.getCallNode(callIndex);
		
		//The className is fully qualified 
		String className = cn.getClassName();
		String methodName = cn.getMethodName();
	
		AnalyzeClass currClass = newSA.getClassAnalysis().get(className);
		HashMap<String, AnalyzeMethod> methods = currClass.getMethods();
		if(methods.containsKey(methodName)) {
			return true;
		}
		
		return false;
	
	}
	

	private void printAffectedInfo(ComputeInterProceduralDiff cipd) {
		System.out.println("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%");
		System.out.println("Method name : " +cipd.getOtherMethodIndex());
		System.out.println(cipd.getSemanticDiff().allAffectedNodes.toString());
		System.out.println("CallEdges :" + cipd.getSemanticDiff().callEdges.toString());
	}


	public CFGBuilder generateCFGsForClass
							(String className, String fileName) {
		ByteSourceHandler bsHandler = new ByteSourceHandler();
		ErrorCount error = new ErrorCount(className);
		try {
			bsHandler.readSourceFile(fileName, error);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		CFGBuilder cfgBuilder = new CFGBuilder();
		try {
			cfgBuilder.parseClass(bsHandler.classFile, error);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		cfgBuilderLookup.put(className, cfgBuilder);
		return cfgBuilder;

	}

	/*
	 * setPreciseHeap: boolean -> void
	 * given a boolean value, this setter method will set the global
	 * variable preciseHeap to be that boolean value.
	 */
	public void setPreciseHeap(boolean precise) {
		
		this.preciseHeap = precise;
	}


}