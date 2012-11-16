package com.l7tech.external.assertions.mqnative;

import com.l7tech.console.util.Registry;
import com.l7tech.external.assertions.mqnative.server.MqNativeAdminServerSupport;
import com.l7tech.gateway.common.transport.SsgActiveConnector;
import com.l7tech.gateway.common.transport.TransportAdmin;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.objectmodel.migration.MigrationMappingSelection;
import com.l7tech.objectmodel.migration.PropertyResolver;
import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.PolicyValidatorResult;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.validator.AssertionValidator;
import com.l7tech.policy.validator.PolicyValidationContext;
import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.policy.wsp.BeanTypeMapping;
import com.l7tech.policy.wsp.Java5EnumTypeMapping;
import com.l7tech.policy.wsp.SimpleTypeMappingFinder;
import com.l7tech.policy.wsp.TypeMapping;
import com.l7tech.server.util.EntityUseUtils.EntityTypeOverride;
import com.l7tech.server.util.EntityUseUtils.EntityUse;
import com.l7tech.util.CollectionUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

import static com.l7tech.policy.assertion.AssertionMetadata.*;
import static com.l7tech.policy.assertion.VariableUseSupport.expressions;
import static com.l7tech.policy.assertion.VariableUseSupport.variables;

/**
 * Route outbound MQ Native to WebSphere MQ.
 */
public class MqNativeRoutingAssertion extends RoutingAssertion implements UsesEntities, UsesVariables, SetsVariables {
    private static final String routePrefix = "Route";
    private static final String getPrefix = "Get";
    private static final String baseName = " via MQ Native";

    private static final AssertionNodeNameFactory policyNameFactory = new AssertionNodeNameFactory<MqNativeRoutingAssertion>(){
        @Override
        public String getAssertionName( final MqNativeRoutingAssertion assertion, final boolean decorate ) {
            String prefix = assertion.isPutToQueue() ? routePrefix : getPrefix;
            if(!decorate || assertion.getSsgActiveConnectorName()==null) return prefix + baseName;
            return prefix + baseName + " Queue " + assertion.getSsgActiveConnectorName();
        }
    };
    private static final String META_INITIALIZED = MqNativeRoutingAssertion.class.getName() + ".metadataInitialized";

    @Nullable
    private Long ssgActiveConnectorId;
    private String ssgActiveConnectorName;
    private String responseTimeout;
    private String responseSize;
    private boolean isPutToQueue = true; // Default: set the message direction to "Put to Queue"
    private MqNativeDynamicProperties dynamicMqRoutingProperties;
    @NotNull
    private MqNativeMessageHeaderType requestMqHeaderType = defaultMqMessageHeaderType();
    @NotNull
    private MqNativeMessageHeaderType responseMqHeaderType = defaultMqMessageHeaderType();
    @Nullable
    private Map<String,String> requestMessageDescriptorOverrides = new HashMap<String, String>(0);
    @Nullable
    private Map<String,String> requestMessagePropertyOverrides = new HashMap<String, String>(0);
    @Nullable
    private Map<String,String> responseMessageDescriptorOverrides = new HashMap<String, String>(0);
    @Nullable
    private Map<String,String> responseMessagePropertyOverrides = new HashMap<String, String>(0);
    @NotNull
    private MqNativeMessagePropertyRuleSet requestMqMessagePropertyRuleSet = defaultMqNativeMessagePropertyRuleSet();
    @NotNull
    private MqNativeMessagePropertyRuleSet responseMqMessagePropertyRuleSet = defaultMqNativeMessagePropertyRuleSet();
    @NotNull
    private MessageTargetableSupport requestTarget = defaultRequestTarget();
    @NotNull
    private MessageTargetableSupport responseTarget = defaultResponseTarget();

    private MqNativeMessagePropertyRuleSet defaultMqNativeMessagePropertyRuleSet() {
        MqNativeMessagePropertyRuleSet mqNativeMessagePropertyRuleSet = new MqNativeMessagePropertyRuleSet();
        mqNativeMessagePropertyRuleSet.setPassThroughMqMessageDescriptors(true);
        mqNativeMessagePropertyRuleSet.setPassThroughMqMessageHeaders(true);
        mqNativeMessagePropertyRuleSet.setPassThroughMqMessageProperties(true);
        return mqNativeMessagePropertyRuleSet;
    }

    private static MessageTargetableSupport defaultRequestTarget() {
        return new MessageTargetableSupport( TargetMessageType.REQUEST, false);
    }

