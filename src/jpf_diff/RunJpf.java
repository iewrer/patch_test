package jpf_diff;

import gov.nasa.jpf.Config;
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
import java.util.Set;

import tree_diff.test_v1;

//import gov.nasa.jpf.JPF;
//import gov.nasa.jpf.Config;
//import gov.nasa.jpf.JPF.ExitException;
//import gov.nasa.jpf.regression.listener.PruningRSEListener;
//import gov.nasa.jpf.tool.RunJPF;


public class RunJpf {

	public static void main(String[] args) {
		// TODO Auto-generated method stub

		Config.enableLogging(true);
		
		String file = "Scanner";
		String old_to_new = "./script/" + file + "/" + file + "_patch.jpf";
		String new_to_patch = "./script/" + file + "/" + file + "_new.jpf";
		Config conf = new Config(old_to_new);
		Config conf1 = new Config(new_to_patch);
//		Config conf = Config.createConfig();
//		
//		conf.setTarget("org.eclipse.jdt.internal.compiler");
//		conf.setProperty("@using", "jpf-regression");
//		conf.setProperty("classpath","build/tests/patch/jdt");
//		conf.setProperty("sourcepath", "./src/tests/patch/jdt");
//		conf.setProperty("rse.newClass", "./build/tests/patch/jdt/org/eclipse/jdt/internal/compiler/Compiler.class");
//		conf.setProperty("rse.oldClass", "./old/bin/jdt/org/eclipse/jdt/internal/compiler/Compiler.class");
//		conf.setProperty("rse.dotFile", "./dotFiles/patch/jdt/Compiler");
//		conf.setProperty("rse.ASTResults", "./diffFiles/patch/jdt/Compiler.xml");		
		
//		try {
//			conf.store(new FileWriter(new File(args[0])), null);
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		
//		JPF jpf = new JPF(conf);
		PruningRSEListener listener = new PruningRSEListener(conf);
		listener.ComputeDiff();
		PruningRSEListener listener1 = new PruningRSEListener(conf1);
		listener1.ComputeDiff();
//		jpf.addListener(listener);
//		jpf.run();
			
	}

}
