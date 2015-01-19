#!/bin/sh


cp /Users/barry/code/patch/eclipse.jdt.core/org.eclipse.jdt.core/compiler/org/eclipse/jdt/internal/compiler/Compiler.java /Users/barry/Documents/workspace/patch_test/old/src/jdt/ 
cp ~/code/patch/eclipse.jdt.core/org.eclipse.jdt.core/bin/org/eclipse/jdt/internal/compiler/Compiler.class /Users/barry/Documents/workspace/patch_test/old/bin/jdt/

cp ~/code/patch/archive-2.9.0.xx-20140320-0100-e43-SNAPSHOT/plugins/org.eclipse.jdt.core.source_3.9.2.xx-20140320-0100-e43-SNAPSHOT/org/eclipse/jdt/internal/compiler/Compiler.java	/Users/barry/Documents/workspace/patch_test/src/tests/patch/jdt/
cp ~/code/patch/archive-2.9.0.xx-20140320-0100-e43-SNAPSHOT/plugins/org.eclipse.jdt.core_3.9.2.xx-20140320-0100-e43-SNAPSHOT/org/eclipse/jdt/internal/compiler/Compiler.class ~/Documents/workspace/patch_test/build/tests/patch/jdt/

java -cp tools.jar:astro.jar cse.unl.edu.ast.ASTDiffer -heu -xml -dir ../../../diffFiles/patch/jdt -original ../../../old/src/jdt/Compiler.java -modified ../../../src/tests/patch/jdt/Compiler.java -file Compiler -ocp ../../../code/old/jdt/org.eclipse.jdt.core/bin:code/patch/jdt/bin/org.eclipse.core.runtime-3.1.0.jar:code/patch/jdt/bin/org.eclipse.jface_3.9.1.v20130725-1141.jar:code/patch/jdt/bin/org.eclipse.text_3.5.300.v20130515-1451.jar 	-mcp ../../../code/patch/jdt/bin/org.eclipse.jdt.core_3.9.2.xx-20140320-0100-e43-SNAPSHOT.jar:code/patch/jdt/bin/org.eclipse.core.runtime-3.1.0.jar:code/patch/jdt/bin/org.eclipse.jface_3.9.1.v20130725-1141.jar:code/patch/jdt/bin/org.eclipse.text_3.5.300.v20130515-1451.jar
