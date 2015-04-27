package jpf_diff;

import java.math.BigInteger;

import gov.nasa.jpf.util.Pair;

public class Dependency {
	public Pair<Integer, Integer> depend;
//	public Pair<Integer, Integer> dependID;
//	public Pair<BigInteger, BigInteger> dependBlock;
	public Dependency(Integer a, Integer b) {
		// TODO Auto-generated constructor stub
		depend = new Pair<Integer, Integer>(a, b);
	}
	@Override
	public int hashCode() {
		// TODO Auto-generated method stub
		//只要a、b相同，则计算出来的hashCode就相同，都为a^b
		return depend.hashCode();
	}
	@Override
	public boolean equals(Object obj) {
		// TODO Auto-generated method stub
	       if (!(obj instanceof Dependency))
	            return false;
	        if (obj == this)
	            return true;
	        Dependency other = (Dependency)obj;
	    //对比两个depends中存储的内容是否一样
		return this.depend.equals(other.depend);
	}
}