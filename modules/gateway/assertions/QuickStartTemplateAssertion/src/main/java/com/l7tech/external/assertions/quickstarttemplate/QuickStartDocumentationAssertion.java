package com.l7tech.external.assertions.quickstarttemplate;

import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;
import com.l7tech.policy.assertion.MessageTargetableAssertion;
import com.l7tech.policy.assertion.SetsVariables;
import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.variable.VariableMetadata;

import java.util.HashMap;
import java.util.Map;

import static com.l7tech.policy.assertion.AssertionMetadata.MODULE_LOAD_LISTENER_CLASSNAME;

/**
 * Make it easier to start using the Gateway without needing to learn how to write complex policy.
 * We explore how to use encapsulated assertions to help make policy format more compact and with less learning curve.
 * Explore how we can transfer the complexity burden to more experienced Gateway users ahead of time using encapsulated assertion templating.
 */
public class QuickStartDocumentationAssertion extends MessageTargetableAssertion implements SetsVariables {
    public static final String QS_DESCRIPTION_PROPERTY = "description";
    public static final String QS_SCHEMA_PROPERTY = "doc_qs_schema";
    public static final String QS_SAMPLE_PROPERTY = "doc_qs_sample";
    public static final String QS_DOC = "qs.doc";

    @Override
    protected VariablesSet doGetVariablesSet() {
        return super.doGetVariablesSet().withVariables(
                new VariableMetadata(QS_DOC, true, true, null, false, DataType.STRING)
        );
    }

    //
    // Metadata
    //
    private static final String META_INITIALIZED = QuickStartDocumentationAssertion.class.getName() + ".metadataInitialized";

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
        meta.put(AssertionMetadata.SHORT_NAME, "Quick Start Documentation");
        meta.put(AssertionMetadata.LONG_NAME, "Quick Start Documentation");

        // Add to palette folder(s) 
        //   accessControl, transportLayerSecurity, xmlSecurity, xml, routing, 
        //   misc, audit, policyLogic, threatProtection 
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[] { "internalAssertions" });
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/Map16.gif");

        // Enable automatic policy advice (default is no advice unless a matching Advice subclass exists)
        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto");

        // Set up smart Getter for nice, informative policy node name, for GUI
        meta.put(AssertionMetadata.POLICY_NODE_ICON, "com/l7tech/console/resources/Map16.gif");

        // request default feature set name for our class name, since we are a known optional module
        // that is, we want our required feature set to be "assertion:QuickStartTemplate" rather than "set:modularAssertions"
        meta.put(AssertionMetadata.FEATURE_SET_NAME, "(fromClass)");

        meta.put(MODULE_LOAD_LISTENER_CLASSNAME, "com.l7tech.external.assertions.quickstarttemplate.server.QuickStartAssertionModuleLifecycle");

        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }

}
