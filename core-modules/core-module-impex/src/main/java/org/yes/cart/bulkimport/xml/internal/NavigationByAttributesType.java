
package org.yes.cart.bulkimport.xml.internal;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for navigation-by-attributesType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="navigation-by-attributesType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="navigation-product-type" type="{}navigation-product-typeType" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="enabled" type="{http://www.w3.org/2001/XMLSchema}boolean" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "navigation-by-attributesType", propOrder = {
    "navigationProductType"
})
public class NavigationByAttributesType {

    @XmlElement(name = "navigation-product-type")
    protected NavigationProductTypeType navigationProductType;
    @XmlAttribute(name = "enabled")
    protected Boolean enabled;

    /**
     * Gets the value of the navigationProductType property.
     * 
     * @return
     *     possible object is
     *     {@link NavigationProductTypeType }
     *     
     */
    public NavigationProductTypeType getNavigationProductType() {
        return navigationProductType;
    }

    /**
     * Sets the value of the navigationProductType property.
     * 
     * @param value
     *     allowed object is
     *     {@link NavigationProductTypeType }
     *     
     */
    public void setNavigationProductType(NavigationProductTypeType value) {
        this.navigationProductType = value;
    }

    /**
     * Gets the value of the enabled property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isEnabled() {
        return enabled;
    }

    /**
     * Sets the value of the enabled property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setEnabled(Boolean value) {
        this.enabled = value;
    }

}
