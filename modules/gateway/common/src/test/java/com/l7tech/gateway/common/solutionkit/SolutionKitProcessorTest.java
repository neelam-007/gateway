package com.l7tech.gateway.common.solutionkit;

import com.l7tech.gateway.api.Bundle;
import com.l7tech.gateway.api.EncapsulatedAssertionMO;
import com.l7tech.gateway.api.Item;
import com.l7tech.gateway.api.Mapping;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionConfig;
import com.l7tech.policy.solutionkit.SolutionKitManagerCallback;
import com.l7tech.policy.solutionkit.SolutionKitManagerContext;
import com.l7tech.policy.solutionkit.SolutionKitManagerUi;
import com.l7tech.util.Functions;
import com.l7tech.util.Pair;
import com.l7tech.util.Triple;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import javax.swing.*;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Solution Kit Processor Tests
 */
@RunWith(MockitoJUnitRunner.class)
public class SolutionKitProcessorTest {
    private SolutionKitsConfig solutionKitsConfig;
    private SolutionKitAdmin solutionKitAdmin;
    private SolutionKitProcessor solutionKitProcessor;

    @Before
    public void before() {
        solutionKitsConfig = mock(SolutionKitsConfig.class);
        solutionKitAdmin = mock(SolutionKitAdmin.class);
        solutionKitProcessor = spy(new SolutionKitProcessor(solutionKitsConfig, solutionKitAdmin));

        when(solutionKitsConfig.getBundleAsString(any(SolutionKit.class))).thenReturn("");
    }

    @Test
    public void testInstallOrUpgrade() throws Throwable {

        // solution kits for the test
        final int numberOfSolutionKits = 2;
        final Set<SolutionKit> selectedSolutionKits = new HashSet<>(numberOfSolutionKits);
        SolutionKit solutionKit1 = new SolutionKitBuilder()
                .name("SK1").build();
        selectedSolutionKits.add(solutionKit1);
        SolutionKit solutionKit2 = new SolutionKitBuilder()
                .name("SK2").build();
        selectedSolutionKits.add(solutionKit2);
        when(solutionKitsConfig.getSelectedSolutionKits()).thenReturn(selectedSolutionKits);

        // install or upgrade
        final AtomicBoolean doTestInstallExecuted = new AtomicBoolean(false);
        solutionKitProcessor.testInstallOrUpgrade(new Functions.UnaryVoidThrows<Triple<SolutionKit, String, Boolean>, Throwable>() {
            @Override
            public void call(Triple<SolutionKit, String, Boolean> loaded) throws Throwable {
                doTestInstallExecuted.set(true);
            }
        });

        // make sure doTestInstall was executed
        assertTrue("Expected testInstallOrUpgrade() to have executed the doTestInstall code.", doTestInstallExecuted.get());

        // make sure custom callbacks are invoked
        verify(solutionKitProcessor, times(numberOfSolutionKits)).invokeCustomCallback(any(SolutionKit.class));

        // make sure setMappingTargetIdsFromResolvedIds called
        verify(solutionKitsConfig, times(numberOfSolutionKits)).setMappingTargetIdsFromResolvedIds(any(SolutionKit.class));

        // fail with BadRequestException
        when(solutionKitsConfig.isUpgrade()).thenReturn(true);
        try {
            solutionKitProcessor.testInstallOrUpgrade(new Functions.UnaryVoidThrows<Triple<SolutionKit, String, Boolean>, Throwable>() {
                @Override
                public void call(Triple<SolutionKit, String, Boolean> loaded) throws Throwable {
                    // do nothing
                }
            });
            fail("Expected upgrade with no provided upgrade information to fail with BadRequestException.");
        } catch (BadRequestException e) {
            assertThat(e.getMessage(), CoreMatchers.endsWith("cannot be used for upgrade due to that its SKAR file does not include UpgradeBundle.xml."));
        }
    }

