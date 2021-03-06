package com.l7tech.policy.exporter;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.common.custom.ClassNameToEntitySerializer;
import com.l7tech.gateway.common.entity.EntitiesResolver;
import com.l7tech.gateway.common.export.ExternalReferenceFactory;
import com.l7tech.identity.IdentityProviderType;
import com.l7tech.objectmodel.*;
import com.l7tech.policy.Policy;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.policy.assertion.ext.entity.CustomEntitySerializer;
import com.l7tech.policy.assertion.ext.security.SignerServices;
import com.l7tech.policy.wsp.WspConstants;
import com.l7tech.policy.wsp.WspWriter;
import com.l7tech.util.DomUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.EntityResolver;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Exports a Policy to an XML file that contains details of all external
 * references necessary to be able to re-import on another SSM.
 */
public class PolicyExporter {
    private final Logger logger = Logger.getLogger(PolicyExporter.class.getName());
    private final ExternalReferenceFinder finder;
    private final EntityResolver entityResolver;

    /**
     * Create a new policy exporter.
     *
     * @param finder The reference finder to use (required)
     * @param entityResolver The entity resolver to use if parsing XML that permits document type declarations (optional)
     */
    public PolicyExporter( final ExternalReferenceFinder finder,
                           final EntityResolver entityResolver ) {
        this.finder = finder;
        this.entityResolver = entityResolver;
    }

    public Document exportToDocument(@Nullable Assertion rootAssertion, Set<ExternalReferenceFactory> factories) throws IOException, SAXException {
        // do policy to xml
        final Document policyDoc = WspWriter.getPolicyDocument(rootAssertion);
        // go through each assertion and list external dependencies
        Collection<ExternalReference> refs = new ArrayList<ExternalReference>();
        traverseAssertionTreeForReferences(rootAssertion, refs, factories);
        // add external dependencies to document
        Element referencesEl = wrapExportReferencesToPolicyDocument(policyDoc);
        serializeReferences(referencesEl, refs.toArray(new ExternalReference[refs.size()]));
        return policyDoc;
    }

    private void serializeReferences(Element referencesEl, ExternalReference[] references) {
        for (ExternalReference reference : references) {
            reference.serializeToRefElement(referencesEl);
        }
    }

