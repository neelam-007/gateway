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
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        DependentAssertion that = (DependentAssertion) o;

        if (!assertionClass.equals(that.assertionClass)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + assertionClass.hashCode();
        return result;
    }
}
