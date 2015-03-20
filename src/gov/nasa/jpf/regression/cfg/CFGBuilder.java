package gov.nasa.jpf.regression.cfg;
import gov.nasa.jpf.regression.analysis.DistanceMatrix;
import gov.nasa.jpf.regression.ast.MethodASTInfo;

import java.io.*;
import java.util.*;

import org.apache.bcel.Repository;
import org.apache.bcel.generic.*;
import org.apache.bcel.verifier.structurals.ControlFlowGraph;
import org.apache.bcel.verifier.structurals.InstructionContext;
import org.apache.bcel.classfile.*;


//import gnu.trove.THashMap;


@SuppressWarnings("unchecked")
public class CFGBuilder {

    /** Name of currently loaded class. */
    private String className;

//    private Map<MethodSignature, CFG> cfgMap =
//        new HashMap<MethodSignature, CFG>();

     Map<String, CFG> cfgMap =
        new HashMap<String, CFG>();
     
     //for each method, a map from pos to line number
     private Map<String,Map<Integer,Integer>> posLineMap = 
    	 new HashMap<String,Map<Integer,Integer>>();

     Map<String, MethodGen> methodGenMap =
    	 new HashMap<String, MethodGen>();

    /** BCEL representation of class, used primarily to extract instruction
        lists to be passed to graph building methods */
    private JavaClass javaClass;
    /** Array of methods implemented by the class */
    private Method[] methods;
    /** Constant pool for the class. */
    private ConstantPoolGen cpg;
    /** Collection of handles to instructions that are leaders of blocks. */
    private TreeSet<Object> leaders;
    /** Collection of handles to instructions that end blocks. */
    private TreeSet<Object> end;
    /** Collection of handles to call instructions for which blocks are
        to be created. */
    private TreeSet<Object> calls;
    /** ID of the normal exit node (as opposed to an exceptional exit node). */
    private int normalExitNodeID = -1;

    /** Conditional compilation flag specifying whether debugging information
        is to be printed. */
    private static final boolean DEBUG = false;

   // MethodGen mg;


    /*************************************************************************
     * Convenience constructor used internally.
     */
    public CFGBuilder() {
        Comparator<Object> c = new IHandleComparator();
        leaders = new TreeSet<Object>(c);
        end = new TreeSet<Object>(c);
        calls = new TreeSet<Object>(c);
    }




    /*************************************************************************
     * Parses the class.
     *
     * <p>Uses BCEL to parse the class file.</p>
     *
     */
    public void parseClass(String className)
                   throws Exception {
       try {
    	   javaClass =  Repository.lookupClass(className);
       }
       catch (ClassNotFoundException e) {
    	   // Maybe it's an absolute (or otherwise qualified) path

    	   File f = new File(className);
    	   if (!f.exists()) {
    		   throw new Exception("Cannot find class: " + className);
    	   }
    	   javaClass = new ClassParser(className).parse();
       }

        if (javaClass.isInterface()) {
            throw new Exception("Error loading class " + className,
                new Exception("Cannot build graphs " + "for interface"));
        }

        this.className = javaClass.getClassName();
        cpg = new ConstantPoolGen(javaClass.getConstantPool());
        methods = javaClass.getMethods();
        if (methods.length == 0) {
            throw new Exception("Error loading class " + className,
               new Exception("Class implements no methods"));
        }
    }


    /*************************************************************************
     * Parses the class.
     *
     * <p>Uses BCEL to parse the class file.</p>
     *
     */
    public void parseClass(JavaClass javaClass)
                   throws Exception {
        this.javaClass = javaClass;

        if (javaClass.isInterface()) {
            throw new Exception("Error loading class " + className,
                new Exception("Cannot build graphs " + "for interface"));
        }

        this.className = javaClass.getClassName();
        cpg = new ConstantPoolGen(javaClass.getConstantPool());
        methods = javaClass.getMethods();
        if (methods.length == 0) {
            throw new Exception("Error loading class " + className,
               new Exception("Class implements no methods"));
        }
    }

