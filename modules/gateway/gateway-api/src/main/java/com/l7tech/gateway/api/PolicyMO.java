package com.l7tech.gateway.api;

import com.l7tech.gateway.api.impl.AccessorFactory;

import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
import java.util.List;

/**
 * 
 */
@XmlRootElement(name="Policy")
@XmlType(name="PolicyType", propOrder={"policyDetail", "extensions", "resourceSets"})
@AccessorFactory.ManagedResource(name ="policies", hasPolicy=true)
public class PolicyMO extends ManagedObject {

    //- PUBLIC

    @XmlTransient
    @Override
    public Integer getVersion() {
        Integer version = null;

        if ( policyDetail != null ) {
            version = policyDetail.getVersion();
        }

        return version;
    }

    @Override
    public void setVersion( final Integer version ) {
        if ( policyDetail != null ) {
            policyDetail.setVersion( version );
        }
    }

    @XmlAttribute(name="guid")
    public String getGuid() {
        return guid;
    }

    public void setGuid( final String guid ) {
        this.guid = guid;
    }

    @XmlElement(name="PolicyDetail", required=true)
    public PolicyDetail getPolicyDetail() {
        return policyDetail;
    }

    public void setPolicyDetail( final PolicyDetail policyDetail ) {
        this.policyDetail = policyDetail;
    }

    @XmlElementWrapper(name="Resources")
    @XmlElement(name="ResourceSet", required=true)
    public List<ResourceSet> getResourceSets() {
        return resourceSets;
    }

    public void setResourceSets( final List<ResourceSet> resourceSets ) {
        this.resourceSets = resourceSets;
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

    PolicyMO() {
    }

    //- PRIVATE

    private String guid;
    private PolicyDetail policyDetail;
    private List<ResourceSet> resourceSets;
}
