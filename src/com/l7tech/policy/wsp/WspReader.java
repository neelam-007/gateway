package com.l7tech.policy.wsp;

import com.l7tech.common.util.XmlUtil;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.LinkedList;
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
     * Reads an XML-encoded policy document from the given input stream and
     * returns the corresponding policy tree.
     * @param wspStream the stream to read
     * @return the policy tree it contained, or null
     * @throws IOException if the stream did not contain a valid policy
     */
    public static Assertion parse(InputStream wspStream) throws IOException {
        try {
            Document doc = XmlUtil.parse(wspStream);
            NodeList policyTags = doc.getElementsByTagNameNS(WspConstants.POLICY_NS,  "Policy");
            if (policyTags.getLength() > 1)
                throw new InvalidPolicyStreamException("More than one Policy tag was found");
            Node policy = policyTags.item(0);
            if (policy == null)
                throw new InvalidPolicyStreamException("No enclosing Policy tag was found (using namespace " +
                                                       WspConstants.POLICY_NS + ")");
            List childElements = WspConstants.getChildElements(policy);
            // allow empty policies.
            if (childElements.isEmpty()) return null;
            if (childElements.size() != 1)
                throw new InvalidPolicyStreamException("Policy does not have exactly one immediate child");
            Node node = (Node) childElements.get(0);
            return nodeToAssertion(node);
        } catch (Exception e) {
            throw new InvalidPolicyStreamException("Unable to parse policy", e);
        }
    }

    private static Assertion nodeToAssertion(Node node) throws InvalidPolicyStreamException {
        if (!WspConstants.POLICY_NS.equals(node.getNamespaceURI()))
            throw new InvalidPolicyStreamException("Policy contains node \"" + node.getNodeName() +
                                             "\" with unrecognized namespace URI \"" + node.getNamespaceURI() + "\"");
        String name = node.getLocalName();
        WspConstants.AssertionMapping am = WspConstants.findAssertionMappingByTagName(name);
        if (am == null)
            throw new InvalidPolicyStreamException("Policy contains unrecognized node \"" + node.getNodeName() + "\"");
        Assertion assertion = am.source.getCopy();

        if (assertion instanceof CompositeAssertion) {
            // gather children
            List convertedKids = new LinkedList();
            List kids = WspConstants.getChildElements(node);
            for (Iterator i = kids.iterator(); i.hasNext();) {
                Node kidNode = (Node) i.next();
                convertedKids.add(nodeToAssertion(kidNode));
            }
            ((CompositeAssertion)assertion).setChildren(convertedKids);
        } else {
            // gather properties
            List properties = WspConstants.getChildElements(node);
            for (Iterator i = properties.iterator(); i.hasNext();) {
                Element kid = (Element) i.next();
                String parm = kid.getLocalName();

                WspConstants.TypedReference thawedReference = WspConstants.TypeMapping.thawElement(kid);
                Object thawed = thawedReference.target;
                Class thawedType = thawedReference.type;

                try {
                    //System.out.println("Thawing: " + name + ".set" + parm + "((" + thawedType.getName() + ") " + thawed + ")");
                    Method setter = assertion.getClass().getMethod("set" + parm, new Class[] { thawedType });
                    setter.invoke(assertion, new Object[] { thawed });
                } catch (NoSuchMethodException e) {
                    throw new InvalidPolicyStreamException("Policy contains reference to unsupported assertion property " + parm, e);
                } catch (SecurityException e) {
                    throw new InvalidPolicyStreamException("Policy contains reference to unsupported assertion property " + parm, e);
                } catch (InvocationTargetException e) {
                    throw new InvalidPolicyStreamException("Policy contains invalid assertion property " + parm, e);
                } catch (IllegalAccessException e) {
                    throw new InvalidPolicyStreamException("Policy contains reference to unsupported assertion property " + parm, e);
                }
            }
        }

        return assertion;
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
