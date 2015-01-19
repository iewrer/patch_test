#!/bin/sh

java -cp tools.jar:astro.jar cse.unl.edu.ast.ASTDiffer -heu -xml 
	-dir diffFiles/patch/jdt 
	-original code/old/jdt/org.eclipse.jdt.core/dom/org/eclipse/jdt/core/dom/Block.java 
	-modified code/patch/jdt/org.eclipse.jdt.core.source_3.9.2.xx-20140320-0100-e43-SNAPSHOT/org/eclipse/jdt/core/dom/Block.java 
	-file Block
	-ocp code/old/jdt/org.eclipse.jdt.core/bin:code/patch/jdt/bin/org.eclipse.core.runtime-3.1.0.jar:code/patch/jdt/bin/org.eclipse.jface_3.9.1.v20130725-1141.jar:code/patch/jdt/bin/org.eclipse.text_3.5.300.v20130515-1451.jar 
	-mcp code/patch/jdt/bin/org.eclipse.jdt.core_3.9.2.xx-20140320-0100-e43-SNAPSHOT.jar:code/patch/jdt/bin/org.eclipse.core.runtime-3.1.0.jar:code/patch/jdt/bin/org.eclipse.jface_3.9.1.v20130725-1141.jar:code/patch/jdt/bin/org.eclipse.text_3.5.300.v20130515-1451.jar

