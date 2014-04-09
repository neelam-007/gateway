package com.l7tech.policy.exporter;

import com.l7tech.gateway.common.entity.EntitiesResolver;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.policy.Policy;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.policy.assertion.identity.IdentityAssertion;
import com.l7tech.policy.wsp.InvalidPolicyStreamException;
import com.l7tech.policy.wsp.PolicyConflictException;
import com.l7tech.policy.wsp.WspReader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Element;
import org.xml.sax.EntityResolver;

import java.io.IOException;
import java.util.*;

/**
 * This class takes a set of external references that were exported with a policy
 * and find corresponding match with local entities. When the resolution cannot
 * be made automatically, it prompts the administrator for manual resolution.
 *
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Jul 22, 2004<br/>
 */
class ExternalReferenceResolver {

    //- PACKAGE

    ExternalReferenceResolver( final WspReader wspReader,
                               final ExternalReferenceFinder finder,
                               final PolicyImporter.PolicyImporterAdvisor advisor,
                               final EntityResolver entityResolver ) {
        this.wspReader = wspReader;
        this.finder = finder;
        this.advisor = advisor;
        this.entityResolver = entityResolver;
    }

    /**
     * Resolve remote references involving the administrator's input when necessary.
     * This method must be invoked before localizePolicy()
     *
     * @param references references parsed from a policy document.
     * @return false if the process cannot continue because the administrator canceled an operation for example.
     */
    boolean resolveReferences(Collection<ExternalReference> references) throws InvalidPolicyStreamException, PolicyImportCancelledException {
        Set<ExternalReference> unresolved = new LinkedHashSet<ExternalReference>();

        // Verify policy fragment references first.  If a policy fragment was imported before, ask the user to substitute the fragment with an already import fragment.
        Collection<IncludedPolicyReference> topParentFragmtRefs = verifyPolicyFragmentReferences(references, unresolved);

        // Verify other non-fragment references
        references.removeAll(topParentFragmtRefs);
        for (ExternalReference reference : references) {
            reference.setPolicyImporterAdvisor( advisor );
            if (!reference.verifyReference()) {
                // for all references not resolved automatically add a page in a wizard
                unresolved.add(reference);
            }
        }
        // Add back the remained policy fragment references
        references.addAll(topParentFragmtRefs);

        if (!unresolved.isEmpty()) {
            ExternalReference[] unresolvedRefsArray = unresolved.toArray(new ExternalReference[unresolved.size()]);
            if ( !advisor.resolveReferences( unresolvedRefsArray ) ) {
                return false;
            }
        }
        resolvedReferences = references;
        return true;
    }

    Assertion localizePolicy(Element policyXML) throws InvalidPolicyStreamException {
        // Go through each assertion and fix the changed references.
        Assertion root;
        try {
            root = wspReader.parsePermissively( policyXML, WspReader.INCLUDE_DISABLED );
        } catch (IOException e) {
            throw new InvalidPolicyStreamException(e);
        }
        traverseAssertionTreeForLocalization(root);
        return root;
    }

    Assertion localizePolicy(Assertion rootAssertion) {
        traverseAssertionTreeForLocalization(rootAssertion);
        return rootAssertion;
    }

    //- PRIVATE

    private final WspReader wspReader;
    private final ExternalReferenceFinder finder;
    private final PolicyImporter.PolicyImporterAdvisor advisor;
    private final EntityResolver entityResolver;
    private Collection<ExternalReference> resolvedReferences = new ArrayList<ExternalReference>();

    private boolean traverseAssertionTreeForLocalization(@Nullable Assertion rootAssertion) {
        if (rootAssertion instanceof CompositeAssertion) {
            CompositeAssertion ca = (CompositeAssertion)rootAssertion;
            List children = ca.getChildren();
            Collection<Assertion> childrenToRemoveFromCA = new ArrayList<Assertion>();
            for (Object aChildren : children) {
                Assertion child = (Assertion) aChildren;
                if (!traverseAssertionTreeForLocalization(child)) {
                    childrenToRemoveFromCA.add(child);
                }
            }
            // remove the children that are no longer wanted
            for (Assertion aChildrenToRemoveFromCA : childrenToRemoveFromCA) {
                ca.removeChild(aChildrenToRemoveFromCA);
            }
            return true;
        } else {
            if (resolvedReferences.isEmpty())
                return true;
            boolean ret = true;
            for (ExternalReference resolvedReference : resolvedReferences) {
                if (!resolvedReference.localizeAssertion(rootAssertion)) {
                    ret = false;
                    break;
                }
            }
            return ret;
        }
    }

