package com.l7tech.server.search.objects;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.search.Dependency;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a dependent assertion.
 *
 * @author Victor Kazakov
 */
public class DependentAssertion<A extends Assertion> extends DependentObject {

    @NotNull
    private final Class<A> assertionClass;

    /**
     * Creates a new DependentAssertion
     *
     * @param name           The name of the assertion
     * @param assertionClass The assertion class
     */
    public DependentAssertion(@NotNull final String name, @NotNull final Class<A> assertionClass) {
        super(name, Dependency.DependencyType.ASSERTION);
        this.assertionClass = assertionClass;
    }

    /**
     * Returns the assertion class
     *
     * @return the assertion class
     */
    @NotNull
    public Class<A> getAssertionClass() {
        return assertionClass;
    }

    @Override
    public boolean equals(Object o) {
        // An assertion dependent object must not equal another.
        // If they are equal we will assume that their dependencies are the same, but we don't have enough information here to know if this is true so just return false
        // For example two assertions can be the same type (assertion class) but they can have different dependencies
        return false;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + assertionClass.hashCode();
        return result;
    }
}
