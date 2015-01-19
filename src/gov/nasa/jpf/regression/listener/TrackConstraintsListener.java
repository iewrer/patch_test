package gov.nasa.jpf.regression.listener;

import gov.nasa.jpf.symbc.ConstraintAdapter;
import gov.nasa.jpf.symbc.numeric.Constraint;

public class TrackConstraintsListener extends ConstraintAdapter{

	static String conStr = "";
	static Constraint constraint = null;

	@Override
	public void constraintAdded(Constraint c) {
		//System.out.println("the constriant is " + c.toString());
		//conStr = c.toString();
		conStr = c.toSExpression();
		constraint = c;
	}
	
	public static Constraint getLastConstraint() {
		return constraint;
	}

	public static boolean resetConstraintStr() {
		conStr = "";
		return true;
	}

	public static String getConstraintStr() {
		return conStr;
	}

}