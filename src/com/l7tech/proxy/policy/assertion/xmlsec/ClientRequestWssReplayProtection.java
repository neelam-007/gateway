package com.l7tech.proxy.policy.assertion.xmlsec;

import com.l7tech.common.security.xml.decorator.DecorationRequirements;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.xmlsec.RequestWssReplayProtection;
import com.l7tech.proxy.datamodel.Ssg;
import com.l7tech.proxy.datamodel.exceptions.*;
import com.l7tech.proxy.message.PolicyApplicationContext;
import com.l7tech.proxy.policy.assertion.ClientAssertion;
import com.l7tech.proxy.policy.assertion.ClientDecorator;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

/**
 * Client side support for RequestWssReplayProtection.  When the Bridge sees this assertion, it makes sure that
 * the request includes a signed timestamp.
 *
 * $Id$
 */
public class ClientRequestWssReplayProtection extends ClientAssertion {
    public ClientRequestWssReplayProtection(RequestWssReplayProtection data) {
        this.requestWssReplayProtection = data;
        if (data == null) {
            throw new IllegalArgumentException("security elements is null");
        }
    }

    /**
     * ClientProxy client-side processing of the given request.
     *
     * @param context
     * @return AssertionStatus.NONE if this Assertion was applied to the request successfully; otherwise, some error code
     */
    public AssertionStatus decorateRequest(PolicyApplicationContext context)
            throws OperationCanceledException, BadCredentialsException,
            GeneralSecurityException, IOException, KeyStoreCorruptException, HttpChallengeRequiredException,
            PolicyRetryableException, ClientCertificateException
    {
        // Prepare a client cert if we haven't seen a signature source yet
        // todo fla , look into all wss requirements (not just default ones)
        if (!context.getDefaultWssRequirements().hasSignatureSource())
            context.prepareClientCertificate();

        context.getSsg().getServerCertificateAlways();

        // add a pending decoration that will be applied only if the rest of this policy branch succeeds
        context.getPendingDecorations().put(this, new ClientDecorator() {
            public AssertionStatus decorateRequest(PolicyApplicationContext context)
                    throws OperationCanceledException, GeneralSecurityException,
                           KeyStoreCorruptException, BadCredentialsException
            {
                // get the client cert and private key
                // We must have credentials to get the private key
                DecorationRequirements wssReqs = context.getDefaultWssRequirements();
                wssReqs.setSignTimestamp();

                // If we still haven't yet seen a signature method, assume a WSS signature.
                if (!wssReqs.hasSignatureSource()) {
                    // We'll need a signature source to sign the timestamp.  We'll assume WSS, which may require
                    // getting a client cert.
                    final Ssg ssg = context.getSsg();
                    final PrivateKey userPrivateKey = ssg.getClientCertificatePrivateKey();
                    final X509Certificate userCert = ssg.getClientCertificate();
                    final X509Certificate ssgCert = ssg.getServerCertificateAlways();
                    wssReqs.setRecipientCertificate(ssgCert);
                    wssReqs.setSenderMessageSigningCertificate(userCert);
                    wssReqs.setSenderMessageSigningPrivateKey(userPrivateKey);
                    wssReqs.setSignTimestamp();
                }

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
        return "Request WSS Replay Protection";
    }

    public String iconResource(boolean open) {
        return "com/l7tech/proxy/resources/tree/xmlencryption.gif";
    }

    protected RequestWssReplayProtection requestWssReplayProtection;
}
