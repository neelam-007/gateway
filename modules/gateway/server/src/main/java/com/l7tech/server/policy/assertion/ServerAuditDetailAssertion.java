package com.l7tech.server.policy.assertion;

import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.server.audit.Auditor;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.AuditDetailAssertion;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.server.message.PolicyEnforcementContext;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
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
public class ServerAuditDetailAssertion extends AbstractServerAssertion<AuditDetailAssertion>  {
    private AuditDetailAssertion subject;
    private final Auditor auditor;
    private final String[] varsUsed;
    private Logger logger = Logger.getLogger(ServerAuditDetailAssertion.class.getName());

    public ServerAuditDetailAssertion(AuditDetailAssertion subject, ApplicationContext springContext) {
        super(subject);
        this.subject = subject;
        auditor = new Auditor(this, springContext, logger);
        varsUsed = subject.getVariablesUsed();
    }

    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        String detail = subject.getDetail();
        detail = ExpandVariables.process(detail, context.getVariableMap(varsUsed, auditor), auditor);

        if (Level.FINEST.toString().equals(subject.getLevel())) {
            auditor.logAndAudit(AssertionMessages.USERDETAIL_FINEST, detail);
        } else if (Level.FINER.toString().equals(subject.getLevel())) {
            auditor.logAndAudit(AssertionMessages.USERDETAIL_FINER, detail);
        } else if (Level.FINE.toString().equals(subject.getLevel())) {
            auditor.logAndAudit(AssertionMessages.USERDETAIL_FINE, detail);
        } else if (Level.INFO.toString().equals(subject.getLevel())) {
            auditor.logAndAudit(AssertionMessages.USERDETAIL_INFO, detail);
        } else if (Level.WARNING.toString().equals(subject.getLevel())) {
            auditor.logAndAudit(AssertionMessages.USERDETAIL_WARNING, detail);
        } else {
            // can't happen
            throw new RuntimeException("unexpected level " + subject.getLevel());
        }
        return AssertionStatus.NONE;
    }
}
