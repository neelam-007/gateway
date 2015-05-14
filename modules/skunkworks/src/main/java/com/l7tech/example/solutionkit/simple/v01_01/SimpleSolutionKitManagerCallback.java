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
 * How to build sample Customization.jar below:
 *
 * Depends on <l7_workspace>/modules/gateway/api/build/layer7-api.jar (e.g. ./build.sh moduled -Dmodule=layer7-api)
 *         and <l7_workspace>/modules/policy/build/layer7-policy.jar (e.g. ./build.sh moduled -Dmodule=layer7-policy)
 *         and <l7_workspace>/modules/utility/build/layer7-utility.jar (e.g. ./build.sh moduled -Dmodule=layer7-utility)
 *
 * prompt> cd <l7_workspace>/modules/skunkworks
 *
 * Make sure build/example/solutionkit/simple/v01_01 directory exists (e.g. mkdir -p build/example/solutionkit/simple/v01_01)
 *
 * prompt> javac -sourcepath src/main/java/ -classpath ../gateway/api/build/layer7-api.jar:../policy/build/layer7-policy.jar:../utility/build/layer7-utility.jar
 *          src/main/java/com/l7tech/example/solutionkit/simple/v01_01/SimpleSolutionKitManagerCallback.java src/main/java/com/l7tech/example/solutionkit/simple/v01_01/console/SimpleSolutionKitManagerUi.java -d build/example/solutionkit/simple/v01_01
 *
 * prompt> cd build/example/solutionkit/simple/v01_01
 *
 * prompt> jar cvf Customization.jar com/l7tech/example/solutionkit/simple/v01_01/SimpleSolutionKitManagerCallback.class com/l7tech/example/solutionkit/simple/v01_01/console/SimpleSolutionKitManagerUi.class com/l7tech/example/solutionkit/simple/v01_01/console/SimpleSolutionKitManagerUi\$1.class
 *
 * Possible improvement: look into how to add this as a build target in the main build.xml.
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
    public void preMigrationBundleImport(final Document restmanBundle, final SolutionKitManagerContext context) throws CallbackException {
        List<Element> itemElements = XpathUtil.findElements(restmanBundle.getDocumentElement(), "//l7:Bundle/l7:References/l7:Item", getNamespaceMap());
        List<Element> mappingElements = XpathUtil.findElements(restmanBundle.getDocumentElement(), "//l7:Bundle/l7:Mappings/l7:Mapping", getNamespaceMap());
        final String message = "*** CUSTOM CODE CALLED *** # item(s) in bundle: " + itemElements.size() + ", # mapping(s): " + mappingElements.size() + ".  " + context.getCustomDataObject();

        logger.info(message);
        System.out.println(message);
    }
}
