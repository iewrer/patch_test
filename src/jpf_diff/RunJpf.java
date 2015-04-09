package jpf_diff;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.regression.ast.MethodASTInfo;
import gov.nasa.jpf.regression.listener.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.lang.ProcessBuilder.Redirect;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import lazyinit.paramAndPoly.intNode;
import tree_diff.test_v1;

//import gov.nasa.jpf.JPF;
//import gov.nasa.jpf.Config;
//import gov.nasa.jpf.JPF.ExitException;
//import gov.nasa.jpf.regression.listener.PruningRSEListener;
//import gov.nasa.jpf.tool.RunJPF;


public class RunJpf {
	
	
	public RunJpf(String file) {
		// TODO Auto-generated constructor stub
		this.file = file;
		old_to_new = "./script/" + file + "/" + file + "_new.jpf";
		new_to_patch = "./script/" + file + "/" + file + "_patch.jpf";
	}
	public void change(String file) {
		this.file = file;
		old_to_new = "./script/" + file + "/" + file + "_new.jpf";
		new_to_patch = "./script/" + file + "/" + file + "_patch.jpf";		
	}
	String file = "";
	String old_to_new = "";
	String new_to_patch = "";
	
	Map<Integer, String> lineToSrc_OTN = new HashMap<Integer, String>(); //old to new
	Map<Integer, String> lineToSrc_NTP = new HashMap<Integer, String>(); //new to patch
	
	Map<String, Integer> srcToLine_OTN = new HashMap<String, Integer>(); //old to new
	Map<String, Integer> srcToLine_NTP = new HashMap<String, Integer>(); //new to patch
	
