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
 * The ResourceDocumentMO managed object represents a document resource.
 *
 * <p>The Global XML Schemas are represented as a ResourceDocumentMO with a
 * Resource of type XML SCHEMA.</p>
 */
@XmlRootElement(name="ResourceDocument")
@XmlType(name="ResourceDocumentType", propOrder={"resource", "extensions", "properties"})
@AccessorFactory.ManagedResource(name ="resources")
public class ResourceDocumentMO extends ManagedObject {

    //- PUBLIC

    @XmlElement(name="Resource", required=false)
    public Resource getResource() {
        return resource;
    }

    public void setResource( final Resource resource ) {
        this.resource = resource;
    }

    @XmlElement(name="Properties")
    @XmlJavaTypeAdapter(PropertiesMapType.PropertiesMapTypeAdapter.class)
    public Map<String, Object> getProperties() {
        return properties;
    }

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

    ResourceDocumentMO(){        
    }

    //- PRIVATE

    private Resource resource;
    private Map<String,Object> properties;
}
