package com.l7tech.console.policy.exporter;

import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.util.ResourceUtils;
import com.l7tech.common.xml.InvalidDocumentFormatException;
import com.l7tech.common.policy.Policy;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.Include;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.policy.wsp.InvalidPolicyStreamException;
import com.l7tech.policy.wsp.WspConstants;
import com.l7tech.policy.wsp.WspWriter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Imports a policy document and resolve references if necessary.
 * This is meant to be invoked on the SSM and might trigger some GUI elements to pop up
 * asking for administrator's feedback.
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Jul 23, 2004<br/>
 */
public class PolicyImporter {
    /**
     * This is a container for the results of importing a policy. It contains the root assertion and a map
     * of the included policy fragments.
     */
    public static class PolicyImporterResult {
        public Assertion assertion;
        public HashMap<String, Policy> policyFragments;

        public PolicyImporterResult(Assertion assertion, HashMap<String, Policy> policyFragments) {
            this.assertion = assertion;
            this.policyFragments = policyFragments;
        }
    }

    /**
     * Import a policy from file.
     * @param input the file containing the exported policy document
     * @return the imported policy or null if the document did not contain a policy or if the policy could
     *         not be resolved.
     * @throws InvalidPolicyStreamException somehing unexpected in the passed file.
     */
    public static PolicyImporterResult importPolicy(File input) throws InvalidPolicyStreamException {
        String name = input.getPath();
        // Read XML document from this
        Document readDoc = null;
        InputStream in = null;
        try {
            //noinspection IOResourceOpenedButNotSafelyClosed
            in = new FileInputStream(input);
            readDoc = XmlUtil.parse(in);
        } catch (IOException e) {
            logger.log(Level.WARNING, "Could not read xml document from " + name, e);
            throw new InvalidPolicyStreamException(e);
        } catch (SAXException e) {
            logger.log(Level.WARNING, "Could not read xml document from " + name, e);
            throw new InvalidPolicyStreamException(e);
        } finally {
            ResourceUtils.closeQuietly(in);
        }
        // Import policy references first
        Element referencesEl = XmlUtil.findFirstChildElementByName(readDoc.getDocumentElement(),
                                                                 ExporterConstants.EXPORTED_POL_NS,
                                                                 ExporterConstants.EXPORTED_REFERENCES_ELNAME);

        RemoteReferenceResolver resolver = new RemoteReferenceResolver();
        ExternalReference[] references = null;
        HashMap<String, Policy> fragments = new HashMap<String, Policy>();
        if (referencesEl != null) {
            try {
                references = ExternalReference.parseReferences(referencesEl);

                for(ExternalReference reference : references) {
                    if(reference instanceof IncludedPolicyReference) {
                        IncludedPolicyReference ipr = (IncludedPolicyReference)reference;
                        ipr.setFromImport(true);
                        fragments.put(ipr.getName(), new Policy(ipr.getType(), ipr.getName(), ipr.getXml(), ipr.isSoap()));
                    }
                }
            } catch (InvalidDocumentFormatException e) {
                logger.log(Level.WARNING, "cannot parse references from document " + name, e);
            }
            if (references == null || !resolver.resolveReferences(references)) {
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

        if (policy == null) {
            Element re = readDoc.getDocumentElement();
            if (WspConstants.POLICY_ELNAME.equals(re.getLocalName()) &&
                    (WspConstants.L7_POLICY_NS.equals(re.getNamespaceURI()) ||
                     WspConstants.WSP_POLICY_NS.equals(re.getNamespaceURI())))
                policy = re;
        }

        if (policy != null) {
            Assertion rootAssertion = resolver.localizePolicy(policy);
            if(references != null && fragments.size() > 0) {
                try {
                    updateIncludeAssertions(rootAssertion, references, fragments);

                    for(ExternalReference reference : references) {
                        if(reference instanceof IncludedPolicyReference) {
                            IncludedPolicyReference ipr = (IncludedPolicyReference)reference;
                            if(ipr.getUseType() == IncludedPolicyReference.UseType.USE_EXISTING && !doesFragmentRequireUpdate(fragments.get(ipr.getName()).getAssertion(), references, fragments)) {
                                fragments.remove(ipr.getName());
                            }
                        }
                    }
                } catch(IOException e) {
                    logger.log(Level.WARNING, "cannot parse references from document " + name, e);
                }
            }
            return new PolicyImporterResult(rootAssertion, fragments);
        } else {
            logger.warning("The document " + name + " did not contain a policy at all.");
        }
        throw new InvalidPolicyStreamException("Document does not seem to include a policy.");
    }

    /**
     * Updates the include assertions in a policy based on the results of the external references resolution.
     * @param importedPolicyRootAssertion The root assertion of the policy to update
     * @param references The external references array. Only uses the IncludedPolicyReference objects.
     * @param fragments The map of policy fragments. This may be updated.
     * @throws IOException
     */
    private static void updateIncludeAssertions(Assertion importedPolicyRootAssertion, ExternalReference[] references, HashMap<String, Policy> fragments) throws IOException {
        updateExistingIncludes(importedPolicyRootAssertion, references, fragments);

        for(ExternalReference reference : references) {
            if(reference instanceof IncludedPolicyReference) {
                IncludedPolicyReference ipr = (IncludedPolicyReference)reference;
                Policy policy = null;
                if(ipr.getUseType() == IncludedPolicyReference.UseType.RENAME) {
                    policy = fragments.get(ipr.getOldName());
                    policy.setName(ipr.getName());
                    fragments.remove(ipr.getOldName());
                    fragments.put(ipr.getName(), policy);
                } else {
                    policy = fragments.get(ipr.getName());
                }

                if(ipr.getUseType() == IncludedPolicyReference.UseType.USE_EXISTING || ipr.getUseType() == IncludedPolicyReference.UseType.UPDATE) {
                    policy.setOid(ipr.getOid());
                }

                if(policy != null) {
                    updateExistingIncludes(policy.getAssertion(), references, fragments);
                    policy.setXml(WspWriter.getPolicyXml(policy.getAssertion()));
                }
            }
        }
    }

    private static void updateExistingIncludes(Assertion rootAssertion, ExternalReference[] references, HashMap<String, Policy> fragments) {
        if(rootAssertion instanceof CompositeAssertion) {
            CompositeAssertion compAssertion = (CompositeAssertion)rootAssertion;
            for(Iterator it = compAssertion.children();it.hasNext();) {
                Assertion child = (Assertion)it.next();
                updateExistingIncludes(child, references, fragments);
            }
        } else if(rootAssertion instanceof Include) {
            Include includeAssertion = (Include)rootAssertion;
            IncludedPolicyReference ipr = null;
            for(ExternalReference reference : references) {
                if(reference instanceof IncludedPolicyReference) {
                    IncludedPolicyReference x = (IncludedPolicyReference)reference;
                    if(includeAssertion.getPolicyName().equals(x.getName())) {
                        includeAssertion.setPolicyOid(x.getOid());
                        break;
                    } else if(x.getUseType() == IncludedPolicyReference.UseType.RENAME && includeAssertion.getPolicyName().equals(x.getOldName())) {
                        includeAssertion.setPolicyName(x.getName());
                        includeAssertion.setPolicyOid(x.getOid());
                        break;
                    }
                }
            }
        }
    }

    private static boolean doesFragmentRequireUpdate(Assertion rootAssertion, ExternalReference[] references, HashMap<String, Policy> fragments) throws IOException {
        if(rootAssertion instanceof CompositeAssertion) {
            CompositeAssertion compAssertion = (CompositeAssertion)rootAssertion;
            for(Iterator it = compAssertion.children();it.hasNext();) {
                Assertion child = (Assertion)it.next();
                if(doesFragmentRequireUpdate(child, references, fragments)) {
                    return true;
                }
            }
        } else if(rootAssertion instanceof Include) {
            Include includeAssertion = (Include)rootAssertion;
            // Find the matching reference
            IncludedPolicyReference ipr = null;
            for(ExternalReference reference : references) {
                if(reference instanceof IncludedPolicyReference) {
                    IncludedPolicyReference x = (IncludedPolicyReference)reference;
                    if(x.getUseType() == IncludedPolicyReference.UseType.USE_EXISTING && includeAssertion.getPolicyName().equals(x.getName())) {
                        return doesFragmentRequireUpdate(fragments.get(x.getName()).getAssertion(), references, fragments);
                    } else if(includeAssertion.getPolicyName().equals(x.getName())) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    static final Logger logger = Logger.getLogger(PolicyImporter.class.getName());
}
