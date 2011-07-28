package com.l7tech.server.policy.assertion.credential.wss;

import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.security.token.UsernameToken;
import com.l7tech.security.token.XmlSecurityToken;
import com.l7tech.security.xml.processor.ProcessorResult;
import com.l7tech.security.xml.SecurityTokenResolver;
import com.l7tech.server.ServerConfigParams;
import com.l7tech.util.CausedIOException;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.credential.wss.WssBasic;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.AuthenticationContext;
import com.l7tech.server.policy.assertion.AbstractMessageTargetableServerAssertion;
import com.l7tech.server.util.WSSecurityProcessorUtils;
import com.l7tech.message.Message;
import com.l7tech.util.Config;
import org.springframework.context.ApplicationContext;
import org.xml.sax.SAXException;

import java.io.IOException;

/**
 * Server assertion for WS-Security UsernameToken authentication.
 *
 * @author alex
 */
public class ServerWssBasic extends AbstractMessageTargetableServerAssertion<WssBasic> {

    //- PUBLIC

    public ServerWssBasic(final WssBasic data, final ApplicationContext springContext) {
        super(data, data);
        this.config = springContext.getBean("serverConfig", Config.class);
        this.securityTokenResolver = (SecurityTokenResolver)springContext.getBean("securityTokenResolver");
    }

    @Override
    public AssertionStatus checkRequest(final PolicyEnforcementContext context) throws IOException, PolicyAssertionException
    {
        if (!assertion.getRecipientContext().localRecipient()) {
            logAndAudit(AssertionMessages.WSS_BASIC_FOR_ANOTHER_RECIPIENT);
            return AssertionStatus.NONE;
        }

        return super.checkRequest( context );
    }

    //- PROTECTED

    @Override
    protected AssertionStatus doCheckRequest( final PolicyEnforcementContext context,
                                              final Message message,
                                              final String messageDescription,
                                              final AuthenticationContext authContext ) throws IOException, PolicyAssertionException {
        ProcessorResult wssResults;
        try {
            if (!message.isSoap()) {
                logAndAudit(AssertionMessages.WSS_BASIC_NOT_SOAP, messageDescription);
                return AssertionStatus.NOT_APPLICABLE;
            }
            if ( isRequest() && !config.getBooleanProperty( ServerConfigParams.PARAM_WSS_PROCESSOR_LAZY_REQUEST,true) ) {
                wssResults = message.getSecurityKnob().getProcessorResult();
            } else {
                wssResults = WSSecurityProcessorUtils.getWssResults(message, messageDescription, securityTokenResolver, getAudit());
            }
        } catch (SAXException e) {
            throw new CausedIOException("Message '"+messageDescription+"' declared as XML but is not well-formed", e);
        }
        if (wssResults == null) {
            logAndAudit(AssertionMessages.WSS_BASIC_NO_CREDENTIALS, messageDescription);
            
            if ( isRequest() ) {
                context.setAuthenticationMissing();
                context.setRequestPolicyViolated();
            }
            return AssertionStatus.AUTH_REQUIRED;
        }

        XmlSecurityToken[] tokens = wssResults.getXmlSecurityTokens();
        for (XmlSecurityToken token : tokens) {
            if (token instanceof UsernameToken) {
                UsernameToken ut = (UsernameToken) token;
                LoginCredentials creds = LoginCredentials.makeLoginCredentials(ut, WssBasic.class);
                authContext.addCredentials(creds);
                return AssertionStatus.NONE;
            }
        }
        logAndAudit(AssertionMessages.WSS_BASIC_CANNOT_FIND_CREDENTIALS);
        // we get here because there were no credentials found in the format we want
        // therefore this assertion was violated
        if ( isRequest() ) {
            context.setRequestPolicyViolated();
        }
        return AssertionStatus.AUTH_REQUIRED;
    }



    //- PRIVATE

    private final Config config;
    private final SecurityTokenResolver securityTokenResolver;
}
