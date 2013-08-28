package com.l7tech.external.assertions.siteminder.server;

import com.ca.siteminder.SiteMinderApiClassException;
import com.ca.siteminder.SiteMinderContext;
import com.ca.siteminder.SiteMinderHighLevelAgent;
import com.l7tech.external.assertions.siteminder.util.SiteMinderAssertionUtil;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.message.Message;
import com.l7tech.message.TcpKnob;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.siteminder.SiteMinderConfigurationManager;
import com.l7tech.util.ConfigFactory;
import com.l7tech.util.ExceptionUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.ApplicationContext;

/**
 * Copyright: Layer 7 Technologies, 2013
 * User: ymoiseyenko
 * Date: 7/16/13
 */
public abstract class AbstractServerSiteMinderAssertion<AT extends Assertion> extends AbstractServerAssertion<AT>{
    public static final String SYSTEM_PROPERTY_SITEMINDER_ENABLED = "com.l7tech.server.siteminder.enabled";
    static final int SM_SUCCESS = 0;
    static final int SM_YES = 1;
    static final int SM_NO = 2;
    static final int SM_FAIL = -1;

    protected SiteMinderHighLevelAgent hla;

    protected SiteMinderConfigurationManager manager;

    public AbstractServerSiteMinderAssertion(@NotNull final AT assertion, final ApplicationContext applicationContext) throws  PolicyAssertionException{
        super(assertion);
        checkSiteMinderEnabled();//check if SiteMinder SDK is installed
        this.hla = applicationContext.getBean("siteMinderHighLevelAgent", SiteMinderHighLevelAgent.class);
        this.manager = applicationContext.getBean("siteMinderConfigurationManager", SiteMinderConfigurationManager.class);
    }

    protected boolean initSmAgentFromContext(Goid agentGoid, SiteMinderContext context) throws PolicyAssertionException {
        try {
            if (context.getAgent() == null) {
                context.setAgent(manager.getSiteMinderLowLevelAgent(agentGoid));
            }
        } catch (SiteMinderApiClassException e) {
            logAndAudit(AssertionMessages.SITEMINDER_ERROR, (String)assertion.meta().get(AssertionMetadata.SHORT_NAME), "SiteMinder agent API exception occurred, agent Goid=" + agentGoid);
            throw new PolicyAssertionException(assertion, "SiteMinder agent API exception", ExceptionUtils.getDebugException(e));
        } catch (FindException e) {
            logAndAudit(AssertionMessages.SITEMINDER_ERROR, (String)assertion.meta().get(AssertionMetadata.SHORT_NAME), "Unable to find SiteMinder agent configuration, agent Goid=" + agentGoid);
            throw new PolicyAssertionException(assertion, "No SiteMinder agent configuration", ExceptionUtils.getDebugException(e));
        } catch (IllegalStateException e) {
            logAndAudit(AssertionMessages.SITEMINDER_ERROR, (String)assertion.meta().get(AssertionMetadata.SHORT_NAME), e.getMessage());
            return false;
        }
        return true;
    }

    protected void populateContextVariables(PolicyEnforcementContext pac, String prefix, SiteMinderContext context) {
        pac.setVariable(prefix + "." + SiteMinderAssertionUtil.SMCONTEXT, context);
    }

    protected String getClientIp(PolicyEnforcementContext context) {
        //TODO: use message targetable setting to get remote address
        //in case we don't have tcp knob use predefined address from the SiteMinderConfig
        String address = null;
        Message target = context.getRequest();
        try {
            TcpKnob tcpKnob = target.getTcpKnob();
            address = tcpKnob.getRemoteAddress();
        } catch (IllegalStateException e) {
            logAndAudit(AssertionMessages.SITEMINDER_FINE, (String)assertion.meta().get(AssertionMetadata.SHORT_NAME), "Client IP address is null!");
        }

        return address;
    }

    protected void checkSiteMinderEnabled() throws  PolicyAssertionException {
        if(!ConfigFactory.getBooleanProperty(SYSTEM_PROPERTY_SITEMINDER_ENABLED, true)) {
            logAndAudit(AssertionMessages.SITEMINDER_ERROR, (String)assertion.meta().get(AssertionMetadata.SHORT_NAME), "SiteMinder SDK is not installed or misconfigured! Assertion failed");
            throw new PolicyAssertionException(assertion, "SiteMinder SDK not installed or misconfigured!");
        }
    }
}
