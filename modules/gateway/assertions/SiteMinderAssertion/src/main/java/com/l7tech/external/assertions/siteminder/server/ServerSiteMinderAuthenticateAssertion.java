package com.l7tech.external.assertions.siteminder.server;

import com.ca.siteminder.*;
import com.l7tech.common.http.HttpCookie;
import com.l7tech.external.assertions.siteminder.SiteMinderAuthenticateAssertion;
import com.l7tech.external.assertions.siteminder.util.SiteMinderAssertionUtil;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.message.HttpRequestKnob;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.credential.CredentialFormat;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.server.message.AuthenticationContext;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.util.*;
import com.l7tech.util.Config;
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

    public AssertionStatus checkRequest( final PolicyEnforcementContext context ) throws IOException, PolicyAssertionException {
        AssertionStatus status = AssertionStatus.FALSIFIED;

        final Map<String, Object> variableMap = context.getVariableMap(variablesUsed, getAudit());
        String varPrefix = SiteMinderAssertionUtil.extractContextVarValue(assertion.getPrefix(), variableMap, getAudit());
        String smCookieName = SiteMinderAssertionUtil.extractContextVarValue(assertion.getCookieNameVariable(), variableMap, getAudit());

        //
        String ssoToken = extractSsoToken(context, variableMap, smCookieName);

        SiteMinderContext smContext = null;
        try {
            try {
                smContext = (SiteMinderContext) context.getVariable(varPrefix + "." + SiteMinderAssertionUtil.SMCONTEXT);
            } catch (NoSuchVariableException e) {
                final String msg = "No SiteMinder context variable ${" + varPrefix + "." + SiteMinderAssertionUtil.SMCONTEXT + "} found in the Policy Enforcement Context";
                logger.log(Level.SEVERE, msg, ExceptionUtils.getDebugException(e));
                logAndAudit(AssertionMessages.SITEMINDER_ERROR, msg);
                return AssertionStatus.FALSIFIED;
            }
            if(smContext.getAgent() == null) {
                logAndAudit(AssertionMessages.SITEMINDER_ERROR, "Agent is null!");
                return AssertionStatus.FALSIFIED;
            }

            //first check what credentials are accepted by the policy server
            SiteMinderCredentials credentials = collectCredentials(context, variableMap, smContext);
            int result = hla.processAuthenticationRequest(credentials, getClientIp(context), ssoToken, smContext);
            if(result == 1) {
                context.setVariable(varPrefix + "." + smCookieName, smContext.getSsoToken());
                status = AssertionStatus.NONE;
            }
            populateContextVariables(context, varPrefix, smContext);

        } catch (SiteMinderApiClassException e) {
            logAndAudit(AssertionMessages.SITEMINDER_ERROR, e.getMessage());
            return AssertionStatus.FAILED;//something really bad happened
        }

        return status;
    }

    private String extractSsoToken(PolicyEnforcementContext context, Map<String, Object> variableMap, String smCookieName) {
        String ssoToken = null;
        if(assertion.isUseSMCookie()) {
            if(assertion.isUseCustomCookieName()) {
                HttpRequestKnob httpRequestKnob = context.getRequest().getHttpRequestKnob();
                //TODO: try to find cookie from the request
               if(httpRequestKnob != null){
                   for(HttpCookie cookie : httpRequestKnob.getCookies()) {
                       if(cookie.getCookieName().equalsIgnoreCase(smCookieName)){
                           ssoToken = cookie.getCookieValue();
                           break;
                       }
                   }
               }
           }
           else {
               //get cookie from a context variable
               ssoToken = ExpandVariables.process(assertion.getCookieSourceVar(), variableMap, getAudit());
           }
        }
        return ssoToken;
    }


    SiteMinderCredentials collectCredentials(PolicyEnforcementContext pec, Map<String, Object> variableMap, SiteMinderContext smContext) throws PolicyAssertionException {
        //determine the type of authentication scheme
        List<SiteMinderContext.AuthenticationScheme> supportedAuthSchemes = smContext.getAuthSchemes();
        if(supportedAuthSchemes.size() == 1 && supportedAuthSchemes.iterator().next() == SiteMinderContext.AuthenticationScheme.NONE) {
            //logAndAudit
            return null;//anonymous credentials
        }

        //get Credentials
        LoginCredentials loginCredentials = null;
        AuthenticationContext context = pec.getDefaultAuthenticationContext();
        if(assertion.isLastCredential()) {
            loginCredentials = context.getLastCredentials();
        }
        else{
            String userName = SiteMinderAssertionUtil.extractContextVarValue(assertion.getLogin(), variableMap, getAudit());
            List<LoginCredentials> creds = context.getCredentials();
            for(LoginCredentials cred : creds){
                if(cred.getName().equals(userName)){
                    loginCredentials = cred;
                    break;
                }
            }
        }

        return buildSiteMinderCredentials(pec, supportedAuthSchemes, loginCredentials);

    }

    private SiteMinderCredentials buildSiteMinderCredentials(PolicyEnforcementContext pec, List<SiteMinderContext.AuthenticationScheme> supportedAuthSchemes, LoginCredentials loginCredentials) {
        if(supportedAuthSchemes.contains(SiteMinderContext.AuthenticationScheme.X509CERT)
                || supportedAuthSchemes.contains(SiteMinderContext.AuthenticationScheme.X509CERTISSUEDN)
                || supportedAuthSchemes.contains(SiteMinderContext.AuthenticationScheme.X509CERTUSERDN)) {
            //certificate is a different case
            HttpRequestKnob httpRequestKnob = pec.getRequest().getHttpRequestKnob();
            if(httpRequestKnob != null) {
                try {
                    X509Certificate[] clientCerts = httpRequestKnob.getClientCertificate();
                    return  new SiteMinderCredentials(clientCerts);
                } catch (IOException ie) {
                    //TODO: logAndAudit this case
                    logger.log(Level.WARNING, "Client certificate is not X509 type!", ExceptionUtils.getDebugException(ie));
                } catch (CertificateEncodingException e) {
                    //TODO: logAndAudit this case
                    logger.log(Level.WARNING, "Unable to encode client certificate", ExceptionUtils.getDebugException(e));
                }
            }
        }
        if(loginCredentials != null) {
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
    * Creates a cookie from the SiteMinder SSO Token and its details and adds it to the HttpServletResponse.
    *
    * @param res
    * @param ssoCookie
    * @param cookieParams
    */
    void setSessionCookie(PolicyEnforcementContext pec, SiteMinderContext smContext, Map<String, Object> varMap) {
        //TODO: use logAndAudit instead
        String ssoCookie = smContext.getSsoToken();
        if(StringUtils.isBlank(ssoCookie)) {
            logger.log(Level.WARNING, "SMSESSION cookie is blank! Cookie is not set");
            return;
        }

        logger.log(Level.FINE, "Adding the SiteMinder SSO cookie to the response. Cookie is '" + ssoCookie + "'");
        //TODO: this should go into Manage Cookies modular assertion
        //get cookie params  directly from assertion
        String domain = SiteMinderAssertionUtil.extractContextVarValue(assertion.getCookieDomain(), varMap, getAudit());
        String secure = SiteMinderAssertionUtil.extractContextVarValue(assertion.isCookieSecure(), varMap, getAudit());
        boolean isSecure = Boolean.parseBoolean(secure);
        String version = SiteMinderAssertionUtil.extractContextVarValue(assertion.getCookieVersion(), varMap, getAudit());
        int iVer = 0;
        if (version != null && version.length() > 0) {
            try {
                iVer = Integer.parseInt(version);
            } catch (NumberFormatException nfe) {
                logger.log(Level.FINE, "Version was set in the context but was not a number");
            }
        }
        String maxAge = SiteMinderAssertionUtil.extractContextVarValue(assertion.getCookieMaxAge(), varMap, getAudit());
        int iMaxAge = -1;
        if (maxAge != null && maxAge.length() > 0) {
            try {
                iMaxAge = Integer.parseInt(maxAge);
            } catch (NumberFormatException nfe) {
                logger.log(Level.FINE, "Max Age was set in the context but was not a number");
            }
        }
        String comment = SiteMinderAssertionUtil.extractContextVarValue(assertion.getCookieComment(), varMap, getAudit());
        String path = SiteMinderAssertionUtil.extractContextVarValue(assertion.getCookiePath(), varMap, getAudit());

        HttpCookie cookie = new HttpCookie(assertion.getCookieNameVariable(), ssoCookie, iVer, path, domain, iMaxAge, isSecure, comment);

        pec.getResponse().getHttpResponseKnob().addCookie(cookie);

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
