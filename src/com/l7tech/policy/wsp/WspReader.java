package com.l7tech.policy.wsp;

import com.l7tech.common.util.XmlUtil;
import com.l7tech.policy.assertion.Assertion;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Build a policy tree from an XML document.
 * User: mike
 * Date: Jun 10, 2003
 * Time: 3:44:19 PM
 */
public class WspReader {

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
        if ("http://www.layer7tech.com/ws/policy".equals(ns)) {
            return WspVersionImpl.VERSION_2_1;
        } else if ("http://schemas.xmlsoap.org/ws/2002/12/policy".equals(ns)) {
            return WspVersionImpl.VERSION_3_0;
        } else
            throw new InvalidPolicyStreamException("Unable to determine policy version");
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
        return parse(policyElement, WspVisitorImpl.INSTANCE);
    }

    static Assertion parse(Element policyElement, WspVisitor visitor) throws InvalidPolicyStreamException {
        List childElements = WspConstants.getChildElements(policyElement);
        if (childElements.isEmpty())
            return null; // Empty Policy tag explicitly means a null policy

        if (childElements.size() != 1)
            throw new InvalidPolicyStreamException("Policy does not have exactly zero or one immediate child");
        Object target = WspConstants.thawElement((Element) childElements.get(0), visitor).target;
        if (!(target instanceof Assertion))
            throw new InvalidPolicyStreamException("Policy does not have an assertion as its immediate child");
        Assertion root = (Assertion) target;
        if (root != null)
            root.treeChanged();        
        return root;
    }

    /**
     * Reads an XML-encoded policy document from the given input stream and
     * returns the corresponding policy tree.
     * @param wspStream the stream to read
     * @return the policy tree it contained.  A null return means a valid &lt;Policy/&gt; tag was present, but it was empty.
     * @throws IOException if the stream did not contain a valid policy
     */
    public static Assertion parse(InputStream wspStream) throws IOException {
        return parse(wspStream, WspVisitorImpl.INSTANCE);
    }

    static Assertion parse(InputStream wspStream, WspVisitor visitor) throws IOException {
        try {
            Document doc = XmlUtil.parse(wspStream);
            Element root = doc.getDocumentElement();
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
        return parse(wspXml, WspVisitorImpl.INSTANCE);
    }

    static Assertion parse(String wspXml, WspVisitor visitor) throws IOException {
        if (wspXml == null)
            throw new IllegalArgumentException("wspXml may not be null");
        return parse(new ByteArrayInputStream(wspXml.getBytes()), visitor);
    }

}