    /**
     * Verify policy fragment references.  If an imported policy fragment was imported before, ask the user to substitute
     * the imported fragment with an already import fragment.  If the user answers with OK, then remove all redundant related
     * references created when the imported fragment was exported.
     *
     * @param references: the list containing all references.
     * @param unresolved: the list of references that cannot be resolved.
     * @return a list of top parent fragment references.
     *
     * @throws InvalidPolicyStreamException: thrown when errors verifying references.
     */
    private Collection<IncludedPolicyReference> verifyPolicyFragmentReferences(Collection<ExternalReference> references, Collection<ExternalReference> unresolved) throws InvalidPolicyStreamException {
        // Create a hirarchy frag-ref map just used to report accurate message when a policy fragment conflict occurs.
        Map<IncludedPolicyReference, Collection<IncludedPolicyReference>> hierarchyFragmtRefMap = getFragmentRefsInHierarchy(references);

        for (IncludedPolicyReference fragmtRef : hierarchyFragmtRefMap.keySet()) {
            try {
                // Check the top parent fragment reference first
                if (!fragmtRef.verifyReference()) {
                    unresolved.add(fragmtRef);
                }

                // If the parent fragment reference is verified, then verify all children fragment references
                Collection<IncludedPolicyReference> childrenFragmtRefs = hierarchyFragmtRefMap.get(fragmtRef);
                for (IncludedPolicyReference childFragmtRef: childrenFragmtRefs) {
                    if (!childFragmtRef.verifyReference()) {
                        unresolved.add(childFragmtRef);
                    }
                }
            } catch (PolicyConflictException pce) {
                final String policyName = pce.getImportedPolicyName();
                final String existingPolicyName = pce.getExistingPolicyName();
                final String guid = pce.getPolicyGuid();
                if ( advisor.acceptPolicyConflict( policyName, existingPolicyName, guid ) ) {
                    fragmtRef.setUseType( IncludedPolicyReference.UseType.USE_EXISTING );
                    
                    // Use the top parent policy fragment reference to remove all redundant references.
                    Policy policy = new Policy(fragmtRef.getType(), fragmtRef.getName(), fragmtRef.getXml(), fragmtRef.isSoap());
                    // Remove all redundant references if a policy fragment will be substituted by an existing policy fragment.
                    try {
                        removeRedundantReferences(references, policy.getAssertion());
                    } catch (IOException ioe) {
                        // do nothing
                    }
                } else {
                    throw pce;
                }
            }
        }

        return hierarchyFragmtRefMap.keySet();
    }

    /**
     * Create a map storing policy fragments by a hierarchy relationship, where keys are top-level parent policy fragments
     * and values are lists of children policy fragments.  So when verifying policy fragments, always process the parent policy
     * fragments first then children fragments.
     *
     * @param references: the list containing all references including policy fragment references and non-policy-fragment references.
     * @return a map, where key is a policy fragment reference and value is a list of children policy fragment references.  
     */
    private Map<IncludedPolicyReference, Collection<IncludedPolicyReference>> getFragmentRefsInHierarchy(Collection<ExternalReference> references) {
        Map<IncludedPolicyReference, Collection<IncludedPolicyReference>> hierarchyFragmtRefsMap = new Hashtable<IncludedPolicyReference, Collection<IncludedPolicyReference>>();

        // Find all policy fragment references
        Collection<IncludedPolicyReference> fragmtRefs = new ArrayList<IncludedPolicyReference>();
        for (ExternalReference reference : references) {
            if (reference instanceof IncludedPolicyReference) {
                fragmtRefs.add((IncludedPolicyReference)reference);
            }
        }
        // Create a fragment references map, whose elements are defined as (key: a frag ref, value: boolean indicating if accessed already)
        Map<IncludedPolicyReference, Boolean> fragmtMap = new Hashtable<IncludedPolicyReference, Boolean>();
        for (IncludedPolicyReference fragmtRef: fragmtRefs) {
            fragmtMap.put(fragmtRef, false);
        }
        // Find all toppest fragment references and store them into the map
        for (IncludedPolicyReference fragmtRef: fragmtRefs) {
            Collection<IncludedPolicyReference> childrenList = new ArrayList<IncludedPolicyReference>();
            Policy policy = new Policy(fragmtRef.getType(), fragmtRef.getName(), fragmtRef.getXml(), fragmtRef.isSoap());
            Assertion root;
            try {
                root = policy.getAssertion();
            } catch (IOException e) {
                fragmtMap.put(fragmtRef, true);
                continue;  // do nothing
            }

            if (root instanceof CompositeAssertion) {
                for (Assertion assn: ((CompositeAssertion)root).getChildren()) {
                    if (assn instanceof Include) {
                        Include include = (Include) assn;
                        for (IncludedPolicyReference ref: fragmtRefs) {
                            if (fragmtMap.get(ref)) continue;

                            if (ref.getGuid().equals(include.getPolicyGuid())) {
                                childrenList.add(ref);
                                fragmtMap.put(ref, true);
                            }
                        }
                        for (IncludedPolicyReference ref: fragmtRefs) {
                            if (ref.getGuid().equals(include.getPolicyGuid())) {
                                childrenList.add(ref);
                                if(hierarchyFragmtRefsMap.containsKey(ref)) {
                                    childrenList.addAll(hierarchyFragmtRefsMap.get(ref));
                                    hierarchyFragmtRefsMap.remove(ref);
                                }
                            }
                        }
                    }
                }
            }

            hierarchyFragmtRefsMap.put(fragmtRef, childrenList);
            fragmtMap.put(fragmtRef, true);
        }

        return hierarchyFragmtRefsMap;
    }