    private String getUniqueMethodIndex(int methodIndex) {
    	 if (methodIndex < 0 || methodIndex >= methods.length) {
             throw new RuntimeException("Method index out of range");
         }
         if (methods[methodIndex].isAbstract()
                 || methods[methodIndex].isNative()) {
             return null;
         }
         reset();

         MethodGen mg = new MethodGen(methods[methodIndex], className, cpg);
         String uniqueMethodName = Utility.formatUniqueName(className, methods[methodIndex]);
         methodGenMap.put(uniqueMethodName, mg);
         return uniqueMethodName;
    }

    //public MethodGen getMethodGen() {
    //	return mg;
    //}

    public MethodGen getMethodGen(String methodName) {
    	if(methodGenMap.containsKey(methodName))
    		return methodGenMap.get(methodName);
    	else{
    		System.err.println("could not find method gen");
    	}
    	return null;
    }
    
    public Map<String,Map<Integer,Integer>> getPosLineMap(){
    	return this.posLineMap;
    }
    
    private void buildPosLineMap(String mName, MethodGen mg){
    	InstructionList il = mg.getInstructionList();
    	int[] positions = il.getInstructionPositions();
    	LineNumberTable lnt = mg.getLineNumberTable(cpg);
    	Map<Integer,Integer> pToLMap = new HashMap<Integer,Integer>();
    	for (int i=0; i<positions.length;i++){
    		pToLMap.put(positions[i], lnt.getSourceLine(positions[i]));
    	}
    	posLineMap.put(mName, pToLMap);
    }

    /***
     * 
     * @param methodIndex
     * @return
     * 
     * 先构造节点
     * 再构造边
     * 最后构造从字节码指令pos到行号linenumber的映射
     */
    private CFG buildCFG(int methodIndex) {
    	String uniqueMethodName = getUniqueMethodIndex(methodIndex);
        CFG cfg = cfgMap.get(uniqueMethodName);
        if ((cfg != null)) {
            return cfg;
        }


        List<Object> pendingInference = new ArrayList<Object>();

        cfg = new CFG(uniqueMethodName);
        cfgMap.put(uniqueMethodName, cfg);

        MethodGen mg = methodGenMap.get(uniqueMethodName);
        formNodes(cfg, mg.getInstructionList());
        formEdges(cfg, pendingInference);

        buildPosLineMap(uniqueMethodName, mg);
        
        return cfg;
    }

    /*************************************************************************
     * Builds a control flow graph for a method and adds it to the cache,
     * using an index to locate the method.
     *
     * @param methodIndex Index of the method in the class for which the
     * control flow graph is to be built.
     *
     * @return The resulting control flow graph.
     */
    @SuppressWarnings("unchecked")
    private CFG buildCFG(int methodIndex, String version, MethodASTInfo astInfo)
            throws Exception {

    	String uniqueMethodName = getUniqueMethodIndex(methodIndex);
        CFG cfg = cfgMap.get(uniqueMethodName);
        if ((cfg != null)) {
            return cfg;
        }


		List<Object> pendingInference = new ArrayList<Object>();

        cfg = new CFG(uniqueMethodName);
        cfgMap.put(uniqueMethodName, cfg);

        MethodGen mg = new MethodGen(methods[methodIndex], className, cpg);
        formNodes(cfg, mg.getInstructionList());
        formEdges(cfg, pendingInference);

        buildPosLineMap(uniqueMethodName, mg);


        if (astInfo != null){
            //LineNumberGen[] lng = mg.getLineNumbers();
            LineNumberTable lnt = mg.getLineNumberTable(cpg);
            InstructionList il = mg.getInstructionList();
            int[] pos = il.getInstructionPositions();
        	addASTInfoToCFG(astInfo,version,cfg,lnt,pos);
        	if (DEBUG){
        	System.out.println("node change types...");
	        	Node[] nodes = cfg.getNodes();
	        	for (int nodeIndex = 0; nodeIndex < nodes.length; nodeIndex++) {
	        		System.out.println(nodes[nodeIndex].changeTypes.toString());
	        	}
        	}
        }

        	checkRelevantIns(mg, cfg);
        	if (DEBUG){
	        	System.out.println("Definitions of variables" + cfg.writeIns.toString());
        	}
        	Iterator<Integer> tmpItr = cfg.tmpMod.iterator();
        	while(tmpItr.hasNext()) {
        		Integer val = tmpItr.next();
        		if(cfg.writeIns.contains(val) || cfg.ifIns.contains(val)) {
        			cfg.modifiedWritesAndIfs.add(val);
        		} if(cfg.retIns.contains(val) || cfg.callIns.contains(val)) {
        			cfg.modifiedCallSitesAndReturns.add(val);
        		}
        	}
        	if (DEBUG)
        		System.out.println("modifications : " + cfg.modifiedWritesAndIfs.toString());
        	//System.exit(1);

        	if (DEBUG)
        		cfg.dm.printMatrix();
        	cfg.dm.computeShortestPathEstimates();
        	if (DEBUG)
        		cfg.dm.printMatrix();
     //   }
        return cfg;
    }

