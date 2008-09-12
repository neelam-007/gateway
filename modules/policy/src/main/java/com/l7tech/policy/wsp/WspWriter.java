package com.l7tech.policy.wsp;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.policy.assertion.Assertion;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.ArrayList;

/**
 * Given a policy tree, emit an XML version of it.
 */
public class WspWriter {
    private Document document = null;
    private List<TypeMappingFinder> typeMappingFinders = new ArrayList<TypeMappingFinder>();
    private TypeMappingFinder metaTmf = new TypeMappingFinderWrapper(typeMappingFinders);

    private static ThreadLocal<WspWriter> currentWspWriter = new ThreadLocal<WspWriter>();


    static void setCurrent(WspWriter wspWriter) {
        currentWspWriter.set(wspWriter);
    }
    static WspWriter getCurrent() {
        WspWriter cur = currentWspWriter.get();
        if (cur == null)
            throw new IllegalStateException("No WspWriter currently defined for this thread");
        return cur;
    }

    /**
     * Create a new WspWriter prepared to emit a policy XML in the current (post-3.2, more WS-Policy-compliant) format.
     */
    public WspWriter() {
    }

    /**
     * Create a skeleton of a policy DOM tree.
     *
     * @return a new Document containing an empty Policy node.
     */
    Document createSkeleton() {
        try {
            String l7p = ":L7p";
            return XmlUtil.stringToDocument("<wsp:Policy " +
                                            "xmlns:wsp=\"" + WspConstants.WSP_POLICY_NS + "\" " +
                                            "xmlns" + l7p + "=\"" + WspConstants.L7_POLICY_NS + "\" " +
                                            "/>");
        } catch (SAXException e) {
            throw new RuntimeException(e); // can't happen
        }
    }

    /**
     * Convert the specified assertion into a DOM Element using the default serialization format.
     * This should not be used normally; it is only here for the benefit of the PermissiveWspVisitor, to help
     * it produce UnknownAssertion elements wrapping fragments of unrecognized XML.
     *
     * @param assertion the assertion to convert into an element.
     * @return the Element.  Never null.
     * @throws InvalidPolicyTreeException if an element cannot be created for this assertion
     */
    Element toElement(Assertion assertion) throws InvalidPolicyTreeException {
        if (assertion == null)
            return null;
        Document dom = createSkeleton();
        TypeMapping tm = TypeMappingUtils.findTypeMappingByClass(assertion.getClass(), this);
        if (tm == null)
            throw new InvalidPolicyTreeException("No TypeMapping for assertion class " + assertion.getClass());
        TypedReference ref = new TypedReference(assertion.getClass(), assertion);
        try {
            Element policyElement = tm.freeze(this, ref, dom.getDocumentElement());
            if (policyElement == null)
                throw new InvalidPolicyTreeException("Assertion did not serialize to an element"); // can't happen
            return policyElement;
        } catch (StackOverflowError e) {
            throw new InvalidPolicyTreeException("Policy is too deeply nested to be processed");
        } catch (Exception e) {
            throw new InvalidPolicyTreeException("Unable to serialize this policy tree", e);
        }
    }

    /**
     * Write the policy tree rooted at assertion to the given output stream
     * as XML, using a default WspWriter.
     * @param assertion     the policy tree to write as XML
     * @param output        the OutputStream to send it to
     * @throws IOException  if there was a problem writing to the output stream
     * @throws InvalidPolicyTreeException if there was a problem with the policy being serialized
     */
    public static void writePolicy(Assertion assertion, OutputStream output) throws IOException {
        WspWriter writer = new WspWriter();
        writer.setPolicy(assertion);
        writer.writePolicyXmlToOutputStream(output);
    }

    /**
     * Set the policy tree that we will be serializing.
     * @param assertion  the assertion to serialize.  May be null, in which case the appropriate XML will be created to reflect this.
     * @throws InvalidPolicyTreeException if this policy cannot be serialized.
     */
    public void setPolicy(Assertion assertion) {
        try {
            document = createSkeleton();
            if (assertion != null) {
                setCurrent(this);
                TypeMapping tm = TypeMappingUtils.findTypeMappingByClass(assertion.getClass(), this);
                if (tm == null)
                    throw new InvalidPolicyTreeException("No TypeMapping for assertion class " + assertion.getClass());
                TypedReference ref = new TypedReference(assertion.getClass(), assertion);
                tm.freeze(this, ref, document.getDocumentElement());
            }
        } catch (StackOverflowError e) {
            throw new InvalidPolicyTreeException("Policy is too deeply nested to be processed");
        } catch (Exception e) {
            throw new InvalidPolicyTreeException("Unable to serialize this policy tree", e);
        } finally {
            setCurrent(null);
        }
    }

    /**
     * Write the current policy out to the specific OutputStream as XML.
     * @param output the outputstream to which the policy should be writted.  Must not be null.
     * @throws IOException if there is an IOException writing to the output stream or traversing the DOM Document
     * @throws IllegalStateException if {@link #setPolicy} has not yet been called
     */
    public void writePolicyXmlToOutputStream(OutputStream output) throws IOException {
        if (document == null) throw new IllegalStateException("No policy has been set yet");
        XmlUtil.nodeToFormattedOutputStream(document, output);
    }

    /**
     * Return the current policy XML as a string.
     * @return a String containing the policy.  Never null.
     * @throws IOException if there is an IOException traversing the DOM Document
     * @throws IllegalStateException if {@link #setPolicy} has not yet been called
     */
    public String getPolicyXmlAsString() throws IOException {
        if (document == null) throw new IllegalStateException("No policy has been set yet");
        return XmlUtil.nodeToFormattedString(document);
    }

    /**
     * Obtain the XML representation of the given policy tree, using the default policy format, and using UTF-8
     * as the character encoding format.
     * @param assertion     the policy tree to examine
     * @return              a string containing XML
     */
    public static String getPolicyXml(Assertion assertion) {
        WspWriter writer = new WspWriter();
        writer.setPolicy(assertion);
        try {
            return writer.getPolicyXmlAsString();
        } catch (IOException e) {
            throw new RuntimeException("Unexpected IOException while serializing policy XML", e); // shouldn't ever happen
        }
    }

    public void addTypeMappingFinder(TypeMappingFinder typeMappingFinder) {
        if (typeMappingFinder == null)
            throw new NullPointerException("typeMappingFinder can't be null");
        typeMappingFinders.add(typeMappingFinder);
    }

    public void removeTypeMappingFinder(TypeMappingFinder typeMappingFinder) {
        typeMappingFinders.remove(typeMappingFinder);
    }

    public TypeMappingFinder getTypeMappingFinder() {
        return metaTmf;
    }
}
