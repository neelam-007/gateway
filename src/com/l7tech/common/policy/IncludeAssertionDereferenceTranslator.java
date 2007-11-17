/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.common.policy;

import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.ReadOnlyEntityManager;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionTranslator;
import com.l7tech.policy.assertion.Include;
import com.l7tech.policy.assertion.PolicyAssertionException;

import java.text.MessageFormat;

/**
 * @author alex
 */
public class IncludeAssertionDereferenceTranslator implements AssertionTranslator {
    private final ReadOnlyEntityManager<Policy, EntityHeader> policyGetter;

    public IncludeAssertionDereferenceTranslator(ReadOnlyEntityManager<Policy,EntityHeader> policyGetter) {
        this.policyGetter = policyGetter;
    }

    public Assertion translate(Assertion sourceAssertion) throws PolicyAssertionException {
        if (!(sourceAssertion instanceof Include)) return sourceAssertion;

        Include include = (Include) sourceAssertion;
        try {
            Policy policy = policyGetter.findByPrimaryKey(include.getPolicyOid());
            if (policy == null) throw new PolicyAssertionException(include, MessageFormat.format("Include assertion refers to Policy #{0} ({1}), which no longer exists", include.getPolicyOid(), include.getPolicyName()));

            return policy.getAssertion(); // TODO deep clone here via WSP round trip?
        } catch (Exception e) {
            throw new PolicyAssertionException(include, "Unable to load Included policy: " + ExceptionUtils.getMessage(e), e);
        }
    }
}
