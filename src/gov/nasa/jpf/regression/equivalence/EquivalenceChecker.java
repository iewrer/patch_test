package gov.nasa.jpf.regression.equivalence;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.smtlib.CharSequenceReader;
import org.smtlib.ICommand;
import org.smtlib.IParser;
import org.smtlib.IPrinter;
import org.smtlib.IResponse;
import org.smtlib.ISolver;
import org.smtlib.ISource;
import org.smtlib.SMT;

//given the symbolic summaries for two methods, determine functional
//equivalence ala DiSE's notion of equivalence
public class EquivalenceChecker {
	
	private static String oldName = "";
	private static String modName = "";
	private List<String> oldPCSummary;
	private List<String> newPCSummary;
	private List<Path> oldSExpSummary;
	private List<Path> newSExpSummary;
	private static String solverName = "cvc3"; //default
	private static String solverPath = "TBD"; //default
	private static String equivalenceType = "";
	private SMT smt;
	private ISolver solver;
	private Map<String,VariableDefinition> vars = new HashMap<String,VariableDefinition>();
	//String dpExePath = "/home/sperson/cvc3/cvc3-2.2-i686-linux-opt";
	//String dpExePath = "/home/sperson/yices/yices-1.0.27/bin/yices";
	
	private static boolean verbose = false;
	
	private static boolean dropConstraints=true;
	private static int oldDropped = 0;
	private static int oldKept = 0;
	private static int newDropped = 0;
	private static int newKept = 0;
	
	/*
	 * Extract summaries from input text files
	 * Format is specified in RSESymbolicListener
	 */
	public void extractMethodSummaries(){
		System.out.println("Comparing " + oldName + " with \n" + modName);
		File file1 = new File(oldName);
		if (file1.exists()){
			oldPCSummary = new ArrayList<String>();
			InputStream is = null;
			try{
				is = new FileInputStream(file1);
		        BufferedReader br = new BufferedReader (new InputStreamReader(is));
		        String line="";
		        while((line=br.readLine())!=null){
		        	if (line.length()>0)
		        		oldPCSummary.add(line);
		        }
			}catch (Exception e){
				e.printStackTrace();
			}
		}else{
			System.out.println("File not found: " + oldName);
			System.exit(0);
		}
		File file2 = new File(modName);
		if (file2.exists()){
			newPCSummary = new ArrayList<String>();
			InputStream is2 = null;
			try{
				is2 = new FileInputStream(file2);
		        BufferedReader br = new BufferedReader (new InputStreamReader(is2));
		        String line="";
		        while((line=br.readLine())!=null){
		        	newPCSummary.add(line);
		        }
			}catch (Exception e){
				e.printStackTrace();
			}
		}else{
			System.out.println("File not found: " + modName);
			System.exit(0);
		}
	}
	
