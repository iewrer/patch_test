package gov.nasa.jpf.regression.listener;

import java.util.HashMap;
import java.util.Map;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.JPF;
import gov.nasa.jpf.PropertyListenerAdapter;
import gov.nasa.jpf.vm.ChoiceGenerator;
import gov.nasa.jpf.vm.ThreadInfo;
import gov.nasa.jpf.vm.VM;
import gov.nasa.jpf.jvm.bytecode.IfInstruction;
import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.regression.output.PrintToXML;
import gov.nasa.jpf.report.PublisherExtension;
import gov.nasa.jpf.search.Search;
import gov.nasa.jpf.symbc.numeric.PCChoiceGenerator;
import gov.nasa.jpf.symbc.numeric.PathCondition;

public class DiSEVisualizationListener extends PropertyListenerAdapter 
		implements PublisherExtension {
	
	private String srcPath = "";
	private String bcPath = "";
	private String statFile = "";
	private String symbolicMethod = "";
	private String symbolicClass = "";
	
	private int constraintCount = 0;
	
	private Map<String,String> nameMap = new HashMap<String,String>();
	
	private PrintToXML printXML;
	
	public DiSEVisualizationListener(Config conf, JPF jpf) {
		System.out.println("---------->You are using the DiSEVisualizationListener 7/23/2012");
		if (conf.containsKey("rse.bcPath"))
			bcPath = conf.getString("rse.bcPath");
		if (conf.containsKey("rse.srcPath"))
			srcPath = conf.getString("rse.srcPath");
		if (conf.containsKey("rse.statFile"))
			statFile = conf.getString("rse.statFile");
		if (conf.containsKey("symbolic.method")){
			String tmp = conf.getString("symbolic.method");
			symbolicMethod = tmp.substring(tmp.lastIndexOf(".")+1,tmp.indexOf("("));
			symbolicClass = tmp.substring(0,tmp.lastIndexOf("."));
		}
		System.out.println("symMeth: " + symbolicMethod);
		System.out.println("symClass: " + symbolicClass);
		
		printXML = new PrintToXML();
	}
	
	public void executeInstruction(VM vm) {

		Instruction currInsn = vm.getCurrentThread().getPC();
		if (currInsn instanceof IfInstruction) {
			ChoiceGenerator <?>cg1 = vm.getChoiceGenerator();
			if (!(cg1 instanceof PCChoiceGenerator)){
				ChoiceGenerator <?> prev_cg = cg1.getPreviousChoiceGenerator();
				while (!((prev_cg == null) || (prev_cg instanceof PCChoiceGenerator))) {
					prev_cg = prev_cg.getPreviousChoiceGenerator();
				}
				cg1 = prev_cg;
			}
			if ((cg1 instanceof PCChoiceGenerator) &&(
					(PCChoiceGenerator) cg1).getCurrentPC() != null){
				constraintCount = ((PCChoiceGenerator) cg1).getCurrentPC().count();
			}
		}

	}
	
	public void instructionExecuted(VM vm, ThreadInfo currentThread,
			  Instruction nextInstruction, Instruction executedInstruction)  {
		Instruction lastInstruction = executedInstruction;
		
		if (!vm.getSystemState().isIgnored()) {
			String className = lastInstruction.getMethodInfo().getClassName();
			//track only instructions executed in the class of interest
			if (className.equalsIgnoreCase(symbolicClass)){
				String[] argsTypes = lastInstruction.getMethodInfo().getArgumentTypeNames();
				String methodName = lastInstruction.getMethodInfo().getName() + "(";
				
				for(int argIndex = 0; argIndex < argsTypes.length; argIndex++) {
					methodName = methodName.concat(argsTypes[argIndex]);
					if (argIndex<argsTypes.length-1)
						methodName= methodName + ",";
					else
						methodName = methodName + ")";
				}
				
				String fullName = lastInstruction.getMethodInfo().getFullName();
				nameMap.put(lastInstruction.getMethodInfo().getName(),fullName);
				
				Integer position = lastInstruction.getPosition();
				
				String fileLoc = lastInstruction.getFileLocation().trim();
				String srcLine = lastInstruction.getSourceLine().trim();
				int lineNum = lastInstruction.getLineNumber();
				String bCode = lastInstruction.getMnemonic();
				LocationData loc = new LocationData(className, methodName, 
						fullName,position, fileLoc, srcLine, lineNum, bCode);
				VisualizationData.addLocation(loc);
					
				if (lastInstruction instanceof IfInstruction) {
					Boolean condVal = ((IfInstruction)lastInstruction).getConditionValue();
	                VisualizationData.addBranchCondition(loc, condVal);
	                
	                // Save the IfInstruction in the program location
	                loc.setInstruction(lastInstruction);
	                
					ChoiceGenerator <?>cg1 = vm.getChoiceGenerator();
					if (!(cg1 instanceof PCChoiceGenerator)){
						ChoiceGenerator <?> prev_cg = cg1.getPreviousChoiceGenerator();
						while (!((prev_cg == null) || (prev_cg instanceof PCChoiceGenerator))) {
							prev_cg = prev_cg.getPreviousChoiceGenerator();
						}
						cg1 = prev_cg;
					}
					if ((cg1 instanceof PCChoiceGenerator) &&(
							(PCChoiceGenerator) cg1).getCurrentPC() != null){
						PathCondition pc = ((PCChoiceGenerator) cg1).getCurrentPC();
						if (pc.count()> constraintCount){
							//kludge to peel off the added constraint
							String pcString = pc.toString();
							if (pcString.contains("&&"))
								pcString = pcString.substring(0,pcString.indexOf("&&"));
							pcString = pcString.substring(pcString.indexOf("\n")+1);
							//System.out.println("ConstraintAdded is: " + pcString+ " at loc: " + loc.getLineNum());
							//System.out.println("Last added: " + pc.getLastConstraintAdded().toString());
							loc.addConstraint(pcString);
						}
					}
				}
			}
		}

	}
	
	public void searchStarted(Search search) {
		VisualizationData.initializeLocations();
		printXML.initialize();
	}
	
	public void searchFinished(Search search) {
		System.out.println("searchFinished");
		  printXML.printXMLFile(statFile,srcPath,bcPath,symbolicMethod,
				 nameMap);
	}
	
}