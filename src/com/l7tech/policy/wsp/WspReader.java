package com.l7tech.policy.wsp;

import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.xml.TooManyChildElementsException;
import com.l7tech.policy.assertion.Assertion;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.logging.Logger;

/**
 * Build a policy tree from an XML document.
 * User: mike
 * Date: Jun 10, 2003
 * Time: 3:44:19 PM
 */
public class WspReader {
    private static final Logger log = Logger.getLogger(WspReader.class.getName());

    private WspReader() {
    }

    /**
     * Check the policy version of the policy rooted in the specified element.  This does a very shallow examination:
     * the policy may be invalid even if a valid version identifier is returned by this method.
     *
     * @param policyElement the element to examine.  Must be a Policy element in either the wsp or l7p namespaces.
     * @return an opaque WspVersion identifier
     * @throws InvalidPolicyStreamException if the policy does not correspond to any known version
     */
    public static WspVersion getPolicyVersion(Element policyElement) throws InvalidPolicyStreamException {
        if (!"Policy".equals(policyElement.getLocalName()))
            throw new InvalidPolicyStreamException("Unable to determine policy version");

        String ns = policyElement.getNamespaceURI();
        // these are hardcoded since they historical namespaces: please do NOT change to SoapUtil.WSP_NAMESPACE
        // (or whatever) since these namespaces are now historical data and hence set in stone.
        if ("http://www.layer7tech.com/ws/policy".equals(ns)) {
            return WspVersionImpl.VERSION_2_1;
        } else if ("http://schemas.xmlsoap.org/ws/2002/12/policy".equals(ns)) {
            return WspVersionImpl.VERSION_3_0;
        } else
            throw new InvalidPolicyStreamException("Unable to determine policy version");
    }

    /**
     * Attempt to locate the Policy element.  It may be the case that rootElement already _is_ the
     * Policy element, or that the rootElement is exp:Export and it contains the Policy element.
     * This does not actually check the version or namespace of the returned policy element.
     *
     * @param rootElement the root element to check
     * @return a Policy element.  Never null.
     * @throws InvalidPolicyStreamException if a Policy element cannot be located.
     */
    public static Element findPolicyElement(Element rootElement) throws InvalidPolicyStreamException {
        if ("Policy".equals(rootElement.getLocalName()))
            return rootElement;
        try {
            Element n = XmlUtil.findOnlyOneChildElementByName(rootElement, "http://schemas.xmlsoap.org/ws/2002/12/policy", "Policy");
            if (n != null)
                return n;
            n = XmlUtil.findOnlyOneChildElementByName(rootElement, "http://www.layer7tech.com/ws/policy", "Policy");
            if (n != null)
                return n;
        } catch (TooManyChildElementsException e1) {
            throw new InvalidPolicyStreamException("More than one Policy element found", e1);
        }
        throw new InvalidPolicyStreamException("Unable to locate Policy element");
    }

    /**
     * Reads an XML-encoded policy document from the given element and
     * returns the corresponding policy tree.
     * @param policyElement an element of type WspConstants.L7_POLICY_NS:WspConstants.POLICY_ELNAME
     * @return the policy tree it contained.  A null return means a valid &lt;Policy/&gt; tag was present, but
     *         it was empty.
     * @throws InvalidPolicyStreamException if the stream did not contain a valid policy
     */
    public static Assertion parse(Element policyElement) throws InvalidPolicyStreamException {
        return parse(findPolicyElement(policyElement), StrictWspVisitor.INSTANCE);
    }

    static Assertion parse(Element policyElement, WspVisitor visitor) throws InvalidPolicyStreamException {
        WspVersion policyVersion = getPolicyVersion(policyElement);
        if (visitor == null || visitor == StrictWspVisitor.INSTANCE) {
            if (WspVersionImpl.VERSION_2_1.equals(policyVersion)) {
                log.info("Applying on-the-fly import filter to convert 2.1 policy to 3.0");
                return parse(WspTranslator21to30.INSTANCE.translatePolicy(policyElement), visitor);
            }
            if (!(WspVersionImpl.VERSION_3_0.equals(policyVersion))) {
                log.warning("Unable to read policy: unsupported policy version " + policyVersion);
                throw new InvalidPolicyStreamException("Unsupported policy version " + policyVersion);
            }
        }

        List childElements = TypeMappingUtils.getChildElements(policyElement);
        if (childElements.isEmpty())
            return null; // Empty Policy tag explicitly means a null policy

        if (childElements.size() != 1)
            throw new InvalidPolicyStreamException("Policy does not have exactly zero or one immediate child");
        Object target = TypeMappingUtils.thawElement((Element) childElements.get(0), visitor).target;
        if (!(target instanceof Assertion))
            throw new InvalidPolicyStreamException("Policy does not have an assertion as its immediate child");
        Assertion root = (Assertion) target;
        if (root != null)
            root.treeChanged();        
        return root;
    }

    /**
     * Reads an XML-encoded policy document from the given input stream and
     * returns the corresponding policy tree.  Supported formats are wsp:Policy and exp:Export.  For exp:Export,
     * encoded external entity references are ignored by this parser.
     *
     * @param wspStream the stream to read
     * @return the policy tree it contained.  A null return means a valid &lt;Policy/&gt; tag was present, but it was empty.
     * @throws IOException if the stream did not contain a valid policy
     */
    public static Assertion parse(InputStream wspStream) throws IOException {
        return parse(wspStream, StrictWspVisitor.INSTANCE);
    }

    static Assertion parse(InputStream wspStream, WspVisitor visitor) throws IOException {
        try {
            Document doc = XmlUtil.parse(wspStream);
            Element root = findPolicyElement(doc.getDocumentElement());
            if (!WspConstants.POLICY_ELNAME.equals(root.getLocalName()))
                throw new InvalidPolicyStreamException("Document element is not wsp:Policy");
            String rootNs = root.getNamespaceURI();
            if (!WspConstants.WSP_POLICY_NS.equals(rootNs) && !WspConstants.L7_POLICY_NS.equals(rootNs))
                throw new InvalidPolicyStreamException("Document element is not in a recognized namespace");
            return parse(root, visitor);
        } catch (Exception e) {
            throw new InvalidPolicyStreamException("Unable to parse policy", e);
        }
    }



    /**
     * Converts an XML-encoded policy document into the corresponding policy tree.
     * @param wspXml    the document to parse
     * @return          the policy tree it contained, or null
     * @throws ArrayIndexOutOfBoundsException if the string contains no objects
     */
    public static Assertion parse(String wspXml) throws IOException {
        return parse(wspXml, StrictWspVisitor.INSTANCE);
    }

    static Assertion parse(String wspXml, WspVisitor visitor) throws IOException {
        if (wspXml == null)
            throw new IllegalArgumentException("wspXml may not be null");
        return parse(new ByteArrayInputStream(wspXml.getBytes()), visitor);
    }

}