    private static MessageTargetableSupport defaultResponseTarget() {
        return new MessageTargetableSupport( TargetMessageType.RESPONSE, true);
    }

    private static MqNativeMessageHeaderType defaultMqMessageHeaderType() {
        return MqNativeMessageHeaderType.ORIGINAL;
    }

    /**
     * Get the OID of the associated SsgActiveConnector.
     *
     * @return the OID of the connector, or null.
     */
    @Nullable
    public Long getSsgActiveConnectorId() {
        return ssgActiveConnectorId;
    }

    /**
     * Set the OID for the associated SsgActiveConnector.
     *
     * @param ssgActiveConnectorId the OID, or null.
     */
    public void setSsgActiveConnectorId( @Nullable final Long ssgActiveConnectorId ) {
        this.ssgActiveConnectorId = ssgActiveConnectorId;
    }

    /**
     * Get the name of the associated SsgActiveConnector.
     *
     * @return the name of the associated endpoint if known, for cosmetic purposes only.
     */
    @Nullable
    public String getSsgActiveConnectorName() {
        return ssgActiveConnectorName;
    }

    /**
     * Set the name of the associated SsgActiveConnector.
     *
     * @param ssgActiveConnectorName the name of the associated endpoint.
     */
    public void setSsgActiveConnectorName( @Nullable final String ssgActiveConnectorName ) {
        this.ssgActiveConnectorName = ssgActiveConnectorName;
    }

    /**
     * Get the response timeout (milliseconds)
     *
     * <p>After this timeout has lapsed, the request fails. Defaults to the
     * value of the "io.mqResponseTimeout" cluster property.</p>
     *
     * @return the response timeout (in milliseconds), or null to use the default
     */
    @Nullable
    public String getResponseTimeout() {
        return responseTimeout;
    }

    /**
     * Set the response timeout (milliseconds)
     *
     * @param responseTimeout the response timeout, null to use the system default.
     */
    public void setResponseTimeout( @Nullable String responseTimeout ) {
        this.responseTimeout = responseTimeout;
    }

    /**
     * Get the maximum response size in bytes.
     *
     * @return The size in bytes, or null to use the system default value..
     */
    @Nullable
    public String getResponseSize(){
        return responseSize;
    }

    /**
     * Set the maximum response size in bytes.
     *
     * @param responseSize The maximum size, null to use the system default.
     */
    public void setResponseSize( @Nullable final String responseSize){
        this.responseSize = responseSize;
    }

    /**
     * Get if the message direction is set to "Put to Queue" or "Get from Queue".
     *
     * @return true if the message direction is set to "Put to Queue".
     */
    public boolean isPutToQueue() {
        return isPutToQueue;
    }

    /**
     * Set if the message direction is set to "Put to Queue" or "Get from Queue".
     *
     * @param putToQueue: an indicate to show if the message direction is set to "Put to Queue" or "Get from Queue".
     */
    public void setPutToQueue(boolean putToQueue) {
        isPutToQueue = putToQueue;
    }

    /**
     * Get the dynamic routing properties.
     *
     * @return The dynamic properties or null.
     */
    @Nullable
    public MqNativeDynamicProperties getDynamicMqRoutingProperties() {
        return dynamicMqRoutingProperties;
    }

    /**
     * Set the dynamic routing properties.
     *
     * @param dynamicMqRoutingProperties The dynamic properties to use
     */
    public void setDynamicMqRoutingProperties( @Nullable final MqNativeDynamicProperties dynamicMqRoutingProperties ) {
        this.dynamicMqRoutingProperties = dynamicMqRoutingProperties;
    }

    @NotNull
    public MqNativeMessageHeaderType getRequestMqHeaderType() {
        return requestMqHeaderType;
    }

    public void setRequestMqHeaderType(@NotNull MqNativeMessageHeaderType requestMqHeaderType) {
        this.requestMqHeaderType = requestMqHeaderType;
    }

    @NotNull
    public MqNativeMessageHeaderType getResponseMqHeaderType() {
        return responseMqHeaderType;
    }

    public void setResponseMqHeaderType(@NotNull MqNativeMessageHeaderType responseMqHeaderType) {
        this.responseMqHeaderType = responseMqHeaderType;
    }

    /**
     * Get the message descriptor overrides for the outbound request message.
     *
     * @return The message properties or null.
     */
    @Nullable
    public Map<String, String> getRequestMessageDescriptorOverrides() {
        return requestMessageDescriptorOverrides;
    }

