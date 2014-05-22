package com.l7tech.policy.exporter;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.common.export.ExternalReferenceFactory;
import com.l7tech.policy.Policy;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.Include;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.policy.wsp.InvalidPolicyStreamException;
import com.l7tech.policy.wsp.WspConstants;
import com.l7tech.policy.wsp.WspReader;
import com.l7tech.policy.wsp.WspWriter;
import com.l7tech.util.DomUtils;
import com.l7tech.util.InvalidDocumentFormatException;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.EntityResolver;

import java.io.IOException;
import java.util.*;
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
 */
public class PolicyImporter {
    /**
     * This is a container for the results of importing a policy. It contains the root assertion and a map
     * of the included policy fragments.
     */
    public static final class PolicyImporterResult {
        public Assertion assertion;
        public HashMap<String, Policy> policyFragments;

        public PolicyImporterResult(Assertion assertion, HashMap<String, Policy> policyFragments) {
            this.assertion = assertion;
            this.policyFragments = policyFragments;
        }
    }

    public interface PolicyImporterAdvisor {
        boolean mapReference( final String referenceType, final String referenceId, final String targetId );
        boolean resolveReferences( ExternalReference[] unresolvedRefsArray ) throws PolicyImportCancelledException;
        boolean acceptPolicyConflict( String policyName, String existingPolicyName, String guid );
        boolean applyRenameToResolvedReferences(Collection<ExternalReference> references )throws PolicyImportCancelledException;
    }

    /**
     * Import a policy from file.
     * @param receiverPolicy the policy that is receiving the imported policy
     * @param policyExport the exported policy document
     * @return the imported policy or null if the document did not contain a policy or if the policy could
     *         not be resolved.
     * @throws InvalidPolicyStreamException something unexpected in the passed file.
     */
    public static PolicyImporterResult importPolicy( final Policy receiverPolicy,
                                                     final Document policyExport,
                                                     final Set<ExternalReferenceFactory> factories,
                                                     final WspReader wspReader,
                                                     final ExternalReferenceFinder finder,
                                                     final EntityResolver entityResolver,
                                                     final ExternalReferenceErrorListener errorListener,
                                                     final PolicyImporterAdvisor advisor ) throws InvalidPolicyStreamException, PolicyImportCancelledException {
        // Import policy references first
        Element referencesEl = DomUtils.findFirstChildElementByName(policyExport.getDocumentElement(),
                                                                 ExporterConstants.EXPORTED_POL_NS,
                                                                 ExporterConstants.EXPORTED_REFERENCES_ELNAME);

        ExternalReferenceResolver resolver = new ExternalReferenceResolver( wspReader, finder, advisor, entityResolver );
        Collection<ExternalReference> references = new ArrayList<ExternalReference>();
        HashMap<String, Policy> fragments = new HashMap<String, Policy>();
        HashMap<Long, String> fragmentOidToNameMap = new HashMap<Long, String>();
        if (referencesEl != null) {
            try {
                references = ExternalReference.parseReferences(finder, entityResolver, factories, referencesEl);

                for(ExternalReference reference : references) {
                    reference.setExternalReferenceErrorListener(errorListener);
                }

                for(ExternalReference reference : references) {
                    if(reference instanceof IncludedPolicyReference) {
                        IncludedPolicyReference ipr = (IncludedPolicyReference)reference;

                        ipr.setFromImport(true);
                        Policy p = new Policy(ipr.getType(), ipr.getName(), ipr.getXml(), ipr.isSoap());
                        p.setGuid(ipr.getGuid());
                        p.setInternalTag(ipr.getInternalTag());
                        fragments.put(ipr.getGuid(), p);
                        fragmentOidToNameMap.put(ipr.getOid(), ipr.getName());
                    }
                }
            } catch ( InvalidDocumentFormatException e) {
                logger.log(Level.WARNING, "Cannot parse policy export references", e);
            }

            Element policy = XmlUtil.findFirstChildElementByName(policyExport.getDocumentElement(),
                                                                 WspConstants.L7_POLICY_NS,
                                                                 WspConstants.POLICY_ELNAME);
            // try alternative
            if (policy == null) {
                policy = XmlUtil.findFirstChildElementByName(policyExport.getDocumentElement(),
                                                             WspConstants.WSP_POLICY_NS,
                                                             WspConstants.POLICY_ELNAME);
            }

            correctIncludeAssertions(policy, fragmentOidToNameMap);
            for(Policy p : fragments.values()) {
                try {
                    if(correctIncludeAssertions(p.getAssertion(), fragmentOidToNameMap)) {
                        p.setXml(WspWriter.getPolicyXml(p.getAssertion()));
                    }
                } catch(IOException e) {
                    logger.log(Level.WARNING, "failed to parse policy XML for policy #" + p.getGoid());
                }
            }
            try {
                Assertion rootAssertion = WspReader.getDefault().parsePermissively(policy, WspReader.INCLUDE_DISABLED);
                HashSet<String> visitedGuids = new HashSet<String>();
                visitedGuids.add(receiverPolicy.getGuid());
                if(containsCircularReferences(rootAssertion, fragments, visitedGuids)) {
                    logger.log(Level.WARNING, "The imported policy contains circular includes. The import will be aborted.");
                    throw new InvalidPolicyStreamException("The imported policy contains circular includes.");
                }
            } catch(Exception e) {
                logger.info("Failed to check for circular includes.");
                return null;
            }

            if (!resolver.resolveReferences(references)) {
                logger.info("The resolution process failed. This policy will not be imported");
                return null;
            }
        } else {
            logger.warning("The policy export did not contain exported references. Maybe this is " +
                        "an old-school style policy export.");
        }
        Element policy = DomUtils.findFirstChildElementByName(policyExport.getDocumentElement(),
                                                             WspConstants.L7_POLICY_NS,
                                                             WspConstants.POLICY_ELNAME);
        // try alternative
        if (policy == null) {
            policy = DomUtils.findFirstChildElementByName(policyExport.getDocumentElement(),
                                                         WspConstants.WSP_POLICY_NS,
                                                         WspConstants.POLICY_ELNAME);
        }

        if (policy == null) {
            Element re = policyExport.getDocumentElement();
            if (WspConstants.POLICY_ELNAME.equals(re.getLocalName()) &&
                    (WspConstants.L7_POLICY_NS.equals(re.getNamespaceURI()) ||
                     WspConstants.WSP_POLICY_NS.equals(re.getNamespaceURI())))
                policy = re;
        }

        if (policy != null) {
            Assertion rootAssertion = resolver.localizePolicy(policy);
            if(fragments.size() > 0) {
                try {
                    for(ExternalReference reference : references) {
                        if(reference instanceof IncludedPolicyReference) {
                            IncludedPolicyReference ipr = (IncludedPolicyReference)reference;
                            if(ipr.getUseType() == IncludedPolicyReference.UseType.USE_EXISTING) {
                                fragments.remove(ipr.getGuid());
                            } else {
                                Policy fragmentPolicy = fragments.get(ipr.getGuid());
                                fragmentPolicy.setXml(WspWriter.getPolicyXml(resolver.localizePolicy(fragmentPolicy.getAssertion())));

                                if(ipr.getUseType() == IncludedPolicyReference.UseType.RENAME && ipr.getOldName().equals(fragmentPolicy.getName())) {
                                    fragmentPolicy.setName(ipr.getName());
                                }
                            }
                        }
                    }
                } catch(IOException e) {
                    logger.log(Level.WARNING, "Cannot parse references from policy export.", e);
                }
            }
            return new PolicyImporterResult(rootAssertion, fragments);
        } else {
            logger.warning("The policy export did not contain a policy.");
        }
        throw new InvalidPolicyStreamException("Document does not seem to include a policy.");
    }

