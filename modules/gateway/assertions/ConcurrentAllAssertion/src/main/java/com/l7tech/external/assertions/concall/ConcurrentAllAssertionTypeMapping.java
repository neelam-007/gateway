package com.l7tech.external.assertions.concall;

import com.l7tech.policy.wsp.CompositeAssertionMapping;

/**
 *
 */
public class ConcurrentAllAssertionTypeMapping extends CompositeAssertionMapping {
    public ConcurrentAllAssertionTypeMapping() {
        super(new ConcurrentAllAssertion(), "ConcurrentAll");
    }
}
