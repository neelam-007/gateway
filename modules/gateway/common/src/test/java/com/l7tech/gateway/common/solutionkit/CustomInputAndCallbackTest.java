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
import org.xml.sax.SAXException;

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
    public void invokeCallbackThrowsException() throws Exception {
        final SolutionKit solutionKit = new SolutionKit();
        solutionKit.setName("My Solution Kit");
        SolutionKitManagerUi mockUi = mock(SolutionKitManagerUi.class);

        // throw CallbackException
        final String errorMessage = "myErrorMessage";
        SolutionKitManagerCallback callback = new SolutionKitManagerCallback() {
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
            fail("Expected custom callback to throw an exception.");
        } catch (BadRequestException e) {
            assertEquals(errorMessage, e.getMessage());
        }

        // throw IncompatibleClassChangeError
        callback = new SolutionKitManagerCallback() {
            @Override
            public void preMigrationBundleImport(SolutionKitManagerContext context) throws CallbackException {
                throw new IncompatibleClassChangeError();
            }
        };
        customizations.clear();
        customizations.put(solutionKit.getSolutionKitGuid(), new Pair<>(solutionKit, new SolutionKitCustomization(mock(SolutionKitCustomizationClassLoader.class), mockUi, callback)));

        // invoke with IncompatibleClassChangeError
        try {
            new SkarProcessor(solutionKitsConfig).invokeCustomCallback(solutionKit);
            fail("Expected custom callback to throw an exception.");
        } catch (BadRequestException e) {
            // do nothing, exception expected
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

        // set install bundle
        when(solutionKitsConfig.getBundleAsDocument(solutionKit)).thenReturn(getBundleAsDocument());

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

        // set install bundle again (mockito workaround to get a new instance)
        // this simulates what actually happens; otherwise mockito gives the same object and our read-only test fails
        when(solutionKitsConfig.getBundleAsDocument(solutionKit)).thenReturn(getBundleAsDocument());

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

        // set uninstall bundle
        solutionKit.setUninstallBundle(uninstallBundleStr);

        // set already installed metadata: some random values
        SolutionKit installedSolutionKit = new SolutionKit();
        installedSolutionKit.setSolutionKitGuid(someGuidString);
        installedSolutionKit.setSolutionKitVersion(someInstalledVersionString);
        installedSolutionKit.setProperty(SolutionKit.SK_PROP_DESC_KEY, someInstalledDescriptionString);
        when(solutionKitsConfig.getSolutionKitToUpgrade(someGuidString, null)).thenReturn(installedSolutionKit);
    }

    private Document getBundleAsDocument() throws Exception {
        InputStream inputStream = new ByteArrayInputStream(installBundleStr.getBytes(StandardCharsets.UTF_8));
        final Document installBundleDoc = XmlUtil.parse(new ByteArrayInputStream(IOUtils.slurpStream(inputStream)));
        inputStream.reset();
        return installBundleDoc;
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

    private void attemptToModifyReadOnly(final SolutionKitManagerContext skmContext) throws SAXException, SolutionKitManagerCallback.CallbackException {
        // modify name in metadata
        {
            final Document solutionKitMetadata = skmContext.getSolutionKitMetadata();
            final List<Element> nameElements = XpathUtil.findElements(solutionKitMetadata.getDocumentElement(), "//l7:SolutionKit/l7:Name", getNamespaceMap());
            if (nameElements.size() > 0) {
                nameElements.get(0).setTextContent(readOnlyStr);
            }
        }

        // modify install bundle
        modifyInstallBundle(skmContext.getMigrationBundle());

        // modify uninstall bundle
        modifyUninstallBundle(skmContext.getUninstallBundle());

        // modify already installed metadata
        {
            final Document installedSolutionKitMetadata = skmContext.getInstalledSolutionKitMetadata();
            final List<Element> nameElements = XpathUtil.findElements(installedSolutionKitMetadata.getDocumentElement(), "//l7:SolutionKit/l7:Name", getNamespaceMap());
            if (nameElements.size() > 0) {
                nameElements.get(0).setTextContent(readOnlyStr);
            }
        }

        // modify instance modifier
        skmContext.setInstanceModifier(readOnlyStr);
    }

    private void modifyInstallBundle(final Document installBundle) throws SAXException, SolutionKitManagerCallback.CallbackException {
        final String MY_FOLDER_ITEM =
                "        <l7:Item xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\">\n" +
                        "            <l7:Name>{0}</l7:Name>\n" +
                        "            <l7:Id>f1649a0664f1ebb6235ac238a6f71b0d</l7:Id>\n" +
                        "            <l7:Type>FOLDER</l7:Type>\n" +
                        "            <l7:TimeStamp>2015-10-14T09:07:44.427-07:00</l7:TimeStamp>\n" +
                        "            <l7:Resource>\n" +
                        "                <l7:Folder folderId=\"f1649a0664f1ebb6235ac238a6f71b0c\" id=\"f1649a0664f1ebb6235ac238a6f71b0d\" version=\"0\">\n" +
                        "                    <l7:Name>My Folder</l7:Name>\n" +
                        "                </l7:Folder>\n" +
                        "            </l7:Resource>\n" +
                        "        </l7:Item>";
        final List<Element> referencesElements = XpathUtil.findElements(installBundle.getDocumentElement(), "//l7:Bundle/l7:References", getNamespaceMap());
        final Element myFolderItem = XmlUtil.stringToDocument(MY_FOLDER_ITEM).getDocumentElement();
        myFolderItem.removeAttribute("xmlns:l7");
        referencesElements.get(0).appendChild(installBundle.importNode(myFolderItem, true));
        final String MY_FOLDER_INSTALL_MAPPING =
                "<l7:Mapping xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\" action=\"AlwaysCreateNew\" srcId=\"f1649a0664f1ebb6235ac238a6f71b0d\" srcUri=\"https://tluong-pc.l7tech.local:8443/restman/1.0/folders/f1649a0664f1ebb6235ac238a6f71b0d\" type=\"FOLDER\"/>";
        final List<Element> mappingsElements = XpathUtil.findElements(installBundle.getDocumentElement(), "//l7:Bundle/l7:Mappings", getNamespaceMap());
        Element myFolderMapping = XmlUtil.stringToDocument(MY_FOLDER_INSTALL_MAPPING).getDocumentElement();
        myFolderMapping.removeAttribute("xmlns:l7");
        mappingsElements.get(0).appendChild(installBundle.importNode(myFolderMapping, true));
    }

    private void modifyUninstallBundle(final Document uninstallBundle) throws SolutionKitManagerCallback.CallbackException, SAXException {
        final String MY_FOLDER_DELETE_MAPPING =
                "<l7:Mapping xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\" action=\"Delete\" srcId=\"f1649a0664f1ebb6235ac238a6f71b0d\" srcUri=\"https://tluong-pc.l7tech.local:8443/restman/1.0/folders/f1649a0664f1ebb6235ac238a6f71b0d\" type=\"FOLDER\"/>";

        final List<Element> uninstallMappingsElements = XpathUtil.findElements(uninstallBundle.getDocumentElement(), "/l7:Bundle/l7:Mappings", getNamespaceMap());
        Element myFolderMapping = XmlUtil.stringToDocument(MY_FOLDER_DELETE_MAPPING).getDocumentElement();
        myFolderMapping.removeAttribute("xmlns:l7");
        uninstallMappingsElements.get(0).appendChild(uninstallBundle.importNode(myFolderMapping, true));
    }

    private void verifyAttemptToModifyReadOnlyFailed(final SolutionKitManagerContext skmContext) {
        // verify metadata name
        {
            final Document solutionKitMetadata = skmContext.getSolutionKitMetadata();
            final List<Element> nameElements = XpathUtil.findElements(solutionKitMetadata.getDocumentElement(), "//l7:SolutionKit/l7:Name", getNamespaceMap());
            final String solutionKitName = nameElements.size() > 0 ? nameElements.get(0).getTextContent() : "";
            assertNotEquals(readOnlyStr , solutionKitName);
        }

        // verify install bundle
        {
            final Document installBundle = skmContext.getMigrationBundle();
            final List<Element> itemElements = XpathUtil.findElements(installBundle.getDocumentElement(), "//l7:Bundle/l7:References/l7:Item", getNamespaceMap());
            assertEquals(0, itemElements.size());
            final List<Element> mappingElements = XpathUtil.findElements(installBundle.getDocumentElement(), "//l7:Bundle/l7:Mappings/l7:Mapping", getNamespaceMap());
            assertEquals(0, mappingElements.size());
        }

        // verify uninstall bundle
        {
            final Document uninstallBundle = skmContext.getUninstallBundle();
            final List<Element> uninstallMappingElements = XpathUtil.findElements(uninstallBundle.getDocumentElement(), "//l7:Bundle/l7:Mappings/l7:Mapping", getNamespaceMap());
            assertEquals(0, uninstallMappingElements.size());
        }

        // verify already installed metadata
        {
            final Document installedSolutionKitMetadata = skmContext.getInstalledSolutionKitMetadata();
            final List<Element> nameElements = XpathUtil.findElements(installedSolutionKitMetadata.getDocumentElement(), "//l7:SolutionKit/l7:Name", getNamespaceMap());
            final String solutionKitName = nameElements.size() > 0 ? nameElements.get(0).getTextContent() : "";
            assertNotEquals(readOnlyStr, solutionKitName);
        }

        // verify instance modifier
        assertNotEquals(readOnlyStr , skmContext.getInstanceModifier());
    }
}