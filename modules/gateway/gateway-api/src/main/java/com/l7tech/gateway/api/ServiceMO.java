package com.l7tech.gateway.api;

import com.l7tech.gateway.api.impl.AccessorFactory;
import com.l7tech.gateway.api.impl.PolicyMOAccessorImpl;

import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.List;

/**
 * 
 */
@XmlRootElement(name="Service")
@XmlType(name="ServiceType", propOrder={"serviceDetail", "extensions", "resourceSets"})
@AccessorFactory.ManagedResource(name="services", accessorType=PolicyMOAccessorImpl.class)
public class ServiceMO extends ManagedObject {

    //- PUBLIC

    @Override
    public Integer getVersion() {
        Integer version = null;

        if ( serviceDetail != null ) {
            version = serviceDetail.getVersion();
        }

        return version;
    }

    @Override
    public void setVersion( final Integer version ) {
        if ( serviceDetail != null ) {
            serviceDetail.setVersion( version );
        }
    }

    @XmlElement(name="ServiceDetail", required=true)
    public ServiceDetail getServiceDetail() {
        return serviceDetail;
    }

    public void setServiceDetail( final ServiceDetail serviceDetail ) {
        this.serviceDetail = serviceDetail;
    }

    @XmlElementWrapper(name="Resources", required=true)
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

    ServiceMO(){        
    }

    //- PRIVATE

    private ServiceDetail serviceDetail;
    private List<ResourceSet> resourceSets;
}
