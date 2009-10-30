package com.l7tech.uddi;

/**
 * Interface for UDDI entities with keys and names.
 *
 * @author Steve Jones
*/
public interface UDDINamedEntity {

    /**
     * Get the UDDI key for the entity.
     *
     * @return The key value
     */
    String getKey();

    /**
     * Get the name for the entity.
     *
     * @return The name
     */
    String getName();

    /**
     * Get the Policy URL for the entity.
     *
     * <p>Note that this will usually be null unless searching for policy
     * information.</p>
     *
     * @return The URL or null
     */
    String getPolicyUrl();

    /**
     * Get the WSDL URL for the entity.
     *
     * <p>Note that this will usually be null unless searching for WSDL
     * information.</p>
     *
     * @return The URL or null
     */
    String getWsdlUrl();
}
