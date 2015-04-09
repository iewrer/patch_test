package gov.nasa.jpf.regression.cfg;


import gov.nasa.jpf.regression.analysis.DistanceMatrix;

import java.math.BigInteger;
import java.util.*;

import jpf_diff.Dependency;


//import static base.SConstants.*;

//import gnu.trove.TIntObjectHashMap;

public class CFG{

	/** The collection of nodes in the graph. */
    protected List<Node> nodes = new ArrayList<Node>();

    /** The collection of edges in the graph. */
    protected List<Edge> edges = new ArrayList<Edge>();

    /** unique name of the method for which this CFG was built. */
    protected String uniqueName;  // Also the legacy key for handlers

    /** Lookup table which maps bytecode offsets to nodes, used to optimize
        retrieval of successor nodes when constructing edges. */
    protected HashMap nodeOffsetMap = new HashMap();


    /** For use with Graph.getEdges(Node,int,Edge[]), specifies that
    outgoing edges from the node will be matched. */
    public static final int MATCH_OUTGOING = 0;

    /** For use with Graph#getEdges(Node,int,Edge[]), specifies that
    incoming edges to the node will be matched. */
    public static final int MATCH_INCOMING = 1;

    /** Records the next available edge ID. Only used internally by the CFG
        builder and type inference module. Not stored to file.*/
    int nextEdgeID = -1;

    /** Conditional compilation debug flag. */
    @SuppressWarnings("unused")
	private static final boolean DEBUG = false;
    
    


    /**
     * Data structures for
     ***/
    Set<Integer> writeIns = new HashSet<Integer>();
    Set<Integer> ifIns = new HashSet<Integer>();
    Set<Integer> callIns = new HashSet<Integer>();
    Set<Integer> retIns = new HashSet<Integer>();

    public Map<Integer, Integer> posToID = new HashMap<Integer, Integer>();
    public Map<Integer, Integer> lineToID = new HashMap<Integer, Integer>();
    public Map<Integer, Set<Dependency> > oldDepend = new HashMap<Integer, Set<Dependency> >();
    
    HashMap<Integer, Integer> posToIndex = new HashMap<Integer, Integer>();
    HashMap<Integer, List<Integer>> edgesCFG = new HashMap<Integer, List<Integer>>();
    
    DistanceMatrix dm;

    Set<Integer> modifiedWritesAndIfs = new HashSet<Integer>();
    Set<Integer> modifiedCallSitesAndReturns = new HashSet<Integer>();

    Set<Integer> removedIns = new HashSet<Integer>();
    Set<Integer> tmpMod = new HashSet<Integer>();
    Set<Integer> asserts = new HashSet<Integer>();
    
    HashMap<Integer, Integer> startOffsetLookup = new HashMap<Integer, Integer>();
    HashMap<Integer, ArrayList<Integer>> controlD;

    public CFG () {

    }

    public DistanceMatrix getDistanceMatrix() {
    	return dm;
    }

    public Set<Integer> getRemovedInstructions() {
    	return removedIns;
    }

    public Set<Integer> getModifiedWritesAndIfs() {
    	return modifiedWritesAndIfs;
    }

    public Set<Integer> getModifiedCallsAndReturns() {
    	return this.modifiedCallSitesAndReturns;
    }

    public Integer getStartOffset(Integer pos) {
    	if(startOffsetLookup.containsKey(pos)) {
    		return startOffsetLookup.get(pos);
    	}
    	return -1;
    }

    public Integer getIndexOfPosition(Integer pos) {
    	if(posToIndex.containsKey(pos)) {
    		return posToIndex.get(pos);
    	}
    	return -1;
    }


    public Set<Integer> getAllWriteInsn() {
    	return writeIns;
    }

    public Set<Integer> getIfIns() {
    	return ifIns;
    }

    public Set<Integer> getCallIns() {
    	return callIns;
    }

    public Set<Integer> getRetInsn() {
    	return retIns;
    }

    /** Used by cache retrieval. */
   // CFG() { }


    /*************************************************************************
     * Creates a control flow graph.
     *
     * @param uniqueName Human-friendly string to represent the method
     * for which the control flow graph is/was built.
     */
    protected CFG(String uniqueName) {
        this.uniqueName = uniqueName;
    }


