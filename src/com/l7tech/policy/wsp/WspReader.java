package com.l7tech.policy.wsp;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.util.XmlUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.NamedNodeMap;
import org.xml.sax.SAXException;
import org.apache.log4j.Category;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Iterator;
import java.util.LinkedList;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

/**
 * Build a policy tree from an XML document.
 * User: mike
 * Date: Jun 10, 2003
 * Time: 3:44:19 PM
 */
public class WspReader {
    private static final Category log = Category.getInstance(WspReader.class);

    private WspReader() {
    }

    /**
     * Reads an XML-encoded policy document from the given input stream and
     * returns the corresponding policy tree.
     * @param wspStream the stream to read
     * @return          the policy tree it contained, or null
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
            List childElements = getChildElements(policy);
            if (childElements.size() != 1)
                throw new InvalidPolicyStreamException("Policy does not have exactly one immediate child");
            Node node = (Node) childElements.get(0);
            return nodeToAssertion(node);
        } catch (Exception e) {
            throw new InvalidPolicyStreamException("Unable to parse policy", e);
        }
    }

    // Return a list of all children which are ELEMENTS
    private static List getChildElements(Node node) {
        NodeList kidNodes = node.getChildNodes();
        LinkedList kidElements = new LinkedList();
        for (int i = 0; i < kidNodes.getLength(); ++i) {
            Node n = kidNodes.item(i);
            if (n.getNodeType() == Node.ELEMENT_NODE)
                kidElements.add(n);
        }
        return kidElements;
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
            List kids = getChildElements(node);
            for (Iterator i = kids.iterator(); i.hasNext();) {
                Node kidNode = (Node) i.next();
                convertedKids.add(nodeToAssertion(kidNode));
            }
            ((CompositeAssertion)assertion).setChildren(convertedKids);
        } else {
            // gather properties
            List properties = getChildElements(node);
            for (Iterator i = properties.iterator(); i.hasNext();) {
                Node kidNode = (Node) i.next();
                String parm = kidNode.getLocalName();
                NamedNodeMap attrs = kidNode.getAttributes();
                if (attrs.getLength() != 1)
                    throw new InvalidPolicyStreamException("Policy contains a " + kidNode.getNodeName() +
                                                           " node that doesn't have exactly one attribute");
                Node attr = attrs.item(0);
                String typeName = attr.getLocalName();
                String value = attr.getNodeValue();

                // Check for Nulls
                if (typeName.endsWith("Null") && value.equals("null") && typeName.length() > 4) {
                    typeName = typeName.substring(0, typeName.length() - 4);
                    value = null;
                }

                WspConstants.TypeMapping tm = WspConstants.findTypeMappingByTypeName(typeName);
                if (tm == null)
                    throw new InvalidPolicyStreamException("Policy contains unrecognized type name \"" + kidNode.getNodeName() + "\"");

                try {
                    //log.info("Thawing: " + name + ".set" + parm + "((" + tm.type + ") " + value + ")");
                    Method setter = assertion.getClass().getMethod("set" + parm, new Class[] { tm.type });
                    tm.thawer.thaw(assertion, setter, value);
                } catch (NoSuchMethodException e) {
                    throw new InvalidPolicyStreamException("Policy contains reference to unsupported assertion property " + parm, e);
                } catch (SecurityException e) {
                    throw new InvalidPolicyStreamException("Policy contains reference to unsupported assertion property " + parm, e);
                } catch (InvocationTargetException e) {
                    throw new InvalidPolicyStreamException("Policy contains reference to unsupported assertion property " + parm, e);
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
