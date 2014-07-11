package com.l7tech.external.assertions.websocket;

import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;
import com.l7tech.policy.assertion.MessageTargetableAssertion;
import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.variable.VariableMetadata;

import java.util.HashMap;
import java.util.Map;

/**
 * Purpose: This assertion will validate WebSocket message with associated XML Schema,
 * If validation success and Send Response Immediately is not checked it will return WebSocket message in a context variable
 * If Send Response Immediately is checked it will exit the policy immediately
 * If validation fail assertion will fail
 * Created with IntelliJ IDEA.
 * User: nilic
 * Date: 6/29/12
 * Time: 12:47 PM
 * To change this template use File | Settings | File Templates.
 */
public class WebSocketValidationAssertion extends MessageTargetableAssertion {

    private final String VARIABLE_PREFIX_DEFAULT  = "websocket";
    private String userVariablePrefix;

    public enum WebSocketSuffix {
        VARIABLE_SUFFIX_TYPE("type"),
        VARIABLE_SUFFIX_OFFSET("offset"),
        VARIABLE_SUFFIX_ORIGIN("origin"),
        VARIABLE_SUFFIX_PROTOCOL("protocol"),
        VARIABLE_SUFFIX_LENGTH("length"),
        VARIABLE_SUFFIX_DATA("data");

        private final String webSocketSuffix;

        WebSocketSuffix(String webSocketSuffix){
            this.webSocketSuffix = webSocketSuffix;
        }

        public String getWebSocketSuffix(){
            return this.webSocketSuffix;
        }

    }

    /**
     * @return the context variable prefix
     */
    public String getUserVariablePrefix() {
        return userVariablePrefix;
    }
    /**
     * @param userVariablePrefix the context variable prefix
     */
    public void setUserVariablePrefix( final String userVariablePrefix ) {
        this.userVariablePrefix = userVariablePrefix;
    }

    public String getVariablePrefix(){
        return userVariablePrefix == null || userVariablePrefix.trim().isEmpty() ? VARIABLE_PREFIX_DEFAULT : userVariablePrefix;
    }

    public String[] getVariableSuffix(){
        return new String[] { WebSocketSuffix.VARIABLE_SUFFIX_TYPE.webSocketSuffix,
                              WebSocketSuffix.VARIABLE_SUFFIX_OFFSET.webSocketSuffix,
                              WebSocketSuffix.VARIABLE_SUFFIX_ORIGIN.webSocketSuffix,
                              WebSocketSuffix.VARIABLE_SUFFIX_PROTOCOL.webSocketSuffix,
                              WebSocketSuffix.VARIABLE_SUFFIX_LENGTH.webSocketSuffix,
                              WebSocketSuffix.VARIABLE_SUFFIX_DATA.webSocketSuffix };
    }

    public final String setVariableName(String name){
        return getVariablePrefix() + "." + name;
    }

    // this will set context variable from this assertion
    @Override
    public VariablesSet doGetVariablesSet() {
        return super.doGetVariablesSet().withVariables(
                new VariableMetadata(setVariableName(WebSocketSuffix.VARIABLE_SUFFIX_TYPE.webSocketSuffix), false, false, null, false, DataType.STRING),
                new VariableMetadata(setVariableName(WebSocketSuffix.VARIABLE_SUFFIX_OFFSET.webSocketSuffix), false, false, null, false, DataType.STRING) ,
                new VariableMetadata(setVariableName(WebSocketSuffix.VARIABLE_SUFFIX_ORIGIN.webSocketSuffix), false, false, null, false, DataType.STRING),
                new VariableMetadata(setVariableName(WebSocketSuffix.VARIABLE_SUFFIX_PROTOCOL.webSocketSuffix), false, false, null, false, DataType.STRING),
                new VariableMetadata(setVariableName(WebSocketSuffix.VARIABLE_SUFFIX_LENGTH.webSocketSuffix), false, false, null, false, DataType.STRING),
                new VariableMetadata(setVariableName(WebSocketSuffix.VARIABLE_SUFFIX_DATA.webSocketSuffix), false, false, null, false, DataType.STRING)
        );
    }

    //
    // Metadata
    //
    private static final String META_INITIALIZED = WebSocketValidationAssertion.class.getName() + ".metadataInitialized";

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        // Cluster properties used by this assertion
        Map<String, String[]> props = new HashMap<String, String[]>();
        //props.put(NAME, new String[] {
        //        DESCRIPTION,
        //        DEFAULT
        //});
        meta.put(AssertionMetadata.CLUSTER_PROPERTIES, props);

        // Set description for GUI
        meta.put(AssertionMetadata.SHORT_NAME, "Validate WebSocket Message");
        meta.put(AssertionMetadata.LONG_NAME, "Validate WebSocket message with xml schema");

        // Add to palette folder(s)
        //   accessControl, transportLayerSecurity, xmlSecurity, xml, routing,
        //   misc, audit, policyLogic, threatProtection
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[]{""});
        meta.put(AssertionMetadata.PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.external.assertions.websocket.console.WebSocketValidationDialog");

        // Enable automatic policy advice (default is no advice unless a matching Advice subclass exists)
        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto");

        // Set up smart Getter for nice, informative policy node name, for GUI
        meta.put(AssertionMetadata.POLICY_NODE_ICON, "com/l7tech/console/resources/xmlsignature.gif");

        // request default feature set name for our class name, since we are a known optional module
        // that is, we want our required feature set to be "assertion:WebSocket" rather than "set:modularAssertions"
        meta.put(AssertionMetadata.FEATURE_SET_NAME, "(fromClass)");

        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }
}
