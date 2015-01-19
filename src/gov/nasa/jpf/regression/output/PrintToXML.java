package gov.nasa.jpf.regression.output;

import gov.nasa.jpf.regression.analysis.ComputeInterProceduralDiff;
import gov.nasa.jpf.regression.ast.MethodASTInfo;
import gov.nasa.jpf.regression.data.GlobalOperation;
import gov.nasa.jpf.regression.listener.LocationData;
import gov.nasa.jpf.regression.listener.VisualizationData;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sun.tools.javac.util.Pair;

public class PrintToXML {
	HashMap<String, Integer> dNodes;
	HashMap<Integer, GlobalOperation> operations;
	HashMap<Integer, ArrayList<Integer>> dEdges;
	String fileName; 
	
	ArrayList<LocationData>  locs;
	HashMap<String, Boolean> branches;
	Map<String,Set<Integer>> trackCondLocs = new HashMap<String,Set<Integer>>();
	Map<String,Set<Integer>> trackWriteLocs = new HashMap<String,Set<Integer>>();
	Map<String,Set<Integer>> changedLocs = new HashMap<String,Set<Integer>>();
	Map<String,Set<Integer>> addedLocs = new HashMap<String,Set<Integer>>();

	public void printOutput(HashMap<String, Integer> dNodes,
			HashMap<Integer, GlobalOperation> operations,
			HashMap<Integer, ArrayList<Integer>> dEdges, 
			String fileName) {
		this.dNodes = dNodes;
		this.operations = operations;
		this.dEdges = dEdges;
		this.fileName = fileName;
		writeToFile();
	}
	
