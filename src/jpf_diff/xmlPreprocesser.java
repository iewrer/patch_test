package jpf_diff;

import java.io.File;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.eclipse.compare.internal.CompareAction;
import org.omg.CORBA.INITIALIZE;

import gov.nasa.jpf.regression.ast.ASTLoader;
import gov.nasa.jpf.regression.ast.BlockSummaryReader;
import gov.nasa.jpf.regression.ast.BlockSummaryWriter;
import gov.nasa.jpf.regression.block.jaxbiface.BlockInfo;
import gov.nasa.jpf.regression.block.jaxbiface.BlockSummary;
import gov.nasa.jpf.regression.block.jaxbiface.ClassSummary;
import gov.nasa.jpf.regression.block.jaxbiface.MethodSummary;

class XML {
	BlockSummary bSum;
	String path;
	BlockSummaryReader bReader;
	BlockSummaryWriter bWriter;
	XML(String path) {
		this.path = path;
		initialize();
	}
	private void initialize() {
		// TODO Auto-generated method stub
		bReader = new BlockSummaryReader();
		bSum = bReader.extractBlock(path);
		bWriter = new BlockSummaryWriter();
	}
}


public class xmlPreprocesser {

	public xmlPreprocesser() {
		// TODO Auto-generated constructor stub

	}

	public static void main(String[] args) throws JAXBException {
		// TODO Auto-generated method stub
		String diffPath = "./diffFiles/";
		String diffOther = "./diffChanged/";
		String project = "/jdt/";
		String newPath = diffPath + "new" + project;
		String patchPath = diffPath + "patch" + project;
		
		String newOther = diffOther + "new" + project;
		String patchOther = diffOther + "patch" + project;
		
		xmlPreprocesser process = new xmlPreprocesser();
		
		File otnRoot = new File(newPath);
		File ntpRoot = new File(patchPath);
		
		File[] otns = otnRoot.listFiles();
		for (File otn : otns) {
			File[] ntps = ntpRoot.listFiles();
			for (File ntp : ntps) {
				if (otn.getName().equals(ntp.getName()) && otn.getName().endsWith(".xml")) {
					XML otnXml = new XML(otn.getAbsolutePath());
					XML ntpXml = new XML(ntp.getAbsolutePath());
					process.compareClass(otnXml, ntpXml);
					otnXml.bWriter.write(otnXml.bSum, newOther + otn.getName());
					ntpXml.bWriter.write(ntpXml.bSum, patchOther + ntp.getName());
					break;
				}
			}
		}
		
	}

	private void compareClass(XML otnXml, XML ntpXml) {
		// TODO Auto-generated method stub
		List<ClassSummary> otn_classes = otnXml.bSum.getClassSummary();
		for (int i = 0; i < otn_classes.size(); i++){
			ClassSummary otn_class = otn_classes.get(i);
			List<ClassSummary> ntp_classes = ntpXml.bSum.getClassSummary();
			for (int j = 0; j < ntp_classes.size(); j++) {
				ClassSummary ntp_class = ntp_classes.get(j);
				if (otn_class.getClassName().equals(ntp_class.getClassName())) {
					compareMethod(otnXml.bSum.getClassSummary().get(i), ntpXml.bSum.getClassSummary().get(j));
					break;
				}
			}
		}
	}

	private void compareMethod(ClassSummary otn_class,
			ClassSummary ntp_class) {
		// TODO Auto-generated method stub
		List<MethodSummary> otn_methods = otn_class.getMethodSummary();
		for (int i =0; i < otn_methods.size(); i++) {
			MethodSummary otn = otn_methods.get(i);
			List<MethodSummary> ntp_methods = ntp_class.getMethodSummary();
			for (int j = 0; j < ntp_methods.size(); j++) {
				MethodSummary ntp = ntp_methods.get(j);
				if (otn.getMethodName().equals(ntp.getMethodName())
						&& otn.getOrigMethodParamTypes().equals(ntp.getOrigMethodParamTypes())
						&& otn.getOrigMethodReturnType() != null &&
								otn.getOrigMethodReturnType().equals(ntp.getOrigMethodReturnType())) {
					compareBlcok(otn_class.getMethodSummary().get(i), ntp_class.getMethodSummary().get(j));
					break;
				}
			}
		}
	}

	private void compareBlcok(MethodSummary otn_method, MethodSummary ntp_method) {
		// TODO Auto-generated method stub
		List<BlockInfo> otn_blocks = otn_method.getBlockInfo();
		for (int i = 0; i < otn_blocks.size(); i++) {
			BlockInfo otn = otn_blocks.get(i);
			List<BlockInfo> ntp_blocks = ntp_method.getBlockInfo();
			for (int j = 0; j < ntp_blocks.size(); j++) {
				BlockInfo ntp = ntp_blocks.get(j);
				if (otn.getVersion().equals(ntp.getVersion())
						&& otn.getStart().equals(ntp.getStart())
						&& otn.getEnd().equals(ntp.getEnd())
						&& otn.getChangeType().equals(ntp.getChangeType())) {
					otn.setChangeType("unchanged");
					ntp.setChangeType("unchanged");
					otn_method.getBlockInfo().set(i, otn);
					ntp_method.getBlockInfo().set(j, ntp);
					System.err.println(otn.getStart() + "," + otn.getEnd() + otn_method.getBlockInfo().get(i).getChangeType());
					break;
				}
			}
		}
	}


}