	//Takes the raw path information computed by DiSE
	//and returns the S-expressions for the pc, effects and dropped constraints
	public Path createPathInfo(String path,String version){
		//Get the path condition
		String pc = path.substring(path.indexOf("pc")+3);
		pc = pc.substring(0,pc.indexOf(";"));
		//Get the effects constraint
		String effects = path.substring(path.indexOf("effects")+8);
		effects = effects.substring(0,effects.indexOf(";"));
		String droppedC = ""; //conjunction of dropped constraints
		List<String> dExp = new ArrayList<String>(); //list of dropped constraints
		if (dropConstraints){
			//Get list of constraints that can be dropped
			String cToDrop ="";
			cToDrop = path.substring(path.indexOf("constraints")+12);
			if (cToDrop.contains("["))
				cToDrop = cToDrop.substring(1,cToDrop.length()-1); //strip []
			String[] dConstraints=null;	
			if (!cToDrop.equalsIgnoreCase("none") && !cToDrop.equalsIgnoreCase("")){
				dConstraints = cToDrop.split(",");
				for(int i=0; i<dConstraints.length;i++){
					String s = dConstraints[i].trim();
					s = removeSolutions(s);
					s = reformat(s);
					if (s.length()>0)
						dExp.add(s);
				}
				//compose dropped constraints
				if (dExp.size()>1)
					droppedC = createConjunction(dExp);
				else
					droppedC = dExp.get(0);
			}
		}
		//now that we have the dropped constraints, process the path conditions
		String[] constraints = pc.split("&&");
		List<String> pcConstraints = new ArrayList<String>();
		for(int i=0; i<constraints.length;i++){
			String constraint = constraints[i].trim();
			String s = processConstraint(constraint,dExp,version);
			if (s.length()>0)
				pcConstraints.add(s);
		}
		//compose PC constraints
		String pathExp = "";
		if (pcConstraints.size()>1)
			pathExp = createConjunction(pcConstraints);
		else if (pcConstraints.size() != 0)
			pathExp = pcConstraints.get(0);
		
		//convert the effects to an S-expr
		String[] effectsList = effects.split("&&");
		List<String> effectsConstraint = new ArrayList<String>();
		for (int i=0; i<effectsList.length; i++){
			String constraint = effectsList[i].trim();
			constraint = removeSolutions(constraint);
			String s = processEqualityExpression(constraint);
			if (s.length()>0)
				effectsConstraint.add(s);
		}
		//compose effects
		String effectsExp = "";
		if (effectsConstraint.size()> 1)
			effectsExp = createConjunction(effectsConstraint);
		else
			effectsExp = effectsConstraint.get(0);
		
		//create a Path object from each of the components
		Path p = new Path(pathExp,effectsExp,droppedC);
		if (verbose){
			System.out.println(p);
			System.out.println("*************************");
		}
		return p;
	}
	
	public String createConjunction(List<String> constraints){
		String conj = " ( and ";
		if (constraints.size()>1){
			for (int i=0;i<constraints.size();i++)
				conj = conj + constraints.get(i) + " ";
			conj = conj + " ) ";
		}else
			conj = " ( " + constraints.get(0) + " ) ";
		return conj;
	}
	
	//each entry in the list represents a pc and return value
	public String createDisjunction(List<Path> constraints){
		String conj = " ( or ";
		if (constraints.size()>1){
			for (int i=0;i<constraints.size();i++)
				conj = conj + formatFinalConstraint(constraints.get(i)) + " ";
			conj = conj + " ) ";
		}else if (constraints.size() == 1)
			conj = formatFinalConstraint(constraints.get(0));
			//conj = " ( " + formatFinalConstraint(constraints.get(0)) + " ) ";
		else
			conj = "";
		return conj;
	}
	
	private String formatFinalConstraint(Path p){
		String finalConstraint = "";
		//and
		if (!p.getPC().startsWith(" ( and "))
			finalConstraint = "( and " + p.getPC() + " " + p.getEffects() + " )";
		else{
			finalConstraint = p.getPC();
			finalConstraint = finalConstraint.substring(0,finalConstraint.lastIndexOf(")")-1);
			finalConstraint = finalConstraint + " " + p.getEffects() + " )";
		}
		//implies
		//finalConstraint = "( => " + p.getPC() + " " + p.getEffects() + " ) ";
		//finalConstraint = "( or ( not " + p.getPC() + " ) " + p.getEffects() + " ) ) ";
		//add dropped with implication
		if (!p.getDropped().equalsIgnoreCase(""))
			finalConstraint = "( => " + p.getDropped() + " " + finalConstraint + " ) ";
			//finalConstraint = "( or ( not " + p.getDropped() + " ) " + finalConstraint + " ) )";
		return finalConstraint;
	}
	
