package com.l7tech.external.assertions.manipulatemultivaluedvariable.server;

import com.l7tech.external.assertions.manipulatemultivaluedvariable.ManipulateMultiValuedVariableAssertion;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.util.ExceptionUtils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.*;


/**
 * Server side implementation of the ManipulateMultiValuedVariableAssertion.
 *
 * @see com.l7tech.external.assertions.manipulatemultivaluedvariable.ManipulateMultiValuedVariableAssertion
 */
public class ServerManipulateMultiValuedVariableAssertion extends AbstractServerAssertion<ManipulateMultiValuedVariableAssertion> {

    public ServerManipulateMultiValuedVariableAssertion( final ManipulateMultiValuedVariableAssertion assertion ) throws PolicyAssertionException {
        super(assertion);
        final String varName = assertion.getVariableName();
        if (varName == null || varName.trim().isEmpty()) {
            throw new PolicyAssertionException(assertion, "Variable Name must be supplied");
        }

        final List<String> varsUsed = new ArrayList<String>(Arrays.asList(assertion.getVariablesUsed()));
        varsUsed.add(assertion.getVariableName());

        this.variablesUsed = varsUsed.toArray(new String[varsUsed.size()]);
    }

    /**
     * If the backing List is unmodifiable or an Array then this assertion will fail. In the case of an unmodifiable
     * List the runtime exception will be caught.
     */
    @Override
    public AssertionStatus checkRequest( final PolicyEnforcementContext context ) throws IOException, PolicyAssertionException {

        final String varName = assertion.getVariableName();
        final Map<String,Object> variableMap = context.getVariableMap(variablesUsed, getAudit());
        final Object existingValue = variableMap.get(varName);
        final List<Object> multiVar;
        boolean created = false;
        if (existingValue != null) {
            if (!(existingValue instanceof List)) {
                logAndAudit(AssertionMessages.USERDETAIL_FINEST, "Variable " + varName + " is not a multi valued variable");
                return AssertionStatus.FALSIFIED;
            }
            multiVar = (List<Object>) existingValue;
            logAndAudit(AssertionMessages.USERDETAIL_FINEST, "Found existing variable " + varName);
        } else {
            multiVar = new ArrayList<Object>();
            created = true;
        }

        final List<Object> value = ExpandVariables.processNoFormat(Syntax.getVariableExpression(assertion.getVariableValue()), variableMap, getAudit());
        // if it's multi valued - support it. If a value is null, that is ok, keep it null
        for (Object o : value) {
            final Class<? extends Object> valuesClass = o.getClass();
            if (!isTypeValid(valuesClass)){
                logAndAudit(AssertionMessages.USERDETAIL_WARNING, "Type "+ o.getClass()+" is not supported. Cannot add it to multi valued variable.");
                return AssertionStatus.FALSIFIED;
            }

            try {
                multiVar.add(copyObject(o));
            } catch (RuntimeException e) {
                logAndAudit(AssertionMessages.USERDETAIL_WARNING,
                        new String[]{"Could not add variable value to variable " + varName + " due to : " +
                                ExceptionUtils.getMessage(e)}, ExceptionUtils.getDebugException(e));
                return AssertionStatus.FAILED;
            }
            logAndAudit(AssertionMessages.USERDETAIL_FINEST, "Added to variable " + varName + " value " + o);
        }

        context.setVariable(varName, multiVar);
        if (created) {
            logAndAudit(AssertionMessages.USERDETAIL_FINEST, "Created variable " + varName);
        } else {
            logAndAudit(AssertionMessages.USERDETAIL_FINEST, "Used existing variable " + varName);
        }

        return AssertionStatus.NONE;
    }

    //- PRIVATE
    private final String[] variablesUsed;
    private final List<Class> allSupportedTypes = Collections.<Class>unmodifiableList(
            Arrays.asList(
                    String.class, Integer.class, Double.class, Float.class, Boolean.class, Date.class));

    private boolean isTypeValid(Class value) {

        for (Class supportedType : allSupportedTypes) {
            if (supportedType.isAssignableFrom(value)) {
                return true;
            }
        }

        return false;
    }

    @NotNull
    private Object copyObject(Object copyMe) {
        Object retVal = null;
        if (copyMe instanceof String) {
            retVal =  new String((String) copyMe);
        } else if (copyMe instanceof Integer) {
            retVal = new Integer((Integer) copyMe);
        } else if (copyMe instanceof Double) {
            retVal = new Double((Double) copyMe);
        } else if (copyMe instanceof Float) {
            retVal = new Float((Float) copyMe);
        } else if (copyMe instanceof Boolean) {
            retVal = new Boolean((Boolean) copyMe);
        } else if (copyMe instanceof Date) {
            retVal = new Date(((Date) copyMe).getTime());
        }

        if (retVal == null) {
            throw new RuntimeException("Unexpected type found: " + copyMe.getClass());
        }
        return retVal;
    }
}
