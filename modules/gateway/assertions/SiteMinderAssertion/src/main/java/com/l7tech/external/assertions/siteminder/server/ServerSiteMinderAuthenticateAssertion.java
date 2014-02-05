package com.l7tech.external.assertions.siteminder.server;

import com.ca.siteminder.*;
import com.l7tech.common.io.CertUtils;
import com.l7tech.external.assertions.siteminder.SiteMinderAuthenticateAssertion;
import com.l7tech.external.assertions.siteminder.util.SiteMinderAssertionUtil;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.credential.CredentialFormat;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.server.message.AuthenticationContext;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.util.*;
import org.apache.commons.lang.StringUtils;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.logging.Level;

/**
 * Server side implementation of the SiteMinderAuthenticateAssertion.
 *
 * @see com.l7tech.external.assertions.siteminder.SiteMinderAuthenticateAssertion
 */
public class ServerSiteMinderAuthenticateAssertion extends AbstractServerSiteMinderAssertion<SiteMinderAuthenticateAssertion> {
    private final String[] variablesUsed;


    public ServerSiteMinderAuthenticateAssertion(final SiteMinderAuthenticateAssertion assertion, ApplicationContext springContext) throws PolicyAssertionException {
        super(assertion, springContext);
        this.variablesUsed = assertion.getVariablesUsed();
    }

    @Override
    protected AssertionStatus doCheckRequest(final PolicyEnforcementContext context,
                                             final Message message,
                                             final String messageDescription,
                                             final AuthenticationContext authContext)  throws IOException, PolicyAssertionException {

        AssertionStatus status = AssertionStatus.FALSIFIED;

        final Map<String, Object> variableMap = context.getVariableMap(variablesUsed, getAudit());
        String varPrefix = SiteMinderAssertionUtil.extractContextVarValue(assertion.getPrefix(), variableMap, getAudit());
        String ssoToken = extractSsoToken(variableMap);

      SiteMinderContext smContext = null;
        try {
            try {
                smContext = (SiteMinderContext) context.getVariable(varPrefix + "." + SiteMinderAssertionUtil.SMCONTEXT);
            } catch (NoSuchVariableException e) {
                final String msg = "No SiteMinder context variable ${" + varPrefix + "." + SiteMinderAssertionUtil.SMCONTEXT + "} found in the Policy Enforcement Context";
                logger.log(Level.SEVERE, msg, ExceptionUtils.getDebugException(e));
                logAndAudit(AssertionMessages.SITEMINDER_ERROR, (String)assertion.meta().get(AssertionMetadata.SHORT_NAME), msg);
                return AssertionStatus.FALSIFIED;
            }

            if(smContext.getAgent() == null) {
                logAndAudit(AssertionMessages.SITEMINDER_ERROR, (String)assertion.meta().get(AssertionMetadata.SHORT_NAME), "Agent is null!");
                return AssertionStatus.FALSIFIED;
            }

            //first check what credentials are accepted by the policy server
            SiteMinderCredentials credentials = collectCredentials(authContext, variableMap, smContext);
            int result = hla.processAuthenticationRequest(credentials, getClientIp(message), ssoToken, smContext);
            if(result == SM_YES) {
                logAndAudit(AssertionMessages.SITEMINDER_FINE, (String)assertion.meta().get(AssertionMetadata.SHORT_NAME), ssoToken != null? "Authenticated via SSO Token: " + ssoToken:"Authenticated credentials: " + credentials);
                status = AssertionStatus.NONE;
            }
            else {
                logAndAudit(AssertionMessages.SITEMINDER_WARNING, (String)assertion.meta().get(AssertionMetadata.SHORT_NAME), "Unable to authenticate user using" + (ssoToken != null? " SSO Token:" + ssoToken: " credentials: " + credentials));
            }

            populateContextVariables(context, varPrefix, smContext);

        } catch (SiteMinderApiClassException e) {
            logAndAudit(AssertionMessages.SITEMINDER_ERROR, (String)assertion.meta().get(AssertionMetadata.SHORT_NAME), e.getMessage());
            return AssertionStatus.FAILED;//something really bad happened
        }

        return status;
    }