	private String processEqualityExpression(String c){
		String loc = c.substring(0, c.indexOf("="));
		String val = c.substring(c.lastIndexOf("=")+1, c.length());
		
		val = processVar(val);
		String type = "";
		if (vars.containsKey(val)){
			type = vars.get(val).getType();
		}else{
			type = "Int";
			System.out.println("WARNING: Assuming type of " + loc + " is Integer!!");
		}
		
		//give these variables new names from the path condition
		if (!loc.equalsIgnoreCase("RETURN"))
			loc = loc + "_FINAL";
		VariableDefinition newVar = new VariableDefinition(loc,type);
		vars.put(loc, newVar);
		
		String exp = "( = " + loc + " " + val + " )"; 
		exp = reformat(exp);
		return exp;
	}
	
	//assumes the string is already an S-expression
	private String reformat(String s){
		String processed = "";
		String[] terms = s.split(" ");	
		for (int i=0; i<terms.length; i++){
			String str = terms[i].trim();
			if (str.startsWith("(")){
				if (processed.endsWith(" "))
					processed = processed + str;
				else
					processed = processed + " " + str;
			}else if (str.startsWith(")"))
				processed = processed + str;
			else if (isOperator(str)){
				if (str.equalsIgnoreCase("=="))
					processed = processed + "= ";
				else if (str.equalsIgnoreCase("!="))
					processed = processed + "distinct ";
				else
					processed = processed + str + " ";
			}else{
				if (!str.equalsIgnoreCase("")){
					String suffix = "";
					if (str.endsWith(")")){
						suffix = str.substring(str.indexOf(")"));
						str = str.substring(0,str.indexOf(")"));
					}
					String tmp = processVar(str) + suffix;
					processed = processed + " " + tmp;
				}

			}
		}
		return processed;
	}
	
	//reformat the constraints on the PC
	//1. remove solutions
	//2. check if constraint should be dropped
	//3. reformat names
	//4. register variables
	private String processConstraint(String s, List<String> drop,String version){
		s = removeSolutions(s);
		//s = removeWhiteSpace(s);
		s = reformat(s);
		//check if this constraint should be dropped
		if (dropConstraints && drop.contains(s)){
				if (version.equals("old"))
					oldDropped++;
				else
					newDropped++;
				return "";
		}
		if (version.equalsIgnoreCase("old"))
			oldKept++;
		else
			newKept++;
//		String processedConstraint = reformat(s);
//		return processedConstraint;
		return s;
	}
	
	//helper method to strip off the solutions in []
	private String removeSolutions(String s){
		while (s.contains("[")){
			int start = s.indexOf("[");
			int end = s.indexOf("]");
			String tmp1 = s.substring(0,start);
			String tmp2 = s.substring(end+1);
			s = tmp1 + tmp2;
		}
		return s;
	}
	
	private boolean isOperator(String s){
		if (s.length()>2)
			return false;
		else if (s.contains(">"))
			return true;
		else if (s.contains("<"))
			return true;
		else if (s.contains("="))
			return true;
		else if (s.contains("=="))
			return true;
		else if (s.contains("!="))
			return true;
		else if (s.contains("-") && s.length()==1)
			return true;
		else if (s.contains("+"))
			return true;
		else if (s.contains("/"))
			return true;
		else 
			return false;
	}
	
	/*
	 * Cleans up the var name and adds it to the list of vars if not
	 * already defined
	 */
	private String processVar(String var){
		String val = "";
		if (var.startsWith("(") && var.endsWith(")"))
			return var;
		//only process if the expression is a simple expression
		if (var.startsWith("("))
			var = var.substring(1);
		if (var.endsWith(")"))
			var = var.substring(0,var.length()-1);
		if (var.contains("["))
			var = var.substring(0,var.indexOf("["));
		if (var.contains("CONST")){
			val = var.substring(var.indexOf("_")+1);
			if (val.startsWith("-")){
				val = val.substring(1);
				val = "(- " + val + ")";
			}
		}else if(var.contains("VOID")){
			val = var;
			if (!vars.containsKey("VOID")){
				VariableDefinition newVar = new VariableDefinition(var,"Int");
				vars.put(var, newVar);
			}
		}else if(var.contains("RETURN")){
			val = var;
			if (!vars.containsKey("RETURN")){
				VariableDefinition newVar = new VariableDefinition(var,"Int");
				vars.put(var, newVar);
			}
		}else if(var.equalsIgnoreCase(""))
			return var;
		else{
			if (!vars.containsKey(var)){
				if (var.contains("INT")){
					VariableDefinition newVar = new VariableDefinition(var,"Int");
					vars.put(var, newVar);
				}else{
					System.out.println("TODO: new type of variable : " + var);
					System.out.println("Assuming of integer type");
					VariableDefinition newVar = new VariableDefinition(var, "Int");
					vars.put(var, newVar);
				}
			}
			val = vars.get(var).getName();
		}
		return val;
	}
	