    /**
     * Remove all redundant references associated with an assertion, which could be a composite assertion or a leaf assertion.
     *
     * @param references: the list of all references
     * @param assertion: If the assertion is a composite assertion, then all references associated with the composite assertion's children will be removed.
     *                   If it is a non-composite assertion, probably one reference associated with the non-compoiste assertion will be removed.
     */
    private void removeRedundantReferences(Collection<ExternalReference> references, @Nullable Assertion assertion) {
        if (assertion instanceof CompositeAssertion) {
            for (Assertion assn: ((CompositeAssertion)assertion).getChildren()) {
                removeRedundantReferences(references, assn);
            }
        } else {
            findAndRemoveReference(assertion, references);
        }
    }

    /**
     * Find and remove redundant references associated with an assertion.
     *
     * @param assertion: a non-composite assertion.  If null, this method does nothing.
     * @param references: the list of all references.
     */
    private void findAndRemoveReference(@Nullable Assertion assertion, Collection<ExternalReference> references) {
        if (references == null || references.isEmpty()) return;

        if (assertion instanceof IdentityAssertion) {
            for (Iterator<ExternalReference> itr = references.iterator(); itr.hasNext(); ) {
                ExternalReference reference = itr.next();
                if (reference instanceof IdProviderReference) {
                    IdentityAssertion idAssn = (IdentityAssertion) assertion;
                    IdProviderReference ipRef = (IdProviderReference) reference;
                    if (ipRef.getProviderId()!=null && ipRef.getProviderId().equals(idAssn.getIdentityProviderOid())) {
                        itr.remove();
                        break;
                    }
                }
            }
        } else if (assertion instanceof CustomAssertionHolder) {
            for (Iterator<ExternalReference> itr = references.iterator(); itr.hasNext(); ) {
                ExternalReference reference = itr.next();
                if (reference instanceof CustomAssertionReference) {
                    CustomAssertionHolder customAssn = (CustomAssertionHolder) assertion;
                    CustomAssertionReference customRef = (CustomAssertionReference) reference;
                    if (customRef.getCustomAssertionName() != null && customRef.getCustomAssertionName().equals(customAssn.getCustomAssertion().getName())) {
                        itr.remove();
                        break;
                    }
                }
            }
            processFindAndRemoveEntityReferenceForAssertion(references, assertion);
        } else if (assertion instanceof PolicyReference) {
            if (((PolicyReference)assertion).retrievePolicyGuid() == null) return;

            for (Iterator<ExternalReference> itr = references.iterator(); itr.hasNext(); ) {
                ExternalReference reference = itr.next();
                if (reference instanceof IncludedPolicyReference) {
                    PolicyReference policyRefAssn = (PolicyReference) assertion;
                    IncludedPolicyReference includedPolicyRef = (IncludedPolicyReference) reference;
                    if (includedPolicyRef.getGuid() != null && includedPolicyRef.getGuid().equals(policyRefAssn.retrievePolicyGuid())) {
                        itr.remove();

                        // Process assertions included in the fragment
                        Policy fragmentPolicy;
                        if (assertion instanceof Include) {
                            fragmentPolicy = ((Include)assertion).retrieveFragmentPolicy();
                            if (fragmentPolicy == null) {
                                fragmentPolicy = new Policy(includedPolicyRef.getType(), includedPolicyRef.getName(), includedPolicyRef.getXml(), includedPolicyRef.isSoap());
                            }
                        } else {
                            fragmentPolicy = new Policy(includedPolicyRef.getType(), includedPolicyRef.getName(), includedPolicyRef.getXml(), includedPolicyRef.isSoap());
                        }
                        // Remove all sub-references in the policy fragment
                        try {
                            removeRedundantReferences(references, fragmentPolicy.getAssertion());
                        } catch (IOException ioe) {
                            // do nothing
                        }

                        break;
                    }
                }
            }
        } else if (assertion instanceof JdbcConnectionable) {
            for (Iterator<ExternalReference> itr = references.iterator(); itr.hasNext(); ) {
                ExternalReference reference = itr.next();
                if (reference instanceof JdbcConnectionReference) {
                    JdbcConnectionable jdbcConnectionable = (JdbcConnectionable) assertion;
                    JdbcConnectionReference jdbcConnRef = (JdbcConnectionReference) reference;
                    if (jdbcConnRef.getConnectionName() != null && jdbcConnRef.getConnectionName().equals(jdbcConnectionable.getConnectionName())) {
                        itr.remove();
                        break;
                    }
                }
            }
        } else if (assertion instanceof UsesEntities) {
            processFindAndRemoveEntityReferenceForAssertion(references, assertion);
        }

        if ( assertion instanceof UsesResourceInfo ) {
            final UsesResourceInfo usesResourceInfo = (UsesResourceInfo) assertion;
            Collection<GlobalResourceReference> assertionReferences = null;

            for ( final Iterator<ExternalReference> itr = references.iterator(); itr.hasNext(); ) {
                final ExternalReference reference = itr.next();

                if ( reference instanceof GlobalResourceReference ) {
                    if ( assertionReferences == null ) {
                        assertionReferences = GlobalResourceReference.buildResourceEntryReferences( finder, entityResolver, usesResourceInfo );
                    }

                    if ( assertionReferences.contains( reference )  ) {
                        itr.remove();
                        break;
                    }
                }
            }
        }

        if (assertion instanceof PrivateKeyable) {
            for (Iterator<ExternalReference> itr = references.iterator(); itr.hasNext(); ) {
                ExternalReference reference = itr.next();
                if (reference instanceof PrivateKeyReference) {
                    PrivateKeyable keyable = (PrivateKeyable) assertion;
                    PrivateKeyReference pkr = (PrivateKeyReference) reference;
                    if (pkr.getKeyAlias() != null && pkr.getKeyAlias().equals(keyable.getKeyAlias())) {
                        itr.remove();
                        break;
                    }
                }
            }
        }
    }

