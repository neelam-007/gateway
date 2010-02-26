package com.l7tech.gateway.api;

/**
 * Accessor for ServiceMO that allows update of service details and WSDL.
 */
public interface ServiceMOAccessor extends PolicyAccessor<ServiceMO> {

    /**
     * Get the details for the identified service resource.
     *
     * @param identifier The identifier for the service.
     * @return The service resource details
     * @throws AccessorException If an error occurs
     */
    ServiceDetail getServiceDetail( String identifier ) throws AccessorException;

    /**
     * Set the details for the identified service resource.
     *
     * @param identifier The identifier for the service.
     * @param serviceDetail The service resource details
     * @throws AccessorException If an error occurs
     */
    void putServiceDetail( String identifier, ServiceDetail serviceDetail ) throws AccessorException;

    /**
     * Get the WSDL for the identified resource.
     *
     * @param identifier The identifier for the resource.
     * @return The WSDL resource set
     * @throws AccessorException If an error occurs
     */
    ResourceSet getWsdl( String identifier ) throws AccessorException;

    /**
     * Set the WSDL for the identified resource.
     *
     * @param identifier The identifier for the resource.
     * @param resourceSet The WSDL resource set
     * @throws AccessorException If an error occurs
     */
    void putWsdl( String identifier, ResourceSet resourceSet ) throws AccessorException;


}
