package com.l7tech.example.solutionkit.simple.v01_02;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.policy.solutionkit.SolutionKitManagerCallback;
import com.l7tech.policy.solutionkit.SolutionKitManagerContext;
import com.l7tech.util.CollectionUtils;
import com.l7tech.xml.xpath.XpathUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;


/**
 * Simple example of callback code to execute before sending bundle into Gateway's restman API.
 * Shows how to use Gateway utility classes in layer7-policy.jar and layer7-utility.jar (XpathUtil and CollectionUtils respectively).
 *
 * How to build sample Customization.jar: see modules/skunkworks/src/main/resources/com/l7tech/example/solutionkit/simple/v01_02/build.sh
 */
public class SimpleSolutionKitManagerCallback extends SolutionKitManagerCallback {
    private static final Logger logger = Logger.getLogger(SimpleSolutionKitManagerCallback.class.getName());

    public static final String MY_INPUT_TEXT_KEY = "MyInputTextKey";
    public static final String MY_HAS_BEEN_CUSTOMIZED_KEY = "MyHasBeenCustomizedKey";

    public static final Map<String, String> nsMap = CollectionUtils.MapBuilder.<String, String>builder()
            .put("l7", "http://ns.l7tech.com/2010/04/gateway-management")
            .unmodifiableMap();

    public static Map<String, String> getNamespaceMap() {
        return nsMap;
    }

    @Override
    public void preMigrationBundleImport(final SolutionKitManagerContext context) throws CallbackException {
        // read metadata
        final Document solutionKitMetadata = context.getSolutionKitMetadata();
        final List<Element> nameElements = XpathUtil.findElements(solutionKitMetadata.getDocumentElement(), "//l7:SolutionKit/l7:Name", getNamespaceMap());
        final String solutionKitName = nameElements.size() > 0 ? nameElements.get(0).getTextContent() : "";

        // read bundle
        final Document restmanBundle = context.getMigrationBundle();
        final List<Element> itemElements = XpathUtil.findElements(restmanBundle.getDocumentElement(), "//l7:Bundle/l7:References/l7:Item", getNamespaceMap());
        final List<Element> mappingElements = XpathUtil.findElements(restmanBundle.getDocumentElement(), "//l7:Bundle/l7:Mappings/l7:Mapping", getNamespaceMap());

        // get user input text
        final String input = context.getKeyValues().get(MY_INPUT_TEXT_KEY);
        final boolean beenCustomized = Boolean.valueOf(context.getKeyValues().get(MY_HAS_BEEN_CUSTOMIZED_KEY));

        final String message = "*** CUSTOM CODE CALLED FOR " + solutionKitName + " ***  # item(s) in bundle: " + itemElements.size() + ", # mapping(s): " + mappingElements.size() + ", instance modifier: " + context.getInstanceModifier() + ". " + input;
        logger.info(message);
        System.out.println(message);


        if (input != null && !beenCustomized) {
            // modify name in metadata
            if (nameElements.size() > 0) {
                nameElements.get(0).setTextContent(input + " " + solutionKitName);
            }

            // create a new folder
            createFolder(restmanBundle, context.getUninstallBundle(), context.isUpgrade(), input);

            // modify encass description in bundle
            final List<Element> encassDescriptionItemElements = XpathUtil.findElements(restmanBundle.getDocumentElement(),
                    "//l7:Bundle/l7:References/l7:Item/l7:Resource/l7:EncapsulatedAssertion/l7:Properties/l7:Property[@key=\"description\"]/l7:StringValue", getNamespaceMap());
            for (Element encassDescription : encassDescriptionItemElements) {
                encassDescription.setTextContent(input + " " + encassDescription.getTextContent());
            }

            context.getKeyValues().put(MY_HAS_BEEN_CUSTOMIZED_KEY, Boolean.TRUE.toString());
        }
    }

    /* The code below is more complex, it shows how to install, upgrade, and delete a custom folder. */

    private static final String MY_FOLDER_ITEM_TEMPLATE =
            "        <l7:Item xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\">\n" +
                    "            <l7:Name>{0}</l7:Name>\n" +
                    "            <l7:Id>f1649a0664f1ebb6235ac238a6f71b0d</l7:Id>\n" +
                    "            <l7:Type>FOLDER</l7:Type>\n" +
                    "            <l7:TimeStamp>2015-10-14T09:07:44.427-07:00</l7:TimeStamp>\n" +
                    "            <l7:Resource>\n" +
                    "                <l7:Folder folderId=\"f1649a0664f1ebb6235ac238a6f71b0c\" id=\"f1649a0664f1ebb6235ac238a6f71b0d\" version=\"0\">\n" +
                    "                    <l7:Name>{0}</l7:Name>\n" +
                    "                </l7:Folder>\n" +
                    "            </l7:Resource>\n" +
                    "        </l7:Item>";

