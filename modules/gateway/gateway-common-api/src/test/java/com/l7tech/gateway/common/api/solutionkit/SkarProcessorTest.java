package com.l7tech.gateway.common.api.solutionkit;

import com.l7tech.gateway.api.Bundle;
import com.l7tech.gateway.api.EncapsulatedAssertionMO;
import com.l7tech.gateway.api.Item;
import com.l7tech.gateway.api.Mapping;
import com.l7tech.gateway.common.solutionkit.SolutionKit;
import com.l7tech.gateway.common.solutionkit.SolutionKitAdmin;
import com.l7tech.gateway.common.solutionkit.SolutionKitException;
import com.l7tech.gateway.common.solutionkit.UntrustedSolutionKitException;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionConfig;
import com.l7tech.policy.solutionkit.SolutionKitManagerCallback;
import com.l7tech.policy.solutionkit.SolutionKitManagerUi;
import com.l7tech.util.Functions;
import com.l7tech.util.Pair;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.InputStream;
import java.security.SignatureException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.l7tech.gateway.common.solutionkit.SolutionKit.*;
import static org.junit.Assert.*;

/**
 * Skar Processor Tests
 * .skar file reading i/o might make this test run relatively slow; if so use this annotation so tests only run for full build
 * // @ConditionalIgnore(condition = IgnoreOnDaily.class) //
 */
@RunWith(MockitoJUnitRunner.class)
public class SkarProcessorTest {
    private static final SolutionKitsConfig solutionKitsConfig = new SolutionKitsConfig();
    private static final SkarProcessor skarProcessor = new SkarProcessor(solutionKitsConfig);

    private static final String SAMPLE_INSTALL_BUNDLE_XML =
            "    <l7:Bundle xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\">\n" +
                    "        <l7:Mappings />\n" +
                    "    </l7:Bundle>";

    private static final String SAMPLE_UPGRADE_BUNDLE_XML  =
            "    <l7:Bundle xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\">\n" +
                    "        <l7:Mappings>\n" +
                    "            <l7:Mapping action=\"NewOrExisting\" srcId=\"f1649a0664f1ebb6235ac238a6f71a6d\" srcUri=\"https://tluong-pc.l7tech.local:8443/restman/1.0/passwords/f1649a0664f1ebb6235ac238a6f71a6d\" type=\"SECURE_PASSWORD\">\n" +
                    "            <l7:Properties>\n" +
                    "                <l7:Property key=\"FailOnNew\"><l7:BooleanValue>true</l7:BooleanValue></l7:Property>\n" +
                    "                <l7:Property key=\"SK_AllowMappingOverride\"><l7:BooleanValue>true</l7:BooleanValue></l7:Property>\n" +
                    "            </l7:Properties>\n" +
                    "        </l7:Mapping>\n" +
                    "        </l7:Mappings>\n" +
                    "    </l7:Bundle>";

    protected static final long GOID_HI_START = Long.MAX_VALUE - 1;

    // do nothing to verify signature (simulate skar is trusted)
    private static final Functions.BinaryVoidThrows<byte[], String, SignatureException> DUMMY_SIGNATURE_VERIFIER_CALLBACK =
            new Functions.BinaryVoidThrows<byte[], String, SignatureException>() {
                @Override
                public void call(byte[] digest, String signature) throws SignatureException {
                    // do nothing to verify signature (simulate skar is trusted)
                }
            };

    @BeforeClass
    public static void load() throws Exception {
        // get the input stream of a signed solution kit
        final InputStream inputStream = SkarProcessorTest.class.getResourceAsStream("com.l7tech.SimpleServiceAndOthers-1.1-signed.skar");
        Assert.assertNotNull(inputStream);
        // load the skar file
        skarProcessor.load(inputStream, DUMMY_SIGNATURE_VERIFIER_CALLBACK);
    }

