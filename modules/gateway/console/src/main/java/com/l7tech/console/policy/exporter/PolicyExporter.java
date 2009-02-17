/**
 * Copyright (C) 2004-2007 Layer 7 Technologies Inc.
 */
package com.l7tech.console.policy.exporter;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.identity.IdentityProviderType;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.policy.Policy;
import com.l7tech.policy.StaticResourceInfo;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.policy.assertion.identity.IdentityAssertion;
import com.l7tech.policy.assertion.xml.SchemaValidation;
import com.l7tech.policy.wsp.WspConstants;
import com.l7tech.policy.wsp.WspWriter;
import com.l7tech.util.DomUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
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

    public Document exportToDocument(Assertion rootAssertion) throws IOException, SAXException {
        // do policy to xml
        Document policydoc = XmlUtil.stringToDocument(WspWriter.getPolicyXml(rootAssertion));
        // go through each assertion and list external dependencies
        Collection<ExternalReference> refs = new ArrayList<ExternalReference>();
        traverseAssertionTreeForReferences(rootAssertion, refs);
        // add external dependencies to document
        Element referencesEl = wrapExportReferencesToPolicyDocument(policydoc);
        serializeReferences(referencesEl, refs.toArray(new ExternalReference[0]));
        return policydoc;
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
     * Adds ExternalReference instances to refs collestion in relation to the assertion
     * if applicable
     */
    private void appendRelatedReferences(Assertion assertion, Collection<ExternalReference> refs) {
        ExternalReference ref = null;
        // create the appropriate reference if applicable
        if (assertion instanceof IdentityAssertion) {
            IdentityAssertion idassertion = (IdentityAssertion)assertion;
            IdProviderReference idProviderRef = new IdProviderReference(idassertion.getIdentityProviderOid());

            if(idProviderRef.getIdProviderTypeVal() == IdentityProviderType.FEDERATED.toVal()) {
                ref = new FederatedIdProviderReference(idassertion.getIdentityProviderOid());
            } else {
                ref = idProviderRef;
            }
        } else if (assertion instanceof JmsRoutingAssertion) {
            JmsRoutingAssertion jmsidass = (JmsRoutingAssertion)assertion;
            if(jmsidass.getEndpointOid() != null) {
                ref = new JMSEndpointReference(jmsidass.getEndpointOid().longValue());
            }
        } else if (assertion instanceof CustomAssertionHolder) {
            CustomAssertionHolder cahAss = (CustomAssertionHolder)assertion;
            ref = new CustomAssertionReference(cahAss.getCustomAssertion().getName());
        } else if (assertion instanceof SchemaValidation) {
            SchemaValidation sva = (SchemaValidation)assertion;
            if (sva.getResourceInfo() instanceof StaticResourceInfo) {
                StaticResourceInfo sri = (StaticResourceInfo)sva.getResourceInfo();
                try {
                    ArrayList<ExternalSchemaReference.ListedImport> listOfImports = ExternalSchemaReference.listImports(XmlUtil.stringToDocument(sri.getDocument()));
                    for (ExternalSchemaReference.ListedImport unresolvedImport : listOfImports) {
                        ExternalSchemaReference esref = new ExternalSchemaReference(unresolvedImport.name, unresolvedImport.tns);
                        if (!refs.contains(esref)) {
                            refs.add(esref);
                        }
                    }
                } catch (SAXException e) {
                    logger.log(Level.WARNING, "cannot read schema doc properly");
                    // fallthrough since it's possible this assertion is just badly configured in which care we wont care
                    // about external references
                }
            }
        } else if (assertion instanceof PolicyReference) {
            if(((PolicyReference)assertion).retrievePolicyGuid() == null) {
                return;
            }
            
            ref = new IncludedPolicyReference((PolicyReference) assertion);

            if(!refs.contains(ref)) {
                IncludedPolicyReference includedReference = (IncludedPolicyReference)ref;
                Policy fragmentPolicy = null;
                //bug 5316: if we are dealing with include assertions, we'll just get the policy fragment from the assertion.
                if (assertion instanceof Include) {
                    fragmentPolicy = ((Include)assertion).retrieveFragmentPolicy();
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
        } else if(assertion instanceof UsesEntities) {
            UsesEntities entitiesUser = (UsesEntities)assertion;
            for(EntityHeader entityHeader : entitiesUser.getEntitiesUsed()) {
                ref = null;
                if(entityHeader.getType().equals(EntityType.ID_PROVIDER_CONFIG)) {
                    ref = new IdProviderReference(entityHeader.getOid());
                } else if(entityHeader.getType().equals(EntityType.JMS_ENDPOINT)) {
                    ref = new JMSEndpointReference(entityHeader.getOid());
                } else if(entityHeader.getType().equals(EntityType.TRUSTED_CERT)) {
                    ref = new TrustedCertReference(entityHeader.getOid());
                }

                if(ref != null && !refs.contains(ref)) {
                    refs.add(ref);
                }
            }

            return;
        }

        // if an assertion was created and it's not already recorded, add it
        if (ref != null && !refs.contains(ref)) {
            refs.add(ref);
        }
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
    public static boolean isExportedPolicy(Document doc) {
        Element rootel = doc.getDocumentElement();
        if (rootel == null || rootel.getNamespaceURI() == null) return false;
        if (!rootel.getNamespaceURI().equals(ExporterConstants.EXPORTED_POL_NS)) return false;
        if (!rootel.getLocalName().equals(ExporterConstants.EXPORTED_DOCROOT_ELNAME)) return false;
        return true;
    }
}
