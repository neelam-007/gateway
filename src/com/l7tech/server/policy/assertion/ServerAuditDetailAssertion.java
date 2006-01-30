package com.l7tech.server.policy.assertion;

import com.l7tech.common.audit.AssertionMessages;
import com.l7tech.common.audit.Auditor;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.AuditDetailAssertion;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.variable.ExpandVariables;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.server.message.PolicyEnforcementContext;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Server side implementation of the AuditDetailAssertion
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Jan 19, 2006<br/>
 */
public class ServerAuditDetailAssertion implements ServerAssertion {
    private AuditDetailAssertion subject;
    private final Auditor auditor;
    private final String[] varsUsed;
    private Logger logger = Logger.getLogger(ServerAuditDetailAssertion.class.getName());
    private final ExpandVariables expandVariables = new ExpandVariables();

    public ServerAuditDetailAssertion(AuditDetailAssertion subject, ApplicationContext springContext) {
        this.subject = subject;
        auditor = new Auditor(this, springContext, logger);
        varsUsed = subject.getVariablesUsed();
    }

    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        String detail = subject.getDetail();
        try {
            Map vars = context.getVariableMap(varsUsed);
            detail = expandVariables.process(detail, vars);
        } catch (NoSuchVariableException e) {
            logger.log(Level.WARNING, "cannot expand all variables", e);
        }

        if (Level.FINEST.toString().equals(subject.getLevel())) {
            auditor.logAndAudit(AssertionMessages.USERDETAIL_FINEST, new String[] {detail});
        } else if (Level.FINER.toString().equals(subject.getLevel())) {
            auditor.logAndAudit(AssertionMessages.USERDETAIL_FINER, new String[] {detail});
        } else if (Level.FINE.toString().equals(subject.getLevel())) {
            auditor.logAndAudit(AssertionMessages.USERDETAIL_FINE, new String[] {detail});
        } else if (Level.INFO.toString().equals(subject.getLevel())) {
            auditor.logAndAudit(AssertionMessages.USERDETAIL_INFO, new String[] {detail});
        } else if (Level.WARNING.toString().equals(subject.getLevel())) {
            auditor.logAndAudit(AssertionMessages.USERDETAIL_WARNING, new String[] {detail});
        } else {
            // can't happen
            throw new RuntimeException("unexpected level " + subject.getLevel());
        }
        return AssertionStatus.NONE;
    }
}
