package com.l7tech.policy.assertion.xmlsec;

import com.l7tech.policy.assertion.*;
import com.l7tech.policy.wsp.Java5EnumTypeMapping;
import com.l7tech.policy.wsp.SimpleTypeMappingFinder;
import com.l7tech.policy.wsp.TypeMapping;

import java.util.Arrays;

import static com.l7tech.policy.assertion.AssertionMetadata.*;

/**
 * @author ghuang
 */
public class CancelSecurityContext extends MessageTargetableAssertion {

    //- PUBLIC

    public enum AuthorizationType {
        /**
         * Any user can cancel the token (authorization not required)
         */
        NONE,

        /**
         * The token can only be cancelled by the owner
         */
        USER,

        /**
         * The token can only be cancelled by the owner when authenticated using the token
         */
        TOKEN
    }

    public CancelSecurityContext() {
    }

    public AuthorizationType getRequiredAuthorization() {
        return requiredAuthorization;
    }

    public void setRequiredAuthorization( final AuthorizationType requiredAuthorization ) {
        this.requiredAuthorization = requiredAuthorization==null ? AuthorizationType.TOKEN : requiredAuthorization;
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

        // Set description for GUI
        meta.put(SHORT_NAME, "Cancel Security Context");
        meta.put(DESCRIPTION, "Cancel a security context associated with a secure conversation session.");

        // Add to palette folder(s)
        meta.put(PALETTE_FOLDERS, new String[]{"xmlSecurity"});
        meta.put(PALETTE_NODE_ICON, "com/l7tech/console/resources/disconnect.gif");

        // Enable automatic policy advice (default is no advice unless a matching Advice subclass exists)
        meta.put(POLICY_ADVICE_CLASSNAME, "auto");

        meta.put(PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.console.panels.CancelSecurityContextPropertiesDialog");
        meta.put(PROPERTIES_ACTION_NAME, "Security Context Cancellation Properties");

        meta.put(AssertionMetadata.WSP_SUBTYPE_FINDER, new SimpleTypeMappingFinder( Arrays.<TypeMapping>asList(
            new Java5EnumTypeMapping(AuthorizationType.class, "authorizationType")
        )));

        meta.put(META_INITIALIZED, Boolean.TRUE);

        return meta;
    }

    //- PRIVATE

    private static final String META_INITIALIZED = CancelSecurityContext.class.getName() + ".metadataInitialized";

    private AuthorizationType requiredAuthorization = AuthorizationType.TOKEN;
    private boolean failIfNotExist = true; // Default set as true.  If user wants to change to false, go to the assertion properties dialog to make change.
    private boolean failIfExpired = true;  // Default set as true.  If user wants to change to false, go to the assertion properties dialog to make change.
}