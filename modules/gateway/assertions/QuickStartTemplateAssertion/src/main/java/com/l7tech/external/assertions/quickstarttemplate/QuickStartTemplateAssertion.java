package com.l7tech.external.assertions.quickstarttemplate;

import com.l7tech.policy.assertion.*;
import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.variable.VariableMetadata;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

import static com.l7tech.policy.assertion.AssertionMetadata.MODULE_LOAD_LISTENER_CLASSNAME;

/**
 * Make it easier to start using the Gateway without needing to learn how to write complex policy.
 * We explore how to use encapsulated assertions to help make policy format more compact and with less learning curve.
 * Explore how we can transfer the complexity burden to more experienced Gateway users ahead of time using encapsulated assertion templating.
 */
public class QuickStartTemplateAssertion extends MessageTargetableAssertion implements SetsVariables {
    public static final String QS_WARNINGS = "qs.warnings";
    public static final String QS_BUNDLE = "qs.bundle";
    /**
     * Indicates the service timestamp (should be used exclusively by the scalar).
     */
    public static final String PROPERTY_QS_REGISTRAR_TMS = "qs.registrar.tms";
    /**
     * Indicates the service creation method, see {@link QsServiceCreateMethod} vor possible values.
     */
    public static final String PROPERTY_QS_CREATE_METHOD = "qs.create.method";

    /**
     * Possible values for {@link #PROPERTY_QS_CREATE_METHOD}
     */
    public enum QsServiceCreateMethod {
        /**
         * The service has been created during bootstrap (i.e. bootstrapping its {@code json} file)
         */
        BOOTSTRAP("bootstrap"),
        /**
         * The service has been cerated by the scalar.
         */
        SCALAR("scalar");

        @NotNull
        private final String displayName;

        QsServiceCreateMethod(@NotNull final String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }

        @SuppressWarnings("unused")
        public static QsServiceCreateMethod findMethod(final String displayName) {
            if (displayName == null) {
                throw new NullPointerException("DisplayName is null");
            }
            for (final QsServiceCreateMethod method : values()) {
                if (displayName.equals(method.toString())) {
                    return method;
                }
            }
            throw new IllegalArgumentException("No QsServiceCreateMethod with DisplayName: " + displayName);
        }
    }

    @Override
    protected VariablesSet doGetVariablesSet() {
        return super.doGetVariablesSet().withVariables(
                new VariableMetadata(QS_WARNINGS, true, true, null, false, DataType.STRING),
                new VariableMetadata(QS_BUNDLE, true, true, null, false, DataType.MESSAGE)
        );
    }

    //
    // Metadata
    //
    private static final String META_INITIALIZED = QuickStartTemplateAssertion.class.getName() + ".metadataInitialized";

    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        // Cluster properties used by this assertion
        Map<String, String[]> props = new HashMap<>();
        //props.put(NAME, new String[] {
        //        DESCRIPTION,
        //        DEFAULT
        //});
        meta.put(AssertionMetadata.CLUSTER_PROPERTIES, props);

        // Set description for GUI
        meta.put(AssertionMetadata.SHORT_NAME, "Quick Start Template");
        meta.put(AssertionMetadata.LONG_NAME, "Quick Start Template");

        // Add to palette folder(s) 
        //   accessControl, transportLayerSecurity, xmlSecurity, xml, routing, 
        //   misc, audit, policyLogic, threatProtection 
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[] { "misc" });
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/Map16.gif");

        // Enable automatic policy advice (default is no advice unless a matching Advice subclass exists)
        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto");

        // Set up smart Getter for nice, informative policy node name, for GUI
        meta.put(AssertionMetadata.POLICY_NODE_ICON, "com/l7tech/console/resources/Map16.gif");

        // request default feature set name for our class name, since we are a known optional module
        // that is, we want our required feature set to be "assertion:QuickStartTemplate" rather than "set:modularAssertions"
        meta.put(AssertionMetadata.FEATURE_SET_NAME, "set:modularAssertions");  // TODO change back to "(fromClass)" and add to license feature set

        meta.put(MODULE_LOAD_LISTENER_CLASSNAME, "com.l7tech.external.assertions.quickstarttemplate.server.QuickStartAssertionModuleLifecycle");

        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }

}
