package com.l7tech.policy.exporter;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.wsp.WspWriter;
import com.l7tech.policy.wsp.WspConstants;
import com.l7tech.common.util.XmlUtil;

import java.io.File;
import java.io.IOException;

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
        Element referencesEl = wrapExportReferencesToPolicyDocument(policydoc);
        // add external dependencies to document
        // todo
        return policydoc;
    }

    public void exportToFile(Assertion rootAssertion, File outputFile) throws IOException, SAXException {
        Document doc = exportToDocument(rootAssertion);
        // write doc to file
        // todo
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
