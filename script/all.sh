#!/bin/sh

#script to make the following files ready,after running this script we can directly run the java code to finish the rest work
#1. AST Diff XML file
#2. Dot dir
#3. Config file

Now="."

Old_src="../data/src/jdt_core_old_version/org.eclipse.jdt.core.source_3.9.2.v20140114-1555"
New_src="../data/src/new_path_for_new_version/org.eclipse.jdt.core"
Patch_src="../data//src/jdt_core_patched__from_new_version/org.eclipse.jdt.core.source_3.9.2.xx-20140320-0100-e43-SNAPSHOT"

Old_class=$(cd "../data/bin/old";pwd)
New_class=$(cd "../data/bin/new";pwd)
Patch_class=$(cd "../data/bin/patch";pwd)

Src=".java"
Class=".class"
Jar=".jar"
Xml=".xml"

function getName() {
	for file in $(find $1 -name '*.java' -print);do
		# Patch_src_ab=$(cd $Patch_src;pwd)
		# echo $Patch_src_ab
		Patch_src_len=${#Patch_src}
		filename=$(basename $file)
		# echo $filename
		filename_len=${#filename}
		file_len=${#file}
		# echo $file
		dir_len=$(expr $file_len - $Patch_src_len - $filename_len)
		

		name=$(basename $file .java)
		# echo $name >> "./filename.txt"  
		dir=${file:$Patch_src_len - 1:dir_len + 1}  
	    # echo $dir

	    FileName=$name

	    if [ $FileName != "Compiler" ]
	    then
	    	continue
	    fi

	    Target="org.eclipse.jdt"
	    Dir=$dir 


	    #the internal dir of the file and it's filename
		SrcFile=$FileName$Src
		ClassFile=$FileName$Class
		AST=$FileName$Xml


		#the whole dir of these files
		old_src=$Old_src$Dir$SrcFile
		old_class=$Old_class$Dir$ClassFile
		# old_class=$Old_src$Dir$ClassFile

		# new_src=$New_src$New_Dir$SrcFile
		new_src=$New_src$Dir$SrcFile
		new_class=$New_class$Dir$ClassFile
		# new_class=$New_src$Dir$ClassFile

		patch_src=$Patch_src$Dir$SrcFile
		patch_class=$Patch_class$Dir$ClassFile
		# patch_class=$Patch_src$Dir$ClassFile

		#the classpath which will be used when AST differ runs
		# old_jar=$Old_class$Jar
		# patch_jar=$Patch_class$Jar
		old_jar=$Old_class
		patch_jar=$Patch_class
		Runtime="../lib/org.eclipse.core.runtime-3.1.0.jar:../lib/org.eclipse.jface_3.9.1.v20130725-1141.jar:../lib/org.eclipse.text_3.5.300.v20130515-1451.jar:../lib/org.eclipse.core.resources_3.8.101.v20130717-0806.jar"


		#the AST diff eval command
		diff_tool="java -cp tools.jar:astro.jar cse.unl.edu.ast.ASTDiffer -heu -xml"
		diff_dir_new=" -dir ../diffFiles/new/jdt"
		diff_dir_patch=" -dir ../diffFiles/patch/jdt"
		diff_file=" -file $FileName"

		diff_original=" -original $old_src"
		diff_new=" -modified $new_src"
		diff_patch=" -modified $patch_src"

		diff_ocp=" -ocp $old_jar:$Runtime"
		diff_mcp_new=" -mcp $New_class:$Runtime"
		diff_mcp_patch=" -mcp $patch_jar:$Runtime"


		#changed begin,at 4.13,swap original and modified
		diff_nAso=" -original $patch_src"
		diff_pAsm=" -modified $new_src"
		diff_nAsocp=" -ocp $patch_jar:$Runtime"
		diff_pAsmcp=" -mcp $New_class:$Runtime"
		

		diff_mAso=" -original $old_src"
		diff_oAsm=" -modified $new_src"
		diff_mAsocp=" -ocp $old_jar:$Runtime"
		diff_oAsmcp=" -mcp $New_class:$Runtime"
		#changed over


		#compose the diff command

		#diff for (new,old)
		Diff_new=$diff_tool$diff_dir_new$diff_mAso$diff_oAsm$diff_file$diff_mAsocp$diff_oAsmcp
		#diff for (new,patch)
		Diff_patch=$diff_tool$diff_dir_patch$diff_nAso$diff_pAsm$diff_file$diff_nAsocp$diff_pAsmcp

		echo "------------------------src path-----------------------------" >> "log.txt"
		echo "old_src: "$old_src >> "log.txt"
		echo "patch_src: "$patch_src >> "log.txt" 2>&1
		echo "new_src: "$new_src >> "log.txt"
		echo "old_class: "$old_class >> "log.txt"
		echo "patch_class: "$patch_class >> "log.txt" 2>&1
		echo "new_class: "$new_class >> "log.txt"

		#eval AST Differ
		echo "--------------------running Diff new-------------------------" >> "log.txt"
		eval $Diff_new >> "log.txt" 2>&1
		echo "--------------------Diff new done----------------------------" >> "log.txt"
		echo "--------------------running Diff patch-----------------------" >> "log.txt"
		eval $Diff_patch >> "log.txt" 2>&1
		echo "--------------------Diff patch done--------------------------" >> "log.txt"

		#make dir to store dot files for this file
		cd ../dotFiles/new/jdt/
		mkdir $FileName
		cd ../../patch/jdt
		mkdir $FileName
		cd ../../../script/

		#jpf property set
		target="target = "$Target
		newjpf="_new.jpf"
		patchjpf="_patch.jpf"

		NewClassPath="classpath = $New_class"
		NewSourcePath="sourcepath = $New_src"
		NewClass="rse.newClass = $new_class"
		NewSrc="rse.newSrc = $new_src"

		NewDot="rse.dotFile = ./dotFiles/new/jdt/$FileName/$FileName"
		NewAST="rse.ASTResults = ./diffFiles/new/jdt/$AST"
		NewJPF=$FileName$newjpf

		PatchClassPath="classpath = $New_class"
		PatchSourcePath="sourcepath = $New_src"

		PatchClass="rse.newClass = $new_class"
		PatchSrc="rse.newSrc = $new_src"

		PatchDot="rse.dotFile = ./dotFiles/patch/jdt/$FileName/$FileName"
		PatchAST="rse.ASTResults = ./diffFiles/patch/jdt/$AST"
		PatchJPF=$FileName$patchjpf

		pAsoClassPath="classpath = $Old_class"
		pAsoSourcePath="sourcepath = $Old_src"
		pAsoClass="rse.newClass = $old_class"
		oAsmClass="rse.oldClass = $patch_class"

		NOLdClass="rse.oldClass = $old_class"
		NOldSrc="rse.oldSrc = $old_src"
		POLdClass="rse.oldClass = $patch_class"
		POldSrc="rse.oldSrc = $patch_src"

		#make a dir for jpf
		mkdir $FileName



		echo "--------------------make jpf new-----------------------------" >> "log.txt"
		#output new jpf
		echo $target > ./$FileName/$NewJPF
		echo $NewClassPath >> ./$FileName/$NewJPF
		echo $NewSourcePath >> ./$FileName/$NewJPF
		echo $NewClass >> ./$FileName/$NewJPF
		echo $NewSrc >> ./$FileName/$NewJPF
		echo $NOLdClass >> ./$FileName/$NewJPF
		echo $NOLdSrc >> ./$FileName/$NewJPF
		echo $NewDot >> ./$FileName/$NewJPF
		echo $NewAST >> ./$FileName/$NewJPF

		echo "--------------------jpf new done-----------------------------" >> "log.txt"

		#output patch jpf
		echo "--------------------make jpf patch---------------------------" >> "log.txt"
		
		echo $target > ./$FileName/$PatchJPF
		echo $PatchClassPath >> ./$FileName/$PatchJPF
		echo $PatchSourcePath >> ./$FileName/$PatchJPF
		echo $PatchClass >> ./$FileName/$PatchJPF
		echo $PatchSrc >> ./$FileName/$PatchJPF
		echo $POLdClass >> ./$FileName/$PatchJPF
		echo $POLdSrc >> ./$FileName/$PatchJPF

		echo $PatchDot >> ./$FileName/$PatchJPF
		echo $PatchAST >> ./$FileName/$PatchJPF

		echo "--------------------jpf patch done---------------------------" >> "log.txt"

	done
}

INIT_PATH="../data/src/jdt_core_patched__from_new_version/org.eclipse.jdt.core.source_3.9.2.xx-20140320-0100-e43-SNAPSHOT";
getName $INIT_PATH