    /**
     * Set the message descriptor overrides for the outbound request message.
     *
     * @param requestMessageDescriptorOverrides The properties to use.
     */
    public void setRequestMessageDescriptorOverrides(@Nullable final Map<String, String> requestMessageDescriptorOverrides) {
        this.requestMessageDescriptorOverrides = requestMessageDescriptorOverrides;
    }

    /**
     * Get the message descriptor overrides for the outbound response message.
     * @return The message properties or null.
     */
    @Nullable
    public Map<String, String> getResponseMessageDescriptorOverrides() {
        return responseMessageDescriptorOverrides;
    }

    /**
     * Set the message descriptor overrides for the outbound response message.
     * @param responseMessageDescriptorOverrides The properties to use.
     */
    public void setResponseMessageDescriptorOverrides(@Nullable Map<String, String> responseMessageDescriptorOverrides) {
        this.responseMessageDescriptorOverrides = responseMessageDescriptorOverrides;
    }

    /**
     * Get the message property overrides for the outbound request message.
     * @return
     */
    @Nullable
    public Map<String, String> getRequestMessagePropertyOverrides() {
        return requestMessagePropertyOverrides;
    }

    /**
     * Set the message property overrides for the outbound request message.
     * @param requestMessagePropertyOverrides
     */
    public void setRequestMessagePropertyOverrides(@Nullable Map<String, String> requestMessagePropertyOverrides) {
        this.requestMessagePropertyOverrides = requestMessagePropertyOverrides;
    }

    /**
     * Get the message property overrides for the outbound response message.
     * @return
     */
    @Nullable
    public Map<String, String> getResponseMessagePropertyOverrides() {
        return responseMessagePropertyOverrides;
    }

    /**
     * Set the message property overrides for the outbound response message.
     * @param responseMessagePropertyOverrides
     */
    public void setResponseMessagePropertyOverrides(@Nullable Map<String, String> responseMessagePropertyOverrides) {
        this.responseMessagePropertyOverrides = responseMessagePropertyOverrides;
    }

    /**
     * Get the rules for propagation of outbound request headers.
     *
     * @return The outbound request propagation rules.
     */
    @NotNull
    public MqNativeMessagePropertyRuleSet getRequestMqNativeMessagePropertyRuleSet() {
        return requestMqMessagePropertyRuleSet;
    }

    /**
     * Set the rules for propagation of outbound request headers.
     *
     * @param ruleSet The rules to use, null to set to the default rules
     */
    public void setRequestMqNativeMessagePropertyRuleSet( @Nullable final MqNativeMessagePropertyRuleSet ruleSet) {
        requestMqMessagePropertyRuleSet = ruleSet==null ? new MqNativeMessagePropertyRuleSet() : ruleSet;
    }

    /**
     * Get the rules for propagation of inbound response headers.
     *
     * @return The inbound response propagation rules.
     */
    @NotNull
    public MqNativeMessagePropertyRuleSet getResponseMqNativeMessagePropertyRuleSet() {
        return responseMqMessagePropertyRuleSet;
    }

    /**
     * Set the rules for propagation of inbound response headers.
     *
     * @param ruleSet The rules to use, null to set to the default rules
     */
    public void setResponseMqNativeMessagePropertyRuleSet( @Nullable final MqNativeMessagePropertyRuleSet ruleSet) {
        responseMqMessagePropertyRuleSet = ruleSet==null ? new MqNativeMessagePropertyRuleSet() : ruleSet;
    }

    /**
     * Get the outbound request message configuration.
     *
     * @return The configuration for the outbound request.
     */
    @NotNull
    public MessageTargetableSupport getRequestTarget() {
        return requestTarget;
    }

    /**
     * Set the outbound request message configuration.
     *
     * @return The configuration for the outbound request, null to reset to the default.
     */
    public void setRequestTarget( @Nullable final MessageTargetableSupport requestTarget ) {
        this.requestTarget = requestTarget==null ? defaultRequestTarget() : requestTarget;
    }

    /**
     * Get the inbound response message configuration.
     *
     * @return The configuration for the inbound response.
     */
    @NotNull
    public MessageTargetableSupport getResponseTarget() {
        return responseTarget;
    }

    /**
     * Set the inbound response message configuration.
     *
     * @return The configuration for the inbound response, null to reset to the default.
     */
    public void setResponseTarget( @Nullable final MessageTargetableSupport responseTarget ) {
        this.responseTarget = responseTarget==null ? defaultResponseTarget() : responseTarget;
    }