    @Test
    public void installOrUpgrade() throws Exception {
        // solution kits for the test
        final int numberOfSolutionKits = 2;
        final Set<SolutionKit> selectedSolutionKits = new HashSet<>(numberOfSolutionKits);
        SolutionKit solutionKit1 = new SolutionKitBuilder()
                .name("SK1").build();
        selectedSolutionKits.add(solutionKit1);
        SolutionKit solutionKit2 = new SolutionKitBuilder()
                .name("SK2").build();
        selectedSolutionKits.add(solutionKit2);

        when(solutionKitsConfig.getSelectedSolutionKits()).thenReturn(selectedSolutionKits);

        solutionKitProcessor.installOrUpgrade();

        // make sure setMappingTargetIdsFromResolvedIds() called
        verify(solutionKitsConfig).setMappingTargetIdsFromResolvedIds(solutionKit1);
        verify(solutionKitsConfig).setMappingTargetIdsFromResolvedIds(solutionKit2);

        // make sure solutionKitAdmin.install() called
        verify(solutionKitAdmin, times(numberOfSolutionKits)).install(any(SolutionKit.class), anyString(), anyBoolean());

        // test doAsyncInstall was executed (when provided)
        final AtomicBoolean doAsyncInstallExecuted = new AtomicBoolean(false);
        solutionKitProcessor.installOrUpgrade(null, new Functions.UnaryVoidThrows<Triple<SolutionKit, String, Boolean>, Exception>() {
            @Override
            public void call(Triple<SolutionKit, String, Boolean> loaded) throws Exception {
                doAsyncInstallExecuted.set(true);
            }
        });
        assertTrue("Expected installOrUpgrade() to have executed the doAsyncInstall code.", doAsyncInstallExecuted.get());

        // test solutionKitAdmin.install() throws exception
        when(solutionKitAdmin.install(any(SolutionKit.class), anyString(), anyBoolean())).thenThrow(new Exception());
        try {
            solutionKitProcessor.installOrUpgrade();
            fail("Expected installOrUpgrade(...) to throw Exception.");
        } catch (Exception e) {
            // do nothing, expected
        }

        // test exceptions can be returned as an error list (when provided)
        final List<Pair<String, SolutionKit>> errorKitList = new ArrayList<>(2);
        try {
            solutionKitProcessor.installOrUpgrade(errorKitList, null);
            assertEquals(2, errorKitList.size());
        } catch (Exception e) {
            fail("Expected exceptions from installOrUpgrade(...) can be returned as an error list.");
        }
    }

    @Test
    public void installOrUpgradeWithParent() throws Exception {
        // parent skar for the test
        SolutionKit parentSolutionKit = new SolutionKitBuilder()
                .name("ParentSK")
                .skGuid("zzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzz")
                .goid(new Goid(0, 1))
                .build();
        when(solutionKitsConfig.getParentSolutionKitLoaded()).thenReturn(parentSolutionKit);

        // skar of skar for the test
        final int numberOfSolutionKits = 2;
        final Set<SolutionKit> selectedSolutionKits = new HashSet<>(numberOfSolutionKits);
        SolutionKit solutionKit1 = new SolutionKitBuilder()
                .name("SK1")
                .parent(parentSolutionKit)
                .build();
        selectedSolutionKits.add(solutionKit1);
        SolutionKit solutionKit2 = new SolutionKitBuilder()
                .name("SK2")
                .parent(parentSolutionKit)
                .build();
        selectedSolutionKits.add(solutionKit2);
        when(solutionKitsConfig.getSelectedSolutionKits()).thenReturn(selectedSolutionKits);

        // test parent not yet saved on Gateway calls solutionKitAdmin.save()
        solutionKitProcessor.installOrUpgrade();
        verify(solutionKitAdmin).save(parentSolutionKit);

        // test parent already saved on Gateway calls solutionKitAdmin.update() - install code path
        when(solutionKitAdmin.get(parentSolutionKit.getSolutionKitGuid(), "")).thenReturn(parentSolutionKit);
        solutionKitProcessor.installOrUpgrade();
        verify(solutionKitAdmin).update(parentSolutionKit);

        // test parent already saved on Gateway calls solutionKitAdmin.update() - upgrade code path
        when(solutionKitsConfig.isUpgrade()).thenReturn(true);
        when(solutionKitsConfig.getSolutionKitToUpgrade(parentSolutionKit.getSolutionKitGuid())).thenReturn(parentSolutionKit);
        solutionKitProcessor.installOrUpgrade();
        verify(solutionKitAdmin, times(2)).update(parentSolutionKit);
    }

