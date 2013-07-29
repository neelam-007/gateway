package com.l7tech.external.assertions.siteminder.server;

import com.ca.siteminder.SiteMinderApiClassException;
import com.ca.siteminder.SiteMinderContext;
import com.ca.siteminder.SiteMinderHighLevelAgent;
import com.l7tech.external.assertions.siteminder.util.SiteMinderAssertionUtil;
import com.l7tech.message.Message;
import com.l7tech.message.TcpKnob;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.siteminder.SiteMinderConfigurationManager;
import com.l7tech.util.ExceptionUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.ApplicationContext;

/**
 * Copyright: Layer 7 Technologies, 2013
 * User: ymoiseyenko
 * Date: 7/16/13
 */
public abstract class AbstractServerSiteMinderAssertion<AT extends Assertion> extends AbstractServerAssertion<AT>{
    protected SiteMinderHighLevelAgent hla;

    protected ApplicationContext applicationContext;
    protected SiteMinderConfigurationManager manager;

    public AbstractServerSiteMinderAssertion(@NotNull final AT assertion, final ApplicationContext applicationContext) {
        super(assertion);
        this.hla = applicationContext.getBean("siteMinderHighLevelAgent", SiteMinderHighLevelAgent.class);
        this.manager = applicationContext.getBean("siteMinderConfigurationManager", SiteMinderConfigurationManager.class);
    }

    protected void initSmAgentFromContext(String agentId, SiteMinderContext context) throws PolicyAssertionException {
        try {
            if (context.getAgent() == null) {
                context.setAgent(manager.getSiteMinderLowLevelAgent(agentId));
            }
        } catch (SiteMinderApiClassException e) {
            throw new PolicyAssertionException(assertion, "SiteMinder agent API exception", e);
        } catch (FindException e) {
            throw new PolicyAssertionException(assertion, "No SiteMinder agent configuration", ExceptionUtils.getDebugException(e));
        }
    }

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
