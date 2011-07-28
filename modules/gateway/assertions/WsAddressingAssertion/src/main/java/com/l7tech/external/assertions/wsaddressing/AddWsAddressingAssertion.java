package com.l7tech.external.assertions.wsaddressing;

import com.l7tech.policy.assertion.*;
import com.l7tech.policy.assertion.annotation.RequiresSOAP;
import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.util.SoapConstants;

import java.util.*;

@RequiresSOAP(wss=true)
public class AddWsAddressingAssertion extends MessageTargetableAssertion {

    // - PUBLIC
    public static final String MESSAGE_ID_AUTOMATIC = "<auto>";
    public static final String ACTION_FROM_TARGET_MESSAGE = "<Obtain from target message>";
    public static final String ACTION_EXPLICIT_FROM_WSDL_INPUT = "<Explicit from WSDL (Input)>";
    public static final String ACTION_EXPLICIT_FROM_WSDL_OUTPUT = "<Explicit from WSDL (Output)>";
    public static final String DEFAULT_NAMESPACE = SoapConstants.WSA_NAMESPACE_10;
    public static final String SUFFIX_ACTION = "action";
    public static final String SUFFIX_MESSAGE_ID = "messageId";
    public static final String VARIABLE_PREFIX = "wsa";
    public static final Collection<String> VARIABLE_SUFFIXES = Collections.unmodifiableCollection( Arrays.asList(
            SUFFIX_ACTION,
            SUFFIX_MESSAGE_ID));

    /**
     * Listed in order of preference (newest to oldest).
     */
    public static final Collection<String> WSA_NAMESPACES = Collections.unmodifiableCollection(
            Arrays.asList(
                    SoapConstants.WSA_NAMESPACE_10,
                    SoapConstants.WSA_NAMESPACE2));

    /**
     * Listed in order of preference.
     */
    public static final Collection<String> WSA_ACTIONS = Collections.unmodifiableCollection(
            Arrays.asList(
                    ACTION_FROM_TARGET_MESSAGE,
                    ACTION_EXPLICIT_FROM_WSDL_INPUT,
                    ACTION_EXPLICIT_FROM_WSDL_OUTPUT));

    
    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public String getSourceEndpoint() {
        return sourceEndpoint;
    }

    public void setSourceEndpoint(String sourceEndpoint) {
        this.sourceEndpoint = sourceEndpoint;
    }

    public String getReplyEndpoint() {
        return replyEndpoint;
    }

    public void setReplyEndpoint(String replyEndpoint) {
        this.replyEndpoint = replyEndpoint;
    }

    public String getFaultEndpoint() {
        return faultEndpoint;
    }

    public void setFaultEndpoint(String faultEndpoint) {
        this.faultEndpoint = faultEndpoint;
    }

    public String getRelatesToMessageId() {
        return relatesToMessageId;
    }

    public void setRelatesToMessageId(String relatesToMessageId) {
        this.relatesToMessageId = relatesToMessageId;
    }

    public String getWsaNamespaceUri() {
        return wsaNamespaceUri;
    }

    public void setWsaNamespaceUri(String wsaNamespaceUri) {
        this.wsaNamespaceUri = wsaNamespaceUri;
    }

    public String getVariablePrefix() {
        return variablePrefix;
    }

    public void setVariablePrefix(String variablePrefix) {
        this.variablePrefix = variablePrefix;
    }
    
    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        meta.put(AssertionMetadata.SHORT_NAME, baseName);
        meta.put(AssertionMetadata.DESCRIPTION, "Add WS-Addressing into an outbound message with optional signing.");

        // Add to palette folder(s)
        //   accessControl, transportLayerSecurity, xmlSecurity, xml, routing,
        //   misc, audit, policyLogic, threatProtection
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[] { "xml" });
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/Information16.gif");

        meta.put(AssertionMetadata.PROPERTIES_ACTION_NAME, "Add WS-Addressing Properties");

        //meta.put(AssertionMetadata.PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.external.assertions.wsaddressing.console.AddWsAddressingPropertiesDialog");

        // Enable automatic policy advice (default is no advice unless a matching Advice subclass exists)
        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto");

        // request default feature set name for our class name, since we are a known optional module
        // that is, we want our required feature set to be "assertion:WsAddressing" rather than "set:modularAssertions"
        meta.put(AssertionMetadata.FEATURE_SET_NAME, "(fromClass)");

        meta.put(META_INITIALIZED, Boolean.TRUE);

        return meta;
    }

    //- PROTECTED

    @Override
    protected VariablesUsed doGetVariablesUsed() {
        return super.doGetVariablesUsed().withExpressions(
                getWsaNamespaceUri(),
                getAction(),
                getMessageId(),
                getDestination(),
                getSourceEndpoint(),
                getReplyEndpoint(),
                getFaultEndpoint(),
                getRelatesToMessageId()
        );
    }

    @Override
    protected VariablesSet doGetVariablesSet() {
        return variablePrefix == null ?
                super.doGetVariablesSet() :
                super.doGetVariablesSet().withVariables(
                        new VariableMetadata(variablePrefix + "." + SUFFIX_ACTION, false, false, null, false, DataType.STRING),
                        new VariableMetadata(variablePrefix + "." + SUFFIX_MESSAGE_ID, false, false, null, false, DataType.STRING)
                );
    }

    //- PRIVATE
    private String action;
    private String messageId;
    private String destination;
    private String sourceEndpoint;
    private String replyEndpoint;
    private String faultEndpoint;
    private String relatesToMessageId;
    private String wsaNamespaceUri;
    private String variablePrefix = VARIABLE_PREFIX;

    private static final String META_INITIALIZED = AddWsAddressingAssertion.class.getName() + ".metadataInitialized";

    private final static String baseName ="Add WS-Addressing";
}
