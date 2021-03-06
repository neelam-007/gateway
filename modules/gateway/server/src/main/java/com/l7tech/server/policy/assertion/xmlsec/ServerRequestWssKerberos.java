package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.kerberos.*;
import com.l7tech.message.HttpRequestKnob;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.xmlsec.RequestWssKerberos;
import com.l7tech.policy.assertion.xmlsec.SecurityHeaderAddressableSupport;
import com.l7tech.security.token.KerberosSigningSecurityToken;
import com.l7tech.security.token.XmlSecurityToken;
import com.l7tech.security.xml.SecurityTokenResolver;
import com.l7tech.security.xml.decorator.DecorationRequirements;
import com.l7tech.security.xml.processor.ProcessorResult;
import com.l7tech.security.xml.processor.SecurityContextFinder;
import com.l7tech.security.xml.processor.WssProcessorImpl;
import com.l7tech.server.ServerConfigParams;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.security.kerberos.KerberosSessionContextManager;
import com.l7tech.server.util.WSSecurityProcessorUtils;
import com.l7tech.util.CausedIOException;
import com.l7tech.util.Config;

import org.jaaslounge.decoding.kerberos.KerberosEncData;
import org.springframework.context.ApplicationContext;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.net.URL;
import java.util.logging.Level;

/**
 * Server side processing for WS-Security Kerberos Tokens.
 */
public class ServerRequestWssKerberos extends AbstractServerAssertion<RequestWssKerberos> {

    //- PUBLIC

    public ServerRequestWssKerberos(RequestWssKerberos requestWssKerberos, ApplicationContext springContext) {
        super(requestWssKerberos);
        this.requestWssKerberos = requestWssKerberos;
        this.config = springContext.getBean("serverConfig", Config.class);
        this.securityTokenResolver = springContext.getBean("securityTokenResolver", SecurityTokenResolver.class);
        this.securityContextFinder = springContext.getBean("securityContextFinder", SecurityContextFinder.class);
        this.kerberosSessionContextManager = springContext.getBean( "kerberosSessionContextManager", KerberosSessionContextManager.class );
    }

    @Override
    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        if (!SecurityHeaderAddressableSupport.isLocalRecipient(requestWssKerberos)) {
            logAndAudit( AssertionMessages.REQUESTWSS_NOT_FOR_US );
            return AssertionStatus.NONE;
        }

        ProcessorResult wssResults;
        try {
            if (!context.getRequest().isSoap()) {
                logAndAudit( AssertionMessages.REQUEST_WSS_KERBEROS_NON_SOAP );
                return AssertionStatus.BAD_REQUEST;
            }
            if ( !config.getBooleanProperty( ServerConfigParams.PARAM_WSS_PROCESSOR_LAZY_REQUEST, true) ) {
                wssResults = context.getRequest().getSecurityKnob().getProcessorResult();
            } else {
                wssResults = WSSecurityProcessorUtils.getWssResults(context.getRequest(), "Request", securityTokenResolver, securityContextFinder, getAudit(), null);
            }
        } catch (SAXException e) {
            throw new CausedIOException(e);
        }
        if (wssResults == null) {
            logAndAudit( AssertionMessages.REQUESTWSS_NO_SECURITY );
            context.setRequestPolicyViolated();
            context.setAuthenticationMissing();
            return AssertionStatus.FALSIFIED;
        }

        XmlSecurityToken[] tokens = wssResults.getXmlSecurityTokens();
        if (tokens == null) {
            logAndAudit( AssertionMessages.REQUEST_WSS_KERBEROS_NO_TOKEN );
            context.setAuthenticationMissing();
            return AssertionStatus.AUTH_REQUIRED;
        }

        String configSpn = requestWssKerberos.getServicePrincipalName();
        if (configSpn == null) {
            HttpRequestKnob requestKnob = context.getRequest().getKnob(HttpRequestKnob.class);
            URL requestUrl = requestKnob != null? requestKnob.getRequestURL(): null;
            String remoteAddress = requestKnob != null? requestKnob.getRemoteAddress(): null;
            configSpn = KerberosUtils.toGssName(KerberosUtils.extractSpnFromRequest(requestUrl, remoteAddress));
        }

