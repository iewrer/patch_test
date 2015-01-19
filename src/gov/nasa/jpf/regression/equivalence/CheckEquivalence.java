package gov.nasa.jpf.regression.equivalence;

import gov.nasa.jpf.regression.listener.MethodSummary.inputAndOutputConstraint;
import gov.nasa.jpf.symbc.numeric.Comparator;
import gov.nasa.jpf.symbc.numeric.IntegerConstant;
import gov.nasa.jpf.symbc.numeric.IntegerConstraint;
import gov.nasa.jpf.symbc.numeric.IntegerExpression;
import gov.nasa.jpf.symbc.numeric.SymbolicConstraintsGeneral;
import gov.nasa.jpf.symbc.numeric.Constraint;
import gov.nasa.jpf.symbc.numeric.SymbolicInteger;


import java.util.Iterator;
import java.util.Set;
import java.util.Vector;
import java.util.Map.Entry;

public class CheckEquivalence extends SymbolicConstraintsGeneral{
	
	public CheckEquivalence() {
		super();
	}
	
	protected Vector<Object> constructConstraint(Constraint cRef) {
		Vector<Object> pbObjs  = new Vector<Object>();
		while (cRef != null) {
			if (cRef instanceof IntegerConstraint)
				pbObjs.add(constructLinearConstraint((IntegerConstraint)cRef));
			else {
					throw new RuntimeException("## Unhandled constraint " + cRef);
			}
			cRef = cRef.and;
		}
		return pbObjs;
	}
	
	public boolean checkEquivalence(Vector<inputAndOutputConstraint> src,
							Vector<inputAndOutputConstraint> targ) {
		boolean solverinit = initializeSolverAndStructInstance("cvc3");
		if(!solverinit) return true;
	
		
		if(src.size() == 0 && targ.size() == 0) return true;
			
		Vector<Object> srcConstraints = new Vector<Object>();
		Vector<Object> targConstraints = new Vector<Object>();

		
		for(int cIndex = 0; cIndex < src.size(); cIndex++) {
			Constraint cRef = src.get(cIndex).getInputConstraint();
			Constraint oRef = src.get(cIndex).getOutputConstraint();
				
			Vector<Object> allConstraints = constructConstraint(cRef);
			//add all the output constraints 
			allConstraints.addAll(constructConstraint(oRef));
			
			Object[] objs = allConstraints.toArray();
			Object andExpr = buildAndExpr(objs);
			
			//create the andExpr
			srcConstraints.add(andExpr);
			
		}
		
		for(int cIndex = 0; cIndex < targ.size(); cIndex++) {
			Constraint cRef = targ.get(cIndex).getInputConstraint();
			Constraint oRef = targ.get(cIndex).getOutputConstraint();
						
			Vector<Object> allConstraints = constructConstraint(cRef);
			//add all the output constraints
			allConstraints.addAll(constructConstraint(oRef));
			
			Object[] objs = allConstraints.toArray();
			Object andExpr = buildAndExpr(objs); 
			targConstraints.add(andExpr);

		}
		
		Object[] finalSrcConstraints = srcConstraints.toArray();
		Object[] finalTargConstraints = targConstraints.toArray();
		
		Object srcOrExpr = buildOrExpr(finalSrcConstraints);
		Object targOrExpr = buildOrExpr(finalTargConstraints);
		
		
		Object equal = pb.not((pb.iff(srcOrExpr, targOrExpr)));
	
		pb.post(equal);
		boolean result = pb.solve();
		
		if(result) {
			//build the solution
			generateSolutions();
		}
		
		//if result is unsat, it is equivalent else it
		// is not equivalent and the satisfying assignment represents
		// the counterexample
		return (!result);
	}
	
	protected void generateSolutions() {
		Set<Entry<SymbolicInteger,Object>> sym_intvar_mappings
			= symIntegerVar.entrySet();
		Iterator<Entry<SymbolicInteger,Object>>	
						i_int = sym_intvar_mappings.iterator();
		while(i_int.hasNext()) {
			Entry<SymbolicInteger,Object> e =  i_int.next();
			e.getKey().solution=pb.getIntValue(e.getValue());
			//System.out.println(e.getKey().toString() +":=" + e.getKey().solution);
		}
	}
	
	protected Object buildAndExpr(Object[] constraints) {
		if(constraints == null) return null;
		if(constraints.length <= 0) return null;
		if(constraints.length == 1) return constraints[0];
		Object first = constraints[0];
		Object second = constraints[1];
		Object andExpr = pb.and(first, second);
		for(int cIndex = 2; cIndex < constraints.length; cIndex++) {
			andExpr = pb.and(andExpr, constraints[cIndex]);
		}
		return andExpr;
	}
	
