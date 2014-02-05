package com.l7tech.test.conditional;

/**
 * Service interface for ignoring tests.
 */
public interface IgnoreCondition {
    /**
     * @return <code>true</code> to indicate that the condition is satisfied
     *         (i.e. the unit-test annotated with {@link ConditionalIgnore} will be ignored), or <code>false</code>
     *         otherwise (i.e. the unit-test annotated with {@link ConditionalIgnore} will be executed).
     */
    boolean isSatisfied();
}
