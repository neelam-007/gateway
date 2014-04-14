package com.l7tech.external.assertions.siteminder.server;

import com.ca.siteminder.SiteMinderApiClassException;
import com.ca.siteminder.SiteMinderContext;
import com.l7tech.common.http.HttpCookie;
import com.l7tech.external.assertions.siteminder.SiteMinderAuthorizeAssertion;
import com.l7tech.external.assertions.siteminder.util.SiteMinderAssertionUtil;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.message.HttpCookiesKnob;
import com.l7tech.message.HttpResponseKnob;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.server.message.AuthenticationContext;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.util.ExceptionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;

/**
 * Copyright: Layer 7 Technologies, 2013
 * User: ymoiseyenko
 * Date: 7/15/13
 */
public class ServerSiteMinderAuthorizeAssertion extends AbstractServerSiteMinderAssertion<SiteMinderAuthorizeAssertion> {

    private final String[] variablesUsed;

    public ServerSiteMinderAuthorizeAssertion(final SiteMinderAuthorizeAssertion assertion, ApplicationContext springContext) throws PolicyAssertionException {
        super(assertion, springContext);
        this.variablesUsed = assertion.getVariablesUsed();

    }

    /**
     * SSG Server-side processing of the given request.
     *
     * @param context the PolicyEnforcementContext.  Never null.
     * @return AssertionStatus.NONE if this Assertion did its business successfully; otherwise, some error code
     * @throws com.l7tech.policy.assertion.PolicyAssertionException
     *                             something is wrong in the policy dont throw this if there is an issue with the request or the response
     * @throws java.io.IOException if there is a problem reading a request or response
     * @throws com.l7tech.server.policy.assertion.AssertionStatusException
     *                             as an alternate mechanism to return an assertion status other than AssertionStatus.NONE.
     */
    @Override
    public AssertionStatus doCheckRequest(final PolicyEnforcementContext context,
                                          final Message message,
                                          final String messageDescription,
                                          final AuthenticationContext authContext) throws IOException, PolicyAssertionException {
        AssertionStatus status = AssertionStatus.FALSIFIED;

        final Map<String, Object> variableMap = context.getVariableMap(variablesUsed, getAudit());
        String varPrefix = SiteMinderAssertionUtil.extractContextVarValue(assertion.getPrefix(), variableMap, getAudit());
        String smCookieName = SiteMinderAssertionUtil.extractContextVarValue(assertion.getCookieName(), variableMap, getAudit());

        SiteMinderContext smContext = null;
        try {
            smContext = (SiteMinderContext) context.getVariable(varPrefix + "." + SiteMinderAssertionUtil.SMCONTEXT);
        } catch (NoSuchVariableException e) {
            final String msg = "No SiteMinder context variable ${" + varPrefix + "." + SiteMinderAssertionUtil.SMCONTEXT + "} found in the Policy Enforcement Context";
            logAndAudit(AssertionMessages.SITEMINDER_ERROR, (String) assertion.meta().get(AssertionMetadata.SHORT_NAME), msg);
            logger.log(Level.SEVERE, msg, ExceptionUtils.getDebugException(e));
            return AssertionStatus.FALSIFIED;
        }

        if(smContext.getAgent() == null) {
            logAndAudit(AssertionMessages.SITEMINDER_ERROR, (String)assertion.meta().get(AssertionMetadata.SHORT_NAME), "Agent is null!");
            return AssertionStatus.FALSIFIED;
        }

        String ssoToken = null;

        if(assertion.isUseVarAsCookieSource() && StringUtils.isNotBlank(assertion.getCookieSourceVar())) {
            //TODO: find better solution
            ssoToken = ExpandVariables.process(Syntax.SYNTAX_PREFIX + assertion.getCookieSourceVar() + Syntax.SYNTAX_SUFFIX, variableMap, getAudit());
        }

        try {
            int result = hla.processAuthorizationRequest(getClientIp(message), ssoToken, smContext);
            if(result == SM_YES) {
                context.setVariable(varPrefix + "." + smCookieName, smContext.getSsoToken());
                /////////////////////////////////////////////////////////////////////////////////////////////
                if (!setSessionCookie(context, smContext, variableMap)) return AssertionStatus.FALSIFIED;
                ////////////////////////////////////////////////////////////////////////////////////////////
                logAndAudit(AssertionMessages.SITEMINDER_FINE, (String)assertion.meta().get(AssertionMetadata.SHORT_NAME), "SM Sessions " + ssoToken + " is authorized");
                status = AssertionStatus.NONE;
            }
            else {
                logAndAudit(AssertionMessages.SITEMINDER_WARNING, (String)assertion.meta().get(AssertionMetadata.SHORT_NAME), "SM Sessions " + ssoToken + " is not authorized!");
            }
            populateContextVariables(context, varPrefix, smContext);

        } catch (SiteMinderApiClassException e) {
            logAndAudit(AssertionMessages.SITEMINDER_ERROR, (String)assertion.meta().get(AssertionMetadata.SHORT_NAME), e.getMessage());
            return AssertionStatus.FAILED;//something really bad happened
        }

        return status;
    }