    /*
     * update the cfg using the information in the AST
     */
    private void addASTInfoToCFG(MethodASTInfo astInfo, String version,CFG cfg,
    		LineNumberTable lnt,int[] pos){
        Node[] nodes = cfg.getNodes();
        //skip root node
        int indx = 0;
        //skip root node
        //skip finish node, and node with all positions
        Node special = nodes[0];
        special.addPosition(0);
        assert((nodes.length - 2 ) > 0);
        special = nodes[nodes.length-2];
        special.addPosition(nodes[nodes.length-2].getEndOffset());
        //these nodes are not processed in the following loop, so
        //add line numbers here - not sure if they are needed or not
        Node specialNode = nodes[0];
        specialNode.setStartLineNumber(lnt.getSourceLine(specialNode.getStartOffset()));
		specialNode.setEndLineNumber(lnt.getSourceLine(specialNode.getEndOffset()));
		specialNode = nodes[nodes.length-1];
		specialNode.setStartLineNumber(lnt.getSourceLine(specialNode.getStartOffset()));
		specialNode.setEndLineNumber(lnt.getSourceLine(specialNode.getEndOffset()));
		specialNode = nodes[nodes.length-2];
		specialNode.setStartLineNumber(lnt.getSourceLine(specialNode.getStartOffset()));
		specialNode.setEndLineNumber(lnt.getSourceLine(specialNode.getEndOffset()));

		int assertLine = -1;
		int firstAssertNodeId = -1;
		
		//设置每个Node的信息
		//包括：offset、行号、pos、type等
		//顺便设置cfg中的信息
		//包括：removedIns、tmpMod、asserts、positionLookUp映射等
        for (int i=1; i<nodes.length-2; i++){
        	Node n = nodes[i];
        	int offset = n.getStartOffset();
        	int endOffset = n.getEndOffset();

        	if (!(offset<=pos[indx]))
        		indx = 0;
    		while(offset<pos[indx])
    			indx++;
    		boolean done = false;
    		//设置node的行号
    		n.setStartLineNumber(lnt.getSourceLine(offset));
    		n.setEndLineNumber(lnt.getSourceLine(endOffset));
    		while (offset <= endOffset && !done){
    			int lineNum = lnt.getSourceLine(pos[indx]);
        		if (version.equalsIgnoreCase("original")) {
        			n.addType(pos[indx],astInfo.getOrigLineType(lineNum));
        			if(astInfo.getOrigLineType(lineNum).equals("removed")) {
        				//如果该版本是original，且该行是removed，则添加到removedIns中
        				//按照指令位置
        				cfg.removedIns.add(pos[indx]);
        			}
        		}
        		else {
        			n.addType(pos[indx],astInfo.getModLineType(lineNum));
        			if((!astInfo.getModLineType(lineNum).equals("unchanged"))) {
        				cfg.tmpMod.add(pos[indx]);
        				if (DEBUG)
        					System.out.println("pos[indx]" + pos[indx]);
        			}
        		}
        		// add marks for assert statements
        		// TODO: ASSUME AT MOST ONE ASSERTION CAN BE AT A LINE
        		if (version.equalsIgnoreCase("original")) {
        			if (astInfo.ifAssertOrig(lineNum)){
            			if(assertLine != lineNum){
            				assertLine = lineNum;
            				firstAssertNodeId = i;
            			}else{
            				// ignore the first assertion node
            				if(firstAssertNodeId != i){
            					cfg.asserts.add(pos[indx]);
            					n.setAssert(true); // may mark the node for multiple times
            				}
            			}	
        			}
        		}else{
        			if (astInfo.ifAssertMod(lineNum)){
            			if(assertLine != lineNum){
            				assertLine = lineNum;
            				firstAssertNodeId = i;
            			}else{
            				// ignore the first assertion node
            				if(firstAssertNodeId != i){
            					cfg.asserts.add(pos[indx]);
            					n.setAssert(true); // may mark the node for multiple times
            				}
            			}
        			}
        		}
        		n.addPosition(pos[indx]);
        		cfg.startOffsetLookup.put(pos[indx], n.getStartOffset());
        		indx++;
        		if (indx<pos.length)
        			offset = pos[indx];
        		else
        			done = true;
    		}
        }
    }


