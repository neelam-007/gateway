package com.l7tech.external.assertions.mqnative;

import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.objectmodel.migration.MigrationMappingSelection;
import com.l7tech.objectmodel.migration.PropertyResolver;
import com.l7tech.policy.assertion.*;
import static com.l7tech.policy.assertion.AssertionMetadata.*;
import static com.l7tech.policy.assertion.VariableUseSupport.expressions;
import static com.l7tech.policy.assertion.VariableUseSupport.variables;
import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.policy.wsp.BeanTypeMapping;
import com.l7tech.policy.wsp.SimpleTypeMappingFinder;
import com.l7tech.policy.wsp.TypeMapping;
import com.l7tech.util.CollectionUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Route outbound MQ Native to WebSphere MQ.
 */
public class MqNativeRoutingAssertion extends RoutingAssertion implements UsesEntities, UsesVariables, SetsVariables {
    private static final String baseName = "Route via MQ Native";
    private static final AssertionNodeNameFactory policyNameFactory = new AssertionNodeNameFactory<MqNativeRoutingAssertion>(){
        @Override
        public String getAssertionName( final MqNativeRoutingAssertion assertion, final boolean decorate ) {
            if(!decorate || assertion.getSsgActiveConnectorName()==null) return baseName;
            return baseName + " Queue " + assertion.getSsgActiveConnectorName();
        }
    };
    private static final String META_INITIALIZED = MqNativeRoutingAssertion.class.getName() + ".metadataInitialized";

    private Long ssgActiveConnectorId;
    private String ssgActiveConnectorName;
    private String responseTimeout;
    private String responseSize;
    private MqNativeDynamicProperties dynamicMqRoutingProperties;
    @NotNull
    private MqNativeMessagePropertyRuleSet requestMqMessagePropertyRuleSet = new MqNativeMessagePropertyRuleSet();
    @NotNull
    private MqNativeMessagePropertyRuleSet responseMqMessagePropertyRuleSet = new MqNativeMessagePropertyRuleSet();
    @NotNull
    private MessageTargetableSupport requestTarget = defaultRequestTarget();
    @NotNull
    private MessageTargetableSupport responseTarget = defaultResponseTarget();

    private static MessageTargetableSupport defaultResponseTarget() {
        return new MessageTargetableSupport( TargetMessageType.RESPONSE, true);
    }

    /**
     * @return the ResId of the MQ Resource Type, or null if there isn't one.
     */
    public Long getSsgActiveConnectorId() {
        return ssgActiveConnectorId;
    }

    /**
     * Set the ResId of the MQ Resource Type.  Set this to null if no endpoint is configured.
     * @param ssgActiveConnectorId  the ResId of a MqResourceType instance, or null.
     */
    public void setSsgActiveConnectorId( Long ssgActiveConnectorId ) {
        this.ssgActiveConnectorId = ssgActiveConnectorId;
    }

    /**
     * The name of the { MqResourceType }
     * @return the name of this endpoint if known, for cosmetic purposes only.
     */
    public String getSsgActiveConnectorName() {
        return ssgActiveConnectorName;
    }

    /**
     * The name of the { MqResourceType}.
     * @param ssgActiveConnectorName the name of this endpoint if known, for cosmetic purposes only.
     */
    public void setSsgActiveConnectorName( String ssgActiveConnectorName ) {
        this.ssgActiveConnectorName = ssgActiveConnectorName;
    }

    /**
     * The time, in milliseconds, that the SSG should wait for a response from the protected service.
     * After this timeout has lapsed, the request fails.
     *
     * Defaults to {ServerConfig.PARAM_JMS_RESPONSE_TIMEOUT}.
     * @return the response timeout (in milliseconds), or null to use the default
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
    public void setResponseTimeout( @Nullable String responseTimeout ) {
        this.responseTimeout = responseTimeout;
    }

    /**
     * The maximum response size in bytes.
     *
     * @return The size in bytes, or null to use the default value..
     */
    public String getResponseSize(){
        return responseSize;
    }

    public void setResponseSize(String responseSize){
        this.responseSize = responseSize;
    }

    /**
     * @return set of rules for propagating request JMS message properties
     */
    @NotNull
    public MqNativeMessagePropertyRuleSet getRequestMqNativeMessagePropertyRuleSet() {
        return requestMqMessagePropertyRuleSet;
    }

    /**
     * Set the rules for propagating request JMS message properties.
     * @param ruleSet   rules for propagating request JMS message properties
     */
    public void setRequestMqNativeMessagePropertyRuleSet( MqNativeMessagePropertyRuleSet ruleSet) {
        requestMqMessagePropertyRuleSet = ruleSet==null ? new MqNativeMessagePropertyRuleSet() : ruleSet;
    }

    /**
     * @return set of rules for propagating response JMS message properties
     */
    @NotNull
    public MqNativeMessagePropertyRuleSet getResponseMqNativeMessagePropertyRuleSet() {
        return responseMqMessagePropertyRuleSet;
    }

    /**
     * Set the rules for propagating response JMS message properties.
     * @param ruleSet   rules for propagating response JMS message properties
     */
    public void setResponseMqNativeMessagePropertyRuleSet( MqNativeMessagePropertyRuleSet ruleSet) {
        responseMqMessagePropertyRuleSet = ruleSet==null ? new MqNativeMessagePropertyRuleSet() : ruleSet;
    }

