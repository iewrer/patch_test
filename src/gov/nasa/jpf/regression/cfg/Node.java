package gov.nasa.jpf.regression.cfg;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class Node {
    /** The node's unique ID. */
    protected int nodeID;

    /** Collection of the node's successors. */
    protected List<Node> successors = new ArrayList<Node>(4);
    /** Collection of the node's predecessors. */
    protected List<Node> predecessors = new ArrayList<Node>(4);

    /** The start offset of this block. */
    protected int startOffset;
    /** The end offset of this block. */
    protected int endOffset;

    protected int startLineNumber =0;
    protected int endLineNumber=0;

    /** List of all positions within the Node */
    protected List<Integer> positions = new ArrayList<Integer>();

    /** Reference to a relevant entity at the start of the block.
        This field is not preserved when the CFG is written to file. */
    protected Object startRef;
    /** Reference to a relevant entity at the end of the block.
        This field is not preserved when the CFG is written to file. */
    protected Object endRef;
    
    protected boolean nodeIsChanged = false;

    protected boolean isAssert = false;
    
    /** Contains a value for each position indicating its change status
     * key = pos; value = change type (below)
     *  0: unchanged
     *  1: modified/changed
     *  2: added
     *  3: removed
     */
    protected Map<Integer,Integer> changeTypes = new HashMap<Integer,Integer>();
    
    /** tarjans' algorithm **/
    public int index = -1;
    public int lowlink;

    /**
     * Creates a new block with fields initialized to default values.
     */
    public Node() {
    	this.nodeID = 0;
        this.startOffset = 0;
        this.endOffset = 0;
        this.startRef = null;
        this.endRef = null;

    }

    /**
     * Creates a new block with a given ID and all other fields initialized
     * to default values.
     *
     * @param id ID to be associated with the new block.
     */
    public Node(int id) {
    	this.nodeID = id;
        this.startOffset = 0;
        this.endOffset = 0;
        this.startRef = null;
        this.endRef = null;
    }



    /**
     * Creates a new block.
     *
     * @param id ID to be associated with the new block.
     * @param type Main type of the new block.
     * @param subType Secondary type of the new block.
     * @param label Label (character) associated with the new block.
     * @param startOffset Offset of the first bytecode instruction in
     * the new block.
     * @param endOffset Offset of the last bytecode instruction in the
     * new block.
     */
     public Node(int id,  int startOffset, int endOffset) {
    	 this.nodeID = id;
        this.startOffset = startOffset;
        this.endOffset = endOffset;
        this.startRef = null;
        this.endRef = null;
    }

     /**
      * Creates a new block.
      *
      * @param id ID to be associated with the new block.
      * @param type Main type of the new block.
      * @param subType Secondary type of the new block.
      * @param label Label (character) associated with the new block.
      * @param startOffset Offset of the first bytecode instruction in
      * the new block.
      * @param startRef Reference to an object with some meaningful correlation
      * to the start of the new block; typically used to reference the BCEL
      * instruction handle of the first instruction in the block.
      * @param endRef Reference to an object with some meaningful correlation
      * to the end of the new block; typically used to reference the BCEL
      * instruction handle of the last instruction in the block.
      */
      public Node(int id, int startOffset, int endOffset,
                   Object startRef, Object endRef) {
    	  this.nodeID = id;
         this.startOffset = startOffset;
         this.endOffset = endOffset;
         this.startRef = startRef;
         this.endRef = endRef;
     }

      public void addType(Integer position, String type){
    	  if (type.equalsIgnoreCase("changed")){
    		  this.changeTypes.put(position, 1);
    		  nodeIsChanged = true;
    	  }else if (type.equalsIgnoreCase("added")){
    		  this.changeTypes.put(position, 2);
    		  nodeIsChanged = true;
    	  }else if (type.equalsIgnoreCase("removed")){
    		  this.changeTypes.put(position, 3);
    		  nodeIsChanged = true;
    	  }else
    		  this.changeTypes.put(position, 0);
      }
      
      public void addPosition(Integer position) {
    	  positions.add(position);
      }

      public List<Integer> getPositions(){
    	  return positions;
      }

      public Map<Integer,Integer> getChangeTypes(){
    	  return this.changeTypes;
      }

      public int getStartLineNumber(){
    	  return this.startLineNumber;
      }

      public void setStartLineNumber(int start){
    	  this.startLineNumber = start;
      }

      public int getEndLineNumber(){
    	  return this.endLineNumber;
      }

      public void setEndLineNumber(int end){
    	  this.endLineNumber = end;
      }
      
      public boolean getNodeIsChanged(){
    	  return this.nodeIsChanged;
      }

      /*************************************************************************
       * Sets this node's ID.
       *
       * @param n ID to be assigned to this node.
       */
      public void setID(int n) {
          nodeID = n;
      }

      /*************************************************************************
       * Gets this node's ID.
       *
       * @return The ID assigned to this node.
       */
      public int getID() {
          return nodeID;
      }


      /*************************************************************************
       * Adds a node to this node's successor list.
       *
       * @param n Node to be added to the successor list.
       */
      public void addSuccessor(Node n) {
          successors.add(n);
      }

      /*************************************************************************
       * Removes a node from this node's successor list.
       *
       * @param n Node to be removed from the successor list.
       */
      public void removeSuccessor(Node n) {
          successors.remove(n);
      }

      /*************************************************************************
       * Adds a node to this node's predecessor list.
       *
       * @param n Node to be added to the predecessor list.
       */
      public void addPredecessor(Node n) {
          predecessors.add(n);
      }

      /*************************************************************************
       * Removes a node from this node's predecessor list.
       *
       * @param n Node to be removed from the predecessor list.
       */
      public void removePredecessor(Node n) {
          predecessors.remove(n);
      }

    /*************************************************************************
     * Sets the start offset variable to the given value.
     *
     * @param i Integer representing the new start offset for this block.
     */
    public void setStartOffset(int i) {
        startOffset = i;
    }

    /*************************************************************************
     * Gets the start offset for this block.
     *
     * @return Integer representing the start offset for this block.
     */
    public int getStartOffset() {
        return startOffset;
    }

    /*************************************************************************
     * Sets the end offset variable to the given value.
     *
     * @param i Integer representing the new end offset for this block.
     */
    public void setEndOffset(int i) {
        endOffset = i;
    }

    /*************************************************************************
     * Gets the end offset for this block.
     *
     * @return Integer representing the end offset for this block.
     */
    public int getEndOffset() {
        return endOffset;
    }

    /*************************************************************************
     * Sets a reference to an object of interest to be associated with
     * the start of the block.
     *
     * <p>For example, the {@link sofya.graphs.cfg.CFG} class stores a
     * reference to the BCEL <code>InstructionHandle</code> which represents
     * the beginning of the block to improve the performance of the
     * {@link sofya.ed.cfInstrumentor}.</p>
     *
     * @param ref Reference to object to be associated with the start of
     * the block.
     */
    public void setStartRef(Object ref) {
        startRef = ref;
    }

    /*************************************************************************
     * Gets the reference to an object of interest associated with the
     * start of the block.
     *
     * @return Reference to an object associated with the start of the
     * block.
     */
    public Object getStartRef() {
        return startRef;
    }

    /*************************************************************************
     * Sets a reference to an object of interest to be associated with
     * the end of the block.
     *
     * <p>For example, the {@link sofya.graphs.cfg.CFG} class stores a
     * reference to the BCEL <code>InstructionHandle</code> which represents
     * the end of the block to improve the performance of the
     * {@link sofya.ed.cfInstrumentor}.</p>
     *
     * @param ref Reference to object to be associated with the end of
     * the block.
     */
    public void setEndRef(Object ref) {
        endRef = ref;
    }

    /*************************************************************************
     * Gets the reference to an object of interest associated with the
     * end of the block.
     *
     * @return Reference to an object associated with the end of the
     * block.
     */
    public Object getEndRef() {
        return endRef;
    }


    /**
     * Returns a direct reference to the list of predecessors, for use
     * by handlers.
     *
     * @return A direct reference to the list of predecessors to this
     * block.
     */
    public List<Node> getPredecessorList() {
        return predecessors;
    }

    /**
     * Returns a direct reference to the list of successors, for use
     * by handlers.
     *
     * @return A direct reference to the list of successors to this
     * block.
     */
    public List<Node> getSuccessorList() {
        return successors;
    }


    /**
     * @return if the node is assertion or not
     */
	public boolean isAssert() {
		return isAssert;
	}

	/**
	 * Sets the node as assertion or code 
	 * @param isAssert
	 */
	public void setAssert(boolean isAssert) {
		this.isAssert = isAssert;
	}

    
    /*************************************************************************
     * Returns a string representation of this block in the form:<br>
     * <code>(nodeID, (nodeType, nodeSubType, nodeLabel),
     * [startOffset, endOffset])</code>.
     *
     * @return This block represented as a string.
     *
     * @see sofya.graphs.Node#toString
     */
    public String toString() {
        return new String("(" + super.toString() +
                          ", ("  +
                          "), " + "[" + startOffset + ", " + endOffset +
                          "], lineNumbers=" + this.startLineNumber + "..." + this.endLineNumber + ")");
    }

}

/****************************************************************************/