    private void checkRelevantIns(MethodGen mg, CFG cfg) {
    	ControlFlowGraph bcelCFG = new ControlFlowGraph(mg);
    	InstructionContext[] ic  = bcelCFG.getInstructionContexts();
    	int  posCounter = 0;
    	//int size = ic.length;
    	//dm = new DistanceMatrix(size);
    	int size = leaders.size();
    	cfg.dm = new DistanceMatrix(size);

    	for(int contextIndex = 0; contextIndex < ic.length; contextIndex++) {

    		InstructionContext context = ic[contextIndex];

    		int insPos = context.getInstruction().getPosition();
    		int currPosIndex;
    		int leaderPos = cfg.startOffsetLookup.get(insPos);

    		if(!cfg.posToIndex.containsKey(leaderPos)) {
    			cfg.posToIndex.put(leaderPos, posCounter);
    			currPosIndex = posCounter;
    			posCounter++;
    		} else {
    			currPosIndex = cfg.posToIndex.get(leaderPos);
    		}
    		InstructionContext[] succ = context.getSuccessors();
    		for(int succIndex = 0; succIndex < succ.length; succIndex++) {
    			int succPosition = succ[succIndex].getInstruction().getPosition();
    			int succPosIndex = -1;
    			int succLeaderPos = cfg.startOffsetLookup.get(succPosition);

    			if(!cfg.posToIndex.containsKey(succLeaderPos)) {
    				cfg.posToIndex.put(succLeaderPos, posCounter);
    				succPosIndex = posCounter;
    				posCounter++;
    			} else {
    				succPosIndex = cfg.posToIndex.get(succLeaderPos);
    			}
    			cfg.dm.setValue(currPosIndex, succPosIndex, 1);

    		}

    		Instruction ins = context.getInstruction().getInstruction();
    		Integer position = context.getInstruction().getPosition();
    		if(ins instanceof StoreInstruction || ins instanceof IINC ||
    				ins instanceof PUTFIELD || ins instanceof PUTSTATIC ||
    				ins instanceof AASTORE || ins instanceof BASTORE ||
    			    ins instanceof CASTORE || ins instanceof DASTORE ||
    				ins instanceof FASTORE || ins instanceof IASTORE ||
    				ins instanceof LASTORE || ins instanceof SASTORE) {
						cfg.writeIns.add(position);
			} else if (ins instanceof IfInstruction ||
					ins instanceof DCMPG || ins instanceof DCMPL ||
					ins instanceof FCMPG || ins instanceof FCMPL ||
					ins instanceof LCMP) {
				cfg.ifIns.add(position);
			} else if (ins instanceof ReturnInstruction) {
				cfg.retIns.add(position);
			} else if(ins instanceof InvokeInstruction) {
				cfg.callIns.add(position);
			}
		}

    }





    /*************************************************************************
     * Attempts to retrieve a control flow graph for a method from the cache,
     * using a unique method name to locate the method. If the graph can't be found, it
     * will be built and added to the cache.
     *
     * @return The requested control flow graph.
     */
    public CFG getCFG(String uniqueMethodName, String version,
    		MethodASTInfo astInfo) throws Exception {
        CFG cfg = cfgMap.get(uniqueMethodName);

        if ((cfg != null)) {
            return cfg;
        }


        if (cfg == null) {
        	int methodIndex = getMethodIndex(uniqueMethodName);
            if (methodIndex < 0) {
                throw new Exception("No such method");
            }
            if (methods[methodIndex].isAbstract()
                    || methods[methodIndex].isNative()) {
                return null;
            }
            cfg = buildCFG(methodIndex,version, astInfo);
        }

        return cfg;
    }

