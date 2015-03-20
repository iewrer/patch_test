package jpf_diff;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.HashSet;
import java.util.Set;
import java.util.Set;

import javax.swing.text.html.HTMLDocument.Iterator;


class Path {
	static String[] prop = {"./script/patch/precise/Example01.jpf",
							"./script/new/precise/Example01.jpf"};
	static String[] dot = {"./dotFiles/patch/jdt/Compiler/Compiler.2.compile.dot",
							"./dotFiles/new/jdt/Compiler/Compiler.2.compile.dot"};
	static String[] classpath = {"/Users/barry/code/patch/archive-2.9.0.xx-20140320-0100-e43-SNAPSHOT/plugins/org.eclipse.jdt.core.source_3.9.2.xx-20140320-0100-e43-SNAPSHOT/org/eclipse/jdt/internal/compiler/Compiler.java",
		"/Users/barry/code/patch/org.eclipse.jdt.source-4.3.2/plugins/org.eclipse.jdt.core.source_3.9.2.v20140114-1555/org/eclipse/jdt/internal/compiler/Compiler.java"};
}

class impactSet {
	Set<Integer> condLoc;
	Set<Integer> writeLoc;
	Set<Integer> assertLoc;
	Set<Integer> changeLoc;
	Set<Integer> allLoc;
	public impactSet() {
		// TODO Auto-generated constructor stub
		condLoc = new HashSet<Integer>();
		writeLoc = new HashSet<Integer>();
		changeLoc = new HashSet<Integer>();
		allLoc = new HashSet<Integer>();;
		assertLoc = new HashSet<Integer>();
	}
}

public class Diff {
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
			if (string.contains("color")) {
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
	public static void main(String[] args) {
		// TODO Auto-generated method stub

		Diff diff = new Diff();
		impactSet s = new impactSet();
		impactSet s1 = new impactSet();
		boolean cross = false;
		try {
			diff.analyzeDot(Path.dot[0], s);
			System.out.println("patch complete!");
			diff.analyzeDot(Path.dot[1], s1);
			System.out.println("new complete!");
			diff.printChange(Path.classpath[0], s);
			diff.printChange(Path.classpath[1], s1);
			if (cross) {
				System.out.println("no cross over!");
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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
