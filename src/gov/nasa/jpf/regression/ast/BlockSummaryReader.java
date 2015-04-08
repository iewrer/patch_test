package gov.nasa.jpf.regression.ast;

import gov.nasa.jpf.regression.block.jaxbiface.BlockSummary;

import java.io.File;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;


public class BlockSummaryReader {
	
	public BlockSummary extractBlock(String fName){
		JAXBContext jc;
		Unmarshaller unmarshaller;
		
		try{
			//jc = JAXBContext.newInstance("gov.nasa.jpf.regression.block.jaxbiface");
			//the following line is a hack to get around the way JPF does class loading :-(
			ClassLoader cl = gov.nasa.jpf.regression.ast.BlockSummaryReader.class.getClassLoader();
			jc = JAXBContext.newInstance("gov.nasa.jpf.regression.block.jaxbiface",cl);
			unmarshaller = jc.createUnmarshaller();
		}catch (JAXBException e){
			throw new RuntimeException("## Error Creating JAXB Context: \n" + e);
		}
		try{
			BlockSummary bSum = (BlockSummary)unmarshaller.unmarshal(new File(fName));
			return bSum;
		}catch(Exception e){
//			e.printStackTrace();
//			throw new RuntimeException("## Error Unmarshalling XML file: " +
//					fName + "\n" + e);
			System.err.println("## (Error Unmarshalling XML file: " +
				fName + "\n");
			return null;
		}
	}

}