    /**
     * retrieve the index of the provided method using a unique method name
     * @param uniqueMethodName
     * @return the index of method
     */
    private int getMethodIndex(String uniqueMethodName){
    	if (!uniqueMethodName.startsWith(className)){
    		return -1;
    	}
    	String methodName = uniqueMethodName.substring(className.length()+1);
    	for (int i=0; i<methods.length; i++){
    		Method m = methods[i];
//    		System.out.println("	searched:" + className + m.getName());
    		if(methodName.equals(m.getName() + m.getSignature())){
    			return i;
    		}
    	}
		return -1;
    }



    /*************************************************************************
     * Builds the control flow graphs for every method in the class and returns
     * an iterator over the collection. This refreshes the cache for every
     * method in the class.
     */
    public Map<String, CFG> buildAllCFG()
    	throws Exception {
        for (int i = 0; i < methods.length; i++) {
            buildCFG(i); //neha: changed this to call
        }
    	return cfgMap;
    }




    /*************************************************************************
     * Resets data structures used by the control flow graph construction
     * algorithm.
     */
    private void reset() {
        leaders.clear();
        end.clear();
        calls.clear();
        //TODO
//        jsrTargets.clear();
    }

    /*************************************************************************
     * Computes the nodes in the method and adds them to the graph.
     *
     * @param cfg Control flow graph to which to add the computed nodes
     * @param il BCEL instruction list representing the method for which
     * to compute the basic blocks.
     * 
     * 按照字节码指令位置pos计算每个block的起始位置和结束位置
     * 然后分配并创建每个block给每个node
     */
    private void formNodes(CFG cfg, InstructionList il) {
        InstructionHandle ih, target, prev_ih, next_ih;
        Instruction instr;
        String methodClass;

        leaders.add(il.getStart());
        for (ih = il.getStart(); ih != null; ih = ih.getNext()) {
            if (calls.contains(ih) &&
                    (leaders.contains(ih) || end.contains(ih))) {
                //System.out.println("Continuing on offset " +
                //                   ih.getPosition());
                continue;
            }
            //System.out.println("Checking for leaders on offset " +
            //                   offset.toString());

            instr = ih.getInstruction();
            if (DEBUG) System.out.println(instr.toString(true));

            if (instr instanceof BranchInstruction) {
                if ((instr instanceof GotoInstruction) ||
                    (instr instanceof IfInstruction) ||
                    (instr instanceof JsrInstruction))
                {
                    // Add the branch instruction to the end list
                    end.add(ih);

                    // Add target to leader list
                    target = ((BranchInstruction) instr).getTarget();
                    leaders.add(target);

                    // Add instruction prior to target to end list
                    prev_ih = target.getPrev();
                    if (prev_ih != null) {
                        end.add(prev_ih);
                    }

                    // Add instruction after branch to leader list
                    next_ih = ih.getNext();
                    if (next_ih != null) {
                        // A GOTO can be the last instruction in a method
                        leaders.add(ih.getNext());
                    }

                    //TODO
//                  if (instr instanceof JsrInstruction) {}
                }
                else if (instr instanceof Select) {
                    Select selectInstr = (Select) instr;

                    // Add switch to end list
                    end.add(ih);

                    // Add default target to leader list. It's not very
                    // apparent, but BCEL stores the default target of a Select
                    // instruction (switch) in the target field inherited
                    // from BranchInstruction.
                    target = selectInstr.getTarget();
                    leaders.add(target);

                    // Add instruction before default target to end list
                    prev_ih = target.getPrev();
                    if (prev_ih != null) {
                        end.add(prev_ih);
                    }

                    // Process case targets
                    InstructionHandle[] targets = selectInstr.getTargets();
                    for (int k = 0; k < targets.length; k++) {
                        // Add case target to leaders list
                        leaders.add(targets[k]);
                        // Add instruction before case target to end list
                        prev_ih = targets[k].getPrev();
                        if (prev_ih != null) {
                            end.add(prev_ih);
                        }
                    }

                    // Add the instruction immediately following the switch
                    // instruction to leaders list
                    leaders.add(ih.getNext());
                }
            }

            //TODO: InvokeInstruction


            //TODO: Athrow


            //TODO: RETURN, RET

        }
        end.add(il.getEnd());

        if (DEBUG) {
            System.out.println(cfg.getMethodName());
            System.out.print("getLeaders # ");
            System.out.print("leaders length: " + leaders.size() + " [");
            for (Iterator it = leaders.iterator(); it.hasNext(); ) {
                System.out.print(" " +
                    ((InstructionHandle) it.next()).getPosition());
            }
            System.out.print(" ]\nend length: " + end.size() + " [");
            for (Iterator it = end.iterator(); it.hasNext(); ) {
                System.out.print(" " +
                    ((InstructionHandle) it.next()).getPosition());
            }
            System.out.print(" ]\ncalls length: " + calls.size() + " [");
            for (Iterator it = calls.iterator(); it.hasNext(); ) {
                System.out.print(" " +
                    ((InstructionHandle) it.next()).getPosition());
            }
            System.out.print(" ]\n");
        }

        // Now create and add the actual basic blocks to the CFG.
        // Virtual blocks (Entry, Exit, Return) are given types immediately,
        // so that they won't be added to the offset->block map by
        // the overridden addNode method. Only 'real' blocks should be
        // retrievable by offset.
        int pos = 1;

        // Create and add entry node
        cfg.addVirtualNode(
            new Node(pos++,
                      0, 0,
                      il.getStart(), il.getStart())
            );

        // Add calculated nodes
        Iterator leadersIt = leaders.iterator();
        Iterator endIt = end.iterator();
        while (leadersIt.hasNext() && endIt.hasNext()) {
            InstructionHandle startHandle =
                (InstructionHandle) leadersIt.next();
            InstructionHandle endHandle =
                (InstructionHandle) endIt.next();

            Node node = new Node(pos++);
            node.setStartOffset(startHandle.getPosition());
            node.setEndOffset(endHandle.getPosition());
            node.setStartRef(startHandle);
            node.setEndRef(endHandle);
            cfg.addNode(node);

            // Add dummy return node after call blocks
            if (calls.contains(endHandle)) {
                int offset = endHandle.getPosition();
                cfg.addNode(
                    new Node(pos++,
                              offset, offset,
                              endHandle, endHandle)
                    );
            }
        }

        // Create and add exit block
        InstructionHandle endHandle = (InstructionHandle) end.last();
        int offset = endHandle.getPosition();
        normalExitNodeID = pos;
        cfg.addVirtualNode(
            new Node(pos++,
                      offset, offset,
                      endHandle, endHandle)
            );

        // Create and add the summary block for all exceptional
        // exits which are not precisely represented in the
        // control flow (operators, array-bounds, etc...)
        cfg.addVirtualNode(
            new Node(pos++,
                      0, il.getEnd().getPosition(),
                      il.getStart(), il.getEnd())
            );
    }

