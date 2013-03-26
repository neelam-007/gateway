package com.l7tech.gateway.api;

import static com.l7tech.gateway.api.impl.AttributeExtensibleType.*;

import com.l7tech.gateway.api.impl.ElementExtensionSupport;
import com.l7tech.gateway.api.impl.PropertiesMapType;
import com.l7tech.util.Functions;

import javax.xml.bind.annotation.*;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import javax.xml.namespace.QName;
import java.util.List;
import java.util.Map;

/**
 * Details for a ServiceMO.
 *
 * <p>The following properties can be used:
 * <ul>
 *   <li><code>defaultRoutingUrl</code>: The default routing URL for the service.</li>
 *   <li><code>internal</code> (read only): True for internal services.</li>
 *   <li><code>policyRevision</code> (read only): The policy revision number.</li>
 *   <li><code>soap</code>: True if the policy is for SOAP message processing.</li>
 *   <li><code>soapVersion</code>: The SOAP version for the service (e.g. "1.1", "1.2", "unspecified"), since v5.4</li>
 *   <li><code>wssProcessingEnabled</code>: True if WS-Security processing is enabled for the service.</li>
 * </ul>
 * </p>
 *
 * @see ServiceMO
 * @see ServiceMOAccessor#getServiceDetail(String)
 * @see ServiceMOAccessor#putServiceDetail(String, ServiceDetail)
 * @see ManagedObjectFactory#createServiceDetail()
 */
@XmlType(name="ServiceDetailType", propOrder={"nameValue","enabledValue","serviceMappings","properties","extension","extensions"})
@XmlSeeAlso({ServiceDetail.HttpMapping.class, ServiceDetail.SoapMapping.class})
public class ServiceDetail extends ElementExtensionSupport {

    //- PUBLIC

    /**
     * Get the identifier for the service.
     *
     * @return The identifier or null.
     */
    @XmlAttribute(name="id")
    public String getId() {
        return id;
    }

    /**
     * Set the identifier for the service.
     *
     * @param id The identifier to use.
     */
    public void setId( final String id ) {
        this.id = id;
    }

    /**
     * Get the version for the service.
     *
     * <p>This is distinct from the 'policyRevision' property.</p>
     *
     * @return The identifier or null.
     */
    @XmlAttribute(name="version")
    public Integer getVersion() {
        return version;
    }

    /**
     * Set the version for the service.
     *
     * @param version The version to use.
     */
    public void setVersion( final Integer version ) {
        this.version = version;
    }

    /**
     * Get the identifier of the folder containing this service.
     *
     * @return The folder identifier or null.
     * @see FolderMO
     */
    @XmlAttribute(name="folderId")
    public String getFolderId() {
        return folderId;
    }

    /**
     * Set the identifier of the folder containing this service.
     *
     * @param folderId The folder identifier to use.
     */
    public void setFolderId( final String folderId ) {
        this.folderId = folderId;
    }

    /**
     * Get the name for the service (required)
     *
     * @return The service name.
     */
    public String getName() {
        return get(name);
    }

    /**
     * Set the name for the service.
     *
     * @param name The name to use.
     */
    public void setName( final String name ) {
        this.name = set(this.name,name);
    }

    /**
     * Get the service enabled flag (required)
     *
     * @return True if the service is enabled.
     */
    public boolean getEnabled() {
        return get(enabled, false);
    }

    /**
     * Set the enabled flag for the service.
     *
     * @param enabled True to enable the service.
     */
    public void setEnabled( final boolean enabled ) {
        this.enabled = set(this.enabled,enabled);
    }

    /**
     * Get the service mappings for the service.
     *
     * <p>If an HttpMapping is not used when creating a service then the
     * service will not be accessible using the HTTP transport.</p>
     *
     * @return The service mappings or null.
     * @see HttpMapping
     * @see SoapMapping
     */
    @XmlElementWrapper(name="ServiceMappings")
    @XmlElementRef
    public List<? extends ServiceMapping> getServiceMappings() {
        return serviceMappings;
    }

    /**
     * Set the service mappings for the service.
     *
     * @param serviceMappings The service mappings to use.
     */
    public void setServiceMappings( final List<? extends ServiceMapping> serviceMappings ) {
        this.serviceMappings = serviceMappings;
    }

    /**
     * Get the properties for the service.
     *
     * @return The properties or null.
     */
    @XmlElement(name="Properties")
    @XmlJavaTypeAdapter(PropertiesMapType.PropertiesMapTypeAdapter.class)
    public Map<String, Object> getProperties() {
        return properties;
    }

    /**
     * Set the properties for the service.
     *
     * @param properties The properties to use.
     */
    public void setProperties( final Map<String, Object> properties ) {
        this.properties = properties;
    }

    /**
     * Abstract base class for service mappings.
     */
    @XmlType(name="ServiceMappingType")
    public static abstract class ServiceMapping {
    }

