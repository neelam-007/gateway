package com.l7tech.server.policy.assertion.composite;

import com.l7tech.gateway.common.LicenseException;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.composite.ForEachLoopAssertion;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AssertionStatusException;
import org.jboss.util.collection.ArrayIterator;
import org.springframework.beans.factory.BeanFactory;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

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
    }

    @Override
    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        final Object values = findValues(context);
        final Iterator iterator = makeIterator(values);

        int iterations = 0;
        boolean hitLimit = false;
        boolean failed = false;
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
            if (!AssertionStatus.NONE.equals(status)) {
                failed = true;
                break;
            }
            iterations++;
        }

        context.setVariable(iterationsVar, iterations);
        context.setVariable(hitLimitVar, hitLimit);
        return failed ? AssertionStatus.FAILED : AssertionStatus.NONE;
    }

    private Object findValues(PolicyEnforcementContext context) {
        final Object multivaluedObj;
        try {
            multivaluedObj = context.getVariable(multivaluedVar);
        } catch (NoSuchVariableException e) {
            logAndAudit(AssertionMessages.NO_SUCH_VARIABLE, multivaluedVar);
            throw new AssertionStatusException(AssertionStatus.FAILED);
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
