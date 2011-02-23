package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.credential.CredentialFormat;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.xmlsec.AddWssSecurityToken;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.security.token.EncryptedKey;
import com.l7tech.security.token.SamlSecurityToken;
import com.l7tech.security.token.SecurityTokenType;
import com.l7tech.security.token.UsernameTokenImpl;
import com.l7tech.security.xml.KeyInfoDetails;
import com.l7tech.security.xml.SecurityTokenResolver;
import com.l7tech.security.xml.UnexpectedKeyInfoException;
import com.l7tech.security.xml.decorator.DecorationRequirements;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.message.AuthenticationContext;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractMessageTargetableServerAssertion;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.server.secureconversation.SecureConversationSession;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.InvalidDocumentFormatException;
import com.l7tech.util.SoapConstants;
import com.l7tech.xml.saml.SamlAssertion;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Map;
import java.util.logging.Logger;

/**
 * @author alex
 */
public class ServerAddWssSecurityToken extends AbstractMessageTargetableServerAssertion<AddWssSecurityToken> {
    private static final Logger logger = Logger.getLogger(ServerAddWssSecurityToken.class.getName());

    private final Auditor auditor;
    private final String[] variableNames;
    private final AddWssSignatureSupport addWssSignatureSupport;
    private final AddWssEncryptionSupport addWssEncryptionSupport;
    private final SecurityTokenResolver securityTokenResolver;

    public ServerAddWssSecurityToken( final AddWssSecurityToken assertion, final BeanFactory beanFactory, final ApplicationEventPublisher eventPub ) {
        super(assertion, assertion);
        this.auditor = new Auditor(this, beanFactory, eventPub, logger);
        this.variableNames = assertion.getVariablesUsed();
        this.addWssEncryptionSupport = new AddWssEncryptionSupport(auditor, logger, assertion, assertion, assertion);
        this.addWssSignatureSupport = new AddWssSignatureSupport(auditor, assertion, beanFactory, shouldFailIfNoElementsToSign(assertion), Assertion.isResponse(assertion));
        this.securityTokenResolver = beanFactory == null ? null : beanFactory.getBean("securityTokenResolver", SecurityTokenResolver.class);
    }

    @Override
    protected AssertionStatus doCheckRequest(PolicyEnforcementContext context, Message message, String messageDescription, AuthenticationContext authContext)
            throws IOException, PolicyAssertionException
    {
        DecorationRequirements dreq = message.getSecurityKnob().getAlternateDecorationRequirements(assertion.getRecipientContext());
        dreq.setProtectTokens(assertion.isProtectTokens());

        SecurityTokenType tokenType = assertion.getTokenType();
        if (SecurityTokenType.WSS_USERNAME == tokenType) {
            return addUsernameToken(context, message, messageDescription, authContext, dreq);
        } else if (SecurityTokenType.WSSC_CONTEXT == tokenType) {
            return addWsscContext(context, message, messageDescription, authContext, dreq);
        } else if (SecurityTokenType.SAML_ASSERTION == tokenType) {
            return addSamlAssertion(context, message, messageDescription, authContext, dreq);
        } else if (SecurityTokenType.WSS_ENCRYPTEDKEY == tokenType) {
            return addEncryptedKey(context, message, messageDescription, authContext, dreq);
        } else {
            auditor.logAndAudit(AssertionMessages.ADD_WSS_TOKEN_UNSUPPORTED_TYPE, assertion.getTokenType().getName());
            return AssertionStatus.SERVER_ERROR;
        }
    }

    private static boolean shouldFailIfNoElementsToSign(AddWssSecurityToken assertion) {
        return SecurityTokenType.WSS_USERNAME == assertion.getTokenType();
    }

    private AssertionStatus addEncryptedKey(PolicyEnforcementContext context, Message message, String messageDescription, AuthenticationContext authContext, DecorationRequirements dreq) throws IOException, PolicyAssertionException {
        dreq.setPreferredSigningTokenType(DecorationRequirements.PreferredSigningTokenType.ENCRYPTED_KEY);
        dreq.setIncludeTimestamp(true);
        dreq.setSignTimestamp(true);
        try {
            AddWssEncryptionContext encryptionContext = addWssEncryptionSupport.buildEncryptionContext(context);
            addWssEncryptionSupport.applyDecorationRequirements(context, dreq, encryptionContext, null);
        } catch (AddWssEncryptionSupport.MultipleTokensException e) {
            auditor.logAndAudit(AssertionMessages.ADD_WSS_TOKEN_MULTIPLE_REQ_TOKENS);
            // Continue anyway after warning, in case subsequent apply security fills in an appropriate encryption recipient
        }
        return AssertionStatus.NONE;
    }

