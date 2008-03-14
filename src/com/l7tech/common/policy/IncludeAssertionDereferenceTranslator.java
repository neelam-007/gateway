/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.common.policy;

import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.objectmodel.ReadOnlyEntityManager;
import com.l7tech.objectmodel.PolicyHeader;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionTranslator;
import com.l7tech.policy.assertion.Include;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.wsp.WspReader;
import com.l7tech.policy.wsp.WspWriter;

import java.text.MessageFormat;
import java.util.Set;
import java.util.HashSet;

/**
 * @author alex
 */
public class IncludeAssertionDereferenceTranslator implements AssertionTranslator {
    private final ReadOnlyEntityManager<Policy, PolicyHeader> policyGetter;
    private final Set<Long> policyOids;
    private final Set<String> policyNames;
    private final boolean readOnly;

    public IncludeAssertionDereferenceTranslator(final ReadOnlyEntityManager<Policy,PolicyHeader> policyGetter) {
        this(policyGetter, null, true);
    }

    public IncludeAssertionDereferenceTranslator(final ReadOnlyEntityManager<Policy,PolicyHeader> policyGetter,
                                                 final Set<Long> includedPolicyOids,
                                                 final boolean readOnly) {
        this.policyGetter = policyGetter;
        this.policyOids = includedPolicyOids;
        this.policyNames = new HashSet<String>();
        this.readOnly = readOnly;
    }

    public Assertion translate(Assertion sourceAssertion) throws PolicyAssertionException {
        if (!(sourceAssertion instanceof Include)) return sourceAssertion;

        Include include = (Include) sourceAssertion;
        Policy policy = include.retrieveFragmentPolicy();

        if(policy == null) {
            try {
                policy = policyGetter.findByPrimaryKey(include.getPolicyOid());
                if (policy == null) throw new PolicyAssertionException(include, MessageFormat.format("Include assertion refers to Policy #{0} ({1}), which no longer exists", include.getPolicyOid(), include.getPolicyName()));
            } catch(Exception e) {
                throw new PolicyAssertionException(include, "Unable to load Included policy: " + ExceptionUtils.getMessage(e), e);
            }
        }

        if(policy.getOid() > 0) {
            if ( policyOids != null ) {
                if (!policyOids.add( policy.getOid() ) ) {
                    throw new PolicyAssertionException(include, "Circular policy include for Policy #" + policy.getOid());
                }
            }
        } else {
            if( policyOids != null ) { // Check this since a null value would disable circular policy checking
                if (!policyNames.add(policy.getName())) {
                    throw new PolicyAssertionException(include, "Circular policy include for Policy " + policy.getName());
                }
            }
        }

        try {
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