        KerberosSigningSecurityToken kerberosToken = null;
        for( XmlSecurityToken tok : tokens ) {
            if( tok instanceof KerberosSigningSecurityToken) {
                KerberosSigningSecurityToken ksst = (KerberosSigningSecurityToken) tok;

                // SSG-13640 - When the SSG has multiple service keys (SPNs) in its keytab it attempts to:
                //     guess the SPN required to authenticate the via in the bound URL (KerberosUtils.extractSpnFromRequest())
                //     or by reverse lookup the remote calling endpoint and do the same (to support Load Balancer Proxies)
                //     or failing those uses the first key in the keytab.
                //     If this property is set true the SSG will manually parse the Kerberos Ticket to determine
                //     the service SPN from the ticket itself.

                if ( ! config.getBooleanProperty(WssProcessorImpl.KRB5_USE_SPN_FROM_TICKET_PROP,false) ) {
                    if (!configSpn.equals(ksst.getServiceTicket().getServicePrincipalName())) {
                        logger.info("Ignoring Kerberos session for another service ('" + configSpn + "', '" + ksst.getServiceTicket().getServicePrincipalName() + "').");
                    } else {
                        kerberosToken = ksst;
                    }
                } else {
                    kerberosToken = ksst;
                }

            }
        }

        if ( kerberosToken != null ) { // process token
            KerberosServiceTicket kerberosServiceTicket = kerberosToken.getServiceTicket();
            assert kerberosServiceTicket!=null;
            LoginCredentials loginCreds = LoginCredentials.makeLoginCredentials(kerberosToken, assertion.getClass());
            context.getAuthenticationContext(context.getRequest()).addCredentials(loginCreds);
            context.setVariable(RequestWssKerberos.KERBEROS_REALM, extractRealm(kerberosServiceTicket.getClientPrincipalName()));

            logAndAudit( AssertionMessages.REQUEST_WSS_KERBEROS_GOT_TICKET, kerberosServiceTicket.getClientPrincipalName() );

            // stash for later reference
            final String sessionIdentifier = KerberosUtils.getSessionIdentifier(kerberosServiceTicket.getGSSAPReqTicket());
            if ( kerberosSessionContextManager.getSession( sessionIdentifier ) == null) {
                try {
                    kerberosSessionContextManager.createSession(
                            sessionIdentifier,
                            kerberosServiceTicket.getExpiry(),
                            loginCreds,
                            kerberosServiceTicket.getKey());
                }
                catch(KerberosSessionContextManager.DuplicateSessionException dse) {
                    //can't happen since duplicate tickets are detected by kerberos.
                    logger.log(Level.SEVERE, "Duplicate session key error when creating kerberos session.", dse);
                }
            }
            //get Kerberos Authorization Data from the ticket and set appropriate context variables so they can be used later
            KerberosEncData encData = kerberosServiceTicket.getEncData();
            if(null != encData){
                context.setVariable(RequestWssKerberos.KERBEROS_DATA, encData);
            }

            // Set up response to be able to sign and encrypt using a reference to this ticket
            addDeferredAssertion(context, kerberosServiceTicket);

            return AssertionStatus.NONE;
        }
        logAndAudit( AssertionMessages.REQUEST_WSS_KERBEROS_NO_TICKET );
        context.setAuthenticationMissing();
        return AssertionStatus.AUTH_REQUIRED;
    }

    //- PRIVATE

    private final Config config;
    private final SecurityTokenResolver securityTokenResolver;
    private final SecurityContextFinder securityContextFinder;
    private final KerberosSessionContextManager kerberosSessionContextManager;

    private RequestWssKerberos requestWssKerberos;

    @SuppressWarnings( { "unchecked" } )
    private void addDeferredAssertion( final PolicyEnforcementContext policyEnforcementContext,
                                       final KerberosServiceTicket kerberosServiceTicket) {
        policyEnforcementContext.addDeferredAssertion(this, new AbstractServerAssertion(requestWssKerberos) {
            @Override
            @SuppressWarnings( { "unchecked" } )
            public AssertionStatus checkRequest(PolicyEnforcementContext context)
                    throws IOException, PolicyAssertionException
            {
                DecorationRequirements dreq = context.getResponse().getSecurityKnob().getAlternateDecorationRequirements(null);
                dreq.setKerberosTicket(kerberosServiceTicket);

                return AssertionStatus.NONE;
            }
        });
    }

    private String extractRealm( final String principal ) {
        String realm = "";

        int index = principal.lastIndexOf( "@" );
        if ( index > -1 ) {
            realm = principal.substring( index + 1 );
        }

        return realm;
    }
}