	public void writeToFile() {
		Writer output = null;
		File file = new File(fileName);
		try {
			output = new BufferedWriter(new FileWriter(file));
		} catch (IOException e) {
			System.err.println("error while creating the file to write");
			e.printStackTrace();
		}
		try {
			printNodes(output);
			printEdges(output);
		} catch (IOException e) {
			System.err.println("Error while writing to the XML file");
			e.printStackTrace();
		}
		
		try {
			output.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
	public void writeToFile(String fName, String data) {
		Writer output = null;
		File file = new File(fName);
		try {
			output = new BufferedWriter(new FileWriter(file));
		} catch (IOException e) {
			System.err.println("error while creating the file to write");
			e.printStackTrace();
		}
		try {
			output.write(data);
		} catch (IOException e) {
			System.err.println("Error while writing to the XML file");
			e.printStackTrace();
		}
		
		try {
			output.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
	public void printNodes(Writer output) throws IOException {
		Iterator<Integer> nodeItr = operations.keySet().iterator();
		while(nodeItr.hasNext()) {
			Integer id = nodeItr.next();
			GlobalOperation gOp = operations.get(id);
			output.write("<node>\n");
			output.write("\t<id>\n");
			output.write("\t\t"+id+"\n");
			output.write("\t</id>\n");
			output.write("\t<className>\n");
			output.write("\t\t" + gOp.getClassName()+"\n");
			output.write("\t</className>\n");
			output.write("\t<methodName>\n");
			output.write("\t\t"+ gOp.getMethodName()+"\n");
			output.write("\t</methodName>\n");
			output.write("\t<position>\n");
			output.write("\t\t"+gOp.getPosition()+"\n");
			output.write("\t</position>\n");
			output.write("</node>\n");
		}
	}
	
	public void printEdges(Writer output) throws IOException  {
		Iterator<Integer> edgeItr = dEdges.keySet().iterator();
		while(edgeItr.hasNext()) {
			Integer head = edgeItr.next();
			ArrayList<Integer> child = dEdges.get(head);
			for(int childIdx = 0; childIdx < child.size(); childIdx++) {
				output.write("<edge>\n");
				output.write("\t<head>\n");
				output.write("\t\t"+head+"\n");
				output.write("\t</head>\n");
				output.write("\t<tail>\n");
				output.write("\t\t" + child.get(childIdx) +"\n");
				output.write("\t</tail>\n");
				output.write("</edge>\n");	
			}
		}
	}
	
	//TODO: prints results of interprocedural analysis
	public void printXMLFile(String dir, String target, 
			Map<String, MethodASTInfo> astInfo,
			Map<String,ComputeInterProceduralDiff> differences){
		File f = new File(dir);
		if (f.isDirectory()){
			target = target.substring(target.lastIndexOf(".")+1);
			fileName = dir + File.separator + target + ".dot";
		}else
			fileName = dir;
	}
	
	public void initialize(){
		locs = VisualizationData.getExecutedInstructions();
		branches = VisualizationData.getBranchConditions();
		trackCondLocs = VisualizationData.getTrackCondLocMap();
		trackWriteLocs = VisualizationData.getTrackWriteLocMap();
		changedLocs = VisualizationData.getChangedLocsMap();
		addedLocs = VisualizationData.getAddedLocsMap();
	}
	
	//replaces "<" and ">" with the appropriate escape seq 
	private String removeBadChars(String s){
		String clean = s;
		if (clean.contains(">"))
			clean = clean.replaceAll(">","&gt;");
		if (clean.contains("<"))
			clean = clean.replaceAll("<","&lt;");
		if (clean.contains("\""))
			clean = clean.replaceAll("\"", "&quot;");
		return clean;
	}
	
	//prints results of intraprocedural analysis
	public void printXMLFile(String dir, String srcPath, String bcPath, 
			String target, Map<String,String> nameMap){
		
		Set<Integer> byteCodes = new HashSet<Integer>();
		String fullMethodName = nameMap.get(target);
		String shortMethodName = fullMethodName.substring(0,fullMethodName.indexOf("("));
		System.out.println(shortMethodName);
		Set<Integer> impactedWrites = trackWriteLocs.get(fullMethodName);
		if (impactedWrites == null)
			impactedWrites = new HashSet<Integer>();
		Set<Integer> impactedConds = trackCondLocs.get(fullMethodName);
		if (impactedConds == null)
			impactedConds = new HashSet<Integer>();
		Set<Integer> changedLines = changedLocs.get(shortMethodName);
		if (changedLines == null)
			changedLines = new HashSet<Integer>();
		Set<Integer> addedLines = addedLocs.get(shortMethodName);
		if (addedLines == null)
			addedLines = new HashSet<Integer>();
		
		//maps bytecode loc to constraints added at that loc
				Map<Integer,List<String>> cListMap = new HashMap<Integer,List<String>>();
				//maps bytecode loc to xmlNode
				ArrayList<Pair<Integer, String>> nodeMap = new ArrayList<Pair<Integer, String>>();
				
				System.out.println("PrintToXML: ImpactedWrites: " + impactedWrites.toString());
				System.out.println("PrintToXML: ImpactedConds: " + impactedConds);
				System.out.println("PrintToXML: ChangedLocs: " + changedLines);
				System.out.println("PrintToXML: AddedLocs: " + addedLines);
				
				int nodeNumber = 0;
				
				File f = new File(dir);
				if (f.isDirectory()){
					fileName = dir + File.separator + target + ".dot";
				}else{
					dir = dir.substring(0,dir.lastIndexOf("."));
					fileName = dir + ".xml";
				}
				
				StringBuffer sb = new StringBuffer();
				sb.append("<?xml version=\"1.0\" encoding=\"utf-8\" ?>\n");
				sb.append("<graph id=\"ImpactAnalysis\" index=\"0\" tracetype=\"abstract\">\n");
				sb.append("\t<jpfstate id=\"none\">\n");
				
				for (int i=0; i<locs.size(); i++){
					LocationData pl = locs.get(i);
					String mName = pl.getMethodName();
					//System.out.println(pl.toString());
					if (mName.contains(target)){
			
							String tmpSrcLine = pl.getSrcLine();
							if (tmpSrcLine.contains("//"))
								tmpSrcLine = tmpSrcLine.substring(0,tmpSrcLine.indexOf("//")).trim();
							if (tmpSrcLine.contains(";"))
								tmpSrcLine = tmpSrcLine.substring(0,tmpSrcLine.lastIndexOf(";"));
							tmpSrcLine = removeBadChars(tmpSrcLine);
							String fLoc = pl.getFileLoc();
							if (fLoc.contains("/"))
								fLoc = fLoc.substring(fLoc.lastIndexOf("/")+1);
							fLoc = fLoc.substring(0,fLoc.indexOf(":"));
							String bcFloc = fLoc.substring(0,fLoc.indexOf(".")) + ".class";
							String impacted="notImpacted";
							if (trackWriteLocs != null && impactedWrites.contains(pl.getLocation()))
									impacted="impactedWrite";
							else if (trackCondLocs != null && impactedConds.contains(pl.getLocation()))
									impacted="impactedCond";
							String changed = "false";
							if (changedLines.contains(pl.getLineNum()))
								changed = "true";
							String xmlNode = "\t\t<node ";
							xmlNode = xmlNode + "id=\"node_" +  nodeNumber + "\" " +
							"class=\"" + pl.getClassName() + "\" " +
							"method=\"" + target + "\" " +
							"bytecode=\"" + pl.getBCode() + "\" " +
							"bytecodeline=\"" + pl.getLocation() + "\" " +
							"sourceline=\"" + pl.getLineNum() + "\" " +
							"tracetype=\"" + "concreteonly" + "\" " + //TODO make this changeable
							"sourcecode=\"" + tmpSrcLine + "\" " +
							"sourcecodefile=\"" + fLoc + "\" " +
							"bytecodefile=\"" + bcFloc + "\" " +
							"executed=\"" + "true" + "\" " + //false when tracetype is abstract?
							"threadnumber=\"" + 0 + "\" " +
							"type=\"" + "regularstmt" + "\" " +
							"impacted=\"" + impacted + "\" " +
							"modified=\"" + changed + "\" " +
							"srcpath=\"" + "path_0" + "\" " +
							"bytecodepath=\"" + "path_1" + "\" ";
							Pair<Integer, String> aPair = new Pair<Integer, String>(nodeNumber, xmlNode);
							nodeMap.add(aPair);
							String constraint = pl.getConstraint().trim();
							constraint = removeBadChars(constraint);
							if (constraint.length()>0){
								List<String> cList = new ArrayList<String>();
								cList.add(constraint);
								cListMap.put(nodeNumber, cList);
							}
							nodeNumber++;
						}
					}
				
				System.out.println(cListMap.toString());
				//System.exit(1);
				//now stich it all together
		        int cNum = 1; //used for the contained field in the pc node (not correct)
				Iterator<Pair<Integer, String>> it = nodeMap.iterator();
			    while (it.hasNext()) {
			        Pair<Integer, String> pair = it.next();
			        int num = pair.fst.intValue();
			        String node = pair.snd;
			        if (cListMap.containsKey(num)){
			        	node = node + ">\n" + "\t\t\t<PCs>\n";
			        	List<String> constraints = cListMap.get(num);
			        	Iterator<String> itL = constraints.iterator();
			        	while (itL.hasNext()){
			        		String c = itL.next();
			        		node = node + "\t\t\t\t<pc value=\"" + c + "\" contained=\"" +
			        			cNum + "\" />\n";
			        	}
			        	node = node + "\t\t\t</PCs>\n" + "\t\t</node>\n";
			        	cNum++;
			        }else{ //no constraints to add
			        	node = node + "/>\n";
			        }
					sb.append(node);
			    }		
				
				sb.append("\t</jpfstate>");
				String paths = "<paths>\n" +
				"\t\t<path id=\"path_0\" path=\"" + srcPath + "\" />\n" +
				"\t\t<path id=\"path_1\" path=\"" + bcPath + "\" />\n" +
				"\t</paths>\n";
				sb.append(paths);
				sb.append("</graph>\n");
				
				writeToFile(fileName,sb.toString());
			}
		}