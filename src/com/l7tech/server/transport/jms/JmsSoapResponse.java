/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.transport.jms;

import com.l7tech.message.SoapResponse;

/**
 * @author alex
 * @version $Revision$
 */
public class JmsSoapResponse extends SoapResponse {
    public JmsSoapResponse(JmsTransportMetadata tm) {
        super(tm);
    }

    public Object doGetParameter(String name) {
        return _transportMetadata.getResponseParameter(name);
    }
}
