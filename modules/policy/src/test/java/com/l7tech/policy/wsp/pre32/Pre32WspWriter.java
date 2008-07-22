package com.l7tech.policy.wsp.pre32;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.policy.assertion.Assertion;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;

/**
 * Old version of the code.  Do not change this -- it is here to behave the same way pre-3.1 versions of the parser did,
 * for backward compat unit tests.
 */
public class Pre32WspWriter {
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
                                            "xmlns:wsp=\"" + Pre32WspConstants.WSP_POLICY_NS + "\" " +
                                            "xmlns=\"" + Pre32WspConstants.L7_POLICY_NS + "\" " +
                                            "/>");
        } catch (SAXException e) {
            throw new RuntimeException(e); // can't happen
        }
    }

    private Pre32WspWriter(OutputStream output) {
        this.output = output;
        this.document = createSkeleton();
    }

    static Element toElement(Assertion assertion) throws Pre32InvalidPolicyTreeException {
        if (assertion == null)
            return null;
        Document dom = createSkeleton();
        Pre32TypeMapping tm = Pre32TypeMappingUtils.findTypeMappingByClass(assertion.getClass());
        if (tm == null)
            throw new Pre32InvalidPolicyTreeException("No TypeMapping for assertion class " + assertion.getClass());
        Pre32TypedReference ref = new Pre32TypedReference(assertion.getClass(), assertion);
        try {
            Element policyElement = tm.freeze(ref, dom.getDocumentElement());
            if (policyElement == null)
                throw new Pre32InvalidPolicyTreeException("Assertion did not serialize to an element"); // can't happen
            return policyElement;
        } catch (StackOverflowError e) {
            throw new Pre32InvalidPolicyTreeException("Policy is too deeply nested to be processed");
        } catch (Exception e) {
            throw new Pre32InvalidPolicyTreeException("Unable to serialize this policy tree", e);
        }
    }

    /**
     * Write the policy tree rooted at assertion to the given output stream
     * as XML.
     * @param assertion     the policy tree to write as XML
     * @param output        the OutputStream to send it to
     * @throws IOException  if there was a problem writing to the output stream
     * @throws Pre32InvalidPolicyTreeException if there was a problem with the policy being serialized
     */
    public static void writePolicy(Assertion assertion, OutputStream output) throws IOException {
        Pre32WspWriter writer = new Pre32WspWriter(output);
        try {
            if (assertion != null) {
                Pre32TypeMapping tm = Pre32TypeMappingUtils.findTypeMappingByClass(assertion.getClass());
                if (tm == null)
                    throw new Pre32InvalidPolicyTreeException("No TypeMapping for assertion class " + assertion.getClass());
                Pre32TypedReference ref = new Pre32TypedReference(assertion.getClass(), assertion);
                tm.freeze(ref, writer.document.getDocumentElement());
            }
        } catch (StackOverflowError e) {
            throw new Pre32InvalidPolicyTreeException("Policy is too deeply nested to be processed");
        } catch (Exception e) {
            throw new Pre32InvalidPolicyTreeException("Unable to serialize this policy tree", e);
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
