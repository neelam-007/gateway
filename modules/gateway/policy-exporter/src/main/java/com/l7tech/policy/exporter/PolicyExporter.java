/**
 * Copyright (C) 2004-2007 Layer 7 Technologies Inc.
 */
package com.l7tech.policy.exporter;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.identity.IdentityProviderType;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.policy.AssertionResourceInfo;
import com.l7tech.policy.Policy;
import com.l7tech.policy.StaticResourceInfo;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.policy.assertion.xml.SchemaValidation;
import com.l7tech.policy.wsp.WspConstants;
import com.l7tech.policy.wsp.WspWriter;
import com.l7tech.util.DomUtils;
import com.l7tech.util.ExceptionUtils;
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

    public Document exportToDocument(Assertion rootAssertion) throws IOException, SAXException {
        // do policy to xml
        Document policyDoc = XmlUtil.stringToDocument(WspWriter.getPolicyXml(rootAssertion));
        // go through each assertion and list external dependencies
        Collection<ExternalReference> refs = new ArrayList<ExternalReference>();
        traverseAssertionTreeForReferences(rootAssertion, refs);
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

    public void exportToFile(Assertion rootAssertion, File outputFile) throws IOException, SAXException {
        Document doc = exportToDocument(rootAssertion);
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
    private void traverseAssertionTreeForReferences(Assertion rootAssertion, Collection<ExternalReference> refs) {
        if (rootAssertion instanceof CompositeAssertion) {
            CompositeAssertion ca = (CompositeAssertion)rootAssertion;
            //noinspection unchecked
            List<Assertion> children = ca.getChildren();
            for (Assertion child : children) {
                traverseAssertionTreeForReferences(child, refs);
            }
        } else {
            appendRelatedReferences(rootAssertion, refs);
        }
    }

    /**
     * Adds ExternalReference instances to refs collection in relation to the assertion
     * if applicable
     */
    private void appendRelatedReferences( final Assertion assertion,
                                          final Collection<ExternalReference> refs ) {

        if ( assertion instanceof CustomAssertionHolder ) {
            CustomAssertionHolder cahAss = (CustomAssertionHolder)assertion;
            addReference( new CustomAssertionReference( finder, cahAss.getCustomAssertion().getName()), refs );
        } else if ( assertion instanceof SchemaValidation ) {
            Document schema = null;
            SchemaValidation sva = (SchemaValidation) assertion;
            AssertionResourceInfo schemaResource = sva.getResourceInfo();
            if (schemaResource instanceof StaticResourceInfo) {
                try {
                    schema = XmlUtil.parse(ExternalSchemaReference.asInputSource(((StaticResourceInfo) sva.getResourceInfo())), entityResolver);
                } catch (SAXException e) {
                    logger.log(Level.WARNING, "Error parsing schema: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
                    // fallthrough since it's possible this assertion is just badly configured in which case we wont care
                    // about external references
                } catch ( IOException e ) {
                    logger.log(Level.WARNING, "Error parsing schema: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
                    // fallthrough
                }
            } else if (schemaResource instanceof GlobalResourceInfo) {
                String globalSchemaName = ((GlobalResourceInfo) schemaResource).getId();
                addReference( new ExternalSchemaReference( finder, entityResolver, globalSchemaName, null), refs);
            }

            // process external schema imports, if any
            if (schema != null) {
                ArrayList<ExternalSchemaReference.ListedImport> listOfImports = ExternalSchemaReference.listImports(schema);
                for (ExternalSchemaReference.ListedImport unresolvedImport : listOfImports) {
                    addReference( new ExternalSchemaReference( finder, entityResolver, unresolvedImport.name, unresolvedImport.tns), refs );
                }
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
                            fragmentPolicy = new Policy(includedReference.getType(), includedReference.getName(), includedReference.getXml(), includedReference.isSoap());
                        }
                    } else {
                        fragmentPolicy = new Policy(includedReference.getType(), includedReference.getName(), includedReference.getXml(), includedReference.isSoap());
                    }

                    try {
                        traverseAssertionTreeForReferences(fragmentPolicy.getAssertion(), refs);
                    } catch(IOException e) {
                        // Ignore and continue with the export
                        logger.log(Level.WARNING, "Failed to create policy from include reference (policy OID = " + includedReference.getGuid() + ")");
                    }
                }
            }
        }

        if ( assertion instanceof JdbcConnectionable ) {
            final JdbcConnectionable connectionable = (JdbcConnectionable)assertion;
            addReference( new JdbcConnectionReference( finder, connectionable), refs );
        }

        if( assertion instanceof UsesEntities ) {
            final UsesEntities entitiesUser = (UsesEntities)assertion;
            for( final EntityHeader entityHeader : entitiesUser.getEntitiesUsed() ) {
                if( entityHeader.getType().equals(EntityType.ID_PROVIDER_CONFIG) ) {
                    final IdProviderReference idProviderRef = new IdProviderReference( finder, entityHeader.getOid());

                    if( idProviderRef.getIdProviderTypeVal() == IdentityProviderType.FEDERATED.toVal() ) {
                        addReference( new FederatedIdProviderReference( finder, entityHeader.getOid()), refs);
                    } else {
                        addReference( idProviderRef, refs);
                    }
                } else if( entityHeader.getType().equals(EntityType.JMS_ENDPOINT) ) {
                    addReference( new JMSEndpointReference( finder, entityHeader.getOid()), refs);
                } else if( entityHeader.getType().equals(EntityType.TRUSTED_CERT) ) {
                    addReference( new TrustedCertReference( finder, entityHeader.getOid()), refs);
                }
            }
        }

        if ( assertion instanceof PrivateKeyable ) {
            final PrivateKeyable keyable = (PrivateKeyable)assertion;
            if ( !keyable.isUsesDefaultKeyStore() ) {
                addReference( new PrivateKeyReference( finder, keyable), refs);
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
        exportRoot.setAttribute(ExporterConstants.VERSION_ATTRNAME, ExporterConstants.CURRENT_VERSION);
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
