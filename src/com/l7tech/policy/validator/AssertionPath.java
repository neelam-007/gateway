package com.l7tech.policy.validator;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.PolicyValidatorResult;

import java.util.List;
import java.util.Collections;
import java.util.ArrayList;

/**
 * Class AssertionPath represents a sequence of <code>Assertion</code>
 * instances.
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class AssertionPath {
    /**
     * Cnstruct the new assertion path with the give n<code>Asserion</code>
     * representing root assertion.
     *
     * @param a the root assertion
     */
    public AssertionPath(Assertion a) {
        if (!DefaultPolicyValidator.isValidRoot(a)) {
            addError(new PolicyValidatorResult.Error(a, "Invalid root", null));
        }
        path.add(a);
    }

    /** copy constructor */
    public AssertionPath(AssertionPath ap) {
        if (ap.path.size() == 0) {
            throw new IllegalArgumentException();
        }
        path.addAll(ap.path);
        pathErrors.addAll(ap.pathErrors);
    }

    public void addAssertion(Assertion a) {
        path.add(a);
    }

    public void addError(PolicyValidatorResult.Error err) {
        pathErrors.add(err);
    }

    public Assertion lastAssertion() {
        return (Assertion)path.get(path.size() - 1);
    }

    public List assertions() {
        return Collections.unmodifiableList(path);
    }

    private List path = new ArrayList();
    private List pathErrors = new ArrayList();

}
