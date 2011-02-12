package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.identity.User;
import com.l7tech.message.HttpRequestKnob;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.xmlsec.SecureConversation;
import com.l7tech.policy.assertion.xmlsec.SecurityHeaderAddressableSupport;
import com.l7tech.security.token.SecurityContextToken;
import com.l7tech.security.token.XmlSecurityToken;
import com.l7tech.security.xml.SecurityTokenResolver;
import com.l7tech.security.xml.decorator.DecorationRequirements;
import com.l7tech.security.xml.processor.BadSecurityContextException;
import com.l7tech.security.xml.processor.ProcessorResult;
import com.l7tech.security.xml.processor.SecurityContextFinder;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.message.AuthenticationContext;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.policy.assertion.AssertionStatusException;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.server.secureconversation.InboundSecureConversationContextManager;
import com.l7tech.server.secureconversation.SecureConversationSession;
import com.l7tech.server.util.WSSecurityProcessorUtils;
import com.l7tech.util.CausedIOException;
import com.l7tech.util.Config;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;
import com.l7tech.util.InvalidDocumentFormatException;
import com.l7tech.xml.SoapFaultLevel;
import com.l7tech.xml.soap.SoapFaultUtils;
import com.l7tech.xml.soap.SoapUtil;
import com.l7tech.xml.soap.SoapVersion;
import org.springframework.context.ApplicationContext;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * The SSG-side processing of the SecureConversation assertion.
 *
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Aug 4, 2004<br/>
 */
public class ServerSecureConversation extends AbstractServerAssertion<SecureConversation> {
    private final Auditor auditor;
    private final Config config;
    private final SecurityTokenResolver securityTokenResolver;
    private final SecurityContextFinder securityContextFinder;
    private final InboundSecureConversationContextManager inboundSecureConversationContextManager;

    public ServerSecureConversation(SecureConversation assertion, ApplicationContext springContext) {
        super(assertion);
        // nothing to remember from the passed assertion
        this.auditor = new Auditor(this, springContext, logger);
        this.config = springContext.getBean("serverConfig", Config.class);
        this.securityTokenResolver = springContext.getBean("securityTokenResolver", SecurityTokenResolver.class);
        this.securityContextFinder = springContext.getBean("securityContextFinder", SecurityContextFinder.class);
        this.inboundSecureConversationContextManager = springContext.getBean( "inboundSecureConversationContextManager", InboundSecureConversationContextManager.class );
    }

    private String getIncomingURL(PolicyEnforcementContext context) {
        HttpRequestKnob hrk = context.getRequest().getKnob(HttpRequestKnob.class);
        if (hrk == null) {
            logger.warning("cannot generate incoming URL");
            return "";
        }
        return hrk.getQueryString() == null ? hrk.getRequestUrl() : hrk.getRequestUrl() + "?" + hrk.getQueryString();
    }

