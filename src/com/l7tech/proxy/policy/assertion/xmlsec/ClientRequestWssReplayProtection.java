package com.l7tech.proxy.policy.assertion.xmlsec;

import com.l7tech.common.security.xml.WssDecorator;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.xmlsec.RequestWssReplayProtection;
import com.l7tech.proxy.datamodel.PendingRequest;
import com.l7tech.proxy.datamodel.Ssg;
import com.l7tech.proxy.datamodel.SsgKeyStoreManager;
import com.l7tech.proxy.datamodel.SsgResponse;
import com.l7tech.proxy.datamodel.exceptions.*;
import com.l7tech.proxy.policy.assertion.ClientAssertion;
import com.l7tech.proxy.policy.assertion.ClientDecorator;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.logging.Logger;

/**
 * Client side support for RequestWssReplayProtection.  When the Agent sees this assertion, it makes sure that
 * the request includes a signed timestamp.
 *
 * $Id$
 */
public class ClientRequestWssReplayProtection extends ClientAssertion {
    private static final Logger log = Logger.getLogger(ClientRequestWssReplayProtection.class.getName());

    public ClientRequestWssReplayProtection(RequestWssReplayProtection data) {
        this.requestWssReplayProtection = data;
        if (data == null) {
            throw new IllegalArgumentException("security elements is null");
        }
    }

    /**
     * ClientProxy client-side processing of the given request.
     *
     * @param request The request to decorate.
     * @return AssertionStatus.NONE if this Assertion was applied to the request successfully; otherwise, some error code
     */
    public AssertionStatus decorateRequest(PendingRequest request)
            throws OperationCanceledException, BadCredentialsException,
            GeneralSecurityException, IOException, KeyStoreCorruptException, HttpChallengeRequiredException,
            PolicyRetryableException, ClientCertificateException
    {
        request.getCredentials();
        request.prepareClientCertificate();

        // get the client cert and private key
        // We must have credentials to get the private key
        final Ssg ssg = request.getSsg();
        final X509Certificate userCert = SsgKeyStoreManager.getClientCert(ssg);
        final PrivateKey userPrivateKey = SsgKeyStoreManager.getClientCertPrivateKey(ssg);

        // add a pending decoration that will be applied only if the rest of this policy branch succeeds
        request.getPendingDecorations().put(this, new ClientDecorator() {
            public AssertionStatus decorateRequest(PendingRequest request) throws PolicyAssertionException {
                // get the client cert and private key
                // We must have credentials to get the private key
                WssDecorator.DecorationRequirements wssReqs = request.getWssRequirements();
                wssReqs.setSenderCertificate(userCert);
                wssReqs.setSenderPrivateKey(userPrivateKey);
                wssReqs.setSignTimestamp(true);
                return AssertionStatus.NONE;
            }
        });
        return AssertionStatus.NONE;
    }

    public AssertionStatus unDecorateReply(PendingRequest request, SsgResponse response) {
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
