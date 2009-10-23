package com.l7tech.uddi;

import java.io.Serializable;

/**
 * <p> Copyright (C) 2005 Layer 7 Technologies Inc.</p>
 * <p> @author fpang </p>
 */
public class WsdlInfo implements Serializable {

    // This is a flag URI.  Do not change this URI -- it is recognized by the SSM to mean that a search was truncated
    //TODO relocate all of these constants
    public static final String MAXED_OUT_UDDI_RESULTS_URL = "http://layer7-tech.com/flag/maxed_out_search_result";
    public static final String MAXED_OUT_UDDI_RESULTS_NAME = "SEARCH TOO BROAD";
    public static final WsdlInfo MAXED_OUT_SEARCH_RESULT = new WsdlInfo(MAXED_OUT_UDDI_RESULTS_NAME, MAXED_OUT_UDDI_RESULTS_URL);
    public static final UDDINamedEntity MAXED_OUT_SEARCH_RESULT_ENTITY =
            new UDDINamedEntityImpl(MAXED_OUT_UDDI_RESULTS_NAME, MAXED_OUT_UDDI_RESULTS_URL);

    private String name;
    private String wsdlUrl;

    public WsdlInfo(String name, String wsdlUrl) {
        this.name = name;
        this.wsdlUrl = wsdlUrl;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getWsdlUrl() {
        return wsdlUrl;
    }

    public void setWsdlUrl(String wsdlUrl) {
        this.wsdlUrl = wsdlUrl;
    }
}
