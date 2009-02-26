package com.l7tech.proxy.policy.assertion.xmlsec;

import com.l7tech.security.xml.decorator.DecorationRequirements;
import com.l7tech.kerberos.KerberosServiceTicket;
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
import java.util.logging.Logger;

/**
 * This assertion means that the request must provide a kerberos ticket for the service/gateway.
 */
public class ClientRequestWssKerberos extends ClientAssertion {
    private final RequestWssKerberos data;

    //- PUBLIC

    public ClientRequestWssKerberos(RequestWssKerberos requestWssKerberos) {
        this.data = requestWssKerberos;
    }

    public AssertionStatus decorateRequest(PolicyApplicationContext context) throws BadCredentialsException,
            OperationCanceledException, GeneralSecurityException, ClientCertificateException,
            IOException, SAXException, KeyStoreCorruptException, HttpChallengeRequiredException,
            PolicyRetryableException, PolicyAssertionException
    {
        final Ssg ssg = context.getSsg();

        final String kerberosId = context.getKerberosServiceTicketId();
        final KerberosServiceTicket ticket = kerberosId!=null ? context.getExistingKerberosServiceTicket() : context.getKerberosServiceTicket();

        // add a pending decoration that will be applied only if the rest of this policy branch succeeds
        context.getPendingDecorations().put(this, new ClientDecorator() {
            public AssertionStatus decorateRequest(PolicyApplicationContext context) throws PolicyAssertionException
            {
                DecorationRequirements wssReqs = context.getWssRequirements(data);

                if(kerberosId!=null) {
                    context.setUsedKerberosServiceTicketReference(true);
                    wssReqs.setIncludeKerberosTicketId(true);
                    wssReqs.setKerberosTicketId(kerberosId);
                    wssReqs.setKerberosTicket(ticket); // needed if signing or encrypting
                }
                else {
                    context.setUsedKerberosServiceTicketReference(false);
                    wssReqs.setIncludeKerberosTicket(true);
                    wssReqs.setKerberosTicket(ticket);
                }
                return AssertionStatus.NONE;
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

}
