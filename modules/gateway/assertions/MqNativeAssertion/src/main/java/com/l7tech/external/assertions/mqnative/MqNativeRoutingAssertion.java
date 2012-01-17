package com.l7tech.external.assertions.mqnative;

import com.l7tech.policy.assertion.*;
import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.policy.wsp.ArrayTypeMapping;
import com.l7tech.policy.wsp.BeanTypeMapping;
import com.l7tech.policy.wsp.SimpleTypeMappingFinder;
import com.l7tech.policy.wsp.TypeMapping;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Route outbound MQ Native to WebSphere MQ.
 */
public class MqNativeRoutingAssertion extends RoutingAssertion implements UsesVariables, SetsVariables {
    private final static String baseName = "Route via MQ Native";
    final static AssertionNodeNameFactory policyNameFactory = new AssertionNodeNameFactory<MqNativeRoutingAssertion>(){
        @Override
        public String getAssertionName( final MqNativeRoutingAssertion assertion, final boolean decorate) {
            if(!decorate || assertion.getEndpointName()==null) return baseName;
            return baseName + " Queue " + assertion.getEndpointName();
        }
    };

    private Long endpointResId = null;
    private String endpointName = null;
    private String responseTimeout = null;
    private MqNativeDynamicProperties dynamicMqRoutingProperties;
    private MqNativeMessagePropertyRuleSet requestMqMessagePropertyRuleSet = new MqNativeMessagePropertyRuleSet();
    private MqNativeMessagePropertyRuleSet responseMqMessagePropertyRuleSet = new MqNativeMessagePropertyRuleSet();

    @NotNull
    private MessageTargetableSupport requestTarget = new MessageTargetableSupport(TargetMessageType.REQUEST, false);
    @NotNull
    private MessageTargetableSupport responseTarget = new MessageTargetableSupport(TargetMessageType.RESPONSE, true);

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        // Ensure inbound transport gets wired up
        meta.put(AssertionMetadata.MODULE_LOAD_LISTENER_CLASSNAME, "com.l7tech.external.assertions.mqnative.server.MqNativeModuleLoadListener");

        // Cluster properties used by this assertion
        Map<String, String[]> props = new HashMap<String, String[]>();
        //props.put(NAME, new String[] {
        //        DESCRIPTION,
        //        DEFAULT
        //});
        meta.put(AssertionMetadata.CLUSTER_PROPERTIES, props);

        // Set description for GUI
        meta.put(AssertionMetadata.SHORT_NAME, baseName);
        meta.put(AssertionMetadata.LONG_NAME, "The incoming message will be routed via MQ to the protected service.");
        meta.put(AssertionMetadata.DESCRIPTION, "<Needs description>");

