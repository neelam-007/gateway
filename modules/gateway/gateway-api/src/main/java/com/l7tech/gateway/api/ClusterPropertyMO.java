package com.l7tech.gateway.api;

import com.l7tech.gateway.api.impl.AccessorFactory;
import com.l7tech.gateway.api.impl.PropertiesMapType;

import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.List;
import java.util.Map;

/**
 * The ClusterPropertyMO managed object represents a cluster wide property.
 */
@XmlRootElement(name="ClusterProperty")
@XmlType(name="ClusterPropertyType", propOrder={"name", "value", "extensions", "properties"})
@AccessorFactory.ManagedResource(name ="clusterProperties")
public class ClusterPropertyMO extends ManagedObject {

    //- PUBLIC

    /**
     * The name for the cluster property (case insensitive)
     *
     * @return The name (may be null)
     */
    @XmlElement(name="Name", required=true)
    public String getName() {
        return name;
    }

    /**
     * Set the name for the cluster property.
     *
     * @param name The name to use.
     */
    public void setName( final String name ) {
        this.name = name;
    }

    /**
     * Get the value for the cluster property.
     *
     * @return The value (may be null)
     */
    @XmlElement(name="Value", required=true)
    public String getValue() {
        return value;
    }

    /**
     * Set the value for the cluster property.
     *
     * @param value The value to use.
     */
    public void setValue( final String value ) {
        this.value = value;
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

    @XmlAnyElement(lax=true)
    @Override
    protected List<Object> getExtensions() {
        return super.getExtensions();
    }

    @Override
    protected void setExtensions( final List<Object> extensions ) {
        super.setExtensions( extensions );
    }

    //- PACKAGE

    ClusterPropertyMO() {
    }

    //- PRIVATE

    private String name;
    private String value;
    private Map<String,Object> properties;
}
