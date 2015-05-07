package jpf_diff;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.regression.listener.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


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
	
	public ErrorCount run_otn(String filename) {
		Config.enableLogging(true);
		
		Config conf = new Config(old_to_new);
//		Config conf1 = new Config(new_to_patch);

		ErrorCount error = new ErrorCount(filename);
		PruningRSEListener listener_otn = new PruningRSEListener(conf, error);
		return listener_otn.ComputeDiff();
		
	}
	public ErrorCount run_ntp(String filename) {
		Config.enableLogging(true);
		
//		Config conf = new Config(old_to_new);
		Config conf1 = new Config(new_to_patch);

		ErrorCount error = new ErrorCount(filename);
		PruningRSEListener listener_ntp = new PruningRSEListener(conf1, error);
		return listener_ntp.ComputeDiff();
		
	}
	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub

		Config.enableLogging(true);
		String filename = "Compiler";
//		String filename = "ASTRewriteAnalyzer";
				
		RunJpf jpf = new RunJpf(filename);
		
//		ErrorCount count = jpf.run_otn(filename);
//		count.print();
		
		ErrorCount count = jpf.run_ntp(filename);
		count.print();

	}

}
