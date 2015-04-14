package jpf_diff;

import gov.nasa.jpf.regression.analysis.ComputeDifferences;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.Set;

import javax.swing.text.html.HTMLDocument.Iterator;

//import lazyinit.paramAndPoly.intNode;

import com.sun.tools.doclets.internal.toolkit.util.DocFinder.Output;


class Path {
	static String[] prop = {"./script/patch/precise/Example01.jpf",
							"./script/new/precise/Example01.jpf"};
	static String[] dot = {"./dotFiles/patch/jdt/Compiler/Compiler_16_compile.dot",
							"./dotFiles/new/jdt/Compiler/Compiler_16_compile.dot"};
	static String[] classpath = {"/Users/barry/code/patch/archive-2.9.0.xx-20140320-0100-e43-SNAPSHOT/plugins/org.eclipse.jdt.core.source_3.9.2.xx-20140320-0100-e43-SNAPSHOT/org/eclipse/jdt/internal/compiler/Compiler.java",
		"/Users/barry/code/patch/org.eclipse.jdt.source-4.3.2/plugins/org.eclipse.jdt.core.source_3.9.2.v20140114-1555/org/eclipse/jdt/internal/compiler/Compiler.java"};
}
class DotNode {
	Integer begin;
	Integer end;
	Integer id;
	public boolean changed;
	
	public DotNode(Integer begin, Integer end, Integer id) {
		// TODO Auto-generated constructor stub
		this.begin = begin;
		this.end = end;
		this.id = id;
		changed = false;
	}
}
class DotEdge {
	Integer begin;
	Integer end;
	public DotEdge(Integer begin, Integer end) {
		// TODO Auto-generated constructor stub
		this.begin = begin;
		this.end = end;
	}
}

class WriteNode extends DotNode {
	public WriteNode(Integer begin, Integer end, Integer id) {
		// TODO Auto-generated constructor stub
		super(begin, end, id);
	}
}

class CondNode extends DotNode {
	public CondNode(Integer begin, Integer end, Integer id) {
		// TODO Auto-generated constructor stub
		super(begin, end, id);
	}
}

class DataEdge extends DotEdge {
	public DataEdge(Integer begin, Integer end) {
		// TODO Auto-generated constructor stub
		super(begin, end);
	}
}

class ControlEdge extends DotEdge {
	public ControlEdge(Integer begin, Integer end) {
		// TODO Auto-generated constructor stub
		super(begin, end);
	}
}

class impactSet {
	String path;
	
	Set<Integer> condLoc;
	Set<Integer> writeLoc;
	Set<Integer> assertLoc;
	Set<Integer> changeLoc;
	Set<Integer> allLoc;

	Set<DotNode> nodes;
	Set<DotEdge> edges;
	
	Map<Integer, Set<Integer> > lineToNodeId;
	Map<Integer, DotNode> idToNode;
	Map<Integer, Set<DotEdge>> relation; //node id -> edges
	
	public impactSet(String path) {
		// TODO Auto-generated constructor stub
		condLoc = new HashSet<Integer>();
		writeLoc = new HashSet<Integer>();
		changeLoc = new HashSet<Integer>();
		allLoc = new HashSet<Integer>();;
		assertLoc = new HashSet<Integer>();
		this.path = path;
		
		nodes = new HashSet<>();
		edges = new HashSet<>();
		lineToNodeId = new HashMap<Integer, Set<Integer>>();
		idToNode = new HashMap<>();
		relation = new HashMap<>();
	}
	//清除当前存储的impact sets的信息
	void clear() {
		condLoc.clear();
		writeLoc.clear();
		changeLoc.clear();
		assertLoc.clear();
		allLoc.clear();
		
		nodes.clear();
		edges.clear();
		
		lineToNodeId.clear();
		idToNode.clear();
	}
}

public class Diff {
	
