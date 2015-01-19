package gov.nasa.jpf.regression.cfg;


import java.util.LinkedList;
import java.util.List;

import org.apache.bcel.Constants;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.Type;



public final class Utility {
    /** The utility class need not be instantiated. */
    private Utility() {
        throw new AssertionError("Illegal constructor");
    }

    /**
     * Converts a BCEL Type object to its corresponding java.lang.Class
     *
     * @param t BCEL type object to be converted.
     *
     * @return The class object corresponding to the given BCEL type.
     *
     * @throws ClassNotFoundException If the corresponding class cannot be
     * found by the classloader (most often because it is not on the
     * classpath), or if the BCEL type is <code>Type.UNKNOWN</code>.
     */
    public static final Class typeToClass(Type t)
            throws ClassNotFoundException {
        switch (t.getType()) {
        case Constants.T_BOOLEAN:
            return Boolean.TYPE;
        case Constants.T_CHAR:
            return Character.TYPE;
        case Constants.T_FLOAT:
            return Float.TYPE;
        case Constants.T_DOUBLE:
            return Double.TYPE;
        case Constants.T_BYTE:
            return Byte.TYPE;
        case Constants.T_SHORT:
            return Short.TYPE;
        case Constants.T_INT:
            return Integer.TYPE;
        case Constants.T_LONG:
            return Long.TYPE;
        case Constants.T_VOID:
            return Void.TYPE;
        case Constants.T_ARRAY:
            return Class.forName(t.getSignature().replace('/', '.'));
        case Constants.T_OBJECT:
            return Class.forName(((ObjectType) t).getClassName());
        default:
            throw new ClassNotFoundException();
        }
    }

    /**
     * Converts an array of BCEL Type objects to their
     * corresponding java.lang.Class objects.
     *
     * @param ts BCEL type objects to be converted.
     *
     * @return The class objects corresponding to the given BCEL types.
     *
     * @throws ClassNotFoundException If a corresponding class cannot be
     * found by the classloader (most often because it is not on the
     * classpath), or if a BCEL type is <code>Type.UNKNOWN</code>.
     */
    public static final Class[] typesToClasses(Type[] ts)
            throws ClassNotFoundException {
        int size = ts.length;
        Class[] classes = new Class[size];
        for (int i = 0; i < size; i++) {
            classes[i] = typeToClass(ts[i]);
        }
        return classes;
    }
    
    

    
    /**
     * Utility method to find all edges with the given source and sink
     * nodes in a list of edges.
     *
     * @param edgeList List of edges to be searched, as an array.
     * @param sourceNode Node which must be the predecessor node of
     * a matching edge.
     * @param sinkNode Node which must be the successor node of a
     * matching edge.
     *
     * @return A list of all matching edges found in the provided
     * edge list, which may be of length zero if no matching edges
     * were found.
     */
    protected List<Edge> findEdges(Edge[] edgeList,
                             Node sourceNode, Node sinkNode) {
        List<Edge> matching = new LinkedList<Edge>();
        for (int i = 0; i < edgeList.length; i++) {
            if ((edgeList[i].getPredNodeID() == sourceNode.getID())
                    && (edgeList[i].getSuccNodeID() == sinkNode.getID())) {
                matching.add(edgeList[i]);
            }
        }
        return matching;
    }
    
    
    /**
     * Utility method to format the unique method signiture from Method
     * @param fullClassName
     * @return
     */
    public static final String formatUniqueName(String className, Method m) {
        return (className + "." + m.getName() + m.getSignature());
    }
    
}
