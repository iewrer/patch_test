package jpf_diff;

import gov.nasa.jpf.util.Pair;

import java.math.BigInteger;

public class Block {

	public Pair<BigInteger, BigInteger> dependBlock;
	
	public Block(BigInteger a, BigInteger b) {
		dependBlock = new Pair<BigInteger, BigInteger>(a, b);
	}
}

