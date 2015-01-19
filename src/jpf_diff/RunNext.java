package jpf_diff;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import gov.nasa.jpf.Config;
//import gov.nasa.jpf.JPF;
import gov.nasa.jpf.regression.listener.InterProcPruningListener;
import gov.nasa.jpf.regression.listener.PruningRSEListener;

public class RunNext {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		Config.enableLogging(true);
		
//		Config conf1 = new Config(Path.prop[1]);
		Config conf1 = Config.createConfig();
		
		conf1.setTarget("Example01");
		conf1.setProperty("@using", "jpf-regression");
		conf1.setProperty("classpath","build/tests/patch");
		conf1.setProperty("sourcepath", "./src/tests/patch");
		conf1.setProperty("rse.newClass", "./build/tests/patch/precise/Example01.class");
		conf1.setProperty("rse.oldClass", "./old/bin/precise/Example01.class");
		conf1.setProperty("rse.dotFile", "./dotFiles/patch/precise/Example01.dot");
		conf1.setProperty("rse.ASTResults", "./diffFiles/patch/precise/Example01.xml");
		
//		try {
//			conf1.store(new FileWriter(new File("EX01_v2.jpf")), null);
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		
//		JPF jpf1 = new JPF(conf1);
//		InterProcPruningListener listener = new InterProcPruningListener(conf1, jpf1);
		PruningRSEListener listener1 = new PruningRSEListener(conf1);
		listener1.ComputeDiff();
//		jpf1.addListener(listener1);
//		jpf1.run();
	}

}
