/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.policy;

import com.l7tech.objectmodel.GuidBasedEntityManager;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionTranslator;
import com.l7tech.policy.assertion.Include;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.policy.wsp.WspReader;
import com.l7tech.policy.wsp.WspWriter;
import com.l7tech.util.ExceptionUtils;
import org.jetbrains.annotations.Nullable;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * @author alex
 */
public class IncludeAssertionDereferenceTranslator implements AssertionTranslator {
    private final GuidBasedEntityManager<Policy> policyGetter;
    private final Set<String> policyGuids;
    private final boolean readOnly;
    private final boolean inlineDisabled;

    public IncludeAssertionDereferenceTranslator(final GuidBasedEntityManager<Policy> policyGetter) {
        this(policyGetter, new HashSet<String>(), true, true);
    }

    /**
     * Create a translator that will use the specified policy getter to look up included policies.
     *
     * @param policyGetter service for looking up target policies referenced by Include assertions.  Required.
     * @param includedPolicyGuids policy GUIDs already included on this thread, for detecting circular imports.  Required, but may be empty. 
     * @param readOnly if true, replacement assertions will be returned directly.
     *                 if false, replacement assertions will be copied to allow safe modification.
     * @param inlineDisabled if true, disabled Include assertions will be replaced as though they were enabled.
     *                       if false, disabled Include assertions will be ignored.
     */
    public IncludeAssertionDereferenceTranslator(final GuidBasedEntityManager<Policy> policyGetter,
                                                 final Set<String> includedPolicyGuids,
                                                 final boolean readOnly,
                                                 final boolean inlineDisabled )
    {
        if (includedPolicyGuids == null) throw new NullPointerException("includedPolicyGuids is required");
        if (policyGetter == null) throw new NullPointerException("policyGetter is required");
        this.policyGetter = policyGetter;
        this.policyGuids = includedPolicyGuids;
        this.readOnly = readOnly;
        this.inlineDisabled = inlineDisabled;
    }

    @Override
    public Assertion translate(@Nullable Assertion sourceAssertion) throws PolicyAssertionException {
        if (!(sourceAssertion instanceof Include)) return sourceAssertion;

        Include include = (Include) sourceAssertion;
        if ( !inlineDisabled && !include.isEnabled() ) {
            return sourceAssertion;            
        }

        Policy policy = include.retrieveFragmentPolicy();

        if(!policyGuids.add(include.getPolicyGuid())) {
            throw new PolicyAssertionException(include, "Circular policy include for Policy #" + include.getPolicyGuid());
        }

        if(policy == null) {
            try {
                policy = policyGetter.findByGuid(include.getPolicyGuid());
                if (policy == null) throw new PolicyAssertionException(include, MessageFormat.format("Include assertion refers to Policy #{0} ({1}), which no longer exists", include.getPolicyGuid(), include.getPolicyName()));
            } catch(Exception e) {
                throw new PolicyAssertionException(include, "Unable to load Included policy: " + ExceptionUtils.getMessage(e), e);
            }
        }

        try {
            if ( readOnly ) {
                return policy.getAssertion();
            } else {
                HashMap<String, Policy> policyFragments = new HashMap<String, Policy>();
                Assertion returnValue = WspReader.getDefault().parsePermissively(WspWriter.getPolicyXml(policy.getAssertion()), WspReader.INCLUDE_DISABLED);
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

    @Override
    public void translationFinished(@Nullable Assertion sourceAssertion) {
        if (!(sourceAssertion instanceof Include)) return;

        policyGuids.remove(((Include)sourceAssertion).getPolicyGuid());
    }

    /**
     * Recursively scans the Assertion tree looking for Include assertions. When an Include assertion is encountered
     * that contains a temporary policy object, that policy object is added to the provided HashMap.
     * @param rootAssertion The root of the Assertion tree to scan.  May be null.
     * @param policyFragments The HashMap to add the temporary policy objects to
     */
    private void extractPolicyFragments(@Nullable Assertion rootAssertion, HashMap<String, Policy> policyFragments) {
        if(rootAssertion instanceof CompositeAssertion) {
            CompositeAssertion compAssertion = (CompositeAssertion)rootAssertion;
            for(Iterator it = compAssertion.children();it.hasNext();) {
                Assertion child = (Assertion)it.next();
                extractPolicyFragments(child, policyFragments);
            }
        } else if(rootAssertion instanceof Include) {
            Include includeAssertion = (Include)rootAssertion;
            if(includeAssertion.retrieveFragmentPolicy() != null) {
                policyFragments.put(includeAssertion.getPolicyGuid(), includeAssertion.retrieveFragmentPolicy());
            }
        }
    }

    /**
     * Recursively scans the Assertion tree looking for Include assertions. When an Include assertion is encountered,
     * its temporary policy object is updated if the provided HashMap contained one for it.
     * @param rootAssertion The root of the Assertion tree.  May be null.
     * @param policyFragments The Map of temporary policy objects, keyed by policy name
     */
    private void setPolicyFragments(@Nullable Assertion rootAssertion, HashMap<String, Policy> policyFragments) {
        if(rootAssertion instanceof CompositeAssertion) {
            CompositeAssertion compAssertion = (CompositeAssertion)rootAssertion;
            for(Iterator it = compAssertion.children();it.hasNext();) {
                Assertion child = (Assertion)it.next();
                setPolicyFragments(child, policyFragments);
            }
        } else if(rootAssertion instanceof Include) {
            Include includeAssertion = (Include)rootAssertion;
            if(policyFragments.containsKey(includeAssertion.getPolicyGuid())) {
                includeAssertion.replaceFragmentPolicy(policyFragments.get(includeAssertion.getPolicyGuid()));
            }
        }
    }
}