    /**
     * HTTP service mapping.
     *
     * @see ManagedObjectFactory#createHttpMapping()
     */
    @XmlRootElement(name="HttpMapping")
    @XmlType(name="HttpServiceMappingType", propOrder={"urlPatternValue","verbsValue","extensions"})
    public static class HttpMapping extends ServiceMapping {
        private AttributeExtensibleString urlPattern;
        private List<AttributeExtensibleString> verbs;
        private List<Object> extensions;
        private Map<QName,Object> attributeExtensions;

        HttpMapping() {
        }

        /**
         * Get the service resolution pattern for the service.
         *
         * <p>The pattern must not start with '/ssg' which is reserved for
         * Gateway use.</p>
         *
         * <p>If not null, the pattern must start with '/' and may contain the
         * '*' wildcard.</p>
         *
         * @return The pattern or null.
         */
        public String getUrlPattern() {
            return get(urlPattern);
        }

        /**
         * Set the service resolution pattern for the service.
         *
         * @param urlPattern The pattern to use.
         */
        public void setUrlPattern( final String urlPattern ) {
            this.urlPattern = set(this.urlPattern,urlPattern);
        }

        /**
         * Get the HTTP verbs permitted for the service.
         *
         * <p>The permitted verbs are:
         * <ul>
         *   <li>GET</li>
         *   <li>POST</li>
         *   <li>PUT</li>
         *   <li>DELETE</li>
         * </ul>
         *
         * @return The permitted verbs or null.
         */
        public List<String> getVerbs() {
            return verbs==null ? null : Functions.map( verbs, new Functions.Unary<String,AttributeExtensibleString>(){
                @Override
                public String call( final AttributeExtensibleString attributeExtensibleString ) {
                    return get(attributeExtensibleString);
                }
            });
        }

        /**
         * Set the HTTP verbs for the service.
         *
         * @param verbs The verbs to use.
         */
        public void setVerbs( final List<String> verbs ) {
            this.verbs = verbs == null ? null : Functions.map( verbs, new Functions.Unary<AttributeExtensibleString,String>(){
                @Override
                public AttributeExtensibleString call( final String s ) {
                    return set(null, s);
                }
            } );
        }

        @XmlElement(name="UrlPattern")
        protected AttributeExtensibleString getUrlPatternValue() {
            return urlPattern;
        }

        protected void setUrlPatternValue( final AttributeExtensibleString urlPattern ) {
            this.urlPattern = urlPattern;
        }

        @XmlElementWrapper(name="Verbs")
        @XmlElement(name="Verb")
        protected List<AttributeExtensibleString> getVerbsValue() {
            return verbs;
        }

        protected void setVerbsValue( final List<AttributeExtensibleString> verbs ) {
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

    /**
     * SOAP service mapping.
     *
     * <p>This mapping should only be used for SOAP service.</p>
     */
    @XmlRootElement(name="SoapMapping")
    @XmlType(name="SoapServiceMappingType",propOrder={"laxValue", "extensions"})
    public static class SoapMapping extends ServiceMapping {
        private AttributeExtensibleBoolean lax = new AttributeExtensibleBoolean(false);
        private List<Object> extensions;
        private Map<QName,Object> attributeExtensions;

        SoapMapping() {            
        }

        /**
         * Flag for lax service resolution.
         *
         * <p>When resolution is lax request messages are not checked against
         * the operations for the service (any request is permitted)</p>
         *
         * @return True for lax resolution.
         */
        public boolean isLax() {
            return get(lax, false);
        }

        /**
         * Set lax service resolution.
         *
         * @param lax True for lax resolution.
         */
        public void setLax( final boolean lax ) {
            this.lax = set(this.lax,lax);
        }

        @XmlElement(name="Lax", required=true)
        protected AttributeExtensibleBoolean getLaxValue() {
            return lax;
        }

        protected void setLaxValue( final AttributeExtensibleBoolean lax ) {
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

    @XmlElement(name="Name", required=true)
    protected AttributeExtensibleString getNameValue() {
        return name;
    }

    protected void setNameValue( final AttributeExtensibleString name ) {
        this.name = name;
    }

    @XmlElement(name="Enabled", required=true)
    protected AttributeExtensibleBoolean getEnabledValue() {
        return enabled;
    }

    protected void setEnabledValue( final AttributeExtensibleBoolean value ) {
        this.enabled = value;
    }

    //- PACKAGE

    ServiceDetail(){
    }

    //- PRIVATE

    private String id;
    private String folderId;
    private Integer version;
    private AttributeExtensibleString name;
    private AttributeExtensibleBoolean enabled = new AttributeExtensibleBoolean(false);
    private List<? extends ServiceMapping> serviceMappings;
    private Map<String,Object> properties;
}
