package com.l7tech.proxy.policy.assertion.xmlsec;

import com.l7tech.common.security.xml.decorator.DecorationRequirements;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.xmlsec.RequestWssX509Cert;
import com.l7tech.proxy.datamodel.Ssg;
import com.l7tech.proxy.datamodel.SsgKeyStoreManager;
import com.l7tech.proxy.datamodel.exceptions.*;
import com.l7tech.proxy.message.PolicyApplicationContext;
import com.l7tech.proxy.policy.assertion.ClientAssertion;
import com.l7tech.proxy.policy.assertion.ClientDecorator;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

/**
 * This assertion means that the request must provide some xml signature.
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Jul 14, 2004<br/>
 * $Id$<br/>
 */
public class ClientRequestWssX509Cert extends ClientAssertion {
    private RequestWssX509Cert subject;

    public ClientRequestWssX509Cert(RequestWssX509Cert subject) {
        this.subject = subject;
    }
    
    public AssertionStatus decorateRequest(PolicyApplicationContext context) throws BadCredentialsException,
            OperationCanceledException, GeneralSecurityException, ClientCertificateException,
            IOException, SAXException, KeyStoreCorruptException, HttpChallengeRequiredException,
            PolicyRetryableException, PolicyAssertionException
    {
        context.prepareClientCertificate();

        final Ssg ssg = context.getSsg();
        final PrivateKey userPrivateKey = SsgKeyStoreManager.getClientCertPrivateKey(ssg);
        final X509Certificate userCert = SsgKeyStoreManager.getClientCert(ssg);
        final X509Certificate ssgCert = SsgKeyStoreManager.getServerCert(ssg);

        // add a pending decoration that will be applied only if the rest of this policy branch succeeds
        context.getPendingDecorations().put(this, new ClientDecorator() {
            public AssertionStatus decorateRequest(PolicyApplicationContext context)
            {
                // get the client cert and private key
                // We must have credentials to get the private key
                // todo fla, look at the recipient information of the assertion before assuming it's for default
                // recipient
                DecorationRequirements wssReqs = context.getDefaultWssRequirements();
                wssReqs.setRecipientCertificate(ssgCert);
                wssReqs.setSenderCertificate(userCert);
                wssReqs.setSenderPrivateKey(userPrivateKey);
                wssReqs.setSignTimestamp(true);
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
