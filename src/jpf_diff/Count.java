package jpf_diff;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import javax.swing.text.DefaultEditorKit.CopyAction;

//import lazyinit.paramAndPoly.intNode;

public class Count {
	
	Set<String> filenames = new HashSet<String>();
	
	public Count() {
		// TODO Auto-generated constructor stub
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
		Count count = new Count();
		String newPath = "./dotFiles/new/jdt/";
		String patchPath = "./dotFiles/patch/jdt/";
		count.readFileName("./script/filename.uniq");
		
		
		String new_newPath = "./dotFiles/impacted/new/jdt/";
		String new_patchPath = "./dotFiles/impacted/patch/jdt/";
		
		
		counting(count, newPath, new_newPath);
		counting(count, patchPath, new_patchPath);

	}
	private static void counting(Count count, String newPath, String new_newPath) {
		// TODO Auto-generated method stub
		int impact = 0;
		int empty = 0;
		int all = 0;
		
		for (String filename : count.filenames) {
			String p = newPath + filename;
			String new_p = new_newPath + filename + "/";
			
			int res = count.countImpact(p, new_p);
			if (res > 0) {
				impact++;
			}
			else if (res == 0) {
				empty++;
			}
			all++;
		}
		System.err.println("all: " + (all - empty));
		System.err.println("impacted: " + impact);
		
	}
	private int countImpact(String p, String new_p) {
		File f = new File(p);
		File[] Files = f.listFiles();
		
		boolean empty = true;
		
		for (File Dot : Files) {
			if (Dot.exists() && Dot.isDirectory() && Dot.getName().contains("impact")) {
				copy(Dot, new_p + Dot.getName());
				System.out.println(p);
				return 1;
			}
			else if (Dot.exists() && !Dot.isDirectory()){
				empty = false;
			}
		}// TODO Auto-generated method stub
		
		if (empty) {
			return 0;
		}
		return -1;
	}
	private void copy(File dot, String path) {
		// TODO Auto-generated method stub
		File newDot = new File(path);
		dot.renameTo(newDot);
		if (!newDot.getParentFile().exists()) {
			newDot.getParentFile().mkdirs();
//			System.err.println("create dir: " + newDot.getParentFile());
		}
	}

}
