package com.l7tech.policy.wsp;

import com.l7tech.policy.assertion.Assertion;

import java.io.OutputStream;
import java.beans.XMLEncoder;

/**
 * Given a policy tree, emit an XML version of it.
 * User: mike
 * Date: Jun 11, 2003
 * Time: 4:06:17 PM
 */
public class WspWriter {
    public static void writePolicy(Assertion assertion, OutputStream output) {
        XMLEncoder encoder = new XMLEncoder(output);
        encoder.writeObject(assertion);
        encoder.close();
    }
}
