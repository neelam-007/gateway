package com.l7tech.policy.wsp;

import com.l7tech.policy.assertion.Assertion;

import java.beans.XMLDecoder;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.NoSuchElementException;

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
     * @throws IOException if the stream did not contain a valid policy
     */
    public static Assertion parse(InputStream wspStream) throws IOException {
        if (wspStream == null)
            throw new IllegalArgumentException("wspStream may not be null");
        XMLDecoder decoder = new XMLDecoder(wspStream);
        try {
            return (Assertion)decoder.readObject();
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new IOException("WspReader: Unable to read a valid policy from the provided stream: " + e.toString());
        } catch (NoSuchElementException e) {
            throw new IOException("WspReader: Unable to read a valid policy from the provided stream: " + e.toString());
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
