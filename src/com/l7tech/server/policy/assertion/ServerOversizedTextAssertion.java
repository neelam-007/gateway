/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.server.policy.assertion;

import com.l7tech.common.audit.AssertionMessages;
import com.l7tech.common.audit.Auditor;
import com.l7tech.common.audit.Messages;
import com.l7tech.common.xml.XpathExpression;
import com.l7tech.policy.PolicyFactory;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.ServerPolicyFactory;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * Server side implementation of the OversizedTextAssertion convenience assertion.
 * Internally this is implemented, essentially, as just a nested xpath assertion.
 */
public class ServerOversizedTextAssertion implements ServerAssertion {
    private static final Logger logger = Logger.getLogger(ServerOversizedTextAssertion.class.getName());
    private final Auditor auditor;
    private final ServerAssertion delegate;

    public ServerOversizedTextAssertion(OversizedTextAssertion data, ApplicationContext springContext) {
        auditor = new Auditor(this, springContext, ServerOversizedTextAssertion.logger);

        PolicyFactory pf = (ServerPolicyFactory)springContext.getBean("policyFactory");
        if (pf == null || !(pf instanceof ServerPolicyFactory)) {
            auditor.logAndAudit(Messages.EXCEPTION_SEVERE, new String[] {"Missing or invalid policyFactory bean"});
            delegate = new ServerFalseAssertion(new FalseAssertion()); // simulate detecting attack every time
        } else {
            ServerPolicyFactory policyFactory = (ServerPolicyFactory)pf;

            // Set up children to do all the actual work of asserting that there is no attack
            CompositeAssertion all = new AllAssertion();
            all.addChild(new RequestXpathAssertion(new XpathExpression(data.makeTextXpath(), null)));
            all.addChild(new RequestXpathAssertion(new XpathExpression(data.makeAttrXpath(), null)));
            delegate = policyFactory.makeServerPolicy(all);
        }
    }

    public AssertionStatus checkRequest(PolicyEnforcementContext context)
            throws PolicyAssertionException, IOException
    {
        if (ServerRegex.isPostRouting(context)) {
            auditor.logAndAudit(AssertionMessages.OVERSIZEDTEXT_ALREADY_ROUTED);
            return AssertionStatus.FAILED;
        }

        AssertionStatus result = delegate.checkRequest(context);
        if (AssertionStatus.NONE != result) {
            auditor.logAndAudit(AssertionMessages.OVERSIZEDTEXT_REQUEST_REJECTED);
            return AssertionStatus.BAD_REQUEST;
        }

        return AssertionStatus.NONE;
    }
}
