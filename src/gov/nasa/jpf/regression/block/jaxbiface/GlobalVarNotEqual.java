//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.4 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2013.03.27 at 03:04:18 PM EDT 
//


package gov.nasa.jpf.regression.block.jaxbiface;

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
 *         &lt;element name="name" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="originalModifiers" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="modifiedModifiers" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="originalType" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="modifiedType" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="originalInitialization" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="modifiedInitialization" type="{http://www.w3.org/2001/XMLSchema}string"/>
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
    "name",
    "originalModifiers",
    "modifiedModifiers",
    "originalType",
    "modifiedType",
    "originalInitialization",
    "modifiedInitialization"
})
@XmlRootElement(name = "globalVarNotEqual")
public class GlobalVarNotEqual {

    @XmlElement(required = true)
    protected String name;
    @XmlElement(required = true)
    protected String originalModifiers;
    @XmlElement(required = true)
    protected String modifiedModifiers;
    @XmlElement(required = true)
    protected String originalType;
    @XmlElement(required = true)
    protected String modifiedType;
    @XmlElement(required = true)
    protected String originalInitialization;
    @XmlElement(required = true)
    protected String modifiedInitialization;

    /**
     * Gets the value of the name property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the value of the name property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setName(String value) {
        this.name = value;
    }

    /**
     * Gets the value of the originalModifiers property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getOriginalModifiers() {
        return originalModifiers;
    }

    /**
     * Sets the value of the originalModifiers property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setOriginalModifiers(String value) {
        this.originalModifiers = value;
    }

    /**
     * Gets the value of the modifiedModifiers property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getModifiedModifiers() {
        return modifiedModifiers;
    }

    /**
     * Sets the value of the modifiedModifiers property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setModifiedModifiers(String value) {
        this.modifiedModifiers = value;
    }

    /**
     * Gets the value of the originalType property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getOriginalType() {
        return originalType;
    }

    /**
     * Sets the value of the originalType property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setOriginalType(String value) {
        this.originalType = value;
    }

    /**
     * Gets the value of the modifiedType property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getModifiedType() {
        return modifiedType;
    }

    /**
     * Sets the value of the modifiedType property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setModifiedType(String value) {
        this.modifiedType = value;
    }

    /**
     * Gets the value of the originalInitialization property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getOriginalInitialization() {
        return originalInitialization;
    }

    /**
     * Sets the value of the originalInitialization property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setOriginalInitialization(String value) {
        this.originalInitialization = value;
    }

    /**
     * Gets the value of the modifiedInitialization property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getModifiedInitialization() {
        return modifiedInitialization;
    }

    /**
     * Sets the value of the modifiedInitialization property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setModifiedInitialization(String value) {
        this.modifiedInitialization = value;
    }

}
