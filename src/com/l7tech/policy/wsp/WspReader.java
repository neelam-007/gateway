package com.l7tech.policy.wsp;

import com.l7tech.policy.assertion.Assertion;
import org.apache.log4j.Category;

import java.io.InputStream;
import java.beans.XMLDecoder;

/**
 * Build a policy tree from a WSP document.
 * User: mike
 * Date: Jun 10, 2003
 * Time: 3:44:19 PM
 */
public class WspReader {
    private static final Category log = Category.getInstance(WspReader.class);

    public static Assertion parse(InputStream wspStream) {
        XMLDecoder decoder = new XMLDecoder(wspStream);
        return (Assertion)decoder.readObject();
    }
}
