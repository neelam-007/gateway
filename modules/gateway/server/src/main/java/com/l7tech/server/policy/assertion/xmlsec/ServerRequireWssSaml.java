package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.message.Message;
import com.l7tech.message.TcpKnob;
import com.l7tech.message.XmlKnob;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.xmlsec.RequireWssSaml;
import com.l7tech.policy.assertion.xmlsec.SecurityHeaderAddressableSupport;
import com.l7tech.security.saml.SamlConstants;
import com.l7tech.security.token.SamlSecurityToken;
import com.l7tech.security.token.XmlSecurityToken;
import com.l7tech.security.xml.SecurityTokenResolver;
import com.l7tech.security.xml.processor.ProcessorResult;
import com.l7tech.server.ServerConfigParams;
import com.l7tech.server.message.AuthenticationContext;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractMessageTargetableServerAssertion;
import com.l7tech.server.util.MessageId;
import com.l7tech.server.util.MessageIdManager;
import com.l7tech.server.util.WSSecurityProcessorUtils;
import com.l7tech.util.Config;
import com.l7tech.util.Pair;
import com.l7tech.util.TimeUnit;
import org.springframework.beans.factory.BeanFactory;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.*;

/**
 * Class <code>ServerRequestWssSaml</code> represents the server
 * side saml Assertion that validates the SAML requestWssSaml.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class ServerRequireWssSaml<AT extends RequireWssSaml> extends AbstractMessageTargetableServerAssertion<AT> {
    private static final long CACHE_ID_EXTRA_TIME_MILLIS = 1000L * 60L * 5L; // cache IDs for at least 5 min extra
    private static final long DEFAULT_EXPIRY = 1000L * 60L * 5L; // 5 minutes
    private final SamlAssertionValidate assertionValidate;
    private final Config config;
    private final SecurityTokenResolver securityTokenResolver;
    private final MessageIdManager messageIdManager;
    private final String[] variablesUsed;

    /**
     * Create the server side saml security policy element
     *
     * @param sa the saml
     */
    public ServerRequireWssSaml(AT sa, BeanFactory beanFactory) {
        super(sa);
        if (sa == null) {
            throw new IllegalArgumentException();
        }

        assertionValidate = new SamlAssertionValidate(sa);
        variablesUsed = sa.getVariablesUsed();
        config = beanFactory.getBean("serverConfig", Config.class);
        securityTokenResolver = beanFactory.getBean("securityTokenResolver", SecurityTokenResolver.class);
        messageIdManager = beanFactory.getBean("distributedMessageIdManager", MessageIdManager.class);
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
            logAndAudit(AssertionMessages.REQUESTWSS_NOT_FOR_US);
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
            if (!message.isSoap()) {
                logAndAudit(AssertionMessages.SAML_AUTHN_STMT_REQUEST_NOT_SOAP, messageDesc);
                return AssertionStatus.NOT_APPLICABLE;
            }

            ProcessorResult wssResults;
            if ( isRequest() && !config.getBooleanProperty( ServerConfigParams.PARAM_WSS_PROCESSOR_LAZY_REQUEST,true) ) {
                wssResults = message.getSecurityKnob().getProcessorResult();
            } else {
                wssResults = WSSecurityProcessorUtils.getWssResults(message, messageDesc, securityTokenResolver, getAudit());
            }
            if (wssResults == null) {
                logAndAudit(AssertionMessages.SAML_AUTHN_STMT_NO_TOKENS_PROCESSED, messageDesc);
                if (isRequest())
                    context.setAuthenticationMissing();
                return AssertionStatus.AUTH_REQUIRED;
            }

            XmlSecurityToken[] tokens = wssResults.getXmlSecurityTokens();
            if (tokens == null) {
                logAndAudit(AssertionMessages.SAML_AUTHN_STMT_NO_TOKENS_PROCESSED, messageDesc);
                if (isRequest())
                    context.setAuthenticationMissing();
                return AssertionStatus.AUTH_REQUIRED;
            }
            SamlSecurityToken samlAssertion = null;
            for (XmlSecurityToken tok : tokens) {
                if (tok instanceof SamlSecurityToken) {
                    SamlSecurityToken samlToken = (SamlSecurityToken) tok;
                    if (samlAssertion != null) {
                        logAndAudit(AssertionMessages.SAML_AUTHN_STMT_MULTIPLE_SAML_ASSERTIONS_UNSUPPORTED, messageDesc);
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
                logAndAudit(AssertionMessages.SAML_AUTHN_STMT_NO_ACCEPTABLE_SAML_ASSERTION);
                if (isRequest())
                    context.setAuthenticationMissing();
                return AssertionStatus.AUTH_REQUIRED;
            }
            final Collection<SamlAssertionValidate.Error> validateResults = new ArrayList<SamlAssertionValidate.Error>();
            final Collection<Pair<String, String[]>> collectAttrValues = new ArrayList<Pair<String, String[]>>();
            final LoginCredentials credentials = authContext.getLastCredentials();
            final Collection<String> clientAddresses = message.getKnob(TcpKnob.class) != null && message.getTcpKnob().getRemoteAddress() != null?
                    Collections.singleton( message.getTcpKnob().getRemoteAddress() ) :
                    null;

            final Map<String, Object> serverVariables = context.getVariableMap( variablesUsed, getAudit() );
            assertionValidate.validate(
                    credentials,
                    wssResults,
                    validateResults,
                    collectAttrValues,
                    clientAddresses,
                    serverVariables,
                    getAudit());
            
            if (validateResults.size() > 0) {
                StringBuilder sb2 = new StringBuilder();
                boolean firstPass = true;
                for (Object validateResult : validateResults) {
                    if (!firstPass) {
                        sb2.append(", ");
                    }
                    SamlAssertionValidate.Error error = (SamlAssertionValidate.Error) validateResult;
                    sb2.append(error.toString());
                    firstPass = false;
                }
                logAndAudit(AssertionMessages.SAML_STMT_VALIDATE_FAILED, sb2.toString());
                return AssertionStatus.FALSIFIED;
            }

            // Enforce to check the expiration of the SAML token against the specified "Maximum Expiry Time".
            final long maxExpiry = assertion.getMaxExpiry();
            if (maxExpiry != 0) {  // "0" means no checking.
                final Calendar now = Calendar.getInstance(SamlAssertionValidate.UTC_TIME_ZONE);

                // Get the issue instant
                final Calendar issueInstant = samlAssertion.getIssueInstant();
                if (issueInstant == null) { // Maybe checking null is redundant, since IssueInstant is a mandatory element in a SAML assertion.
                    logAndAudit(AssertionMessages.SAML_STMT_VALIDATE_FAILED, "SAML Assertion Validation Error: The issue instant is not specified.");
                    return AssertionStatus.FALSIFIED;
                }

                // Get and adjust the lower bound of the checking condition based on the NotBefore clock skew
                Calendar lowerBound = SamlAssertionValidate.adjustNotBefore(issueInstant);

                // Check if the issue instant is in the past or not.
                if (now.before(lowerBound)) {
                    logAndAudit(AssertionMessages.SAML_STMT_VALIDATE_FAILED, "SAML Assertion Validation Error: The issue instant is not in the past.");
                    return AssertionStatus.FALSIFIED;
                }

                // Get the maximum lifetime of the SAML token
                @SuppressWarnings({"UnnecessaryLocalVariable"})
                Calendar tokenMaxExpiryTime = issueInstant;
                if (maxExpiry > TimeUnit.MINUTES.getMultiplier()) {
                    int howmanyMins = (int) (maxExpiry / TimeUnit.MINUTES.getMultiplier());
                    tokenMaxExpiryTime.add(Calendar.MINUTE, howmanyMins);

                    int howmanyMs = (int) (maxExpiry % TimeUnit.MINUTES.getMultiplier());
                    tokenMaxExpiryTime.add(Calendar.MILLISECOND, howmanyMs);
                } else {
                    tokenMaxExpiryTime.add(Calendar.MILLISECOND, (int) maxExpiry);
                }

                // Get and adjust the upper bound of the checking condition based on the NotBefore clock skew
                Calendar upperBound = SamlAssertionValidate.adjustNotAfter(tokenMaxExpiryTime);

                // Check if the SAML token is expired or not.
                if (now.after(upperBound)) {
                    logAndAudit(AssertionMessages.SAML_TOKEN_EXPIRATION_WARNING);
                    return AssertionStatus.FALSIFIED;
                }
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
                    logAndAudit(AssertionMessages.SAML_STMT_VALIDATE_FAILED, "Replay of assertion that is for OneTimeUse.");
                    return AssertionStatus.FALSIFIED;
                } catch (MessageIdManager.MessageIdCheckException e) {
                    logAndAudit(AssertionMessages.SAML_STMT_VALIDATE_FAILED, new String[]{"Error checking for replay of assertion that is for OneTimeUse"}, e);
                    return AssertionStatus.FAILED;
                }
            }

            authContext.addCredentials( LoginCredentials.makeLoginCredentials( samlAssertion, RequireWssSaml.class ) ) ;

            // Record attribute values
            if (!collectAttrValues.isEmpty()) {
                setAttributeContextVariables(context, collectAttrValues);
            }

            return AssertionStatus.NONE;
        } catch (SAXException e) {
            throw new IOException(e);
        }
    }

    void setAttributeContextVariables(PolicyEnforcementContext context, Collection<Pair<String, String[]>> collectAttrValues) {
        // Collect and collate multivalued attrs
        Map<String, List<String>> attrvals = collateAttrValues(collectAttrValues);

        // Publish as context variables
        publishAttrVariables(context, attrvals);
    }

    private static Map<String, List<String>> collateAttrValues(Collection<Pair<String, String[]>> collectAttrValues) {
        Map<String, List<String>> attrvals = new LinkedHashMap<String, List<String>>();
        for (Pair<String, String[]> av : collectAttrValues) {
            final String varname = "saml.attr." + RequireWssSaml.toContextVariableName(av.left.toLowerCase());
            List<String> list = attrvals.get(varname);
            if (list == null)
                list = new ArrayList<String>();
            list.addAll(Arrays.asList(av.right));
            attrvals.put(varname, list);
        }
        return attrvals;
    }

    private static void publishAttrVariables(PolicyEnforcementContext context, Map<String, List<String>> attrvals) {
        for (Map.Entry<String, List<String>> entry : attrvals.entrySet()) {
            final String name = entry.getKey();
            final List<String> vals = entry.getValue();
            context.setVariable(name, vals.toArray(new String[entry.getValue().size()]));
        }
    }

    /**
     * Encode BASE64 text for safe use as a message identifier.
     */
    private String encode( final String text ) {
        return text.replace( '/', '-' ).replace( '+', '_' );
    }
}
