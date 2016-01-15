package com.l7tech.gateway.common.solutionkit;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.policy.solutionkit.SolutionKitManagerCallback;
import com.l7tech.policy.solutionkit.SolutionKitManagerContext;
import com.l7tech.policy.solutionkit.SolutionKitManagerUi;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.Functions;
import com.l7tech.util.IOUtils;
import com.l7tech.util.Pair;
import com.l7tech.xml.xpath.XpathUtil;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.swing.*;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.l7tech.util.DomUtils.findExactlyOneChildElementByName;
import static com.l7tech.util.DomUtils.getTextValue;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Custom input and callback tests.
 */
@RunWith(MockitoJUnitRunner.class)
public class CustomInputAndCallbackTest {
    private static SolutionKitsConfig solutionKitsConfig;

    @BeforeClass
    public static void load() throws Exception {
        solutionKitsConfig = mock(SolutionKitsConfig.class);
    }

    @Test
    public void invokeCallbackNullContext() throws Throwable {
        final SolutionKit solutionKit = new SolutionKit();

        // null context
        SolutionKitManagerUi mockUi = mock(SolutionKitManagerUi.class);
        when(mockUi.getContext()).thenReturn(null);

        SolutionKitManagerCallback mockCallback = mock(SolutionKitManagerCallback.class);

        Map<String, Pair<SolutionKit, SolutionKitCustomization>> customizations = new HashMap<>();
        customizations.put(solutionKit.getSolutionKitGuid(), new Pair<>(solutionKit, new SolutionKitCustomization(mock(SolutionKitCustomizationClassLoader.class), mockUi, mockCallback)));
        when(solutionKitsConfig.getCustomizations()).thenReturn(customizations);

        // invoke callback, null context should not fail
        try {
            new SkarProcessor(solutionKitsConfig).invokeCustomCallback(solutionKit);
            verify(solutionKitsConfig, times(0)).getBundleAsDocument(solutionKit);
        } catch (Throwable t) {
            fail("Expected custom callback to invoke without error when context is null.");
        }
    }

    @Test
    public void invokeCallbackThrowsCallbackException() throws Exception {
        final SolutionKit solutionKit = new SolutionKit();
        SolutionKitManagerUi mockUi = mock(SolutionKitManagerUi.class);

        // throw CallbackException
        final String errorMessage = "errorMessage";
        final SolutionKitManagerCallback callback = new SolutionKitManagerCallback() {
            @Override
            public void preMigrationBundleImport(SolutionKitManagerContext context) throws CallbackException {
                throw new CallbackException(errorMessage);
            }
        };

        Map<String, Pair<SolutionKit, SolutionKitCustomization>> customizations = new HashMap<>();
        customizations.put(solutionKit.getSolutionKitGuid(), new Pair<>(solutionKit, new SolutionKitCustomization(mock(SolutionKitCustomizationClassLoader.class), mockUi, callback)));
        when(solutionKitsConfig.getCustomizations()).thenReturn(customizations);

        // invoke with CallbackException
        try {
            new SkarProcessor(solutionKitsConfig).invokeCustomCallback(solutionKit);
            fail("Expected custom callback to throw a an exception.");
        } catch (SolutionKitException e) {
            assertEquals(BadRequestException.class, e.getClass());
            assertEquals(errorMessage, e.getMessage());
        }
    }


    @Test
    public void invokeCallbackVerifyContext() throws Throwable {
        // test target ID set properly
        //    verify called solutionKitsConfig.setPreviouslyResolvedIds()
        //    verify called solutionKitsConfig.setMappingTargetIdsFromPreviouslyResolvedIds(solutionKit, solutionKitsConfig.getBundle(solutionKit))

        // make changes in callback and verify changes
    }

    private final String someGuidString = "someGuidString";
    private final String someVersionString = "someVersionString";
    private final String someNameString = "someNameString";
    private final String someDescriptionString = "someDescriptionString";
    private final String uiClassName = CustomUiTestHook.class.getName();
    private final String callbackClassName = CustomCallbackTestHook.class.getName();
    private final String installBundleStr = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<l7:Bundle xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\">\n" +
            "    <l7:References/>\n" +
            "    <l7:Mappings/>\n" +
            "</l7:Bundle>\n";
    private final String uninstallBundleStr = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<l7:Bundle xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\">\n" +
            "    <l7:Mappings/>\n" +
            "</l7:Bundle>\n";
    private final String someInstalledVersionString = "someInstalledVersionString";
    private final String someInstalledDescriptionString = "someInstalledDescriptionString";
    private final String someKey = "someKey";
    private final String someValue = "someValue";
    private static final Map<String, String> nsMap = CollectionUtils.MapBuilder.<String, String>builder()
            .put("l7", "http://ns.l7tech.com/2010/04/gateway-management")
            .unmodifiableMap();
    private static Map<String, String> getNamespaceMap() {
        return nsMap;
    }