    private AssertionStatus addSamlAssertion(PolicyEnforcementContext context, Message message, String messageDescription, AuthenticationContext authContext, DecorationRequirements dreq) {
        String assertionVar = assertion.getSamlAssertionVariable();
        if (assertionVar == null || assertionVar.length() < 1) {
            auditor.logAndAudit(AssertionMessages.ASSERTION_MISCONFIGURED, "no SAML assertion variable was provided");
            return AssertionStatus.SERVER_ERROR;
        }

        Element samlElement;
        try {
            final Map<String,Object> vars = context.getVariableMap( variableNames, auditor );
            Object samlVal = ExpandVariables.processSingleVariableAsObject( Syntax.getVariableExpression(assertionVar), vars, auditor );

            if (null == samlVal) {
                auditor.logAndAudit(AssertionMessages.ADD_WSS_TOKEN_NOT_SAML, "SAML variable not found or null");
                return AssertionStatus.SERVER_ERROR;
            }

            // Unwrap singleton array or collection
            if (samlVal instanceof Object[]) {
                Object[] vals = (Object[]) samlVal;
                if (vals.length == 1)
                    samlVal = vals[0];
            } else if (samlVal instanceof Collection) {
                Collection vals = (Collection) samlVal;
                if (vals.size() == 1)
                    samlVal = vals.iterator().next();
            }

            // Deal with Element, Document, String value
            if (samlVal instanceof Element) {
                samlElement = (Element) samlVal;
            } else if (samlVal instanceof Document) {
                samlElement = ((Document) samlVal).getDocumentElement();
            } else if (samlVal instanceof CharSequence) {
                String samlXml = samlVal.toString();
                samlElement = XmlUtil.stringToDocument(samlXml).getDocumentElement();
            } else {
                auditor.logAndAudit(AssertionMessages.ADD_WSS_TOKEN_NOT_SAML, "SAML variable is unsupported type: " + samlVal.getClass().getName());
                return AssertionStatus.SERVER_ERROR;
            }

        } catch (SAXException e) {
            //noinspection ThrowableResultOfMethodCallIgnored
            auditor.logAndAudit(AssertionMessages.ADD_WSS_TOKEN_NOT_SAML, new String[] {ExceptionUtils.getMessage(e)}, ExceptionUtils.getDebugException(e));
            return AssertionStatus.SERVER_ERROR;
        }

        SamlAssertion samlAssertion;
        try {
            samlAssertion = SamlAssertion.newInstance(samlElement, securityTokenResolver);
        } catch (SAXException e) {
            //noinspection ThrowableResultOfMethodCallIgnored
            auditor.logAndAudit(AssertionMessages.ADD_WSS_TOKEN_NOT_SAML, new String[] {ExceptionUtils.getMessage(e)}, ExceptionUtils.getDebugException(e));
            return AssertionStatus.SERVER_ERROR;
        }

        dreq.setSenderSamlToken(samlAssertion, assertion.isProtectTokens());
        dreq.setSenderMessageSigningPrivateKey(addWssSignatureSupport.getSignerInfo().getPrivate());

        if (samlAssertion.isHolderOfKey()) {
            dreq.setPreferredSigningTokenType(DecorationRequirements.PreferredSigningTokenType.SAML_HOK);
        }

        if (samlAssertion.hasSubjectConfirmationEncryptedKey()) {
            // To use this assertion for decoration we'll need to either already have its secret key or else be able to unwrap it
            EncryptedKey encryptedKey = null;
            try {
                encryptedKey = samlAssertion.getSubjectConfirmationEncryptedKey(securityTokenResolver, null);
            } catch (InvalidDocumentFormatException e) {
                auditor.logAndAudit(AssertionMessages.ADD_WSS_TOKEN_SAML_SECRET_KEY_UNAVAILABLE, null, e);
            } catch (UnexpectedKeyInfoException e) {
                auditor.logAndAudit(AssertionMessages.ADD_WSS_TOKEN_SAML_SECRET_KEY_UNAVAILABLE, null, e);
            } catch (GeneralSecurityException e) {
                auditor.logAndAudit(AssertionMessages.ADD_WSS_TOKEN_SAML_SECRET_KEY_UNAVAILABLE, null, e);
            }

            if (encryptedKey != null) {
                try {
                    byte[] secretKeyBytes = encryptedKey.getSecretKey();
                    dreq.setEncryptedKey(secretKeyBytes);
                    String valueTypeUri = SamlSecurityToken.VERSION_2_0 == samlAssertion.getVersionId()
                            ? SoapConstants.VALUETYPE_SAML_ASSERTIONID_SAML20
                            : SoapConstants.VALUETYPE_SAML_ASSERTIONID_SAML11;
                    dreq.setEncryptedKeyReferenceInfo(KeyInfoDetails.makeKeyId(samlAssertion.getAssertionId(), false, valueTypeUri));
                } catch (InvalidDocumentFormatException e) {
                    auditor.logAndAudit(AssertionMessages.ADD_WSS_TOKEN_SAML_SECRET_KEY_UNAVAILABLE, null, e);
                } catch (GeneralSecurityException e) {
                    auditor.logAndAudit(AssertionMessages.ADD_WSS_TOKEN_SAML_SECRET_KEY_UNAVAILABLE, null, e);
                }
            }
        }

        return AssertionStatus.NONE;
    }

