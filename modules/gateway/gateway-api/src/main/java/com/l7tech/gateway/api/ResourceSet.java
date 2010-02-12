package com.l7tech.gateway.api;

import javax.xml.bind.annotation.XmlAnyAttribute;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.namespace.QName;
import java.util.List;
import java.util.Map;

/**
 * 
 */
@XmlRootElement(name="ResourceSet")
@XmlType(name="ResourceSetType", propOrder={"resources"})
public class ResourceSet {

    //- PUBLIC

    @XmlAttribute(name="tag", required=true)
    public String getTag() {
        return tag;
    }

    public void setTag( final String tag ) {
        this.tag = tag;
    }

    @XmlAttribute(name="rootUrl")
    public String getRootUrl() {
        return rootUrl;
    }

    public void setRootUrl( final String rootUrl ) {
        this.rootUrl = rootUrl;
    }

    @XmlElement(name="Resource", required=false)
    public List<Resource> getResources() {
        return resources;
    }

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

    //- PACKAGE

    ResourceSet() {
    }

    //- PRIVATE

    private String tag;
    private String rootUrl;
    private List<Resource> resources;
    private Map<QName,Object> attributeExtensions;
}
