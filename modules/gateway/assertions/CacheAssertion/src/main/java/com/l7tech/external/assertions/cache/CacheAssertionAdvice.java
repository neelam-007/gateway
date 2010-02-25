package com.l7tech.external.assertions.cache;

import com.l7tech.console.tree.policy.PolicyChange;
import com.l7tech.console.tree.policy.advice.Advice;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.MessageTargetable;
import com.l7tech.policy.assertion.TargetMessageType;

/**
 * Advice for cache assertion to set their default target RESPONSE,
 * overriding MessageTargetableAdvce's behaviour.
 * @author jbufu
 */
public class CacheAssertionAdvice implements Advice {

    @Override
    public void proceed(PolicyChange pc) {
        Assertion[] assertions = pc.getEvent().getChildren();
        if ( assertions == null || assertions.length != 1 || !(assertions[0] instanceof MessageTargetable) ||
             ( ! (assertions[0] instanceof CacheStorageAssertion) && ! (assertions[0] instanceof CacheLookupAssertion)) ) {
            throw new IllegalArgumentException();
        } else {
            ((MessageTargetable)assertions[0]).setTarget(TargetMessageType.RESPONSE);
        }
        pc.proceed();
    }
}
