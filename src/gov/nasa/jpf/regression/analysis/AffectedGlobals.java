package gov.nasa.jpf.regression.analysis;

import java.util.HashSet;
import java.util.Set;

import org.apache.bcel.classfile.Field;

public class AffectedGlobals {

	protected Set<Field> allGlobals;
	protected Set<Field> affectedGlobals;
	protected Set<Integer> affectedReferences;
	
	protected Set<String> allFieldNames;
	protected Set<String> affectedFieldNames;
	protected Set<Integer> affectedFieldReferences;
	
	public AffectedGlobals() {
		
		this.allGlobals = new HashSet<Field>();
		this.affectedGlobals = new HashSet<Field>();
		this.affectedReferences = new HashSet<Integer>();
		
		this.allFieldNames = new HashSet<String>();
		this.affectedFieldNames = new HashSet<String>();
		this.affectedFieldReferences = new HashSet<Integer>();
	}
	
	/**
	 * Constructor for initializing the allGlobals field of this
	 * object with a given array of Fields.
	 * @param fields
	 */
	public AffectedGlobals(Field[] fields) {
		this.allGlobals = new HashSet<Field>();
		this.affectedGlobals = new HashSet<Field>();
		this.affectedReferences = new HashSet<Integer>();
		
		// Initializes the allGlobals with the given array of fields
		for(Field f : fields) {
			this.allGlobals.add(f);
		}
	}
	
	/**
	 * addAffectedGlobal - allows for adding a global to the set of
	 * affected global variables. Returns true if it is successfully
	 * added to the set, returns false otherwise.
	 * @param f The Field to be added.
	 * @return boolean
	 */
	public boolean addAffectedGlobal(Field f) {
		
		return this.affectedGlobals.add(f);
	}
	
	/**
	 * removeAffectedGlobal - allows for the removal of a global from
	 * the set of affected global variables. Returns true if it is
	 * successfully added to the set, returns false otherwise.
	 * @param f The Field to be removed.
	 * @return boolean
	 */
	public boolean removeAffectedGlobal(Field f) {
		
		return this.affectedGlobals.remove(f);
	}
	
	/**
	 * addAffectedReference - adds the Integer that represents an
	 * affected reference to AffectedReferences set.
	 * @param r
	 * 		The Integer that represents the affected reference.
	 * @return boolean
	 */
	public boolean addAffectedReference(Integer r) {
		return this.affectedReferences.add(r);
	}
	
	/**
	 * removeAffectedReference - removes the Integer that represents
	 * an affected reference from AffectedReferences set.
	 * @param r
	 * 		The Integer that represents the affected reference.
	 * @return boolean
	 */
	public boolean removeAffectedReference(Integer r) {
		return this.affectedReferences.remove(r);
	}

	public Set<Field> getAllGlobals() {
		return allGlobals;
	}

	public void setAllGlobals(Set<Field> allGlobals) {
		this.allGlobals = allGlobals;
	}

	public Set<Field> getAffectedGlobals() {
		return affectedGlobals;
	}
	
	public Set<String> getAllFieldNames() {
		return this.allFieldNames;
	}
	
	public Set<String> getAffectedFieldNames() {
		return this.affectedFieldNames;
	}
	
	public Set<Integer> getAffectedFieldReferences() {
		return this.affectedFieldReferences;
	}

	public void setAffectedGlobals(Set<Field> affectedGlobals) {
		this.affectedGlobals = affectedGlobals;
	}
	
	public Set<Integer> getAffectedReferences() {
		return affectedReferences;
	}

	public void setAffectedReferences(Set<Integer> affectedReferences) {
		this.affectedReferences = affectedReferences;
	}
	
	public String toString() {
		
		String str = "";
		
		str += "All Globals: " + this.allGlobals.toString();
		str += "Affected Globals: " + this.affectedGlobals.toString();
		str += "Affected References " + this.affectedReferences.toString();
		
		return str;
	}
}
