package com.l7tech.external.assertions.mapparts;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;
import com.l7tech.policy.assertion.SetsVariables;
import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.variable.VariableMetadata;

import java.util.logging.Logger;

/**
 * Sets a context variable to an array of all the multipart content IDs. 
 */
public class MapPartsAssertion extends Assertion implements SetsVariables {
    protected static final Logger logger = Logger.getLogger(MapPartsAssertion.class.getName());
    public static final String REQUEST_PARTS_CONTENT_IDS = "request.parts.contentIds";

    public VariableMetadata[] getVariablesSet() {
        return new VariableMetadata[] {
                new VariableMetadata(REQUEST_PARTS_CONTENT_IDS, false, true, null, true, DataType.STRING),
        };
    }

    //
    // Metadata
    //
    private static final String META_INITIALIZED = MapPartsAssertion.class.getName() + ".metadataInitialized";

    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        // Set description for GUI
        meta.put(AssertionMetadata.SHORT_NAME, "Map Part IDs");
        meta.put(AssertionMetadata.LONG_NAME, "Map multipart content IDs");

        // Add to palette folder(s) 
        //   accessControl, transportLayerSecurity, xmlSecurity, xml, routing, 
        //   misc, audit, policyLogic, threatProtection 
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[] { "misc" });
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/external/assertions/mapparts/console/resources/map16.gif");

        // Enable automatic policy advice (default is no advice unless a matching Advice subclass exists)
        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto");

        // Set up smart Getter for nice, informative policy node name, for GUI
        meta.put(AssertionMetadata.POLICY_NODE_ICON, "com/l7tech/external/assertions/mapparts/console/resources/map16.gif");

        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }
}
