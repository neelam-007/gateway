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
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.policy.wsp.WspReader;
import com.l7tech.policy.wsp.WspWriter;

import java.text.MessageFormat;
import java.util.Set;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Iterator;
import java.io.IOException;

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
                HashMap<String, Policy> policyFragments = new HashMap<String, Policy>();
                Assertion returnValue = WspReader.getDefault().parsePermissively(WspWriter.getPolicyXml(policy.getAssertion()));
                if(include.retrieveFragmentPolicy() != null) {
                    extractPolicyFragments(include.retrieveFragmentPolicy().getAssertion(), policyFragments);
                    setPolicyFragments(returnValue, policyFragments);
                }
                return returnValue;
            }
        } catch (Exception e) {
            throw new PolicyAssertionException(include, "Unable to load Included policy: " + ExceptionUtils.getMessage(e), e);
        }
    }

    /**
     * Recursively scans the Assertion tree looking for Include assertions. When an Include assertion is encountered
     * that contains a temporary policy object, that policy object is added to the provided HashMap.
     * @param rootAssertion The root of the Assertion tree to scan
     * @param policyFragments The HashMap to add the temporary policy objects to
     */
    private void extractPolicyFragments(Assertion rootAssertion, HashMap<String, Policy> policyFragments) {
        if(rootAssertion instanceof CompositeAssertion) {
            CompositeAssertion compAssertion = (CompositeAssertion)rootAssertion;
            for(Iterator it = compAssertion.children();it.hasNext();) {
                Assertion child = (Assertion)it.next();
                extractPolicyFragments(child, policyFragments);
            }
        } else if(rootAssertion instanceof Include) {
            Include includeAssertion = (Include)rootAssertion;
            if(includeAssertion.retrieveFragmentPolicy() != null) {
                policyFragments.put(includeAssertion.getPolicyName(), includeAssertion.retrieveFragmentPolicy());
            }
        }
    }

    /**
     * Recursively scans the Assertion tree looking for Include assertions. When an Include assertion is encountered,
     * its temporary policy object is updated if the provided HashMap contained one for it.
     * @param rootAssertion The root of the Assertion tree
     * @param policyFragments The Map of temporary policy objects, keyed by policy name
     */
    private void setPolicyFragments(Assertion rootAssertion, HashMap<String, Policy> policyFragments) {
        if(rootAssertion instanceof CompositeAssertion) {
            CompositeAssertion compAssertion = (CompositeAssertion)rootAssertion;
            for(Iterator it = compAssertion.children();it.hasNext();) {
                Assertion child = (Assertion)it.next();
                setPolicyFragments(child, policyFragments);
            }
        } else if(rootAssertion instanceof Include) {
            Include includeAssertion = (Include)rootAssertion;
            if(policyFragments.containsKey(includeAssertion.getPolicyName())) {
                includeAssertion.replaceFragmentPolicy(policyFragments.get(includeAssertion.getPolicyName()));
            }
        }
    }
}
