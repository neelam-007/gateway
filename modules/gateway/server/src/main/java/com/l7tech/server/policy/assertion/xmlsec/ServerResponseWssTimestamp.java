/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.security.xml.decorator.DecorationRequirements;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.xmlsec.ResponseWssTimestamp;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.AuthenticationContext;
import org.springframework.context.ApplicationContext;
import org.w3c.dom.Document;

import java.util.logging.Logger;

/**
 * @author alex
 */
public class ServerResponseWssTimestamp extends ServerResponseWssSignature<ResponseWssTimestamp> {
    private static final Logger logger = Logger.getLogger(ServerResponseWssTimestamp.class.getName());

    public ServerResponseWssTimestamp(ResponseWssTimestamp assertion, ApplicationContext spring) {
        super(assertion, assertion, assertion, spring, logger);
    }

    @Override
    protected int addDecorationRequirements( final PolicyEnforcementContext context,
                                             final AuthenticationContext authContext,
                                             final Document soapmsg,
                                             final DecorationRequirements wssReq ) throws PolicyAssertionException {
        int signElements = 0;
        if (assertion.isSignatureRequired()) {
            wssReq.setSignTimestamp();
            signElements = 1;
        }
        else {
            wssReq.setIncludeTimestamp(true);            
        }
        wssReq.setTimestampTimeoutMillis(assertion.getExpiryMilliseconds());
        return signElements;
    }
}
