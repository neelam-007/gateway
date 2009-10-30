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

    String getBusinessServiceName();

    String getBusinessServiceKey();

    String getBusinessEntityKey();

    long getUddiRegistryOid();

    String getWsdlServiceName();
    
    /**
     * Unfortuantly this information is required in this data structure for now anyway
     *
     * @param oid long oid of the Entity from another package which we have no knowledge about here
     */
    void setUddiRegistryOid(long oid);
}