    private static boolean containsCircularReferences(final @Nullable Assertion rootAssertion, HashMap<String, Policy> fragments, HashSet<String> visitedGuids) throws IOException {
        if(rootAssertion instanceof CompositeAssertion) {
            CompositeAssertion compositeAssertion = (CompositeAssertion)rootAssertion;
            for(Iterator it = compositeAssertion.children();it.hasNext();) {
                Assertion child = (Assertion)it.next();
                if(containsCircularReferences(child, fragments, visitedGuids)) {
                    return true;
                }
            }
        } else if(rootAssertion instanceof Include) {
            Include includeAssertion = (Include)rootAssertion;
            if(visitedGuids.contains(includeAssertion.getPolicyGuid())) {
                return true;
            }

            Policy p = fragments.get(includeAssertion.getPolicyGuid());
            if(p != null) {
                Assertion includeRoot = p.getAssertion();
                visitedGuids.add(includeAssertion.getPolicyGuid());
                if(containsCircularReferences(includeRoot, fragments, visitedGuids)) {
                    return true;
                }
                visitedGuids.remove(includeAssertion.getPolicyGuid());
            }
        }

        return false;
    }

    private static void correctIncludeAssertions(Element element, HashMap<Long, String> fragmentOidToNameMap) {
        NodeList includeElements = element.getElementsByTagName("L7p:Include");
        for(int i = 0;i < includeElements.getLength();i++) {
            Element includeElement = (Element)includeElements.item(i);

            NodeList oidElements = includeElement.getElementsByTagName("L7p:PolicyOid");
            if(oidElements.getLength() > 0) {
                Element oidElement = (Element)oidElements.item(0);

                NodeList nameElements = includeElement.getElementsByTagName("L7p:PolicyName");
                if(nameElements.getLength() > 0) {
                    Element nameElement = (Element)nameElements.item(0);

                    try {
                        String realFragmentName = fragmentOidToNameMap.get(new Long(oidElement.getAttribute("boxedLongValue")));
                        if(realFragmentName != null) {
                            nameElement.setAttributeNS( null, "stringValue", realFragmentName );
                        }
                    } catch(NumberFormatException e) {
                        // Ignore, this include assertion is broken
                    }
                }
            }
        }
    }

    private static boolean correctIncludeAssertions(@Nullable Assertion rootAssertion, HashMap<Long, String> fragmentOidToNameMap) {
        if(rootAssertion instanceof CompositeAssertion) {
            CompositeAssertion compAssertion = (CompositeAssertion)rootAssertion;
            boolean retVal = false;
            for(Iterator it = compAssertion.children();it.hasNext();) {
                Assertion child = (Assertion)it.next();
                retVal = retVal | correctIncludeAssertions(child, fragmentOidToNameMap);
            }
            return retVal;
        } else if(rootAssertion instanceof Include) {
            Include includeAssertion = (Include)rootAssertion;
            if(includeAssertion.getPolicyOid() != null && includeAssertion.getPolicyName() != null) {
                String realFragmentName = fragmentOidToNameMap.get(includeAssertion.getPolicyOid());
                if(realFragmentName != null && !realFragmentName.equals(includeAssertion.getPolicyName())) {
                    includeAssertion.setPolicyName(realFragmentName);
                    return true;
                }
            }
        }

        return false;
    }

    static final Logger logger = Logger.getLogger(PolicyImporter.class.getName());
}