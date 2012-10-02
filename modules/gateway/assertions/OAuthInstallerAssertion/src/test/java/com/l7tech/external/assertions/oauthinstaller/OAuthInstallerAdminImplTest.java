package com.l7tech.external.assertions.oauthinstaller;

import com.l7tech.policy.bundle.BundleInfo;
import com.l7tech.util.Pair;
import org.junit.Ignore;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

public class OAuthInstallerAdminImplTest {

    private final String baseName = "/com/l7tech/external/assertions/oauthinstaller/bundles/";

    @Ignore
    @Test
    public void testInstallOfAllBundleFolders() throws Exception {
//        for (String bundleName : ALL_BUNDLE_NAMES) {
//            testInstallFolders_NoneExist(bundleName);
//        }
        // OAuth_2_0
        //todo fix
//        testInstallFolders_NoneExist("ba525763-6e55-4748-9376-76055247c8b1");
    }

    @Test
    public void testListAllBundles() throws Exception {
        final OAuthInstallerAdminImpl admin = new OAuthInstallerAdminImpl(baseName);

        final List<BundleInfo> allBundles = admin.getAllOtkComponents();
        assertNotNull(allBundles);

        BundleInfo expected;

        expected = new BundleInfo("1c2a2874-df8d-4e1d-b8b0-099b576407e1", "1.0", "OAuth 1.0", "Core Services and Test Client");
        assertTrue(expected.toString(), allBundles.contains(expected));

        expected = new BundleInfo("ba525763-6e55-4748-9376-76055247c8b1", "1.0", "OAuth 2.0", "Auth Server and Test Clients");
        assertTrue(expected.toString(), allBundles.contains(expected));

        expected = new BundleInfo("f69c7d15-4999-4761-ab26-d29d58c0dd57", "1.0", "Secure Zone OVP", "OVP - OAuth Validation Point");
        assertTrue(expected.toString(), allBundles.contains(expected));

        expected = new BundleInfo("b082274b-f00e-4fbf-bbb7-395a95ca2a35", "1.0", "SecureZone Storage", "Token and Client Store");
        assertTrue(expected.toString(), allBundles.contains(expected));

        expected = new BundleInfo("a07924c0-0265-42ea-90f1-2428e31ae5ae", "1.0", "OAuth Manager", "Manager utility for Client and Token store for OAuth 1.0 and 2.0");
        assertTrue(expected.toString(), allBundles.contains(expected));
    }

    /**
     * The same policy is contained in more than one bundle. This test validates that all policies are logically
     * equivalent. If not then two policies / service could refer to the same policy with the same guid but receive
     * a different policy.
     *
     */
    @Test
    @Ignore
    public void testValidateTheSamePoliciesAreIdentical() throws Exception {
        fail("Must implement to check if all bundles with the same policy guid are logically equivalent");

        final OAuthInstallerAdminImpl admin = new OAuthInstallerAdminImpl(baseName);

        final List<BundleInfo> allBundles = admin.getAllOtkComponents();


    }

    @Test
    @Ignore
    public void testGetId() throws Exception {

    }

    @Test
    @Ignore
    public void testResponse_PermissionDenied() throws Exception {
        //todo
        fail("Implement");
    }

    @Test
    @Ignore
    public void testAllFolderIdsAreTheSame() {
        fail("Test to ensure that all bundle names contain the same folder ids");
    }

    @Test
    @Ignore
    public void testHostnamesDoNotContainTraililngSlash() throws Exception {
        fail("Test to ensure no hostnames contain a trailing slash");
    }

    @Test
    public void testGetUpdatedHostValue() throws Exception {
        String test = "https://${host_target}${request.url.path}";
        String actual = OAuthInstallerAdminImpl.getUpdatedHostValue("version1", test);
        System.out.println(actual);
        assertEquals("https://${host_target}/version1${request.url.path}", actual);

        actual = OAuthInstallerAdminImpl.getUpdatedHostValue("version1", "https://${host_target}/auth/oauth/v1/token");
        System.out.println(actual);
        assertEquals("https://${host_target}/version1/auth/oauth/v1/token", actual);

        actual = OAuthInstallerAdminImpl.getUpdatedHostValue("version1", "${host_target}");
        assertNull(actual);

        actual = OAuthInstallerAdminImpl.getUpdatedHostValue("version1", "https://${host_target}");
        assertEquals("https://${host_target}/version1", actual);
    }

    // - PRIVATE
    private final static List<Pair<BundleInfo, String>> ALL_BUNDLE_NAMES =
            Collections.unmodifiableList(
                    Arrays.asList(
                            new Pair<BundleInfo, String>(new BundleInfo("1c2a2874-df8d-4e1d-b8b0-099b576407e1", "1.0", "OAuth_1_0", "Desc"), "OAuth_1_0"),
                            new Pair<BundleInfo, String>(new BundleInfo("ba525763-6e55-4748-9376-76055247c8b1", "1.0", "OAuth_2_0", "Desc"), "OAuth_2_0"),
                            new Pair<BundleInfo, String>(new BundleInfo("f69c7d15-4999-4761-ab26-d29d58c0dd57", "1.0", "SecureZone_OVP", "Desc"), "SecureZone_OVP"),
                            new Pair<BundleInfo, String>(new BundleInfo("b082274b-f00e-4fbf-bbb7-395a95ca2a35", "1.0", "SecureZone_Storage", "Desc"), "SecureZone_Storage"),
                            new Pair<BundleInfo, String>(new BundleInfo("a07924c0-0265-42ea-90f1-2428e31ae5ae", "1.0", "StorageManager", "Desc"), "StorageManager")
                            ));

}
