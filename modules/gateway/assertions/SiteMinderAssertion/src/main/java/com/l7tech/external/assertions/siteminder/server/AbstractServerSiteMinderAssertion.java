package com.l7tech.external.assertions.siteminder.server;

import com.ca.siteminder.SiteMinderAgentConfigurationException;
import com.ca.siteminder.SiteMinderApiClassException;
import com.ca.siteminder.SiteMinderContext;
import com.ca.siteminder.SiteMinderHighLevelAgent;
import com.l7tech.external.assertions.siteminder.util.SiteMinderAssertionUtil;
import com.l7tech.message.Message;
import com.l7tech.message.TcpKnob;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.util.Config;
import com.l7tech.util.ExceptionUtils;
import org.jetbrains.annotations.NotNull;

/**
 * Copyright: Layer 7 Technologies, 2013
 * User: ymoiseyenko
 * Date: 7/16/13
 */
public abstract class AbstractServerSiteMinderAssertion<AT extends Assertion> extends AbstractServerAssertion<AT>{
    protected Config config;
    protected SiteMinderHighLevelAgent hla;

    public AbstractServerSiteMinderAssertion(@NotNull final AT assertion) {
        super(assertion);
        this.hla = new SiteMinderHighLevelAgent();
    }

    public AbstractServerSiteMinderAssertion(@NotNull final AT assertion, SiteMinderHighLevelAgent agent, Config config) {
        super(assertion);
        this.hla = agent;
        this.config = config;
    }

    protected void initSmAgentFromContext(SiteMinderContext context) throws PolicyAssertionException {
        //TODO: we need to use a pool of configured agents that will be available to every assertion
        try {
//            return new SiteMinderHighLevelAgent(config.getProperty("smAgentConfig"), getAgentIdOrDefault(context));
             hla.checkAndInitialize(config.getProperty("smAgentConfig"), getAgentIdOrDefault(context));
        } catch (SiteMinderAgentConfigurationException e) {
            throw new PolicyAssertionException(assertion, "SiteMinder agent configuration is invalid", ExceptionUtils.getDebugException(e));
        } catch (SiteMinderApiClassException e) {
            throw new PolicyAssertionException(assertion, "SiteMinder agent API exception", e);
        }
    }

    protected abstract String getAgentIdOrDefault(SiteMinderContext context);

    protected void populateContextVariables(PolicyEnforcementContext pac, String prefix, SiteMinderContext context) {
        pac.setVariable(prefix + "." + SiteMinderAssertionUtil.SMCONTEXT, context);
/*        List<Pair<String, Object>> attrList = context.getAttrList();
        for(Pair<String, Object> attr : attrList){
            pac.setVariable(prefix + "." + attr.left, attr.right);
            logger.log(Level.FINE, "key: " + prefix + "." + attr.left + " value: " + attr.right);
        }*/
    }

    protected String getClientIp(PolicyEnforcementContext context) {
        //TODO: use message targetable setting to get remote address
        //in case we don't have tcp knob use
        String address = null;
        Message target = context.getRequest();
        TcpKnob tcpKnob = target.getTcpKnob();
        if(tcpKnob != null)
            address = tcpKnob.getRemoteAddress();

        return address;
    }
}
