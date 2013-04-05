package com.l7tech.policy;

import com.l7tech.objectmodel.NamedEntity;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.util.Functions;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

/**
 * Virtual entity that represents the capability to use a given policy assertion within a policy.
 */
public class AssertionAccess implements NamedEntity, Serializable {
    private final String assertionClass;

    public AssertionAccess(String assertionClass) {
        this.assertionClass = assertionClass;
    }

    public static AssertionAccess forAssertion(@NotNull Assertion assertion) {
        return new AssertionAccess(assertion.getClass().getName());
    }

    @Override
    public String getName() {
        return assertionClass;
    }

    @Override
    public String getId() {
        return assertionClass;
    }


    /**
     * @return a builder that will map an Assertion instance to a corresponding new AssertionAccess instance.
     */
    public static Functions.Unary<AssertionAccess, Assertion> builderFromAssertion() {
        return new Functions.Unary<AssertionAccess, Assertion>() {
            @Override
            public AssertionAccess call(Assertion assertion) {
                return forAssertion(assertion);
            }
        };
    }
}
