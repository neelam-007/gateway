package com.l7tech.server.policy.assertion;

import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.audit.AuditDetailMessage;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.AuditDetailAssertion;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.server.audit.AuditLogFormatter;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.variable.ExpandVariables;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.LogRecord;
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
    private static final AuditLogFormatter formatter = new AuditLogFormatter();
    private Logger logger;
    private Auditor auditor;
    private final String[] varsUsed;
    private final AuditDetailMessage detailMessage;
    private final ApplicationContext springContext;

    public ServerAuditDetailAssertion(AuditDetailAssertion subject, ApplicationContext springContext) {
        super(subject);
        this.springContext = springContext;
        varsUsed = subject.getVariablesUsed();
        detailMessage = findDetailMessage(Level.parse(subject.getLevel()));
    }

    @Override
    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        // Check if logger name includes any invalid suffix, then create logger
        final String customLoggerSuffix = assertion.getCustomLoggerSuffix();
        String loggerName = customLoggerSuffix != null ? "com.l7tech.log.custom." + customLoggerSuffix : ServerAuditDetailAssertion.class.getName();

        final String loggerNameVars[] = Syntax.getReferencedNames(loggerName);
        final StringBuilder invalidVars = new StringBuilder();
        for (int i = 0; i < loggerNameVars.length; i++) {
            String loggerNameVar = loggerNameVars[i];
            if (ExpandVariables.process("${" + loggerNameVar + "}", context.getVariableMap(varsUsed, getAudit()), getAudit()).trim().isEmpty()) {
                if (i > 0 && invalidVars.length() > 0) invalidVars.append(", ");
                invalidVars.append(loggerNameVar);
            }
        }
        if (invalidVars.length() > 0) {
            loggerName = ServerAuditDetailAssertion.class.getName();
        } else {
            loggerName = ExpandVariables.process(loggerName, context.getVariableMap(varsUsed, getAudit()), getAudit());
        }

        logger = Logger.getLogger(loggerName);
        auditor = new Auditor(this, springContext, logger);

        if (invalidVars.length() > 0) {
            auditor.logAndAudit(AssertionMessages.CUSTOM_LOGGER_NAME_FALLBACK, invalidVars.toString(), ServerAuditDetailAssertion.class.getName());
        }

        String detail = assertion.getDetail();
        detail = ExpandVariables.process(detail, context.getVariableMap(varsUsed, getAudit()), getAudit());

        if (assertion.isLoggingOnly()) {
            LogRecord record = new LogRecord(detailMessage.getLevel(), formatter.formatDetail(detailMessage));
            record.setParameters(new String[] { detail });
            record.setSourceClassName("");
            record.setSourceMethodName("");
            record.setLoggerName(logger.getName());
            logger.log(record);
        } else {
            auditor.logAndAudit(detailMessage, detail);
        }

        return AssertionStatus.NONE;
    }

    private AuditDetailMessage findDetailMessage(Level level) {
        if (Level.FINEST.equals(level))
            return AssertionMessages.USERDETAIL_FINEST;
        if (Level.FINER.equals(level))
            return AssertionMessages.USERDETAIL_FINER;
        if (Level.FINE.equals(level))
            return AssertionMessages.USERDETAIL_FINE;
        if (Level.INFO.equals(level))
            return AssertionMessages.USERDETAIL_INFO;
        if (Level.WARNING.equals(level))
            return AssertionMessages.USERDETAIL_WARNING;
        throw new RuntimeException("unexpected level " + assertion.getLevel());
    }
}