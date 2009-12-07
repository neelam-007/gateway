package com.l7tech.console.policy.exporter;

import com.l7tech.gui.util.Utilities;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.console.panels.ResolveExternalPolicyReferencesWizard;
import com.l7tech.console.util.TopComponents;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.assertion.xml.SchemaValidation;
import com.l7tech.policy.assertion.identity.IdentityAssertion;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.policy.wsp.InvalidPolicyStreamException;
import com.l7tech.policy.wsp.WspReader;
import com.l7tech.policy.wsp.PolicyConflictException;
import com.l7tech.policy.Policy;
import com.l7tech.policy.StaticResourceInfo;
import com.l7tech.objectmodel.EntityHeader;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.swing.*;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.awt.*;

/**
 * This class takes a set of remote references that were exported with a policy
 * and find corresponding match with local entities. When the resolution cannot
 * be made automatically, it prompts the administrator for manual resolution.
 *
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Jul 22, 2004<br/>
 */
public class RemoteReferenceResolver {

    private WspReader wspReader = null;

    private WspReader getWspReader() {
        if (wspReader == null) {
            wspReader = (WspReader)TopComponents.getInstance().getApplicationContext().getBean("wspReader", WspReader.class);
        }
        return wspReader;
    }

    /**
     * Resolve remote references involving the administrator's input when necessary.
     * This method must be invoked before localizePolicy()
     *
     * @param references references parsed from a policy document.
     * @return false if the process cannot continue because the administrator canceled an operation for example.
     */
    public boolean resolveReferences(Collection<ExternalReference> references) throws InvalidPolicyStreamException, PolicyImportCancelledException {
        Set<ExternalReference> unresolved = new LinkedHashSet<ExternalReference>();

        // Verify policy fragment references first.  If a policy fragment was imported before, ask the user to substitute the fragment with an already import fragment.
        Collection<IncludedPolicyReference> topParentFragmtRefs = verifyPolicyFragmentReferences(references, unresolved);

        // Verify other non-fragment references
        references.removeAll(topParentFragmtRefs);
        for (ExternalReference reference : references) {
            if (!reference.verifyReference()) {
                // for all references not resolved automatically add a page in a wizard
                unresolved.add(reference);
            }
        }
        // Add back the remained policy fragment references
        references.addAll(topParentFragmtRefs);

        if (!unresolved.isEmpty()) {
            ExternalReference[] unresolvedRefsArray = unresolved.toArray(new ExternalReference[unresolved.size()]);
            final Frame mw = TopComponents.getInstance().getTopParent();
            boolean wasCancelled = false;
            try {
                ResolveExternalPolicyReferencesWizard wiz =
                        ResolveExternalPolicyReferencesWizard.fromReferences(mw, unresolvedRefsArray);
                wiz.pack();
                Utilities.centerOnScreen(wiz);
                wiz.setModal(true);
                wiz.setVisible(true);
                // if the wizard returns false, we must return
                if (wiz.wasCanceled()) wasCancelled = true;
            } catch(Exception e) {
                return false;
            }

            if(wasCancelled) {
                throw new PolicyImportCancelledException();
            }
        }
        resolvedReferences = references;
        return true;
    }

    public Assertion localizePolicy(Element policyXML) throws InvalidPolicyStreamException {
        // Go through each assertion and fix the changed references.
        Assertion root;
        try {
            root = getWspReader().parsePermissively( XmlUtil.nodeToString(policyXML), WspReader.INCLUDE_DISABLED);
        } catch (IOException e) {
            throw new InvalidPolicyStreamException(e);
        }
        traverseAssertionTreeForLocalization(root);
        return root;
    }

    public Assertion localizePolicy(Assertion rootAssertion) {
        traverseAssertionTreeForLocalization(rootAssertion);
        return rootAssertion;
    }

