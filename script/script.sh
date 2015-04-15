!/bin/sh

#script to make the following files ready,after running this script we can directly run the java code to finish the rest work
#1. AST Diff XML file
#2. dot dir
#3. jpf files


#properties that needs to be changed if we want to compute another file
#----------------------------------------

#which file to diff and which kind of it
#!!!
FileName="Compiler"
# FileName="Parser"
# FileName="Scanner"
# FileName="AST"
# FileName="ASTConverter"
# FileName="CompletionEngine"
# FileName="ASTNode"
# FileName="CompilationUnit"
# FileName="ReadManager"

#jpf property
#!!!
Target="org.eclipse.jdt.internal.compiler"
# Target="org.eclipse.jdt.internal.compiler.parser"
# Target="org.eclipse.jdt.core.dom"
# Target="org.eclipse.jdt.internal.codeassist"

#the internal dir of the file
#!!!
Dir="/org/eclipse/jdt/internal/compiler/" 
# Dir="/org/eclipse/jdt/internal/compiler/parser/" 
# Dir="/org/eclipse/jdt/core/dom/" 
# Dir="/org/eclipse/jdt/internal/codeassist/"

#!!!
New_Dir="/compiler"$Dir
# New_Dir="/dom"$Dir
# New_Dir="/codeassist"$Dir

#----------------------------------------

Old_class="/Users/barry/code/patch/org.eclipse.jdt.source-4.3.2/plugins/org.eclipse.jdt.core_3.9.2.v20140114-1555"
New_class="/Users/barry/code/patch/eclipse.jdt.core/org.eclipse.jdt.core/bin"
Patch_class="/Users/barry/code/patch/archive-2.9.0.xx-20140320-0100-e43-SNAPSHOT/plugins/org.eclipse.jdt.core_3.9.2.xx-20140320-0100-e43-SNAPSHOT"

# Old_src="/Users/barry/code/patch/org.eclipse.jdt.source-4.3.2/plugins/org.eclipse.jdt.core.source_3.9.2.v20140114-1555"
# New_src="/Users/barry/code/patch/eclipse.jdt.core/org.eclipse.jdt.core"
# Patch_src="/Users/barry/code/patch/archive-2.9.0.xx-20140320-0100-e43-SNAPSHOT/plugins/org.eclipse.jdt.core.source_3.9.2.xx-20140320-0100-e43-SNAPSHOT"

Old_src="/Users/barry/Documents/workspace/jdt_core/jdt_core_old_version/org.eclipse.jdt.core.source_3.9.2.v20140114-1555"
New_src="/Users/barry/Documents/workspace/jdt_core/new_path_for_new_version/org.eclipse.jdt.core"
Patch_src="/Users/barry/Documents/workspace/jdt_core/jdt_core_patched__from_new_version/org.eclipse.jdt.core.source_3.9.2.xx-20140320-0100-e43-SNAPSHOT"


Src=".java"
Class=".class"
Jar=".jar"
Xml=".xml"

#the internal dir of the file and it's filename
SrcFile=$FileName$Src
ClassFile=$FileName$Class
AST=$FileName$Xml


#the whole dir of these files
old_src=$Old_src$Dir$SrcFile
# old_class=$Old_class$Dir$ClassFile
old_class=$Old_src$Dir$ClassFile

# new_src=$New_src$New_Dir$SrcFile
new_src=$New_src$Dir$SrcFile
# new_class=$New_class$Dir$ClassFile
new_class=$New_src$Dir$ClassFile

patch_src=$Patch_src$Dir$SrcFile
# patch_class=$Patch_class$Dir$ClassFile
patch_class=$Patch_src$Dir$ClassFile

#the classpath which will be used when AST differ runs
old_jar=$Old_class$Jar
patch_jar=$Patch_class$Jar
Runtime="../code/patch/jdt/bin/org.eclipse.core.runtime-3.1.0.jar:../code/patch/jdt/bin/org.eclipse.jface_3.9.1.v20130725-1141.jar:../code/patch/jdt/bin/org.eclipse.text_3.5.300.v20130515-1451.jar"

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


diff_nAso=" -original $new_src"
diff_pAsm=" -modified $patch_src"
diff_nAsocp=" -ocp $New_class:$Runtime"
diff_pAsmcp=" -mcp $patch_jar:$Runtime"

# diff_pAso=" -original $patch_src"
# diff_oAsm=" -modified $old_src"
# diff_pAsocp=" -ocp $patch_jar:$Runtime"
# diff_oAsmcp=" -mcp $old_jar:$Runtime"

