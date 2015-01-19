package gov.nasa.jpf.regression.cfg;

//import gnu.trove.THashMap;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;

import org.apache.bcel.Repository;
import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.Constant;
import org.apache.bcel.classfile.ConstantPool;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ArrayType;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.ObjectType;

/**
 * The ByteSourceHandler provides routines to retrieve bytecode information
 * from Java binary class files.
 */
@SuppressWarnings("unchecked")
public class ByteSourceHandler {
    /** BCEL representation of class file being read. */
    public JavaClass classFile;
    /** BCEL representation of the class constant pool. */
    private ConstantPoolGen cpg;
    /** Map which links method names to BCEL method objects. */
    private Map<Object, Object> methodsMap = new HashMap();
    /** Map which uses a formal object representation of method signatures
        as keys to BCEL method objects. */

    /** Name of class file being read. */
    private String className;


    /*************************************************************************
     * Parses the class.
     *
     * <p>Uses BCEL to parse the class file.</p>
     *
     */
    protected void parseClass(String className)
                   throws Exception {
    	try {
    		classFile =  Repository.lookupClass(className);
        }catch (ClassNotFoundException e) {
        	// Maybe it's an absolute (or otherwise qualified) path

        	File f = new File(className);
            if (!f.exists()) {
            	throw new Exception("Cannot find class: " + className);
            }
            classFile = new ClassParser(className).parse();
        }
    }
    

   
    /*************************************************************************
     * Reads bytecode source from file.
     *
     * <p>The bytecode source is read from the class file and stored to
     * internal data structures. This data can then be retrieved via other
     * accessor functions in this class, such as {@link #getInstructions}.</p>
     *
     * @param className Name of Java class file from which bytecode source
     * data is to be read.
     *
     * @throws IOException If there is an error reading the class file.
     */
    public void readSourceFile(String className) throws Exception {
        methodsMap.clear();

        parseClass(className);

        initClass();
    }

    /**
     * Initializes internal data structures containing information about the
     * currently loaded class.
     */
    private void initClass() {
        this.cpg = new ConstantPoolGen(classFile.getConstantPool());
        this.className = classFile.getClassName();

        Method[] methods = classFile.getMethods(); 
        for (int i = 0; i < methods.length; i++) {
            methodsMap.put(Utility.formatUniqueName(className, methods[i]),
                        methods[i]);
        }
    }

    
    /*************************************************************************
     * Gets the list of methods read from the class file.
     *
     * @return List of names of the methods read from the class file.
     */
    public String[] getMethodList() {
        String[] methodList = (String[]) methodsMap.keySet().toArray(
            new String[methodsMap.size()]);
        Arrays.sort(methodList);
        return methodList;
    }


    /*************************************************************************
     * Gets bytecode source for a method in human-readable form.
     *
     * @param methodName Name of the method for which the bytecode source is
     * to be retrieved.
     *
     * @return String containing list of instructions constituting the
     * specified method, in human-readable format using Java Virtual Machine
     * specification assembly mnemonics for instructions, or <code>null</code>
     * if the specified method cannot be found.
     *
     * @throws MethodNotFoundException If the handler has no bytecode listing
     * associated with a method of the specified name.
     */
    public String getSource(String methodName) throws Exception {
        Code code = null;
        Method m = (Method) methodsMap.get(methodName);
        if (m != null) {
            code = m.getCode();
            if (code != null) {
                return code.toString();
            }
        }
        throw new Exception(methodName);
    }

   
    /*************************************************************************
     * Gets bytecode source for a method as a list of string representations
     * of the bytecode instructions.
     *
     * @param methodName Name of the method for which the bytecode source is
     * to be retrieved.
     *
     * @return List of strings constituting the instructions of the specified
     * method, which may be zero-length if the instruction is
     * <code>abstract</code> or otherwise contains no instructions.
     *
     * @throws MethodNotFoundException If the handler has no bytecode listing
     * associated with a method of the specified name.
     */
    public String[] getInstructions(String methodName)
                    throws Exception {
        Method m = (Method) methodsMap.get(methodName);
        if (m == null) throw new Exception(methodName);
        return getInstructions(m);
    } 