    @NotNull
    public MessageTargetableSupport getRequestTarget() {
        return requestTarget;
    }

    public void setRequestTarget(MessageTargetableSupport requestTarget) {
        this.requestTarget = requestTarget==null ? defaultRequestTarget() : requestTarget;
    }

    private static MessageTargetableSupport defaultRequestTarget() {
        return new MessageTargetableSupport( TargetMessageType.REQUEST, false);
    }

    @NotNull
    public MessageTargetableSupport getResponseTarget() {
        return responseTarget;
    }

    public void setResponseTarget(MessageTargetableSupport responseTarget) {
        this.responseTarget = responseTarget==null ? defaultResponseTarget() : responseTarget;
    }

    @Override
    public boolean initializesRequest() {
        return TargetMessageType.REQUEST == responseTarget.getTarget();
    }

    @Override
    public boolean needsInitializedRequest() {
        return TargetMessageType.REQUEST == requestTarget.getTarget();
    }

    @Override
    public boolean initializesResponse() {
        return TargetMessageType.RESPONSE == responseTarget.getTarget();
    }

    @Override
    public boolean needsInitializedResponse() {
        return TargetMessageType.RESPONSE == requestTarget.getTarget();
    }

    public MqNativeDynamicProperties getDynamicMqRoutingProperties() {
        return dynamicMqRoutingProperties;
    }

    public void setDynamicMqRoutingProperties( MqNativeDynamicProperties dynamicMqRoutingProperties) {
        this.dynamicMqRoutingProperties = dynamicMqRoutingProperties;
    }

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        meta.put( SHORT_NAME, baseName );
        meta.put( DESCRIPTION, "The incoming message will be routed via MQ to the protected service." );
        meta.put( PALETTE_FOLDERS, new String[] { "routing" } );
        meta.put( PALETTE_NODE_ICON, "com/l7tech/console/resources/server16.gif" );
        meta.put( POLICY_ADVICE_CLASSNAME, "auto" );
        meta.put( POLICY_NODE_ICON, "com/l7tech/console/resources/server16.gif" );
        meta.put( POLICY_NODE_NAME_FACTORY, policyNameFactory );
        meta.put( GLOBAL_ACTION_CLASSNAMES, new String[] { "com.l7tech.external.assertions.mqnative.console.MqNativeCustomAction" } );
        meta.put( FEATURE_SET_NAME, "(fromClass)" );
        meta.put( PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.external.assertions.mqnative.console.MqNativeRoutingAssertionDialog" );
        meta.put( MODULE_LOAD_LISTENER_CLASSNAME, "com.l7tech.external.assertions.mqnative.server.MqNativeModuleLoadListener" );
        meta.put( PROPERTIES_ACTION_NAME, "MQ Native Routing Properties" );

        meta.put( WSP_SUBTYPE_FINDER, new SimpleTypeMappingFinder( CollectionUtils.<TypeMapping>list(
                new BeanTypeMapping( MqNativeMessagePropertyRuleSet.class, "mappingRuleSet" ),
                new BeanTypeMapping( MqNativeDynamicProperties.class, "mqDynamicProperties" )
        ) ));

        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }

    @Override
    @Migration(mapName = MigrationMappingSelection.REQUIRED, resolver = PropertyResolver.Type.ASSERTION)
    public EntityHeader[] getEntitiesUsed() {
        if( ssgActiveConnectorId != null) {
            return new EntityHeader[] {new EntityHeader(ssgActiveConnectorId.toString(), EntityType.SSG_ACTIVE_CONNECTOR, ssgActiveConnectorName, null)}; // always outgoing
        } else {
            return new EntityHeader[0];
        }
    }

    @Override
    public void replaceEntity(EntityHeader oldEntityHeader, EntityHeader newEntityHeader) {
        if( oldEntityHeader.getType().equals( EntityType.SSG_ACTIVE_CONNECTOR) && ssgActiveConnectorId != null &&
                oldEntityHeader.getOid() == ssgActiveConnectorId && newEntityHeader.getType().equals(EntityType.SSG_ACTIVE_CONNECTOR))
        {
            ssgActiveConnectorId = newEntityHeader.getOid();
            ssgActiveConnectorName = newEntityHeader.getName();
        }
    }

    @Override
    public String[] getVariablesUsed() {
        return expressions( responseTimeout, responseSize )
                .with( requestTarget.getMessageTargetVariablesUsed() )
                .withExpressions( dynamicMqRoutingProperties == null ? null : dynamicMqRoutingProperties.getVariableExpressions() )
                .asArray();
    }

    @Override
    public VariableMetadata[] getVariablesSet() {
        return variables(
                new VariableMetadata( "mq.completion.code", false, true, null, false, DataType.INTEGER ),
                new VariableMetadata( "mq.reason.code", false, true, null, false, DataType.INTEGER )
        ).withVariables( responseTarget.getVariablesSet() ).asArray();
    }

    @Override
    public Object clone() {
        MqNativeRoutingAssertion copy = (MqNativeRoutingAssertion) super.clone();
        copy.requestTarget = new MessageTargetableSupport(requestTarget);
        copy.responseTarget = new MessageTargetableSupport(responseTarget);
        copy.dynamicMqRoutingProperties = dynamicMqRoutingProperties==null ?
                null :
                new MqNativeDynamicProperties(dynamicMqRoutingProperties);

        return copy;
    }
}
