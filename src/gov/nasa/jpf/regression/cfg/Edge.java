package gov.nasa.jpf.regression.cfg;

public class Edge {
    /** The edge's ID. */
    protected int edgeID;
    /** ID of the node on which this edge is incident. */
    protected int succNodeID;
    /** ID of the node from which this edge originates. */
    protected int predNodeID;
    /** The edge's label. D: switch default; [1...number]: switch flow; T; F; E: normal flow*/ 
    protected String label;
    protected boolean dangerous =false;

    
    /*************************************************************************
     * Creates an edge with ID zero, an empty label, and the successor and
     * predecessor nodes set to zero.
     */
    public Edge() {
        edgeID = 0;
        succNodeID = 0;
        predNodeID = 0;
        label = null;
    }

    /*************************************************************************
     * Creates an edge with the given label, ID, successor, and predecessor
     * nodes.
     *
     * @param id ID of the new edge.
     * @param s ID of the new edge's successor node.
     * @param p ID of the new edge's predecessor node.
     * @param label Label to be assigned to the new edge.
     */
    public Edge(int id, int s, int p, String label) {
        edgeID = id;
        succNodeID = s;
        predNodeID = p;
        this.label = label;
    }

    /*************************************************************************
     * Sets the ID of this edge.
     *
     * @param id New ID to assign to this edge.
     */
    public void setID(int id) {
        edgeID = id;
    }

    /*************************************************************************
     * Gets the ID of this edge.
     *
     * @return The ID assigned to this edge.
     */
    public int getID() {
        return edgeID;
    }

    /*************************************************************************
     * Sets the successor node of this edge.
     *
     * @param s ID of the node that is to be the new successor of this edge.
     */
    public void setSuccNodeID(int s)
    {
        succNodeID = s;
    }

    /*************************************************************************
     * Gets the successor node of this edge. 
     *
     * @return The ID of the successor node of this edge.
     */
    public int getSuccNodeID()
    {
        return succNodeID ;
    }

    /*************************************************************************
     * Sets the predecessor node of this edge.
     *
     * @param p ID of the node that is to be the new predecessor node of this
     * edge.
     */
    public void setPredNodeID(int p)
    {
        predNodeID = p;
    }

    /*************************************************************************
     * Gets the predecessor node of this edge. 
     *
     * @return The ID of the predecessor node of this edge.
     */
    public int getPredNodeID()
    {
        return predNodeID ;
    }

    /*************************************************************************
     * Sets the label for this edge.
     *
     * <p>Assigning version-dependent values to this field should be
     * avoided.</p>
     *
     * @param s New label to be assigned to this edge.
     */
    public void setLabel(String s) {
        label = s;
    }

    /*************************************************************************
     * Gets the label for this edge.
     *
     * @return The label assigned to this edge.
     */
    public String getLabel(){
        return label;
    }
    
    public boolean getDangerous(){
    	return dangerous;
    }
    
    public void setDangerous(boolean danger){
    	this.dangerous = danger;
    }

    /*************************************************************************
     * Returns a string representation of this edge in the form:<br>
     * <code>label: sourceNodeID -&gt; sinkNodeID</code>.
     * <p>If the edge is unlabeled, the special token "<code>(nl)</code>" is
     * used for the label.</p>
     *
     * @return This edge represented as a string. 
     */
    public String toString() {
        if ((label == null) || (label.length() == 0)) {
            return new String("(nl): " + predNodeID + " -> " + succNodeID);
        }
        else {
            return new String(label + ": " + predNodeID + " -> " + succNodeID);
        }
    }

    /*************************************************************************
     * Test driver for the Edge class.
     */
    public static void main(String[] args){
        Edge e = new Edge();
        System.out.println(e.label);
        e.setLabel("t");
        System.out.println(e.getLabel());
    }

}

/****************************************************************************/