    /*************************************************************************
     * Retrieves the source for a method as a list of strings
     *
     * @param m BCEL representation of method for which source is to be
     * retrieved.
     *
     * @return List of strings constituting the instructions in the specified
     * method, which may be zero-length if the instruction is
     * <code>abstract</code> or otherwise contains no instructions.
     */
    private String[] getInstructions(Method m) {
        InstructionList il = new MethodGen(m, className, cpg)
                                          .getInstructionList();
        if ((il == null) || (il.getLength() == 0)) return new String[0];

        String[] instrList = new String[il.getLength()];
        InstructionHandle ih = il.getStart();
        int index = 0;
        while (ih != null) {
            instrList[index] = ih.getInstruction().toString();
            ih = ih.getNext();
            index++;
        }
        return instrList;
    }

    /*************************************************************************
     * Gets bytecode source for a method as a BCEL
     * <code>InstructionList</code>.
     *
     * @param methodName Name of the method for which the bytecode source is
     * to be retrieved.
     *
     * @return BCEL <code>InstructionList</code> object containing the
     * bytecode instructions that constitute the specified method.
     *
     * @throws MethodNotFoundException If the handler has no bytecode listing
     * associated with a method of the specified name.
     */
    public InstructionList getInstructionList(String methodName)
                           throws Exception {
        Method m = (Method) methodsMap.get(methodName);
        if (m == null) throw new Exception(methodName);
        return getInstructionList(m);
    }


    /*************************************************************************
     * Retrieves the source for a method as a BCEL
     *
     * @param m BCEL representation of method for which source is to be
     * retrieved.
     *
     * @return BCEL <code>InstructionList</code> object containing the
     * bytecode instructions that constitute the specified method.
     */
    private InstructionList getInstructionList(Method m) {
        InstructionList il = new MethodGen(m, className, cpg)
                                          .getInstructionList();
        if (il == null) {  // Abstract or native?
            return new InstructionList();
        }
        else {
            return il;
        }
    }

    /*************************************************************************
     * Gets the bytecode source in a method between two instruction offsets,
     * inclusive.
     *
     * @param methodName Name of the method for which the bytecode source is
     * to be retrieved.
     * @param startOffset Offset to the first instruction to be retrieved.
     * @param endOffset Offset to the last instruction to be retrieved.
     *
     * @return List of strings representing the instructions in the specified
     * range, which will be of length zero if the method cannot be found
     * or is <code>abstract</code>.
     *
     * @throws MethodNotFoundException If the handler has no bytecode listing
     * associated with a method of the specified name.
     */
    public String[] getInstructions(String methodName, int startOffset,
                        int endOffset) throws Exception {
        if (!methodsMap.containsKey(methodName)) {
            throw new Exception(methodName);
        }
        return getInstructions((Method) methodsMap.get(methodName),
                               startOffset, endOffset);
    }

 

    /*************************************************************************
     * Retrieves the source in a method between two offsets
     *
     * @param m BCEL representation of method for which source is to be
     * retrieved.
     * @param startOffset Offset to the first instruction to be retrieved.
     * @param endOffset Offset to the last instruction to be retrieved.
     *
     * @return List of strings representing the instructions in the specified
     * range, which will be of length zero if the method cannot be found
     * or is <code>abstract</code>.
     */
    private String[] getInstructions(Method m, int startOffset, int endOffset) {
        InstructionList il = new MethodGen(m, className, cpg)
                                          .getInstructionList();
        if ((il == null) || (il.getLength() == 0)) return new String[0];
        ArrayList<Object> instructions = new ArrayList<Object>();

        InstructionHandle ih = il.findHandle(startOffset);
        InstructionHandle ihEnd = il.findHandle(endOffset);
        if (ihEnd == null) ihEnd = il.getEnd();

        while (ih != null) {
            if (ih == ihEnd) {
                instructions.add(ih.getInstruction().toString());
                break;
            }
            instructions.add(ih.getInstruction().toString());
            ih = ih.getNext();
        }
        return (String[]) instructions.toArray(new String[instructions.size()]);
    }