    /**
     * Custom assertions also use entities, currently only secure-passwords and key-value-store, however if we ever
     * decide to support IdProviderReference, JMSEndpointReference or TrustedCertReference entities, this will work seamlessly.
     */
    private void processFindAndRemoveEntityReferenceForAssertion(
            @NotNull final Collection<ExternalReference> references,
            @NotNull final Assertion assertion
    ) {
        final EntitiesResolver entitiesResolver = EntitiesResolver
                .builder()
                .keyValueStore(finder.getCustomKeyValueStore())
                .build();
        for (Iterator<ExternalReference> itr = references.iterator(); itr.hasNext(); ) {
            ExternalReference reference = itr.next();
            if (reference instanceof IdProviderReference) {
                IdProviderReference ipRef = (IdProviderReference) reference;
                for (EntityHeader entityHeader : entitiesResolver.getEntitiesUsed(assertion)) {
                    if (ipRef.getProviderId()!=null && ipRef.getProviderId().equals(entityHeader.getGoid())) {
                        itr.remove();
                        break;
                    }
                }
            } else if (reference instanceof JMSEndpointReference) {
                JMSEndpointReference endpointRef = (JMSEndpointReference) reference;
                for (EntityHeader entityHeader : entitiesResolver.getEntitiesUsed(assertion)) {
                    if (endpointRef.getGoid().equals(entityHeader.getGoid())) {
                        itr.remove();
                        break;
                    }
                }
            } else if (reference instanceof TrustedCertReference) {
                TrustedCertReference trustedCertRef = (TrustedCertReference) reference;
                for (EntityHeader entityHeader : entitiesResolver.getEntitiesUsed(assertion)) {
                    if ( entityHeader.equalsId(trustedCertRef.getGoid()) ) {
                        itr.remove();
                        break;
                    }
                }
            }
        }
    }
}
