package com.l7tech.external.assertions.amqpassertion;


import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.objectmodel.migration.MigrationMappingSelection;
import com.l7tech.objectmodel.migration.PropertyResolver;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.search.Dependency;
import com.l7tech.util.GoidUpgradeMapper;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 *
 */
public class RouteViaAMQPAssertion extends RoutingAssertion implements UsesVariables {
    protected static final Logger logger = Logger.getLogger(RouteViaAMQPAssertion.class.getName());

    private Goid ssgActiveConnectorGoid = null;

    private String ssgActiveConnectorName;
    private String routingKeyExpression;
    private MessageTargetableSupport requestTarget = new MessageTargetableSupport(TargetMessageType.REQUEST, false);
    private MessageTargetableSupport responseTarget = new MessageTargetableSupport(TargetMessageType.RESPONSE, true);


    public String getRoutingKeyExpression() {
        return routingKeyExpression;
    }

    public void setRoutingKeyExpression(String routingKeyExpression) {
        this.routingKeyExpression = routingKeyExpression;
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

    public boolean initializesRequest() {
        return false;
    }

    public boolean needsInitializedRequest() {
        return requestTarget.getTarget() == TargetMessageType.REQUEST;
    }

    public boolean initializesResponse() {
        return responseTarget.getTarget() == TargetMessageType.RESPONSE;
    }

    public boolean needsInitializedResponse() {
        return requestTarget.getTarget() == TargetMessageType.RESPONSE;
    }

    public String[] getVariablesUsed() {
        return requestTarget.getMessageTargetVariablesUsed().withExpressions(routingKeyExpression, ssgActiveConnectorName).asArray();
    }

    public VariableMetadata[] getVariablesSet() {
        return responseTarget.getMessageTargetVariablesSet().asArray();
    }

    //
    // Metadata
    //
    private static final String META_INITIALIZED = RouteViaAMQPAssertion.class.getName() + ".metadataInitialized";

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

        // Ensure inbound transport gets wired up
        meta.put(AssertionMetadata.MODULE_LOAD_LISTENER_CLASSNAME, "com.l7tech.external.assertions.amqpassertion.server.ModuleLoadListener");

        // Set description for GUI
        meta.put(AssertionMetadata.SHORT_NAME, "Route via AMQP");
        meta.put(AssertionMetadata.LONG_NAME, "Route via AMQP");

        meta.put(AssertionMetadata.GLOBAL_ACTION_CLASSNAMES, new String[]{"com.l7tech.external.assertions.amqpassertion.console.AmqpDestinationsAction"});

        // Add to palette folder(s) 
        //   accessControl, transportLayerSecurity, xmlSecurity, xml, routing, 
        //   misc, audit, policyLogic, threatProtection 
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[]{"routing"});
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/server16.gif");

        // Enable automatic policy advice (default is no advice unless a matching Advice subclass exists)
        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto");

        // Set up smart Getter for nice, informative policy node name, for GUI
        meta.put(AssertionMetadata.POLICY_NODE_ICON, "com/l7tech/console/resources/server16.gif");
        meta.put(AssertionMetadata.GLOBAL_ACTION_CLASSNAMES, new String[]{"com.l7tech.external.assertions.amqpassertion.console.AmqpDestinationsAction"});

        /*meta.put(AssertionMetadata.EXTENSION_INTERFACES_FACTORY, new Functions.Unary<Collection<ExtensionInterfaceBinding>, ApplicationContext>() {
            @Override
            public Collection<ExtensionInterfaceBinding> call(ApplicationContext appContext) {
                return AMQPEntitySupport.getInstance(appContext).getExtensionInterfaceBindings();
            }
        });*/

        // request default feature set name for our class name, since we are a known optional module
        // that is, we want our required feature set to be "assertion:AMQPAssertion" rather than "set:modularAssertions"
        meta.put(AssertionMetadata.FEATURE_SET_NAME, "set:modularAssertions");

        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }

    /**
     * This is here for backwards compatibility, we will take the old OID and convert it to a GOID;
     * then disregard the OID altogether.
     *
     * @param ssgActiveConnectorId the OID to convert to a GOID
     */
    public void setSsgActiveConnectorId(@Nullable final Long ssgActiveConnectorId) {
        if (ssgActiveConnectorGoid == null && ssgActiveConnectorId > -1) {
            ssgActiveConnectorGoid = GoidUpgradeMapper.mapOid(EntityType.SSG_ACTIVE_CONNECTOR, ssgActiveConnectorId);
        }
    }

    /**
     * Get the GOID of the associated SsgActiveConnector.
     *
     * @return the GOID of the connector, or null.
     */
    @Nullable
    @Dependency(type = Dependency.DependencyType.SSG_ACTIVE_CONNECTOR, methodReturnType = Dependency.MethodReturnType.GOID)
    @Migration(mapName = MigrationMappingSelection.REQUIRED, resolver = PropertyResolver.Type.SERVER_VARIABLE)
    public Goid getSsgActiveConnectorGoid() {
        return ssgActiveConnectorGoid;
    }

    /**
     * Set the GOID for the associated SsgActiveConnector.
     *
     * @param ssgActiveConnectorGoid the GOID, or null.
     */
    public void setSsgActiveConnectorGoid(@Nullable final Goid ssgActiveConnectorGoid) {
        this.ssgActiveConnectorGoid = ssgActiveConnectorGoid;
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
    public void setSsgActiveConnectorName(@Nullable final String ssgActiveConnectorName) {
        this.ssgActiveConnectorName = ssgActiveConnectorName;
    }

}