    /*************************************************************************
     * Gets the root node of the graph
     */
    public Node getRootNode() {
        return (Node) nodes.get(0);
    }


    /*************************************************************************
     * Removes a node from the graph.
     *
     */
    protected void removeNode(Node n) {
        nodes.remove(n);
    }


    /*************************************************************************
     * Removes an edge from the graph.
     *
     * @param e {@link sofya.graphs.Edge} to be removed from the graph.
     */
    protected void removeEdge(Edge e) {
        edges.remove(e);
    }

    /*************************************************************************
     * Gets an edge in the graph. This method will match only the
     * <strong>first</strong> edge which is found to have the given source
     * and sink nodes. Note that edges are searched in the order in which
     * they were added to the graph.
     *
     * @param sourceNode Source node of the edge to be matched.
     * @param sinkNode Sink node of the edge to be matched.
     *
     * @return The edge in the control flow graph with the specified source
     * node and sink node, if any.
     *
     * @throws NoSuchElementException If a matching edge cannot be found in
     * the graph.
     */
    public Edge getEdge(Node sourceNode, Node sinkNode) {
        for (int i = 0; i < edges.size(); i++) {
            Edge e = (Edge) edges.get(i);
            if ((e.predNodeID == sourceNode.nodeID)
                    && (e.succNodeID == sinkNode.nodeID)) {
                return e;
            }
        }
        throw new NoSuchElementException("No matching edge exists in graph");
    }

    /**
     * Utility method to find an edge with a given basic label in
     * a list of edges.
     *
     */
    public Edge matchLabelToEdge(List<Edge> edgeList, String edgeLabel) {
        for (int i = 0; i < edgeList.size(); i++) {
            if (edgeLabel == null) {
                if (edgeList.get(i).getLabel() == null) {
                    if (edgeList.size() > 1) {
                        System.err.println("WARNING: Graph may be invalid - " +
                            "null edge label detected in multiple edge set");
                    }
                    return edgeList.get(i);
                }
            }
            else if (edgeLabel.equals(edgeList.get(i).getLabel())) {
                return edgeList.get(i);
            }
        }
        return null;
    }

    /*************************************************************************
     * Gets all of the edges in the graph, in the order in which they
     * were added to the graph.
     *
     * @param a Array which will be used to determine the runtime type
     * of the array returned by this method.
     *
     * @return The complete set of edges in the graph, in order of addition.
     */
    public Edge[] getEdges(Edge[] a) {
        return (Edge[]) edges.toArray(a);
    }

    /*************************************************************************
     * Gets all edges which have the given source and sink nodes.
     *
     * <p><i>Special explanatory note regarding CFGs:</i> Under some
     * circumstances, it is possible for switch statements and
     * finally-block returns to have multiple edges which point to the
     * same successor node (e.g. multiple cases point to the same block or
     * multiple exceptions return to the same location after handling).
     * The {@link Graph#getEdge(Node,Node)} method will only return the first
     * matching edge, which may be insufficient.</p>
     *
     * @param sourceNode Source node of matching edges.
     * @param sinkNode Sink node of matching edges.
     * @param a Array which will be used to determine the runtime type
     * of the array returned by this method.
     *
     * @return The set of edges in the graph with the specified source node
     * and sink node, if any.
     *
     * @throws NoSuchElementException If no matching edges can be found in
     * the graph.
     */
    public Edge[] getEdges(Node sourceNode, Node sinkNode, Edge[] a) {
        List<Object> matchingEdges = new ArrayList<Object>();
        for (int i = 0; i < edges.size(); i++) {
            Edge e = (Edge) edges.get(i);
            if ((e.predNodeID == sourceNode.nodeID)
                    && (e.succNodeID == sinkNode.nodeID)) {
                matchingEdges.add(e);
            }
        }
        if (matchingEdges.size() > 0) {
            return (Edge[]) matchingEdges.toArray(a);
        }
        else {
            throw new NoSuchElementException("No matching edges " +
                "exist in graph");
        }
    }

