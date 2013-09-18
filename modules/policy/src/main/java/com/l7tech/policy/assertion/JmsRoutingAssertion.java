package com.l7tech.policy.assertion;

import com.l7tech.objectmodel.*;
import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.objectmodel.migration.MigrationMappingSelection;
import com.l7tech.objectmodel.migration.PropertyResolver;
import com.l7tech.policy.JmsDynamicProperties;
import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.policy.wsp.Java5EnumTypeMapping;
import com.l7tech.policy.wsp.SimpleTypeMappingFinder;
import com.l7tech.policy.wsp.TypeMapping;
import com.l7tech.search.Dependency;
import com.l7tech.util.GoidUpgradeMapper;

import java.util.Arrays;

import static com.l7tech.objectmodel.ExternalEntityHeader.ValueType.TEXT_ARRAY;
import static com.l7tech.policy.assertion.AssertionMetadata.*;

/**
 * Holds information needed to route a message to an outbound JMS destination.
 */
public class JmsRoutingAssertion extends RoutingAssertionWithSamlSV implements UsesEntities, UsesVariables, SetsVariables {

    /**
     * @return the GOID of the JMS endpoint, or null if there isn't one.
     */
    public Goid getEndpointOid() {
        return endpointGoid;
    }

    /**
     * Set the GOID of the JMS endpoint.  Set this to null if no endpoint is configured.
     * @param endpointGoid  the GOID of a JmsEndpoint instance, or null.
     */

    public void setEndpointOid(Goid endpointGoid) {
        this.endpointGoid = endpointGoid;
    }

    // For backward compat while parsing pre-GOID policies.  Not needed for new assertions.
    @Deprecated
    public void setEndpointOid(Long endpointOid) {
        endpointGoid = GoidUpgradeMapper.mapOid(EntityType.JMS_ENDPOINT, endpointOid);
    }

    /**
     * The name of the { JmsEndpoint}
     * @return the name of this endpoint if known, for cosmetic purposes only. 
     */
    public String getEndpointName() {
        return endpointName;
    }

    /**
     * The name of the { JmsEndpoint}.
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

    @Deprecated
    public void setResponseTimeout (int responseTimeout)
    {
        this.responseTimeout = Integer.toString(responseTimeout);
    }

    /**
     * @return set of rules for propagating request JMS message properties
     * @since SecureSpan 4.0
     */
    public JmsMessagePropertyRuleSet getRequestJmsMessagePropertyRuleSet() {
        return requestJmsMessagePropertyRuleSet;
    }

    /**
     * Set the rules for propagating request JMS message properties.
     * @param ruleSet   rules for propagating request JMS message properties
     * @since SecureSpan 4.0
     */
    public void setRequestJmsMessagePropertyRuleSet(JmsMessagePropertyRuleSet ruleSet) {
        requestJmsMessagePropertyRuleSet = ruleSet;
    }

    /**
     * @return set of rules for propagating response JMS message properties
     * @since SecureSpan 4.0
     */
    public JmsMessagePropertyRuleSet getResponseJmsMessagePropertyRuleSet() {
        return responseJmsMessagePropertyRuleSet;
    }

