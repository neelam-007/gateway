package com.l7tech.policy.wsp;

import com.l7tech.common.util.XmlUtil;
import com.l7tech.policy.assertion.Assertion;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;

/**
 * Given a policy tree, emit an XML version of it.
 * User: mike
 * Date: Jun 11, 2003
 * Time: 4:06:17 PM
 */
public class WspWriter {
    private OutputStream output;
    Document document;

    /**
     * Create a skeleton of a policy DOM tree.
     *
     * @return a new Document containing an empty Policy node.
     */
    static Document createSkeleton() {
        try {
            return XmlUtil.stringToDocument("<wsp:Policy " +
                                            "xmlns:wsp=\"" + WspConstants.WSP_POLICY_NS + "\" " +
                                            "xmlns=\"" + WspConstants.L7_POLICY_NS + "\"/>");
        } catch (IOException e) {
            throw new RuntimeException(e); // can't happen
        } catch (SAXException e) {
            throw new RuntimeException(e); // can't happen
        }
    }

    private WspWriter(OutputStream output) {
        this.output = output;
        this.document = createSkeleton();
    }

    static Element toElement(Assertion assertion) throws InvalidPolicyTreeException {
        if (assertion == null)
            return null;
        Document dom = createSkeleton();
        TypeMapping tm = TypeMappingUtils.findTypeMappingByClass(assertion.getClass());
        if (tm == null)
            throw new InvalidPolicyTreeException("No TypeMapping for assertion class " + assertion.getClass());
        TypedReference ref = new TypedReference(assertion.getClass(), assertion);
        try {
            Element policyElement = tm.freeze(ref, dom.getDocumentElement());
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
     * as XML.
     * @param assertion     the policy tree to write as XML
     * @param output        the OutputStream to send it to
     * @throws IOException  if there was a problem writing to the output stream
     * @throws InvalidPolicyTreeException if there was a problem with the policy being serialized
     */
    public static void writePolicy(Assertion assertion, OutputStream output) throws IOException {
        WspWriter writer = new WspWriter(output);
        try {
            if (assertion != null) {
                TypeMapping tm = TypeMappingUtils.findTypeMappingByClass(assertion.getClass());
                if (tm == null)
                    throw new InvalidPolicyTreeException("No TypeMapping for assertion class " + assertion.getClass());
                TypedReference ref = new TypedReference(assertion.getClass(), assertion);
                tm.freeze(ref, writer.document.getDocumentElement());
            }
        } catch (StackOverflowError e) {
            throw new InvalidPolicyTreeException("Policy is too deeply nested to be processed");
        } catch (Exception e) {
            throw new InvalidPolicyTreeException("Unable to serialize this policy tree", e);
        }
        writer.writeToOutputStream();
    }

    private void writeToOutputStream() throws IOException {
        XmlUtil.nodeToFormattedOutputStream(document, output);
    }

    /**
     * Obtain the XML representation of the given policy tree.
     * @param assertion     the policy tree to examine
     * @return              a string containing XML
     */
    public static String getPolicyXml(Assertion assertion) {
        final StringWriter sw = new StringWriter();
        OutputStream swo = new OutputStream() {
            public void write(int b) throws IOException {
                sw.write(b);
            }
        };
        try {
            writePolicy(assertion, swo);
        } catch (IOException e) {
            throw new RuntimeException("Unexpected IOException writing to StringWriter", e); // shouldn't ever happen
        }
        return sw.toString();
    }
}