    @Test
    public void installChildrenWithDifferentIM() throws Exception {
        // parent skar for the test
        SolutionKit parentSolutionKit = new SolutionKitBuilder()
                .name("ParentSK")
                .skGuid("zzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzz")
                .goid(new Goid(0, 1))
                .build();
        when(solutionKitsConfig.getParentSolutionKitLoaded()).thenReturn(parentSolutionKit);

        // skar of skar for the test
        final int numberOfSolutionKits = 2;
        final Set<SolutionKit> selectedSolutionKits = new HashSet<>(numberOfSolutionKits);
        SolutionKit solutionKit1 = new SolutionKitBuilder()
                .name("SK1")
                .parent(parentSolutionKit)
                .addProperty(SolutionKit.SK_PROP_INSTANCE_MODIFIER_KEY, "test1")
                .build();
        selectedSolutionKits.add(solutionKit1);
        SolutionKit solutionKit2 = new SolutionKitBuilder()
                .name("SK2")
                .parent(parentSolutionKit)
                .addProperty(SolutionKit.SK_PROP_INSTANCE_MODIFIER_KEY, "test2")
                .build();
        selectedSolutionKits.add(solutionKit2);
        when(solutionKitsConfig.getSelectedSolutionKits()).thenReturn(selectedSolutionKits);

        //test 1: Install should make new parents
        solutionKitProcessor.installOrUpgrade();
        //verify that two parents were saved, one for test1, the other for test2
        verify(solutionKitAdmin, times(2)).save(parentSolutionKit);
    }

    @Test
    public void invokeCustomCallbackFromInputStream() throws Exception {
        final SolutionKitsConfig solutionKitsConfig = new SolutionKitsConfig();

        // get payload from test skar via input stream
        initializeSkarPayload(solutionKitsConfig);

        final SolutionKit solutionKit = solutionKitsConfig.getLoadedSolutionKits().keySet().iterator().next();

        // expecting SimpleSolutionKit-1.1.skar to contain Customization.jar which has a custom ui, SimpleSolutionKitManagerUi
        final SolutionKitManagerUi ui = solutionKitsConfig.getCustomizations().get(solutionKit.getSolutionKitGuid()).right.getCustomUi();
        assertNotNull(ui);

        // set input value (e.g. like passing in a value from the custom ui)
        final String inputValue = "CUSTOMIZED!";
        ui.getContext().getKeyValues().put("MyInputTextKey", "CUSTOMIZED!");

        // expecting SimpleSolutionKit-1.1.skar to contain Customization.jar which has a custom callback, SimpleSolutionKitManagerCallback
        solutionKitProcessor.invokeCustomCallback(solutionKit);

        // this callback is expected to prefix solution kit name with the input value
        assertThat(solutionKit.getName(), CoreMatchers.startsWith(inputValue));

        // this callback is expected to prefix encapsulated assertion description with the input value
        final Bundle restmanBundle = solutionKitsConfig.getBundle(solutionKit);
        assertNotNull(restmanBundle);
        for (Item item : restmanBundle.getReferences()) {
            if (EntityType.ENCAPSULATED_ASSERTION == EntityType.valueOf(item.getType())) {
                assertThat(((EncapsulatedAssertionMO) item.getContent()).getProperties().get(EncapsulatedAssertionConfig.PROP_DESCRIPTION), CoreMatchers.startsWith(inputValue));
            }
        }
    }

    @Test
    public void invokeCustomCallback() throws Exception {
        // setup solution kit metadata
        final SolutionKit solutionKit = new SolutionKitBuilder()
                .name("SK1")
                .skGuid("zzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzz")
                .goid(new Goid(0, 1))
                .skVersion("v1")
                .addProperty(SolutionKit.SK_PROP_DESC_KEY, "SK1 description")
                .addProperty(SolutionKit.SK_PROP_TIMESTAMP_KEY, "2016-03-24T09:08:01.603-08:00")
                .addProperty(SolutionKit.SK_PROP_IS_COLLECTION_KEY, "false")
                .build();

        // mock customizations
        Map<String, Pair<SolutionKit, SolutionKitCustomization>> customizations = mock(Map.class);
        SolutionKitCustomization customization = mock(SolutionKitCustomization.class);
        final Pair<SolutionKit, SolutionKitCustomization> customizationPair = new Pair<>(solutionKit, customization);
        SolutionKitManagerCallback customCallback = new SolutionKitManagerCallback() {
            @Override
            public void preMigrationBundleImport(SolutionKitManagerContext context) throws CallbackException {
                super.preMigrationBundleImport(context);
            }
        };
        SolutionKitManagerUi customUi = new SolutionKitManagerUi() {
            @Override
            public JButton createButton(JPanel parentPanel) {
                return null;
            }
        };
        when(solutionKitsConfig.getCustomizations()).thenReturn(customizations);
        when(customizations.get(solutionKit.getSolutionKitGuid())).thenReturn(customizationPair);
        when(customization.getCustomCallback()).thenReturn(customCallback);
        when(customization.getCustomUi()).thenReturn(customUi);
        customUi.getContext().setSolutionKitMetadata(SolutionKitUtils.createDocument(solutionKit));

        // test customization with no uninstall bundle (SSG-13239)
        solutionKit.setUninstallBundle(null);
        final SolutionKitProcessor solutionKitProcessor = new SolutionKitProcessor(solutionKitsConfig, solutionKitAdmin);
        solutionKitProcessor.invokeCustomCallback(solutionKit);
    }

