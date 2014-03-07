package com.l7tech.policy.bundle;

import org.junit.Test;

import java.util.Arrays;

import static com.l7tech.policy.bundle.BundleInfo.getPrefixedUrlErrorMsg;
import static junit.framework.Assert.*;
import static junit.framework.Assert.assertEquals;

public class BundleInfoTest {
    private final static String bundleId = "bundleId";
    private final static String bundleVersion = "bundleVersion";
    private final static String bundleName = "bundleName";
    private final static String bundleDescription = "bundleDescription";
    private final static String bundlePrerequisiteFoldersString = "prerequisite_1,prerequisite_2";
    private final static String[] bundlePrerequisiteFolders = {"prerequisite_1", "prerequisite_2"};

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

    @Test
    public void testConstructorInitialization() throws Exception {
        BundleInfo bundleInfo = new BundleInfo(bundleId, bundleVersion, bundleName, bundleDescription, bundlePrerequisiteFoldersString);
        assertEqualsBundleInfo(bundleInfo);

        bundleInfo = new BundleInfo(bundleInfo);
        assertEqualsBundleInfo(bundleInfo);

        assertEquals(new BundleInfo(bundleId, bundleVersion, bundleName, bundleDescription), new BundleInfo(bundleId, bundleVersion, bundleName, bundleDescription, ""));

    }

    private void assertEqualsBundleInfo(BundleInfo bundleInfo) {
        assertEquals(bundleId, bundleInfo.getId());
        assertEquals(bundleVersion, bundleInfo.getVersion());
        assertEquals(bundleName, bundleInfo.getName());
        assertEquals(bundleDescription, bundleInfo.getDescription());
        assertTrue(Arrays.equals(bundlePrerequisiteFolders, bundleInfo.getPrerequisiteFolders()));
    }
}
