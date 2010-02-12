package com.l7tech.gateway.api;

import com.l7tech.gateway.api.impl.PropertiesMapType;

import javax.xml.bind.annotation.*;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import javax.xml.namespace.QName;
import java.util.List;
import java.util.Map;

/**
 * 
 */
@XmlType(name="ServiceDetailType", propOrder={"name","enabled","serviceMappings","extensions","properties"})
@XmlSeeAlso({ServiceDetail.HttpMapping.class, ServiceDetail.SoapMapping.class})
public class ServiceDetail {

    //- PUBLIC

    @XmlAttribute(name="id")
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

    @XmlAttribute(name="folderId")
    public String getFolderId() {
        return folderId;
    }

    public void setFolderId( final String folderId ) {
        this.folderId = folderId;
    }

    @XmlElement(name="Name", required=true)
    public String getName() {
        return name;
    }

    public void setName( final String name ) {
        this.name = name;
    }

    @XmlElement(name="Enabled", required=true)
    public boolean getEnabled() {
        return enabled;
    }

    public void setEnabled( final boolean enabled ) {
        this.enabled = enabled;
    }

    @XmlElementWrapper(name="ServiceMappings")
    @XmlElementRef
    public List<? extends ServiceMapping> getServiceMappings() {
        return serviceMappings;
    }

    public void setServiceMappings( final List<? extends ServiceMapping> serviceMappings ) {
        this.serviceMappings = serviceMappings;
    }

    @XmlElement(name="Properties")
    @XmlJavaTypeAdapter(PropertiesMapType.PropertiesMapTypeAdapter.class)
    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setProperties( final Map<String, Object> properties ) {
        this.properties = properties;
    }

    @XmlType(name="ServiceMappingType")
    public static abstract class ServiceMapping {
    }

    @XmlRootElement(name="HttpMapping")
    @XmlType(name="HttpServiceMappingType", propOrder={"urlPattern","verbs","extensions"})
    public static class HttpMapping extends ServiceMapping {
        private String urlPattern;
        private List<String> verbs;
        private List<Object> extensions;
        private Map<QName,Object> attributeExtensions;

        HttpMapping() {
        }

        @XmlElement(name="UrlPattern")
        public String getUrlPattern() {
            return urlPattern;
        }

        public void setUrlPattern( final String urlPattern ) {
            this.urlPattern = urlPattern;
        }

        @XmlElementWrapper(name="Verbs")
        @XmlElement(name="Verb")
        public List<String> getVerbs() {
            return verbs;
        }

        public void setVerbs( final List<String> verbs ) {
            this.verbs = verbs;
        }

        @XmlAnyAttribute
        protected Map<QName, Object> getAttributeExtensions() {
            return attributeExtensions;
        }

        protected void setAttributeExtensions( final Map<QName, Object> attributeExtensions ) {
            this.attributeExtensions = attributeExtensions;
        }

        @XmlAnyElement(lax=true)
        protected List<Object> getExtensions() {
            return extensions;
        }

        protected void setExtensions( final List<Object> extensions ) {
            this.extensions = extensions;
        }
    }

    @XmlRootElement(name="SoapMapping")
    @XmlType(name="SoapServiceMappingType",propOrder={"lax", "extensions"})
    public static class SoapMapping extends ServiceMapping {
        private boolean lax;
        private List<Object> extensions;
        private Map<QName,Object> attributeExtensions;

        SoapMapping() {            
        }

        @XmlElement(name="Lax")
        public boolean isLax() {
            return lax;
        }

        public void setLax( final boolean lax ) {
            this.lax = lax;
        }

        @XmlAnyAttribute
        protected Map<QName, Object> getAttributeExtensions() {
            return attributeExtensions;
        }

        protected void setAttributeExtensions( final Map<QName, Object> attributeExtensions ) {
            this.attributeExtensions = attributeExtensions;
        }

        @XmlAnyElement(lax=true)
        protected List<Object> getExtensions() {
            return extensions;
        }

        protected void setExtensions( final List<Object> extensions ) {
            this.extensions = extensions;
        }
    }

    //- PROTECTED

    @XmlAnyAttribute
    protected Map<QName, Object> getAttributeExtensions() {
        return attributeExtensions;
    }

    protected void setAttributeExtensions( final Map<QName, Object> attributeExtensions ) {
        this.attributeExtensions = attributeExtensions;
    }

    @XmlAnyElement(lax=true)
    protected List<Object> getExtensions() {
        return extensions;
    }

    protected void setExtensions( final List<Object> extensions ) {
        this.extensions = extensions;
    }

    //- PACKAGE

    ServiceDetail(){
    }

    //- PRIVATE

    private String id;
    private String folderId;
    private Integer version;
    private String name;
    private boolean enabled;
    private List<? extends ServiceMapping> serviceMappings;
    private Map<String,Object> properties;
    private Map<QName,Object> attributeExtensions;
    private List<Object> extensions;
    
}
