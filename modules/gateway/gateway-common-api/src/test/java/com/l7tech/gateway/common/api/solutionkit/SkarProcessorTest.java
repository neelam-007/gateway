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
import com.l7tech.objectmodel.encass.EncapsulatedAssertionConfig;
import com.l7tech.policy.solutionkit.SolutionKitManagerUi;
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
import java.util.HashMap;
import java.util.Map;

import static com.l7tech.gateway.common.api.solutionkit.SkarProcessor.MAPPING_PROPERTY_NAME_SK_ALLOW_MAPPING_OVERRIDE;
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


    @BeforeClass
    public static void load() throws Exception {
        // get the input stream of a signed solution kit
        final InputStream inputStream = SkarProcessorTest.class.getResourceAsStream("com.l7tech.SimpleServiceAndOthers-1.1-signed.skar");
        Assert.assertNotNull(inputStream);
        // load the skar file
        skarProcessor.load(inputStream);
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

        // TODO test skarProcessor.mergeBundle()

        // TODO test skarProcessor.setCustomizationInstances()
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
            mapping.addProperty(MAPPING_PROPERTY_NAME_SK_ALLOW_MAPPING_OVERRIDE, false);
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
            fail("Expected: mappings with property " + MAPPING_PROPERTY_NAME_SK_ALLOW_MAPPING_OVERRIDE + " set to false.");
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
            invalidSkarProcessor.load(invalidInputStream);
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
            invalidSkarProcessor.load(invalidInputStream);
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
            invalidSkarProcessor.load(invalidInputStream);
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
            skarProcessor.load(unsignedSkarStream);
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
        skarProcessor.load(signedSkarOfSkarStream);
        // verifySkarSignature is called only once
        Mockito.verify(skarProcessor, Mockito.times(1)).load(signedSkarOfSkarStream);
        // 3 times; once for the parent and twice for two children
        Mockito.verify(skarProcessor, Mockito.times(3)).loadWithoutSignatureCheck(Mockito.<InputStream>any());
    }
}