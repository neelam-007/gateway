/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.external.assertions.idattr.policy;

import com.l7tech.external.assertions.idattr.IdentityAttributesAssertion;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.PolicyValidatorResult;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.identity.IdentityAssertion;
import com.l7tech.policy.validator.AssertionValidator;
import com.l7tech.policy.validator.PolicyValidationContext;

import java.util.HashSet;
import java.util.Set;

/**
 * Validates that any {@link IdentityAttributesAssertion} is preceded in its path by an {@link IdentityAssertion}.
 * @author alex
 */
public class IdentityAttributesAssertionValidator implements AssertionValidator {
    private final IdentityAttributesAssertion assertion;

    public IdentityAttributesAssertionValidator(IdentityAttributesAssertion assertion) {
        this.assertion = assertion;
    }

    @Override
    public void validate(AssertionPath path, PolicyValidationContext pvc, PolicyValidatorResult result) {
        int firstIdPos = -1;
        Set<Goid> authenticatedProviders = new HashSet<Goid>();
        for (int i = 0; i < path.getPath().length; i++) {
            Assertion assertion = path.getPath()[i];
            if (!assertion.isEnabled()) continue;
            if (assertion instanceof IdentityAssertion) {
                if (firstIdPos == -1) firstIdPos = i;
                authenticatedProviders.add(((IdentityAssertion)assertion).getIdentityProviderOid());
            } else if (assertion == this.assertion) {
                if (firstIdPos == -1 || firstIdPos > i || !authenticatedProviders.contains(this.assertion.getIdentityProviderOid())) {
                    result.addWarning(new PolicyValidatorResult.Warning(assertion, "Must be preceded by an Identity Assertion (e.g. Authenticate User or Group) matching the expected Identity Provider", null));
                }
                break;
            }
        }
    }
}
