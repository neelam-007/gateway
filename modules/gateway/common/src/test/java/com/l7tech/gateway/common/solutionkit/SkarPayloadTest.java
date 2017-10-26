//package com.l7tech.gateway.common.solutionkit;
//
//import com.l7tech.common.io.XmlUtil;
//import com.l7tech.gateway.api.Bundle;
//import com.l7tech.gateway.api.Mapping;
//import com.l7tech.gateway.api.impl.MarshallingUtils;
//import com.l7tech.objectmodel.Goid;
//import com.l7tech.policy.solutionkit.SolutionKitManagerCallback;
//import com.l7tech.policy.solutionkit.SolutionKitManagerUi;
//import com.l7tech.util.IOUtils;
//import com.l7tech.util.Pair;
//import org.hamcrest.CoreMatchers;
//import org.junit.Assert;
//import org.junit.BeforeClass;
//import org.junit.Test;
//import org.junit.runner.RunWith;
//import org.mockito.Mockito;
//import org.mockito.runners.MockitoJUnitRunner;
//import org.w3c.dom.Document;
//import org.w3c.dom.Element;
//import org.xml.sax.SAXException;
//
//import javax.xml.transform.dom.DOMSource;
//import java.io.ByteArrayInputStream;
//import java.io.IOException;
//import java.io.InputStream;
//import java.nio.charset.StandardCharsets;
//import java.util.Collections;
//import java.util.Date;
//import java.util.UUID;
//
//import static com.l7tech.gateway.common.solutionkit.SolutionKit.*;
//import static org.junit.Assert.*;
//
///**
// * Skar Payload Tests (previous called as Skar Processor Tests)
// * .skar file reading i/o might make this test run relatively slow; if so use this annotation so tests only run for full build
// * // @ConditionalIgnore(condition = IgnoreOnDaily.class) //
// */
//@RunWith(MockitoJUnitRunner.class)
//public class SkarPayloadTest {
//    private static final SolutionKitsConfig solutionKitsConfig = new SolutionKitsConfig();
//    private static SkarPayload skarPayload;
//
//    protected static final long GOID_HI_START = Long.MAX_VALUE - 1;
//
//    @BeforeClass
//    public static void load() throws Exception {
//        // get the input stream of a signed solution kit
//        final InputStream inputStream = SkarPayloadTest.class.getResourceAsStream("com.l7tech.SimpleServiceAndOthers-1.1.skar");
//        Assert.assertNotNull(inputStream);
//        // load the skar file
//        skarPayload = new UnsignedSkarPayloadStub(solutionKitsConfig, inputStream);
//        skarPayload.process();
//    }
//
//    @Test
//    public void loaded() throws Exception {
//        // verify solution kit config populated with expected values
//        final SolutionKit solutionKit = solutionKitsConfig.getLoadedSolutionKits().keySet().iterator().next();
//        assertEquals("Simple Service and Other Dependencies", solutionKit.getName());
//        assertEquals("33b16742-d62d-4095-8f8d-4db707e9ad52", solutionKit.getSolutionKitGuid());
//        assertEquals("1.1", solutionKit.getSolutionKitVersion());
//        assertEquals("This contains the simple service and other dependent entities (excluding the non-upgradable Server Module File); part of the simple Solution Kit example.", solutionKit.getProperty(SK_PROP_DESC_KEY));
//        assertEquals("false", solutionKit.getProperty(SK_PROP_IS_COLLECTION_KEY));
//        assertEquals("feature:FooBar", solutionKit.getProperty(SK_PROP_FEATURE_SET_KEY));
//        assertEquals("2015-08-04T16:58:35.603-08:00", solutionKit.getProperty(SK_PROP_TIMESTAMP_KEY));
//        final Bundle bundle = solutionKitsConfig.getLoadedSolutionKits().get(solutionKit);
//        assertEquals(12, bundle.getMappings().size());
//        assertEquals(10, bundle.getReferences().size());
//    }
//
//    @Test
//    public void setCustomizationInstances()  throws Exception {
//
//        // get the input stream of the Customization jar
//        final InputStream inputStream = SkarPayloadTest.class.getResourceAsStream("com.l7tech.Customization.jar");
//        Assert.assertNotNull(inputStream);
//        try {
//            //create a class loader with the jar
//            SolutionKitCustomizationClassLoader classLoader = skarPayload.getCustomizationClassLoader(inputStream);
//            Assert.assertNotNull(classLoader);
//
//            //Create a solution kit to test with
//            final SolutionKit solutionKit = new SolutionKit();
//            solutionKit.setGoid(new Goid(GOID_HI_START, 2));
//            solutionKit.setSolutionKitVersion("1.1");
//            final UUID firstChildGUID = UUID.randomUUID();
//            solutionKit.setName("Customization Instance SK");
//            solutionKit.setSolutionKitGuid(firstChildGUID.toString());
//            solutionKit.setProperty(SolutionKit.SK_PROP_DESC_KEY, "Customization Instance SK");
//            solutionKit.setProperty(SolutionKit.SK_PROP_TIMESTAMP_KEY, String.valueOf(new Date().getTime()));
//            solutionKit.setProperty(SolutionKit.SK_PROP_IS_COLLECTION_KEY, String.valueOf(false));
//            solutionKit.setProperty(SolutionKit.SK_PROP_CUSTOM_CALLBACK_KEY, "com.l7tech.example.solutionkit.simple.v01_01.SimpleSolutionKitManagerCallback");
//            solutionKit.setProperty(SolutionKit.SK_PROP_CUSTOM_UI_KEY, "com.l7tech.example.solutionkit.simple.v01_01.console.SimpleSolutionKitManagerUi");
//
//            //call the method we're testing to set the customization classes
//            skarPayload.setCustomizationInstances(solutionKit, classLoader);
//
//            //check to see whether the SolutionKitManagerCallback and SolutionKitManagerUi were set
//            Pair<SolutionKit, SolutionKitCustomization> customization;
//            SolutionKitManagerCallback customCallback;
//            SolutionKitManagerUi customUi;
//
//            customization = solutionKitsConfig.getCustomizations().get(solutionKit.getSolutionKitGuid());
//            Assert.assertNotNull(customization);
//
//            //check class loader not null
//            Assert.assertNotNull(customization.right.getClassLoader());
//
//            //check that the customCallBack class that was loaded is the right one
//            customCallback = customization.right.getCustomCallback();
//            Assert.assertNotNull(customCallback);
//            Assert.assertEquals(customCallback.getClass().getName(), "com.l7tech.example.solutionkit.simple.v01_01.SimpleSolutionKitManagerCallback");
//
//            //check that the customUI class that was loaded is the right one
//            customUi = customization.right.getCustomUi();
//            Assert.assertNotNull(customUi);
//            Assert.assertEquals(customUi.getClass().getName(), "com.l7tech.example.solutionkit.simple.v01_01.console.SimpleSolutionKitManagerUi");
//
//        } catch (SolutionKitException e) {
//            fail(e.getMessage());
//        }
//    }
//
//    @Test
//    public void invalidLoads() throws Exception {
//        // expect error message "... value cannot be empty."
//        SolutionKitsConfig invalidSolutionKitsConfig = new SolutionKitsConfig();
//        InputStream invalidInputStream = SkarPayloadTest.class.getResourceAsStream("com.l7tech.SimpleSolutionKit-1.0-EmptyMetadataElements.skar");
//        Assert.assertNotNull(invalidInputStream);
//        SkarPayload invalidSkarPayload = new UnsignedSkarPayloadStub(invalidSolutionKitsConfig, invalidInputStream);
//        try {
//            invalidSkarPayload.process();
//            fail("Expected: an invalid .skar file.");
//        } catch (SolutionKitException e) {
//            assertThat(e.getMessage(), CoreMatchers.containsString("value cannot be empty."));
//        }
//
//        // expect error message "Required element ... not found"
//        invalidSolutionKitsConfig = new SolutionKitsConfig();
//        invalidInputStream = SkarPayloadTest.class.getResourceAsStream("com.l7tech.SimpleSolutionKit-1.0-MissingMetadataElements.skar");
//        Assert.assertNotNull(invalidInputStream);
//        invalidSkarPayload = new UnsignedSkarPayloadStub(invalidSolutionKitsConfig, invalidInputStream);
//        try {
//            invalidSkarPayload.process();
//            fail("Expected: an invalid .skar file.");
//        } catch (SolutionKitException e) {
//            assertThat(e.getMessage(), CoreMatchers.containsString("Required element"));
//        }
//
//        // expect error message "Missing required file ..."
//        invalidSolutionKitsConfig = new SolutionKitsConfig();
//        invalidInputStream = SkarPayloadTest.class.getResourceAsStream("com.l7tech.SimpleSolutionKit-1.0-MissingInstallBundle.skar");
//        Assert.assertNotNull(invalidInputStream);
//        invalidSkarPayload = new UnsignedSkarPayloadStub(invalidSolutionKitsConfig, invalidInputStream);
//        try {
//            invalidSkarPayload.process();
//            fail("Expected: an invalid .skar file.");
//        } catch (SolutionKitException e) {
//            assertThat(e.getMessage(), CoreMatchers.containsString("Missing required file"));
//        }
//    }
//
//    @Test
//    public void signedSkarOfSkars() throws Exception {
//        final SolutionKitsConfig solutionKitsConfig = new SolutionKitsConfig();
//        final InputStream signedSkarOfSkarStream = SkarPayloadTest.class.getResourceAsStream("com.l7tech.SimpleSolutionKit-1.1-skar-of-skars.skar");
//        Assert.assertNotNull(signedSkarOfSkarStream);
//        final SkarPayload skarPayload = Mockito.spy(new UnsignedSkarPayloadStub(solutionKitsConfig, signedSkarOfSkarStream));
//
//        // load the skar-of-skars
//        skarPayload.process();
//        // 3 times; once for the parent and twice for two children
//        Mockito.verify(skarPayload, Mockito.times(3)).load(Mockito.<InputStream>any());
//    }
//
//    @Test
//    public void mergeBundleEmptyUpgradeMappings() throws Exception {
//        final SolutionKit solutionKit = new SolutionKit();
//        solutionKit.setSolutionKitGuid("1f87436b-7ca5-41c8-9418-21d7a7848853");
//
//        final SolutionKitsConfig solutionKitsConfig = new SolutionKitsConfig();
//        final SkarPayload skarPayload = new UnsignedSkarPayloadStub(solutionKitsConfig, Mockito.mock(InputStream.class));
//
//        // setup install bundle
//        final Bundle installBundle = createBundle(
//                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
//                    "<l7:Bundle xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\">\n" +
//                    "    <l7:References/>\n" +   // references not used for this test
//                    "    <l7:Mappings>\n" +
//                    "        <l7:Mapping action=\"NewOrExisting\" srcId=\"f1649a0664f1ebb6235ac238a6f71b0c\" srcUri=\"https://tluong-pc.l7tech.local:8443/restman/1.0/folders/f1649a0664f1ebb6235ac238a6f71b0c\" type=\"FOLDER\"/>\n" +
//                    "        <l7:Mapping action=\"AlwaysCreateNew\" srcId=\"f1649a0664f1ebb6235ac238a6f71b61\" srcUri=\"https://tluong-pc.l7tech.local:8443/restman/1.0/policies/f1649a0664f1ebb6235ac238a6f71b61\" type=\"POLICY\"/>\n" +
//                    "        <l7:Mapping action=\"AlwaysCreateNew\" srcId=\"f1649a0664f1ebb6235ac238a6f71b89\" srcUri=\"https://tluong-pc.l7tech.local:8443/restman/1.0/encapsulatedAssertions/f1649a0664f1ebb6235ac238a6f71b89\" type=\"ENCAPSULATED_ASSERTION\"/>\n" +
//                    "    </l7:Mappings>\n" +
//                    "</l7:Bundle>");
//
//        // setup upgrade bundle; empty mappings
//        final Bundle upgradeBundle = createBundle(
//                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
//                "<l7:Bundle xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\">\n" +
//                "    <l7:References/>\n" +   // references not used for this test
//                "    <l7:Mappings/>\n" +   // empty mappings
//                "</l7:Bundle>");
//
//        final Bundle mergedBundle = skarPayload.mergeBundle(solutionKit, installBundle, upgradeBundle);
//
//        // expect a merged original bundle (i.e. same as merged bundle)
//        assertEquals(installBundle, mergedBundle);
//
//        // expect original 3 mappings
//        assertEquals(3, mergedBundle.getMappings().size());
//    }
//
//    @Test
//    public void mergeBundleChangedUpgradeMappings() throws Exception {
//        final SolutionKit solutionKit = new SolutionKit();
//        solutionKit.setSolutionKitGuid("1f87436b-7ca5-41c8-9418-21d7a7848853");
//
//        final SolutionKit solutionKitToUpgrade = new SolutionKit();
//        solutionKitToUpgrade.setSolutionKitGuid("1f87436b-7ca5-41c8-9418-21d7a7848853");
//        solutionKitToUpgrade.setGoid(new Goid("1f87436b7ca541c8941821d7a7848853"));
//        solutionKitToUpgrade.setVersion(1);
//
//        final SolutionKitsConfig solutionKitsConfig = new SolutionKitsConfig();
//        solutionKitsConfig.setSolutionKitsToUpgrade(Collections.singletonList(solutionKitToUpgrade));
//        final SkarPayload skarPayload = new UnsignedSkarPayloadStub(solutionKitsConfig, Mockito.mock(InputStream.class));
//
//        // setup install bundle
//        final Bundle installBundle = createBundle(
//                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
//                "<l7:Bundle xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\">\n" +
//                "    <l7:References/>\n" +   // references not used for this test
//                "    <l7:Mappings>\n" +
//                "        <l7:Mapping action=\"NewOrExisting\" srcId=\"f1649a0664f1ebb6235ac238a6f71b0c\" srcUri=\"https://tluong-pc.l7tech.local:8443/restman/1.0/folders/f1649a0664f1ebb6235ac238a6f71b0c\" type=\"FOLDER\"/>\n" +
//                "        <l7:Mapping action=\"AlwaysCreateNew\" srcId=\"f1649a0664f1ebb6235ac238a6f71b61\" srcUri=\"https://tluong-pc.l7tech.local:8443/restman/1.0/policies/f1649a0664f1ebb6235ac238a6f71b61\" type=\"POLICY\"/>\n" +
//                "        <l7:Mapping action=\"AlwaysCreateNew\" srcId=\"f1649a0664f1ebb6235ac238a6f71b89\" srcUri=\"https://tluong-pc.l7tech.local:8443/restman/1.0/encapsulatedAssertions/f1649a0664f1ebb6235ac238a6f71b89\" type=\"ENCAPSULATED_ASSERTION\"/>\n" +
//                "    </l7:Mappings>\n" +
//                "</l7:Bundle>");
//
//        // setup upgrade bundle
//        final Bundle upgradeBundle = createBundle(
//                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
//                "<l7:Bundle xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\">\n" +
//                "    <l7:References/>\n" +   // references not used for this test
//                "    <l7:Mappings>\n" +
//                "        <l7:Mapping action=\"NewOrExisting\" srcId=\"f1649a0664f1ebb6235ac238a6f71b0c\" srcUri=\"https://tluong-pc.l7tech.local:8443/restman/1.0/folders/f1649a0664f1ebb6235ac238a6f71b0c\" type=\"FOLDER\"/>\n" +
//                "        <l7:Mapping action=\"NewOrUpdate\" srcId=\"f1649a0664f1ebb6235ac238a6f71b61\" srcUri=\"https://tluong-pc.l7tech.local:8443/restman/1.0/policies/f1649a0664f1ebb6235ac238a6f71b61\" type=\"POLICY\"/>\n" +
//                "    </l7:Mappings>\n" +
//                "</l7:Bundle>");
//
//        final Bundle mergedBundle = skarPayload.mergeBundle(solutionKit, installBundle, upgradeBundle);
//
//        // expect a merged original bundle (i.e. same as merged bundle)
//        assertEquals(installBundle, mergedBundle);
//        assertEquals(installBundle.getMappings(), upgradeBundle.getMappings());
//
//        // expect the 2 upgrade mappings are not AlwaysCreateNew
//        assertEquals(2, mergedBundle.getMappings().size());
//        for (Mapping mapping : mergedBundle.getMappings()) {
//            assertNotEquals(Mapping.Action.AlwaysCreateNew, mapping.getAction());
//        }
//
//        //expect the original Solution Kit's goid and version has been updated
//        assertEquals(solutionKit.getGoid(), solutionKitToUpgrade.getGoid());
//        assertEquals(solutionKit.getVersion(),1);
//    }
//
//    @Test
//    public void mergeBundleDifferentSolutionKitGuid() throws Exception {
//        final SolutionKit solutionKit = new SolutionKit();
//        solutionKit.setSolutionKitGuid("1f87436b-7ca5-41c8-9418-21d7a7848853");
//
//        final SolutionKit solutionKitToUpgrade = new SolutionKit();
//        //Different guid
//        solutionKitToUpgrade.setSolutionKitGuid("1f87436b-7ca5-41c8-9418-21d7a784aafe");
//        solutionKitToUpgrade.setGoid(new Goid("1f87436b7ca541c8941821d7a7848853"));
//        solutionKitToUpgrade.setVersion(1);
//
//        final SolutionKitsConfig solutionKitsConfig = new SolutionKitsConfig();
//        solutionKitsConfig.setSolutionKitsToUpgrade(Collections.singletonList(solutionKitToUpgrade));
//        final SkarPayload skarPayload = new UnsignedSkarPayloadStub(solutionKitsConfig, Mockito.mock(InputStream.class));
//
//        // setup install bundle
//        final Bundle installBundle = createBundle(
//                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
//                        "<l7:Bundle xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\">\n" +
//                        "    <l7:References/>\n" +   // references not used for this test
//                        "    <l7:Mappings>\n" +
//                        "        <l7:Mapping action=\"AlwaysCreateNew\" srcId=\"f1649a0664f1ebb6235ac238a6f71b0c\" srcUri=\"https://tluong-pc.l7tech.local:8443/restman/1.0/folders/f1649a0664f1ebb6235ac238a6f71b0c\" type=\"FOLDER\"/>\n" +
//                        "        <l7:Mapping action=\"AlwaysCreateNew\" srcId=\"f1649a0664f1ebb6235ac238a6f71b61\" srcUri=\"https://tluong-pc.l7tech.local:8443/restman/1.0/policies/f1649a0664f1ebb6235ac238a6f71b61\" type=\"POLICY\"/>\n" +
//                        "        <l7:Mapping action=\"AlwaysCreateNew\" srcId=\"f1649a0664f1ebb6235ac238a6f71b89\" srcUri=\"https://tluong-pc.l7tech.local:8443/restman/1.0/encapsulatedAssertions/f1649a0664f1ebb6235ac238a6f71b89\" type=\"ENCAPSULATED_ASSERTION\"/>\n" +
//                        "    </l7:Mappings>\n" +
//                        "</l7:Bundle>");
//
//        // setup upgrade bundle
//        final Bundle upgradeBundle = createBundle(
//                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
//                        "<l7:Bundle xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\">\n" +
//                        "    <l7:References/>\n" +   // references not used for this test
//                        "    <l7:Mappings>\n" +
//                        "        <l7:Mapping action=\"NewOrExisting\" srcId=\"f1649a0664f1ebb6235ac238a6f71b0c\" srcUri=\"https://tluong-pc.l7tech.local:8443/restman/1.0/folders/f1649a0664f1ebb6235ac238a6f71b0c\" type=\"FOLDER\"/>\n" +
//                        "        <l7:Mapping action=\"NewOrUpdate\" srcId=\"f1649a0664f1ebb6235ac238a6f71b61\" srcUri=\"https://tluong-pc.l7tech.local:8443/restman/1.0/policies/f1649a0664f1ebb6235ac238a6f71b61\" type=\"POLICY\"/>\n" +
//                        "    </l7:Mappings>\n" +
//                        "</l7:Bundle>");
//
//        final Bundle mergedBundle = skarPayload.mergeBundle(solutionKit, installBundle, upgradeBundle);
//
//        // expect the install and merged bundle to be different
//        assertEquals(installBundle, mergedBundle);
//        assertNotEquals(installBundle.getMappings(), upgradeBundle.getMappings());
//
//        // expect the install bundle is unchanged, and all the mappings are still AlwaysCreateNew
//        assertEquals(3, mergedBundle.getMappings().size());
//        for (Mapping mapping : mergedBundle.getMappings()) {
//            assertEquals(Mapping.Action.AlwaysCreateNew, mapping.getAction());
//        }
//
//        //expect the original Solution Kit's goid and version has not been updated
//        assertNotEquals(solutionKit.getGoid(), solutionKitToUpgrade.getGoid());
//        assertEquals(solutionKit.getVersion(),0);
//    }
//
//    private Bundle createBundle(final String bundleStr) throws IOException, SAXException {
//        final InputStream inputStream = new ByteArrayInputStream(bundleStr.getBytes(StandardCharsets.UTF_8));
//        final DOMSource bundleSource = new DOMSource();
//        final Document bundleDoc = XmlUtil.parse(new ByteArrayInputStream(IOUtils.slurpStream(inputStream)));
//        final Element bundleEle = bundleDoc.getDocumentElement();
//        bundleSource.setNode(bundleEle);
//        return MarshallingUtils.unmarshal(Bundle.class, bundleSource, true);
//    }
//}