    /*************************************************************************
     * Gets the bytecode source in a method between two instruction offsets,
     * inclusive.
     *
     * @param methodName Name of the method for which the bytecode source is
     * to be retrieved.
     * @param startOffset Offset to the first instruction to be retrieved.
     * @param endOffset Offset to the last instruction to be retrieved.
     *
     * @return Array of BCEL <code>Instruction</code> objects representing
     * the bytecode in the given range.
     *
     * @throws MethodNotFoundException If the handler has no bytecode listing
     * associated with a method of the specified name.
     */
    public Instruction[] getInstructionList(String methodName, int startOffset,
                             int endOffset) throws Exception {
        if (!methodsMap.containsKey(methodName)) {
            throw new Exception(methodName);
        }
        return getInstructionList((Method) methodsMap.get(methodName),
                                  startOffset, endOffset);
    }



    /*************************************************************************
     * Retrieves the source in a method between two offsets
     *
     * @param m BCEL representation of method for which source is to be
     * retrieved.
     * @param startOffset Offset to the first instruction to be retrieved.
     * @param endOffset Offset to the last instruction to be retrieved.
     *
     * @return Array of BCEL <code>Instruction</code> objects representing
     * the bytecode in the given range.
     */
    private Instruction[] getInstructionList(Method m, int startOffset,
                                             int endOffset) {
        InstructionList il = new MethodGen(m, className, cpg)
                                          .getInstructionList();
        if ((il == null) || (il.getLength() == 0)) return new Instruction[0];
        ArrayList<Object> instrList = new ArrayList<Object>(); 

        InstructionHandle ih = il.findHandle(startOffset);
        InstructionHandle ihEnd = il.findHandle(endOffset);
        if (ihEnd == null) ihEnd = il.getEnd();

        while (ih != null) {
            if (ih == ihEnd) {
                instrList.add(ih.getInstruction());
                break;
            }
            instrList.add(ih.getInstruction());
            ih = ih.getNext();
        }
        return (Instruction[]) instrList.toArray(
                new Instruction[instrList.size()]);
    }

 



    /*************************************************************************
     * Returns an unmodifiable view of the BCEL ConstantPoolGen object
     * for the class file loaded in the handler, which is required to obtain
     * information about certain types of instructions.
     *
     * @return An unmodifiable view of the constant pool for the class
     * currently loaded by the handler. Calling a method on the returned
     * object that would cause a change to the constant pool will result
     * in an <code>UnsupportedOperationException</code> being thrown.
     */
    public ConstantPoolGen getConstantPool() {
        return new UnmodifiableCPG(cpg);
    }





