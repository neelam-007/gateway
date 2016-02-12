package com.l7tech.external.assertions.siteminder.server;

import com.ca.siteminder.*;
import com.l7tech.external.assertions.siteminder.SiteMinderCheckProtectedAssertion;
import com.l7tech.external.assertions.siteminder.util.SiteMinderAssertionUtil;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.server.message.AuthenticationContext;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.util.ExceptionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Copyright: Layer 7 Technologies, 2013
 * User: ymoiseyenko
 * Date: 7/12/13
 */
public class ServerSiteMinderCheckProtectedAssertion extends AbstractServerSiteMinderAssertion<SiteMinderCheckProtectedAssertion> {
    private final String[] variablesUsed;


    public ServerSiteMinderCheckProtectedAssertion(final SiteMinderCheckProtectedAssertion assertion, ApplicationContext springContext) throws PolicyAssertionException {
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
        String smAgentName = SiteMinderAssertionUtil.extractContextVarValue(assertion.getSmAgentName(), variableMap, getAudit());
        String resource = SiteMinderAssertionUtil.extractContextVarValue(assertion.getProtectedResource(), variableMap, getAudit());
        String action = SiteMinderAssertionUtil.extractContextVarValue(assertion.getAction(), variableMap, getAudit());
        String userIpAddress = SiteMinderAssertionUtil.extractContextVarValue(assertion.getSourceIpAddress(), variableMap, getAudit());
        String serverName = SiteMinderAssertionUtil.extractContextVarValue(assertion.getServerName(), variableMap, getAudit());
        SiteMinderContext smContext = null;
        try {
            smContext = (SiteMinderContext) context.getVariable(varPrefix + "." + SiteMinderAssertionUtil.SMCONTEXT);
        } catch (NoSuchVariableException e) {
            final String msg = "No CA Single Sign-On context variable ${" + varPrefix + "." +
                    SiteMinderAssertionUtil.SMCONTEXT + "} found in the Policy Enforcement Context. Creating an empty one";
            logAndAudit(AssertionMessages.SINGLE_SIGN_ON_FINE, (String)assertion.meta().get(AssertionMetadata.SHORT_NAME), msg);
            logger.log(Level.FINE, msg, ExceptionUtils.getDebugException(e));
            smContext = new SiteMinderContext();
        }

        if (!initSmAgentFromContext(assertion.getAgentGoid(), smContext)) {
            return AssertionStatus.FALSIFIED;
        }

        String transactionId = UUID.randomUUID().toString();//generate SiteMinder transaction id.
        smContext.setTransactionId(transactionId);

        if(StringUtils.isNotBlank(userIpAddress)){
            smContext.setSourceIpAddress(userIpAddress);
        }

        smContext.setConfig(getSmConfig(assertion.getAgentGoid()));

        try {
            //check if protected and return AssertionStatus.NONE if it is
            //the server name can be eith
            if(hla.checkProtected(getClientIp(message, smContext), smAgentName, serverName, resource, action, smContext)) {
                logAndAudit(AssertionMessages.SINGLE_SIGN_ON_FINE, (String)assertion.meta().get(AssertionMetadata.SHORT_NAME), "The resource " + resource + " is protected");
                status = AssertionStatus.NONE;
            }
            else {
                logAndAudit(AssertionMessages.SINGLE_SIGN_ON_WARNING,(String)assertion.meta().get(AssertionMetadata.SHORT_NAME), "The resource " + resource + " is not protected!");
            }
            populateContextVariables(context, varPrefix, smContext);

        } catch (SiteMinderApiClassException e) {
            logAndAudit(AssertionMessages.SINGLE_SIGN_ON_ERROR, (String)assertion.meta().get(AssertionMetadata.SHORT_NAME), e.getMessage());
            return AssertionStatus.FAILED;//something really bad happened
        }

        return status;
    }


}
