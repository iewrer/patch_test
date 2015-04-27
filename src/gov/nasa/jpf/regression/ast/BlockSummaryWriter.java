package gov.nasa.jpf.regression.ast;

import gov.nasa.jpf.regression.block.jaxbiface.BlockSummary;

import java.io.File;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.eclipse.pde.api.tools.ui.internal.markers.CreateApiFilterOperation;

public class BlockSummaryWriter {

	public BlockSummaryWriter() {
		// TODO Auto-generated constructor stub
	}
	public void write(BlockSummary bSum, String path) throws JAXBException {
		JAXBContext jc;
		Marshaller marshaller;
		try{
			//jc = JAXBContext.newInstance("gov.nasa.jpf.regression.block.jaxbiface");
			//the following line is a hack to get around the way JPF does class loading :-(
			ClassLoader cl = gov.nasa.jpf.regression.ast.BlockSummaryReader.class.getClassLoader();
			jc = JAXBContext.newInstance("gov.nasa.jpf.regression.block.jaxbiface",cl);
			marshaller = jc.createMarshaller();
		}catch (JAXBException e){
			throw new RuntimeException("## Error Creating JAXB Context: \n" + e);
		}	
		File file = create(path);
		marshaller.marshal(bSum, file);
	}
	private File create(String path) {
		// TODO Auto-generated method stub
		File file = new File(path);
		if (!file.getParentFile().exists()) {
			file.getParentFile().mkdirs();
		}
		return file;
	}

}
