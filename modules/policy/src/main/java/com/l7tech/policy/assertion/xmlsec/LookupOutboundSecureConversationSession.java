package com.l7tech.policy.assertion.xmlsec;

import com.l7tech.policy.assertion.*;
import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.util.TextUtils;

import java.util.*;

import static com.l7tech.policy.assertion.AssertionMetadata.PROPERTIES_ACTION_NAME;

/**
 * @author ghuang
 */
public class LookupOutboundSecureConversationSession extends MessageTargetableAssertion {
    public static final String DEFAULT_VARIABLE_PREFIX = "scLookup";
    public static final String VARIABLE_SESSION = "session";

    private static final String ASSERTION_BASIC_NAME = "Look Up Outbound Secure Conversation Session";
    private static final String META_INITIALIZED = LookupOutboundSecureConversationSession.class.getName() + ".metadataInitialized";

    private String serviceUrl;
    private String variablePrefix = DEFAULT_VARIABLE_PREFIX;

    public LookupOutboundSecureConversationSession() {}

    public String getServiceUrl() {
        return serviceUrl;
    }

    public void setServiceUrl(String serviceUrl) {
        this.serviceUrl = serviceUrl;
    }

    public String getVariablePrefix() {
        if (variablePrefix == null || variablePrefix.trim().isEmpty()) variablePrefix = DEFAULT_VARIABLE_PREFIX;

        return variablePrefix;
    }

    public void setVariablePrefix(String variablePrefix) {
        this.variablePrefix = (variablePrefix == null || variablePrefix.trim().isEmpty())? DEFAULT_VARIABLE_PREFIX : variablePrefix;
    }

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        // Set description for GUI
        meta.put(AssertionMetadata.SHORT_NAME, ASSERTION_BASIC_NAME);
        meta.put(AssertionMetadata.DESCRIPTION, "Look up an outbound secure conversation session mapped to the authenticated user and the service.");

        // Add to palette folder(s)
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[]{"xmlSecurity"});
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/lookup16.gif");

        // Enable automatic policy advice (default is no advice unless a matching Advice subclass exists)
        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto");

        // Set up smart Getter for nice, informative policy node name, for GUI
        meta.put(AssertionMetadata.POLICY_NODE_NAME_FACTORY, new AssertionNodeNameFactory<LookupOutboundSecureConversationSession>() {
            @Override
            public String getAssertionName(LookupOutboundSecureConversationSession assertion, boolean decorate) {
                if (! decorate) return ASSERTION_BASIC_NAME;

                String serviceUrl = assertion.getServiceUrl();
                if (serviceUrl == null || serviceUrl.trim().isEmpty()) return ASSERTION_BASIC_NAME;
                else return AssertionUtils.decorateName(assertion, ASSERTION_BASIC_NAME + " to " +
                        (serviceUrl.length() <= 128? serviceUrl : TextUtils.truncateStringAtEnd(serviceUrl, 128)));
            }
        });

        meta.put(AssertionMetadata.POLICY_NODE_ICON, "com/l7tech/console/resources/lookup16.gif");
        meta.put(AssertionMetadata.PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.console.panels.LookupOutboundSecureConversationSessionPropertiesDialog");
        meta.put(PROPERTIES_ACTION_NAME, "Outbound Secure Conversation Session Lookup Properties");

        meta.put(META_INITIALIZED, Boolean.TRUE);

        return meta;
    }

    public String[] getVariableSuffixes() {
        return new String[] {VARIABLE_SESSION};
    }

    @Override
    protected VariablesSet doGetVariablesSet() {
        return super.doGetVariablesSet().withVariables(
                new VariableMetadata(variablePrefix + "." + VARIABLE_SESSION, false, false, null, false, DataType.UNKNOWN)
        );
    }

    @Override
    protected VariablesUsed doGetVariablesUsed() {
        return super.doGetVariablesUsed().withExpressions( serviceUrl );
    }
}