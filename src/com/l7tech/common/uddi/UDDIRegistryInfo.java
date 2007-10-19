package com.l7tech.common.uddi;

/**
 * Information on a (type of) UDDI registry.
 *
 * @author Steve Jones
 */
public interface UDDIRegistryInfo {

    /**
     * Get the name for this registry.
     *
     * @return The name
     */
    String getName();

    /**
     * Get the inquiry service URL suffix for this registry.
     *
     * @return The url suffix
     */
    String getInquiry();

    /**
     * Get the publication service URL suffix for this registry.
     *
     * @return The url suffix
     */
    String getPublication();

    /**
     * Get the security policy service URL suffix for this registry.
     *
     * @return The url suffix
     */
    String getSecurityPolicy();

}
