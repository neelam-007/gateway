/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.policy.assertion;

import com.l7tech.policy.assertion.JmsTransportAssertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.message.Request;
import com.l7tech.message.Response;
import com.l7tech.server.transport.jms.JmsSoapRequest;

import java.io.IOException;

/**
 * @author alex
 * @version $Revision$
 */
public class ServerJmsTransportAssertion implements ServerAssertion {
    public ServerJmsTransportAssertion( JmsTransportAssertion data ) {
        this.data = data;
    }

    public AssertionStatus checkRequest( Request request, Response response ) throws IOException, PolicyAssertionException {
        if ( request instanceof JmsSoapRequest ) {
            return AssertionStatus.NONE;
        } else {
            return AssertionStatus.FALSIFIED;
        }
    }

    private JmsTransportAssertion data;
}
