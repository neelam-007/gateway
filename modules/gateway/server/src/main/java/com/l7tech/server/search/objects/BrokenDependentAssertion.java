package com.l7tech.server.search.objects;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.search.Dependency;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a dependent assertion that cannot be found on the gateway.
 *
 */
public class BrokenDependentAssertion<A extends Assertion> extends DependentObject {

    @NotNull
    private final Class<A> assertionClass;

    /**
     * Creates a new DependentAssertion
     *
     * @param assertionClass The assertion class
     */
    public BrokenDependentAssertion(@NotNull final Class<A> assertionClass) {
        super(assertionClass.getName(), Dependency.DependencyType.ASSERTION);
        this.assertionClass = assertionClass;
    }

}
