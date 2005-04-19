package com.l7tech.proxy.policy.assertion.xmlsec;

import com.l7tech.common.security.xml.decorator.DecorationRequirements;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.xmlsec.RequestWssX509Cert;
import com.l7tech.proxy.datamodel.Ssg;
import com.l7tech.proxy.datamodel.exceptions.*;
import com.l7tech.proxy.message.PolicyApplicationContext;
import com.l7tech.proxy.policy.assertion.ClientAssertion;
import com.l7tech.proxy.policy.assertion.ClientDecorator;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.logging.Level;
import java.util.logging.Logger;

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
    private final static Logger logger = Logger.getLogger(ClientRequestWssX509Cert.class.getName());
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
        final PrivateKey userPrivateKey = ssg.getClientCertificatePrivateKey();
        final X509Certificate userCert = ssg.getClientCertificate();
        final X509Certificate ssgCert = ssg.getServerCertificate();
        if (ssgCert == null)
            throw new ServerCertificateUntrustedException(); // Trigger server cert disco

        // add a pending decoration that will be applied only if the rest of this policy branch succeeds
        context.getPendingDecorations().put(this, new ClientDecorator() {
            public AssertionStatus decorateRequest(PolicyApplicationContext context) throws PolicyAssertionException
            {
                try {
                    DecorationRequirements wssReqs;
                    if (subject.getRecipientContext().localRecipient()) {
                        wssReqs = context.getDefaultWssRequirements();
                        wssReqs.setRecipientCertificate(ssgCert);
                    } else {
                        wssReqs = context.getAlternateWssRequirements(subject.getRecipientContext());
                    }
                    wssReqs.setSenderMessageSigningCertificate(userCert);
                    wssReqs.setSenderMessageSigningPrivateKey(userPrivateKey);
                    wssReqs.setSignTimestamp();
                    return AssertionStatus.NONE;

                } catch (IOException e) {
                    String msg = "Cannot initialize the recipient's  DecorationRequirements";
                    logger.log(Level.WARNING, msg, e);
                    throw new PolicyAssertionException(msg, e);
                } catch (CertificateException e) {
                    String msg = "Cannot initialize the recipient's  DecorationRequirements";
                    logger.log(Level.WARNING, msg, e);
                    throw new PolicyAssertionException(msg, e);
                }
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