    /*************************************************************************
     * Gets either all the edges which originate on a given node or which are
     * incident on a node.
     *
     * @param n Node for which associated edges will be returned.
     * @param matchType Constant indicating whether edges which start on the
     * node or which end on the node are to be returned. Acceptable values
     * are {@link Graph#MATCH_OUTGOING} and {@link Graph#MATCH_INCOMING}.
     * @param a Array which will be used to determine the runtime type
     * of the array returned by this method.
     *
     * @return Either the set of edges in the graph which start on the given
     * node or the set of edges which end on the node.
     *
     * @throws NoSuchElementException If no matching edges can be found in
     * the graph.
     */
    public Edge[] getEdges(Node n, int matchType, Edge[] a) {
        List<Object> matchingEdges = new ArrayList<Object>();
        for (int i = 0; i < edges.size(); i++) {
            Edge e = (Edge) edges.get(i);
            if ((matchType == MATCH_OUTGOING) && (e.predNodeID == n.nodeID)) {
                matchingEdges.add(e);
            }
            else if ((matchType == MATCH_INCOMING)
                    && (e.succNodeID == n.nodeID)) {
                matchingEdges.add(e);
            }
        }
        if (matchingEdges.size() > 0) {
            return (Edge[]) matchingEdges.toArray(a);
        }
        else {
        	return null;
//            throw new NoSuchElementException("No matching edges " +
//                "exist in graph");
        }
    }


    /*************************************************************************
     * Gets list of edges which originate on a given node or which are
     * incident on a node.
     */
    public List<Edge> getEdges(Node n, int matchType) {
        List<Edge> matchingEdges = new ArrayList<Edge>();
        for (int i = 0; i < edges.size(); i++) {
            Edge e = (Edge) edges.get(i);
            if ((matchType == MATCH_OUTGOING) && (e.predNodeID == n.nodeID)) {
                matchingEdges.add(e);
            }
            else if ((matchType == MATCH_INCOMING)
                    && (e.succNodeID == n.nodeID)) {
                matchingEdges.add(e);
            }
        }
        return matchingEdges;
    }

    /*************************************************************************
     * Gets the total number of nodes contained in the graph.
     *
     * @return The number of nodes in the graph.
     */
    public int getNodeCount() {
        return nodes.size();
    }

    /*************************************************************************
     * Gets the total number of edges contained in the graph.
     *
     * @return The number of edges in the graph.
     */
    public int getEdgeCount() {
        return edges.size();
    }

    /*************************************************************************
     * Clears the graph, such that it has no nodes or edges.
     */
    protected void clear() {
        nodes.clear();
        edges.clear();
    }

    /*************************************************************************
     * Gets the nodes in this control flow graph.
     *
     * @return An array containing the basic blocks identified in the method.
     */
    public Node[] getNodes() {
        Node b[] = new Node[nodes.size()];
        nodes.toArray(b);
        return b;
    }


    /*************************************************************************
     * Gets a node from the node list.
     *
     * @param id ID of the node to be retrieved.
     *
     * @return The node in the node list with the specified ID, if any.
     */
    public Node getNode(int id) {
        // Node IDs should be assigned contiguously and normally are
        // expected to correspond to their index (offset by 1) in the
        // node list.
        if (id > nodes.size()) {
            System.err.println("WARNING: CFG may contain non-contiguous " +
                "node IDs!");
            return findNode(id);
        }

        // If the expected preconditions are honored, we should normally
        // be done here
        Node node = (Node) nodes.get(id - 1);
        if (node.getID() == id) {
            return node;
        }
        else {
            // If the expected preconditions are not honored,
            // attempt to recover by sorting the node list on the
            // node IDs
            Collections.sort(nodes,
                new Comparator<Object>() {
                    public int compare(Object o1, Object o2) {
                        if (o1 == o2) return 0;
                        if (!o1.getClass().equals(Node.class) ||
                                !o2.getClass().equals(Node.class)) {
                            throw new ClassCastException();
                        }
                        int id1 = ((Node) o1).getID();
                        int id2 = ((Node) o2).getID();
                        if (id1 < id2) {
                            return -1;
                        }
                        else if (id1 == id2) {
                            return 0;
                        }
                        else {
                            return 1;
                        }
                    }
                }
            );
            // Retrieve and verify again
            node = (Node) nodes.get(id - 1);
            if (node.getID() == id) {
                return node;
            }
            else {
                // Major problem with node list, attempt a linear
                // search anyway
                System.err.println("WARNING: CFG contains non-contiguous or " +
                    "redundant node IDs - first match will be returned!");
                return findNode(id);
            }
        }
    }

