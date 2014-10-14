package com.l7tech.gateway.api;

import com.l7tech.gateway.api.impl.*;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.Map;

import static com.l7tech.gateway.api.impl.AttributeExtensibleType.*;

/**
 * MO representation of sample messages.
 *
 */
@XmlRootElement(name ="SampleMessage")
@XmlType(name="SampleMessageType", propOrder={"name","serviceId","operation", "xml","properties","extension","extensions"})
@AccessorSupport.AccessibleResource(name ="sampleMessages")
public class SampleMessageMO extends ElementExtendableAccessibleObject {

    //- PUBLIC

    /**
     * Get name of the sample message (case insensitive, required)
     *
     * @return The name (may be null)
     */
    @XmlElement(name="Name", required=true)
    public String getName() {
        return name;
    }

    /**
     * Set the name for the sample message.
     *
     * @param name The name to use.
     */
    public void setName(final String name) {
        this.name = name;
    }

    /**
     * Get xml of the sample message
     *
     * @return The xml (may be null)
     */
    @XmlElement(name="Xml", required=true)
    public String getXml() {
        return xml;
    }

    /**
     * Set the name for the sample message.
     *
     * @param xml The xml
     */
    public void setXml(final String xml) {
        this.xml = xml;
    }

    /**
     * Get xml of the sample message
     *
     * @return The xml (may be null)
     */
    @XmlElement(name="Operation")
    public String getOperation() {
        return operation;
    }

    /**
     * Set the operation for the sample message.
     *
     * @param operation The operation
     */
    public void setOperation(final String operation) {
        this.operation = operation;
    }

    /**
     * The service to which this message belongs
     *
     * @return The service id
     */
    @XmlElement(name = "ServiceId", required=true)
    public String getServiceId() {
        return serviceId;
    }

    /**
     * Sets the id to to which this message belongs. Required.
     *
     * @param serviceId The service's id
     */
    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    /**
     * Get the properties for this sample message
     *
     * @return The properties (may be null)
     */
    @XmlElement(name="Properties")
    @XmlJavaTypeAdapter( PropertiesMapType.PropertiesMapTypeAdapter.class)
    public Map<String, String> getProperties() {
        return properties;
    }

    /**
     * Set the properties for this sample message.
     *
     * @param properties The properties to use
     */
    public void setProperties( final Map<String, String> properties ) {
        this.properties = properties;
    }

    //- PACKAGE

    SampleMessageMO() {
    }

    //- PRIVATE

    private String name;
    private String operation;
    private String xml;
    private String serviceId;
    private Map<String,String> properties;

}
