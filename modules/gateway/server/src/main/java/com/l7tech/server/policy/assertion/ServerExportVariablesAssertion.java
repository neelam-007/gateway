package com.l7tech.server.policy.assertion;

import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.ExportVariablesAssertion;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.policy.variable.VariableNotSettableException;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.variable.ServerVariables;
import com.l7tech.util.ExceptionUtils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * Server implementation of ExportVariablesAssertion.
 */
public class ServerExportVariablesAssertion extends AbstractServerAssertion<ExportVariablesAssertion> {
    public ServerExportVariablesAssertion(@NotNull final ExportVariablesAssertion assertion) {
        super(assertion);
    }

    @Override
    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        final PolicyEnforcementContext rootContext = ServerVariables.getRootContext(context);

        String[] vars = assertion.getExportedVars();
        if (vars != null) {
            for (String var : vars) {
                copyToRootContextSharedBucket(var, context, rootContext);
            }
        }

        return AssertionStatus.NONE;
    }

    private void copyToRootContextSharedBucket(String var, PolicyEnforcementContext source, PolicyEnforcementContext rootContext) {
        Object value;
        try {
            value = source.getVariable(var);
        } catch (NoSuchVariableException e) {
            getAudit().logAndAudit(AssertionMessages.NO_SUCH_VARIABLE, var);
            return;
        } catch (RuntimeException e) {
            getAudit().logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[] {"Unable to read variable: " + ExceptionUtils.getMessage(e) }, ExceptionUtils.getDebugException(e));
            return;
        }

        try {
            rootContext.setVariable(ServerVariables.PREFIX_SHARED_BUCKET + "request.shared." + var, value);
        } catch (VariableNotSettableException e) {
            getAudit().logAndAudit(AssertionMessages.VARIABLE_NOTSET, var);
        } catch (RuntimeException e) {
            getAudit().logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[] {"Unable to set variable: " + ExceptionUtils.getMessage(e) }, ExceptionUtils.getDebugException(e));
        }
    }
}