    private AssertionStatus addWsscContext(PolicyEnforcementContext context, Message message, String messageDescription, AuthenticationContext authContext, DecorationRequirements dreq) {
        String wsscVarName = assertion.getWsscSessionVariable();
        if (wsscVarName == null || wsscVarName.length() < 1) {
            auditor.logAndAudit(AssertionMessages.ASSERTION_MISCONFIGURED, "no secure conversation session variable name was provided");
            return AssertionStatus.SERVER_ERROR;
        }

        final Map<String,Object> vars = context.getVariableMap( variableNames, auditor );
        final Object wsscVal = ExpandVariables.processSingleVariableAsObject( Syntax.getVariableExpression(wsscVarName), vars, auditor );
        if ( wsscVal == null ) {
            auditor.logAndAudit(AssertionMessages.NO_SUCH_VARIABLE_WARNING, wsscVarName);
            return AssertionStatus.SERVER_ERROR;
        }

        if (!(wsscVal instanceof SecureConversationSession)) {
            auditor.logAndAudit(AssertionMessages.ADD_WSS_TOKEN_NOT_SESSION);
            return AssertionStatus.SERVER_ERROR;
        }

        SecureConversationSession session = (SecureConversationSession) wsscVal;
        dreq.setSecureConversationSession(session);
        dreq.setOmitSecurityContextToken(assertion.isOmitSecurityContextToken());
        return AssertionStatus.NONE;
    }

    private AssertionStatus addUsernameToken(PolicyEnforcementContext context, Message message, String messageDescription, AuthenticationContext authContext, DecorationRequirements dreq) throws IOException, PolicyAssertionException {
        if (assertion.isUseLastGatheredCredentials()) {
            return applyUsernameTokenGatheredCredentialsDecorationRequirements(context, message, messageDescription, authContext);
        }

        final boolean signing = assertion.isProtectTokens();
        dreq.setSignUsernameToken(signing);
        if (signing) {
            // Ensure some signing key is confiured (Bug #9677)
            addWssSignatureSupport.applySignatureDecorationRequirements(context, message, messageDescription, authContext, new AddWssSignatureSupport.SignedElementSelector() {
                @Override
                public int selectElementsToSign(PolicyEnforcementContext context, AuthenticationContext authContext, Document soapmsg, DecorationRequirements wssReq, Message targetMessage) throws PolicyAssertionException {
                    return 1;
                }
            });
        }
        return ServerAddWssUsernameToken.applyUsernameTokenSpecifiedCredentialsDecorationRequirements(addWssEncryptionSupport, context, message, messageDescription, assertion.getRecipientContext(),
                assertion.getUsername(), assertion.isIncludePassword() ? assertion.getPassword() : null, variableNames,
                assertion.isIncludeCreated(), assertion.isEncrypt(), assertion.isIncludeNonce(), assertion.isDigest(), this);
    }

    private AssertionStatus applyUsernameTokenGatheredCredentialsDecorationRequirements(PolicyEnforcementContext context, Message message, String messageDescription, AuthenticationContext authContext)
            throws PolicyAssertionException, IOException {
        AddWssSignatureSupport.SignedElementSelector signedElementSelector = new AddWssSignatureSupport.SignedElementSelector() {
            @Override
            public int selectElementsToSign(PolicyEnforcementContext context, AuthenticationContext authContext, Document soapmsg, DecorationRequirements wssReq, Message targetMessage)
                    throws PolicyAssertionException
            {
                LoginCredentials creds = context.getDefaultAuthenticationContext().getLastCredentials();
                String name = creds == null ? null : creds.getLogin();
                char[] pass = null;
                if (creds != null && creds.getFormat() == CredentialFormat.CLEARTEXT) {
                    pass = creds.getCredentials();
                } else {
                    Object payload = creds == null ? null : creds.getPayload();
                    if (payload instanceof X509Certificate) {
                        X509Certificate x509Certificate = (X509Certificate) payload;
                        name = x509Certificate.getSubjectDN().getName();
                    } else if (payload instanceof SamlAssertion) {
                        SamlAssertion samlAssertion = (SamlAssertion) payload;
                        name = samlAssertion.getNameIdentifierValue();
                        if (name == null) {
                            X509Certificate cert = samlAssertion.getSubjectCertificate();
                            if (cert != null) name = cert.getSubjectDN().getName();
                        }
                    }
                }

                if (name == null) {
                    auditor.logAndAudit(AssertionMessages.ADD_WSS_TOKEN_NO_USERNAME);
                    return -1;
                }

                if (assertion.isIncludePassword()) {
                    if (pass == null) {
                        auditor.logAndAudit(AssertionMessages.ADD_WSS_TOKEN_NO_PASSWORD);
                        return -1;
                    }
                } else {
                    pass = null;
                }
                wssReq.setUsernameTokenCredentials(new UsernameTokenImpl(name, pass));
                if (assertion.isProtectTokens()) {
                    wssReq.setSignUsernameToken(true);
                    wssReq.setSignTimestamp(true);
                }
                return 1;
            }
        };
        return addWssSignatureSupport.applySignatureDecorationRequirements(context, message, messageDescription, authContext, signedElementSelector);
    }

    @Override
    protected Audit getAuditor() {
        return auditor;
    }
}