    /*************************************************************************
     * Wrapper which provides an unmodifiable view of a BCEL ConstantPoolGen
     * object.
     *
     * <p>It may seem strange to make an object intended for building a
     * constant pool unmodifiable, however the ConstantPoolGen class is
     * required as an argument to numerous accessor methods on Instruction
     * objects. Therefore to allow access to necessary information about
     * instructions, we must expose a reference to the ConstantPoolGen for
     * the class, but want to do so in a safe manner.</p>
     */
    @SuppressWarnings("serial")
    private class UnmodifiableCPG extends ConstantPoolGen {
        ConstantPoolGen cpg;
        private UnmodifiableCPG() { }
        public UnmodifiableCPG(ConstantPoolGen cpg) {
            if (cpg == null) throw new NullPointerException();
            this.cpg = cpg;
        }
        public int addArrayClass(ArrayType type) {
            throw new UnsupportedOperationException();
        }
        public int addClass(ObjectType type) {
            throw new UnsupportedOperationException();
        }
        public int addClass(String str) {
            throw new UnsupportedOperationException();
        }
        public int addConstant(Constant c, ConstantPoolGen cp) {
            throw new UnsupportedOperationException();
        }
        public int addDouble(double n) {
            throw new UnsupportedOperationException();
        }
        public int addFieldref(String class_name, String field_name,
                               String signature) {
            throw new UnsupportedOperationException();
        }
        public int addFloat(float n) {
            throw new UnsupportedOperationException();
        }
        public int addInteger(int n) {
            throw new UnsupportedOperationException();
        }
        public int addInterfaceMethodref(MethodGen method) {
            throw new UnsupportedOperationException();
        }
        public int addInterfaceMethodref(String class_name,
                                         String method_name,
                                         String signature) {
            throw new UnsupportedOperationException();
        }
        public int addLong(long n) {
            throw new UnsupportedOperationException();
        }
        public int addMethodref(MethodGen method) {
            throw new UnsupportedOperationException();
        }
        public int addMethodref(String class_name, String method_name,
                                String signature) {
            throw new UnsupportedOperationException();
        }
        public int addNameAndType(String name, String signature) {
            throw new UnsupportedOperationException();
        }
        public int addString(String str) {
            throw new UnsupportedOperationException();
        }
        public int addUtf8(String n) {
            throw new UnsupportedOperationException();
        }
        public Constant getConstant(int i)
            { return cpg.getConstant(i); }
        public ConstantPool getConstantPool()
            { return cpg.getConstantPool(); }
        public ConstantPool getFinalConstantPool()
            { return cpg.getFinalConstantPool(); }
        public int getSize()
            { return cpg.getSize(); }
        public int lookupClass(String str)
            { return cpg.lookupClass(str); }
        public int lookupDouble(double n)
            { return cpg.lookupDouble(n); }
        public int lookupFieldref(String class_name, String field_name,
                                  String signature) {
            return cpg.lookupFieldref(class_name, field_name, signature);
        }
        public int lookupFloat(float n)
            { return cpg.lookupFloat(n); }
        public int lookupInteger(int n)
            { return cpg.lookupInteger(n); }
        public int lookupInterfaceMethodref(MethodGen method) {
            return cpg.lookupInterfaceMethodref(method);
        }
        public int lookupInterfaceMethodref(String class_name,
                                            String method_name,
                                            String signature) {
            return cpg.lookupInterfaceMethodref(class_name,
                method_name, signature);
        }
        public int lookupNameAndType(String name, String signature) {
            return cpg.lookupNameAndType(name, signature);
        }
        public int lookupString(String str)
            { return cpg.lookupString(str); }
        public int lookupUtf8(String n)
            { return cpg.lookupUtf8(n); }
        public void setConstant(int i, Constant c) {
            throw new UnsupportedOperationException();
        }
        public String toString()
            { return cpg.toString(); }
    }

    /*************************************************************************
     * Test driver for ByteSourceHandler.
     */ 
    public static void main(String args[]) {
//        ByteSourceHandler src = new ByteSourceHandler();
//        String[] methods;
//        String[] srcLines;
//
//        try {
//            src.readSourceFile(args[0]);
//        } catch (Exception e) {
//            e.printStackTrace();
//            System.out.println(e.getMessage()) ;
//            System.exit(1);
//        } 
//
//        methods = src.getMethodList();
//        for (int i = 0; i < methods.length; i++) {
//            try {
//                srcLines = src.getInstructions(methods[i]);
//                /*Instruction[] il = src.getInstructionList(methods[i], 0, 9);
//                for (int j = 0; j < il.length; j++) {
//                    System.out.println(il[j].toString());
//                }
//                System.out.println("-----");*/
//            }
//            catch (Exception e) {
//                System.err.println("Warning: handler falsely claimed to " +
//                    "have instruction list for " + methods[i]);
//                continue;
//            }
//            System.out.println("MethodName: " + methods[i]);
//            System.out.println("Contents:");
//            for (int j = 0; j < srcLines.length; j++) {
//                System.out.println(srcLines[j]);
//            }
//        }
//
//        // Check retrieval by signature keys
//        MethodSignature[] keys = src.getSignatureList();
//        for (int i = 0; i < keys.length; i++) {
//            System.out.println(keys[i]);
//        }
    }
}



/*****************************************************************************/


