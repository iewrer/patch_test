package gov.nasa.jpf.regression.analysis;

import gov.nasa.jpf.regression.cfg.CFG;
import gov.nasa.jpf.regression.cfg.Node;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

public class SCC {
	
	public enum Loop {
	    TARGET, NEXT, 
	    TARGET_AND_NEXT,
	    NONE;
	}

	int index = 0;
	Stack<Node> S;
	Set<Integer>allSCC = new HashSet<Integer>();
	CFG cfg; 
	

	public SCC(CFG cfg) {
		this.cfg = cfg;
	}

	public void stronglyConnectedComponents() {
		S = new Stack<Node>();
		Node[] nodes = cfg.getNodes();
		for(int nodeIndex = 0; nodeIndex < nodes.length; nodeIndex++) {
			Node n = nodes[nodeIndex];
			if(n.index == -1) {
				tarjan(n);
			}
		}
	}

	private void tarjan(Node n) {
		n.index = index;
		n.lowlink = index;
		index = index + 1;
		S.push(n);
		Node nPrime;
		List<Node> succ = n.getSuccessorList();
		for(int succIndex = 0; succIndex < succ.size(); succIndex++) {
			nPrime = succ.get(succIndex);
			if(nPrime.index == -1) {
				tarjan(nPrime);
				n.lowlink = Math.min(n.lowlink, nPrime.lowlink);
			} else if (S.contains(nPrime)) {
				n.lowlink = Math.min(n.lowlink, nPrime.index);
			}
		}
		if(n.lowlink == n.index) {
			//System.out.print("SCC: ");
			Set<Integer> SCC = new HashSet<Integer>();
			do  {
				nPrime = S.pop();
				//System.out.print(nPrime.startOffset + " :");
				SCC.add(nPrime.getStartOffset());
			} while (n != nPrime);
			//System.out.println();
			if(SCC.size() > 1) {
				allSCC.addAll(SCC);
			}
		}

	}

}

/*****
 * This is the pseudocode for Tarjan's algorithm to detect
 * Strongly connected componenets. This pseudocode has been
 * taken from wikipedia
 *
	Input: Graph G = (V, E)

	index = 0                                         // DFS node number counter
	S = empty                                         // An empty stack of node
	for all v in V do
		if (v.index is undefined)                       // Start a DFS at each node
			tarjan(v)                                     // we haven't visited yet

	procedure tarjan(v)
	v.index = index                                 // Set the depth index for v
	v.lowlink = index
	index = index + 1
	S.push(v)                                       // Push v on the stack
	for all (v, v') in E do                         // Consider successors of v
		if (v'.index is undefined)                    // Was successor v' visited?
			tarjan(v')                                // Recurse
			v.lowlink = min(v.lowlink, v'.lowlink)
		else if (v' is in S)                          // Was successor v' in stack S?
			v.lowlink = min(v.lowlink, v'.index )     // v' is in stack but it isn't in the dfs tree
	if (v.lowlink == v.index)                       // Is v the root of an SCC?
		print "SCC:"
		repeat
			v' = S.pop
			print v'
		until (v' == v)
****/