    /*
    * Creates a cookie from the SiteMinder SSO Token and its details and adds it to the HttpServletResponse.
    * As of 8.2 release this method is depricated!
    * @param pec - PEC
    * @param smContext - SiteMinder context
    * @param varMap - context variable map
    * @return - false if failed to set the cookie
    */
    @Deprecated
    boolean setSessionCookie(PolicyEnforcementContext pec, SiteMinderContext smContext, Map<String, Object> varMap) {
        boolean result = true;
        if(assertion.isSetSMCookie()) {
            result = false;
            String ssoCookie = smContext.getSsoToken();

            if (StringUtils.isBlank(ssoCookie)) {
                logAndAudit(AssertionMessages.SITEMINDER_FINE, (String) assertion.meta().get(AssertionMetadata.SHORT_NAME), "SMSESSION cookie is blank! Cookie is not set");
                return false;
            }

            logAndAudit(AssertionMessages.SITEMINDER_FINE, (String) assertion.meta().get(AssertionMetadata.SHORT_NAME), "Adding the SiteMinder SSO cookie to the response. Cookie is '" + ssoCookie + "'");
            //get cookie params  directly from assertion
            String domain = SiteMinderAssertionUtil.extractContextVarValue(assertion.getCookieDomain(), varMap, getAudit());
            String secure = SiteMinderAssertionUtil.extractContextVarValue(assertion.getCookieSecure(), varMap, getAudit());
            boolean isSecure = Boolean.parseBoolean(secure);
            String version = SiteMinderAssertionUtil.extractContextVarValue(assertion.getCookieVersion(), varMap, getAudit());
            int iVer = 0;
            if (version != null && version.length() > 0) {
                try {
                    iVer = Integer.parseInt(version);
                } catch (NumberFormatException nfe) {
                    logAndAudit(AssertionMessages.SITEMINDER_FINE, (String) assertion.meta().get(AssertionMetadata.SHORT_NAME), "Version was set in the context but was not a number: " + version);
                }
            }
            String maxAge = SiteMinderAssertionUtil.extractContextVarValue(assertion.getCookieMaxAge(), varMap, getAudit());
            int iMaxAge = -1;
            if (maxAge != null && maxAge.length() > 0) {
                try {
                    iMaxAge = Integer.parseInt(maxAge);
                } catch (NumberFormatException nfe) {
                    logAndAudit(AssertionMessages.SITEMINDER_FINE, (String) assertion.meta().get(AssertionMetadata.SHORT_NAME), "Max Age was set in the context but was not a number: " + maxAge);
                }
            }
            String comment = SiteMinderAssertionUtil.extractContextVarValue(assertion.getCookieComment(), varMap, getAudit());
            String path = SiteMinderAssertionUtil.extractContextVarValue(assertion.getCookiePath(), varMap, getAudit());

            HttpCookie cookie = new HttpCookie(assertion.getCookieName(), ssoCookie, iVer, path, domain, iMaxAge, isSecure, comment, false);
            Message response = pec.getResponse();
            if (response.isHttpResponse()) {
                response.getHttpCookiesKnob().addCookie(cookie);
                result = true;
            } else {
                logAndAudit(AssertionMessages.SITEMINDER_ERROR, (String) assertion.meta().get(AssertionMetadata.SHORT_NAME), "Unable to set Http Cookie: response is not HTTP type");
            }
        }

        return result;
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
