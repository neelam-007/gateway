package com.l7tech.console.panels.solutionkit;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.api.Bundle;
import com.l7tech.gateway.api.BundleBuilder;
import com.l7tech.gateway.common.solutionkit.SolutionKitBuilder;
import com.l7tech.gateway.common.solutionkit.SolutionKitException;
import com.l7tech.gateway.common.solutionkit.SolutionKitUtils;
import com.l7tech.gateway.common.solutionkit.SolutionKit;
import org.junit.Assert;
import org.junit.Test;
import org.w3c.dom.Document;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertTrue;

/**
 * Test utility methods
 */
public class SolutionKitUtilsTest {

    @Test
    public void createDocumentAndCopyToSolutionKit() throws Exception {
        final String expectedMetadata = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<l7:SolutionKit xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\">\n" +
                "    <l7:Id>33b16742-d62d-4095-8f8d-4db707e9ad52</l7:Id>\n" +
                "    <l7:Version>1.1</l7:Version>\n" +
                "    <l7:Name>Simple Solution Kit</l7:Name>\n" +
                "    <l7:Description>This is a simple Solution Kit.</l7:Description>\n" +
                "    <l7:TimeStamp>2015-05-11T12:56:35.603-08:00</l7:TimeStamp>\n" +
                "    <l7:IsCollection>false</l7:IsCollection>\n" +
                "    <l7:FeatureSet>feature:FooBar</l7:FeatureSet>\n" +
                "    <l7:CustomCallback>com.l7tech.example.solutionkit.simple.v01_01.SimpleSolutionKitManagerCallback</l7:CustomCallback>\n" +
                "    <l7:CustomUI>com.l7tech.example.solutionkit.simple.v01_01.console.SimpleSolutionKitManagerUi</l7:CustomUI>\n" +
                "    <l7:AllowAddendum>false</l7:AllowAddendum>\n" +
                "</l7:SolutionKit>\n";

        SolutionKit original = new SolutionKit();
        original.setSolutionKitGuid("33b16742-d62d-4095-8f8d-4db707e9ad52");
        original.setSolutionKitVersion("1.1");
        original.setName("Simple Solution Kit");
        original.setProperty(SolutionKit.SK_PROP_DESC_KEY, "This is a simple Solution Kit.");
        original.setProperty(SolutionKit.SK_PROP_TIMESTAMP_KEY, "2015-05-11T12:56:35.603-08:00");
        original.setProperty(SolutionKit.SK_PROP_IS_COLLECTION_KEY, "false");
        original.setProperty(SolutionKit.SK_PROP_FEATURE_SET_KEY, "feature:FooBar");
        original.setProperty(SolutionKit.SK_PROP_CUSTOM_CALLBACK_KEY, "com.l7tech.example.solutionkit.simple.v01_01.SimpleSolutionKitManagerCallback");
        original.setProperty(SolutionKit.SK_PROP_CUSTOM_UI_KEY, "com.l7tech.example.solutionkit.simple.v01_01.console.SimpleSolutionKitManagerUi");
        original.setProperty(SolutionKit.SK_PROP_ALLOW_ADDENDUM_KEY, "false");

        // make sure document created from solution kit is the same
        Document doc = SolutionKitUtils.createDocument(original);
        Assert.assertEquals(expectedMetadata, XmlUtil.nodeToFormattedString(doc));

        // make sure original is same as version copied from xml document
        SolutionKit copy = new SolutionKit();
        SolutionKitUtils.copyDocumentToSolutionKit(doc, copy);
        Assert.assertEquals(original, copy);
    }

    @Test
    public void generateListOfDeleteBundlesSuccess() throws Exception {
        final SolutionKit solutionKitWithUninstallBundle1 = new SolutionKitBuilder()
                .name("Solution Kit with Uninstall bundle1")
                .skVersion("1")
                .skGuid("guid1")
                .uninstallBundle("test bundle1")
                .build();

        final SolutionKit solutionKitWithUninstallBundle2 = new SolutionKitBuilder()
                .name("Solution Kit with Uninstall bundle2")
                .skVersion("1")
                .skGuid("guid2")
                .uninstallBundle("test bundle2")
                .build();

        final SolutionKit parentSK = new SolutionKitBuilder()
                .name("parentSK")
                .skVersion("1")
                .addProperty(SolutionKit.SK_PROP_IS_COLLECTION_KEY, "true")
                .skGuid("parent_guid")
                .build();

        final List<SolutionKit> result = SolutionKitUtils.generateListOfDeleteBundles(Arrays.asList(solutionKitWithUninstallBundle1, solutionKitWithUninstallBundle2, parentSK));
        assertTrue("Only child solution kits added", result.size()==2);
        assertTrue("child 1 is in result", result.contains(solutionKitWithUninstallBundle1));
        assertTrue("child 2 is in result", result.contains(solutionKitWithUninstallBundle2));
    }

    @Test (expected = SolutionKitException.class)
    public void generateListOfDeleteBundlesError() throws Exception {
        //This is the bad solution kit
        final SolutionKit solutionKitWithOutUninstallBundle1 = new SolutionKitBuilder()
                .name("Solution Kit with Uninstall bundle1")
                .skVersion("1")
                .skGuid("guid1")
                .build();

        final SolutionKit solutionKitWithUninstallBundle2 = new SolutionKitBuilder()
                .name("Solution Kit with Uninstall bundle2")
                .skVersion("1")
                .skGuid("guid2")
                .uninstallBundle("test bundle2")
                .build();

        final SolutionKit parentSK = new SolutionKitBuilder()
                .name("parentSK")
                .skVersion("1")
                .addProperty(SolutionKit.SK_PROP_IS_COLLECTION_KEY, "true")
                .skGuid("parent_guid")
                .build();

        SolutionKitUtils.generateListOfDeleteBundles(Arrays.asList(solutionKitWithOutUninstallBundle1, solutionKitWithUninstallBundle2, parentSK));
    }

    @Test
    public void generateBundleListPayloadSuccess() throws IOException {
        final Bundle deleteBundle = new BundleBuilder()
                .name("Delete bundle")
                .build();

        final Bundle installBundle = new BundleBuilder()
                .name("Install bundle")
                .build();

        final String result = SolutionKitUtils.generateBundleListPayload(Collections.singletonList(deleteBundle),
                Collections.singletonList(installBundle));
        assertTrue("Contains the delete bundle",
                result.contains("<l7:Bundle><l7:name>Delete bundle</l7:name><l7:References/><l7:Mappings/></l7:Bundle>"));
        assertTrue("Contains the install bundle",
                result.contains("<l7:Bundle><l7:name>Install bundle</l7:name><l7:References/><l7:Mappings/></l7:Bundle>"));
    }
}
