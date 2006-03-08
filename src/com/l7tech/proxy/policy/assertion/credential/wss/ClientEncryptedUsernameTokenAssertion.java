/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.policy.assertion.credential.wss;

import com.l7tech.common.security.token.UsernameTokenImpl;
import com.l7tech.common.security.xml.decorator.DecorationRequirements;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.credential.wss.EncryptedUsernameTokenAssertion;
import com.l7tech.proxy.datamodel.exceptions.OperationCanceledException;
import com.l7tech.proxy.datamodel.exceptions.ServerCertificateUntrustedException;
import com.l7tech.proxy.message.PolicyApplicationContext;
import com.l7tech.proxy.policy.assertion.ClientDecorator;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Decorates a request with an ephemeral EncryptedKey addressed to the server, and a UsernameToken
 * which is signed and encrypted using this ephemeral key.  Other elements can then be signed
 * and encrypted as well.  No client certificate is required, only a regular UsernameToken.
 */
public class ClientEncryptedUsernameTokenAssertion extends ClientWssCredentialSource {
    public static final Logger log = Logger.getLogger(ClientEncryptedUsernameTokenAssertion.class.getName());

    public ClientEncryptedUsernameTokenAssertion( EncryptedUsernameTokenAssertion data ) {
        this.data = data;
    }

    public AssertionStatus decorateRequest(PolicyApplicationContext context)
            throws OperationCanceledException, IOException, SAXException,
            PolicyAssertionException, ServerCertificateUntrustedException
    {
        if (context.getSsg().isFederatedGateway()) {
            log.log(Level.INFO, "Plaintext passwords not permitted with Federated Gateway.  Assertion therefore fails.");
            return AssertionStatus.FAILED;
        }

        final X509Certificate serverCert = context.getSsg().getServerCertificateAlways();

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
                        throw new PolicyAssertionException(data, "cannot initialize recipient", e);
                    }
                }

                wssReqs.setUsernameTokenCredentials(new UsernameTokenImpl(username, password));
                wssReqs.setRecipientCertificate(serverCert);
                wssReqs.setEncryptUsernameToken(true);
                wssReqs.setUseDerivedKeys(true);

                return AssertionStatus.NONE;
            }
        });

        return AssertionStatus.NONE;
    }

    public AssertionStatus unDecorateReply(PolicyApplicationContext context) {
        // no action on response
        return AssertionStatus.NONE;
    }

    public String getName() {
        return "Require Encrypted UsernameToken Authentication";
    }

    public String iconResource(boolean open) {
        return "com/l7tech/proxy/resources/tree/authentication.gif";
    }

    protected EncryptedUsernameTokenAssertion data;
}
