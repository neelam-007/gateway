package com.l7tech.external.assertions.xmlsec.server;

import com.l7tech.external.assertions.xmlsec.ItemLookupByIndexAssertion;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.policy.variable.VariableNameSyntaxException;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.policy.assertion.AssertionStatusException;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.util.ExceptionUtils;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.ApplicationEventPublisher;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 *
 */
public class ServerItemLookupByIndexAssertion extends AbstractServerAssertion<ItemLookupByIndexAssertion> {
    private static final Logger logger = Logger.getLogger(ServerItemLookupByIndexAssertion.class.getName());

    private final Auditor auditor;
    private final String indexValue;
    private final String multivaluedVariableName;
    private final String outputVariableName;
    private final String[] varsUsed;


    public ServerItemLookupByIndexAssertion(ItemLookupByIndexAssertion assertion, BeanFactory beanFactory, ApplicationEventPublisher eventPub) {
        super(assertion);
        auditor = new Auditor(this, beanFactory, eventPub, logger);
        this.indexValue = assertion.getIndexValue();
        this.multivaluedVariableName = assertion.getMultivaluedVariableName();
        this.outputVariableName = assertion.getOutputVariableName();
        this.varsUsed = assertion.getVariablesUsed();
    }

    @Override
    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        try {
            final Map<String,Object> varMap = context.getVariableMap(varsUsed, auditor);
            int index = Integer.parseInt(ExpandVariables.process(indexValue, varMap, auditor, true, 64));
            if (index < 0) throw new NumberFormatException();

            Object multival = context.getVariable(multivaluedVariableName);
            Object value = getItemAtIndex(multivaluedVariableName, multival, index);

            context.setVariable(outputVariableName, value);
            return AssertionStatus.NONE;

        } catch (NoSuchVariableException e) {
            auditor.logAndAudit(AssertionMessages.NO_SUCH_VARIABLE, e.getVariable());
            return AssertionStatus.SERVER_ERROR;
        } catch (VariableNameSyntaxException e) {
            auditor.logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[] { "Bad variable syntax: " + ExceptionUtils.getMessage(e) }, ExceptionUtils.getDebugException(e));
            return AssertionStatus.SERVER_ERROR;
        } catch (NumberFormatException e) {
            auditor.logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, "Index value is not a nonnegative integer");
            return AssertionStatus.SERVER_ERROR;
        } catch (IndexOutOfBoundsException e) {
            auditor.logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, "Index value is out of bounds");
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
        } else {
            final String msg = "Not a multi-valued context variable: " + varname;
            auditor.logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, msg);
            throw new AssertionStatusException(AssertionStatus.SERVER_ERROR, msg);
        }
    }
}