	/***
	 * 目前选择的是对changedLinesMod进行修改
	 * 因为目前是：
	 * 	new->old
	 *  new->patch 
	 * 所以以new为中介，检查两个ASTDiff中对应的method中的修改是否有完全相同的
	 * 若有，则删去之
	 * 
	 * 目前考虑changedLinesMod和changedLinesMod，选择了Mod进行操作
	 * 
	 * 目前还有问题：
	 * 	没有效果：
	 *		可能因为changedLinesMod在此时还没有添加进来？
	 *		其他？
	 * @throws IOException 
	 *	 
	 */
	void deleteSameChange(PruningRSEListener listener_otn, PruningRSEListener listener_ntp,String type) throws IOException {	
		//读入并记录源文件
		BufferedReader reader = new BufferedReader(new FileReader(new File(listener_otn.newSrc)));
		String line = "";
		int i = 1;
		while((line = reader.readLine()) != null) {
			lineToSrc_OTN.put(i, line);
			srcToLine_OTN.put(line, i);
		}
		reader.close();
		reader = new BufferedReader(new FileReader(new File(listener_ntp.newSrc)));
		line = "";
		i = 1;
		while((line = reader.readLine()) != null) {
			lineToSrc_NTP.put(i, line);
			srcToLine_NTP.put(line, i);
		}		
		reader.close();
		
		java.util.Iterator<Entry<String, MethodASTInfo>> entries = listener_otn.methodASTInfo.entrySet().iterator();
		//遍历每个方法
		while (entries.hasNext()) {
			Map.Entry entry = (Map.Entry) entries.next();
			String name = (String) entry.getKey();
			MethodASTInfo info_otn = (MethodASTInfo) entry.getValue();
			MethodASTInfo info_ntp = new MethodASTInfo();
			//如果能找到对应的方法
			if (listener_ntp.methodASTInfo.containsKey(name)) {
				info_ntp = listener_ntp.methodASTInfo.get(name);
			}
			else {
				continue;
			}
			Set<String> otn = new HashSet<String>();
			Set<String> ntp = new HashSet<String>();
			
			Set<Integer> inter_otn = new HashSet<Integer>();
			Set<Integer> inter_ntp = new HashSet<Integer>();
			
			Set<Integer> changed_otn = new HashSet<Integer>();
			Set<Integer> changed_ntp = new HashSet<Integer>();
			
			if (type.equals("changedMod")) {
				changed_otn = info_otn.getChangedLinesMod();
				changed_ntp = info_ntp.getChangedLinesMod();
			}
			else if (type.equals("changedOrig")) {
				changed_otn = info_otn.getChangedLinesOrig();
				changed_ntp = info_ntp.getChangedLinesOrig();				
			}
			else if (type.equals("removed")) {
				changed_otn = info_otn.getRemovedLines();
				changed_ntp = info_ntp.getRemovedLines();				
			}
			else {
				changed_otn = info_otn.getAddedLines();
				changed_ntp = info_ntp.getAddedLines();					
			}
			//获取对应的changedLines的集合
			for (Integer k : changed_otn) {
				otn.add(lineToSrc_OTN.get(k));
			}
			for (Integer k : changed_ntp) {
				ntp.add(lineToSrc_NTP.get(k));
			}
			//求交集
			if (otn.retainAll(ntp)) {
				for (String k : otn) {
					inter_otn.add(srcToLine_OTN.get(k));
				}
				for (String k : otn) {
					inter_ntp.add(srcToLine_NTP.get(k));
				}
				Set<Integer> retained_otn = changed_otn;
				retained_otn.removeAll(inter_otn);
				
				Set<Integer> retained_ntp = changed_ntp;
				retained_ntp.removeAll(inter_ntp);
			
				if (type.equals("changedMod")) {
					listener_otn.methodASTInfo.get(name).setChangedLinesMod(retained_otn);
					listener_ntp.methodASTInfo.get(name).setChangedLinesMod(retained_ntp);	
				}
				else if (type.equals("changedOrig")) {
					listener_otn.methodASTInfo.get(name).setChangedLinesOrig(retained_otn);
					listener_ntp.methodASTInfo.get(name).setChangedLinesOrig(retained_ntp);					
				}
				else if (type.equals("removed")) {
					listener_otn.methodASTInfo.get(name).setRemovedLines(retained_otn);
					listener_ntp.methodASTInfo.get(name).setRemovedLines(retained_ntp);					
				}
				else {
					listener_otn.methodASTInfo.get(name).setAddedLines(retained_otn);
					listener_ntp.methodASTInfo.get(name).setAddedLines(retained_ntp);					
				}
			}
		}
	}
	public int run_otn() {
		Config.enableLogging(true);
		
		Config conf = new Config(old_to_new);
//		Config conf1 = new Config(new_to_patch);

		PruningRSEListener listener_otn = new PruningRSEListener(conf);
		return listener_otn.ComputeDiff();
		
	}
	public int run_ntp() {
		Config.enableLogging(true);
		
//		Config conf = new Config(old_to_new);
		Config conf1 = new Config(new_to_patch);

		PruningRSEListener listener_ntp = new PruningRSEListener(conf1);
		return listener_ntp.ComputeDiff();
		
	}
	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub

		Config.enableLogging(true);
		String filename = "Compiler";
				
		RunJpf jpf = new RunJpf(filename);
		
		Config conf = new Config(jpf.old_to_new);
		Config conf1 = new Config(jpf.new_to_patch);

		
//		PruningRSEListener listener_otn = new PruningRSEListener(conf);
//		listener_otn.ComputeDiff();
		
		PruningRSEListener listener_ntp = new PruningRSEListener(conf1);
		listener_ntp.ComputeDiff();

		
//		jpf.deleteSameChange(listener_otn, listener_ntp, "changedMod");
//		jpf.deleteSameChange(listener_otn, listener_ntp, "changedOrig");
//		jpf.deleteSameChange(listener_otn, listener_ntp, "removed");
//		jpf.deleteSameChange(listener_otn, listener_ntp, "added");
		
		
		
//		jpf.addListener(listener);
//		jpf.run();
			
	}

}
