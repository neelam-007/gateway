package com.l7tech.example.solutionkit.simple.v01_03;

import com.l7tech.policy.solutionkit.SolutionKitManagerContext;
import com.l7tech.xml.xpath.XpathUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.List;

/**
 * Simple example of callback code to execute before sending bundle into Gateway's restman API.
 * Shows how to use the shared context map and get access to other kits context.
 *
 * How to build sample Customization.jar: see modules/skunkworks/src/main/resources/com/l7tech/example/solutionkit/simple/v01_03/build.sh
 */
public class SimpleServiceSolutionKitManagerCallback extends BaseSolutionKitManagerCallback {
    public static final String MY_WAS_UI_PROCESSED = "MyWasUiProcessed";
    public static final String MY_WAS_BUTTON_CREATED_KEY = "MyWasButtonCreatedKey";
    public static final String OTHER_GUID = "33b16742-d62d-4095-8f8d-4db707e9ad52";

    @Override
    protected void OnPreMigrationBundleImport(final SolutionKitManagerContext context) throws CallbackException {
        // read bundle
        final Document restmanBundle = context.getMigrationBundle();
        final List<Element> itemElements = XpathUtil.findElements(restmanBundle.getDocumentElement(), "//l7:Bundle/l7:References/l7:Item", getNamespaceMap());
        final List<Element> mappingElements = XpathUtil.findElements(restmanBundle.getDocumentElement(), "//l7:Bundle/l7:Mappings/l7:Mapping", getNamespaceMap());

        final String message = "*** CUSTOM CODE CALLED FOR " + getSolutionKitName() + " ***  # item(s) in bundle: " + itemElements.size() + ", # mapping(s): " + mappingElements.size() + ", instance modifier: " + context.getInstanceModifier();
        logger.info(message);
        System.out.println(message);

        final boolean buttonCreated = Boolean.valueOf(context.getKeyValues().get(MY_WAS_BUTTON_CREATED_KEY));
        if (buttonCreated) {
            // make sure our UI button has been pressed
            // this is ignored on headless side
            final boolean uiExecuted = Boolean.valueOf(context.getKeyValues().get(MY_WAS_UI_PROCESSED));
            if (!uiExecuted) {
                throw new CallbackException(
                        "Solution kit '" + getSolutionKitName() + "' requires custom UI to be executed." +
                        "Please highlight the solution kit row and click the 'Custom UI: ...' button."
                );
            }
        }

        // print the keyValues for the other kit
        printOutContextMap(OTHER_GUID);
    }
}
