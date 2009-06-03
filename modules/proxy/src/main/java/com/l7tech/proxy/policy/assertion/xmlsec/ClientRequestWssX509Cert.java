package com.l7tech.proxy.policy.assertion.xmlsec;

import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.xmlsec.RequireWssX509Cert;
import com.l7tech.proxy.datamodel.Ssg;
import com.l7tech.proxy.datamodel.exceptions.*;
import com.l7tech.proxy.message.PolicyApplicationContext;
import com.l7tech.proxy.policy.assertion.ClientAssertion;
import com.l7tech.proxy.policy.assertion.ClientDecorator;
import com.l7tech.security.xml.KeyInfoInclusionType;
import com.l7tech.security.xml.decorator.DecorationRequirements;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.logging.Logger;

/**
 * This assertion means that the request must provide some xml signature.
 */
public class ClientRequestWssX509Cert extends ClientAssertion {
    private final static Logger logger = Logger.getLogger(ClientRequestWssX509Cert.class.getName());
    private RequireWssX509Cert subject;

    private final String PROP_KEYINFOTYPE = this.getClass().getName() + ".keyInfoInclusionType";

    public ClientRequestWssX509Cert(RequireWssX509Cert subject) {
        this.subject = subject;
    }
    
    public AssertionStatus decorateRequest(PolicyApplicationContext context) throws BadCredentialsException,
            OperationCanceledException, GeneralSecurityException, ClientCertificateException,
            IOException, SAXException, KeyStoreCorruptException, HttpChallengeRequiredException,
            PolicyRetryableException, PolicyAssertionException
    {
        context.prepareClientCertificate();

        final Ssg ssg = context.getSsg();
        final PrivateKey userPrivateKey = ssg.getClientCertificatePrivateKey();
        final X509Certificate userCert = ssg.getClientCertificate();

        // add a pending decoration that will be applied only if the rest of this policy branch succeeds
        context.getPendingDecorations().put(this, new ClientDecorator() {
            public AssertionStatus decorateRequest(PolicyApplicationContext context) throws PolicyAssertionException
            {
                DecorationRequirements wssReqs = context.getWssRequirements(subject);
                String stype = ssg.getProperties().get(PROP_KEYINFOTYPE);
                KeyInfoInclusionType type = null;
                if (stype != null) type = KeyInfoInclusionType.valueOf(stype);
                if (type == null) type = KeyInfoInclusionType.CERT;
                wssReqs.setKeyInfoInclusionType(type);
                wssReqs.setSenderMessageSigningCertificate(userCert);
                wssReqs.setSenderMessageSigningPrivateKey(userPrivateKey);
                wssReqs.setSignTimestamp();
                return AssertionStatus.NONE;
            }
        });

        return AssertionStatus.NONE;
    }

    public AssertionStatus unDecorateReply(PolicyApplicationContext context) throws BadCredentialsException,
            OperationCanceledException, GeneralSecurityException, IOException, SAXException,
            ResponseValidationException, KeyStoreCorruptException, PolicyAssertionException
    {
        // no action on response
        return AssertionStatus.NONE;
    }

    public String getName() {
        return "WSS Sign SOAP Request";
    }

    public String iconResource(boolean open) {
        return "com/l7tech/proxy/resources/tree/xmlencryption.gif";
    }
}