        // Add to palette folder(s)
        //   accessControl, transportLayerSecurity, xmlSecurity, xml, routing,
        //   misc, audit, policyLogic, threatProtection
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[] { "routing" });
        //meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[] { "policyLogic" });
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/server16.gif");
        //meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/ServerLogs.gif");

        // Enable automatic policy advice (default is no advice unless a matching Advice subclass exists)
        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto");

        // Set up smart Getter for nice, informative policy node name, for GUI
        meta.put(AssertionMetadata.POLICY_NODE_ICON, "com/l7tech/console/resources/server16.gif");
        //meta.put(AssertionMetadata.POLICY_NODE_ICON, "com/l7tech/console/resources/ServerLogs.gif");
        meta.put(AssertionMetadata.POLICY_NODE_NAME_FACTORY, policyNameFactory);

        meta.put(AssertionMetadata.GLOBAL_ACTION_CLASSNAMES, new String[] { "com.l7tech.external.assertions.mqnative.console.MqNativeCustomAction" });

        // request default feature set name for our class name, since we are a known optional module
        // that is, we want our required feature set to be "assertion:MQNativeConnector" rather than "set:modularAssertions"
        meta.put(AssertionMetadata.FEATURE_SET_NAME, "(fromClass)");

        meta.put(AssertionMetadata.PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.external.assertions.mqnative.console.MqNativeRoutingAssertionDialog");
        meta.put(AssertionMetadata.PROPERTIES_ACTION_NAME, "MQ Native Routing Properties");

        Collection<TypeMapping> othermappings = new ArrayList<TypeMapping>();
        othermappings.add(new ArrayTypeMapping(new MqNativeMessagePropertyRule[0], "mqNativeMessagePropertyRule"));
        othermappings.add(new BeanTypeMapping(MqNativeMessagePropertyRule.class, "mappingRule"));
        othermappings.add(new BeanTypeMapping(MqNativeMessagePropertyRuleSet.class, "mappingRuleSet"));
        othermappings.add(new BeanTypeMapping(MqNativeDynamicProperties.class, "mqDynamicProperties"));
        meta.put(AssertionMetadata.WSP_SUBTYPE_FINDER, new SimpleTypeMappingFinder(othermappings));

        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }

    @Override
    public VariableMetadata[] getVariablesSet() {
        List<VariableMetadata> vars = new ArrayList<VariableMetadata>();
        vars.add(new VariableMetadata("module.exists", false, false, null, false, DataType.BOOLEAN));
        vars.add(new VariableMetadata("module.name", false, false, null, false, DataType.STRING));
        vars.add(new VariableMetadata("module.sha1", false, false, null, false, DataType.STRING));
        vars.add(new VariableMetadata("module.assertions", false, true, null, false, DataType.STRING));
        vars.add(new VariableMetadata("mq.completion.code", false, true, null, false, DataType.INTEGER));
        vars.add(new VariableMetadata("mq.reason.code", false, true, null, false, DataType.INTEGER));
        vars.addAll(Arrays.asList(responseTarget.getVariablesSet()));
        return vars.toArray(new VariableMetadata[vars.size()]);
    }

    @Override
    public String[] getVariablesUsed() {
        Set<String> vars = new HashSet<String>();

        if (responseTimeout != null) {
            vars.addAll(Arrays.asList(Syntax.getReferencedNames(responseTimeout)));
        }
        if (dynamicMqRoutingProperties != null) {
            String dynamicVars = dynamicMqRoutingProperties.getFieldsAsVariables();

            if (dynamicVars != null && !dynamicVars.equals(""))
                vars.addAll(Arrays.asList(Syntax.getReferencedNames(dynamicVars)));
        }

        vars.addAll(Arrays.asList(requestTarget.getVariablesUsed()));
        vars.addAll(Arrays.asList(requestMqMessagePropertyRuleSet.getVariablesUsed()));
        vars.addAll(Arrays.asList(responseMqMessagePropertyRuleSet.getVariablesUsed()));
        return vars.toArray(new String[vars.size()]);
    }

    /**
     * Clone MqNativeRoutingAssertion.
     * @noinspection CloneDoesntDeclareCloneNotSupportedException
     */
    @Override
    public Object clone() {
        MqNativeRoutingAssertion copy = (MqNativeRoutingAssertion) super.clone();
        copy.requestTarget = new MessageTargetableSupport(requestTarget);
        copy.responseTarget = new MessageTargetableSupport(responseTarget);

        return copy;
    }
    //
    // Metadata
    //
    private static final String META_INITIALIZED = MqNativeRoutingAssertion.class.getName() + ".metadataInitialized";
    private String assertionXml = "";

    public String getAssertionXml() {
        return assertionXml;
    }

    public void setAssertionXml(String assertionXml) {
        if (assertionXml == null)
            throw new IllegalArgumentException("assertionXml may not be null");
        this.assertionXml = assertionXml;
    }

    /**
     * @return the ResId of the MQ Resource Type, or null if there isn't one.
     */
    public Long getEndpointResId() {
        return endpointResId;
    }

    /**
     * Set the ResId of the MQ Resource Type.  Set this to null if no endpoint is configured.
     * @param endpointResId  the ResId of a MqResourceType instance, or null.
     */
    public void setEndpointResId(Long endpointResId) {
        this.endpointResId = endpointResId;
    }

    /**
     * The name of the { MqResourceType }
     * @return the name of this endpoint if known, for cosmetic purposes only.
     */
    public String getEndpointName() {
        return endpointName;
    }

    /**
     * The name of the { MqResourceType}.
     * @param endpointName the name of this endpoint if known, for cosmetic purposes only.
     */
    public void setEndpointName(String endpointName) {
        this.endpointName = endpointName;
    }

    /**
     * The time, in milliseconds, that the SSG should wait for a response from the protected service.
     * After this timeout has lapsed, the request fails.
     *
     * Defaults to {ServerConfig.PARAM_JMS_RESPONSE_TIMEOUT}.
     * @return the response timeout (in milliseconds)
     */
    public String getResponseTimeout() {
        return responseTimeout;
    }

    /**
     * The time, in milliseconds, that the SSG should wait for a response from the protected service.
     * After this timeout has lapsed, the request fails.
     *
     * Defaults to {ServerConfig.PARAM_JMS_RESPONSE_TIMEOUT}.
     * @param responseTimeout the response timeout (in milliseconds)
     */
    public void setResponseTimeout( String responseTimeout ) {
        this.responseTimeout = responseTimeout;
    }

    /**
     * @return set of rules for propagating request JMS message properties
     * @since SecureSpan 4.0
     */
    public MqNativeMessagePropertyRuleSet getRequestMqNativeMessagePropertyRuleSet() {
        return requestMqMessagePropertyRuleSet;
    }

    /**
     * Set the rules for propagating request JMS message properties.
     * @param ruleSet   rules for propagating request JMS message properties
     * @since SecureSpan 4.0
     */
    public void setRequestMqNativeMessagePropertyRuleSet(MqNativeMessagePropertyRuleSet ruleSet) {
        requestMqMessagePropertyRuleSet = ruleSet;
    }

    /**
     * @return set of rules for propagating response JMS message properties
     * @since SecureSpan 4.0
     */
    public MqNativeMessagePropertyRuleSet getResponseMqNativeMessagePropertyRuleSet() {
        return responseMqMessagePropertyRuleSet;
    }

    /**
     * Set the rules for propagating response JMS message properties.
     * @param ruleSet   rules for propagating response JMS message properties
     * @since SecureSpan 4.0
     */
    public void setResponseMqNativeMessagePropertyRuleSet(MqNativeMessagePropertyRuleSet ruleSet) {
        responseMqMessagePropertyRuleSet = ruleSet;
    }

    public MessageTargetableSupport getRequestTarget() {
        return requestTarget;
    }

    public void setRequestTarget(MessageTargetableSupport requestTarget) {
        this.requestTarget = requestTarget;
    }

    public MessageTargetableSupport getResponseTarget() {
        return responseTarget;
    }

    public void setResponseTarget(MessageTargetableSupport responseTarget) {
        this.responseTarget = responseTarget;
    }

    @Override
    public boolean initializesRequest() {
        return responseTarget != null && TargetMessageType.REQUEST == responseTarget.getTarget();
    }

    @Override
    public boolean needsInitializedRequest() {
        return requestTarget == null || TargetMessageType.REQUEST == requestTarget.getTarget();
    }

    @Override
    public boolean initializesResponse() {
        return responseTarget != null && TargetMessageType.RESPONSE == responseTarget.getTarget();
    }

    @Override
    public boolean needsInitializedResponse() {
        return requestTarget != null && TargetMessageType.RESPONSE == requestTarget.getTarget();
    }

    public MqNativeDynamicProperties getDynamicMqRoutingProperties() {
        return dynamicMqRoutingProperties;
    }

    public void setDynamicMqRoutingProperties(MqNativeDynamicProperties dynamicMqRoutingProperties) {
        this.dynamicMqRoutingProperties = dynamicMqRoutingProperties;
    }
}
