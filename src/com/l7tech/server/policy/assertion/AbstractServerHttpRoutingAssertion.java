package com.l7tech.server.policy.assertion;

import java.util.logging.Logger;
import java.util.logging.Level;
import java.security.cert.CertificateException;
import java.security.SignatureException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.net.URL;
import java.net.MalformedURLException;

import org.springframework.context.ApplicationContext;
import org.xml.sax.SAXException;
import org.w3c.dom.Document;

import com.l7tech.policy.assertion.HttpRoutingAssertion;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.common.audit.Auditor;
import com.l7tech.common.audit.AssertionMessages;
import com.l7tech.common.security.saml.SamlAssertionGenerator;
import com.l7tech.common.security.saml.SubjectStatement;
import com.l7tech.common.security.xml.SignerInfo;
import com.l7tech.common.message.TcpKnob;
import com.l7tech.server.message.PolicyEnforcementContext;

/**
 * Base class for Server HTTP routing assertions
 *
 * @author Steve Jones
 */
public abstract class AbstractServerHttpRoutingAssertion<HRAT extends HttpRoutingAssertion> extends ServerRoutingAssertion<HRAT> {

    //- PROTECTED

    protected final Auditor auditor;

    /**
     * Create a new AbstractServerHttpRoutingAssertion.
     *
     * @param assertion The assertion data.
     * @param applicationContext The spring application context.
     * @param logger The logger to use.
     */
    protected AbstractServerHttpRoutingAssertion(final HRAT assertion,
                                                 final ApplicationContext applicationContext,
                                                 final Logger logger) {
        super(assertion, applicationContext);
        this.logger = logger;
        this.auditor = new Auditor(this, applicationContext, logger);
    }

    /**
     * Get the connection timeout.
     *
     * <p>This is either from the assertion data or the system default.</p>
     *
     * @return The connection timeout in millis
     */
    protected int getConnectionTimeout() {
        Integer timeout = validateTimeout(data.getConnectionTimeout());

        if (timeout == null)
            timeout = super.getConnectionTimeout();

        return timeout;
    }

    /**
     * Get the timeout.
     *
     * <p>This is either from the assertion data or the system default.</p>
     *
     * @return The timeout in millis
     */
    protected int getTimeout() {
        Integer timeout = validateTimeout(data.getTimeout());

        if (timeout == null)
            timeout = super.getTimeout();

        return timeout;
    }

    /**
     * Get the maximum number of connections to each host
     *
     * @return The max
     */
    protected int getMaxConnectionsPerHost() {
        return data.getMaxConnections();
    }

    /**
     * Get the maximum number of connections to all hosts
     *
     * @return The max
     */
    protected int getMaxConnectionsAllHosts() {
        return data.getMaxConnections() * 10;
    }

    /**
     * Validate the given addresses as URL hosts.
     *
     * @param addrs The addresses to validate
     * @return true if all addresses are valid
     */
    protected boolean areValidUrlHostnames(String[] addrs) {
        for (String addr : addrs) {
            try {
                new URL("http", addr, 777, "/foo/bar");
            } catch (MalformedURLException e) {
                auditor.logAndAudit(AssertionMessages.IP_ADDRESS_INVALID, new String[]{addr});
                return false;
            }
        }
        return true;
    }

    /**
     * Attach a sender-vouches SAML assertion to the request.
     *
     * @param context The pec to use
     * @param signerInfo For signing the assertion / message
     * @throws SAXException If the request is not XML
     * @throws IOException If there is an error getting the request document
     * @throws SignatureException If an error occurs when signing
     * @throws CertificateException If the signing certificate is invalid.
     */
    protected void doAttachSamlSenderVouches(PolicyEnforcementContext context, SignerInfo signerInfo)
            throws SAXException, IOException, SignatureException, CertificateException {
        LoginCredentials svInputCredentials = context.getCredentials();
        if (svInputCredentials == null) {
            auditor.logAndAudit(AssertionMessages.HTTPROUTE_SAML_SV_NOT_AUTH);
        } else {
            Document document = context.getRequest().getXmlKnob().getDocumentWritable();
            SamlAssertionGenerator ag = new SamlAssertionGenerator(signerInfo);
            SamlAssertionGenerator.Options samlOptions = new SamlAssertionGenerator.Options();
            samlOptions.setAttestingEntity(signerInfo);
            TcpKnob requestTcp = (TcpKnob)context.getRequest().getKnob(TcpKnob.class);
            if (requestTcp != null) {
                try {
                    InetAddress clientAddress = InetAddress.getByName(requestTcp.getRemoteAddress());
                    samlOptions.setClientAddress(clientAddress);
                } catch (UnknownHostException e) {
                    auditor.logAndAudit(AssertionMessages.HTTPROUTE_CANT_RESOLVE_IP, null, e);
                }
            }
            samlOptions.setVersion(data.getSamlAssertionVersion());
            samlOptions.setExpiryMinutes(data.getSamlAssertionExpiry());
            samlOptions.setUseThumbprintForSignature(data.isUseThumbprintInSamlSignature());
            SubjectStatement statement = SubjectStatement.createAuthenticationStatement(
                                                            svInputCredentials,
                                                            SubjectStatement.SENDER_VOUCHES,
                                                            data.isUseThumbprintInSamlSubject());
            ag.attachStatement(document, statement, samlOptions);
        }
    }

    /**
     * Validate the given timeout value
     *
     * @param timeout The timeout to check (may be null)
     * @return The timeout if valid, else null
     */
    protected Integer validateTimeout(Integer timeout) {
        Integer value = timeout;

        if (value != null) {
            if (value <= 0 || value > 86400000) { // 1 day in millis
                value = null;
                logger.log(Level.WARNING, "Ignoring out of range timeout {0} (will use system default)", value);
            }
        }

        return value;
    }

    //- PRIVATE

    private final Logger logger;
}
