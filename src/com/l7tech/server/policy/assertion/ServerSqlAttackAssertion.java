/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.policy.assertion;

import com.l7tech.common.audit.AssertionMessages;
import com.l7tech.common.audit.Auditor;
import com.l7tech.common.audit.Messages;
import com.l7tech.policy.PolicyFactory;
import com.l7tech.policy.assertion.*;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.ServerPolicyFactory;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Server side implementation of the SqlAttackAssertion all-in-one convenience assertion.
 * Internally this is implemented, essentially, as just zero or more regexp assertions.
 */
public class ServerSqlAttackAssertion extends AbstractServerAssertion implements ServerAssertion {
    private static final Logger logger = Logger.getLogger(ServerSqlAttackAssertion.class.getName());
    private final Auditor auditor;
    private final ArrayList children = new ArrayList();

    public ServerSqlAttackAssertion(SqlAttackAssertion data, ApplicationContext springContext) {
        super(data);
        auditor = new Auditor(this, springContext, logger);
        boolean abort = false;

        PolicyFactory pf = (ServerPolicyFactory)springContext.getBean("policyFactory");
        if (pf == null) {
            auditor.logAndAudit(Messages.EXCEPTION_SEVERE, new String[] {"No policyFactory bean was found"});
            abort = true;
        } else if (!(pf instanceof ServerPolicyFactory)) {
            auditor.logAndAudit(Messages.EXCEPTION_SEVERE, new String[] {"policyFactory bean was not a ServerPolicyFactory"});
            abort = true;
        } else {
            ServerPolicyFactory policyFactory = (ServerPolicyFactory)pf;

            // Set up children to do all the actual work
            Set prots = data.getProtections();
            for (Iterator i = prots.iterator(); i.hasNext();) {
                String prot = (String)i.next();
                String regex = SqlAttackAssertion.getProtectionRegex(prot);
                if (regex == null) {
                    auditor.logAndAudit(AssertionMessages.SQLATTACK_UNRECOGNIZED_PROTECTION,
                                        new String[] { prot });
                    abort = true;
                    break;
                }

                Regex ra = new Regex();
                ra.setRegex(regex);
                ra.setCaseInsensitive(false);
                ra.setProceedIfPatternMatches(false);
                ra.setReplace(false);
                ServerRegex sr = new ServerRegex(ra, springContext);
                children.add(sr);
            }
        }

        if (abort) {
            children.clear();
            children.add(new ServerFalseAssertion(new FalseAssertion()));
        }
    }

    public AssertionStatus checkRequest(PolicyEnforcementContext context)
            throws PolicyAssertionException, IOException
    {
        if (ServerRegex.isPostRouting(context)) {
            auditor.logAndAudit(AssertionMessages.SQLATTACK_ALREADY_ROUTED);
            return AssertionStatus.FAILED;
        }

        for (Iterator i = children.iterator(); i.hasNext();) {
            ServerAssertion serverAssertion = (ServerAssertion)i.next();
            AssertionStatus result = serverAssertion.checkRequest(context);
            if (AssertionStatus.NONE != result) {
                auditor.logAndAudit(AssertionMessages.SQLATTACK_REQUEST_REJECTED);
                return AssertionStatus.BAD_REQUEST;
            }
        }

        return AssertionStatus.NONE;
    }
}