	public void reformatPathConditions(){
		if (verbose)
			System.out.println("OldPCS");
		Iterator<String> itO = oldPCSummary.iterator();
		oldSExpSummary = new ArrayList<Path>();
		while (itO.hasNext()){
			String pc = itO.next();
			if (pc.length()>0){
				Path p  = createPathInfo(pc,"old");
				if (p.getPC().length()!=0)
					oldSExpSummary.add(p);
			}
		}
		if (verbose)
			System.out.println("NewPCS");
		Iterator<String> itN = newPCSummary.iterator();
		newSExpSummary = new ArrayList<Path>();
		while (itN.hasNext()){
			String pc = itN.next();
			if (pc.length()>0){
				Path p = createPathInfo(pc,"new");
				if (p.getPC().length() != 0)
					newSExpSummary.add(p);
			}
		}
	}
	
	public void initSolver(){
		smt = new SMT();
	}
	
	private String getDefString(){
		String def = "";
		for (Map.Entry<String, VariableDefinition> entry: vars.entrySet()){
			VariableDefinition val = entry.getValue();
			def = def + "(declare-fun " + val.getName() + " () " + val.getType() + ")";
		}
		//def = def +"(declare-fun return() Int)"; //TODO need to know if int or...
		return def;
	}
	
	public String compareVersionsFunc(){
		String equivalent = "??";
		try {
			String oldConj = createDisjunction(oldSExpSummary);
			String newConj = createDisjunction(newSExpSummary);
			if (oldConj.length()== 0 || newConj.length()==0){
				if (oldConj.length()==0 && newConj.length()==0){
					System.out.println("******WARNING: No Queries to check!!");
					equivalent = "yes";
				}else{
					System.out.println("*****WARNING: Only one version contains path conditions");
					System.out.println("old length=" + oldConj.length() + " new length=" +  newConj.length());
					equivalent = "no";
				}
			}else{
				//join into an equality check and negate since we are checking validity
				String query ="(assert (not ( = " + oldConj + " " + newConj + " )))";
				//query checking implies in both directions
				//String query = "(assert (not ( or ( and " + oldConj + " (not " + newConj + " )) (and " + newConj + "(not " + oldConj + " )))))";
				//TEST START
				//String tmp = "( = p p )";
				//query ="( assert ( not " + tmp + " "  + " ))"; //TEST
				//String query = "(declare-fun p () Int)";
				//TEST END
				String option = "(set-option :produce-models true) ";
				//String option = "(set-option :produce-assignments true) ";
				String logic = "(set-logic QF_LIA) ";
				String check = "(check-sat)";
				String definitions = getDefString() + " ";
				String command = option + logic + definitions + query + " " + check;
				if (verbose)
					System.out.println("Command: " + command);
				//Parse the command string built above
				ISource source = 
					smt.smtConfig.smtFactory.createSource(new CharSequenceReader(new java.io.StringReader(command)),null);
				IParser parser = smt.smtConfig.smtFactory.createParser(smt.smtConfig,source);
				List<ICommand> commandList = new ArrayList<ICommand>();
				int numberOfCommands = vars.entrySet().size() + 4;
				for (int i=0; i< numberOfCommands; i++){
					commandList.add(parser.parseCommand());
				}
				//Assemble the script of commands
				ICommand.IScript script = new org.smtlib.impl.Script();
				Iterator<ICommand> it = commandList.iterator();
				while (it.hasNext()){
					ICommand cmd = it.next();
					script.commands().add(cmd);
					if (verbose)
						System.out.println(smt.smtConfig.defaultPrinter.toString(cmd));
				}
				// Execute the script
				System.out.println("Executing the script...");
				File sPath = new File(solverPath);
				if (!sPath.exists()){
					System.out.println("ERROR: Cannot find the directory containing the solver: " + solverPath);
					System.exit(0);
				}
				if (solverName.equalsIgnoreCase("yices"))
					solver = new org.smtlib.solvers.Solver_yices(smt.smtConfig,solverPath);
				else
					solver = new org.smtlib.solvers.Solver_cvc(smt.smtConfig,solverPath);
				solver.start();
				IResponse response = script.execute(solver);
				IPrinter printer = smt.smtConfig.defaultPrinter;
				String sat = printer.toString(response);
				if (verbose)
					System.out.println("response: " + sat);
				response = solver.exit();
				if (verbose)
					System.out.println("Final response: " + response.toString());
				if (sat.equalsIgnoreCase("unsat"))
						equivalent = "yes";
				else
					equivalent = "no";
			}
			return equivalent;
		}catch (java.io.IOException e) {
			// Can happen if the ISource is reading from a file
			return equivalent;
		} catch (IParser.ParserException e) {
			System.out.println(e.getMessage());
			return equivalent;
		}
	}
	
	
	private static boolean checkArgs(String[] args){
		boolean valid = true;
		if (args.length < 5){
			System.out.println("USAGE: java EquivalenceChecker " + 
					" -o(riginal) <test file>.txt " +
					" -m(odified) <test file>.txt " + 
					" -s(olver) <yices | cvc3>" +
					" -p(ath to solver) <path>" +
					" -v(erbose)" +
					" -d(rop) constraints" +
					" -e(quivalence) <f>");  //functional equivalence
            valid = false;
		}else{
			for (int i=0; i<args.length; i++){
				if (args[i].startsWith("-o")) {
					oldName = args[i+1];
					i++;
				}else if (args[i].startsWith("-m")) {
					modName = args[i+1];
					i++;
				}else if (args[i].startsWith("-s")) {
					solverName = args[i+1];
					i++;
				}else if (args[i].startsWith("-p")) {
					solverPath = args[i+1];
					i++;
				}else if (args[i].startsWith("-v")) {
					verbose = true;
				}else if (args[i].startsWith("-d")){
					dropConstraints = true;
				}else if (args[i].startsWith("-e")) {
					if (args[i+1].equalsIgnoreCase("f"))
						equivalenceType = args[i+1];
					i++;
				}
			}
		}
		if (oldName.equalsIgnoreCase("") || modName.equalsIgnoreCase("")){
			System.out.println("You must at least enter the file names containing the method summaries");
            valid = false;
		}
		return valid;
	}
	
	public static void main(String[] args){
		System.out.println("iDiSE Equivalence Checker 3/21/2012");
		System.out.println("Dropping constraints? -->" + dropConstraints);
		if (!checkArgs(args))
			System.exit(0);
		EquivalenceChecker eq = new EquivalenceChecker();
		eq.extractMethodSummaries();
		eq.reformatPathConditions();
		eq.initSolver();
		if (equivalenceType.startsWith("f")){
			String equivalent = eq.compareVersionsFunc();
			System.out.println("Equivalent?--->"  + equivalent);
		}else
			System.out.println("Unsupported Equivalence Type: " + equivalenceType);
		if (verbose){
			System.out.println("Number of dropped constraints for this method (old version) = " + oldDropped);
			System.out.println("Number of dropped constraints for this method (new version) = " + newDropped);
			System.out.println("Number of constraints kept for this method (old version) = " + oldKept);
			System.out.println("Number of constraints kept for this method (new version) = " + newKept);
		}
		System.out.println("Equivalence Checker Done.");
	}
}