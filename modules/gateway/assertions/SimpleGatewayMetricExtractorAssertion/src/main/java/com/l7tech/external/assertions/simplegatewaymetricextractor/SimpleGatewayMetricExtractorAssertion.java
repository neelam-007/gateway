package com.l7tech.external.assertions.simplegatewaymetricextractor;

import com.l7tech.external.assertions.simplegatewaymetricextractor.server.GenericEntityManagerSimpleGatewayMetricExtractorServerSupport;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.GenericEntityHeader;
import com.l7tech.policy.assertion.*;
import com.l7tech.util.Functions;
import org.springframework.context.ApplicationContext;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * A sample monitoring assertion for the GateWayMetrics framework.
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public class SimpleGatewayMetricExtractorAssertion extends Assertion implements UsesVariables, UsesEntities {
    private Goid genericEntityId;
    private String genericEntityClass;

    public String[] getVariablesUsed() {
        return new String[0]; //Syntax.getReferencedNames(...);
    }

    //
    // Metadata
    //
    private static final String META_INITIALIZED = SimpleGatewayMetricExtractorAssertion.class.getName() + ".metadataInitialized";


    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        // Cluster properties used by this assertion
        Map<String, String[]> props = new HashMap<>();

        meta.put(AssertionMetadata.CLUSTER_PROPERTIES, props);

        // Set description for GUI
        meta.put(AssertionMetadata.SHORT_NAME, "Simple Gateway Metric Extractor");
        meta.put(AssertionMetadata.LONG_NAME, "Simple Gateway Metric Extractor");

        // Enable automatic policy advice (default is no advice unless a matching Advice subclass exists)
        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto");

        // Set up smart Getter for nice, informative policy node name, for GUI
        meta.put(AssertionMetadata.POLICY_NODE_ICON, "com/l7tech/console/resources/CA_Logo_Black_16x16.png");

        // request default feature set name for our class name, since we are a known optional module
        // that is, we want our required feature set to be "assertion:SimpleGatewayMetricExtractor" rather than "set:modularAssertions"
        meta.put(AssertionMetadata.FEATURE_SET_NAME, "set:modularAssertions");

        // Subscribe our extractor to the module loading events so it can set up its application listener
        meta.put(AssertionMetadata.MODULE_LOAD_LISTENER_CLASSNAME, "com.l7tech.external.assertions.simplegatewaymetricextractor.server.SimpleGatewayMetricExtractor");

        // want a placeholder server assertion that always fails
        meta.put(AssertionMetadata.SERVER_ASSERTION_CLASSNAME, "com.l7tech.server.policy.assertion.ServerFalseAssertion");

        meta.put(AssertionMetadata.GLOBAL_ACTION_CLASSNAMES, new String[] { "com.l7tech.external.assertions.simplegatewaymetricextractor.console.SimpleGatewayMetricExtractorAction" } );

        meta.put(AssertionMetadata.EXTENSION_INTERFACES_FACTORY, new Functions.Unary<Collection<ExtensionInterfaceBinding>, ApplicationContext>() {
            @Override
            public Collection<ExtensionInterfaceBinding> call(ApplicationContext appContext) {
                return GenericEntityManagerSimpleGatewayMetricExtractorServerSupport.getInstance(appContext).getExtensionInterfaceBindings();
            }
        });

        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }

    @Override
    public EntityHeader[] getEntitiesUsed() {
        return genericEntityId == null
                ? new EntityHeader[0]
                : new EntityHeader[] { new GenericEntityHeader(genericEntityId.toString(), null, null, null, genericEntityClass) };
    }

    @Override
    public void replaceEntity(EntityHeader oldEntityHeader, EntityHeader newEntityHeader) {
        if (newEntityHeader != null && EntityType.GENERIC.equals(newEntityHeader.getType()) && newEntityHeader instanceof GenericEntityHeader) {
            genericEntityId = newEntityHeader.getGoid();
            genericEntityClass = ((GenericEntityHeader) newEntityHeader).getEntityClassName();
        }
    }
}
