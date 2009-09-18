/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.security.xml.decorator.DecorationRequirements;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.xmlsec.AddWssTimestamp;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.AuthenticationContext;
import org.springframework.context.ApplicationContext;
import org.w3c.dom.Document;

import java.util.logging.Logger;

/**
 * @author alex
 */
public class ServerAddWssTimestamp extends ServerAddWssSignature<AddWssTimestamp> {
    private static final Logger logger = Logger.getLogger(ServerAddWssTimestamp.class.getName());

    public ServerAddWssTimestamp(AddWssTimestamp assertion, ApplicationContext spring) {
        super(assertion, assertion, assertion, spring, logger, false);
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
        if ( assertion.getResolution() != null ) {
            switch ( assertion.getResolution() ) {
                case MILLISECONDS:
                    wssReq.setTimestampResolution( DecorationRequirements.TimestampResolution.MILLISECONDS );
                    break;
                case NANOSECONDS:
                    wssReq.setTimestampResolution( DecorationRequirements.TimestampResolution.NANOSECONDS );
                    break;
                case SECONDS:
                    wssReq.setTimestampResolution( DecorationRequirements.TimestampResolution.SECONDS );
                    break;
            }
        }
        wssReq.setTimestampTimeoutMillis(assertion.getExpiryMilliseconds());
        return signElements;
    }
}
