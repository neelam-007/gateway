package com.l7tech.gateway.api;

import com.l7tech.gateway.api.impl.Extension;

import javax.xml.bind.annotation.XmlAnyAttribute;
import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.namespace.QName;
import java.util.List;
import java.util.Map;

/**
 * ResourceSet represents a set of {@code Resource}s.
 *
 * @see ManagedObjectFactory#createResourceSet()
 */
@XmlRootElement(name="ResourceSet")
@XmlType(name="ResourceSetType", propOrder={"resources","extension","extensions"})
public class ResourceSet {

    //- PUBLIC

    /**
     * Get the tag for the resource set (required)
     *
     * @return The tag or null.
     */
    @XmlAttribute(name="tag", required=true)
    public String getTag() {
        return tag;
    }

    /**
     * Set the tag for the resource set.
     *
     * @param tag The tag to use.
     */
    public void setTag( final String tag ) {
        this.tag = tag;
    }

    /**
     * Get the root URL for the resource set.
     *
     * <p>The root URL of the resource set must match the source URL or the
     * id of the primary resource in the set. For an id reference the URL
     * would be in the form '#identifier' (which would match the resource with
     * the id "identifier").</p>
     *
     * @return The root URL or null.
     */
    @XmlAttribute(name="rootUrl")
    public String getRootUrl() {
        return rootUrl;
    }

    /**
     * Set the root URL for the resource set.
     *
     * @param rootUrl The root URL to use.
     */
    public void setRootUrl( final String rootUrl ) {
        this.rootUrl = rootUrl;
    }

    /**
     * Get the resources for this resource set (required)
     *
     * @return The resources or null.
     */
    @XmlElement(name="Resource", required=false)
    public List<Resource> getResources() {
        return resources;
    }

    /**
     * Set the resources for this resource set.
     *
     * @param resources The resources to use.
     */
    public void setResources( final List<Resource> resources ) {
        this.resources = resources;
    }

    //- PROTECTED

    @XmlAnyAttribute
    protected Map<QName, Object> getAttributeExtensions() {
        return attributeExtensions;
    }

    protected void setAttributeExtensions( final Map<QName, Object> attributeExtensions ) {
        this.attributeExtensions = attributeExtensions;
    }

    @XmlElement(name="Extension")
    protected Extension getExtension() {
        return extension;
    }

    protected void setExtension( final Extension extension ) {
        this.extension = extension;
    }

    @XmlAnyElement(lax=true)
    protected List<Object> getExtensions() {
        return extensions;
    }

    protected void setExtensions( final List<Object> extensions ) {
        this.extensions = extensions;
    }

    //- PACKAGE

    ResourceSet() {
    }

    //- PRIVATE

    private String tag;
    private String rootUrl;
    private List<Resource> resources;
    private Map<QName,Object> attributeExtensions;
    private Extension extension;
    private List<Object> extensions;
}
