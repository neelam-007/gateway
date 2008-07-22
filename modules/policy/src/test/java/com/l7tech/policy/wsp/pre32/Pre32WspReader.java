package com.l7tech.policy.wsp.pre32;

import com.l7tech.util.DomUtils;
import com.l7tech.util.TooManyChildElementsException;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.policy.assertion.Assertion;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.logging.Logger;

/**
 * Old version of the code.  Do not change this -- it is here to behave the same way pre-3.1 versions of the parser did,
 * for backward compat unit tests.
 */
public class Pre32WspReader {
    private static final Logger log = Logger.getLogger(Pre32WspReader.class.getName());

    private Pre32WspReader() {
    }

    /**
     * Attempt to locate the Policy element.  It may be the case that rootElement already _is_ the
     * Policy element, or that the rootElement is exp:Export and it contains the Policy element.
     * This does not actually check the version or namespace of the returned policy element.
     *
     * @param rootElement the root element to check
     * @return a Policy element.  Never null.
     * @throws Pre32InvalidPolicyStreamException if a Policy element cannot be located.
     */
    public static Element findPolicyElement(Element rootElement) throws Pre32InvalidPolicyStreamException {
        if ("Policy".equals(rootElement.getLocalName()))
            return rootElement;
        try {
            Element n = DomUtils.findOnlyOneChildElementByName(rootElement, "http://schemas.xmlsoap.org/ws/2002/12/policy", "Policy");
            if (n != null)
                return n;
            n = DomUtils.findOnlyOneChildElementByName(rootElement, "http://www.layer7tech.com/ws/policy", "Policy");
            if (n != null)
                return n;
        } catch (TooManyChildElementsException e1) {
            throw new Pre32InvalidPolicyStreamException("More than one Policy element found", e1);
        }
        throw new Pre32InvalidPolicyStreamException("Unable to locate Policy element");
    }

    /**
     * Reads an XML-encoded policy document from the given element and
     * returns the corresponding policy tree.  Unrecognized chunks of XML will be preserved inside
     * {@link com.l7tech.policy.assertion.UnknownAssertion} instances, and processing will try to continue.
     *
     * @param policyElement an element of type Pre32WspConstants.L7_POLICY_NS_CURRENT:Pre32WspConstants.POLICY_ELNAME
     * @return the policy tree it contained.  A null return means a valid &lt;Policy/&gt; tag was present, but
     *         it was empty.
     * @throws Pre32InvalidPolicyStreamException if the stream did not contain a valid policy
     */
    public static Assertion parsePermissively(Element policyElement) throws Pre32InvalidPolicyStreamException {
        return parse(findPolicyElement(policyElement), new Pre32WspVisitor());
    }

    static Assertion parse(Element policyElement, Pre32WspVisitor visitor) throws Pre32InvalidPolicyStreamException {
        List childElements = Pre32TypeMappingUtils.getChildElements(policyElement);
        if (childElements.isEmpty())
            return null; // Empty Policy tag explicitly means a null policy

        if (childElements.size() != 1)
            throw new Pre32InvalidPolicyStreamException("Policy does not have exactly zero or one immediate child");
        Object target = Pre32TypeMappingUtils.thawElement((Element) childElements.get(0), visitor).target;
        if (!(target instanceof Assertion))
            throw new Pre32InvalidPolicyStreamException("Policy does not have an assertion as its immediate child");
        Assertion root = (Assertion) target;
        if (root != null)
            root.treeChanged();        
        return root;
    }

    static Assertion parse(InputStream wspStream, Pre32WspVisitor visitor) throws IOException {
        try {
            Document doc = XmlUtil.parse(wspStream);
            Element root = findPolicyElement(doc.getDocumentElement());
            if (!Pre32WspConstants.POLICY_ELNAME.equals(root.getLocalName()))
                throw new Pre32InvalidPolicyStreamException("Document element local name is not Policy");
            String rootNs = root.getNamespaceURI();
            if (!Pre32WspConstants.isRecognizedPolicyNsUri(rootNs))
                throw new Pre32InvalidPolicyStreamException("Document element is not in a recognized namespace");
            return parse(root, visitor);
        } catch (Exception e) {
            throw new Pre32InvalidPolicyStreamException("Unable to parse policy", e);
        }
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
        return parse(wspXml, new Pre32WspVisitor());
    }

    static Assertion parse(String wspXml, Pre32WspVisitor visitor) throws IOException {
        if (wspXml == null)
            throw new IllegalArgumentException("wspXml may not be null");
        return parse(new ByteArrayInputStream(wspXml.getBytes()), visitor);
    }

}
