/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.policy.assertion.credential.wss;

import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.credential.wss.WssBasic;
import com.l7tech.proxy.datamodel.exceptions.HttpChallengeRequiredException;
import com.l7tech.proxy.datamodel.exceptions.OperationCanceledException;
import com.l7tech.proxy.message.PolicyApplicationContext;
import com.l7tech.proxy.policy.assertion.ClientDecorator;
import com.l7tech.security.token.UsernameTokenImpl;
import com.l7tech.security.xml.decorator.DecorationRequirements;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * decorates a request with a header that looks like that:
 * <wsse:Security xmlns:wsse="http://schemas.xmlsoap.org/ws/2002/04/secext">
 *           <wsse:UsernameToken Id="MyID">
 *                   <wsse:Username>Zoe</wsse:Username>
 *                   <Password>ILoveLlamas</Password>
 *           </wsse:UsernameToken>
 * </wsse:Security>
 * @author flascell
 * @version $Revision$
 */
public class ClientWssBasic extends ClientWssCredentialSource {
    public static final Logger log = Logger.getLogger(ClientWssBasic.class.getName());

    public ClientWssBasic( WssBasic data ) {
        super(data);
        this.data = data;
    }

    /**
     * Decorate the xml soap message with a WSS header containing the username and password.
     *
     * @param context
     * @return the result of running the decorateRequest() event through the policy.  Never null.
     */
    public AssertionStatus decorateRequest(PolicyApplicationContext context)
            throws OperationCanceledException, IOException, SAXException, HttpChallengeRequiredException {
        if (context.getSsg().isFederatedGateway()) {
            log.log(Level.INFO, "Plaintext passwords not permitted with Federated Gateway.  Assertion therefore fails.");
            return AssertionStatus.FAILED;
        }

        // get the username and passwords
        final String username = context.getUsername();
        final char[] password = context.getPassword();

        if (username != null) {
            context.getPendingDecorations().put(this, new ClientDecorator() {
                public AssertionStatus decorateRequest(PolicyApplicationContext context)
                                                            throws PolicyAssertionException, IOException {
                    DecorationRequirements wssReqs = context.getWssRequirements(data);

                    wssReqs.setUsernameTokenCredentials(new UsernameTokenImpl(username, password));
                    if (!context.getClientSidePolicy().isPlaintextAuthAllowed())
                        context.setSslRequired(true); // force SSL when using WSS basic
                    return AssertionStatus.NONE;
                }
            });

            return AssertionStatus.NONE;
        } else {
            context.getDefaultAuthenticationContext().setAuthenticationMissing();
            return AssertionStatus.FAILED;            
        }
    }

    public AssertionStatus unDecorateReply(PolicyApplicationContext context) {
        // no action on response
        return AssertionStatus.NONE;
    }

    protected WssBasic data;
}
