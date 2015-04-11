package jpf_diff;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import gov.nasa.jpf.Config;
//import gov.nasa.jpf.JPF;
//import gov.nasa.jpf.regression.listener.InterProcPruningListener;
import gov.nasa.jpf.regression.listener.PruningRSEListener;

public class RunNext {

	String file = "Compiler";
	String old_to_new = "./script/" + file + "/" + file + "_new.jpf";
	String new_to_patch = "./script/" + file + "/" + file + "_patch.jpf";
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		Config.enableLogging(true);
		
		RunNext jpf = new RunNext();
		
		Config conf = new Config(jpf.old_to_new);
		Config conf1 = new Config(jpf.new_to_patch);

		
//		PruningRSEListener listener_otn = new PruningRSEListener(conf);
//		listener_otn.ComputeDiff();
		
//		PruningRSEListener listener_ntp = new PruningRSEListener(conf1);
//		listener_ntp.ComputeDiff();
	}

}
