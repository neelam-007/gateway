package com.l7tech.gateway.api;

import com.l7tech.gateway.api.impl.PropertiesMapType;

import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.List;
import java.util.Map;

/**
 * 
 */
@XmlRootElement(name="PolicyValidationContext")
@XmlType(name="PolicyValidationContextType", propOrder={"policyType", "extensions", "properties", "resourceSets"})
public class PolicyValidationContext extends ManagedObject {

    //- PUBLIC

    @XmlElement(name="PolicyType")
    public PolicyDetail.PolicyType getPolicyType() {
        return policyType;
    }

    public void setPolicyType( final PolicyDetail.PolicyType policyType ) {
        this.policyType = policyType;
    }

    @XmlElement(name="Properties")
    @XmlJavaTypeAdapter( PropertiesMapType.PropertiesMapTypeAdapter.class)
    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setProperties( final Map<String, Object> properties ) {
        this.properties = properties;
    }

    @XmlElementWrapper(name="Resources")
    @XmlElement(name="ResourceSet")
    public List<ResourceSet> getResourceSets() {
        return resourceSets;
    }

    public void setResourceSets( final List<ResourceSet> resourceSets ) {
        this.resourceSets = resourceSets;
    }

    //- PROTECTED

    @Override
    @XmlAnyElement(lax=true)
    protected List<Object> getExtensions() {
        return super.getExtensions();
    }

    @Override
    protected void setExtensions( final List<Object> extensions ) {
        super.setExtensions( extensions );
    }

    //- PACKAGE

    PolicyValidationContext() {
    }

    //- PRIVATE

    private PolicyDetail.PolicyType policyType;
    private Map<String,Object> properties;
    private List<ResourceSet> resourceSets;
}
