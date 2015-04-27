package tree_diff;

import java.awt.print.Printable;
import java.io.File;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.internal.compiler.ast.ASTNode;

import ch.uzh.ifi.seal.changedistiller.ChangeDistiller;
import ch.uzh.ifi.seal.changedistiller.ChangeDistiller.Language;
import ch.uzh.ifi.seal.changedistiller.ast.ASTHelper;
import ch.uzh.ifi.seal.changedistiller.ast.ASTHelperFactory;
import ch.uzh.ifi.seal.changedistiller.ast.FileUtils;
import ch.uzh.ifi.seal.changedistiller.ast.java.JavaASTHelper;
import ch.uzh.ifi.seal.changedistiller.ast.java.JavaCompilation;
import ch.uzh.ifi.seal.changedistiller.distilling.FileDistiller;
import ch.uzh.ifi.seal.changedistiller.model.entities.SourceCodeChange;
import ch.uzh.ifi.seal.changedistiller.structuredifferencing.StructureNode;
import ch.uzh.ifi.seal.changedistiller.structuredifferencing.java.JavaStructureNode;

public class diff_patch {
	public static void main(String[] args) {
		// TODO Auto-generated method stub
//		File left =  new File("test_v1.java");
//		File right = new File("test_v2.java");
		File left = new File("/Users/barry/Documents/workspace/jdt_core/new_path_for_new_version/org.eclipse.jdt.core/org/eclipse/jdt/internal/compiler/Compiler.java");
		File right = new File("/Users/barry/Documents/workspace/jdt_core/jdt_core_old_version/org.eclipse.jdt.core.source_3.9.2.v20140114-1555/org/eclipse/jdt/internal/compiler/Compiler.java");
		
		FileDistiller distiller = ChangeDistiller.createFileDistiller(Language.JAVA);
//		System.out.println("wait1");
		distiller.extractClassifiedSourceCodeChanges(left, right);
//		System.out.println("wait2");
		List<SourceCodeChange> changes = distiller.getSourceCodeChanges();
//		ASTHelper<JavaStructureNode> fHelper = distiller.getASTHelperFactory().create(left, "default");
//		JavaStructureNode node = fHelper.createStructureTree();
//		ASTNode root = node.getASTNode();
//		System.out.println(root.sourceStart() + "," + root.sourceEnd());

		if (!changes.isEmpty()) {
		    for(SourceCodeChange change : changes) {
//		    	System.out.println("wait");
		    	System.out.println(change.toString() + " " + change.getChangedEntity().getStartPosition() );
		    	System.out.println(change.getParentEntity().toString() + " " +change.getParentEntity().getSourceRange().toString());
		    	
//		        System.out.println(change.getSignificanceLevel().toString() + " " + change.getChangeType().toString() + " " + change.toString());
		    }			
		}
		else {
			System.out.println("nothing changed!");
		}
	}

//	private static void print(JavaStructureNode root) {
//		// TODO Auto-generated method stub
//		if (root == null) {
//			return;
//		}
//		for(JavaStructureNode node:root.getChildren()) {
//			System.out.println(root.getASTNode().toString() + "--->" + node.getASTNode().toString());
//			print(node);
//		}
//	}

}
