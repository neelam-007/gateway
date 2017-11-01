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
import org.mockito.ArgumentCaptor;
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
    public void testInstall() throws Throwable {
        // solution kits for the test
        final int numberOfSolutionKits = 2;
        final Set<SolutionKit> selectedSolutionKits = new HashSet<>(numberOfSolutionKits);
        final SolutionKit solutionKit1 = new SolutionKitBuilder()
                .name("SK1").build();
        selectedSolutionKits.add(solutionKit1);
        final SolutionKit solutionKit2 = new SolutionKitBuilder()
                .name("SK2").build();
        selectedSolutionKits.add(solutionKit2);
        when(solutionKitsConfig.getSelectedSolutionKits()).thenReturn(selectedSolutionKits);

        // install
        final AtomicBoolean doTestInstallExecuted = new AtomicBoolean(false);
        solutionKitProcessor.testInstall(new Functions.UnaryVoidThrows<Triple<SolutionKit, String, Boolean>, Throwable>() {
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
    }

    @Test
    public void testInstallParentWithDifferentMetadataError() throws Throwable {
        // test install a parent with different metadata than one that already exists in the database. Occurs when users
        // want to install one child solution kit at a time. If the meta data is different then return error.

        // parent skar loaded
        final SolutionKit parentSolutionKitLoaded = new SolutionKitBuilder()
                .name("ParentSK")
                .skGuid("SameGuid")
                .skVersion("2.0")
                .addProperty(SolutionKit.SK_PROP_TIMESTAMP_KEY, "testStamp")
                .addProperty(SolutionKit.SK_PROP_DESC_KEY, "test")
                .addProperty(SolutionKit.SK_PROP_IS_COLLECTION_KEY, "true")
                .addProperty(SolutionKit.SK_PROP_FEATURE_SET_KEY, "testFeature")
                .addProperty(SolutionKit.SK_PROP_ALLOW_ADDENDUM_KEY, "false")
                .addProperty(SolutionKit.SK_PROP_CUSTOM_UI_KEY, "test.java")
                .addProperty(SolutionKit.SK_PROP_CUSTOM_CALLBACK_KEY, "test.java")
                .goid(new Goid(0, 1))
                .build();
        when(solutionKitsConfig.getParentSolutionKitLoaded()).thenReturn(parentSolutionKitLoaded);

        // parent skar from DB (Different version)
        final SolutionKit parentSolutionKitFromDb = new SolutionKitBuilder()
                .name("ParentSK")
                .skGuid("SameGuid")
                .skVersion("1.0")
                .addProperty(SolutionKit.SK_PROP_TIMESTAMP_KEY, "testStamp")
                .addProperty(SolutionKit.SK_PROP_DESC_KEY, "test")
                .addProperty(SolutionKit.SK_PROP_IS_COLLECTION_KEY, "true")
                .addProperty(SolutionKit.SK_PROP_FEATURE_SET_KEY, "testFeature")
                .addProperty(SolutionKit.SK_PROP_ALLOW_ADDENDUM_KEY, "false")
                .addProperty(SolutionKit.SK_PROP_CUSTOM_CALLBACK_KEY, "test.java")
                .addProperty(SolutionKit.SK_PROP_CUSTOM_UI_KEY, "test.java")
                .addProperty(SolutionKit.SK_PROP_INSTANCE_MODIFIER_KEY, "IM1")
                .goid(new Goid(0, 1))
                .build();
        when(solutionKitAdmin.get(parentSolutionKitFromDb.getSolutionKitGuid(), "IM1")).thenReturn(parentSolutionKitFromDb);

        // skar of skar for the test
        final int numberOfSolutionKits = 1;
        final Set<SolutionKit> selectedSolutionKits = new HashSet<>(numberOfSolutionKits);
        final SolutionKit solutionKit1 = new SolutionKitBuilder()
                .name("SK1")
                .addProperty(SolutionKit.SK_PROP_INSTANCE_MODIFIER_KEY, "IM1")
                .build();
        selectedSolutionKits.add(solutionKit1);
        when(solutionKitsConfig.getSelectedSolutionKits()).thenReturn(selectedSolutionKits);
        when(solutionKitsConfig.isUpgrade()).thenReturn(false);

        // test install, should fail because the metadata for loaded + solution kit on db have different metadata
        try {
            solutionKitProcessor.testInstall(new Functions.UnaryVoidThrows<Triple<SolutionKit, String, Boolean>, Throwable>() {
                @Override
                public void call(Triple<SolutionKit, String, Boolean> loaded) throws Throwable {
                    //do nothing
                }
            });
            fail("Exception should've been thrown");
        } catch (SolutionKitConflictException e) {
            assertEquals("Solution kit versions are different",
                    "<html>Install failure: Install process attempts to overwrite an existing parent Solution " +
                            "Kit ('SameGuid' with instance modifier 'IM1')<br/> with a new Solution Kit that has " +
                            "different properties. Please install again with a different instance modifier.</html>",
                    e.getMessage());
        }
    }

    @Test
    public void testInstallParentWithSameMetaDataSuccess() throws Throwable {
        // test install a parent with different metadata than one that already exists in the database. Occurs when users
        // want to install one child solution kit at a time. If the meta data is same, then proceed.

        // parent skar loaded
        final SolutionKit parentSolutionKitLoaded = new SolutionKitBuilder()
                .name("ParentSK")
                .skGuid("SameGuid")
                .skVersion("1.0")
                .addProperty(SolutionKit.SK_PROP_TIMESTAMP_KEY, "testStamp")
                .addProperty(SolutionKit.SK_PROP_DESC_KEY, "test")
                .addProperty(SolutionKit.SK_PROP_IS_COLLECTION_KEY, "true")
                .addProperty(SolutionKit.SK_PROP_FEATURE_SET_KEY, "testFeature")
                .addProperty(SolutionKit.SK_PROP_ALLOW_ADDENDUM_KEY, "false")
                .addProperty(SolutionKit.SK_PROP_CUSTOM_CALLBACK_KEY, "test.java")
                .addProperty(SolutionKit.SK_PROP_CUSTOM_UI_KEY, "test.java")
                .goid(new Goid(0, 1))
                .build();
        when(solutionKitsConfig.getParentSolutionKitLoaded()).thenReturn(parentSolutionKitLoaded);

        // parent skar from DB (Different version)
        final SolutionKit parentSolutionKitFromDb = new SolutionKitBuilder()
                .name("ParentSK")
                .skGuid("SameGuid")
                .skVersion("1.0")
                .addProperty(SolutionKit.SK_PROP_TIMESTAMP_KEY, "testStamp")
                .addProperty(SolutionKit.SK_PROP_DESC_KEY, "test")
                .addProperty(SolutionKit.SK_PROP_IS_COLLECTION_KEY, "true")
                .addProperty(SolutionKit.SK_PROP_FEATURE_SET_KEY, "testFeature")
                .addProperty(SolutionKit.SK_PROP_ALLOW_ADDENDUM_KEY, "false")
                .addProperty(SolutionKit.SK_PROP_CUSTOM_CALLBACK_KEY, "test.java")
                .addProperty(SolutionKit.SK_PROP_CUSTOM_UI_KEY, "test.java")
                .addProperty(SolutionKit.SK_PROP_INSTANCE_MODIFIER_KEY, "IM1")
                .goid(new Goid(0, 1))
                .build();
        when(solutionKitAdmin.get(parentSolutionKitFromDb.getSolutionKitGuid(), "IM1")).thenReturn(parentSolutionKitFromDb);

        // skar of skar for the test
        final int numberOfSolutionKits = 1;
        final Set<SolutionKit> selectedSolutionKits = new HashSet<>(numberOfSolutionKits);
        final SolutionKit solutionKit1 = new SolutionKitBuilder()
                .name("SK1")
                .addProperty(SolutionKit.SK_PROP_INSTANCE_MODIFIER_KEY, "IM1")
                .build();
        selectedSolutionKits.add(solutionKit1);
        when(solutionKitsConfig.getSelectedSolutionKits()).thenReturn(selectedSolutionKits);
        when(solutionKitsConfig.isUpgrade()).thenReturn(false);

        // install or upgrade
        final AtomicBoolean doTestInstallExecuted = new AtomicBoolean(false);
        solutionKitProcessor.testInstall(new Functions.UnaryVoidThrows<Triple<SolutionKit, String, Boolean>, Throwable>() {
            @Override
            public void call(Triple<SolutionKit, String, Boolean> loaded) throws Throwable {
                doTestInstallExecuted.set(true);
            }
        });

        assertTrue("Validation successful, new child can be installed under the same parent", doTestInstallExecuted.get());
    }

    @Test
    public void install() throws Exception {
        // solution kits for the test
        final int numberOfSolutionKits = 2;
        final Set<SolutionKit> selectedSolutionKits = new HashSet<>(numberOfSolutionKits);
        final SolutionKit solutionKit1 = new SolutionKitBuilder()
                .name("SK1").build();
        selectedSolutionKits.add(solutionKit1);
        final SolutionKit solutionKit2 = new SolutionKitBuilder()
                .name("SK2").build();
        selectedSolutionKits.add(solutionKit2);

        when(solutionKitsConfig.getSelectedSolutionKits()).thenReturn(selectedSolutionKits);

        solutionKitProcessor.install(null, null);

        // Make sure children are installed
        verifyChildrenInstalled(solutionKit1, solutionKit2);

        // make sure solutionKitAdmin.install() called
        verify(solutionKitAdmin, times(numberOfSolutionKits)).install(any(SolutionKit.class), anyString(), anyBoolean());

        // test doAsyncInstall was executed (when provided)
        final AtomicBoolean doAsyncInstallExecuted = new AtomicBoolean(false);
        solutionKitProcessor.install(null, new Functions.UnaryVoidThrows<Triple<SolutionKit, String, Boolean>, Exception>() {
            @Override
            public void call(Triple<SolutionKit, String, Boolean> loaded) throws Exception {
                doAsyncInstallExecuted.set(true);
            }
        });
        assertTrue("Expected install() to have executed the doAsyncInstall code.", doAsyncInstallExecuted.get());

        // test solutionKitAdmin.install() throws exception
        when(solutionKitAdmin.install(any(SolutionKit.class), anyString(), anyBoolean())).thenThrow(new Exception());
        try {
            solutionKitProcessor.install(null, null);
            fail("Expected installOrUpgrade(...) to throw Exception.");
        } catch (Exception e) {
            // do nothing, expected
        }

        // test exceptions can be returned as an error list (when provided)
        final List<Pair<String, SolutionKit>> errorKitList = new ArrayList<>(2);
        try {
            solutionKitProcessor.install(errorKitList, null);
            assertEquals(2, errorKitList.size());
        } catch (Exception e) {
            fail("Expected exceptions from installOrUpgrade(...) can be returned as an error list.");
        }
    }

    @Test
    public void installWithParent() throws Exception {
        // parent skar for the test
        final SolutionKit parentSolutionKit = new SolutionKitBuilder()
                .name("ParentSK")
                .skGuid("zzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzz")
                .goid(new Goid(0, 1))
                .build();
        when(solutionKitsConfig.getParentSolutionKitLoaded()).thenReturn(parentSolutionKit);

        // skar of skar for the test
        final int numberOfSolutionKits = 2;
        final Set<SolutionKit> selectedSolutionKits = new HashSet<>(numberOfSolutionKits);
        final SolutionKit solutionKit1 = new SolutionKitBuilder()
                .name("SK1")
                .parent(parentSolutionKit)
                .build();
        selectedSolutionKits.add(solutionKit1);
        final SolutionKit solutionKit2 = new SolutionKitBuilder()
                .name("SK2")
                .parent(parentSolutionKit)
                .build();
        selectedSolutionKits.add(solutionKit2);
        when(solutionKitsConfig.getSelectedSolutionKits()).thenReturn(selectedSolutionKits);

        // test parent not yet saved on Gateway calls solutionKitAdmin.save()
        solutionKitProcessor.install(null, null);
        final ArgumentCaptor<SolutionKit> parentSKCaptor = ArgumentCaptor.forClass(SolutionKit.class);
        verify(solutionKitAdmin).save(parentSKCaptor.capture());
        assertEquals("ParentSK", parentSKCaptor.getValue().getName());

        // test parent already saved on Gateway calls solutionKitAdmin.update() - install code path
        when(solutionKitAdmin.get(parentSolutionKit.getSolutionKitGuid(), null)).thenReturn(parentSolutionKit);
        solutionKitProcessor.install(null, null);
        verify(solutionKitAdmin).update(parentSolutionKit);
    }

    @Test
    public void installChildrenWithDifferentIM() throws Exception {
        // parent skar for the test
        final SolutionKit parentSolutionKit = new SolutionKitBuilder()
                .name("ParentSK")
                .skGuid("zzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzz")
                .goid(new Goid(0, 1))
                .build();
        when(solutionKitsConfig.getParentSolutionKitLoaded()).thenReturn(parentSolutionKit);

        // skar of skar for the test
        final int numberOfSolutionKits = 2;
        final Set<SolutionKit> selectedSolutionKits = new HashSet<>(numberOfSolutionKits);
        final SolutionKit solutionKit1 = new SolutionKitBuilder()
                .name("SK1")
                .parent(parentSolutionKit)
                .addProperty(SolutionKit.SK_PROP_INSTANCE_MODIFIER_KEY, "test1")
                .build();
        selectedSolutionKits.add(solutionKit1);
        final SolutionKit solutionKit2 = new SolutionKitBuilder()
                .name("SK2")
                .parent(parentSolutionKit)
                .addProperty(SolutionKit.SK_PROP_INSTANCE_MODIFIER_KEY, "test2")
                .build();
        selectedSolutionKits.add(solutionKit2);
        when(solutionKitsConfig.getSelectedSolutionKits()).thenReturn(selectedSolutionKits);

        //test: Install should make new parents
        solutionKitProcessor.install(null, null);
        //verify that two different parents were saved, one for test1, the other for test2
        final ArgumentCaptor<SolutionKit> parentSKCaptor = ArgumentCaptor.forClass(SolutionKit.class);
        verify(solutionKitAdmin, times(2)).save(parentSKCaptor.capture());
        final List<SolutionKit> allParents = parentSKCaptor.getAllValues();
        assertEquals(2, parentSKCaptor.getAllValues().size());
        assertEquals("test2", allParents.get(0).getProperty(SolutionKit.SK_PROP_INSTANCE_MODIFIER_KEY));
        assertEquals("test1", allParents.get(1).getProperty(SolutionKit.SK_PROP_INSTANCE_MODIFIER_KEY));

        verifyChildrenInstalled(solutionKit1, solutionKit2);
        // make sure solutionKitAdmin.install() called
        verify(solutionKitAdmin, times(numberOfSolutionKits)).install(any(SolutionKit.class), anyString(), anyBoolean());
    }

    @Test
    public void upgradeChildrenWithSameIM() throws Exception {
        // parent skar for the test
        final SolutionKit parentSolutionKit = new SolutionKitBuilder()
                .name("ParentSK")
                .skGuid("zzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzz")
                .addProperty(SolutionKit.SK_PROP_INSTANCE_MODIFIER_KEY, "same")
                .goid(new Goid(0, 1))
                .build();
        when(solutionKitsConfig.getParentSolutionKitLoaded()).thenReturn(parentSolutionKit);

        // skar of skar for the test
        final int numberOfSolutionKits = 2;
        final Set<SolutionKit> selectedSolutionKits = new HashSet<>(numberOfSolutionKits);
        final SolutionKit solutionKit1 = new SolutionKitBuilder()
                .name("SK1")
                .skVersion("1")
                .addProperty(SolutionKit.SK_PROP_INSTANCE_MODIFIER_KEY, "same")
                .build();
        selectedSolutionKits.add(solutionKit1);
        final SolutionKit solutionKit2 = new SolutionKitBuilder()
                .name("SK2")
                .skVersion("1")
                .addProperty(SolutionKit.SK_PROP_INSTANCE_MODIFIER_KEY, "same")
                .build();
        selectedSolutionKits.add(solutionKit2);
        when(solutionKitsConfig.getSelectedSolutionKits()).thenReturn(selectedSolutionKits);

        // test parent solution kit was updated once
        when(solutionKitsConfig.isUpgrade()).thenReturn(true);
        when(solutionKitsConfig.getSolutionKitToUpgrade(parentSolutionKit.getSolutionKitGuid())).thenReturn(parentSolutionKit);
        doReturn(null).when(solutionKitProcessor).collectSolutionKitInformation(anyListOf(SolutionKit.class));

        solutionKitProcessor.upgrade(null);

        //verify instance modifier "same" is updated once
        final ArgumentCaptor<SolutionKit> updateParentCaptor = ArgumentCaptor.forClass(SolutionKit.class);
        verify(solutionKitAdmin).update(updateParentCaptor.capture());
        assertEquals("same", updateParentCaptor.getValue().getProperty(SolutionKit.SK_PROP_INSTANCE_MODIFIER_KEY));
        verifyChildrenInstalled(solutionKit1, solutionKit2);
    }

    @Test
    public void upgradeParentIMToDifferentIMError() throws Exception {
        // parent skar for the test
        final SolutionKit parentSolutionKit = new SolutionKitBuilder()
                .name("ParentSK")
                .skGuid("zzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzz")
                .addProperty(SolutionKit.SK_PROP_INSTANCE_MODIFIER_KEY, "same")
                .goid(new Goid(0, 1))
                .build();
        when(solutionKitsConfig.getParentSolutionKitLoaded()).thenReturn(parentSolutionKit);

        // skar of skar for the test
        final int numberOfSolutionKits = 1;
        final Set<SolutionKit> selectedSolutionKits = new HashSet<>(numberOfSolutionKits);
        final SolutionKit solutionKit1 = new SolutionKitBuilder()
                .name("SK1")
                .parent(parentSolutionKit)
                .addProperty(SolutionKit.SK_PROP_INSTANCE_MODIFIER_KEY, "different")
                .build();
        selectedSolutionKits.add(solutionKit1);
        when(solutionKitsConfig.getSelectedSolutionKits()).thenReturn(selectedSolutionKits);
        when(solutionKitsConfig.isUpgrade()).thenReturn(true);
        when(solutionKitsConfig.getSolutionKitToUpgrade(parentSolutionKit.getSolutionKitGuid())).thenReturn(parentSolutionKit);

        try {
            solutionKitProcessor.upgrade(null);
            fail("Exception should've been thrown");
        } catch (SolutionKitException e) {
            assertEquals(e.getMessage(), "Unable to change the instance modifier on upgrade. Please install the Solution Kit and specify a unique instance modifier instead.");
        }
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

    private void verifyChildrenInstalled(final SolutionKit solutionKit1,
                                         final SolutionKit solutionKit2) throws Exception {
        // test children are installed
        // make sure setMappingTargetIdsFromResolvedIds() called
        verify(solutionKitsConfig).setMappingTargetIdsFromResolvedIds(solutionKit1);
        verify(solutionKitsConfig).setMappingTargetIdsFromResolvedIds(solutionKit2);
    }

}