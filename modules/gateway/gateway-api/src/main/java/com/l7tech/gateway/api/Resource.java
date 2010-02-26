package com.l7tech.gateway.api;

import javax.xml.bind.annotation.*;
import javax.xml.namespace.QName;
import java.util.Map;

/**
 * 
 */
@XmlType(name="ResourceType")
public class Resource {

    //- PUBLIC

    @XmlAttribute(name="id")
    @XmlID
    public String getId() {
        return id;
    }

    public void setId( final String id ) {
        this.id = id;
    }

    @XmlAttribute(name="version")
    public Integer getVersion() {
        return version;
    }

    public void setVersion( final Integer version ) {
        this.version = version;
    }

    @XmlAttribute(name="type", required=true)
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @XmlAttribute(name="sourceUrl")
    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    @XmlValue
    public String getContent() {
        return content;
    }

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
