package com.l7tech.policy.exporter;

import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.xml.InvalidDocumentFormatException;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.wsp.InvalidPolicyStreamException;
import com.l7tech.policy.wsp.WspConstants;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Imports a policy document and resolve references if necessary.
 * This is meant to be invoked on the SSM and might trigger some GUI elements to pop up
 * asking for administrator's feedback.
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Jul 23, 2004<br/>
 * $Id$
 */
public class PolicyImporter {

    /**
     * Import a policy from file.
     * @param input the file containing the exported policy document
     * @return the imported policy or null if the document did not contain a policy or if the policy could
     *         not be resolved.
     * @throws InvalidPolicyStreamException somehing unexpected in the passed file.
     */
    public static Assertion importPolicy(File input) throws InvalidPolicyStreamException {
        String name = input.getPath();
        // Read XML document from this
        Document readDoc = null;
        try {
            readDoc = XmlUtil.parse(new FileInputStream(input));
        } catch (IOException e) {
            logger.log(Level.WARNING, "Could not read xml document from " + name, e);
            throw new InvalidPolicyStreamException(e);
        } catch (SAXException e) {
            logger.log(Level.WARNING, "Could not read xml document from " + name, e);
            throw new InvalidPolicyStreamException(e);
        }
        // Import policy references first
        Element referencesEl = XmlUtil.findFirstChildElementByName(readDoc.getDocumentElement(),
                                                                 ExporterConstants.EXPORTED_POL_NS,
                                                                 ExporterConstants.EXPORTED_REFERENCES_ELNAME);

        // Importing the policy may involve GUI elements and take a loong time
        // see note below regarding the handling of a RTE caused by an expiration
        // of the admin session.
        // Todo, catching RTEs is a bit evil. we should have some special
        // SessionExpiredException extends RuntimeException
        try {
            RemoteReferenceResolver resolver = new RemoteReferenceResolver();
            if (referencesEl != null) {
                ExternalReference[] references = null;
                try {
                    references = ExternalReference.parseReferences(referencesEl);
                } catch (InvalidDocumentFormatException e) {
                    logger.log(Level.WARNING, "cannot parse references from document " + name, e);
                }
                if (!resolver.resolveReferences(references)) {
                    logger.info("The resolution process failed. This policy will not be imported");
                    return null;
                }
            } else {
                logger.warning("The policy document " + name + " did not contain exported references. Maybe this is " +
                            "an old-school style policy export.");
            }
            Element policy = XmlUtil.findFirstChildElementByName(readDoc.getDocumentElement(),
                                                                 WspConstants.L7_POLICY_NS,
                                                                 WspConstants.POLICY_ELNAME);
            // try alternative
            if (policy == null) {
                policy = XmlUtil.findFirstChildElementByName(readDoc.getDocumentElement(),
                                                             WspConstants.WSP_POLICY_NS,
                                                             WspConstants.POLICY_ELNAME);
            }

            if (policy != null) {
                return resolver.localizePolicy(policy);
            } else {
                logger.warning("The document " + name + " did not contain a policy at all.");
            }
        } catch (RuntimeException e) {
            // fla, fix for bugzilla #1043. when the admin session expires before you get there, this throws
            // a Runtime Exception
            logger.log(Level.WARNING, "Could not localize policy import document, perhaps " +
                                      "the admin session expired in the middle of the operation", e);
        }
        return null;
    }

    static final Logger logger = Logger.getLogger(PolicyImporter.class.getName());
}