    @Override
    public boolean initializesRequest() {
        /*
           when responding, the route assertion initializes the Request target if:
              - the route is Get from Queue and target is Request
              - or the route is Put to Queue and reads reply queue to Request
        */
        boolean getFromQueueWritesToRequest = !isPutToQueue() && TargetMessageType.REQUEST == responseTarget.getTarget();
        boolean putToQueueReadsReplyWritesToRequest = isPutToQueue() && TargetMessageType.REQUEST == responseTarget.getTarget();
        return  getFromQueueWritesToRequest || putToQueueReadsReplyWritesToRequest;
    }

    @Override
    public boolean needsInitializedRequest() {
        // when requesting, the route assertion needs an initialized Request target if the route is Put to Queue and target is Request
        return isPutToQueue() && TargetMessageType.REQUEST == requestTarget.getTarget();
    }

    @Override
    public boolean initializesResponse() {
        /*
           when responding, the route assertion initializes the Response target if:
              - the route is Get from Queue and writes to Response
              - or the route is Put to Queue and reads reply queue and writes to Response
         */
        boolean getFromQueueWritesToResponse = !isPutToQueue() && TargetMessageType.RESPONSE == responseTarget.getTarget();
        boolean putToQueueReadsReplyWritesToResponse = isPutToQueue() && TargetMessageType.RESPONSE == responseTarget.getTarget();
        return getFromQueueWritesToResponse || putToQueueReadsReplyWritesToResponse;
    }

    @Override
    public boolean needsInitializedResponse() {
        // when requesting, the route assertion needs an initialized Response target if the route is Put to Queue and target is Response
        return isPutToQueue() && TargetMessageType.RESPONSE == requestTarget.getTarget();
    }

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        meta.put( SHORT_NAME, routePrefix + baseName );
        meta.put( DESCRIPTION, "The incoming message will be routed via MQ Native to the protected service." );
        meta.put( PALETTE_FOLDERS, new String[] { "routing" } );
        meta.put( PALETTE_NODE_ICON, "com/l7tech/console/resources/server16.gif" );
        meta.put( POLICY_ADVICE_CLASSNAME, "auto" );
        meta.put( POLICY_NODE_NAME_FACTORY, policyNameFactory );
        meta.put( GLOBAL_ACTION_CLASSNAMES, new String[] { "com.l7tech.external.assertions.mqnative.console.MqNativeCustomAction" } );
        meta.put( PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.external.assertions.mqnative.console.MqNativeRoutingAssertionDialog" );
        meta.put( MODULE_LOAD_LISTENER_CLASSNAME, "com.l7tech.external.assertions.mqnative.server.MqNativeModuleLoadListener" );
        meta.put( POLICY_VALIDATOR_CLASSNAME, Validator.class.getName());

        // fix bug #12529: logged message(s) not showing in SOAP fault
        try {
            Class.forName("com.ibm.mq.MQException", false, MqNativeAdminServerSupport.class.getClassLoader());
            meta.put( SERVER_ASSERTION_CLASSNAME, "com.l7tech.external.assertions.mqnative.server.ServerMqNativeRoutingAssertion");
        } catch (ClassNotFoundException e) {
            // required jars not installed
            meta.put( SERVER_ASSERTION_CLASSNAME, "com.l7tech.external.assertions.mqnative.server.DelegatingServerMqNativeRoutingAssertion");
        }

        meta.put( PROPERTIES_ACTION_NAME, "MQ Native Routing Properties" );
        meta.put( FEATURE_SET_NAME, "(fromClass)" );

        meta.put( WSP_SUBTYPE_FINDER, new SimpleTypeMappingFinder( CollectionUtils.<TypeMapping>list(
                new BeanTypeMapping( MqNativeMessagePropertyRuleSet.class, "mappingRuleSet" ),
                new BeanTypeMapping( MqNativeDynamicProperties.class, "mqDynamicProperties" ),
                new Java5EnumTypeMapping( MqNativeMessageHeaderType.class, "mqMessageHeaderType" )
        ) ));

        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }

    public static class Validator implements AssertionValidator {

        private enum ActiveConnectorStatus {
            ENABLED,
            NOT_FOUND,
            DISABLED
        };

        MqNativeRoutingAssertion assertion;
        ActiveConnectorStatus activeConnectorStatus;
        boolean isQueueNameValid;
        boolean isReplyToQueueNameValid;