	protected Object buildOrExpr(Object[] constraints) {
		if(constraints == null) return null;
		if(constraints.length <= 0) return null;
		if(constraints.length == 1) return constraints[0];
		Object first = constraints[0];
		Object second = constraints[1];
		Object orExpr = pb.and(first, second);
		for(int cIndex = 2; cIndex < constraints.length; cIndex++) {
			orExpr = pb.or(orExpr, constraints[cIndex]);
		}
		return orExpr;
	}
	
	public Object constructLinearConstraint(IntegerConstraint cRef) {

		Comparator c_compRef = cRef.getComparator();
		Object pbObject = null;

		IntegerExpression c_leftRef = (IntegerExpression)cRef.getLeft();
		IntegerExpression c_rightRef = (IntegerExpression)cRef.getRight();
		
		switch(c_compRef){
		case EQ:
			if (c_leftRef instanceof IntegerConstant && c_rightRef instanceof IntegerConstant) {
				pbObject = pb.eq(((IntegerConstant)c_leftRef).value, ((IntegerConstant)c_rightRef).value);
			}
			else if (c_leftRef instanceof IntegerConstant) {
				pbObject = pb.eq(((IntegerConstant)c_leftRef).value,getExpression(c_rightRef));
			}
			else if (c_rightRef instanceof IntegerConstant) {
				pbObject = pb.eq(getExpression(c_leftRef),((IntegerConstant)c_rightRef).value);
			}
			else
				pbObject = pb.eq(getExpression(c_leftRef),getExpression(c_rightRef));
			break;
		case NE:
			if (c_leftRef instanceof IntegerConstant && c_rightRef instanceof IntegerConstant) {
				pbObject = pb.neq(((IntegerConstant)c_leftRef).value, ((IntegerConstant)c_rightRef).value);
			}
			else if (c_leftRef instanceof IntegerConstant) {
				pbObject = pb.neq(((IntegerConstant)c_leftRef).value,getExpression(c_rightRef));
			}
			else if (c_rightRef instanceof IntegerConstant) {
				pbObject = pb.neq(getExpression(c_leftRef),((IntegerConstant)c_rightRef).value);
			}
			else
				pbObject = pb.neq(getExpression(c_leftRef),getExpression(c_rightRef));
			break;
		case LT:
			if (c_leftRef instanceof IntegerConstant && c_rightRef instanceof IntegerConstant) {
				pbObject = pb.lt(((IntegerConstant)c_leftRef).value, ((IntegerConstant)c_rightRef).value);
			}
			else if (c_leftRef instanceof IntegerConstant) {
				pbObject = pb.lt(((IntegerConstant)c_leftRef).value,getExpression(c_rightRef));
			}
			else if (c_rightRef instanceof IntegerConstant) {
				pbObject = pb.lt(getExpression(c_leftRef),((IntegerConstant)c_rightRef).value);
			}
			else
				pbObject = pb.lt(getExpression(c_leftRef),getExpression(c_rightRef));
			break;
		case GE:
			if (c_leftRef instanceof IntegerConstant && c_rightRef instanceof IntegerConstant) {
				pbObject = pb.geq(((IntegerConstant)c_leftRef).value, ((IntegerConstant)c_rightRef).value);
			}
			else if (c_leftRef instanceof IntegerConstant) {
				pbObject = pb.geq(((IntegerConstant)c_leftRef).value,getExpression(c_rightRef));
			}
			else if (c_rightRef instanceof IntegerConstant) {
				pbObject = pb.geq(getExpression(c_leftRef),((IntegerConstant)c_rightRef).value);
			}
			else
				pbObject = pb.geq(getExpression(c_leftRef),getExpression(c_rightRef));
			break;
		case LE:
			if (c_leftRef instanceof IntegerConstant && c_rightRef instanceof IntegerConstant) {
				pbObject = pb.leq(((IntegerConstant)c_leftRef).value, ((IntegerConstant)c_rightRef).value);
			}
			else if (c_leftRef instanceof IntegerConstant) {
				pbObject = pb.leq(((IntegerConstant)c_leftRef).value,getExpression(c_rightRef));
			}
			else if (c_rightRef instanceof IntegerConstant) {
				pbObject = pb.leq(getExpression(c_leftRef),((IntegerConstant)c_rightRef).value);
			}
			else
				pbObject = pb.leq(getExpression(c_leftRef),getExpression(c_rightRef));
			break;
		case GT:
			if (c_leftRef instanceof IntegerConstant && c_rightRef instanceof IntegerConstant) {
				pbObject = pb.gt(((IntegerConstant)c_leftRef).value, ((IntegerConstant)c_rightRef).value);
			}
			else if (c_leftRef instanceof IntegerConstant) {
				pbObject = pb.gt(((IntegerConstant)c_leftRef).value,getExpression(c_rightRef));
			}
			else if (c_rightRef instanceof IntegerConstant) {
				pbObject = pb.gt(getExpression(c_leftRef),((IntegerConstant)c_rightRef).value);
			}
			else
				pbObject = pb.gt(getExpression(c_leftRef),getExpression(c_rightRef));
			break;
		}
		return pbObject;
	}
}