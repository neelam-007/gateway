package com.l7tech.common.uddi;

import java.io.Serializable;

/**
 * <p> Copyright (C) 2005 Layer 7 Technologies Inc.</p>
 * <p> @author fpang </p>
 * $Id$
 */
public class WsdlInfo implements Serializable {
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
