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

import static com.l7tech.policy.assertion.AssertionMetadata.*;

/**
 * Holds information needed to route a message to an outbound JMS destination.
 */
public class JmsRoutingAssertion extends RoutingAssertion implements UsesVariables{
    public static final int DEFAULT_TIMEOUT = 10000;

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
     * The name of the {@link com.l7tech.gateway.common.transport.jms.JmsEndpoint}.
     * @return the name of this endpoint if known, for cosmetic purposes only. 
     */
    public String getEndpointName() {
        return endpointName;
    }

    /**
     * The name of the {@link com.l7tech.gateway.common.transport.jms.JmsEndpoint}.
     * @param endpointName the name of this endpoint if known, for cosmetic purposes only.
     */
    public void setEndpointName(String endpointName) {
        this.endpointName = endpointName;
    }

    /**
     * The time, in milliseconds, that the SSG should wait for a response from the protected service.
     * After this timeout has lapsed, the request fails.
     *
     * Defaults to {@link JmsRoutingAssertion#DEFAULT_TIMEOUT}.
     * @return the response timeout (in milliseconds)
     */
    public int getResponseTimeout() {
        return responseTimeout;
    }

    /**
     * The time, in milliseconds, that the SSG should wait for a response from the protected service.
     * After this timeout has lapsed, the request fails.
     *
     * Defaults to {@link JmsRoutingAssertion#DEFAULT_TIMEOUT}.
     * @param responseTimeout the response timeout (in milliseconds)
     */
    public void setResponseTimeout( int responseTimeout ) {
        this.responseTimeout = responseTimeout;
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

    @Migration(mapName = MigrationMappingSelection.REQUIRED, resolver = PropertyResolver.Type.ASSERTION)
    public EntityHeader[] getEntitiesUsed() {
        if(endpointOid != null) {
            return new JmsEndpointHeader[] { new JmsEndpointHeader(endpointOid.toString(), endpointName, null, -1, false)}; // always outgoing
        } else {
            return new JmsEndpointHeader[0];
        }
    }

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

    public String[] getVariablesUsed() {
        if (dynamicJmsRoutingProperties != null) {
            String vars = dynamicJmsRoutingProperties.getFieldsAsVariables();

            if (vars != null && !vars.equals(""))
                return Syntax.getReferencedNames(vars);
        }
        return new String[0];
    }

    public JmsDynamicProperties getDynamicJmsRoutingProperties() {
        return dynamicJmsRoutingProperties;
    }

    public void setDynamicJmsRoutingProperties(JmsDynamicProperties dynamicJmsRoutingProperties) {
        this.dynamicJmsRoutingProperties = dynamicJmsRoutingProperties;
    }

    private Long endpointOid = null;
    private String endpointName = null;
    private int responseTimeout = DEFAULT_TIMEOUT;
    private JmsMessagePropertyRuleSet requestJmsMessagePropertyRuleSet;
    private JmsMessagePropertyRuleSet responseJmsMessagePropertyRuleSet;
    private JmsDynamicProperties dynamicJmsRoutingProperties;
}
