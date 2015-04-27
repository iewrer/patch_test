package tree_diff;

import java.io.File;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;

import ch.uzh.ifi.seal.changedistiller.ast.FileUtils;

public class compile {

	public compile() {
		// TODO Auto-generated constructor stub
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		File left = new File("/Users/barry/Documents/workspace/jdt_core/new_path_for_new_version/org.eclipse.jdt.core/org/eclipse/jdt/internal/compiler/Compiler.java");
		ASTParser parser = ASTParser.newParser(AST.JLS3);
	    // Parse the class as a compilation unit.
	    parser.setKind(ASTParser.K_COMPILATION_UNIT);
	    parser.setSource(FileUtils.getContent(left).toCharArray()); // give your java source here as char array
	    parser.setResolveBindings(true);

	    // Return the compiled class as a compilation unit
	    CompilationUnit compilationUnit = (CompilationUnit) parser.createAST(null);
	}

}
