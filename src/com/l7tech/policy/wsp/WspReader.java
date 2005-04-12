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
     * returns the corresponding policy tree.  Unrecognized chunks of XML will be preserved inside
     * {@link com.l7tech.policy.assertion.UnknownAssertion} instances, and processing will try to continue.
     *
     * @param policyElement an element of type WspConstants.L7_POLICY_NS_CURRENT:WspConstants.POLICY_ELNAME
     * @return the policy tree it contained.  A null return means a valid &lt;Policy/&gt; tag was present, but
     *         it was empty.
     * @throws InvalidPolicyStreamException if the stream did not contain a valid policy
     */
    public static Assertion parsePermissively(Element policyElement) throws InvalidPolicyStreamException {
        return parse(findPolicyElement(policyElement), PermissiveWspVisitor.INSTANCE);
    }

    static Assertion parse(Element policyElement, WspVisitor visitor) throws InvalidPolicyStreamException {
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

    static Assertion parse(InputStream wspStream, WspVisitor visitor) throws IOException {
        try {
            Document doc = XmlUtil.parse(wspStream);
            Element root = findPolicyElement(doc.getDocumentElement());
            if (!WspConstants.POLICY_ELNAME.equals(root.getLocalName()))
                throw new InvalidPolicyStreamException("Document element local name is not Policy");
            String rootNs = root.getNamespaceURI();
            if (!WspConstants.isRecognizedPolicyNsUri(rootNs))
                throw new InvalidPolicyStreamException("Document element is not in a recognized namespace");
            return parse(root, visitor);
        } catch (Exception e) {
            throw new InvalidPolicyStreamException("Unable to parse policy", e);
        }
    }

    /**
     * Converts an XML-encoded policy document into the corresponding policy tree.
     * Unrecognized policy assertions, or unrecognized properties of
     * recognized assertions, will immediately abort processing with an InvalidPolicyStreamException.
     *
     * @param wspXml    the document to parse
     * @return          the policy tree it contained, or null
     * @throws IOException if the policy was not valid.
     */
    public static Assertion parseStrictly(String wspXml) throws IOException {
        return parse(wspXml, StrictWspVisitor.INSTANCE);
    }

    /**
     * Converts an XML-encoded policy document into the corresponding policy tree.
     * Unrecognized chunks of XML will be preserved inside {@link com.l7tech.policy.assertion.UnknownAssertion}
     * instances, and processing will try to continue.
     *
     * @param wspXml    the document to parse
     * @return          the policy tree it contained, or null
     * @throws IOException if the policy was not valid, even if unrecognized assertions are preserved as {@link com.l7tech.policy.assertion.UnknownAssertion}.
     */
    public static Assertion parsePermissively(String wspXml) throws IOException {
        return parse(wspXml, PermissiveWspVisitor.INSTANCE);
    }

    static Assertion parse(String wspXml, WspVisitor visitor) throws IOException {
        if (wspXml == null)
            throw new IllegalArgumentException("wspXml may not be null");
        return parse(new ByteArrayInputStream(wspXml.getBytes()), visitor);
    }

}