        public Validator(MqNativeRoutingAssertion assertion) {

            this.assertion = assertion;
            SsgActiveConnector activeConnector;
            TransportAdmin ta = Registry.getDefault().getTransportAdmin();
            try {
                activeConnector = ta.findSsgActiveConnectorByPrimaryKey(assertion.getSsgActiveConnectorId());
                if ( activeConnector == null ) {
                    activeConnectorStatus = ActiveConnectorStatus.NOT_FOUND;
                } else  if ( activeConnector.isEnabled() ) {
                    activeConnectorStatus = ActiveConnectorStatus.ENABLED;
                } else {
                    activeConnectorStatus = ActiveConnectorStatus.DISABLED;
                }

                String mqQueueName = activeConnector.getProperty(SsgActiveConnector.PROPERTIES_KEY_MQ_NATIVE_TARGET_QUEUE_NAME);
                String replyToQueueName = activeConnector.getProperty(SsgActiveConnector.PROPERTIES_KEY_MQ_NATIVE_SPECIFIED_REPLY_QUEUE_NAME);
                MqNativeDynamicProperties dynamicProperties = assertion.getDynamicMqRoutingProperties();

                if ( dynamicProperties != null ) {
                    String dynamicQueueName = dynamicProperties.getQueueName();
                    String dynamicReplyToQueueName = dynamicProperties.getReplyToQueue();

                    if ( dynamicQueueName != null && !dynamicQueueName.isEmpty()) {
                        mqQueueName = dynamicQueueName;
                    }

                    if ( dynamicReplyToQueueName != null
                              && !dynamicReplyToQueueName.isEmpty()
                              && activeConnector.getProperty(SsgActiveConnector.PROPERTIES_KEY_MQ_NATIVE_REPLY_TYPE).equals(MqNativeReplyType.REPLY_SPECIFIED_QUEUE.toString())) {
                        replyToQueueName = dynamicReplyToQueueName;
                    }
                }

                isQueueNameValid = mqQueueName != null && !mqQueueName.isEmpty();
                isReplyToQueueNameValid = ( !activeConnector.getProperty(SsgActiveConnector.PROPERTIES_KEY_MQ_NATIVE_REPLY_TYPE).equals(MqNativeReplyType.REPLY_SPECIFIED_QUEUE.toString()) )
                                            || ( replyToQueueName != null && !replyToQueueName.isEmpty() );

            } catch (FindException e) {
                activeConnectorStatus = ActiveConnectorStatus.NOT_FOUND;
            }
    }

        @Override
        public void validate(AssertionPath path, PolicyValidationContext pvc, PolicyValidatorResult result) {

            switch (activeConnectorStatus) {
                case ENABLED:
                    break;
                case NOT_FOUND:
                    result.addWarning(new PolicyValidatorResult.Warning(assertion,"Destination Queue set to unknown MQ Native Queue",null));
                    break;
                case DISABLED:
                    result.addWarning(new PolicyValidatorResult.Warning(assertion,"Destination Queue is disabled",null));
                    break;
            }

            if ( ! isQueueNameValid ) {
                result.addWarning(new PolicyValidatorResult.Warning(assertion,"Queue Name is not set in either MQ Native Queue Template or Routing Assertion",null));
            }

            if ( ! isReplyToQueueNameValid ) {
                result.addWarning(new PolicyValidatorResult.Warning(assertion,"Reply to Queue Name is not set in either MQ Queue Native Template or Routing Assertion.",null));
            }
        }
    }

    @Override
    @EntityUse(@EntityTypeOverride(type = EntityType.SSG_ACTIVE_CONNECTOR, description = "MQ Native Queue"))
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
                new VariableMetadata( "mq.reason.code", false, true, null, false, DataType.INTEGER ),
                new VariableMetadata( "mq.md", true, false, null, false, DataType.INTEGER ),
                new VariableMetadata( "mq.header", true, false, null, false, DataType.INTEGER ),
                new VariableMetadata( "mq.headernames", false, false, null, false, DataType.INTEGER ),
                new VariableMetadata( "mq.allheadervalues", false, false, null, false, DataType.INTEGER ),
                new VariableMetadata( "mq.property", true, false, null, false, DataType.INTEGER ),
                new VariableMetadata( "mq.propertynames", false, false, null, false, DataType.INTEGER ),
                new VariableMetadata( "mq.allpropertyvalues", false, false, null, false, DataType.INTEGER )
                // TODO declare variables set
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