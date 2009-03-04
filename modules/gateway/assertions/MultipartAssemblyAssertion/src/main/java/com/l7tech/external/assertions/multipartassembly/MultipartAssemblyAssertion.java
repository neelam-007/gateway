package com.l7tech.external.assertions.multipartassembly;

import com.l7tech.util.Functions;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;
import com.l7tech.policy.assertion.UsesVariables;
import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.objectmodel.migration.MigrationMappingSelection;
import com.l7tech.objectmodel.migration.PropertyResolver;
import static com.l7tech.objectmodel.ExternalEntityHeader.ValueType.TEXT_ARRAY;

import java.util.logging.Logger;

/**
 * 
 */
public class MultipartAssemblyAssertion extends Assertion implements UsesVariables {
    protected static final Logger logger = Logger.getLogger(MultipartAssemblyAssertion.class.getName());
    public static final String SUFFIX_PAYLOADS = ".payloads";
    public static final String SUFFIX_CONTENT_TYPES = ".contentTypes";
    public static final String SUFFIX_PART_IDS = ".partIds";

    private boolean actOnRequest = true;
    private String variablePrefix = "multipartAssembly";

    public boolean isActOnRequest() {
        return actOnRequest;
    }

    public void setActOnRequest(boolean actOnRequest) {
        this.actOnRequest = actOnRequest;
    }

    public String getVariablePrefix() {
        return variablePrefix;
    }

    public void setVariablePrefix(String variablePrefix) {
        this.variablePrefix = variablePrefix;
    }

    @Migration(mapName = MigrationMappingSelection.NONE, mapValue = MigrationMappingSelection.REQUIRED, export = false, valueType = TEXT_ARRAY, resolver = PropertyResolver.Type.SERVER_VARIABLE)
    public String[] getVariablesUsed() {
        return new String[] {
                variablePrefix + SUFFIX_PAYLOADS,
                variablePrefix + SUFFIX_CONTENT_TYPES,
                variablePrefix + SUFFIX_PART_IDS
        };
    }

    //
    // Metadata
    //
    private static final String META_INITIALIZED = MultipartAssemblyAssertion.class.getName() + ".metadataInitialized";

    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        // Set description for GUI
        meta.put(AssertionMetadata.SHORT_NAME, "Multipart Assembly");
        meta.put(AssertionMetadata.LONG_NAME, "Wrap a message in a multipart envelope and add attachments");
        meta.put(AssertionMetadata.DESCRIPTION, "Wraps a message in a multipart envelope and adds attachments specified in context variables, each expected to contain an array.");

        meta.put(AssertionMetadata.POLICY_NODE_NAME_FACTORY, new Functions.Unary< String, MultipartAssemblyAssertion >() {
            public String call(MultipartAssemblyAssertion assertion) {
                return assertion.isActOnRequest()
                        ? "Multipart Assembly into Request"
                        : "Multipart Assembly into Response";
            }
        });

        // Add to palette folder(s)
        //   accessControl, transportLayerSecurity, xmlSecurity, xml, routing, 
        //   misc, audit, policyLogic, threatProtection 
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[] { "misc" });
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/external/assertions/multipartassembly/console/resources/assembly16.gif");

        // Enable automatic policy advice (default is no advice unless a matching Advice subclass exists)
        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto");

        // Set up smart Getter for nice, informative policy node name, for GUI
        meta.put(AssertionMetadata.POLICY_NODE_ICON, "com/l7tech/external/assertions/multipartassembly/console/resources/assembly16.gif");

        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }

}