    @Override
    public AssertionStatus checkRequest(final PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        if (!SecurityHeaderAddressableSupport.isLocalRecipient(assertion)) {
            auditor.logAndAudit(AssertionMessages.REQUESTWSS_NOT_FOR_US);
            return AssertionStatus.NONE;
        }

        ProcessorResult wssResults;

        try {
            if (!context.getRequest().isSoap()) {
                auditor.logAndAudit(AssertionMessages.SC_REQUEST_NOT_SOAP);
                return AssertionStatus.NOT_APPLICABLE;
            }
            if ( !config.getBooleanProperty(ServerConfig.PARAM_WSS_PROCESSOR_LAZY_REQUEST, true) ) {
                wssResults = context.getRequest().getSecurityKnob().getProcessorResult();
            } else {
                wssResults = WSSecurityProcessorUtils.getWssResults(context.getRequest(), "Request", securityTokenResolver, securityContextFinder, auditor, new Functions.Unary<Boolean,Throwable>(){
                    @Override
                    public Boolean call( final Throwable throwable ) {
                        if ( BadSecurityContextException.class.isInstance(throwable) ) {
                            handleInvalidSecurityContext( context, ((BadSecurityContextException)throwable).getWsscNamespace() );
                            throw new AssertionStatusException(AssertionStatus.AUTH_FAILED);
                        }
                        return false;
                    }
                });
            }
        } catch (SAXException e) {
            throw new CausedIOException(e);
        }

        if (wssResults == null) {
            auditor.logAndAudit(AssertionMessages.REQUESTWSS_NO_SECURITY);
            context.setAuthenticationMissing();
            context.setRequestPolicyViolated();
            return AssertionStatus.FALSIFIED;
        }

        XmlSecurityToken[] tokens = wssResults.getXmlSecurityTokens();
        for (XmlSecurityToken token : tokens) {
            if (token instanceof SecurityContextToken) {
                SecurityContextToken secConTok = (SecurityContextToken) token;
                if (!secConTok.isPossessionProved()) {
                    auditor.logAndAudit(AssertionMessages.SC_NO_PROOF_OF_POSSESSION);
                    continue;
                }
                String contextId = secConTok.getContextIdentifier();
                SecureConversationSession session = inboundSecureConversationContextManager.getSession(contextId);
                if (session == null) {
                    handleInvalidSecurityContext( context, secConTok.getWsscNamespace() );
                    return AssertionStatus.AUTH_FAILED;
                }
                User authenticatedUser = session.getUsedBy();
                AuthenticationContext authContext = context.getAuthenticationContext(context.getRequest());
                LoginCredentials loginCreds = LoginCredentials.makeLoginCredentials(session.getCredentials().getSecurityToken(), false, SecureConversation.class, secConTok);
                authContext.addCredentials(loginCreds);
                context.addDeferredAssertion(this, deferredSecureConversationResponseDecoration(session));
                auditor.logAndAudit(AssertionMessages.SC_SESSION_FOR_USER, authenticatedUser.getLogin());

                final String messageId;
                try {
                    messageId = SoapUtil.getL7aMessageId(context.getRequest().getXmlKnob().getDocumentReadOnly());
                    context.setSavedRequestL7aMessageId(messageId == null ? "" : messageId);
                } catch (InvalidDocumentFormatException e) {
                    auditor.logAndAudit(AssertionMessages.EXCEPTION_INFO_WITH_MORE_INFO, "Invalid request SOAP:" + ExceptionUtils.getMessage(e));
                    return AssertionStatus.BAD_REQUEST;
                } catch (SAXException e) {
                    // Almost certainly can't happen
                    auditor.logAndAudit(AssertionMessages.EXCEPTION_INFO_WITH_MORE_INFO, "Invalid request XML:" + ExceptionUtils.getMessage(e));
                    return AssertionStatus.BAD_REQUEST;
                }

                // After everything goes OK, then set the context variable, inboundSC.session.
                context.setVariable(SecureConversation.VARIABLE_INBOUND_SC_SESSION, session);

                return AssertionStatus.NONE;
            }
        }
        auditor.logAndAudit(AssertionMessages.SC_REQUEST_NOT_REFER_TO_SC_TOKEN);
        context.setAuthenticationMissing();
        context.setRequestPolicyViolated();
        return AssertionStatus.AUTH_REQUIRED;
    }

    private void handleInvalidSecurityContext( final PolicyEnforcementContext context,
                                               final String namespace ) {
        auditor.logAndAudit( AssertionMessages.SC_TOKEN_INVALID);
        context.setRequestPolicyViolated();
        // here, we must override the soapfault detail in order to send the fault required by the spec
        SoapFaultLevel cfault = new SoapFaultLevel();
        cfault.setLevel(SoapFaultLevel.TEMPLATE_FAULT);
        cfault.setFaultTemplate( SoapFaultUtils.badContextTokenFault(getSoapVersion(context), namespace, getIncomingURL(context)));
        context.setFaultlevel(cfault);
    }

    private static SoapVersion getSoapVersion(PolicyEnforcementContext context) {
        final PublishedService service = context.getService();
        return service != null ? service.getSoapVersion() : SoapVersion.getDefaultSoapVersion();
    }

    private ServerAssertion deferredSecureConversationResponseDecoration(final SecureConversationSession session) {
        return new AbstractServerAssertion<SecureConversation>(assertion) {
            @Override
            public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException {
                DecorationRequirements wssReq;

                try {
                    if (!context.getResponse().isSoap()) {
                        auditor.logAndAudit(AssertionMessages.SC_UNABLE_TO_ATTACH_SC_TOKEN);
                        return AssertionStatus.NOT_APPLICABLE;
                    }
                    wssReq = context.getResponse().getSecurityKnob().getOrMakeDecorationRequirements();
                } catch (SAXException e) {
                    auditor.logAndAudit(AssertionMessages.SC_UNABLE_TO_ATTACH_SC_TOKEN);
                    throw new CausedIOException(e);
                }
                wssReq.setSignTimestamp(true);
                wssReq.setSecureConversationSession(session);
                return AssertionStatus.NONE;
            }
        };
    }

    private final Logger logger = Logger.getLogger(getClass().getName());
}