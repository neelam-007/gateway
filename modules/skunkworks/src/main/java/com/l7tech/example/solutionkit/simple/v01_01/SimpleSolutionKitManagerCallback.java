package com.l7tech.example.solutionkit.simple.v01_01;

import com.l7tech.policy.solutionkit.SolutionKitManagerCallback;
import com.l7tech.policy.solutionkit.SolutionKitManagerContext;
import com.l7tech.util.CollectionUtils;
import com.l7tech.xml.xpath.XpathUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;


/**
 * Simple example of callback code to execute before sending bundle into Gateway's restman API.
 * Shows how to use Gateway utility classes in layer7-policy.jar and layer7-utility.jar (XpathUtil and CollectionUtils respectively).
 *
 * How to build sample Customization.jar: see modules/skunkworks/src/main/resources/com/l7tech/example/solutionkit/simple/v01_01/build.sh
 */
public class SimpleSolutionKitManagerCallback extends SolutionKitManagerCallback {
    private static final Logger logger = Logger.getLogger(SimpleSolutionKitManagerCallback.class.getName());

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

        final String message = "*** CUSTOM CODE CALLED FOR " + solutionKitName + " ***  # item(s) in bundle: " + itemElements.size() + ", # mapping(s): " + mappingElements.size() + ".  " + context.getCustomDataObject();
        logger.info(message);
        System.out.println(message);

        // get user input text
        final StringBuilder customText = (StringBuilder)context.getCustomDataObject();

        // modify name in metadata
        if (nameElements.size() > 0) {
            nameElements.get(0).setTextContent(customText.toString() + " " + solutionKitName);
        }

        // modify encass description in bundle
        final List<Element> encassDescriptionItemElements = XpathUtil.findElements(restmanBundle.getDocumentElement(),
                "//l7:Bundle/l7:References/l7:Item/l7:Resource/l7:EncapsulatedAssertion/l7:Properties/l7:Property[@key=\"description\"]/l7:StringValue", getNamespaceMap());
        for (Element encassDescription : encassDescriptionItemElements) {
            encassDescription.setTextContent(customText.toString() + " " + encassDescription.getTextContent());
        }
    }
}