    private boolean traverseAssertionTreeForLocalization(Assertion rootAssertion) {
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
                // Prompt an optional dialog to resolve policy fragment conflicts..
                int result = JOptionPane.showOptionDialog(TopComponents.getInstance().getTopParent(),
                    "<html><center>The imported policy contains an embedded fragment with name '" + pce.getImportedPolicyName() + "' that already exists in the system.</center>" +
                        "<center>This policy importer will use the already existing policy fragment in place of the embedded fragment.</center></html>",
                    "Resolving Policy Fragment Conflict", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE, null, null, null);
                if (result == JOptionPane.OK_OPTION) {
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
                                childrenList.addAll(hierarchyFragmtRefsMap.get(ref));
                                hierarchyFragmtRefsMap.remove(ref);
                            }
                        }
                    }
                }
            } else {
                throw new IllegalStateException("Invalid policy fragment, " + fragmtRef.getName());
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
    private void removeRedundantReferences(Collection<ExternalReference> references, Assertion assertion) {
        if (assertion instanceof CompositeAssertion) {
            for (Assertion assn: ((CompositeAssertion)assertion).getChildren()) {
                removeRedundantReferences(references, assn);
            }
        } else {
            findAndRemoveReference(assertion, references);
        }
    }

    /**
     * Find and remove redundant references assoicated with an assertion, which 
     *
     * @param assertion: a non-composite assertion
     * @param references: the list of all references.
     */
    private void findAndRemoveReference(Assertion assertion, Collection<ExternalReference> references) {
        if (references == null || references.isEmpty()) return;

        if (assertion instanceof IdentityAssertion) {
            for (Iterator<ExternalReference> itr = references.iterator(); itr.hasNext(); ) {
                ExternalReference reference = itr.next();
                if (reference instanceof IdProviderReference) {
                    IdentityAssertion idAssn = (IdentityAssertion) assertion;
                    IdProviderReference ipRef = (IdProviderReference) reference;
                    if (ipRef.getProviderId() == idAssn.getIdentityProviderOid()) {
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
        } else if (assertion instanceof SchemaValidation) {
            for (Iterator<ExternalReference> itr = references.iterator(); itr.hasNext(); ) {
                ExternalReference reference = itr.next();
                if (reference instanceof ExternalSchemaReference) {
                    SchemaValidation sva = (SchemaValidation)assertion;
                    if (sva.getResourceInfo() instanceof StaticResourceInfo) {
                        ExternalSchemaReference schemaRef = (ExternalSchemaReference) reference;
                        StaticResourceInfo sri = (StaticResourceInfo)sva.getResourceInfo();
                        boolean found = false;
                        try {
                            List<ExternalSchemaReference.ListedImport> listOfImports = ExternalSchemaReference.listImports(XmlUtil.stringToDocument(sri.getDocument()));
                            for (ExternalSchemaReference.ListedImport unresolvedImport : listOfImports) {
                                if (schemaRef.getName() != null && schemaRef.getName().equals(unresolvedImport.name) &&
                                    schemaRef.getTns() != null && schemaRef.getTns().equals(unresolvedImport.tns)) {
                                    found = true;
                                    itr.remove();
                                    break;
                                }
                            }
                        } catch (SAXException e) {
                            // do nothing
                        }
                        if (found) break;
                    }
                }
            }
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
            UsesEntities entitiesUser = (UsesEntities) assertion;
            for (Iterator<ExternalReference> itr = references.iterator(); itr.hasNext(); ) {
                ExternalReference reference = itr.next();
                if (reference instanceof IdProviderReference) {
                    IdProviderReference ipRef = (IdProviderReference) reference;
                    for (EntityHeader entityHeader : entitiesUser.getEntitiesUsed()) {
                        if (ipRef.getProviderId() == entityHeader.getOid()) {
                            itr.remove();
                            break;
                        }
                    }
                } else if (reference instanceof JMSEndpointReference) {
                    JMSEndpointReference endpointRef = (JMSEndpointReference) reference;
                    for (EntityHeader entityHeader : entitiesUser.getEntitiesUsed()) {
                        if (endpointRef.getOid() == entityHeader.getOid()) {
                            itr.remove();
                            break;
                        }
                    }
                } else if (reference instanceof TrustedCertReference) {
                    TrustedCertReference trustedCertRef = (TrustedCertReference) reference;
                    for (EntityHeader entityHeader : entitiesUser.getEntitiesUsed()) {
                        if (trustedCertRef.getOid() == entityHeader.getOid()) {
                            itr.remove();
                            break;
                        }
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

    private Collection<ExternalReference> resolvedReferences = new ArrayList<ExternalReference>();
}
