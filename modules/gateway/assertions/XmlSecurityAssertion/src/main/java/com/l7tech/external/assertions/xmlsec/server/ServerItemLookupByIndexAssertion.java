package com.l7tech.external.assertions.xmlsec.server;

import com.l7tech.external.assertions.xmlsec.ItemLookupByIndexAssertion;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.policy.variable.VariableNameSyntaxException;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.policy.assertion.AssertionStatusException;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.util.ExceptionUtils;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 *
 */
public class ServerItemLookupByIndexAssertion extends AbstractServerAssertion<ItemLookupByIndexAssertion> {
    private final String indexValue;
    private final String multivaluedVariableName;
    private final String outputVariableName;
    private final String[] varsUsed;


    public ServerItemLookupByIndexAssertion(ItemLookupByIndexAssertion assertion) {
        super(assertion);
        this.indexValue = assertion.getIndexValue();
        this.multivaluedVariableName = assertion.getMultivaluedVariableName();
        this.outputVariableName = assertion.getOutputVariableName();
        this.varsUsed = assertion.getVariablesUsed();
    }

    @Override
    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        try {
            final Map<String,Object> varMap = context.getVariableMap(varsUsed, getAudit());
            long lindex = Math.round(Double.parseDouble(ExpandVariables.process(indexValue, varMap, getAudit(), true, 64)));
            if (lindex < 0) throw new NumberFormatException();
            if (lindex > Integer.MAX_VALUE) throw new NumberFormatException();

            Object multival = context.getVariable(multivaluedVariableName);
            Object value = getItemAtIndex(multivaluedVariableName, multival, (int) lindex);

            context.setVariable(outputVariableName, value);
            return AssertionStatus.NONE;

        } catch (NoSuchVariableException e) {
            logAndAudit(AssertionMessages.NO_SUCH_VARIABLE, e.getVariable());
            return AssertionStatus.SERVER_ERROR;
        } catch (VariableNameSyntaxException e) {
            logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[] { "Bad variable syntax: " + ExceptionUtils.getMessage(e) }, ExceptionUtils.getDebugException(e));
            return AssertionStatus.SERVER_ERROR;
        } catch (NumberFormatException e) {
            logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, "Index value did not round to a nonnegative integer less than or equal to " + Integer.MAX_VALUE);
            return AssertionStatus.SERVER_ERROR;
        } catch (IndexOutOfBoundsException e) {
            logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, "Index value is out of bounds");
            return AssertionStatus.SERVER_ERROR;
        }
    }

    private Object getItemAtIndex(String varname, Object multival, int index) {
        if (multival instanceof Object[]) {
            return ((Object[]) multival)[index];
        } else if (multival instanceof List) {
            return ((List) multival).get(index);
        } else if (multival instanceof Collection) {
            Collection collection = (Collection) multival;
            int idx = 0;
            for (Object obj : collection) {
                if (idx == index)
                    return obj;
                idx++;
            }
            throw new IndexOutOfBoundsException("Collection contains fewer than " + (index + 1) + " items");
        } else if (index == 0) {
            return multival;
        } else {
            final String msg = "Not a multi-valued context variable: " + varname;
            logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, msg);
            throw new AssertionStatusException(AssertionStatus.SERVER_ERROR, msg);
        }
    }
}
