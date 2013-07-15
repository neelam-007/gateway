package com.l7tech.external.assertions.siteminder.server;

import com.ca.siteminder.*;
import com.l7tech.external.assertions.siteminder.SiteMinderCheckProtectedAssertion;
import com.l7tech.external.assertions.siteminder.util.SiteMinderAssertionUtil;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.message.Message;
import com.l7tech.message.TcpKnob;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.util.Config;
import com.l7tech.util.ExceptionUtils;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

/**
 * Copyright: Layer 7 Technologies, 2013
 * User: ymoiseyenko
 * Date: 7/12/13
 */
public class ServerSiteMinderCheckProtectedAssertion extends AbstractServerAssertion<SiteMinderCheckProtectedAssertion> {
    private final String[] variablesUsed;

    private SiteMinderHighLevelAgent hla;
    private Config config;

    public ServerSiteMinderCheckProtectedAssertion(final SiteMinderCheckProtectedAssertion assertion, ApplicationContext springContext) throws PolicyAssertionException {
        super(assertion);
        this.config = springContext.getBean("serverConfig", com.l7tech.util.Config.class);
        this.variablesUsed = assertion.getVariablesUsed();
        //TODO: replace with the real config
        checkSmAgentConfig();
//        String smConfig = config.getProperty("smAgentConfig");
//        try {
//            hla = new SiteMinderHighLevelAgent(smConfig, getAgentIdOrDefault());
//        } catch (SiteMinderAgentConfigurationException e) {
//            throw new PolicyAssertionException(assertion, "SiteMinder agent configuration is invalid", ExceptionUtils.getDebugException(e));
//        } catch (SiteMinderApiClassException e) {
//            throw new PolicyAssertionException(assertion, "SiteMinder agent API exception", e);
//        }
    }

    /**
     * package level constructor used for unit tests
     * initializes the SM HLA with the configuration read fromm the smAgentConfigProperty
     * TODO: this should be changed to allow configuration read from
     * @param assertion SiteMinderAuthenticateAssertion
     * @param agent  SiteMInderHighLevelAgent
     * @throws PolicyAssertionException
     */
    ServerSiteMinderCheckProtectedAssertion( final SiteMinderCheckProtectedAssertion assertion, final SiteMinderHighLevelAgent agent ) throws PolicyAssertionException {
        super(assertion);
        this.variablesUsed = assertion.getVariablesUsed();
        this.hla = agent;
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
        String resource = SiteMinderAssertionUtil.extractContextVarValue(assertion.getProtectedResource(), variableMap, getAudit());
        String action = SiteMinderAssertionUtil.extractContextVarValue(assertion.getAction(), variableMap, getAudit());

        //TODO: replace with real siteMinder config from layer7-siteminder module
        checkSmAgentConfig();
        SiteMinderContext smContext = new SiteMinderContext();
        smContext.setAgentId(getAgentIdOrDefault());//set agent config
        String transactionId = UUID.randomUUID().toString();//generate SiteMinder transaction id.
        smContext.setTransactionId(transactionId);
        try {
            //check if protected and return AssertionStatus.NONE if it is
            if(hla.checkProtected(getAgentIdOrDefault(), getClientIp(context), resource, action, smContext)) {
                status = AssertionStatus.NONE;
            }
            populateContextVariables(context, varPrefix, smContext);

        } catch (SiteMinderApiClassException e) {
            logAndAudit(AssertionMessages.SITEMINDER_ERROR, e.getMessage());
            return AssertionStatus.FAILED;//something really bad happened
        }

        return status;
    }

    private void populateContextVariables(PolicyEnforcementContext pac, String prefix, SiteMinderContext context) {
        pac.setVariable(prefix + "." + "smcontext", context);
    }

    private String getClientIp(PolicyEnforcementContext context) {
        //TODO: use message targetable setting to get remote address
        //in case we don't have tcp knob use
        String address = null;
        Message target = context.getRequest();
        TcpKnob tcpKnob = target.getTcpKnob();
        if(tcpKnob != null)
            address = tcpKnob.getRemoteAddress();

        return address;
    }

    //TODO: replace with the proper one that reads configuration from the database
    private void checkSmAgentConfig() throws PolicyAssertionException {
        try {
            final String smAgentConfig = config.getProperty("smAgentConfig");
            if(hla == null || hla.checkConfigModified(smAgentConfig, getAgentIdOrDefault())){
                hla = new SiteMinderHighLevelAgent(smAgentConfig, getAgentIdOrDefault());
            }
        } catch (SiteMinderAgentConfigurationException e) {
            throw new PolicyAssertionException(assertion, "SiteMinder agent configuration is invalid", ExceptionUtils.getDebugException(e));
        } catch (SiteMinderApiClassException e) {
            throw new PolicyAssertionException(assertion, "SiteMinder agent API exception", e);
        }
    }

    private String getAgentIdOrDefault() {
        return assertion.getAgentID();  //To change body of created methods use File | Settings | File Templates.
    }
}