    @Test
    public void addUiVerifyContext() throws Exception {
        SolutionKit solutionKit = new SolutionKit();
        setupUiAccess(solutionKit);

        // mock class loading
        Map<String, Pair<SolutionKit, SolutionKitCustomization>> customizations = new HashMap<>();
        when(solutionKitsConfig.getCustomizations()).thenReturn(customizations);
        SolutionKitCustomizationClassLoader mockClassLoader = mock(SolutionKitCustomizationClassLoader.class);
        Class uiClass = CustomUiTestHook.class;
        // noinspection unchecked
        when(mockClassLoader.loadClass(uiClassName)).thenReturn(uiClass);
        Class callbackClass = CustomCallbackTestHook.class;
        // noinspection unchecked
        when(mockClassLoader.loadClass(callbackClassName)).thenReturn(callbackClass);

        // instantiate and initialize ui
        SkarProcessor skarProcessor = new SkarProcessor(solutionKitsConfig);
        skarProcessor.setCustomizationInstances(solutionKit, mockClassLoader);

        // setup test hook to run in the ui
        SolutionKitCustomization customization = customizations.get(solutionKit.getSolutionKitGuid()).right;
        CustomUiTestHook ui = (CustomUiTestHook) customization.getCustomUi();
        assert ui != null;
        ui.setTestToRunCreateButton(new Functions.UnaryVoidThrows<SolutionKitManagerContext, RuntimeException>() {
            @Override
            public void call(SolutionKitManagerContext skmContext) throws RuntimeException {
                try {
                    verifyUiAccess(skmContext);

                    // set a key-value pair
                    skmContext.getKeyValues().put(someKey, someValue);

                    // attempt to make read-only changes; which will be ignored and not passed to the callback
                    attemptToModifyReadOnly(skmContext);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });

        // add ui
        SolutionKitCustomization.addCustomUis(new JPanel(), solutionKitsConfig, solutionKit);

        // verify methods were called
        assertTrue("Expected SolutionKitManagerUi.initialize() to be called, but was not.", ui.isInitializeCalled());
        assertTrue("Expected SolutionKitManagerUi.createButton(...) to be called, but was not.", ui.isCreateButtonCalled());

        // setup test hook to run in the callback
        CustomCallbackTestHook callback = (CustomCallbackTestHook) customization.getCustomCallback();
        assert callback != null;
        callback.setTestToRunPreMigrationBundleImport(new Functions.UnaryVoidThrows<SolutionKitManagerContext, RuntimeException>() {
            @Override
            public void call(SolutionKitManagerContext skmContext) throws RuntimeException {
                try {
                    // verify key-value pair was set in ui is available in callback
                    assertEquals(someValue, skmContext.getKeyValues().get(someKey));

                    // verify ui read only changes do not affect callback (e.g. was correctly ignored)
                    verifyAttemptToModifyReadOnlyFailed(skmContext);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });

        // invoke callback
        skarProcessor.invokeCustomCallback(solutionKit);

        // verify method was called
        assertTrue("Expected SolutionKitManagerCallback.preMigrationBundleImportCalled(...) to be called, but was not.", callback.isPreMigrationBundleImportCalled());
    }

    private void setupUiAccess(final SolutionKit solutionKit) throws Exception {
        // set metadata: some random values
        solutionKit.setSolutionKitGuid(someGuidString);
        solutionKit.setSolutionKitVersion(someVersionString);
        solutionKit.setName(someNameString);
        solutionKit.setProperty(SolutionKit.SK_PROP_DESC_KEY, someDescriptionString);
        solutionKit.setProperty(SolutionKit.SK_PROP_CUSTOM_UI_KEY, uiClassName);
        solutionKit.setProperty(SolutionKit.SK_PROP_CUSTOM_CALLBACK_KEY, callbackClassName);
        solutionKit.setProperty(SolutionKit.SK_PROP_TIMESTAMP_KEY, Long.toString(new Date().getTime()));
        solutionKit.setProperty(SolutionKit.SK_PROP_IS_COLLECTION_KEY, Boolean.toString(false));

        // set install bundle
        InputStream inputStream = new ByteArrayInputStream(installBundleStr.getBytes(StandardCharsets.UTF_8));
        final Document installBundleDoc = XmlUtil.parse(new ByteArrayInputStream(IOUtils.slurpStream(inputStream)));
        inputStream.reset();
        when(solutionKitsConfig.getBundleAsDocument(solutionKit)).thenReturn(installBundleDoc);

        // set uninstall bundle
        solutionKit.setUninstallBundle(uninstallBundleStr);

        // set already installed metadata: some random values
        SolutionKit installedSolutionKit = new SolutionKit();
        installedSolutionKit.setSolutionKitGuid(someGuidString);
        installedSolutionKit.setSolutionKitVersion(someInstalledVersionString);
        installedSolutionKit.setProperty(SolutionKit.SK_PROP_DESC_KEY, someInstalledDescriptionString);
        when(solutionKitsConfig.getSolutionKitToUpgrade(someGuidString, null)).thenReturn(installedSolutionKit);
    }

    private void verifyUiAccess(final SolutionKitManagerContext skmContext) throws Exception {
        // verify ui can access metadata
        final Element metadata = skmContext.getSolutionKitMetadata().getDocumentElement();
        assertEquals(someGuidString, getTextValue(findExactlyOneChildElementByName(metadata, SolutionKitUtils.SK_NS, SolutionKitUtils.SK_ELE_ID)));
        assertEquals(someVersionString, getTextValue(findExactlyOneChildElementByName(metadata, SolutionKitUtils.SK_NS, SolutionKitUtils.SK_ELE_VERSION)));
        assertEquals(someNameString, getTextValue(findExactlyOneChildElementByName(metadata, SolutionKitUtils.SK_NS, SolutionKitUtils.SK_ELE_NAME)));
        assertEquals(someDescriptionString, getTextValue(findExactlyOneChildElementByName(metadata, SolutionKitUtils.SK_NS, SolutionKitUtils.SK_ELE_DESC)));
        assertEquals(callbackClassName, getTextValue(findExactlyOneChildElementByName(metadata, SolutionKitUtils.SK_NS, SolutionKitUtils.SK_ELE_CUSTOM_CALLBACK)));
        assertEquals(uiClassName, getTextValue(findExactlyOneChildElementByName(metadata, SolutionKitUtils.SK_NS, SolutionKitUtils.SK_ELE_CUSTOM_UI)));

        // verify ui can access install bundle
        assertEquals(installBundleStr, XmlUtil.nodeToFormattedString(skmContext.getMigrationBundle()));

        // verify ui can access uninstall bundle
        assertEquals(uninstallBundleStr, XmlUtil.nodeToFormattedString(skmContext.getUninstallBundle()));

        // verify ui can access already installed metadata
        Element installedMetadata = skmContext.getInstalledSolutionKitMetadata().getDocumentElement();
        assertEquals(someGuidString, getTextValue(findExactlyOneChildElementByName(installedMetadata, SolutionKitUtils.SK_NS, SolutionKitUtils.SK_ELE_ID)));
        assertEquals(someInstalledVersionString, getTextValue(findExactlyOneChildElementByName(installedMetadata, SolutionKitUtils.SK_NS, SolutionKitUtils.SK_ELE_VERSION)));
        assertEquals(someInstalledDescriptionString, getTextValue(findExactlyOneChildElementByName(installedMetadata, SolutionKitUtils.SK_NS, SolutionKitUtils.SK_ELE_DESC)));
    }

    final String readOnlyStr = "SHOULD BE READ ONLY!";

    private void attemptToModifyReadOnly(final SolutionKitManagerContext skmContext) {
        // modify name in metadata
        final Document solutionKitMetadata = skmContext.getSolutionKitMetadata();
        final List<Element> nameElements = XpathUtil.findElements(solutionKitMetadata.getDocumentElement(), "//l7:SolutionKit/l7:Name", getNamespaceMap());
        if (nameElements.size() > 0) {
            nameElements.get(0).setTextContent(readOnlyStr);
        }

        // modify install bundle
        // skmContext.getMigrationBundle();

        // modify uninstall bundle
        // skmContext.getUninstallBundle();

        // modify already install bundle
        // skmContext.getInstalledSolutionKitMetadata();

        // modify instance modifier
        skmContext.setInstanceModifier(readOnlyStr);
    }

    private void verifyAttemptToModifyReadOnlyFailed(final SolutionKitManagerContext skmContext) {
        // verify metadata name
        final Document solutionKitMetadata = skmContext.getSolutionKitMetadata();
        final List<Element> nameElements = XpathUtil.findElements(solutionKitMetadata.getDocumentElement(), "//l7:SolutionKit/l7:Name", getNamespaceMap());
        final String solutionKitName = nameElements.size() > 0 ? nameElements.get(0).getTextContent() : "";
        assertNotEquals(readOnlyStr , solutionKitName);

        // verify install bundle
        // skmContext.getMigrationBundle();

        // verify uninstall bundle
        // skmContext.getUninstallBundle();

        // verify already install bundle
        // skmContext.getInstalledSolutionKitMetadata();

        // verify instance modifier
        assertNotEquals(readOnlyStr , skmContext.getInstanceModifier());
    }
}