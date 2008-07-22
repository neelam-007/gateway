package com.l7tech.policy;

import com.l7tech.policy.assertion.Assertion;

import java.util.Set;

/**
 * An AssertionFinder provides access to a set of Assertion subclasses, represented by prototype instances.
 * This access is read-only.  The {@link AssertionRegistry} abstract class extends this with the ability to
 * register new assertions.
 */
public interface AssertionFinder {
    /**
     * Get all assertion subclasses known to this AssertionFinder, represented by prototype instances.
     * The returned prototype instances should not be modified.
     *
     * @return a Set of Assertion prototype instances.  May be empty but never null.
     */
    Set<Assertion> getAssertions();
}
