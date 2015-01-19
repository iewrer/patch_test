package gov.nasa.jpf.regression.analysis;

import java.util.HashSet;
import java.util.Set;

public class AffectedNodes {

	protected Set<Integer> affectedCondN;
	protected Set<Integer> affectedWriteN;
	protected Set<Integer> affectedCallN;
	protected Set<Integer> affectedReturnN;
	protected Set<Integer> canbeDropped;
	//protected AffectedGlobals affectedGlobals;

	public AffectedNodes() {
		affectedCondN = new HashSet<Integer>();
		affectedWriteN = new HashSet<Integer>();
		affectedCallN = new HashSet<Integer>();
		affectedReturnN = new HashSet<Integer>();
		canbeDropped = new HashSet<Integer>();
		//affectedGlobals = new AffectedGlobals();
	}

	public void addNodes(Set<Integer> affectedCondN,
			Set<Integer> affectedWriteN, Set<Integer> affectedCallN,
			Set<Integer> affectedReturnN, Set<Integer> canbeDropped) {
		this.affectedCondN.addAll(affectedCondN);
		this.affectedWriteN.addAll(affectedWriteN);
		this.affectedCallN.addAll(affectedCallN);
		this.affectedReturnN.addAll(affectedReturnN);
		this.canbeDropped.addAll(canbeDropped);
	}

	public Set<Integer> getAffectedCondNodes() {
		return this.affectedCondN;
	}

	public Set<Integer> getAffectedWriteNodes() {
		return this.affectedWriteN;
	}

	public Set<Integer> getAffectedCallNodes() {
		return this.affectedCallN;
	}

	public Set<Integer> getAffectedReturnNodes() {
		return this.affectedReturnN;
	}

	public Set<Integer> getCanbeDropped() {
		return this.canbeDropped;
	}

	//public AffectedGlobals getAffectedGlobals() {
	//	return this.affectedGlobals;
	//}
	
	
	
	public void addAllAffectedNodes(AffectedNodes other) {
		this.affectedCondN.addAll(other.affectedCondN);
		this.affectedWriteN.addAll(other.affectedWriteN);
		this.affectedCallN.addAll(other.affectedCallN);
		this.affectedReturnN.addAll(other.affectedReturnN);
	}

	public void addCanBeDropped(Set<Integer> dropped) {
		this.canbeDropped.addAll(dropped);
	}

	public boolean isEmpty() {
		if(this.affectedCallN.size() > 0) return false;
		if(this.affectedCondN.size() > 0) return false;
		if(this.affectedReturnN.size() > 0) return false;
		if(this.affectedWriteN.size() > 0) return false;
		return true;
	}

	public String toString() {
		String ret = "";
		ret = ret.concat("ACN:" + affectedCondN.toString() + "\t");
		ret = ret.concat("AWN:" + affectedWriteN.toString() + "\t");
		ret = ret.concat("ASN:" + affectedCallN.toString() + "\t");
		ret = ret.concat("ARN:" + affectedReturnN.toString() + "\t");
		ret = ret.concat("drop: " + canbeDropped.toString() + "\n");
		//ret = ret.concat(this.affectedGlobals.toString()+"\n");
		return ret;
	}

}