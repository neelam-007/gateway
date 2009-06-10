/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.policy.assertion;

import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.server.audit.Auditor;
import com.l7tech.policy.assertion.*;
import com.l7tech.server.message.PolicyEnforcementContext;
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
public class ServerSqlAttackAssertion extends AbstractMessageTargetableServerAssertion<SqlAttackAssertion> {
    private static final Logger logger = Logger.getLogger(ServerSqlAttackAssertion.class.getName());
    private final Auditor auditor;
    private final ArrayList<AbstractServerAssertion<? extends Assertion>> children =
                                    new ArrayList<AbstractServerAssertion<? extends Assertion>>();

    public ServerSqlAttackAssertion(SqlAttackAssertion assertion, ApplicationContext springContext) {
        super(assertion, assertion);
        auditor = new Auditor(this, springContext, logger);
        boolean abort = false;

        // Set up children to do all the actual work
        Set prots = assertion.getProtections();
        //noinspection ForLoopReplaceableByForEach
        for (Iterator i = prots.iterator(); i.hasNext();) {
            String prot = (String)i.next();
            String regex = SqlAttackAssertion.getProtectionRegex(prot);
            if (regex == null) {
                auditor.logAndAudit(AssertionMessages.SQLATTACK_UNRECOGNIZED_PROTECTION, prot);
                abort = true;
                break;
            }

            Regex ra = new Regex();
            ra.setAutoTarget(false);
            ra.setRegex(regex);
            ra.setCaseInsensitive(false);
            ra.setProceedIfPatternMatches(false);
            ra.setReplace(false);
            ServerRegex sr = new ServerRegex(ra, springContext);
            children.add(sr);
        }

        if (abort) {
            children.clear();
            children.add(new ServerFalseAssertion(new FalseAssertion()));
        }
    }

    @Override
    public AssertionStatus checkRequest(PolicyEnforcementContext context)
            throws PolicyAssertionException, IOException
    {
        if ( isRequest() && context.isPostRouting() ) {
            auditor.logAndAudit(AssertionMessages.SQLATTACK_ALREADY_ROUTED);
            return AssertionStatus.FAILED;
        }

        if ( isResponse() && !context.isPostRouting() ) {
            auditor.logAndAudit(AssertionMessages.SQLATTACK_SKIP_RESPONSE_NOT_ROUTED);
            return AssertionStatus.NONE;
        }

        TargetMessageType target = getAssertion().getTarget();
        for (AbstractServerAssertion<? extends Assertion> serverAssertion : children) {
            Assertion assertion = serverAssertion.getAssertion();
            if ( assertion instanceof MessageTargetable ) {
                MessageTargetable messageTargetable = (MessageTargetable) assertion;
                messageTargetable.setTarget(target);
                messageTargetable.setOtherTargetMessageVariable(getAssertion().getOtherTargetMessageVariable());
            }
            AssertionStatus result = serverAssertion.checkRequest(context);
            if (AssertionStatus.NONE != result) {
                auditor.logAndAudit(AssertionMessages.SQLATTACK_REJECTED, getAssertion().getTargetName());
                return getBadMessageStatus();
            }
        }

        return AssertionStatus.NONE;
    }

    @Override
    protected Auditor getAuditor() {
        return auditor;
    }
}