    private static final String MY_FOLDER_INSTALL_MAPPING =
            "<l7:Mapping xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\" action=\"AlwaysCreateNew\" srcId=\"f1649a0664f1ebb6235ac238a6f71b0d\" srcUri=\"https://tluong-pc.l7tech.local:8443/restman/1.0/folders/f1649a0664f1ebb6235ac238a6f71b0d\" type=\"FOLDER\"/>";
    private static final String MY_FOLDER_UPGRADE_MAPPING =
            "<l7:Mapping xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\" action=\"NewOrUpdate\" srcId=\"f1649a0664f1ebb6235ac238a6f71b0d\" srcUri=\"https://tluong-pc.l7tech.local:8443/restman/1.0/folders/f1649a0664f1ebb6235ac238a6f71b0d\" type=\"FOLDER\"/>";
    private static final String MY_FOLDER_DELETE_MAPPING =
            "<l7:Mapping xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\" action=\"Delete\" srcId=\"f1649a0664f1ebb6235ac238a6f71b0d\" srcUri=\"https://tluong-pc.l7tech.local:8443/restman/1.0/folders/f1649a0664f1ebb6235ac238a6f71b0d\" type=\"FOLDER\"/>";

    private void createFolder(final Document restmanBundle, final Document uninstallBundle, final boolean isUpgrade, final String input) throws CallbackException {
        final List<Element> referencesElements = XpathUtil.findElements(restmanBundle.getDocumentElement(), "//l7:Bundle/l7:References", getNamespaceMap());
        final List<Element> mappingsElements = XpathUtil.findElements(restmanBundle.getDocumentElement(), "//l7:Bundle/l7:Mappings", getNamespaceMap());
        if (referencesElements.size() > 0 && mappingsElements.size() > 0) {
            try {
                // append item
                final Element myFolderItem = XmlUtil.stringToDocument(MessageFormat.format(MY_FOLDER_ITEM_TEMPLATE, isUpgrade ? input + " (isUpgrade was true)" : input + " (isUpgrade was false)")).getDocumentElement();
                myFolderItem.removeAttribute("xmlns:l7");
                referencesElements.get(0).appendChild(restmanBundle.importNode(myFolderItem, true));

                // append install or upgrade mapping
                Element myFolderMapping = XmlUtil.stringToDocument(isUpgrade ? MY_FOLDER_UPGRADE_MAPPING : MY_FOLDER_INSTALL_MAPPING).getDocumentElement();
                myFolderMapping.removeAttribute("xmlns:l7");
                mappingsElements.get(0).appendChild(restmanBundle.importNode(myFolderMapping, true));

                // insert delete mapping
                insertFolderDeleteMapping(uninstallBundle);
            } catch (SAXException e) {
                logger.warning(e.toString());
                throw new CallbackException(e);
            }
        }
    }

    private void insertFolderDeleteMapping(final Document uninstallBundle) throws CallbackException, SAXException {
        if (uninstallBundle != null) {
            final List<Element> uninstallMappingsElements = XpathUtil.findElements(uninstallBundle.getDocumentElement(), "/l7:Bundle/l7:Mappings", getNamespaceMap());
            if (uninstallMappingsElements.size() > 0) {
                final List<Element> parentFolderMappingElements = XpathUtil.findElements(uninstallBundle.getDocumentElement(), "/l7:Bundle/l7:Mappings/l7:Mapping[@srcId='f1649a0664f1ebb6235ac238a6f71b0c']", getNamespaceMap());
                if (parentFolderMappingElements.size() > 0) {
                    final Element myNewFolderDeleteMapping = XmlUtil.stringToDocument(MY_FOLDER_DELETE_MAPPING).getDocumentElement();
                    myNewFolderDeleteMapping.removeAttribute("xmlns:l7");

                    // must be deleted before parent folder can be deleted
                    uninstallMappingsElements.get(0).insertBefore(uninstallBundle.importNode(myNewFolderDeleteMapping, true), parentFolderMappingElements.get(0));
                } else {
                    final String msg = "Unexpected error: unable to find uninstall mapping for srcId=f1649a0664f1ebb6235ac238a6f71b0c.";
                    logger.warning(msg);
                    throw new CallbackException(msg);
                }
            }
        }
    }
}