    public void exportToFile(Assertion rootAssertion, File outputFile, Set<ExternalReferenceFactory> factories) throws IOException, SAXException {
        Document doc = exportToDocument(rootAssertion, factories);
        // write doc to file
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(outputFile);
            XmlUtil.nodeToFormattedOutputStream(doc,fos);
            fos.flush();
        }
        finally {
            if (fos != null) try{ fos.close(); }catch(IOException ioe){ /* */ }
        }
    }

    /**
     * Recursively go through an assertion tree populating the references as necessary.
     */
    private void traverseAssertionTreeForReferences(@Nullable Assertion rootAssertion, Collection<ExternalReference> refs, Set<ExternalReferenceFactory> factories) {
        if (rootAssertion instanceof CompositeAssertion) {
            CompositeAssertion ca = (CompositeAssertion)rootAssertion;
            //noinspection unchecked
            List<Assertion> children = ca.getChildren();
            for (Assertion child : children) {
                traverseAssertionTreeForReferences(child, refs, factories);
            }
        } else {
            appendRelatedReferences(rootAssertion, refs, factories);
        }
    }

    /**
     * Adds ExternalReference instances to refs collection in relation to the assertion
     * if applicable
     */
    private void appendRelatedReferences( final @Nullable Assertion assertion,
                                          final Collection<ExternalReference> refs,
                                          final Set<ExternalReferenceFactory> factories) {

        // Solving Modular Assertion's External References
        if (factories != null && !factories.isEmpty()) {
            for (ExternalReferenceFactory<ExternalReference, ExternalReferenceFinder> factory: factories) {
                if (factory.matchByModularAssertion(assertion.getClass())) {
                    addReference(factory.createExternalReference(finder, assertion), refs);
                    break;
                }
            }
        }

        if ( assertion instanceof CustomAssertionHolder ) {
            CustomAssertionHolder cahAss = (CustomAssertionHolder)assertion;
            addReference( new CustomAssertionReference( finder, cahAss.getCustomAssertion().getName()), refs );
        }

        if ( assertion instanceof UsesResourceInfo ) {
            final UsesResourceInfo usesResourceInfo = (UsesResourceInfo) assertion;
            for ( final GlobalResourceReference globalResourceReference :
                    GlobalResourceReference.buildResourceEntryReferences( finder, entityResolver, usesResourceInfo ) ) {
                addReference( globalResourceReference, refs );
            }
        }

        if (assertion instanceof PolicyReference) {
            if(((PolicyReference)assertion).retrievePolicyGuid() != null) {
                final IncludedPolicyReference includedReference = new IncludedPolicyReference( finder, (PolicyReference) assertion);
                if( addReference( includedReference, refs ) ) {
                    Policy fragmentPolicy;
                    //bug 5316: if we are dealing with include assertions, we'll just get the policy fragment from the assertion.
                    if (assertion instanceof Include) {
                        fragmentPolicy = ((PolicyReference) assertion).retrieveFragmentPolicy();
                        //bug 5316: this is here to handle the scenario where if the policy was imported, added new policy
                        //fragment and export it. The new added policy fragment needs to be created because it does not
                        //exist in the assertion yet.
                        if (fragmentPolicy == null) {
                            fragmentPolicy = new Policy(includedReference.getType(), includedReference.getName(), includedReference.getXml(), includedReference.isSoap() != null ? includedReference.isSoap() : false);
                        }
                    } else {
                        fragmentPolicy = new Policy(includedReference.getType(), includedReference.getName(), includedReference.getXml(), includedReference.isSoap() != null ? includedReference.isSoap() : false);
                    }

                    try {
                        traverseAssertionTreeForReferences(fragmentPolicy.getAssertion(), refs, factories);
                    } catch(IOException e) {
                        // Ignore and continue with the export
                        logger.log(Level.WARNING, "Failed to create policy from include reference (policy OID = " + includedReference.getGuid() + ")");
                    }
                }
            }
        }

        if (assertion instanceof JdbcConnectionable) {
            final JdbcConnectionable connectionable = (JdbcConnectionable) assertion;
            addReference(new JdbcConnectionReference(finder, connectionable), refs);
        } else if (assertion instanceof CassandraConnectionable) {
            final CassandraConnectionable connectionable = (CassandraConnectionable) assertion;
            addReference(new CassandraConnectionReference(finder, connectionable), refs);
        }

        processAssertionEntityHeaders(refs, assertion);

        if ( assertion instanceof PrivateKeyable ) {
            final PrivateKeyable keyable = (PrivateKeyable)assertion;
            if ( !keyable.isUsesDefaultKeyStore() ) {
                addReference( new PrivateKeyReference( finder, keyable), refs);
            }
        }
    }

    /**
     * Process used entities from both modular and custom assertions.
     * Used during export only.
     */
    private void processAssertionEntityHeaders(
            final Collection<ExternalReference> refs,
            @NotNull final Assertion assertion
    ) {
        final EntitiesResolver entitiesResolver = EntitiesResolver
                .builder()
                .keyValueStore(finder.getCustomKeyValueStore())
                .classNameToSerializer(new ClassNameToEntitySerializer() {
                    @Override
                    public CustomEntitySerializer getSerializer(final String className) {
                        return finder.getCustomKeyValueEntitySerializer(className);
                    }
                })
                .build();
        for( final EntityHeader entityHeader : entitiesResolver.getEntitiesUsed(assertion) ) {
            if( EntityType.ID_PROVIDER_CONFIG.equals(entityHeader.getType()) ) {
                final IdProviderReference idProviderRef = new IdProviderReference( finder, entityHeader.getGoid());

                if( idProviderRef.getIdProviderTypeVal() == IdentityProviderType.FEDERATED.toVal() ) {
                    addReference( new FederatedIdProviderReference( finder, entityHeader.getGoid()), refs);
                } else {
                    addReference( idProviderRef, refs);
                }
            } else if( EntityType.JMS_ENDPOINT.equals(entityHeader.getType()) ) {
                addReference( new JMSEndpointReference( finder, entityHeader.getGoid()), refs);
            } else if( EntityType.TRUSTED_CERT.equals(entityHeader.getType()) ) {
                addReference( new TrustedCertReference( finder, entityHeader.getGoid()), refs);
            } else if( EntityType.ENCAPSULATED_ASSERTION.equals(entityHeader.getType()) ) {
                addReference( new EncapsulatedAssertionReference( finder, (GuidEntityHeader)entityHeader), refs);
            } else if( EntityType.SECURE_PASSWORD.equals(entityHeader.getType()) ) {
                addReference( new StoredPasswordReference( finder, (SecurePasswordEntityHeader)entityHeader), refs);
            } else if( EntityType.SSG_KEY_ENTRY.equals(entityHeader.getType()) ) {
                SsgKeyHeader ssgKeyHeader = (SsgKeyHeader) entityHeader;
                if (!(PersistentEntity.DEFAULT_GOID.equals(ssgKeyHeader.getKeystoreId()) && ssgKeyHeader.getAlias() == null) && !SignerServices.KEY_ID_SSL.equals(ssgKeyHeader.getStrId())) {
                    // Add none default keys only.
                    //
                    addReference(new PrivateKeyReference(finder, false, ssgKeyHeader.getKeystoreId(), ssgKeyHeader.getAlias()), refs);
                }
            } else if( EntityType.CUSTOM_KEY_VALUE_STORE.equals(entityHeader.getType()) ) {
                final CustomKeyStoreEntityHeader customKeyStoreEntityHeader = (CustomKeyStoreEntityHeader)entityHeader;
                if (customKeyStoreEntityHeader.hasBytes()) {
                    addReference( new CustomKeyValueReference(finder, customKeyStoreEntityHeader), refs);
                } else {
                    logger.warning("Ignoring empty custom-key-value-store header: " + entityHeader);
                }
            }
        }
    }

    private boolean addReference( final ExternalReference reference,
                                  final Collection<ExternalReference> references ) {
            boolean added = false;

        // Add reference only if not already present
        if ( !references.contains( reference ) ) {
            references.add( reference );
            added = true;
        }

        return added;
    }

    private Element wrapExportReferencesToPolicyDocument(Document originalPolicy) {
        Element exportRoot = originalPolicy.createElementNS(ExporterConstants.EXPORTED_POL_NS,
                                                            ExporterConstants.EXPORTED_DOCROOT_ELNAME);
        exportRoot.setAttributeNS(DomUtils.XMLNS_NS, "xmlns:" + ExporterConstants.EXPORTED_POL_PREFIX, ExporterConstants.EXPORTED_POL_NS);
        exportRoot.setAttributeNS(DomUtils.XMLNS_NS, "xmlns:L7p", WspConstants.L7_POLICY_NS);
        exportRoot.setAttributeNS(DomUtils.XMLNS_NS, "xmlns:wsp", WspConstants.WSP_POLICY_NS);

        exportRoot.setPrefix(ExporterConstants.EXPORTED_POL_PREFIX);
        exportRoot.setAttributeNS(null, ExporterConstants.VERSION_ATTRNAME, ExporterConstants.CURRENT_VERSION);
        Element referencesEl = originalPolicy.createElementNS(ExporterConstants.EXPORTED_POL_NS,
                                                              ExporterConstants.EXPORTED_REFERENCES_ELNAME);
        referencesEl.setPrefix(ExporterConstants.EXPORTED_POL_PREFIX);
        exportRoot.appendChild(referencesEl);
        Element previousRoot = originalPolicy.getDocumentElement();
        exportRoot.appendChild(previousRoot);
        originalPolicy.appendChild(exportRoot);
        return referencesEl;
    }

    @SuppressWarnings({"RedundantIfStatement"})
    public static boolean isExportedPolicy( final Document doc ) {
        Element documentElement = doc.getDocumentElement();
        if (documentElement == null || documentElement.getNamespaceURI() == null) return false;
        if (!documentElement.getNamespaceURI().equals(ExporterConstants.EXPORTED_POL_NS)) return false;
        if (!documentElement.getLocalName().equals(ExporterConstants.EXPORTED_DOCROOT_ELNAME)) return false;
        return true;
    }
}