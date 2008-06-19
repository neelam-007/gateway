/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.policy.assertion;

/**
 * @author alex
 */
public interface AssertionTranslator {
    /**
     * Given a source {@link Assertion}, return a replacement Assertion.  This can be used to replace {@link Include}
     * assertions with their contents prior to building AssertionPaths.
     * @param sourceAssertion the source assertion that may be replaced.  Must not be null.
     * @return a target Assertion (may be the sourceAssertion).  Never null.
     * @throws PolicyAssertionException if the source assertion cannot be translated.
     */
    Assertion translate(Assertion sourceAssertion) throws PolicyAssertionException;

    /**
     * Notifies the translator that processing of the specified assertion has completed. The translator can clean
     * up any state that is related to the assertion.
     * @param sourceAssertion The assertion that has finished getting processed
     */
    void translationFinished(Assertion sourceAssertion);
}
