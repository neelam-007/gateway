package com.l7tech.server.policy.bundle;

import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.bundle.BundleInfo;
import com.l7tech.policy.bundle.BundleMapping;
import com.l7tech.test.BugNumber;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.w3c.dom.Document;

import java.util.Arrays;
import java.util.List;

import static junit.framework.Assert.*;

public class PolicyBundleInstallerContextTest {
    @BugNumber(13287)
    @Test
    public void testEmptyPrefixIsIgnored() throws Exception {
        final BundleResolver bundleResolver = new BundleResolver() {
            @NotNull
            @Override
            public List<BundleInfo> getResultList() {
                return Arrays.asList(new BundleInfo("id", "version", "name", "desc", ""));
            }

            @Override
            public Document getBundleItem(@NotNull String bundleId, @NotNull BundleItem bundleItem, boolean allowMissing) throws UnknownBundleException, BundleResolverException, InvalidBundleException {
                return null;
            }

            @Override
            public Document getBundleItem(@NotNull String bundleId, @NotNull String prerequisiteFolder, @NotNull BundleItem bundleItem, boolean allowMissing) throws UnknownBundleException, BundleResolverException, InvalidBundleException {
                return null;
            }
        };
        final List<BundleInfo> resultList = bundleResolver.getResultList();
        //OAuth_1_0
        final BundleInfo bundleInfo = resultList.get(0);
        final String prefix = "    ";
        PolicyBundleInstallerContext context = new PolicyBundleInstallerContext(bundleInfo, new Goid(0,-5002), new BundleMapping(), prefix, bundleResolver, true, null);
        assertNull("Empty prefix should be ignored", context.getInstallationPrefix());
        assertTrue(context.isCheckingAssertionExistenceRequired());

        context = new PolicyBundleInstallerContext(bundleInfo, new Goid(0,-5002), new BundleMapping(), null, bundleResolver, false, null);
        assertNull("Null value should remain null", context.getInstallationPrefix());
        assertFalse(context.isCheckingAssertionExistenceRequired());

        context = new PolicyBundleInstallerContext(bundleInfo, new Goid(0,-5002), new BundleMapping(), " value with spaces  ", bundleResolver, true, null);
        assertEquals("Leading and trailing spaces should have been removed.", "value with spaces", context.getInstallationPrefix());
    }
}