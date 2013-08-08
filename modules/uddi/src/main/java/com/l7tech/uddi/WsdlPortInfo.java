/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * @author darmstrong
 */
package com.l7tech.uddi;

public interface WsdlPortInfo {
    // This is a flag URI.  Do not change this URI -- it is recognized by the SSM to mean that a search was truncated
    String MAXED_OUT_UDDI_RESULTS_URL = "http://layer7-tech.com/flag/maxed_out_search_result";
    String MAXED_OUT_UDDI_RESULTS_NAME = "SEARCH TOO BROAD";

    String getWsdlUrl();

    String getWsdlPortName();

    String getWsdlPortBinding();

    String getWsdlPortBindingNamespace();

    String getBusinessServiceName();

    String getBusinessServiceKey();

    String getBusinessEntityKey();

    String getBusinessEntityName();

    String getUddiRegistryId();

    String getWsdlServiceName();
    
    /**
     * Unfortuantly this information is required in this data structure for now anyway
     *
     * @param id String id of the Entity from another package which we have no knowledge about here
     */
    void setUddiRegistryId(String id);

    /**
     * Return true when absoutely certain that the WSDL url contained in this class is from this SecureSpan Gate
     * @return true means the WSDL is from this Gateway. This Gateway may be in a cluster
     */
    boolean isGatewayWsdl();

    /**
     * Return true when it is very likely that the WSDL is from this Gateway.
     * @return true when the WSDL is likely from this gateway
     */
    boolean isLikelyGatewayWsdl();

    /**
     * Get the value of the accessPoint element which belons to the bindingTemplate representign this wsdl:port
     * @return String endpoint URL. Never null or empty.
     */
    String getAccessPointURL();

    /**
     * Get the last time any element of the BusinessService this WsdlPortInfo belongs to was modified
     * @return
     */
    long getLastUddiMonitoredTimeStamp();

    /**
     * If true, then we know the user explicitly chose a wsdl:port, if false then the user did not and the contents
     * of this WsdlPortInfo is most likely simply the first wsdl:port found in UDDI
     *
     * If false, then the WSDL should not be placed under UDDI Control.
     *
     * @return boolean true when a wsdl:port was known to have been explicitly chosen by a user, false otherwise
     */
    boolean isWasWsdlPortSelected();

    /**
     * Set whether this WsdlPortInfo was known to have been explicitly selected by a user
     * @param wasSelected true when user made a selection, false when selected automatically
     */
    void setWasWsdlPortSelected(boolean wasSelected);
}
