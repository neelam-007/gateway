package com.l7tech.policy;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.PolicyAssertionException;

import java.util.logging.Logger;
import java.util.Set;
import java.util.List;

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
     * Modify the given policy to inline any policy includes.
     *
     * <p>This will change the structure of the given tree if there are any includes.</p>
     *
     * @param assertion The assertion to modify
     * @param includedGuids Set of included policy oids (optional)
     * @oparam includeDisabled True to inline policies even for disabled include assertions
     * @return the modified assertion.
     */
    public abstract Assertion inlineIncludes(Assertion assertion, Set<String> includedGuids, boolean includeDisabled) throws InterruptedException, PolicyAssertionException;

    /**
     * Generate the policy path result (policy assertion paths for
     * the <code>Assertion</code> tree.
     *
     * @param assertion the assertion tree to attempt the build
     * path for.
     * @return the result of the build
     */
    public PolicyPathResult generate(Assertion assertion) throws InterruptedException, PolicyAssertionException {
        return generate( assertion, true );
    }

    /**
     * Essentially a helper method that can pre-proecess the assertion for any include fragments problems.  If there
     * were any PolicyAssertionException found, then it will accumuldate into a list then returned back to the caller
     * to decide what to do with the exceptions.
     *
     * @param assertion The assertion to process for Include fragments
     * @return  Returns a list of PolicyAssertionException, if any.  Will never return NULL.
     */
    public abstract List<PolicyAssertionException> preProcessIncludeFragments( Assertion assertion );

    /**
     * Generate the policy path result (policy assertion paths for
     * the <code>Assertion</code> tree.
     *
     * @param assertion the assertion tree to attempt the build
     * path for.
     * @return the result of the build
     */
    abstract public PolicyPathResult generate(Assertion assertion, boolean processIncludes) throws InterruptedException, PolicyAssertionException;
}
