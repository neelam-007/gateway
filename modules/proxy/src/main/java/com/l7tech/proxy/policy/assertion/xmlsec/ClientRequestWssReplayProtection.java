package com.l7tech.proxy.policy.assertion.xmlsec;

import com.l7tech.security.xml.decorator.DecorationRequirements;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.xmlsec.WssReplayProtection;
import com.l7tech.proxy.datamodel.Ssg;
import com.l7tech.proxy.datamodel.exceptions.*;
import com.l7tech.proxy.message.PolicyApplicationContext;
import com.l7tech.proxy.policy.assertion.ClientDecorator;
import com.l7tech.proxy.policy.assertion.ClientAssertionWithMetaSupport;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

/**
 * Client side support for RequestWssReplayProtection.  When the Bridge sees this assertion, it makes sure that
 * the request includes a signed timestamp.
 */
public class ClientRequestWssReplayProtection extends ClientAssertionWithMetaSupport {
    public ClientRequestWssReplayProtection(WssReplayProtection data) {
        super(data);
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
    @Override
    public AssertionStatus decorateRequest(PolicyApplicationContext context)
            throws OperationCanceledException, BadCredentialsException,
            GeneralSecurityException, IOException, KeyStoreCorruptException, HttpChallengeRequiredException,
            PolicyRetryableException, ClientCertificateException
    {
        // Not applicable if target is not request
        if ( !Assertion.isRequest(requestWssReplayProtection) || requestWssReplayProtection.isCustomProtection() ) return AssertionStatus.NONE;
        context.getSsg().getServerCertificateAlways();

        // add a pending decoration that will be applied only if the rest of this policy branch succeeds
        context.getPendingDecorations().put(this, new ClientDecorator() {
            @Override
            public AssertionStatus decorateRequest(PolicyApplicationContext context)
                    throws OperationCanceledException, GeneralSecurityException,
                    KeyStoreCorruptException, BadCredentialsException, ClientCertificateException,
                    PolicyRetryableException, HttpChallengeRequiredException
            {
                DecorationRequirements wssReqs = context.getWssRequirements(requestWssReplayProtection);

                // Prepare a client cert if we haven't seen a signature source yet.
                // not necessary for any policy that passed the policy validator
                // todo fla , look into all wss requirements (not just default ones)
                if (!wssReqs.hasSignatureSource())
                    context.prepareClientCertificate();

                // get the client cert and private key
                // We must have credentials to get the private key
                wssReqs.setSignTimestamp(true);

                // If we still haven't yet seen a signature method, assume a WSS signature.
                // LYONSM: This behavior is controversial -- it has been suggested that we should simply have
                //         the policy validator should flag this instead. I'm coming around to this point of view.
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
                    wssReqs.setSignTimestamp(true);
                }

                return AssertionStatus.NONE;
            }
        });
        return AssertionStatus.NONE;
    }

    @Override
    public AssertionStatus unDecorateReply(PolicyApplicationContext context) {
        // no action on response
        return AssertionStatus.NONE;
    }

    protected WssReplayProtection requestWssReplayProtection;
}