    /**
     * Set the rules for propagating response JMS message properties.
     * @param ruleSet   rules for propagating response JMS message properties
     * @since SecureSpan 4.0
     */
    public void setResponseJmsMessagePropertyRuleSet(JmsMessagePropertyRuleSet ruleSet) {
        responseJmsMessagePropertyRuleSet = ruleSet;
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

    public JmsDeliveryMode getRequestDeliveryMode() {
        return requestDeliveryMode;
    }

    public void setRequestDeliveryMode( final JmsDeliveryMode requestDeliveryMode ) {
        this.requestDeliveryMode = requestDeliveryMode;
    }

    public String getRequestPriority() {
        return requestPriority;
    }

    public void setRequestPriority( final String requestPriority ) {
        this.requestPriority = requestPriority;
    }

    public String getRequestTimeToLive() {
        return requestTimeToLive;
    }

    public void setRequestTimeToLive( final String requestTimeToLive ) {
        this.requestTimeToLive = requestTimeToLive;
    }

    public String getResponseSize(){
        return responseSize;
    }

    public void setResponseSize(String responseSize){
        this.responseSize = responseSize;
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

    @Override
    @Migration(mapName = MigrationMappingSelection.REQUIRED, resolver = PropertyResolver.Type.ASSERTION)
    public EntityHeader[] getEntitiesUsed() {
        if(endpointGoid != null ) {
            return new JmsEndpointHeader[] { new JmsEndpointHeader(endpointGoid.toString(), endpointName, null, -1, false)}; // always outgoing
        } else {
            return new JmsEndpointHeader[0];
        }
    }

    @Override
    public void replaceEntity(EntityHeader oldEntityHeader, EntityHeader newEntityHeader) {
        if(oldEntityHeader.getType().equals(EntityType.JMS_ENDPOINT) && endpointGoid != null &&
                oldEntityHeader.getGoid().equals(endpointGoid) && newEntityHeader.getType().equals(EntityType.JMS_ENDPOINT))
        {
            endpointGoid = newEntityHeader.getGoid();
            endpointName = newEntityHeader.getName();
        }
    }

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        meta.put(PALETTE_FOLDERS, new String[]{"routing"});

        meta.put(SHORT_NAME, "Route via JMS");
        meta.put(DESCRIPTION, "The incoming message will be routed via JMS to the protected service.");

        meta.put(PALETTE_NODE_ICON, "com/l7tech/console/resources/server16.gif");

        meta.put(PROPERTIES_ACTION_CLASSNAME, "com.l7tech.console.action.JmsRoutingAssertionPropertiesAction");
        meta.put(PROPERTIES_ACTION_NAME, "JMS Routing Properties");

        meta.put(WSP_SUBTYPE_FINDER, new SimpleTypeMappingFinder(Arrays.<TypeMapping>asList(
            new Java5EnumTypeMapping(JmsDeliveryMode.class, "jmsDeliveryMode")
        )));

        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }

    @Migration(mapName = MigrationMappingSelection.NONE, mapValue = MigrationMappingSelection.REQUIRED, export = false, valueType = TEXT_ARRAY, resolver = PropertyResolver.Type.SERVER_VARIABLE)
    @Override
    public String[] getVariablesUsed() {
        return requestTarget.getMessageTargetVariablesUsed().withExpressions(
                dynamicJmsRoutingProperties != null ? dynamicJmsRoutingProperties.getVariableExpressions() : null
        ).withExpressions(
                requestPriority,
                requestTimeToLive,
                responseTimeout,
                responseSize
        ).asArray();
    }

    @Override
    public VariableMetadata[] getVariablesSet() {
        return responseTarget.getMessageTargetVariablesSet().asArray();
    }

    @Override
    public Object clone() {
        JmsRoutingAssertion copy = (JmsRoutingAssertion) super.clone();
        copy.requestTarget = new MessageTargetableSupport(requestTarget);
        copy.responseTarget = new MessageTargetableSupport(responseTarget);
        return copy;
    }

    @Dependency(searchObject = true)
    public JmsDynamicProperties getDynamicJmsRoutingProperties() {
        return dynamicJmsRoutingProperties;
    }

    public void setDynamicJmsRoutingProperties(JmsDynamicProperties dynamicJmsRoutingProperties) {
        this.dynamicJmsRoutingProperties = dynamicJmsRoutingProperties;
    }

    private static final String META_INITIALIZED = JmsRoutingAssertion.class.getName() + ".metadataInitialized";

    private Goid endpointGoid = null;
    private String endpointName = null;
    private JmsMessagePropertyRuleSet requestJmsMessagePropertyRuleSet;
    private JmsMessagePropertyRuleSet responseJmsMessagePropertyRuleSet;
    private JmsDynamicProperties dynamicJmsRoutingProperties;

    private MessageTargetableSupport requestTarget = new MessageTargetableSupport(TargetMessageType.REQUEST, false);
    private MessageTargetableSupport responseTarget = new MessageTargetableSupport(TargetMessageType.RESPONSE, true);
    private JmsDeliveryMode requestDeliveryMode;
    private String requestPriority;
    private String requestTimeToLive;
    private String responseTimeout = null;
    private String responseSize = null;
}
