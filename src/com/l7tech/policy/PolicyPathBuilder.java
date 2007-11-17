package com.l7tech.policy;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.common.policy.Policy;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.ReadOnlyEntityManager;

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
     * Generate the policy path result (policy assertion paths for
     * the <code>Assertion</code> tree.
     *
     * @param assertion the assertion tree to attempt the build
     * path for.
     * @return the result of the build
     */
    abstract public PolicyPathResult generate(Assertion assertion) throws InterruptedException, PolicyAssertionException;
}
