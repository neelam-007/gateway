package com.l7tech.external.assertions.stripparts;

import com.l7tech.common.util.Functions;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;

import java.util.logging.Logger;

/**
 * 
 */
public class StripPartsAssertion extends Assertion {
    protected static final Logger logger = Logger.getLogger(StripPartsAssertion.class.getName());

    private boolean actOnRequest = true;

    /**
     * @return true if this assertion will act on the request; false if it will act on the reply
     */
    public boolean isActOnRequest() {
        return actOnRequest;
    }

    /**
     * @param actOnRequest true to act on the request; false to act on the reply
     */
    public void setActOnRequest(boolean actOnRequest) {
        this.actOnRequest = actOnRequest;
    }

    //
    // Metadata
    //
    private static final String META_INITIALIZED = StripPartsAssertion.class.getName() + ".metadataInitialized";

    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        // Set description for GUI
        meta.put(AssertionMetadata.SHORT_NAME, "Strip Parts");
        meta.put(AssertionMetadata.LONG_NAME, "Strips all MIME multipart parts except the XML part.");
        meta.put(AssertionMetadata.DESCRIPTION, "If the message is multipart, converts it to a single part message by throwing away all but the XML part.");

        meta.put(AssertionMetadata.POLICY_NODE_NAME_FACTORY, new Functions.Unary< String, StripPartsAssertion >() {
            public String call(StripPartsAssertion assertion) {
                return assertion.isActOnRequest()
                        ? "Strip Parts from Request"
                        : "Strip Parts from Response";
            }
        });

        // Add to palette folder(s) 
        //   accessControl, transportLayerSecurity, xmlSecurity, xml, routing, 
        //   misc, audit, policyLogic, threatProtection 
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[] { "misc" });
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/external/assertions/stripparts/console/resources/strip16.gif");

        // Enable automatic policy advice (default is no advice unless a matching Advice subclass exists)
        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto");

        // Set up smart Getter for nice, informative policy node name, for GUI
        meta.put(AssertionMetadata.POLICY_NODE_ICON, "com/l7tech/external/assertions/stripparts/console/resources/strip16.gif");

        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }

}
