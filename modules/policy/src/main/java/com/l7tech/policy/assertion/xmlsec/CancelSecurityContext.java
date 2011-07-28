package com.l7tech.policy.assertion.xmlsec;

import com.l7tech.policy.assertion.*;
import com.l7tech.policy.wsp.Java5EnumTypeMapping;
import com.l7tech.policy.wsp.SimpleTypeMappingFinder;
import com.l7tech.policy.wsp.TypeMapping;
import com.l7tech.util.TextUtils;

import java.util.Arrays;

import static com.l7tech.policy.assertion.AssertionMetadata.*;

/**
 * @author ghuang
 */
public class CancelSecurityContext extends MessageTargetableAssertion {

    //- PUBLIC

    public static final String ASSERTION_BASIC_NAME = "Cancel Security Context";

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

    public boolean isCancelInbound() {
        return cancelInbound;
    }

    public void setCancelInbound( final boolean cancelInbound ) {
        this.cancelInbound = cancelInbound;
    }

    public AuthorizationType getRequiredAuthorization() {
        return requiredAuthorization;
    }

    public void setRequiredAuthorization( final AuthorizationType requiredAuthorization ) {
        this.requiredAuthorization = requiredAuthorization==null ? AuthorizationType.TOKEN : requiredAuthorization;
    }

    public String getOutboundServiceUrl() {
        return outboundServiceUrl;
    }

    public void setOutboundServiceUrl( final String outboundServiceUrl ) {
        this.outboundServiceUrl = outboundServiceUrl;
    }

    public boolean isFailIfNotExist() {
        return failIfNotExist;
    }

    public void setFailIfNotExist(boolean failIfNotExist) {
        this.failIfNotExist = failIfNotExist;
    }

   @Override
    protected VariablesUsed doGetVariablesUsed() {
        return super.doGetVariablesUsed().withExpressions( outboundServiceUrl );
    }

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        // Set description for GUI
        meta.put(SHORT_NAME, ASSERTION_BASIC_NAME);
        meta.put(DESCRIPTION, "Cancel a security context associated with an outbound/inbound secure conversation.");

        // Add to palette folder(s)
        meta.put(PALETTE_FOLDERS, new String[]{"xmlSecurity"});
        meta.put(PALETTE_NODE_ICON, "com/l7tech/console/resources/disconnect.gif");

        // Enable automatic policy advice (default is no advice unless a matching Advice subclass exists)
        meta.put(POLICY_ADVICE_CLASSNAME, "auto");

        // Set up smart Getter for nice, informative policy node name, for GUI
        meta.put(AssertionMetadata.POLICY_NODE_NAME_FACTORY, new AssertionNodeNameFactory<CancelSecurityContext>() {
            @Override
            public String getAssertionName(CancelSecurityContext assertion, boolean decorate) {
                if (! decorate) return ASSERTION_BASIC_NAME;

                String serviceUrl = assertion.getOutboundServiceUrl();
                if (serviceUrl == null || serviceUrl.trim().isEmpty()) return ASSERTION_BASIC_NAME;
                else return AssertionUtils.decorateName(assertion, ASSERTION_BASIC_NAME + " to " +
                        (serviceUrl.length() <= 128? serviceUrl : TextUtils.truncateStringAtEnd(serviceUrl, 128)));
            }
        });

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
    private boolean cancelInbound = true;
    private String outboundServiceUrl;
}
