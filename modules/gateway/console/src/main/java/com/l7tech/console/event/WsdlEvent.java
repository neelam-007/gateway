package com.l7tech.console.event;

import com.l7tech.uddi.WsdlPortInfo;

import java.util.EventObject;

/**
 * <p> Copyright (C) 2005 Layer 7 Technologies Inc.</p>
 * <p> @author fpang </p>
 * $Id$
 */
public class WsdlEvent extends EventObject {

    private WsdlPortInfo wsdlPortInfo;

    /**
     * create the Certificate event
     *
     * @param source the event source
     */
     public WsdlEvent(Object source, WsdlPortInfo wsdlPortInfo) {
        super(source);
        this.wsdlPortInfo = wsdlPortInfo;
    }

    public WsdlPortInfo getWsdlInfo() {
        return wsdlPortInfo;
    }
}