diff_mAso=" -original $new_src"
diff_oAsm=" -modified $old_src"
diff_mAsocp=" -ocp $New_class:$Runtime"
diff_oAsmcp=" -mcp $old_jar:$Runtime"


#compose the diff command
#diff for (old,new)
# Diff_new=$diff_tool$diff_dir_new$diff_original$diff_new$diff_file$diff_ocp$diff_mcp_new
#diff for (new,old)
Diff_new=$diff_tool$diff_dir_new$diff_mAso$diff_oAsm$diff_file$diff_mAsocp$diff_oAsmcp
#diff for (new,patch)
Diff_patch=$diff_tool$diff_dir_patch$diff_nAso$diff_pAsm$diff_file$diff_nAsocp$diff_pAsmcp
#diff for (old,patch)
# Diff_patch=$diff_tool$diff_dir_patch$diff_pAso$diff_oAsm$diff_file$diff_pAsocp$diff_oAsmcp


echo $old_src
echo $patch_src
echo $new_src
# echo $patch_class

#eval AST Differ
# echo $Diff_new
eval $Diff_new
eval $Diff_patch

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

# NewClassPath="classpath = $New_class"
NewClassPath="classpath = $New_src"
# NewSourcePath="sourcepath = $New_src"
NewSourcePath="sourcepath = $New_src"
# NewClass="rse.newClass = $new_class"
NewClass="rse.newClass = $new_class"
NewSrc="rse.newSrc = $new_src"

NewDot="rse.dotFile = ./dotFiles/new/jdt/$FileName/$FileName"
# NewDot="rse.dotFile = ./dotFiles/new/jdt/$FileName"
NewAST="rse.ASTResults = ./diffFiles/new/jdt/$AST"
NewJPF=$FileName$newjpf

# PatchClassPath="classpath = $Patch_class"
PatchClassPath="classpath = $New_src"
PatchSourcePath="sourcepath = $New_src"

PatchClass="rse.newClass = $new_class"
PatchSrc="rse.newSrc = $new_src"

PatchDot="rse.dotFile = ./dotFiles/patch/jdt/$FileName/$FileName"
# PatchDot="rse.dotFile = ./dotFiles/patch/jdt/$FileName"
PatchAST="rse.ASTResults = ./diffFiles/patch/jdt/$AST"
PatchJPF=$FileName$patchjpf

# pAsoClassPath="classpath = $Old_class"
pAsoClassPath="classpath = $Old_src"
pAsoSourcePath="sourcepath = $Old_src"
pAsoClass="rse.newClass = $old_class"
oAsmClass="rse.oldClass = $patch_class"

# OLdClass="rse.oldClass = $old_class"
NOLdClass="rse.oldClass = $old_class"
NOldSrc="rse.oldSrc = $old_src"
POLdClass="rse.oldClass = $patch_class"
POldSrc="rse.oldSrc = $patch_src"

#make a dir for jpf
mkdir $FileName

#output new jpf
echo $target > ./$FileName/$NewJPF
echo $NewClassPath >> ./$FileName/$NewJPF
echo $NewSourcePath >> ./$FileName/$NewJPF
echo $NewClass >> ./$FileName/$NewJPF
echo $NewSrc >> ./$FileName/$NewJPF
# echo $OLdClass >> ./$FileName/$NewJPF
echo $NOLdClass >> ./$FileName/$NewJPF
echo $NOLdSrc >> ./$FileName/$NewJPF
echo $NewDot >> ./$FileName/$NewJPF
echo $NewAST >> ./$FileName/$NewJPF

#output patch jpf
echo $target > ./$FileName/$PatchJPF
echo $PatchClassPath >> ./$FileName/$PatchJPF
echo $PatchSourcePath >> ./$FileName/$PatchJPF
echo $PatchClass >> ./$FileName/$PatchJPF
echo $PatchSrc >> ./$FileName/$PatchJPF
# echo $OLdClass >> ./$FileName/$PatchJPF
echo $POLdClass >> ./$FileName/$PatchJPF
echo $POLdSrc >> ./$FileName/$PatchJPF


# echo $pAsoClassPath >> ./$FileName/$PatchJPF
# echo $pAsoSourcePath >> ./$FileName/$PatchJPF
# echo $pAsoClass >> ./$FileName/$PatchJPF
# echo $oAsmClass >> ./$FileName/$PatchJPF



echo $PatchDot >> ./$FileName/$PatchJPF
echo $PatchAST >> ./$FileName/$PatchJPF



