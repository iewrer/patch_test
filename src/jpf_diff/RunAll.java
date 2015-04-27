package jpf_diff;

import gov.nasa.jpf.tool.Run;

import java.io.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

//import lazyinit.paramAndPoly.intNode;

public class RunAll {
	
	Set<String> filenames = new HashSet<String>();
	Set<ErrorCount> errors = new HashSet<>(); 
	
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
		String path = "./script/filename.uniq";
		RunAll runAll = new RunAll();
		runAll.readFileName(path);
		
		
		String newPath = "./dotFiles/new/jdt/";
		String patchPath = "./dotFiles/patch/jdt/";
		
		int index = -1;
		Scanner in = new Scanner(System.in);
		index = in.nextInt();
		
		if (index == -1) {
			for (String filename : runAll.filenames) {
				String p = newPath + filename;
				String p1 = patchPath + filename;
				
				runAll.bakDot(p);
				runAll.bakDot(p1);

			}
			System.out.println("bak Dot file over,quit...");
			System.exit(0);			
		}
		else if (index == 0) {
			//先删除之

			for (String filename : runAll.filenames) {
				String p = newPath + filename;
				String p1 = patchPath + filename;
				
				runAll.deleteDot(p);
				runAll.deleteDot(p1);

			}
			System.out.println("delete Dot over,quit...");
			System.exit(0);
		}
		
		
		Set<String> stop = new HashSet<>();
		Set<String> read = new HashSet<>();
		stop.add("UnconditionalFlowInfo");
		stop.add("BinaryTypeConverter");
		stop.add("LongCache");
		stop.add("TypeDeclaration");
		stop.add("ArrayAllocationExpression");
		stop.add("JavaModelOperation");
		stop.add("TypeReference");
		
		RunJpf runJpf = new RunJpf("");
		File run = new File("./script/run.txt");
		File error_count = new File("./script/error_count.txt");
		int i = 0;
//		int j = 0;
		BufferedReader reader = new BufferedReader(new FileReader(run));
		String line = "";
		while((line = reader.readLine()) != null) {
			read.add(line);
		}
		reader.close();
		BufferedWriter error_writer = new BufferedWriter(new FileWriter(error_count, true));
		BufferedWriter writer = new BufferedWriter(new FileWriter(run, true));
		int compute_diff_error = 0;
		try {
			for (String filename : runAll.filenames) {
				if (stop.contains(filename) || read.contains(filename)) {
					System.out.println("not this one");
					continue;
				}
				runJpf.change(filename);
				ErrorCount error = null;
				if (index == 1) {
					error = runJpf.run_otn(filename);
				}
				else {
					error = runJpf.run_ntp(filename);
				}
				writer.write(filename + "\n");
				runAll.errors.add(error);
			}			
		} catch (Exception e) {
			// TODO: handle exception
			System.err.println("error happened when running all!");
			e.printStackTrace();
		}
		finally {
			System.err.println("compute diff error: " + compute_diff_error);
			writer.close();
			
			for(ErrorCount errorCount : runAll.errors) {
				error_writer.write(errorCount.print());
			}
			
			error_writer.close();
		}
	}
	private void bakDot(String p) throws IOException {
		// TODO Auto-generated method stub
		File f = new File(p);
		File[] Files = f.listFiles();
		
		for (File Dot : Files) {
			if (Dot.exists() && !Dot.getName().contains("bak")) {
				System.out.println(f.getPath());
				Dot.renameTo(new File(f.getPath() + "/bak_" + Dot.getName()));
			}
		}		
	}
	private void deleteDot(String p) {
		// TODO Auto-generated method stub
		File f = new File(p);
		File[] Files = f.listFiles();
		
		for (File Dot : Files) {
			if (Dot.exists() && !Dot.getName().contains("bak")) {
				if (Dot.isDirectory()) {
					deleteDot(Dot.getAbsolutePath());
				}
				Dot.delete();
			}
		}
	}

}