    private String extractSsoToken(Map<String, Object> variableMap) {
        String ssoToken = null;
        if(assertion.isUseSMCookie()) {
           //get cookie from a context variable
           ssoToken = ExpandVariables.process(Syntax.SYNTAX_PREFIX + assertion.getCookieSourceVar() + Syntax.SYNTAX_SUFFIX, variableMap, getAudit());
        }
        return ssoToken;
    }


    SiteMinderCredentials collectCredentials(AuthenticationContext context,  Map<String, Object> variableMap, SiteMinderContext smContext) throws PolicyAssertionException {
        //determine the type of authentication scheme
        List<SiteMinderContext.AuthenticationScheme> supportedAuthSchemes = smContext.getAuthSchemes();
        if(supportedAuthSchemes.size() == 1 && supportedAuthSchemes.iterator().next() == SiteMinderContext.AuthenticationScheme.NONE) {
            //logAndAudit
            return null;//anonymous credentials
        }
        //get Credentials
        LoginCredentials loginCredentials = null;
        if(assertion.isLastCredential()) {
            loginCredentials = context.getLastCredentials();
        }
        else{
            String credentialsName = SiteMinderAssertionUtil.extractContextVarValue(assertion.getCredentialsName(), variableMap, getAudit());
            if(StringUtils.isNotBlank(credentialsName)){
                List<LoginCredentials> creds = context.getCredentials();
                for(LoginCredentials cred : creds){
                    if(cred.getFormat() == CredentialFormat.CLIENTCERT) {
                        X509Certificate clientCert = cred.getClientCert();
                        //now compare subject DN from the certificate or CN from the certificate
                        if(clientCert != null &&
                           (CertUtils.isEqualDNCanonical(CertUtils.getSubjectDN(clientCert), credentialsName) ||
                            CertUtils.getCn(clientCert).equalsIgnoreCase(credentialsName))) {
                            loginCredentials = cred;
                            break;
                        }
                    }
                    else if((cred.getFormat() == CredentialFormat.BASIC ||
                            cred.getFormat() == CredentialFormat.CLEARTEXT) &&
                            credentialsName.equals(cred.getName())) {
                        loginCredentials = cred;
                        break;
                    }
                }
            }
        }

        return buildSiteMinderCredentials(supportedAuthSchemes, loginCredentials);
    }

    private SiteMinderCredentials buildSiteMinderCredentials(List<SiteMinderContext.AuthenticationScheme> supportedAuthSchemes, LoginCredentials loginCredentials) {
        if(loginCredentials != null) {
            //X509 Certificate
            if(supportedAuthSchemes.contains(SiteMinderContext.AuthenticationScheme.X509CERT)
                    || supportedAuthSchemes.contains(SiteMinderContext.AuthenticationScheme.X509CERTISSUEDN)
                    || supportedAuthSchemes.contains(SiteMinderContext.AuthenticationScheme.X509CERTUSERDN)) {
                //certificate is a different case
                if(loginCredentials.getFormat() == CredentialFormat.CLIENTCERT) {
                    try {
                        return  new SiteMinderCredentials(loginCredentials.getClientCert());
                    } catch (CertificateEncodingException e) {
                        logAndAudit(AssertionMessages.SITEMINDER_WARNING, (String)assertion.meta().get(AssertionMetadata.SHORT_NAME), "Unable to encode client certificate for login credentials:" + loginCredentials.getName());
                        logger.log(Level.WARNING, "Unable to encode client certificate", ExceptionUtils.getDebugException(e));
                    }
                }
            }
            //BASIC authentication scheme
            if(supportedAuthSchemes.contains(SiteMinderContext.AuthenticationScheme.BASIC)){
                if(loginCredentials.getFormat() == CredentialFormat.CLEARTEXT || loginCredentials.getFormat() == CredentialFormat.BASIC) {
                    return new SiteMinderCredentials(loginCredentials.getLogin(), new String(loginCredentials.getCredentials()));
                }
            }
            //TODO: collect credentials of a different type (SAML, Kerberos, NTLM, etc.)
        }
        return new SiteMinderCredentials();
    }

    /*
* Called reflectively by module class loader when module is unloaded, to ask us to clean up any globals
* that would otherwise keep our instances from getting collected.
*
* DELETEME if not required.
*/
public static void onModuleUnloaded() {
    // This assertion doesn't have anything to do in response to this, but it implements this anyway
        // since it will be used as an example by future modular assertion authors
    }
}