    @Test
    public void entityIdReplace() throws Exception {
        final SolutionKitsConfig solutionKitsConfig = new SolutionKitsConfig();
        initializeSkarPayload(solutionKitsConfig);
        final SolutionKit solutionKit = solutionKitsConfig.getLoadedSolutionKits().keySet().iterator().next();

        // simulate remapping of IDs in the bundle (secure password and JDBC)
        Map<String, String> entityIdReplaceMap = new HashMap<>(2);
        entityIdReplaceMap.put("f1649a0664f1ebb6235ac238a6f71a6d", "yyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyy");
        entityIdReplaceMap.put("0567c6a8f0c4cc2c9fb331cb03b4de6f", "zzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzz");
        Map<String, Pair<SolutionKit, Map<String, String>>> resolvedEntityIds = new HashMap<>();
        resolvedEntityIds.put(solutionKit.getSolutionKitGuid(), new Pair<>(solutionKit, entityIdReplaceMap));
        solutionKitsConfig.setResolvedEntityIds(resolvedEntityIds);

        solutionKitsConfig.setMappingTargetIdsFromResolvedIds(solutionKit);
        solutionKitProcessor.getAsSolutionKitTriple(solutionKit);

        // verify secure password and JDBC were resolved via mapping targetId in the bundle
        final String bundleStr = solutionKitsConfig.getBundleAsString(solutionKit);
        assertThat(bundleStr, CoreMatchers.containsString("targetId=\"yyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyy"));
        assertThat(bundleStr, CoreMatchers.containsString("targetId=\"zzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzz"));
    }

    @Test
    public void entityIdReplaceNotAllowed() throws Exception {
        final SolutionKitsConfig solutionKitsConfig = new SolutionKitsConfig();
        initializeSkarPayload(solutionKitsConfig);
        final SolutionKit solutionKit = solutionKitsConfig.getLoadedSolutionKits().keySet().iterator().next();

        // set <l7:Property key="SkmEntityIdReplaceable"> to false
        // simulating mapping not explicitly allowed as replaceable
        Bundle bundle = solutionKitsConfig.getBundle(solutionKit);
        assertNotNull(bundle);
        for (Mapping mapping : bundle.getMappings()) {
            mapping.addProperty(SolutionKitsConfig.MAPPING_PROPERTY_NAME_SK_ALLOW_MAPPING_OVERRIDE, false);
        }

        // simulate caller *trying* to remap IDs in the bundle
        Map<String, String> entityIdReplaceMap = new HashMap<>(2);
        entityIdReplaceMap.put("f1649a0664f1ebb6235ac238a6f71a6d", "www...www");
        entityIdReplaceMap.put("0567c6a8f0c4cc2c9fb331cb03b4de6f", "xxx...xxx");
        Map<String, Pair<SolutionKit, Map<String, String>>> resolvedEntityIds = new HashMap<>();
        resolvedEntityIds.put(solutionKit.getSolutionKitGuid(), new Pair<>(solutionKit, entityIdReplaceMap));
        solutionKitsConfig.setResolvedEntityIds(resolvedEntityIds);

        try {
            solutionKitsConfig.setMappingTargetIdsFromResolvedIds(solutionKit);
            solutionKitProcessor.getAsSolutionKitTriple(solutionKit);
            fail("Expected: mappings with property " + SolutionKitsConfig.MAPPING_PROPERTY_NAME_SK_ALLOW_MAPPING_OVERRIDE + " set to false.");
        } catch (SolutionKitException e) {
            assertThat(e.getMessage(), CoreMatchers.startsWith("Unable to process entity ID replace for mapping with scrId="));
        }
    }

    private void initializeSkarPayload(final SolutionKitsConfig solutionKitsConfig) throws SolutionKitException {
        // get the input stream of a signed solution kit
        final InputStream inputStream = SkarPayloadTest.class.getResourceAsStream("com.l7tech.SimpleServiceAndOthers-1.1.skar");
        Assert.assertNotNull(inputStream);
        final SkarPayload skarPayload = new UnsignedSkarPayloadStub(solutionKitsConfig, inputStream);

        // load the skar file
        skarPayload.process();

        solutionKitProcessor = new SolutionKitProcessor(solutionKitsConfig, solutionKitAdmin);
    }
}