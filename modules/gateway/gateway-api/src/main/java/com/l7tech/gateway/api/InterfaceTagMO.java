package com.l7tech.gateway.api;

import com.l7tech.gateway.api.impl.AccessorSupport;
import com.l7tech.gateway.api.impl.AttributeExtensibleType;
import static com.l7tech.gateway.api.impl.AttributeExtensibleType.set;
import com.l7tech.gateway.api.impl.ElementExtendableAccessibleObject;
import com.l7tech.gateway.api.impl.PropertiesMapType;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.l7tech.gateway.api.impl.AttributeExtensibleType.*;

/**
 * The InterfaceTagMO managed object represents an interface tag.
 *
 * <p>The Accessor for interface tags supports read and write.</p>
 *
 * @see ManagedObjectFactory#createInterfaceTag()
 */
@XmlRootElement(name="InterfaceTag")
@XmlType(name="InterfaceTagType", propOrder={"nameValue", "addressPatternValues", "properties", "extension", "extensions"})
@AccessorSupport.AccessibleResource(name ="interfaceTags")
public class InterfaceTagMO extends ElementExtendableAccessibleObject {

    //- PUBLIC

    /**
     * Get the name for the interface tag (case sensitive, required)
     *
     * <p>Interface tag names must be unique.</p>
     *
     * @return The name of the interface tag (may be null)
     */
    public String getName() {
        return get(name);
    }

    /**
     * Set the name for the interface tag.
     *
     * @param name The name to use.
     */
    public void setName( final String name ) {
        this.name = set(this.name,name);
    }

    /**
     * Get the address patterns for this interface tag.
     *
     * <p>An address pattern is any of:</p>
     *
     * <ul>
     *   <li>CIDR notation</li>
     *   <li>IP Address</li>
     *   <li>IP Address prefix (e.g. "192.168")</li>
     * </ul>
     *
     * @return The list of address patterns (never null)
     */
    public List<String> getAddressPatterns() {
        return unwrap(get( addressPatterns, new ArrayList<AttributeExtensibleType.AttributeExtensibleString>() ));
    }

    /**
     * Set the address patterns for this interface tag.
     *
     * @param addressPatterns The address patterns to use
     */
    public void setAddressPatterns( final List<String> addressPatterns ) {
        this.addressPatterns = set( this.addressPatterns, wrap(addressPatterns,AttributeExtensibleStringBuilder) );
    }

    /**
     * Get the properties for this cluster property.
     *
     * @return The properties (may be null)
     */
    @XmlElement(name="Properties")
    @XmlJavaTypeAdapter(PropertiesMapType.PropertiesMapTypeAdapter.class)
    public Map<String, Object> getProperties() {
        return properties;
    }

    /**
     * Set the properties for this cluster property.
     *
     * @param properties The properties to use
     */
    public void setProperties( final Map<String, Object> properties ) {
        this.properties = properties;
    }

    //- PROTECTED

    @XmlElement(name="Name", required=true)
    protected AttributeExtensibleString getNameValue() {
        return name;
    }

    protected void setNameValue( final AttributeExtensibleString name ) {
        this.name = name;
    }

    @XmlElement(name="AddressPatterns", required=true)
    protected AttributeExtensibleType.AttributeExtensibleStringList getAddressPatternValues() {
        return addressPatterns;
    }

    protected void setAddressPatternValues( final AttributeExtensibleType.AttributeExtensibleStringList addressPattern ) {
        this.addressPatterns = addressPattern;
    }

    //- PACKAGE

    InterfaceTagMO() {
    }

    //- PRIVATE

    private AttributeExtensibleString name;
    private AttributeExtensibleType.AttributeExtensibleStringList addressPatterns;
    private Map<String,Object> properties;

}
