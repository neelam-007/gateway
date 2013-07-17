package com.l7tech.external.assertions.siteminder.server;

import com.ca.siteminder.SiteMinderApiClassException;
import com.ca.siteminder.SiteMinderContext;
import com.ca.siteminder.SiteMinderHighLevelAgent;
import com.l7tech.common.http.HttpCookie;
import com.l7tech.external.assertions.siteminder.SiteMinderAuthorizeAssertion;
import com.l7tech.external.assertions.siteminder.util.SiteMinderAssertionUtil;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.server.message.PolicyEnforcementContext;
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

//    private SiteMinderHighLevelAgent hla;

    public ServerSiteMinderAuthorizeAssertion(final SiteMinderAuthorizeAssertion assertion, ApplicationContext springContext) throws PolicyAssertionException {
        super(assertion);
        this.config = springContext.getBean("serverConfig", com.l7tech.util.Config.class);
        this.variablesUsed = assertion.getVariablesUsed();

    }

    /**
     * package level constructor used for unit tests
     * initializes the SM HLA with the configuration read fromm the smAgentConfigProperty
     * TODO: this should be changed to allow configuration read from
     * @param assertion SiteMinderAuthenticateAssertion
     * @param agent  SiteMInderHighLevelAgent
     * @throws PolicyAssertionException
     */
    ServerSiteMinderAuthorizeAssertion( final SiteMinderAuthorizeAssertion assertion, final SiteMinderHighLevelAgent agent ) throws PolicyAssertionException {
        super(assertion);
        this.variablesUsed = assertion.getVariablesUsed();
//        this.hla = agent;
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
    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        AssertionStatus status = AssertionStatus.FALSIFIED;

        final Map<String, Object> variableMap = context.getVariableMap(variablesUsed, getAudit());
        String varPrefix = SiteMinderAssertionUtil.extractContextVarValue(assertion.getPrefix(), variableMap, getAudit());
        String smCookieName = SiteMinderAssertionUtil.extractContextVarValue(assertion.getCookieName(), variableMap, getAudit());

        SiteMinderContext smContext = null;
        try {
            smContext = (SiteMinderContext) context.getVariable(varPrefix + "." + SiteMinderAssertionUtil.SMCONTEXT);
        } catch (NoSuchVariableException e) {
            final String msg = "No SiteMinder context variable ${" + varPrefix + "." + SiteMinderAssertionUtil.SMCONTEXT + "} found in the Policy Enforcement Context";
            logger.log(Level.SEVERE, msg, ExceptionUtils.getDebugException(e));
            logAndAudit(AssertionMessages.SITEMINDER_ERROR, msg);
            return AssertionStatus.FALSIFIED;
        }

        if(smContext.getAgentId() == null) {
            logAndAudit(AssertionMessages.SITEMINDER_ERROR, "Agent ID is null!");
            return AssertionStatus.FALSIFIED;
        }

        String ssoToken = null;

        if(assertion.isUseVarAsCookieSource()){
            ssoToken = SiteMinderAssertionUtil.extractContextVarValue(assertion.getCookieSourceVar(), variableMap, getAudit());
        }
        //TODO: replace with real siteMinder config from layer7-siteminder module
        initSmAgentFromContext(smContext);
        //
        try {
            int result = hla.processAuthorizationRequest(getClientIp(context), ssoToken, smContext);
            if(result == 1) {
                context.setVariable(varPrefix + "." + smCookieName, smContext.getSsoToken());
                setSessionCookie(context, smContext, variableMap);
                status = AssertionStatus.NONE;
            }
            populateContextVariables(context, varPrefix, smContext);

        } catch (SiteMinderApiClassException e) {
            logAndAudit(AssertionMessages.SITEMINDER_ERROR, e.getMessage());
            return AssertionStatus.FAILED;//something really bad happened
        }

        return status;
    }

    @Override
    protected String getAgentIdOrDefault(SiteMinderContext context) {
        //TODO: need to get the proper default agent
        return context.getAgentId();
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

        HttpCookie cookie = new HttpCookie(assertion.getCookieName(), ssoCookie, iVer, path, domain, iMaxAge, isSecure, comment);

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
