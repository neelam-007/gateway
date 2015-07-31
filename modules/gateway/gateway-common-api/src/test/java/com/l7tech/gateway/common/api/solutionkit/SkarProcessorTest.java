package com.l7tech.gateway.common.api.solutionkit;

import com.l7tech.gateway.api.Bundle;
import com.l7tech.gateway.common.solutionkit.SolutionKit;
import com.l7tech.gateway.common.solutionkit.SolutionKitAdmin;
import com.l7tech.gateway.common.solutionkit.SolutionKitException;
import org.hamcrest.CoreMatchers;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import static com.l7tech.gateway.common.solutionkit.SolutionKit.*;
import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Skar Processor Tests
 * .skar file reading i/o might make this test run relatively slow; if so use this annotation so tests only run for full build
 * // @ConditionalIgnore(condition = IgnoreOnDaily.class) //
 */
public class SkarProcessorTest {
    private static final SolutionKitsConfig solutionKitsConfig = new SolutionKitsConfig();
    private static final SkarProcessor skarProcessor = new SkarProcessor(solutionKitsConfig);

    @BeforeClass
    public static void load() throws Exception {
        // IDEA does not copy test .skar resource files into the output directory
        // https://www.jetbrains.com/idea/help/resource-files.html?search=Resource%20Files
        // TODO Is there a way to configure with our build target? E.g. ./build.sh idea
        // work around this by changing the suffix for now
        final InputStream inputStream = SkarProcessorTest.class.getResourceAsStream("SimpleSolutionKit-1.1.skar.xml");
        skarProcessor.load(inputStream);
    }

    @Test
    public void loaded() throws Exception {
        // verify solution kit config populated with expected values
        final SolutionKit solutionKit = solutionKitsConfig.getLoadedSolutionKits().keySet().iterator().next();
        assertEquals("Simple Solution Kit", solutionKit.getName());
        assertEquals("33b16742-d62d-4095-8f8d-4db707e9ad52", solutionKit.getSolutionKitGuid());
        assertEquals("1.1", solutionKit.getSolutionKitVersion());
        assertEquals("This is a simple Solution Kit example.", solutionKit.getProperty(SK_PROP_DESC_KEY));
        assertEquals("false", solutionKit.getProperty(SK_PROP_IS_COLLECTION_KEY));
        assertEquals("feature:FooBar", solutionKit.getProperty(SK_PROP_FEATURE_SET_KEY));
        assertEquals("2015-05-11T12:56:35.603-08:00", solutionKit.getProperty(SK_PROP_TIMESTAMP_KEY));
        final Bundle bundle = solutionKitsConfig.getLoadedSolutionKits().get(solutionKit);
        assertEquals(13, bundle.getMappings().size());
        assertEquals(11, bundle.getReferences().size());

        // TODO test skarProcessor.mergeBundle()

        // TODO test skarProcessor.setCustomizationInstances()
    }

    @Test
    public void installOrUpgrade() throws Exception {
        final SolutionKitAdmin solutionKitAdmin = Mockito.mock(SolutionKitAdmin.class);
        final SolutionKit solutionKit = solutionKitsConfig.getLoadedSolutionKits().keySet().iterator().next();

        // simulate remapping of IDs from the bundle (secure password and JDBC)
        Map<String, String> entityIdReplaceMap = new HashMap<>(1);
        entityIdReplaceMap.put("f1649a0664f1ebb6235ac238a6f71a6d", "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx");
        entityIdReplaceMap.put("0567c6a8f0c4cc2c9fb331cb03b4de6f", "zzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzz");
        Map<SolutionKit, Map<String, String>> resolvedEntityIds = new HashMap<>();
        resolvedEntityIds.put(solutionKit, entityIdReplaceMap);
        solutionKitsConfig.setResolvedEntityIds(resolvedEntityIds);

        skarProcessor.installOrUpgrade(solutionKitAdmin, solutionKit);

        // verify solutionKitAdmin.install() called
        verify(solutionKitAdmin, times(1)).install(any(SolutionKit.class), anyString(), anyBoolean());

        // verify secure password and JDBC were resolved via mapping targetId in the bundle
        final String bundle = solutionKitsConfig.getBundleAsString(solutionKit);
        assertThat(bundle, CoreMatchers.containsString("targetId=\"xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"));
        assertThat(bundle, CoreMatchers.containsString("targetId=\"zzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzz"));
    }

    @Test
    public void invokeCustomCallback() throws Exception {
        // TODO
    }

    @Test
    public void invalidLoads() throws Exception {
        // expect error message "... value cannot be empty."
        SolutionKitsConfig invalidSolutionKitsConfig = new SolutionKitsConfig();
        SkarProcessor invalidSkarProcessor = new SkarProcessor(invalidSolutionKitsConfig);
        InputStream invalidInputStream = SkarProcessorTest.class.getResourceAsStream("SimpleSolutionKit-1.0-EmptyMetadataElements.skar.xml");
        try {
            invalidSkarProcessor.load(invalidInputStream);
            fail("Expected: an invalid .skar file.");
        } catch (SolutionKitException e) {
            assertThat(e.getMessage(), CoreMatchers.containsString("value cannot be empty."));
        }

        // expect error message "Required element ... not found"
        invalidSolutionKitsConfig = new SolutionKitsConfig();
        invalidSkarProcessor = new SkarProcessor(invalidSolutionKitsConfig);
        invalidInputStream = SkarProcessorTest.class.getResourceAsStream("SimpleSolutionKit-1.0-MissingMetadataElements.skar.xml");
        try {
            invalidSkarProcessor.load(invalidInputStream);
            fail("Expected: an invalid .skar file.");
        } catch (SolutionKitException e) {
            assertThat(e.getMessage(), CoreMatchers.containsString("Required element"));
        }

        // expect error message "Missing required file ..."
        invalidSolutionKitsConfig = new SolutionKitsConfig();
        invalidSkarProcessor = new SkarProcessor(invalidSolutionKitsConfig);
        invalidInputStream = SkarProcessorTest.class.getResourceAsStream("SimpleSolutionKit-1.0-MissingInstallBundle.skar.xml");
        try {
            invalidSkarProcessor.load(invalidInputStream);
            fail("Expected: an invalid .skar file.");
        } catch (SolutionKitException e) {
            assertThat(e.getMessage(), CoreMatchers.containsString("Missing required file"));
        }
    }
}