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
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;
import java.util.Set;

import javax.swing.text.html.HTMLDocument.Iterator;


class Path {
	static String[] prop = {"./script/patch/precise/Example01.jpf",
							"./script/new/precise/Example01.jpf"};
	static String[] dot = {"./dotFiles/patch/jdt/Compiler/Compiler_16_compile.dot",
							"./dotFiles/new/jdt/Compiler/Compiler_16_compile.dot"};
	static String[] classpath = {"/Users/barry/code/patch/archive-2.9.0.xx-20140320-0100-e43-SNAPSHOT/plugins/org.eclipse.jdt.core.source_3.9.2.xx-20140320-0100-e43-SNAPSHOT/org/eclipse/jdt/internal/compiler/Compiler.java",
		"/Users/barry/code/patch/org.eclipse.jdt.source-4.3.2/plugins/org.eclipse.jdt.core.source_3.9.2.v20140114-1555/org/eclipse/jdt/internal/compiler/Compiler.java"};
}

class impactSet {
	String path;
	Set<Integer> condLoc;
	Set<Integer> writeLoc;
	Set<Integer> assertLoc;
	Set<Integer> changeLoc;
	Set<Integer> allLoc;
	public impactSet(String path) {
		// TODO Auto-generated constructor stub
		condLoc = new HashSet<Integer>();
		writeLoc = new HashSet<Integer>();
		changeLoc = new HashSet<Integer>();
		allLoc = new HashSet<Integer>();;
		assertLoc = new HashSet<Integer>();
		this.path = path;
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
			if (string.contains("color") && !string.contains("Depends")) {
				String label = string.substring(string.indexOf(")") + 1, string.lastIndexOf("\""));
				String[] nums = label.split("-");
				Integer begin = new Integer(nums[0]);
				Integer end = new Integer(nums[1]);
				if (string.contains("**")) {
					for (int i = begin; i < end; i++) {
						now.changeLoc.add(i);
						now.allLoc.add(i);
//						System.out.println(i);
					}
				}
				else if (string.contains("red")) {
					for (int i = begin; i < end; i++) {
						now.condLoc.add(i);
						now.allLoc.add(i);
					}	
				}
				else if (string.contains("purple")){
					for (int i = begin; i < end; i++) {
						now.assertLoc.add(i);
						now.allLoc.add(i);
					}					
				}
				else if (string.contains("blue")){
					for (int i = begin; i < end; i++) {
						now.writeLoc.add(i);
						now.allLoc.add(i);
					}
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
			String path = newPath + filename;
			String path1 = patchPath + filename;
			impactSet s = new impactSet(path);
			impactSet s1 = new impactSet(path1);
			
			diff.computeImpact(s, s1);
			

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
					
					analyzeDot(newPath, s);
					System.out.println("patch complete!");
					analyzeDot(patchPath, s1);
					System.out.println("new complete!");
					
					s.allLoc.retainAll(s1.allLoc);
					BufferedWriter writer = new BufferedWriter(new FileWriter(newDot.getAbsolutePath() + ".txt"));
					if (s.allLoc.isEmpty()) {
						writer.write("no intersections!\n");
					}
					else {
						for (Integer integer : s.allLoc) {
							writer.write(integer + "\n");
						}
					}
					writer.close();
				}
			}
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
