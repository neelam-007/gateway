package com.l7tech.gateway.api;

import com.l7tech.gateway.api.impl.AccessorSupport;
import com.l7tech.gateway.api.impl.ElementExtendableAccessibleObject;
import com.l7tech.gateway.api.impl.PropertiesMapType;
import static com.l7tech.gateway.api.impl.AttributeExtensibleType.*;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.Map;

/**
 * The ClusterPropertyMO managed object represents a cluster wide property.
 *
 * <p>The Accessor for cluster properties supports read and write. Cluster
 * properties can be accessed by name or identifier.</p>
 *
 * @see ManagedObjectFactory#createClusterProperty()
 */
@XmlRootElement(name="ClusterProperty")
@XmlType(name="ClusterPropertyType", propOrder={"nameValue", "valueValue", "properties", "extension", "extensions"})
@AccessorSupport.AccessibleResource(name ="clusterProperties")
public class ClusterPropertyMO extends ElementExtendableAccessibleObject {

    //- PUBLIC

    /**
     * The name for the cluster property (case insensitive, required)
     *
     * @return The name (may be null)
     */
    public String getName() {
        return get(name);
    }

    /**
     * Set the name for the cluster property.
     *
     * @param name The name to use.
     */
    public void setName( final String name ) {
        this.name = set(this.name,name);
    }

    /**
     * Get the value for the cluster property (required)
     *
     * @return The value (may be null)
     */
    public String getValue() {
        return get(value);
    }

    /**
     * Set the value for the cluster property.
     *
     * @param value The value to use.
     */
    public void setValue( final String value ) {
        this.value = set(this.value,value);
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

    @XmlElement(name="Value", required=true)
    protected AttributeExtensibleString getValueValue() {
        return value;
    }

    protected void setValueValue( final AttributeExtensibleString value ) {
        this.value = value;
    }

    //- PACKAGE

    ClusterPropertyMO() {
    }

    //- PRIVATE

    private AttributeExtensibleString name;
    private AttributeExtensibleString value;
    private Map<String,Object> properties;
}
