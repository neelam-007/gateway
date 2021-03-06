package com.l7tech.gateway.api;

import com.l7tech.gateway.api.impl.AccessorSupport;
import com.l7tech.gateway.api.impl.ElementExtendableAccessibleObject;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.List;

/**
 * The ServiceMO managed object represents a service.
 *
 * <p>The Accessor for services supports read and write. Services can be
 * accessed by identifier only.</p>
 *
 * @see ManagedObjectFactory#createService()
 */
@XmlRootElement(name="Service")
@XmlType(name="ServiceType", propOrder={"serviceDetail", "resourceSets", "extension", "extensions"})
@AccessorSupport.AccessibleResource(name="services", accessorClassname="com.l7tech.gateway.api.impl.ServiceMOAccessorImpl")
public class ServiceMO extends ElementExtendableAccessibleObject {

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

    /**
     * Get the details for the service (required)
     *
     * @return The details for null.
     */
    @XmlElement(name="ServiceDetail", required=true)
    public ServiceDetail getServiceDetail() {
        return serviceDetail;
    }

    /**
     * Set the details for the service.
     *
     * @param serviceDetail The details to use.
     */
    public void setServiceDetail( final ServiceDetail serviceDetail ) {
        this.serviceDetail = serviceDetail;
    }

    /**
     * Get the resource sets for the service (required)
     *
     * <p>The policy document for a service is a ResourceSet with tag 'policy'
     * containing a Resource of type 'policy'.</p>
     *
     * <p>The WSDL for a (SOAP) service is a {@code ResourceSet} with tag 'wsdl'
     * containing {@code Resource}s of types 'wsdl' and 'xmlschema'. The
     * primary WSDL resource should be identified by the root URL of the
     * {@code ResourceSet}.</p>
     *
     * <p>If the WSDL resource set has a source URL but no {@code Resource}s
     * the Gateway will attempt to download the WSDL from the URL.</p>
     *
     * @return The resources or null.
     */
    @XmlElementWrapper(name="Resources", required=true)
    @XmlElement(name="ResourceSet", required=true)
    public List<ResourceSet> getResourceSets() {
        return resourceSets;
    }

    /**
     * Set the resource sets for the service.
     *
     * @param resourceSets The resource sets to use.
     */
    public void setResourceSets( final List<ResourceSet> resourceSets ) {
        this.resourceSets = resourceSets;
    }

    //- PACKAGE

    ServiceMO(){        
    }

    //- PRIVATE

    private ServiceDetail serviceDetail;
    private List<ResourceSet> resourceSets;
}
