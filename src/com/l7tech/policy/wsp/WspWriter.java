package com.l7tech.policy.wsp;

import com.l7tech.policy.assertion.Assertion;

import java.io.OutputStream;
import java.io.StringWriter;
import java.beans.XMLEncoder;

import org.mortbay.util.WriterOutputStream;

/**
 * Given a policy tree, emit an XML version of it.
 * User: mike
 * Date: Jun 11, 2003
 * Time: 4:06:17 PM
 */
public class WspWriter {
    private WspWriter() {
    }

    /**
     * Write the policy tree rooted at assertion to the given output stream
     * as XML.
     * @param assertion     the policy tree to write as XML
     * @param output        the OutputStream to send it to
     */
    public static void writePolicy(Assertion assertion, OutputStream output) {
        XMLEncoder encoder = new XMLEncoder(output);
        encoder.writeObject(assertion);
        encoder.close();
    }

    /**
     * Obtain the XML representation of the given policy tree.
     * @param assertion     the policy tree to examine
     * @return              a string containing XML
     */
    public static String getPolicyXml(Assertion assertion) {
        StringWriter sw = new StringWriter();
        writePolicy(assertion, new WriterOutputStream(sw));
        return sw.toString();
    }
}
