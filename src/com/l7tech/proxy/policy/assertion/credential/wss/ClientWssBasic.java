/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.policy.assertion.credential.wss;

import com.l7tech.common.security.xml.decorator.DecorationRequirements;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.credential.wss.WssBasic;
import com.l7tech.proxy.datamodel.exceptions.OperationCanceledException;
import com.l7tech.proxy.message.PolicyApplicationContext;
import com.l7tech.proxy.policy.assertion.ClientDecorator;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.security.cert.CertificateException;
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
        this.data = data;
    }

    /**
     * Decorate the xml soap message with a WSS header containing the username and password.
     *
     * @param context
     * @return
     */
    public AssertionStatus decorateRequest(PolicyApplicationContext context)
            throws OperationCanceledException, IOException, SAXException
    {
        if (context.getSsg().isFederatedGateway()) {
            log.log(Level.INFO, "Plaintext passwords not permitted with Federated Gateway.  Assertion therefore fails.");
            return AssertionStatus.FAILED;
        }

        // get the username and passwords
        final String username = context.getUsername();
        final char[] password = context.getPassword();

        context.getPendingDecorations().put(this, new ClientDecorator() {
            public AssertionStatus decorateRequest(PolicyApplicationContext context)
                                                        throws PolicyAssertionException, IOException {
                DecorationRequirements wssReqs;
                if (data.getRecipientContext().localRecipient()) {
                    wssReqs = context.getDefaultWssRequirements();
                } else {
                    try {
                        wssReqs = context.getAlternateWssRequirements(data.getRecipientContext());
                    } catch (CertificateException e) {
                        throw new PolicyAssertionException("cannot initialize recipient", e);
                    }
                }

                wssReqs.setUsernameTokenCredentials(new LoginCredentials(username, password, WssBasic.class));
                if (!context.getClientSidePolicy().isPlaintextAuthAllowed())
                    context.setSslRequired(true); // force SSL when using WSS basic
                return AssertionStatus.NONE;
            }
        });

        return AssertionStatus.NONE;
    }

    public AssertionStatus unDecorateReply(PolicyApplicationContext context) {
        // no action on response
        return AssertionStatus.NONE;
    }

    /**
     * @return the human-readable node name that is displayed.
     */
    public String getName() {
        return "Require WS Token Basic Authentication";
    }

    /**
     * subclasses override this method specifying the resource name of the
     * icon to use when this assertion is displayed in the tree view.
     *
     * @param open for nodes that can be opened, can have children
     * @return a string such as "com/l7tech/proxy/resources/tree/assertion.png"
     */
    public String iconResource(boolean open) {
        return "com/l7tech/proxy/resources/tree/authentication.gif";
    }

    protected WssBasic data;
}
