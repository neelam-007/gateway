package com.l7tech.policy.bundle;

import org.junit.Test;

import static com.l7tech.policy.bundle.BundleInfo.getPrefixedUrlErrorMsg;
import static junit.framework.Assert.*;

public class BundleInfoTest {
    @Test
    public void testGetPrefixedUrlErrorMsg() throws Exception {

        // characters which are a problem for an XML document
        String [] invalidChars = new String[]{"\"", "&", "'", "<", ">"};
        for (String invalidChar : invalidChars) {
            String prefix = "pre" + invalidChar + "fix";
            assertEquals("Invalid character '" + invalidChar + "' is not allowed in the installation prefix.",
                    getPrefixedUrlErrorMsg(prefix));
        }

        // characters which are a problem in a URL
        final String prefix = "prefix with spaces";
        assertEquals("Invalid prefix '" + prefix + "'. It must be possible to construct a valid routing URI using the prefix.",
                getPrefixedUrlErrorMsg(prefix));

        assertNull(getPrefixedUrlErrorMsg("validprefix"));
    }
}
