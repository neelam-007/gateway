package com.l7tech.policy;

import com.l7tech.policy.assertion.Assertion;

import java.util.logging.Logger;

/**
 * A class for building policy assertion paths.
 *
 * To create a <code>PolicyPathBuilder</code>, call one of the static factory
 * methods.

 * the result is returned in an object of <code>PolicyPathResult</code> type.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com>Emil Marceta</a>
 * @version 1.0
 */
public abstract class PolicyPathBuilder {
    static Logger log = Logger.getLogger(PolicyPathBuilder.class.getName());

    /**
     * Protected constructor, the <code>PolicyPathBuilder</code> instances
     * are obtained using factory methods.
     */
    protected PolicyPathBuilder() {
    }

    /**
     * Obtain the default policy validator
     *
     * @return the policy validator instance
     */
    public static PolicyPathBuilder getDefault() {
        return new DefaultPolicyPathBuilder();
    }

    /**
     * Generate the policy path result (policy assertion paths for
     * the <code>Assertion</code> tree.
     *
     * @param assertion the assertion tree to attempt the build
     * path for.
     * @return the result of the build
     */
    abstract public PolicyPathResult generate(Assertion assertion);
}
