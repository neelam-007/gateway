package com.l7tech.external.assertions.odatavalidation;

import com.l7tech.policy.assertion.*;
import com.l7tech.policy.validator.ValidatorFlag;
import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.policy.wsp.Java5EnumSetTypeMapping;
import com.l7tech.policy.wsp.SimpleTypeMappingFinder;
import com.l7tech.policy.wsp.TypeMapping;
import com.l7tech.util.Functions;
import org.apache.commons.lang.StringUtils;

import java.util.*;

import static com.l7tech.policy.assertion.AssertionMetadata.POLICY_VALIDATOR_FLAGS_FACTORY;

/**
 * Validates OData messages against a Service Metadata Document.
 *
 * @author ymoiseyenko
 */
public class OdataValidationAssertion extends MessageTargetableAssertion implements UsesVariables, SetsVariables {

    public static final String DEFAULT_PREFIX = "odata";
    public static final String QUERY_TOP = ".query.top";
    public static final String QUERY_FILTER = ".query.filter";
    public static final String QUERY_SKIP = ".query.skip";
    public static final String QUERY_ORDERBY = ".query.orderby";
    public static final String QUERY_EXPAND = ".query.expand";
    public static final String QUERY_FORMAT = ".query.format";
    public static final String QUERY_INLINECOUNT = ".query.inlinecount";
    public static final String QUERY_SELECT = ".query.select";
    public static final String QUERY_CUSTOMOPTIONS = ".query.customoptions";
    public static final String QUERY_PATHSEGMENTS = ".query.pathsegments";
    public static final String QUERY_COUNT = ".query.count";

    public enum OdataOperations { GET, POST, PUT, DELETE, MERGE, PATCH }
    public enum ProtectionActions { ALLOW_METADATA, ALLOW_RAW_VALUE }

    private String odataMetadataSource;
    private String resourceUrl;
    private String variablePrefix;
    private boolean validatePayload = true;

    private EnumSet<ProtectionActions> actions;

    private boolean readOperation = true;
    private boolean createOperation = true;
    private boolean updateOperation = true;
    private boolean partialUpdateOperation = false;
    private boolean mergeOperation = false;
    private boolean deleteOperation = false;

    public boolean isValidatePayload() {
        return validatePayload;
    }

    public void setValidatePayload(boolean validatePayload) {
        this.validatePayload = validatePayload;
    }

    public boolean isReadOperation() {
        return readOperation;
    }

    public void setReadOperation(boolean readOperation) {
        this.readOperation = readOperation;
    }

    public boolean isCreateOperation() {
        return createOperation;
    }

    public void setCreateOperation(boolean createOperation) {
        this.createOperation = createOperation;
    }

    public boolean isUpdateOperation() {
        return updateOperation;
    }

    public void setUpdateOperation(boolean updateOperation) {
        this.updateOperation = updateOperation;
    }

    public boolean isPartialUpdateOperation() {
        return partialUpdateOperation;
    }

    public void setPartialUpdateOperation(boolean partialUpdateOperation) {
        this.partialUpdateOperation = partialUpdateOperation;
    }

    public boolean isMergeOperation() {
        return mergeOperation;
    }

    public void setMergeOperation(boolean mergeOperation) {
        this.mergeOperation = mergeOperation;
    }

    public boolean isDeleteOperation() {
        return deleteOperation;
    }

    public void setDeleteOperation(boolean deleteOperation) {
        this.deleteOperation = deleteOperation;
    }

    public String getOdataMetadataSource() {
        return odataMetadataSource;
    }

    public void setOdataMetadataSource(String odataMetadataSource) {
        this.odataMetadataSource = odataMetadataSource;
    }

    public String getResourceUrl() {
        return resourceUrl;
    }

    public void setResourceUrl(String resourceUrl) {
        this.resourceUrl = resourceUrl;
    }

    public EnumSet<ProtectionActions> getActions() {
        return actions;
    }

    public void setActions(EnumSet<ProtectionActions> actions) {
        this.actions = actions;
    }

    public String getVariablePrefix() {
        return StringUtils.isBlank(variablePrefix)
                ? DEFAULT_PREFIX
                : variablePrefix;
    }

    public void setVariablePrefix(String variablePrefix) {
        this.variablePrefix = variablePrefix;
    }

    @Override
    protected VariablesUsed doGetVariablesUsed() {
        return super.doGetVariablesUsed().withExpressions(odataMetadataSource, resourceUrl, variablePrefix);
    }

    @Override
    protected VariablesSet doGetVariablesSet() {
        return super.doGetVariablesSet().withVariables(new VariableMetadata(getVariablePrefix() + QUERY_COUNT),
                new VariableMetadata(getVariablePrefix() + QUERY_TOP),
                new VariableMetadata(getVariablePrefix() + QUERY_FILTER, false, true, null, false),
                new VariableMetadata(getVariablePrefix() + QUERY_SKIP),
                new VariableMetadata(getVariablePrefix() + QUERY_ORDERBY, false, true, null, false),
                new VariableMetadata(getVariablePrefix() + QUERY_EXPAND),
                new VariableMetadata(getVariablePrefix() + QUERY_FORMAT),
                new VariableMetadata(getVariablePrefix() + QUERY_INLINECOUNT),
                new VariableMetadata(getVariablePrefix() + QUERY_SELECT),
                new VariableMetadata(getVariablePrefix() + QUERY_CUSTOMOPTIONS, false, true, null, false),
                new VariableMetadata(getVariablePrefix() + QUERY_PATHSEGMENTS, false, true, null, false)
        );
    }

    //
    // Metadata
    //
    private static final String META_INITIALIZED = OdataValidationAssertion.class.getName() + ".metadataInitialized";

    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        // Cluster properties used by this assertion
        Map<String, String[]> props = new HashMap<>();
        meta.put(AssertionMetadata.CLUSTER_PROPERTIES, props);

        // Set description for GUI
        meta.put(AssertionMetadata.SHORT_NAME, "Validate OData Request");
        meta.put(AssertionMetadata.LONG_NAME, "Validate OData request against Service Metadata Document");

        // Add to palette folder(s) 
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[] { "threatProtection" });
        meta.put(AssertionMetadata.PALETTE_NODE_ICON,
                "com/l7tech/external/assertions/odatavalidation/console/resources/OData.jpg");

        // Enable automatic policy advice (default is no advice unless a matching Advice subclass exists)
        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto");

        // Set up smart Getter for nice, informative policy node name, for GUI
        meta.put(AssertionMetadata.PROPERTIES_EDITOR_CLASSNAME,
                "com.l7tech.external.assertions.odatavalidation.console.OdataValidationDialog");
        meta.put(AssertionMetadata.PROPERTIES_ACTION_NAME, "OData Request Validation Properties");

        // request default feature set name for our class name, since we are a known optional module, that is, we
        // want our required feature set to be "assertion:OdataValidation" rather than "set:modularAssertions"
        meta.put(AssertionMetadata.FEATURE_SET_NAME, "(fromClass)");
        meta.put(POLICY_VALIDATOR_FLAGS_FACTORY, new Functions.Unary<Set<ValidatorFlag>, OdataValidationAssertion>(){
            @Override
            public Set<ValidatorFlag> call(OdataValidationAssertion assertion) {
                return EnumSet.of(ValidatorFlag.PERFORMS_VALIDATION);
            }
        });
        meta.put(AssertionMetadata.WSP_SUBTYPE_FINDER,
                new SimpleTypeMappingFinder(Arrays.<TypeMapping>asList(new Java5EnumSetTypeMapping(EnumSet.class, ProtectionActions.class, "actions"))));

        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }
}
