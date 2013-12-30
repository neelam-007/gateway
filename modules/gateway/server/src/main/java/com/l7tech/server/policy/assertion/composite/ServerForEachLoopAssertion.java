package com.l7tech.server.policy.assertion.composite;

import com.l7tech.gateway.common.LicenseException;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.composite.ForEachLoopAssertion;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.server.message.PolicyEnforcementContext;
import org.jboss.util.collection.ArrayIterator;
import org.springframework.beans.factory.BeanFactory;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

/**
 * Server side implementation of the ForEachLoopAssertion.
 *
 * @see com.l7tech.policy.assertion.composite.ForEachLoopAssertion
 */
public class ServerForEachLoopAssertion extends ServerCompositeAssertion<ForEachLoopAssertion> {

    private final String multivaluedVar;
    private final String currentValueVar;
    private final int iterationLimit;
    private final String iterationsVar;
    private final String hitLimitVar;
    private final String breakVar;

    private final AssertionResultListener assertionResultListener = new AssertionResultListener() {
        @Override
        public boolean assertionFinished(PolicyEnforcementContext context, AssertionStatus result) {
            if (result != AssertionStatus.NONE) {
                return false;
            }
            return true;
        }
    };

    public ServerForEachLoopAssertion(ForEachLoopAssertion assertion, BeanFactory beanFactory) throws PolicyAssertionException, LicenseException {
        super(assertion, beanFactory);
        this.multivaluedVar = assertion.getLoopVariableName();
        this.iterationLimit = assertion.getIterationLimit();
        final String prefix = assertion.getVariablePrefix();

        this.currentValueVar = prefix + ".current";
        this.iterationsVar = prefix + ".iterations";
        this.hitLimitVar = prefix + ".exceededlimit";
        this.breakVar = prefix + ForEachLoopAssertion.BREAK;
    }

    @Override
    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        final Object values = findValues(context);
        final Iterator iterator = makeIterator(values);

        int iterations = 0;
        boolean hitLimit = false;
        boolean failed = false;
        if (!variableExists(context, breakVar))
            context.setVariable(breakVar, Boolean.FALSE);
        context.setVariable(hitLimitVar, hitLimit);
        while (iterator.hasNext()) {
            if (iterationLimit > 0 && iterations >= iterationLimit) {
                hitLimit = true;
                break;
            }
            Object next = iterator.next();
            context.setVariable(currentValueVar, next);
            context.setVariable(iterationsVar, iterations);
            AssertionStatus status = iterateChildren(context, assertionResultListener);
            //always check status first
            if (!AssertionStatus.NONE.equals(status)) {
                failed = true;
                break;
            }
            else if (isExitLoop(context)) {
                break;
            }

            iterations++;
        }

        context.setVariable(iterationsVar, iterations);
        context.setVariable(hitLimitVar, hitLimit);
        return failed ? AssertionStatus.FAILED : AssertionStatus.NONE;
    }

    private boolean variableExists(PolicyEnforcementContext context, String var) {
        try {
            context.getVariable(var);
            return true;
        } catch (NoSuchVariableException e) {
            return false;
        }
    }

    private boolean isExitLoop(PolicyEnforcementContext context) {
        boolean exit;
        try {
            final Object breakValue = context.getVariable(breakVar);
            exit = isTrue(breakValue);
        } catch (NoSuchVariableException e) {
            return false;
        }
        return exit;
    }

    private boolean isTrue(Object val) {
        if (val instanceof Boolean) {
            return (Boolean) val;
        } else if (val instanceof String) {
            String s = (String) val;
            return Boolean.parseBoolean(s);
        } else if (val instanceof Number) {
            Number number = (Number) val;
            return 0L != number.longValue();
        } else {
            return false;
        }
    }


    private Object findValues(PolicyEnforcementContext context) {
        // Use getVariableMap() rather than getVariable() so that empty collections do not result in NoSuchVariableException (Bug #12309)
        Map<String, Object> map = context.getVariableMap(new String[]{multivaluedVar}, getAudit());
        final Object multivaluedObj = map.get(multivaluedVar);

        if (multivaluedObj == null) {
            // Might have been a selector trying to return an empty collection, so treat as empty collection for iteration purposes (Bug #12309)
            return Collections.emptyList();
        }

        return multivaluedObj;
    }

    private static Iterator makeIterator(Object values) {
        final Iterator iterator;
        if (values instanceof Object[]) {
            iterator = new ArrayIterator((Object[])values);
        } else if (values instanceof Collection) {
            iterator = ((Collection)values).iterator();
        } else {
            iterator = Collections.singletonList(values).iterator();
        }
        return iterator;
    }
}
