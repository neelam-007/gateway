/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.security.xml.decorator.DecorationRequirements;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.xmlsec.ResponseWssTimestamp;
import com.l7tech.server.message.PolicyEnforcementContext;
import org.springframework.context.ApplicationContext;
import org.w3c.dom.Document;

import java.util.logging.Logger;

/**
 * @author alex
 */
public class ServerResponseWssTimestamp extends ServerResponseWssSignature {
    private static final Logger logger = Logger.getLogger(ServerResponseWssTimestamp.class.getName());
    private final ResponseWssTimestamp assertion;

    public ServerResponseWssTimestamp(ResponseWssTimestamp assertion, ApplicationContext spring) {
        super(assertion, spring, logger);
        this.assertion = assertion;
    }

    protected int addDecorationRequirements(PolicyEnforcementContext context, Document soapmsg, DecorationRequirements wssReq) throws PolicyAssertionException {
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
