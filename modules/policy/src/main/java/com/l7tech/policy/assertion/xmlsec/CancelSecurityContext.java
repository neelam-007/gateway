package com.l7tech.policy.assertion.xmlsec;

import com.l7tech.policy.assertion.*;

import java.util.HashMap;
import java.util.Map;

import static com.l7tech.policy.assertion.AssertionMetadata.PROPERTIES_ACTION_NAME;

/**
 * @author ghuang
 */
public class CancelSecurityContext extends MessageTargetableAssertion {

    private static final String ASSERTION_SHORT_NAME = "Cancel Security Context";
    private static final String META_INITIALIZED = CancelSecurityContext.class.getName() + ".metadataInitialized";

    private boolean failIfNotExist = true; // Default set as true.  If user wants to change to false, go to the assertion properties dialog to make change.
    private boolean failIfExpired = true;  // Default set as true.  If user wants to change to false, go to the assertion properties dialog to make change.

    public CancelSecurityContext() {
    }

    public boolean isFailIfNotExist() {
        return failIfNotExist;
    }

    public void setFailIfNotExist(boolean failIfNotExist) {
        this.failIfNotExist = failIfNotExist;
    }

    public boolean isFailIfExpired() {
        return failIfExpired;
    }

    public void setFailIfExpired(boolean failIfExpired) {
        this.failIfExpired = failIfExpired;
    }

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        // Cluster properties used by this assertion
        Map<String, String[]> props = new HashMap<String, String[]>();
        meta.put(AssertionMetadata.CLUSTER_PROPERTIES, props);

        // Set description for GUI
        meta.put(AssertionMetadata.SHORT_NAME, ASSERTION_SHORT_NAME);
        meta.put(AssertionMetadata.DESCRIPTION, "Cancel a security context associated with a secure conversation session.");

        // Add to palette folder(s)
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[]{"xmlSecurity"});
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/disconnect.gif");

        // Enable automatic policy advice (default is no advice unless a matching Advice subclass exists)
        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto");

        meta.put(AssertionMetadata.POLICY_NODE_ICON, "com/l7tech/console/resources/disconnect.gif");
        meta.put(AssertionMetadata.PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.console.panels.CancelSecurityContextPropertiesDialog");
        meta.put(PROPERTIES_ACTION_NAME, "Security Context Cancellation Properties");

        meta.put(META_INITIALIZED, Boolean.TRUE);

        return meta;
    }
}