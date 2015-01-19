package gov.nasa.jpf.regression.ast;

import gov.nasa.jpf.regression.block.jaxbiface.BlockInfo;
import gov.nasa.jpf.regression.block.jaxbiface.BlockSummary;
import gov.nasa.jpf.regression.block.jaxbiface.ClassSummary;
import gov.nasa.jpf.regression.block.jaxbiface.MethodSummary;

import java.io.File;
import java.io.FileNotFoundException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class ASTLoader {

	//key=className+methodName value=MethodASTInfo
	Map<String,MethodASTInfo> methodASTInfo =
		new HashMap<String,MethodASTInfo>();
	
	private String direction = "regular";
	
	String fName = "";
	private String modFileName = "";
	private String origFileName = "";

	/*
	 * For each method, store the beginning and end of the block and
	 * its change type
	 * 0=unchanged, 1=modified, 2=added, 3=removed
	 */
	private void processMethod(MethodSummary mSum, String className){
		MethodASTInfo mi = new MethodASTInfo(className, mSum.getMethodName());
		mi.setEquivalent(mSum.isEquivalent());
		mi.setMatched(mSum.isMatched());
		mi.setOrigModifiers(mSum.getOrigModifiers());
		mi.setModModifiers(mSum.getModModifiers());
		mi.setOriginalFileName(origFileName);
		mi.setModifiedFileName(modFileName);
		if (mSum.getDefaultConstructorName() == null)
			mi.setDefaultConstructorName("");
		else
			mi.setDefaultConstructorName(mSum.getDefaultConstructorName());
		if(mSum.isMatched()){
			mi.setReturnType(mSum.getModMethodReturnType());
			if (mSum.getModMethodParameters().size()>0){
				for (int i=0; i<mSum.getModMethodParameters().size();i++){
					mi.addParameter(mSum.getModMethodParameters().get(i));
					mi.addParamType(mSum.getModMethodParamTypes().get(i));
				}
			}
			if (!mSum.isEquivalent()){
				mi.setOrigReturnType(mSum.getOrigMethodReturnType());
				if (mSum.getOrigMethodParameters().size()>0){
					for (int i=0; i<mSum.getOrigMethodParameters().size();i++){
						mi.addOrigParameter(mSum.getOrigMethodParameters().get(i));
						mi.addOrigParamType(mSum.getOrigMethodParamTypes().get(i));
					}
				}
			}
		}else{ //added or removed
			//first check if there is a method body -- won't be if method is abstract
			if (mSum.getBlockInfo().size()>0){ //won't be null but will be empty
				BlockInfo bi = mSum.getBlockInfo().get(0);
				if (bi.getVersion().equalsIgnoreCase("original")){ //removed
					mi.setOrigReturnType(mSum.getOrigMethodReturnType());
					if (mSum.getOrigMethodParameters().size()>0){
						for (int i=0; i<mSum.getOrigMethodParameters().size();i++){
							mi.addOrigParameter(mSum.getOrigMethodParameters().get(i));
							mi.addOrigParamType(mSum.getOrigMethodParamTypes().get(i));
						}
					}
				}else{ //added
					mi.setReturnType(mSum.getModMethodReturnType());
					if (mSum.getModMethodParameters().size()>0){
						for (int i=0; i<mSum.getModMethodParameters().size();i++){
							mi.addParameter(mSum.getModMethodParameters().get(i));
							mi.addParamType(mSum.getModMethodParamTypes().get(i));
						}
					}
				}
			}
		}
		
		//indexName = className + "." + mSum.getMethodName();
		Map<BigInteger,BlockInfo> orig = new HashMap<BigInteger,BlockInfo>();
		Map<BigInteger,BlockInfo> mod = new HashMap<BigInteger,BlockInfo>();
		//split the blocks into original and modified lists
		List<BlockInfo> blocks = mSum.getBlockInfo();
		for (BlockInfo bi : blocks){
			if (bi.getVersion().equalsIgnoreCase("original"))
				orig.put(bi.getBlockId(),bi);
			else
				mod.put(bi.getBlockId(),bi);
		}
		//process each block in the original version and its
		//corresponding block in the modified
		Iterator it = orig.entrySet().iterator();
	    while (it.hasNext()){
	    	Map.Entry me = (Map.Entry)it.next();
	    	BigInteger blockID = (BigInteger)me.getKey();
	    	BlockInfo bi = (BlockInfo)me.getValue();
	    	BlockASTInfo astB = new BlockASTInfo();
	    	int start = bi.getStart().intValue();
	    	int end = bi.getEnd().intValue();
	    	BigInteger parent = bi.getParentBlock();
	    	astB.setStartLine(start);
	    	astB.setEndLine(end);
	    	astB.setParent(parent);
	    	List<String> asserts = bi.getAssertStmts();
	    	if (asserts.size()>0){
	    		Iterator<String> itA = asserts.iterator();
	    		while (itA.hasNext()){
	    			mi.addAssertLineOrig(new Integer(itA.next()));
	    		}
	    	}
	    	String changeType = bi.getChangeType();
	    	if (changeType.equalsIgnoreCase("removed")){
	    		astB.setChangeType(bi.getChangeType());
	    		for (int i=start; i<=end; i++)
	    			mi.addRemovedLine(i);
	    	}else if (changeType.equalsIgnoreCase("unchanged")){
	    		astB.setChangeType(bi.getChangeType());
	    	}else{
	    		astB.setChangeType("changed");
	    		for (int i=start; i<=end; i++)
	    			mi.addChangedOrig(i);
	    	}
	    	astB.setMatchedBlock(bi.getMatchingBlock());
	    	List<String> atts = bi.getAttributes();
	    	if (atts.contains("asserts")){
	    		String numbers = "";
	    		List<String> lineNumbers = bi.getAssertStmts();
	    		Iterator<String> itL = lineNumbers.iterator();
	    		while (itL.hasNext()){
	    			numbers = itL.next();
	    			if (itL.hasNext())
	    				numbers = numbers + ",";
	    		}
	    		if (numbers.length()>1){
	    			String[] numb = numbers.split(",");
	    			for (int i=0; i< numb.length;i++){
		    			astB.getAssertStatement().add(new Integer(numb[i]));
	    			}
	    		}
	    	}
	    	mi.addOriginalBlock(blockID, astB);
		}
		//now, process blocks in the modified version
		Iterator itM = mod.entrySet().iterator();
		while (itM.hasNext()){
		   	Map.Entry me = (Map.Entry)itM.next();
		    BigInteger blockID = (BigInteger)me.getKey();
		    BlockInfo bi = (BlockInfo)me.getValue();
		    BlockASTInfo astB = new BlockASTInfo();
		    int start = bi.getStart().intValue();
	    	int end = bi.getEnd().intValue();
	    	astB.setStartLine(bi.getStart().intValue());
	    	astB.setEndLine(bi.getEnd().intValue());
	    	String changeType = bi.getChangeType();
	    	if (changeType.equalsIgnoreCase("added")){
	    		astB.setChangeType(bi.getChangeType());
	    		for (int i=start; i<=end; i++)
	    			mi.addAddedLine(i);
	    	}else if (changeType.equalsIgnoreCase("unchanged")){
	    		astB.setChangeType(bi.getChangeType());
	    	}else{
	    		astB.setChangeType("changed");
	    		for (int i=start; i<=end; i++)
	    			mi.addChangedMod(i);
	    	}
	    	astB.setMatchedBlock(bi.getMatchingBlock());
	    	//check assertions
	    	List<String> atts = bi.getAttributes();
	    	if (atts.contains("asserts")){
	    		String numbers = "";
	    		List<String> lineNumbers = bi.getAssertStmts();
	    		Iterator<String> itL = lineNumbers.iterator();
	    		while (itL.hasNext()){
	    			numbers = itL.next();
	    			if (itL.hasNext())
	    				numbers = numbers + ",";
	    		}
	    		if (numbers.length()>1){
	    			String[] numb = numbers.split(",");
	    			for (int i=0; i< numb.length;i++){
	    				mi.addAssertLineMod(Integer.valueOf(numb[i]).intValue());
		    			astB.getAssertStatement().add(new Integer(numb[i]));
	    			}
	    		}
	    	}
	    	mi.addModifiedBlock(blockID, astB);
		}
    	if (!mi.getMatched()){
    		if (mi.getNumberChangedLinesMod() > 0)
    			mi.setAdded(true);
    		else if (mi.getNumberChangedLinesOrig() > 0)
    			mi.setRemoved(true);
    	}
		String indexName = className + "." + mi.getUniqueMethodIndex();
		//System.out.println(mi.toString());
		if (direction.equalsIgnoreCase("reverse")){
			mi.reverse();
			//System.out.println(mi.toString());
		}
		mi.setFileName(fName);
		
		// remove generics info and other stuff from indexName
		indexName = cleanMethodSignatureIndex(indexName);
		
		methodASTInfo.put(indexName,mi);
	}
	
	/*
	 * cleanMethodSignatureIndex
	 * 
	 * given a String that represents the method signature index for a
	 * MethodASTInfo mapping, this method will use a regex to remove the
	 * generics information (e.g. <...>) and the inner class notation symbol
	 * ($). The resulting cleaned string will be returned.
	 */
	public static String cleanMethodSignatureIndex(String indexName) {
		
		// first, remove the generic information
		// we need to ignore the init method, so check and return if it is that
		// the extra replace lines are because
		// 1) <init> will not appear at the beginning of a method sign. string
		// 2) <init> statements may contain parameters with generics info
		if(indexName.equals("<init>") || indexName.contains("<init>")) {
			indexName = indexName.replace("<init>", "@@@@@@");
			indexName = indexName.replaceAll("<[\\w\\.]+>", "");
			indexName = indexName.replaceAll("\\$", ".");
			indexName = indexName.replace("@@@@@@", "<init>");
			return indexName;
		}
		
		// otherwise, try to remove anything wrapped in <...>
		indexName = indexName.replaceAll("<[\\w\\.]+>", "");
		
		// second, we should remove any $ from inner classes
		indexName = indexName.replaceAll("\\$", ".");
		
		return indexName;
	}

	//recursive method to process each class in the ClassSummary list found
	//in the BlockSummary and ClassSummary objects
	//process all methods in the class first, then process the class list
	private void processClass(ClassSummary cSum, String baseName){
		String longClassName = cSum.getClassName(); //includes package name
//		String className = longClassName.substring(longClassName.lastIndexOf(".")+1);
		String className = longClassName;
		//ignore globalVarNotEqual and initializationBlockSummary for now
		if (baseName.length() > 0)
			className = baseName + "." + className;
		List<MethodSummary> mSum = cSum.getMethodSummary();
		for (MethodSummary m : mSum){
			processMethod(m,className);
		}
		List<ClassSummary> subClasses = cSum.getClassSummary();
		for (ClassSummary c : subClasses){
			processClass(c, className);
		}
	}

	public Map<String,MethodASTInfo> loadAST(String ASTFile){
		//extract information from XML files
		BlockSummaryReader bReader = new BlockSummaryReader();
		BlockSummary bSum = bReader.extractBlock(ASTFile);
		modFileName = bSum.getModifiedFile();
		origFileName = bSum.getOriginalFile();
		List<ClassSummary> classes = bSum.getClassSummary();
		for (ClassSummary c : classes){
			processClass(c,"");
		}
		return methodASTInfo;
	}

	 static private List<File> getSortedFileListing(File aStartingDir)
		throws FileNotFoundException {
		 validateDirectory(aStartingDir);
		 List<File> result = new ArrayList<File>();
		 File[] filesAndDirs = aStartingDir.listFiles();
		 Arrays.sort(filesAndDirs);
		 List<File> filesDirs = Arrays.asList(filesAndDirs);
		 for(File file : filesDirs) {
			 result.add(file); //always add, even if directory
			 if ( ! file.isFile() ) {
				 //must be a directory
				 //recursive call!
				 List<File> deeperList = getSortedFileListing(file);
				 result.addAll(deeperList);
			 }
		 }
		 return result;
	 }

	 static private void validateDirectory (File aDirectory) throws FileNotFoundException {
		 if (aDirectory == null) {
			 throw new IllegalArgumentException("Directory should not be null.");
		 }
		 if (!aDirectory.exists()) {
			 throw new FileNotFoundException("Directory does not exist: " + aDirectory);
		 }
		 if (!aDirectory.isDirectory()) {
			 throw new IllegalArgumentException("Is not a directory: " + aDirectory);
		 }
		 if (!aDirectory.canRead()) {
			 throw new IllegalArgumentException("Directory cannot be read: " + aDirectory);
		 }
	 }


	/*
	 * Loads the list of files/directories containing the AST diff results
	 */
	public Map<String,MethodASTInfo> loadAST(String[] astInfo, String direction){
		this.direction = direction;
		try{
			//extract information from XML files
			BlockSummaryReader bReader = new BlockSummaryReader();
			for (int i=0; i< astInfo.length; i++){
				String name = astInfo[i];
				File file = new File(name);
				if (file.isDirectory()){
					List<File> astFiles = getSortedFileListing(file);
		    		Iterator<File> itO= astFiles.iterator();
		    		while (itO.hasNext()){
		    			File f = (File)itO.next();
		    			
		    			// check that we are actually looking at an XML file,
		    			// otherwise just continue on to the next file
		    			if(!f.getName().endsWith(".xml")) continue;
		    			
		    			BlockSummary bSum = bReader.extractBlock(f.getAbsolutePath());
		    			fName = bSum.getModifiedFile();
		    			List<ClassSummary> classes = bSum.getClassSummary();
		    			for (ClassSummary c : classes){
		    				processClass(c,"");
		    			}
		    		}
				}else{
					BlockSummary bSum = bReader.extractBlock(file.getAbsolutePath());
					fName = bSum.getModifiedFile();
	    			List<ClassSummary> classes = bSum.getClassSummary();
	    			for (ClassSummary c : classes){
	    				processClass(c,"");
	    			}
				}
			}
			return methodASTInfo;
		}catch(Exception e){
			e.printStackTrace();
			return null;
		}
	}
}