    private Node findNode(int id) {
        Iterator iterator = nodes.iterator();
        for (int i = nodes.size(); i-- > 0; ) {
            Node node = (Node) iterator.next();
            if (node.getID() == id) {
                return node;
            }
        }
        throw new NoSuchElementException();
    }

    public Node getCFGNode(int startOffset) {
        Iterator<Node> iterator = nodes.iterator();
        for (int i = nodes.size(); i-- > 0; ) {
            Node node = (Node) iterator.next();
            if (node.getStartOffset() == startOffset) {
                return node;
            }
        }
        throw new NoSuchElementException();
    }

    /*************************************************************************
     * Adds an edge to the list of edges.
     *
     * @param e Edge to be added to the CFG.
     */
    protected void addEdge(Edge e) {
        edges.add(e);
        Node fromNode = getNode(e.getPredNodeID());
        Node toNode = getNode(e.getSuccNodeID());
        fromNode.addSuccessor(toNode);
        toNode.addPredecessor(fromNode);
    }

    /*************************************************************************
     * Adds a node to the CFG, creating an entry in the offset map so the
     * node can be retrieved by its start offset later.
     */
    protected void addNode(Node n) {
    	nodes.add(n);
        try {
            Node b = (Node) n;
            nodeOffsetMap.put(b.getStartOffset(), b);
        }
        catch (ClassCastException e) {
            // Not a block - won't be able to find it by offset
        }
    }

    protected void addVirtualNode(Node n) {
    	nodes.add(n);
    }



    /*************************************************************************
     * Returns the number of nodes in the control flow graph.
     *
     * @return The number of nodes in the control flow graph.
     */
    public int getNumberOfNodes() {
        return nodes.size();
    }

    /*************************************************************************
     * Returns the number of edges in the control flow graph.
     */
    public int getNumberOfEdges() {
        return edges.size();
    }



    /*************************************************************************
     * Returns the name of the method with which this control flow graph
     * is associated.
     *
     * @return The name of the method for which this control flow graph
     * was built.
     */
    public String getMethodName() {
        return uniqueName;
    }

    /*************************************************************************
     * Returns the ID of the root node of the control flow graph.
     *
     * @return The ID of the node that is the root of the control flow graph.
     */
    public int getRootNodeID() {
        return (getNode(1)).getID();
    }


    /*************************************************************************
     * Returns a direct reference to the list of nodes in the
     * CFG
     */
    List<Node> nodeList() {
        return nodes;
    }

    /*************************************************************************
     * Returns a direct reference to the list of edges in the
     * CFG, for use by handlers.
     *
     * @return The list of edges in the CFG.
     */
    List<Edge> edgeList() {
        return edges;
    }

    /*************************************************************************
     * Returns string representation of the control flow graph, which
     * is a list of the edges that constitute the CFG.
     *
     * @return List of edges that constitute this control flow graph.
     *
     * @see sofya.graphs.cfg.Block#toString()
     * @see sofya.graphs.Edge#toString()
     */
    public String toString() {
        return edgesToString();
    }

    /*************************************************************************
     * Gets string representation of the edges that compose the control flow
     * graph for this method.
     */
    private String edgesToString() {
        StringBuilder sb = new StringBuilder();
        String label;
        Edge e;
        for (int i = 0; i < edges.size(); i++) {
            e  = (Edge) edges.get(i);
            label = e.getLabel();
            if ((label == null) || (label.length() == 0)) {
                sb.append("(nl): ");
            }
            else {
                sb.append(label + ": ");
            }
            sb.append(getNode(e.getPredNodeID()).toString() + "  ->  " +
                      getNode(e.getSuccNodeID()).toString() + "\n");
        }
        return sb.toString();
    }

	public HashMap getNodeOffsetMap() {
		return nodeOffsetMap;
	}

	public void setNodeOffsetMap(HashMap nodeOffsetMap) {
		this.nodeOffsetMap = nodeOffsetMap;
	}
}

/****************************************************************************/