    /**
     * Computes and adds the edges between basic blocks to the control
     * flow graph.
     *
     * @param cfg Control flow graph to which to add the edges.
     * @param pendingInference <strong>out</strong> Exception throwing
     * blocks for which type inference should be performed are placed
     * in this list.
     */
    @SuppressWarnings("unchecked")
    private void formEdges(CFG cfg, List<Object> pendingInference) {
        int targetOffset = 0;
        int nextID = 1;

        // Holds JSR edges which are waiting to have their special node ID set
        // (to the block containing the corresponding RET).
        Map<Object, Object> pendingJSRs = new HashMap();

        // Create edge between entry block and first real block
        cfg.addEdge(new Edge(nextID++, 2, 1, "E"));

        int pos = 2;
        for (Iterator it = end.iterator(); it.hasNext(); ) {
            InstructionHandle ih = (InstructionHandle) it.next();

            Instruction instr = ih.getInstruction();
            if (instr instanceof Select) { // Switch constructs
                Select selectInstr = (Select) instr;
                InstructionHandle[] targets = selectInstr.getTargets();
                int[] matches = selectInstr.getMatchs();

                if (matches.length != targets.length) {
                    throw new ClassFormatError("Invalid switch instruction: " +
                        ih.toString().trim());
                }

                Node node = cfg.getNode(pos);

                for (int j = 0; j < targets.length; j++) {
                	node = (Node) cfg.nodeOffsetMap.get(
                        targets[j].getPosition());
                    cfg.addEdge(
                        new Edge(nextID++,
                        		node.getID(),
                                   pos,
                                   Integer.toString(matches[j]))
                        );
                }
                // Add default. Easier to do it this way than to do a bunch
                // of manipulations just to add it to the end of the
                // targets array.
                node = (Node) cfg.nodeOffsetMap.get(
                    selectInstr.getTarget().getPosition());
                cfg.addEdge(
                    new Edge(nextID++, node.getID(), pos, "D")); // D denotes "Default"
            }
            else if ((instr instanceof GotoInstruction) ||
                     (instr instanceof JsrInstruction))
                {
                BranchInstruction branchInstr = (BranchInstruction) instr;
                InstructionHandle target = branchInstr.getTarget();
                targetOffset = target.getPosition();
                Node predecessor = cfg.getNode(pos);
                Node successor = (Node) cfg.nodeOffsetMap.get(targetOffset);

                if (instr instanceof JsrInstruction) {
//                    CFEdge e = (CFEdge) pendingJSRs.get(ih);
//                    if (e == null) {
//                        // We initially set the special node ID to be the
//                        // same as the successor. (See footer comment #1).
//                        e = new CFEdge(nextID++,
//                                       successor.getID(),
//                                       pos,
//                                       "jsr",
//                                       String.valueOf(ih.getPosition()),
//                                       successor.getID());
//                        pendingJSRs.put(ih, e);
//                    }
//                    else {
//                        e.setPredNodeID(pos);
//                        e.setSuccNodeID(successor.getID());
//                        e.setID(nextID++);
//                        pendingJSRs.remove(ih);
//                    }
//                    cfg.addEdge(e);
                }
                else {
                    // Just a boring old GOTO
                    cfg.addEdge(
                        new Edge(nextID++, successor.getID(), pos, "E")); // E denotes empty(normal flow)
                }
            }

            else if (instr instanceof IfInstruction) {
            	Node block = cfg.getNode(pos);
                // Target if true
                targetOffset =
                    ((IfInstruction) instr).getTarget().getPosition();
                block = (Node) cfg.nodeOffsetMap.get(targetOffset);
               // cfg.addEdge(new Edge(nextID++, block.getID(), pos, "T"));
                cfg.addEdge(new Edge(nextID++, block.getID(), pos, "F"));
                // Target if false (flow to next block)
               // cfg.addEdge(new Edge(nextID++, pos + 1, pos, "F"));
                cfg.addEdge(new Edge(nextID++, pos + 1, pos, "T"));
            }

            else if (instr instanceof ReturnInstruction){
            	Node block = cfg.getNode(pos);
            	cfg.addEdge(new Edge(nextID++, normalExitNodeID, pos, "E")); // E denotes empty(normal flow)
        	}else { // Normal flow from the current block to next block
            	Node block = cfg.getNode(pos);
                cfg.addEdge(new Edge(nextID++, pos + 1, pos, "E"));
            }
            pos++;
        }

        cfg.nextEdgeID = nextID;
    }