	Set<String> filenames = new HashSet<String>();
	
	
	void analyzeDot(String path, impactSet now) throws IOException {
		File diff = new File(path);
		BufferedReader reader = new BufferedReader(new FileReader(diff));
		String line;
		String file = "";
		while((line = reader.readLine()) != null) {
			file += line + "\n";
		}
		reader.close();
		String[] text = file.split("\n");
		for (String string : text) {
			//分析Node，提取出impacted nodes
			if (string.contains("color") && !string.contains("Depends")) {
				
				//获取Node对应的block的起止范围
				String label = string.substring(string.indexOf(")") + 1, string.lastIndexOf("\""));
				String[] nums = label.split("-");
				Integer begin = new Integer(nums[0]);
				Integer end = new Integer(nums[1]);
				
				//获取Node ID
				String[] contents = string.split("\\[");
				Integer id = new Integer(contents[0]);
				
				DotNode nowNode;
				
				if (string.contains("red")) {
					for (int i = begin; i <= end; i++) {
						now.condLoc.add(i);
						now.allLoc.add(i);
						//记录从line number -> node id's的映射
						if (!now.lineToNodeId.containsKey(i)) {
							Set<Integer> ids = new HashSet<>();
							ids.add(id);
							now.lineToNodeId.put(i, ids);
						}
						else {
							now.lineToNodeId.get(i).add(id);
						}
					}
					nowNode = new CondNode(begin, end, id);
					
				}
				else if (string.contains("purple")){
					for (int i = begin; i <= end; i++) {
						now.assertLoc.add(i);
						now.allLoc.add(i);
						if (!now.lineToNodeId.containsKey(i)) {
							Set<Integer> ids = new HashSet<>();
							ids.add(id);
							now.lineToNodeId.put(i, ids);
						}
						else {
							now.lineToNodeId.get(i).add(id);
						}
					}	
					nowNode = new DotNode(begin, end, id);
				}
				else if (string.contains("blue")){
					for (int i = begin; i <= end; i++) {
						now.writeLoc.add(i);
						now.allLoc.add(i);
						if (!now.lineToNodeId.containsKey(i)) {
							Set<Integer> ids = new HashSet<>();
							ids.add(id);
							now.lineToNodeId.put(i, ids);
						}
						else {
							now.lineToNodeId.get(i).add(id);
						}
					}
					nowNode = new WriteNode(begin, end, id);
				}
				else {
					nowNode = new DotNode(begin, end, id);
				}
				//设置changed属性
				if (string.contains("**")) {
					for (int i = begin; i <= end; i++) {
						now.changeLoc.add(i);
//						now.allLoc.add(i);
//						System.out.println(i);
						nowNode.changed = true;
					}
				}
				//记录从node id -> node的映射
				now.idToNode.put(id, nowNode);
				now.nodes.add(nowNode);
			}
			//分析Edge，提取出节点间的依赖关系
			else if (string.contains("Depends")) {
				String[] contents = string.split("\\[|->");
				Integer begin = new Integer(contents[0]);
				Integer end = new Integer(contents[1]);
				DotEdge nowEdge;
				if (string.contains("Data")) {
					nowEdge = new DataEdge(begin, end);
				}
				else {
					nowEdge = new ControlEdge(begin, end);
				}
				now.edges.add(nowEdge);
				//添加从该node出发的边关系
				if (!now.relation.containsKey(begin)) {
					Set<DotEdge> edges = new HashSet<>();
					edges.add(nowEdge);
					now.relation.put(begin, edges);
				}
				else {
					now.relation.get(begin).add(nowEdge);
				}
				
			}
		}
	}
	
	
	public void readFileName(String path) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(new File(path)));
		String line = "";
		while((line = reader.readLine()) != null) {
			filenames.add(line);
		}
		reader.close();
	}
	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub

		Diff diff = new Diff();
		String newPath = "./dotFiles/new/jdt/";
		String patchPath = "./dotFiles/patch/jdt/";
		diff.readFileName("./script/filename.uniq");
		
		for (String filename : diff.filenames) {
			String p = newPath + filename;
			String p1 = patchPath + filename;
			
			diff.deleteDot(p);
			diff.deleteDot(p1);

		}
		for (String filename : diff.filenames) {
			String path = newPath + filename;
			String path1 = patchPath + filename;
			impactSet s = new impactSet(path);
			impactSet s1 = new impactSet(path1);
			
			diff.computeImpact(s, s1);

		}
	}
	private void deleteDot(String p) {
		// TODO Auto-generated method stub
		File f = new File(p);
		File[] Files = f.listFiles();
		
		for (File Dot : Files) {
			if (Dot.exists() && Dot.getName().endsWith("txt")) {
				Dot.delete();
			}
		}
	}
	private void computeImpact(impactSet s, impactSet s1) throws IOException {
		// TODO Auto-generated method stub
		File newf = new File(s.path);
		File[] newFiles = newf.listFiles();
		
		for (File newDot : newFiles) {
			File patchf = new File(s1.path);
			File[] patchFiles = patchf.listFiles();
			for (File patchDot : patchFiles) {
				if (newDot.getName().equals(patchDot.getName())) {
					String newPath = newDot.getAbsolutePath();
					String patchPath = patchDot.getAbsolutePath();
					
					//清除上个文件的内容
					s.clear();
					s1.clear();
					
					analyzeDot(newPath, s);
//					System.out.println("patch complete!");
					analyzeDot(patchPath, s1);
//					System.out.println("new complete!");
					
					s.allLoc.retainAll(s1.allLoc);
					//若没有交集，忽略之
					if (s.allLoc.isEmpty()) {
						continue;
					}
//					BufferedWriter writer = new BufferedWriter(new FileWriter(newDot.getAbsolutePath() + ".txt"));
					System.out.println(newDot.getAbsolutePath() + " begin!");
					output(s.allLoc, s, newDot.getAbsolutePath() + "_impacted.dot");
					System.out.println(newDot.getAbsolutePath() + " complete!");
					output(s.allLoc, s1,  patchDot.getAbsolutePath() + "_impacted.dot");
					System.out.println(patchDot.getAbsolutePath() + " begin!");
					System.out.println(patchDot.getAbsolutePath() + "complete!");
//					writer.close();
					break;
				}
			}
		}		
	}


	private void output(Set<Integer> intersection, impactSet now, String path) throws IOException {
		// TODO Auto-generated method stub
		BufferedWriter writer = new BufferedWriter(new FileWriter(path));
		writer.write("digraph \"\" { \n");
		Set<Integer> impacted = new HashSet<>();
		for (Integer i : intersection) {
			Set<Integer> ids = now.lineToNodeId.get(i);
			for (Integer id : ids) {
				DotNode node = now.idToNode.get(id);
				String lineNumbers = node.begin + "-" + node.end;
				if (node.changed) {
					if (node instanceof WriteNode) {
						writer.write(id +"[ label=\"(" + id + "**)" +
								lineNumbers + "\",color=blue,style=filled];\n");		
					}
					if (node instanceof CondNode) {
						writer.write(id +"[ label=\"(" + id + "**)" +
								lineNumbers + "\",color=red,style=filled];\n");						
					}
				}
				else {
					if (node instanceof WriteNode) {
						writer.write(id +"[ label=\"(" + id + ")" +
								lineNumbers + "\",color=blue,style=filled];\n");		
					}
					if (node instanceof CondNode) {
						writer.write(id +"[ label=\"(" + id + ")" +
								lineNumbers + "\",color=red,style=filled];\n");						
					}				
				}
				impacted.add(id);	
			}
		}
		writeEdge(impacted, now, writer);
		writer.write("}");
		writer.close();
	}


	private void writeEdge(Set<Integer> impacted, impactSet now,
			BufferedWriter writer) throws IOException {
		// TODO Auto-generated method stub
		if (impacted.isEmpty()) {
			return;
		}
		Set<Integer> next = new HashSet<>();
		for (Integer id : impacted) {
			Set<DotEdge> edges = now.relation.get(id);
			//若当前节点没有下一层了，则继续
			if (edges == null) {
				continue;
			}
			for (DotEdge edge : edges) {
				if (id.intValue() != edge.end.intValue() && now.idToNode.containsKey(edge.end) && !now.idToNode.get(edge.end).changed
						&& !now.idToNode.get(id).changed) {
					next.add(edge.end);
				}
				if (edge instanceof DataEdge) {
					writer.write(id + "->" +
							edge.end +
						"[ color=\"red\" label=\"" + "Data Depends on" +	"\" style = dotted ];\n");
				}
				if (edge instanceof ControlEdge) {
					writer.write(id + "->" +
							edge.end +
						"[ color=\"blue\" label=\"" + "Control Depends on" +	"\" style = dotted ];\n");
				}
				System.out.println("	" + id + "," + edge.end);
			}
			writeEdge(next, now, writer);
		}
	}


	private void printChange(String classpath, impactSet s) throws IOException {
		// TODO Auto-generated method stub
		System.out.println("-----print begin!------");
		File classfile = new File(classpath);
		BufferedReader reader = new BufferedReader(new FileReader(classfile));
		String line;
		String file = "";
		while((line = reader.readLine()) != null) {
			file += line + "\n";
		}
		reader.close();
		String[] text = file.split("\n");
		for (int i = 0; i < text.length; i++) {
			if (s.allLoc.contains(i + 1)) {
				System.out.println((i + 1) + " :" + text[i]);
			}
		}
	}

}
