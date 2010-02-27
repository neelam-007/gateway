package com.l7tech.gateway.api;

import javax.xml.bind.annotation.*;
import javax.xml.namespace.QName;
import java.util.Map;

/**
 * Resource represents a document and related metadata.
 *
 * @see ResourceSet
 * @see ManagedObjectFactory#createResource()
 */
@XmlType(name="ResourceType")
public class Resource {

    //- PUBLIC

    /**
     * Get the transient identifier for the resource.
     *
     * @return The identifier or null.
     */
    @XmlAttribute(name="id")
    @XmlID
    public String getId() {
        return id;
    }

    /**
     * Set the transient identifier for the resource.
     *
     * @param id The transient identifier to use.
     */
    public void setId( final String id ) {
        this.id = id;
    }

    /**
     * Get the version for the resource.
     *
     * @return The version or null.
     */
    @XmlAttribute(name="version")
    public Integer getVersion() {
        return version;
    }

    /**
     * Set the version for the resource.
     *
     * @param version The version to use.
     */
    public void setVersion( final Integer version ) {
        this.version = version;
    }

    /**
     * Get the type for the resource.
     *
     * @return The resource type or null.
     */
    @XmlAttribute(name="type", required=true)
    public String getType() {
        return type;
    }

    /**
     * Set the type for the resource.
     *
     * @param type The type to use.
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Get the source URL for the resource.
     *
     * @return The source URL or null.
     */
    @XmlAttribute(name="sourceUrl")
    public String getSourceUrl() {
        return sourceUrl;
    }

    /**
     * Set the source URL for the resource.
     *
     * @param sourceUrl The source URL to use.
     */
    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    /**
     * Get the content for the resource.
     *
     * @return The resource content or null.
     */
    @XmlValue
    public String getContent() {
        return content;
    }

    /**
     * Set the content for the resource.
     *
     * @param content The resource content to use.
     */
    public void setContent(String content) {
        this.content = content;
    }

    //- PROTECTED

    @XmlAnyAttribute
    protected Map<QName, Object> getAttributeExtensions() {
        return attributeExtensions;
    }

    protected void setAttributeExtensions( final Map<QName, Object> attributeExtensions ) {
        this.attributeExtensions = attributeExtensions;
    }

    //- PACKAGE

    Resource(){
    }

    //- PRIVATE

    private String id;
    private Integer version;
    private String type; // not content type (wsdl, policy, policyexport, xmlschema, etc)
    private String sourceUrl;
    private String content;
    private Map<QName,Object> attributeExtensions;
}
