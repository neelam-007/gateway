package com.l7tech.gateway.api;

import com.l7tech.gateway.api.impl.AccessorSupport;
import com.l7tech.gateway.api.impl.Extension;
import com.l7tech.gateway.api.impl.PropertiesMapType;

import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.List;
import java.util.Map;

/**
 * The ResourceDocumentMO managed object represents a document resource.
 *
 * <p>Global XML Schemas are represented as a ResourceDocumentMO with a
 * Resource of type "xmlschema".</p>
 *
 * <p>The Accessor for resource documents supports read and write. Resource
 * documents can be accessed by identifier only.</p>
 *
 * <p> The following properties can be set:
 * <ul>
 *   <li><code>description</code>: The description for the resource.</li>
 *   <li><code>targetNamespace</code> (read only): The target namespace for the
 *        resource if it is an XML Schema.</li>
 *   <li><code>publicIdentifier</code>: The public identifier for the
 *        resource if it is DTD.</li>
 * </ul>
 * </p>
 *
 * @see ManagedObjectFactory#createResourceDocument()
 */
@XmlRootElement(name="ResourceDocument")
@XmlType(name="ResourceDocumentType", propOrder={"resource", "properties", "extension", "extensions"})
@AccessorSupport.AccessibleResource(name ="resources")
public class ResourceDocumentMO extends AccessibleObject {

    //- PUBLIC

    /**
     * The resource for this resource document (required)
     *
     * @return The resource or null.
     */
    @XmlElement(name="Resource", required=false)
    public Resource getResource() {
        return resource;
    }

    /**
     * Set the resource for this resource document.
     *
     * @param resource The resource to use.
     */
    public void setResource( final Resource resource ) {
        this.resource = resource;
    }

    /**
     * Get the properties for the resource document.
     *
     * @return The properties or null.
     */
    @XmlElement(name="Properties")
    @XmlJavaTypeAdapter(PropertiesMapType.PropertiesMapTypeAdapter.class)
    public Map<String, Object> getProperties() {
        return properties;
    }

    /**
     * Set the properties for the resource document.
     *
     * @param properties The properties to use.
     */
    public void setProperties( final Map<String, Object> properties ) {
        this.properties = properties;
    }

    //- PROTECTED

    @XmlElement(name="Extension")
    @Override
    protected Extension getExtension() {
        return super.getExtension();
    }

    @Override
    protected void setExtension( final Extension extension ) {
        super.setExtension( extension );
    }

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

    ResourceDocumentMO(){        
    }

    //- PRIVATE

    private Resource resource;
    private Map<String,Object> properties;
}
