package com.l7tech.policy.wsp;

import com.l7tech.common.util.XmlUtil;
import com.l7tech.policy.assertion.Assertion;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

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
     * Reads an XML-encoded policy document from the given element and
     * returns the corresponding policy tree.
     * @param policyElement an element of type WspConstants.POLICY_NS:WspConstants.POLICY_ELNAME
     * @return the policy tree it contained.  A null return means a valid &lt;Policy/&gt; tag was present, but
     *         it was empty.
     * @throws InvalidPolicyStreamException if the stream did not contain a valid policy
     */
    public static Assertion parse(Element policyElement) throws InvalidPolicyStreamException {
        List childElements = WspConstants.getChildElements(policyElement);
        if (childElements.isEmpty())
            return null; // Empty Policy tag explicitly means a null policy

        if (childElements.size() != 1)
            throw new InvalidPolicyStreamException("Policy does not have exactly zero or one immediate child");
        Object target = WspConstants.thawElement((Element) childElements.get(0)).target;
        if (!(target instanceof Assertion))
            throw new InvalidPolicyStreamException("Policy does not have an assertion as its immediate child");
        return (Assertion) target;
    }

    /**
     * Reads an XML-encoded policy document from the given input stream and
     * returns the corresponding policy tree.
     * @param wspStream the stream to read
     * @return the policy tree it contained.  A null return means a valid &lt;Policy/&gt; tag was present, but it was empty.
     * @throws IOException if the stream did not contain a valid policy
     */
    public static Assertion parse(InputStream wspStream) throws IOException {
        try {
            Document doc = XmlUtil.parse(wspStream);
            NodeList policyTags = doc.getElementsByTagNameNS(WspConstants.POLICY_NS,  WspConstants.POLICY_ELNAME);
            if (policyTags.getLength() > 1)
                throw new InvalidPolicyStreamException("More than one Policy tag was found");
            Node policy = policyTags.item(0);
            if (policy == null)
                throw new InvalidPolicyStreamException("No enclosing Policy tag was found (using namespace " +
                                                       WspConstants.POLICY_NS + ")");
            return parse((Element)policy);
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
        if (wspXml == null)
            throw new IllegalArgumentException("wspXml may not be null");
        return parse(new ByteArrayInputStream(wspXml.getBytes()));
    }
}
