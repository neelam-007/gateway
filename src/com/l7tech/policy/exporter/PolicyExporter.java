package com.l7tech.policy.exporter;

import com.l7tech.common.util.XmlUtil;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.identity.SpecificUser;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.policy.wsp.WspWriter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.io.FileOutputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Collection;
import java.util.ArrayList;

/**
 * Exports a Policy to an XML file that contains details of all external
 * references necessary to be able to re-import on another SSM.
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Jul 16, 2004<br/>
 * $Id$<br/>
 */
public class PolicyExporter {

    public Document exportToDocument(Assertion rootAssertion) throws IOException, SAXException {
        // do policy to xml
        Document policydoc = XmlUtil.stringToDocument(WspWriter.getPolicyXml(rootAssertion));
        // go through each assertion and list external dependencies
        Collection refs = new ArrayList();
        traverseAssertionTreeForReferences(rootAssertion, refs);
        // add external dependencies to document
        Element referencesEl = wrapExportReferencesToPolicyDocument(policydoc);
        serializeReferences(referencesEl, (ExternalReference[])refs.toArray(new ExternalReference[0]));
        return policydoc;
    }

    private void serializeReferences(Element referencesEl, ExternalReference[] references) {
        for (int i = 0; i < references.length; i++) {
            references[i].serializeToRefElement(referencesEl);
        }
    }

    public void exportToFile(Assertion rootAssertion, File outputFile) throws IOException, SAXException {
        Document doc = exportToDocument(rootAssertion);
        // write doc to file
        FileOutputStream fos = new FileOutputStream(outputFile);
        fos.write(XmlUtil.nodeToString(doc).getBytes());
        fos.close();
    }

    /**
     * Recursively go through an assertion tree populating the references as necessary.
     */
    private void traverseAssertionTreeForReferences(Assertion rootAssertion, Collection refs) {
        if (rootAssertion instanceof CompositeAssertion) {
            CompositeAssertion ca = (CompositeAssertion)rootAssertion;
            List children = ca.getChildren();
            for (Iterator i = children.iterator(); i.hasNext();) {
                Assertion child = (Assertion)i.next();
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
    private void appendRelatedReferences(Assertion assertion, Collection refs) {
        if (assertion instanceof SpecificUser) {
            SpecificUser suassertion = (SpecificUser)assertion;
            IdProviderReference ref = new IdProviderReference(suassertion.getIdentityProviderOid());
            if (!refs.contains(ref)) {
                refs.add(ref);
            }
        }
        // todo, same for all assertion types
    }

    private Element wrapExportReferencesToPolicyDocument(Document originalPolicy) {
        Element exportRoot = originalPolicy.createElementNS(ExporterConstants.EXPORTED_POL_NS,
                                                            ExporterConstants.EXPORTED_DOCROOT_ELNAME);
        exportRoot.setAttribute("xmlns:" + ExporterConstants.EXPORTED_POL_PREFIX, ExporterConstants.EXPORTED_POL_NS);
        exportRoot.setPrefix(ExporterConstants.EXPORTED_POL_PREFIX);
        Element referencesEl = originalPolicy.createElementNS(ExporterConstants.EXPORTED_POL_NS,
                                                              ExporterConstants.EXPORTED_REFERENCES_ELNAME);
        referencesEl.setPrefix(ExporterConstants.EXPORTED_POL_PREFIX);
        exportRoot.appendChild(referencesEl);
        Element previousRoot = originalPolicy.getDocumentElement();
        exportRoot.appendChild(previousRoot);
        originalPolicy.appendChild(exportRoot);
        return referencesEl;
    }
}
