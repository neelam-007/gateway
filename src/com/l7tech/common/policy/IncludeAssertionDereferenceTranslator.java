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
import com.l7tech.policy.wsp.WspReader;
import com.l7tech.policy.wsp.WspWriter;

import java.text.MessageFormat;

/**
 * @author alex
 */
public class IncludeAssertionDereferenceTranslator implements AssertionTranslator {
    private final ReadOnlyEntityManager<Policy, EntityHeader> policyGetter;
    private final boolean readOnly;

    public IncludeAssertionDereferenceTranslator(final ReadOnlyEntityManager<Policy,EntityHeader> policyGetter) {
        this(policyGetter, true);
    }

    public IncludeAssertionDereferenceTranslator(final ReadOnlyEntityManager<Policy,EntityHeader> policyGetter,
                                                 final boolean readOnly) {
        this.policyGetter = policyGetter;
        this.readOnly = readOnly;
    }

    public Assertion translate(Assertion sourceAssertion) throws PolicyAssertionException {
        if (!(sourceAssertion instanceof Include)) return sourceAssertion;

        Include include = (Include) sourceAssertion;
        try {
            Policy policy = policyGetter.findByPrimaryKey(include.getPolicyOid());
            if (policy == null) throw new PolicyAssertionException(include, MessageFormat.format("Include assertion refers to Policy #{0} ({1}), which no longer exists", include.getPolicyOid(), include.getPolicyName()));

            if ( readOnly ) {
                return policy.getAssertion();
            } else {
                return WspReader.getDefault().parsePermissively(WspWriter.getPolicyXml(policy.getAssertion()));
            }
        } catch (Exception e) {
            throw new PolicyAssertionException(include, "Unable to load Included policy: " + ExceptionUtils.getMessage(e), e);
        }
    }
}
