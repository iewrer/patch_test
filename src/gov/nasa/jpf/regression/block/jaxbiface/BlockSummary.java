//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.4 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2013.03.27 at 03:04:18 PM EDT 
//


package gov.nasa.jpf.regression.block.jaxbiface;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for anonymous complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="originalFile" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="modifiedFile" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element ref="{}classSummary" maxOccurs="unbounded"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "originalFile",
    "modifiedFile",
    "classSummary"
})
@XmlRootElement(name = "blockSummary")
public class BlockSummary {

    @XmlElement(required = true)
    protected String originalFile;
    @XmlElement(required = true)
    protected String modifiedFile;
    @XmlElement(required = true)
    protected List<ClassSummary> classSummary;

    /**
     * Gets the value of the originalFile property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getOriginalFile() {
        return originalFile;
    }

    /**
     * Sets the value of the originalFile property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setOriginalFile(String value) {
        this.originalFile = value;
    }

    /**
     * Gets the value of the modifiedFile property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getModifiedFile() {
        return modifiedFile;
    }

    /**
     * Sets the value of the modifiedFile property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setModifiedFile(String value) {
        this.modifiedFile = value;
    }

    /**
     * Gets the value of the classSummary property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the classSummary property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getClassSummary().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link ClassSummary }
     * 
     * 
     */
    public List<ClassSummary> getClassSummary() {
        if (classSummary == null) {
            classSummary = new ArrayList<ClassSummary>();
        }
        return this.classSummary;
    }

}