    /*************************************************************************
     * Comparator for InstructionHandle objects.
     *
     * InstructionHandles are considered equal if they refer to the same
     * object. Otherwise they are ordered according to their byte code
     * offset.
     */
    private class IHandleComparator implements Comparator<Object> {
        /**
         * Implementation of the <code>compare</code> method defined
         * by interface <code>Comparator</code>.
         */
        public int compare(Object o1, Object o2) {
            if (o1 == o2) return 0;
            if (((InstructionHandle) o1).getPosition()
                    < ((InstructionHandle) o2).getPosition()) {
                return -1;
            }
            else {
                return 1;
            }
        }
    }




    public static void main(String[] args){
    	CFGBuilder cfgb = new CFGBuilder();
      	String entry = "MethodsTran";
      	try {
      		cfgb.parseClass(entry);
      	}
      	catch (Exception e) {
      		String msg = e.getMessage();
      		System.err.println(msg);
      		System.exit(1);
      	}

      	try {
      		cfgb.buildAllCFG();
      	}
      	catch (Exception e) {
      		System.err.println(e.getMessage());
      		System.exit(1);
      	}
    }

    class VarInfo {
    	String varName;
    	String varType;

    	public VarInfo(String varName, String varType) {
    		this.varName = varName;
    		this.varType = varType;
    	}

    	public String toString() {
    		String retVal = "[";
    		retVal = retVal.concat(this.varName +" ," + this.varType +"]");
    		return retVal;
    	}
    }
}
