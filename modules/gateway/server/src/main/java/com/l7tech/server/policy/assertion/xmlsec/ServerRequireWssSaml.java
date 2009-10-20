package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.message.XmlKnob;
import com.l7tech.message.Message;
import com.l7tech.security.saml.SamlConstants;
import com.l7tech.security.token.SamlSecurityToken;
import com.l7tech.security.token.XmlSecurityToken;
import com.l7tech.security.xml.processor.ProcessorResult;
import com.l7tech.security.xml.SecurityTokenResolver;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.xmlsec.RequireWssSaml;
import com.l7tech.policy.assertion.xmlsec.SecurityHeaderAddressableSupport;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.AuthenticationContext;
import com.l7tech.server.policy.assertion.AbstractMessageTargetableServerAssertion;
import com.l7tech.server.util.MessageId;
import com.l7tech.server.util.MessageIdManager;
import com.l7tech.server.util.WSSecurityProcessorUtils;
import org.springframework.context.ApplicationContext;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class <code>ServerRequestWssSaml</code> represents the server
 * side saml Assertion that validates the SAML requestWssSaml.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class ServerRequireWssSaml<AT extends RequireWssSaml> extends AbstractMessageTargetableServerAssertion<AT> {
    private static final long CACHE_ID_EXTRA_TIME_MILLIS = 1000L * 60L * 5L; // cache IDs for at least 5 min extra
    private static final long DEFAULT_EXPIRY = 1000L * 60L * 5L; // 5 minutes
    private final Logger logger = Logger.getLogger(getClass().getName());
    private final SamlAssertionValidate assertionValidate;
    private final Auditor auditor;
    private final SecurityTokenResolver securityTokenResolver;
    private final MessageIdManager messageIdManager;

    /**
     * Create the server side saml security policy element
     *
     * @param sa the saml
     */
    public ServerRequireWssSaml(AT sa, ApplicationContext context) {
        super(sa,sa);
        if (sa == null) {
            throw new IllegalArgumentException();
        }
        if (context == null) {
            throw new IllegalArgumentException("The Application Context is required");
        }

        assertionValidate = new SamlAssertionValidate(sa);
        auditor = new Auditor(this, context, logger);
        securityTokenResolver = (SecurityTokenResolver)context.getBean("securityTokenResolver", SecurityTokenResolver.class);
        messageIdManager = (MessageIdManager)context.getBean("distributedMessageIdManager", MessageIdManager.class);
    }

    /**
     * SSG Server-side processing of the given request.
     *
     * @param context
     * @return AssertionStatus.NONE if this Assertion did its business successfully; otherwise, some error code
     * @throws com.l7tech.policy.assertion.PolicyAssertionException
     *          something is wrong in the policy dont throw this if there is an issue with the request or the response
     */
    @Override
    public AssertionStatus checkRequest(final PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        if (!SecurityHeaderAddressableSupport.isLocalRecipient(assertion)) {
            auditor.logAndAudit(AssertionMessages.REQUESTWSS_NOT_FOR_US);
            return AssertionStatus.NONE;
        }

        return super.checkRequest( context );
    }

    @Override
    protected AssertionStatus doCheckRequest( final PolicyEnforcementContext context,
                                              final Message message,
                                              final String messageDesc,
                                              final AuthenticationContext authContext ) throws IOException {
        try {
            final XmlKnob xmlKnob = message.getXmlKnob();
            if (!message.isSoap()) {
                auditor.logAndAudit(AssertionMessages.SAML_AUTHN_STMT_REQUEST_NOT_SOAP, messageDesc);
                return AssertionStatus.NOT_APPLICABLE;
            }

            ProcessorResult wssResults;
            if ( isRequest() ) {
                wssResults = message.getSecurityKnob().getProcessorResult();
            } else {
                wssResults = WSSecurityProcessorUtils.getWssResults(message, messageDesc, securityTokenResolver, auditor);
            }
            if (wssResults == null) {
                auditor.logAndAudit(AssertionMessages.SAML_AUTHN_STMT_NO_TOKENS_PROCESSED, messageDesc);
                if (isRequest())
                    context.setAuthenticationMissing();
                return AssertionStatus.AUTH_REQUIRED;
            }

            XmlSecurityToken[] tokens = wssResults.getXmlSecurityTokens();
            if (tokens == null) {
                auditor.logAndAudit(AssertionMessages.SAML_AUTHN_STMT_NO_TOKENS_PROCESSED, messageDesc);
                if (isRequest())
                    context.setAuthenticationMissing();
                return AssertionStatus.AUTH_REQUIRED;
            }
            SamlSecurityToken samlAssertion = null;
            for (XmlSecurityToken tok : tokens) {
                if (tok instanceof SamlSecurityToken) {
                    SamlSecurityToken samlToken = (SamlSecurityToken) tok;
                    if (samlAssertion != null) {
                        auditor.logAndAudit(AssertionMessages.SAML_AUTHN_STMT_MULTIPLE_SAML_ASSERTIONS_UNSUPPORTED, messageDesc);
                        return getBadMessageStatus();
                    }
                    samlAssertion = samlToken;
                }
            }
            boolean correctVersion = false;
            boolean requestIsVersion1 = samlAssertion != null && samlAssertion.getVersionId()==SamlSecurityToken.VERSION_1_1;
            boolean anyVersionAllowed = assertion.getVersion() != null && assertion.getVersion() ==0;
            boolean requireVersion1 = assertion.getVersion() == null || assertion.getVersion() ==1;
            boolean requireVersion2 = assertion.getVersion() != null && assertion.getVersion() ==2;
            if (requestIsVersion1 &&  (anyVersionAllowed || requireVersion1) ) {
                correctVersion = true;
            } else if (!requestIsVersion1 && (anyVersionAllowed || requireVersion2)) {
                correctVersion = true;
            }
            if (samlAssertion==null || !correctVersion) {
                auditor.logAndAudit(AssertionMessages.SAML_AUTHN_STMT_NO_ACCEPTABLE_SAML_ASSERTION);
                if (isRequest())
                    context.setAuthenticationMissing();
                return AssertionStatus.AUTH_REQUIRED;
            }
            Collection validateResults = new ArrayList();
            LoginCredentials credentials = authContext.getLastCredentials();
            assertionValidate.validate(xmlKnob.getDocumentReadOnly(), credentials, wssResults, validateResults);
            if (validateResults.size() > 0) {
                StringBuffer sb2 = new StringBuffer();
                boolean firstPass = true;
                for (Object validateResult : validateResults) {
                    if (!firstPass) {
                        sb2.append(", ");
                    }
                    SamlAssertionValidate.Error error = (SamlAssertionValidate.Error) validateResult;
                    sb2.append(error.toString());
                    firstPass = false;
                }
                String error = "SAML Assertion Validation Errors:" + sb2.toString();
                auditor.logAndAudit(AssertionMessages.SAML_STMT_VALIDATE_FAILED, sb2.toString());
                logger.log(Level.INFO, error);
                return AssertionStatus.FALSIFIED;
            }

            // enforce one time use condition if requested
            if (samlAssertion.isOneTimeUse()) {
                long expires = samlAssertion.getExpires() == null ?
                        System.currentTimeMillis() + DEFAULT_EXPIRY :
                        samlAssertion.getExpires().getTimeInMillis();
                MessageId messageId = new MessageId(encode(SamlConstants.NS_SAML_PREFIX + "-" + samlAssertion.getUniqueId()), expires + CACHE_ID_EXTRA_TIME_MILLIS);
                try {
                    messageIdManager.assertMessageIdIsUnique(messageId);
                } catch (MessageIdManager.DuplicateMessageIdException e) {
                    auditor.logAndAudit(AssertionMessages.SAML_STMT_VALIDATE_FAILED, "Replay of assertion that is for OneTimeUse.");
                    return AssertionStatus.FALSIFIED;
                } catch (MessageIdManager.MessageIdCheckException e) {
                    auditor.logAndAudit(AssertionMessages.SAML_STMT_VALIDATE_FAILED, new String[]{"Error checking for replay of assertion that is for OneTimeUse"}, e);
                    return AssertionStatus.FAILED;
                }
            }

            authContext.addCredentials( LoginCredentials.makeLoginCredentials( samlAssertion, RequireWssSaml.class ) ) ;
            return AssertionStatus.NONE;
        } catch (SAXException e) {
            throw new IOException(e);
        }
    }

    @Override
    protected Auditor getAuditor() {
        return auditor;
    }

    /**
     * Encode BASE64 text for safe use as a message identifier.
     */
    private String encode( final String text ) {
        return text.replace( '/', '-' ).replace( '+', '_' );
    }
}
