//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.4 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2013.03.27 at 03:04:18 PM EDT 
//


package gov.nasa.jpf.regression.block.jaxbiface;

import java.math.BigInteger;
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
 *         &lt;element name="originalIndex" type="{http://www.w3.org/2001/XMLSchema}integer"/>
 *         &lt;element name="modifiedIndex" type="{http://www.w3.org/2001/XMLSchema}integer"/>
 *         &lt;element name="static" type="{http://www.w3.org/2001/XMLSchema}boolean"/>
 *         &lt;element name="matched" type="{http://www.w3.org/2001/XMLSchema}boolean"/>
 *         &lt;element name="equivalent" type="{http://www.w3.org/2001/XMLSchema}boolean"/>
 *         &lt;element ref="{}blockInfo" maxOccurs="unbounded"/>
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
    "originalIndex",
    "modifiedIndex",
    "_static",
    "matched",
    "equivalent",
    "blockInfo"
})
@XmlRootElement(name = "initializationBlockSummary")
public class InitializationBlockSummary {

    @XmlElement(required = true)
    protected BigInteger originalIndex;
    @XmlElement(required = true)
    protected BigInteger modifiedIndex;
    @XmlElement(name = "static")
    protected boolean _static;
    protected boolean matched;
    protected boolean equivalent;
    @XmlElement(required = true)
    protected List<BlockInfo> blockInfo;

    /**
     * Gets the value of the originalIndex property.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getOriginalIndex() {
        return originalIndex;
    }

    /**
     * Sets the value of the originalIndex property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setOriginalIndex(BigInteger value) {
        this.originalIndex = value;
    }

    /**
     * Gets the value of the modifiedIndex property.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getModifiedIndex() {
        return modifiedIndex;
    }

    /**
     * Sets the value of the modifiedIndex property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setModifiedIndex(BigInteger value) {
        this.modifiedIndex = value;
    }

    /**
     * Gets the value of the static property.
     * 
     */
    public boolean isStatic() {
        return _static;
    }

    /**
     * Sets the value of the static property.
     * 
     */
    public void setStatic(boolean value) {
        this._static = value;
    }

    /**
     * Gets the value of the matched property.
     * 
     */
    public boolean isMatched() {
        return matched;
    }

    /**
     * Sets the value of the matched property.
     * 
     */
    public void setMatched(boolean value) {
        this.matched = value;
    }

    /**
     * Gets the value of the equivalent property.
     * 
     */
    public boolean isEquivalent() {
        return equivalent;
    }

    /**
     * Sets the value of the equivalent property.
     * 
     */
    public void setEquivalent(boolean value) {
        this.equivalent = value;
    }

    /**
     * Gets the value of the blockInfo property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the blockInfo property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getBlockInfo().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link BlockInfo }
     * 
     * 
     */
    public List<BlockInfo> getBlockInfo() {
        if (blockInfo == null) {
            blockInfo = new ArrayList<BlockInfo>();
        }
        return this.blockInfo;
    }

}