    @Test
    public void loaded() throws Exception {
        // verify solution kit config populated with expected values
        final SolutionKit solutionKit = solutionKitsConfig.getLoadedSolutionKits().keySet().iterator().next();
        assertEquals("Simple Service and Other Dependencies", solutionKit.getName());
        assertEquals("33b16742-d62d-4095-8f8d-4db707e9ad52", solutionKit.getSolutionKitGuid());
        assertEquals("1.1", solutionKit.getSolutionKitVersion());
        assertEquals("This contains the simple service and other dependent entities (excluding the non-upgradable Server Module File); part of the simple Solution Kit example.", solutionKit.getProperty(SK_PROP_DESC_KEY));
        assertEquals("false", solutionKit.getProperty(SK_PROP_IS_COLLECTION_KEY));
        assertEquals("feature:FooBar", solutionKit.getProperty(SK_PROP_FEATURE_SET_KEY));
        assertEquals("2015-08-04T16:58:35.603-08:00", solutionKit.getProperty(SK_PROP_TIMESTAMP_KEY));
        final Bundle bundle = solutionKitsConfig.getLoadedSolutionKits().get(solutionKit);
        assertEquals(12, bundle.getMappings().size());
        assertEquals(10, bundle.getReferences().size());
    }

    @Test
    public void setCustomizationInstances()  throws Exception {

        // get the input stream of the Customization jar
        final InputStream inputStream = SkarProcessorTest.class.getResourceAsStream("com.l7tech.Customization.jar");
        Assert.assertNotNull(inputStream);
        try {
            //create a class loader with the jar
            SolutionKitCustomizationClassLoader classLoader = skarProcessor.getCustomizationClassLoader(inputStream);
            Assert.assertNotNull(classLoader);

            //Create a solution kit to test with
            final SolutionKit solutionKit = new SolutionKit();
            solutionKit.setGoid(new Goid(GOID_HI_START, 2));
            solutionKit.setSolutionKitVersion("1.1");
            final UUID firstChildGUID = UUID.randomUUID();
            solutionKit.setName("Customization Instance SK");
            solutionKit.setSolutionKitGuid(firstChildGUID.toString());
            solutionKit.setProperty(SolutionKit.SK_PROP_DESC_KEY, "Customization Instance SK");
            solutionKit.setProperty(SolutionKit.SK_PROP_TIMESTAMP_KEY, String.valueOf(new Date().getTime()));
            solutionKit.setProperty(SolutionKit.SK_PROP_IS_COLLECTION_KEY, String.valueOf(false));
            solutionKit.setProperty(SolutionKit.SK_PROP_CUSTOM_CALLBACK_KEY, "com.l7tech.example.solutionkit.simple.v01_01.SimpleSolutionKitManagerCallback");
            solutionKit.setProperty(SolutionKit.SK_PROP_CUSTOM_UI_KEY, "com.l7tech.example.solutionkit.simple.v01_01.console.SimpleSolutionKitManagerUi");

            //call the method we're testing to set the customization classes
            skarProcessor.setCustomizationInstances(solutionKit, classLoader);

            //check to see whether the SolutionKitManagerCallback and SolutionKitManagerUi were set
            Pair<SolutionKit, SolutionKitCustomization> customization;
            SolutionKitManagerCallback customCallback;
            SolutionKitManagerUi customUi;
            SolutionKitCustomizationClassLoader returnedClassLoader;

            customization = solutionKitsConfig.getCustomizations().get(solutionKit.getSolutionKitGuid());
            Assert.assertNotNull(customization);

            //check the classloader is the correct one
            returnedClassLoader = customization.right.getClassLoader();
            Assert.assertNotNull(returnedClassLoader);
            Assert.assertTrue(returnedClassLoader instanceof SolutionKitCustomizationClassLoader);

            //check that the customCallBack class that was loaded is the right one
            customCallback = customization.right.getCustomCallback();
            Assert.assertNotNull(customCallback);
            Assert.assertEquals(customCallback.getClass().getName(), "com.l7tech.example.solutionkit.simple.v01_01.SimpleSolutionKitManagerCallback");

            //check that the customUI class that was loaded is the right one
            customUi = customization.right.getCustomUi();
            Assert.assertNotNull(customUi);
            Assert.assertEquals(customUi.getClass().getName(), "com.l7tech.example.solutionkit.simple.v01_01.console.SimpleSolutionKitManagerUi");

        } catch (SolutionKitException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void installOrUpgrade() throws Exception {
        final SolutionKit solutionKit = solutionKitsConfig.getLoadedSolutionKits().keySet().iterator().next();

        // simulate remapping of IDs in the bundle (secure password and JDBC)
        Map<String, String> entityIdReplaceMap = new HashMap<>(1);
        entityIdReplaceMap.put("f1649a0664f1ebb6235ac238a6f71a6d", "yyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyy");
        entityIdReplaceMap.put("0567c6a8f0c4cc2c9fb331cb03b4de6f", "zzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzz");
        Map<String, Pair<SolutionKit, Map<String, String>>> resolvedEntityIds = new HashMap<>();
        resolvedEntityIds.put(solutionKit.getSolutionKitGuid(), new Pair<>(solutionKit, entityIdReplaceMap));
        solutionKitsConfig.setResolvedEntityIds(resolvedEntityIds);

        skarProcessor.installOrUpgrade(solutionKit);

        // verify secure password and JDBC were resolved via mapping targetId in the bundle
        final String bundleStr = solutionKitsConfig.getBundleAsString(solutionKit);
        assertThat(bundleStr, CoreMatchers.containsString("targetId=\"yyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyy"));
        assertThat(bundleStr, CoreMatchers.containsString("targetId=\"zzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzz"));
    }

    @Test
    public  void notAllowedEntityIdReplace() throws Exception {
        final SolutionKit solutionKit = solutionKitsConfig.getLoadedSolutionKits().keySet().iterator().next();

        // set <l7:Property key="SkmEntityIdReplaceable"> to false
        // simulating mapping not explicitly allowed as replaceable
        Bundle bundle = solutionKitsConfig.getBundle(solutionKit);
        assertNotNull(bundle);
        for (Mapping mapping : bundle.getMappings()) {
            mapping.addProperty(SolutionKitsConfig.MAPPING_PROPERTY_NAME_SK_ALLOW_MAPPING_OVERRIDE, false);
        }

        // simulate caller *trying* to remap IDs in the bundle
        Map<String, String> entityIdReplaceMap = new HashMap<>(1);
        entityIdReplaceMap.put("f1649a0664f1ebb6235ac238a6f71a6d", "www...www");
        entityIdReplaceMap.put("0567c6a8f0c4cc2c9fb331cb03b4de6f", "xxx...xxx");
        Map<String, Pair<SolutionKit, Map<String, String>>> resolvedEntityIds = new HashMap<>();
        resolvedEntityIds.put(solutionKit.getSolutionKitGuid(), new Pair<>(solutionKit, entityIdReplaceMap));
        solutionKitsConfig.setResolvedEntityIds(resolvedEntityIds);

        try {
            skarProcessor.installOrUpgrade(solutionKit);
            fail("Expected: mappings with property " + SolutionKitsConfig.MAPPING_PROPERTY_NAME_SK_ALLOW_MAPPING_OVERRIDE + " set to false.");
        } catch (SolutionKitException e) {
            assertThat(e.getMessage(), CoreMatchers.startsWith("Unable to process entity ID replace for mapping with scrId="));
        }
    }

    @Test
    public void invokeCustomCallback() throws Exception {
        final SolutionKit solutionKit = solutionKitsConfig.getLoadedSolutionKits().keySet().iterator().next();

        // expecting SimpleSolutionKit-1.1.skar to contain Customization.jar which has a custom ui, SimpleSolutionKitManagerUi
        final SolutionKitManagerUi ui = solutionKitsConfig.getCustomizations().get(solutionKit.getSolutionKitGuid()).right.getCustomUi();
        assertNotNull(ui);

        // set input value (e.g. like passing in a value from the custom ui)
        final StringBuilder inputValue = new StringBuilder().append("CUSTOMIZED!");
        ui.getContext().getKeyValues().put("MyInputTextKey", "CUSTOMIZED!");

        // expecting SimpleSolutionKit-1.1.skar to contain Customization.jar which has a custom callback, SimpleSolutionKitManagerCallback
        skarProcessor.invokeCustomCallback(solutionKit);

        // this callback is expected to prefix solution kit name with the input value
        assertThat(solutionKit.getName(), CoreMatchers.startsWith(inputValue.toString()));

        // this callback is expected to prefix encapsulated assertion description with the input value
        final Bundle restmanBundle = solutionKitsConfig.getBundle(solutionKit);
        assertNotNull(restmanBundle);
        for (Item item : restmanBundle.getReferences()) {
            if (EntityType.ENCAPSULATED_ASSERTION == EntityType.valueOf(item.getType())) {
                assertThat(((EncapsulatedAssertionMO) item.getContent()).getProperties().get(EncapsulatedAssertionConfig.PROP_DESCRIPTION), CoreMatchers.startsWith(inputValue.toString()));
            }
        }
    }

    @Test
    public void invalidLoads() throws Exception {
        // expect error message "... value cannot be empty."
        SolutionKitsConfig invalidSolutionKitsConfig = new SolutionKitsConfig();
        SkarProcessor invalidSkarProcessor = new SkarProcessor(invalidSolutionKitsConfig);
        InputStream invalidInputStream = SkarProcessorTest.class.getResourceAsStream("com.l7tech.SimpleSolutionKit-1.0-EmptyMetadataElements-signed.skar");
        Assert.assertNotNull(invalidInputStream);
        try {
            invalidSkarProcessor.load(invalidInputStream, DUMMY_SIGNATURE_VERIFIER_CALLBACK);
            fail("Expected: an invalid .skar file.");
        } catch (SolutionKitException e) {
            assertThat(e.getMessage(), CoreMatchers.containsString("value cannot be empty."));
        }

        // expect error message "Required element ... not found"
        invalidSolutionKitsConfig = new SolutionKitsConfig();
        invalidSkarProcessor = new SkarProcessor(invalidSolutionKitsConfig);
        invalidInputStream = SkarProcessorTest.class.getResourceAsStream("com.l7tech.SimpleSolutionKit-1.0-MissingMetadataElements-signed.skar");
        Assert.assertNotNull(invalidInputStream);
        try {
            invalidSkarProcessor.load(invalidInputStream, DUMMY_SIGNATURE_VERIFIER_CALLBACK);
            fail("Expected: an invalid .skar file.");
        } catch (SolutionKitException e) {
            assertThat(e.getMessage(), CoreMatchers.containsString("Required element"));
        }

        // expect error message "Missing required file ..."
        invalidSolutionKitsConfig = new SolutionKitsConfig();
        invalidSkarProcessor = new SkarProcessor(invalidSolutionKitsConfig);
        invalidInputStream = SkarProcessorTest.class.getResourceAsStream("com.l7tech.SimpleSolutionKit-1.0-MissingInstallBundle-signed.skar");
        Assert.assertNotNull(invalidInputStream);
        try {
            invalidSkarProcessor.load(invalidInputStream, DUMMY_SIGNATURE_VERIFIER_CALLBACK);
            fail("Expected: an invalid .skar file.");
        } catch (SolutionKitException e) {
            assertThat(e.getMessage(), CoreMatchers.containsString("Missing required file"));
        }
    }

    @Test
    public void unsignedSkar() throws Exception {
        // mock SolutionKitAdmin
        final SolutionKitAdmin solutionKitAdmin = Mockito.mock(SolutionKitAdmin.class);
        Mockito.doNothing().when(solutionKitAdmin).verifySkarSignature(Mockito.any(byte[].class), Mockito.anyString());

        // expect error message "... Invalid signed Zip."
        final SolutionKitsConfig solutionKitsConfig = new SolutionKitsConfig();
        final SkarProcessor skarProcessor = new SkarProcessor(solutionKitsConfig);
        final InputStream unsignedSkarStream = SkarProcessorTest.class.getResourceAsStream("com.l7tech.SimpleServiceAndOthers-1.1-unsigned.skar");
        Assert.assertNotNull(unsignedSkarStream);
        try {
            skarProcessor.load(unsignedSkarStream, DUMMY_SIGNATURE_VERIFIER_CALLBACK);
            fail("Expected: an invalid .skar file.");
        } catch (UntrustedSolutionKitException e) {
            assertThat(e.getMessage(), Matchers.containsString("Invalid signed Zip"));
        }
    }

    @Test
    public void signedSkarOfSkars() throws Exception {
        final SolutionKitsConfig solutionKitsConfig = new SolutionKitsConfig();
        final SkarProcessor skarProcessor = Mockito.spy(new SkarProcessor(solutionKitsConfig));
        final InputStream signedSkarOfSkarStream = SkarProcessorTest.class.getResourceAsStream("com.l7tech.SimpleSolutionKit-1.1-skar-of-skars-signed.skar");
        Assert.assertNotNull(signedSkarOfSkarStream);

        // load the skar-of-skars
        skarProcessor.load(signedSkarOfSkarStream, DUMMY_SIGNATURE_VERIFIER_CALLBACK);
        // verifySkarSignature is called only once
        Mockito.verify(skarProcessor, Mockito.times(1)).load(signedSkarOfSkarStream, DUMMY_SIGNATURE_VERIFIER_CALLBACK);
        // 3 times; once for the parent and twice for two children
        Mockito.verify(skarProcessor, Mockito.times(3)).loadWithoutSignatureCheck(Mockito.<InputStream>any());
    }
}