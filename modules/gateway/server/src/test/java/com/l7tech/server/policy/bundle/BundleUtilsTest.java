package com.l7tech.server.policy.bundle;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.policy.bundle.BundleInfo;
import com.l7tech.util.IOUtils;
import com.l7tech.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.ByteArrayInputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

public class BundleUtilsTest {
    static List<Pair<BundleInfo,String>> bundleInfos;

    @Before
    public void loadBundleInfos() throws Exception {
        if (bundleInfos == null) {
            bundleInfos = BundleUtils.getBundleInfos(getClass(), "/com/l7tech/server/policy/bundle/bundles");
        }
    }

    @Test
    public void testGetBundleInfos() throws Exception {
        assertNotNull(bundleInfos);
        assertEquals("Incorrect number of bundles found. Check if only 2 test bundles exist", 2, bundleInfos.size());
        // for (Pair<BundleInfo, String> bundleInfo : bundleInfos) {
            // System.out.println("Bundle found: " + bundleInfo.left.toString() + " Path: " + bundleInfo.right);
        // }
    }

    /**
     * Basic test coverage for all methods involved in finding JDBC references from a Service gateway mgmt enumeration element.
     */
    @Test
    public void testFindJdbcReferences() throws Exception {

        final BundleInfo bundleInfo = new BundleInfo("not used - test is hardcoded", "Test version", "Test", "Test Desc", "");
        BundleUtils.findReferences(bundleInfo, new BundleResolver() {
            @Override
            public Document getBundleItem(@NotNull String bundleId, @NotNull BundleItem bundleItem, boolean allowMissing) throws UnknownBundleException, BundleResolverException, InvalidBundleException {
                return getBundleItem(bundleId, "", bundleItem, allowMissing);
            }

            @Override
            public Document getBundleItem(@NotNull String bundleId, @NotNull String prerequisiteFolder, @NotNull BundleItem bundleItem, boolean allowMissing) throws UnknownBundleException, BundleResolverException, InvalidBundleException {
                final URL resourceUrl = getClass().getResource("/com/l7tech/server/policy/bundle/bundles/Bundle1/Service.xml");
                try {
                    final byte[] bytes = IOUtils.slurpUrl(resourceUrl);
                    return XmlUtil.parse(new ByteArrayInputStream(bytes));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            @NotNull
            @Override
            public List<BundleInfo> getResultList() {
                return Collections.emptyList();
            }
        });

        final Set<String> jdbcConns = bundleInfo.getJdbcConnectionReferences();
        assertFalse(jdbcConns.isEmpty());
        // for (String s : jdbcConns) {
            // System.out.println(s);
        // }

        final List<String> strings = new ArrayList<>(jdbcConns);
        assertTrue(strings.size() == 1);
        assertEquals("Unexpected JDBC Connection found", "OAuth", strings.get(0));
    }

    /**
     * Test extracting the PolicyDetail element from a l7:Policy gateway management element
     * @throws Exception
     */
    @Test
    public void testGetPolicyNameElement() throws Exception {
        final URL resourceUrl = getClass().getResource("/com/l7tech/server/policy/bundle/bundles/Bundle1/Policy.xml");
        final byte[] bytes = IOUtils.slurpUrl(resourceUrl);
        final Document enumPolicy = XmlUtil.parse(new ByteArrayInputStream(bytes));
        final List<Element> policyElms = GatewayManagementDocumentUtilities.getEntityElements(enumPolicy.getDocumentElement(), "Policy");
        for (Element policyElm : policyElms) {
            GatewayManagementDocumentUtilities.getPolicyDetailElement(policyElm);
            // final Element policyDetailElm = GatewayManagementDocumentUtilities.getPolicyDetailElement(policyElm);
            // System.out.println(XmlUtil.nodeToFormattedString(policyDetailElm));
            // System.out.println(GatewayManagementDocumentUtilities.getEntityName(policyDetailElm));
        }
    }

    /**
     * Test extracting the prerequisite folders
     * @throws Exception
     */
    @Test
    public void testPrerequisiteFolders() throws Exception {
        for (Pair<BundleInfo, String> bundleInfoPair : bundleInfos) {
            final String[] prerequisiteFolders = bundleInfoPair.left.getPrerequisiteFolders();
            if ("Bundle1".equals(bundleInfoPair.right)) {
                assertEquals(0, prerequisiteFolders.length);
            } else if ("Bundle2".equals(bundleInfoPair.right)) {
                assertEquals(2, prerequisiteFolders.length);
                assertEquals("Unexpected prerequisite folders, they may be incorrectly ordered.", prerequisiteFolders[0], "prerequisite_1");
                assertEquals("Unexpected prerequisite folders, they may be incorrectly ordered.", prerequisiteFolders[1], "prerequisite_2");
            }
        }
    }
}
