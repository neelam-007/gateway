package com.l7tech.proxy.policy.assertion.xmlsec;

import com.l7tech.common.security.xml.decorator.DecorationRequirements;
import com.l7tech.common.security.kerberos.KerberosClient;
import com.l7tech.common.security.kerberos.KerberosException;
import com.l7tech.common.security.kerberos.KerberosServiceTicket;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.xmlsec.RequestWssKerberos;
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
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.NameCallback;

/**
 * This assertion means that the request must provide a kerberos ticket for the service/gateway.
 *
 * @author $Author$
 * @version $Version: $
 */
public class ClientRequestWssKerberos extends ClientAssertion {

    //- PUBLIC

    public ClientRequestWssKerberos(RequestWssKerberos requestWssKerberos) {
        requestKerberos = requestWssKerberos;
    }

    public AssertionStatus decorateRequest(PolicyApplicationContext context) throws BadCredentialsException,
            OperationCanceledException, GeneralSecurityException, ClientCertificateException,
            IOException, SAXException, KeyStoreCorruptException, HttpChallengeRequiredException,
            PolicyRetryableException, PolicyAssertionException
    {
        final Ssg ssg = context.getSsg();

        // add a pending decoration that will be applied only if the rest of this policy branch succeeds
        context.getPendingDecorations().put(this, new ClientDecorator() {
            public AssertionStatus decorateRequest(PolicyApplicationContext context) throws PolicyAssertionException
            {
                DecorationRequirements wssReqs = context.getDefaultWssRequirements();

                try {
                    KerberosClient client = new KerberosClient();
                    client.setCallbackHandler(new CallbackHandler(){
                        public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
                            for (int i = 0; i < callbacks.length; i++) {
                                Callback callback = callbacks[i];
                                if(callback instanceof PasswordCallback) {
                                    PasswordCallback pc = (PasswordCallback) callback;
                                    pc.setPassword(ssg.getRuntime().getCredentials().getPassword());
                                }
                                else if(callback instanceof NameCallback) {
                                    NameCallback nc = (NameCallback) callback;
                                    nc.setName(ssg.getRuntime().getCredentials().getUserName());
                                }
                            }
                        }
                    });
                    KerberosServiceTicket kst = client.getKerberosServiceTicket(requestKerberos.getServicePrincipalName());

                    wssReqs.setIncludeKerberosTicket(true);
                    wssReqs.setKerberosTicket(kst.getGSSAPReqTicket());
                    return AssertionStatus.NONE;
                }
                catch(KerberosException ke) {
                    logger.log(Level.WARNING, "Error getting ticket", ke);
                    return AssertionStatus.FALSIFIED;
                }
            }
        });

        return AssertionStatus.NONE;
    }

    public AssertionStatus unDecorateReply(PolicyApplicationContext context) throws BadCredentialsException,
            OperationCanceledException, GeneralSecurityException, IOException, SAXException,
            ResponseValidationException, KeyStoreCorruptException, PolicyAssertionException
    {
        // no action on response (if we did mutual auth it would go here)
        return AssertionStatus.NONE;
    }

    public String getName() {
        return "Kerberos Ticket";
    }

    public String iconResource(boolean open) {
        return "com/l7tech/proxy/resources/tree/xmlencryption.gif";
    }

    //- PRIVATE

    private final static Logger logger = Logger.getLogger(ClientRequestWssKerberos.class.getName());
    private RequestWssKerberos requestKerberos;

}
