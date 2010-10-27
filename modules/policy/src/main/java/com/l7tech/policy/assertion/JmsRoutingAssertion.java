/*
 * Copyright (C) 2003-2006 Layer 7 Technologies Inc.
 */

package com.l7tech.policy.assertion;

import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.JmsEndpointHeader;
import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.objectmodel.migration.MigrationMappingSelection;
import com.l7tech.objectmodel.migration.PropertyResolver;
import com.l7tech.policy.JmsDynamicProperties;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.policy.variable.VariableMetadata;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static com.l7tech.objectmodel.ExternalEntityHeader.ValueType.TEXT_ARRAY;
import static com.l7tech.policy.assertion.AssertionMetadata.*;

/**
 * Holds information needed to route a message to an outbound JMS destination.
 */
public class JmsRoutingAssertion extends RoutingAssertion implements UsesEntities, UsesVariables, SetsVariables {

    /**
     * @return the OID of the JMS endpoint, or null if there isn't one.
     */
    public Long getEndpointOid() {
        return endpointOid;
    }

    /**
     * Set the OID of the JMS endpoint.  Set this to null if no endpoint is configured.
     * @param endpointOid  the OID of a JmsEndpoint instance, or null.
     */
    public void setEndpointOid(Long endpointOid) {
        this.endpointOid = endpointOid;
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
        if(endpointOid != null) {
            return new JmsEndpointHeader[] { new JmsEndpointHeader(endpointOid.toString(), endpointName, null, -1, false)}; // always outgoing
        } else {
            return new JmsEndpointHeader[0];
        }
    }

    @Override
    public void replaceEntity(EntityHeader oldEntityHeader, EntityHeader newEntityHeader) {
        if(oldEntityHeader.getType().equals(EntityType.JMS_ENDPOINT) && endpointOid != null &&
                oldEntityHeader.getOid() == endpointOid && newEntityHeader.getType().equals(EntityType.JMS_ENDPOINT))
        {
            endpointOid = newEntityHeader.getOid();
            endpointName = newEntityHeader.getName();
        }
    }

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = defaultMeta();

        meta.put(PALETTE_FOLDERS, new String[]{"routing"});

        meta.put(SHORT_NAME, "Route via JMS");
        meta.put(DESCRIPTION, "The incoming message will be routed via JMS to the protected service.");

        meta.put(PALETTE_NODE_ICON, "com/l7tech/console/resources/server16.gif");

        meta.put(PROPERTIES_ACTION_CLASSNAME, "com.l7tech.console.action.JmsRoutingAssertionPropertiesAction");
        meta.put(PROPERTIES_ACTION_NAME, "JMS Routing Properties");
        
        return meta;
    }

    @Migration(mapName = MigrationMappingSelection.NONE, mapValue = MigrationMappingSelection.REQUIRED, export = false, valueType = TEXT_ARRAY, resolver = PropertyResolver.Type.SERVER_VARIABLE)
    @Override
    public String[] getVariablesUsed() {
        Set<String> vars = new HashSet<String>();

        if (dynamicJmsRoutingProperties != null) {
            String dynamicVars = dynamicJmsRoutingProperties.getFieldsAsVariables();

            if (dynamicVars != null && !dynamicVars.equals(""))
                vars.addAll(Arrays.asList(Syntax.getReferencedNames(dynamicVars)));
        }

        if (responseTimeout != null) {
            vars.addAll(Arrays.asList(Syntax.getReferencedNames(responseTimeout)));
        }
        vars.addAll(Arrays.asList(requestTarget.getVariablesUsed()));
        return vars.toArray(new String[vars.size()]);
    }

    @Override
    public VariableMetadata[] getVariablesSet() {
        return responseTarget.getVariablesSet();
    }

    @Override
    public Object clone() {
        JmsRoutingAssertion copy = (JmsRoutingAssertion) super.clone();
        copy.requestTarget = new MessageTargetableSupport(requestTarget);
        copy.responseTarget = new MessageTargetableSupport(responseTarget);
        return copy;
    }

    public JmsDynamicProperties getDynamicJmsRoutingProperties() {
        return dynamicJmsRoutingProperties;
    }

    public void setDynamicJmsRoutingProperties(JmsDynamicProperties dynamicJmsRoutingProperties) {
        this.dynamicJmsRoutingProperties = dynamicJmsRoutingProperties;
    }

    private Long endpointOid = null;
    private String endpointName = null;
    private String responseTimeout = null;
    private JmsMessagePropertyRuleSet requestJmsMessagePropertyRuleSet;
    private JmsMessagePropertyRuleSet responseJmsMessagePropertyRuleSet;
    private JmsDynamicProperties dynamicJmsRoutingProperties;

    private MessageTargetableSupport requestTarget = new MessageTargetableSupport(TargetMessageType.REQUEST, false);
    private MessageTargetableSupport responseTarget = new MessageTargetableSupport(TargetMessageType.RESPONSE, true);
}
