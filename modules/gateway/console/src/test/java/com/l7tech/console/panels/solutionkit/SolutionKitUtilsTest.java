package com.l7tech.console.panels.solutionkit;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.common.solutionkit.SolutionKit;
import org.junit.Assert;
import org.junit.Test;
import org.w3c.dom.Document;

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
                "    <l7:FeatureSet>feature:FooBar</l7:FeatureSet>\n" +
                "    <l7:CustomCallback>com.l7tech.example.solutionkit.simple.v01_01.SimpleSolutionKitManagerCallback</l7:CustomCallback>\n" +
                "    <l7:CustomUI>com.l7tech.example.solutionkit.simple.v01_01.console.SimpleSolutionKitManagerUi</l7:CustomUI>\n" +
                "</l7:SolutionKit>\n";

        SolutionKit original = new SolutionKit();
        original.setSolutionKitGuid("33b16742-d62d-4095-8f8d-4db707e9ad52");
        original.setSolutionKitVersion("1.1");
        original.setName("Simple Solution Kit");
        original.setProperty(SolutionKit.SK_PROP_DESC_KEY, "This is a simple Solution Kit.");
        original.setProperty(SolutionKit.SK_PROP_TIMESTAMP_KEY, "2015-05-11T12:56:35.603-08:00");
        original.setProperty(SolutionKit.SK_PROP_FEATURE_SET_KEY, "feature:FooBar");
        original.setProperty(SolutionKit.SK_PROP_CUSTOM_CALLBACK_KEY, "com.l7tech.example.solutionkit.simple.v01_01.SimpleSolutionKitManagerCallback");
        original.setProperty(SolutionKit.SK_PROP_CUSTOM_UI_KEY, "com.l7tech.example.solutionkit.simple.v01_01.console.SimpleSolutionKitManagerUi");

        // make sure document created from solution kit is the same
        Document doc = SolutionKitUtils.createDocument(original);
        Assert.assertEquals(expectedMetadata, XmlUtil.nodeToFormattedString(doc));

        // make sure original is same as version copied from xml document
        SolutionKit copy = new SolutionKit();
        SolutionKitUtils.copyDocumentToSolutionKit(doc, copy);
        Assert.assertEquals(original, copy);
    }
}
