package com.l7tech.console.event;

import com.l7tech.common.uddi.WsdlInfo;

import java.util.EventObject;

/**
 * <p> Copyright (C) 2005 Layer 7 Technologies Inc.</p>
 * <p> @author fpang </p>
 * $Id$
 */
public class WsdlEvent extends EventObject {

    private WsdlInfo wsdlInfo;

    /**
     * create the Certificate event
     *
     * @param source the event source
     */
     public WsdlEvent(Object source, WsdlInfo wsdlInfo) {
        super(source);
        this.wsdlInfo = wsdlInfo;
    }

    public WsdlInfo getWsdlInfo() {
        return wsdlInfo;
    }
}
