package com.l7tech.server.uddi;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlTransient;
import java.util.Map;
import java.util.HashMap;
import java.util.Collection;

/**
 * JAX-B serializable class for UDDI Templates.
 */
@XmlRootElement
public class UDDITemplate {

    //- PUBLIC

    @XmlTransient
    public String getName() {
        return name;
    }

    public void setName( final String name ) {
        this.name = name;
    }

    @XmlElement
    public String getInquiryUrl() {
        return inquiryUrl;
    }

    public void setInquiryUrl( final String inquiryUrl ) {
        this.inquiryUrl = inquiryUrl;
    }

    @XmlElement
    public String getPublicationUrl() {
        return publicationUrl;
    }

    public void setPublicationUrl( final String publicationUrl ) {
        this.publicationUrl = publicationUrl;
    }

    @XmlElement
    public String getSecurityPolicyUrl() {
        return securityPolicyUrl;
    }

    public void setSecurityPolicyUrl( final String securityPolicyUrl ) {
        this.securityPolicyUrl = securityPolicyUrl;
    }

    @XmlElement
    public String getSubscriptionUrl() {
        return subscriptionUrl;
    }

    public void setSubscriptionUrl( final String subscriptionUrl ) {
        this.subscriptionUrl = subscriptionUrl;
    }

    @XmlElement
    public KeyedReferenceTemplate getServiceMetricsKeyedReference() {
        return serviceMetricsKeyedReference;
    }

    public void setServiceMetricsKeyedReference( final KeyedReferenceTemplate serviceMetricsKeyedReference ) {
        this.serviceMetricsKeyedReference = serviceMetricsKeyedReference;
    }

    @XmlElement
    public String getMetricsTModelName() {
        return metricsTModelName;
    }

    public void setMetricsTModelName( final String metricsTModelName ) {
        this.metricsTModelName = metricsTModelName;
    }

    @XmlElement
    public String getMetricsTModelDescription() {
        return metricsTModelDescription;
    }

    public void setMetricsTModelDescription( final String metricsTModelDescription ) {
        this.metricsTModelDescription = metricsTModelDescription;
    }

    @XmlElement(name="metricsKeyedReference")
    @XmlElementWrapper
    public Collection<KeyedReferenceTemplate> getMetricsKeyedReferences() {
        return metricsKeyedReferences;
    }

    public void setMetricsKeyedReferences( final Collection<KeyedReferenceTemplate> metricsKeyedReferences ) {
        this.metricsKeyedReferences = metricsKeyedReferences;
    }

    @XmlElement
    public String getPolicyTModelName() {
        return policyTModelName;
    }

    public void setPolicyTModelName( final String policyTModelName ) {
        this.policyTModelName = policyTModelName;
    }

    @XmlElement
    public String getPolicyTModelDescription() {
        return policyTModelDescription;
    }

    public void setPolicyTModelDescription( final String policyTModelDescription ) {
        this.policyTModelDescription = policyTModelDescription;
    }

    /**
     * Template for a keyed reference.
     *
     * <p>The keyed reference can have a static value or reference a property
     * that is substituted at runtime (a Metrics value for example)</p> 
     */
    @XmlRootElement
    public static final class KeyedReferenceTemplate {
        private String key;
        private String name;
        private String value;
        private String valueProperty;

        @XmlElement
        public String getKey() {
            return key;
        }

        public void setKey( final String key ) {
            this.key = key;
        }

        @XmlElement
        public String getName() {
            return name;
        }

        public void setName( final String name ) {
            this.name = name;
        }

        @XmlElement
        public String getValue() {
            return value;
        }

        public void setValue( final String value ) {
            this.value = value;
        }

        @XmlElement
        public String getValueProperty() {
            return valueProperty;
        }

        public void setValueProperty( final String valueProperty ) {
            this.valueProperty = valueProperty;
        }
    }

    //- PACKAGE

    /**
     * Get the client visible properties of this template as a map.
     *
     * @return The client template map.
     */
    Map<String,Object> toClientMap() {
        Map<String,Object> map = new HashMap<String,Object>();
        map.put( "name", name );
        map.put( "inquiry", inquiryUrl );
        map.put( "publication", publicationUrl );
        map.put( "securityPolicy", securityPolicyUrl );
        map.put( "subscription", subscriptionUrl );
        return map;
    }

    //- PRIVATE

    private String name;
    private String inquiryUrl;
    private String publicationUrl;
    private String securityPolicyUrl;
    private String subscriptionUrl;
    private KeyedReferenceTemplate serviceMetricsKeyedReference;
    private String metricsTModelName;
    private String metricsTModelDescription;
    private Collection<KeyedReferenceTemplate> metricsKeyedReferences;
    private String policyTModelName;
    private String policyTModelDescription;

}
