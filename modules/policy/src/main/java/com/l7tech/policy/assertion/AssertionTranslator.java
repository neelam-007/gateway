/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.policy.assertion;

import org.jetbrains.annotations.Nullable;

/**
 * @author alex
 */
public interface AssertionTranslator {
    /**
     * Given a source {@link Assertion}, return a replacement Assertion.  This can be used to replace {@link Include}
     * assertions with their contents prior to building AssertionPaths.
     * @param sourceAssertion the source assertion that may be replaced.  May be null.
     * @return a target Assertion (may be the sourceAssertion).  May be null only if the source assertion was null.
     * @throws PolicyAssertionException if the source assertion cannot be translated.
     */
    @Nullable Assertion translate(@Nullable Assertion sourceAssertion) throws PolicyAssertionException;

    /**
     * Notifies the translator that processing of the specified assertion has completed. The translator can clean
     * up any state that is related to the assertion.
     * @param sourceAssertion The assertion that has finished getting processed.  May be null.
     */
    void translationFinished(@Nullable Assertion sourceAssertion);
}
