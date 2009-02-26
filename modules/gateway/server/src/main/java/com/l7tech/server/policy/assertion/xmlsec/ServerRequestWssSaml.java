package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.server.cluster.DistributedMessageIdManager;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.message.SecurityKnob;
import com.l7tech.message.XmlKnob;
import com.l7tech.security.saml.SamlConstants;
import com.l7tech.security.token.SamlSecurityToken;
import com.l7tech.security.token.XmlSecurityToken;
import com.l7tech.security.xml.processor.ProcessorResult;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.credential.CredentialFormat;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.xmlsec.RequestWssSaml;
import com.l7tech.policy.assertion.xmlsec.SamlAuthenticationStatement;
import com.l7tech.policy.assertion.xmlsec.SecurityHeaderAddressableSupport;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.server.util.MessageId;
import com.l7tech.server.util.MessageIdManager;
import org.springframework.context.ApplicationContext;
import org.xml.sax.SAXException;
import sun.security.x509.X500Name;

import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class <code>ServerRequestWssSaml</code> represents the server
 * side saml Assertion that validates the SAML requestWssSaml.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class ServerRequestWssSaml extends AbstractServerAssertion implements ServerAssertion {
    private static final long CACHE_ID_EXTRA_TIME_MILLIS = 1000L * 60L * 5L; // cache IDs for at least 5 min extra
    private static final long DEFAULT_EXPIRY = 1000L * 60L * 5L; // 5 minutes
    private RequestWssSaml requestWssSaml;
    private final Logger logger = Logger.getLogger(getClass().getName());
    private ApplicationContext applicationContext;
    private SamlAssertionValidate assertionValidate;
    private final Auditor auditor;

    /**
     * Create the server side saml security policy element
     *
     * @param sa the saml
     */
    public ServerRequestWssSaml(RequestWssSaml sa, ApplicationContext context) {
        super(sa);
        if (sa == null) {
            throw new IllegalArgumentException();
        }
        this.applicationContext = context;
        if (applicationContext == null) {
            throw new IllegalArgumentException("The Application Context is required");
        }

        requestWssSaml = sa;
        assertionValidate = new SamlAssertionValidate(requestWssSaml);
        auditor = new Auditor(this, context, logger);
    }

    /**
     * SSG Server-side processing of the given request.
     *
     * @param context
     * @return AssertionStatus.NONE if this Assertion did its business successfully; otherwise, some error code
     * @throws com.l7tech.policy.assertion.PolicyAssertionException
     *          something is wrong in the policy dont throw this if there is an issue with the request or the response
     */
    public AssertionStatus checkRequest(PolicyEnforcementContext context)
      throws IOException, PolicyAssertionException {
        if (!SecurityHeaderAddressableSupport.isLocalRecipient(assertion)) {
            auditor.logAndAudit(AssertionMessages.REQUESTWSS_NOT_FOR_US);
            return AssertionStatus.NONE;
        }

        try {
            final XmlKnob xmlKnob = context.getRequest().getXmlKnob();
            final SecurityKnob secKnob = context.getRequest().getSecurityKnob();
            if (!context.getRequest().isSoap()) {
                auditor.logAndAudit(AssertionMessages.SAML_AUTHN_STMT_REQUEST_NOT_SOAP);
                return AssertionStatus.NOT_APPLICABLE;
            }

            ProcessorResult wssResults = secKnob.getProcessorResult();
            if (wssResults == null) {
                auditor.logAndAudit(AssertionMessages.SAML_AUTHN_STMT_NO_TOKENS_PROCESSED);
                context.setAuthenticationMissing();
                return AssertionStatus.AUTH_REQUIRED;
            }

            XmlSecurityToken[] tokens = wssResults.getXmlSecurityTokens();
            if (tokens == null) {
                auditor.logAndAudit(AssertionMessages.SAML_AUTHN_STMT_NO_TOKENS_PROCESSED);
                context.setAuthenticationMissing();
                return AssertionStatus.AUTH_REQUIRED;
            }
            SamlSecurityToken samlAssertion = null;
            for (int i = 0; i < tokens.length; i++) {
                XmlSecurityToken tok = tokens[i];
                if (tok instanceof SamlSecurityToken) {
                    SamlSecurityToken samlToken = (SamlSecurityToken)tok;
                    if (samlAssertion != null) {
                        auditor.logAndAudit(AssertionMessages.SAML_AUTHN_STMT_MULTIPLE_SAML_ASSERTIONS_UNSUPPORTED);
                        return AssertionStatus.BAD_REQUEST;
                    }
                    samlAssertion = samlToken;
                }
            }
            boolean correctVersion = false;
            boolean requestIsVersion1 = samlAssertion != null && samlAssertion.getVersionId()==SamlSecurityToken.VERSION_1_1;
            boolean anyVersionAllowed = requestWssSaml.getVersion() != null && requestWssSaml.getVersion().intValue()==0;
            boolean requireVersion1 = requestWssSaml.getVersion() == null || requestWssSaml.getVersion().intValue()==1;
            boolean requireVersion2 = requestWssSaml.getVersion() != null && requestWssSaml.getVersion().intValue()==2;
            if (requestIsVersion1 &&  (anyVersionAllowed || requireVersion1) ) {
                correctVersion = true;
            } else if (!requestIsVersion1 && (anyVersionAllowed || requireVersion2)) {
                correctVersion = true;
            }
            if (samlAssertion==null || !correctVersion) {
                auditor.logAndAudit(AssertionMessages.SAML_AUTHN_STMT_NO_ACCEPTABLE_SAML_ASSERTION);
                context.setAuthenticationMissing();
                return AssertionStatus.AUTH_REQUIRED;
            }
            Collection validateResults = new ArrayList();
            LoginCredentials credentials = context.getLastCredentials();
            assertionValidate.validate(xmlKnob.getDocumentReadOnly(), credentials, wssResults, validateResults);
            if (validateResults.size() > 0) {
                StringBuffer sb2 = new StringBuffer();
                boolean firstPass = true;
                for (Iterator iterator = validateResults.iterator(); iterator.hasNext();) {
                    if (!firstPass) {
                        sb2.append(", ");
                    }
                    SamlAssertionValidate.Error error = (SamlAssertionValidate.Error)iterator.next();
                    sb2.append(error.toString());
                    firstPass = false;
                }
                String error = "SAML Assertion Validation Errors:" + sb2.toString();
                auditor.logAndAudit(AssertionMessages.SAML_STMT_VALIDATE_FAILED, new String[] {sb2.toString()});
                logger.log(Level.INFO, error);
                return AssertionStatus.FALSIFIED;
            }

            // enforce one time use condition if requested
            if (samlAssertion.isOneTimeUse()) {
                long expires = samlAssertion.getExpires() == null ?
                        System.currentTimeMillis() + DEFAULT_EXPIRY :
                        samlAssertion.getExpires().getTimeInMillis();
                MessageId messageId = new MessageId(SamlConstants.NS_SAML_PREFIX + "-" + samlAssertion.getUniqueId(), expires + CACHE_ID_EXTRA_TIME_MILLIS);
                try {
                    DistributedMessageIdManager dmm = (DistributedMessageIdManager)applicationContext.getBean("distributedMessageIdManager");
                    dmm.assertMessageIdIsUnique(messageId);
                } catch (MessageIdManager.DuplicateMessageIdException e) {
                    auditor.logAndAudit(AssertionMessages.SAML_STMT_VALIDATE_FAILED, new String[] {"Replay of assertion that is for OneTimeUse."});
                    return AssertionStatus.FALSIFIED;
                }
            }

            String nameIdentifier = null;
            X509Certificate subjectCertificate = samlAssertion.getSubjectCertificate();
            if (subjectCertificate != null) {
                X500Name x500name = new X500Name(subjectCertificate.getSubjectX500Principal().getName());
                nameIdentifier = x500name.getCommonName();
            } else if (SamlConstants.NAMEIDENTIFIER_X509_SUBJECT.equals(samlAssertion.getNameIdentifierFormat())) {
                   X500Name x500name = new X500Name(samlAssertion.getNameIdentifierValue());
                   nameIdentifier = x500name.getCommonName();
            } else {
              nameIdentifier = samlAssertion.getNameIdentifierValue();
            }

            context.addCredentials(new LoginCredentials(nameIdentifier,
                                                        null,
                                                        CredentialFormat.SAML,
                                                        SamlAuthenticationStatement.class,
                                                        null,
                                                        samlAssertion));
            return AssertionStatus.NONE;
        } catch (SAXException e) {
            throw (IOException)new IOException().initCause(e);
        }
    }
}
