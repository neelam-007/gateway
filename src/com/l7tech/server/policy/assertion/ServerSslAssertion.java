/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.policy.assertion;

import com.l7tech.policy.assertion.SslAssertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.message.Request;
import com.l7tech.message.Response;
import com.l7tech.message.TransportMetadata;
import com.l7tech.message.TransportProtocol;
import com.l7tech.logging.LogManager;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author alex
 * @version $Revision$
 */
public class ServerSslAssertion implements ServerAssertion {
    public ServerSslAssertion( SslAssertion data ) {
        _data = data;
    }

    public AssertionStatus checkRequest(Request request, Response response) throws PolicyAssertionException {
        TransportMetadata tm = request.getTransportMetadata();
        boolean ssl = (tm.getProtocol() == TransportProtocol.HTTPS);
        AssertionStatus status;

        String message;
        Level level;
        SslAssertion.Option option = _data.getOption();
        if ( option == SslAssertion.REQUIRED) {
            if (ssl) {
                status = AssertionStatus.NONE;
                message = "SSL required and present";
                level = Level.FINE;
            } else {
                status = AssertionStatus.FALSIFIED;
                message = "SSL required but not present";
                level = Level.INFO;
            }
        } else if ( option == SslAssertion.FORBIDDEN) {
            if (ssl) {
                status = AssertionStatus.FALSIFIED;
                message = "SSL forbidden but present";
                level = Level.INFO;
            } else {
                status = AssertionStatus.NONE;
                message = "SSL forbidden and not present";
                level = Level.FINE;
            }
        } else {
            level = Level.FINE;
            status = AssertionStatus.NONE;
            message = ssl ? "SSL optional and present" : "SSL optional and not present";
        }

        logger.log(level, message);

        if (status == AssertionStatus.FALSIFIED) response.setPolicyViolated(true);

        return status;
    }

    protected SslAssertion _data;
    protected Logger logger = LogManager.getInstance().getSystemLogger();
}
