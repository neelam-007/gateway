package com.l7tech.policy.wsp;

import com.l7tech.policy.assertion.Assertion;

import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.beans.XMLDecoder;

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
     * @return          the policy tree it contained, or null
     * @throws ArrayIndexOutOfBoundsException if the stream contains no objects (or no more objects)
     */
    public static Assertion parse(InputStream wspStream) throws ArrayIndexOutOfBoundsException {
        XMLDecoder decoder = new XMLDecoder(wspStream);
        return (Assertion)decoder.readObject();
    }

    /**
     * Converts an XML-encoded policy document into the corresponding policy tree.
     * @param wspXml    the document to parse
     * @return          the policy tree it contained, or null
     * @throws ArrayIndexOutOfBoundsException if the string contains no objects
     */
    public static Assertion parse(String wspXml) throws ArrayIndexOutOfBoundsException {
        return parse(new ByteArrayInputStream(wspXml.getBytes()));
    }
}
