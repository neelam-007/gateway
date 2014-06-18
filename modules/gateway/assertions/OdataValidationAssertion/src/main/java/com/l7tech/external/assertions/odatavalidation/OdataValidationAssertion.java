package com.l7tech.external.assertions.odatavalidation;

import com.l7tech.policy.assertion.*;
import com.l7tech.policy.validator.ValidatorFlag;
import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.util.Functions;
import org.apache.commons.lang.StringUtils;

import java.util.*;
import java.util.logging.Logger;

import static com.l7tech.policy.assertion.AssertionMetadata.POLICY_VALIDATOR_FLAGS_FACTORY;

/**
 * Validates OData messages against a Service Metadata Document.
 *
 * @author ymoiseyenko
 */
public class OdataValidationAssertion extends MessageTargetableAssertion implements UsesVariables, SetsVariables {

    public enum OdataOperations { GET, POST, PUT, DELETE, MERGE, PATCH }

    public static final String DEFAULT_PREFIX = "odata";

    private String odataMetadataSource;
    private String variablePrefix;

    private Map<String, Object> actionsMap = new HashMap<>();

    private boolean readOperation;
    private boolean createOperation;
    private boolean updateOperation;
    private boolean partialUpdateOperation;
    private boolean mergeOperation;
    private boolean deleteOperation;

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


    public Map<String, Object> getAllActions() {
        return Collections.unmodifiableMap(actionsMap);
    }

    public void addAction(String name, Object value) {
        if(StringUtils.isEmpty(name)) {
            throw new IllegalArgumentException("Action name cannot be null!");
        }
        actionsMap.put(name, value);
    }

    public void removeAction(String name) {
        actionsMap.remove(name);
    }


    public String getVariablePrefix() {
        return StringUtils.isBlank(variablePrefix)?DEFAULT_PREFIX:variablePrefix;
    }

    public void setVariablePrefix(String variablePrefix) {
        this.variablePrefix = variablePrefix;
    }

    @Override
    protected VariablesUsed doGetVariablesUsed() {
        return super.doGetVariablesUsed().withExpressions(odataMetadataSource, variablePrefix);
    }

    @Override
    protected VariablesSet doGetVariablesSet() {
        return super.doGetVariablesSet().withVariables(new VariableMetadata(getVariablePrefix() + "." + "query"));
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
        //props.put(NAME, new String[] {
        //        DESCRIPTION,
        //        DEFAULT
        //});
        meta.put(AssertionMetadata.CLUSTER_PROPERTIES, props);

        // Set description for GUI
        meta.put(AssertionMetadata.SHORT_NAME, "OData Validation");
        meta.put(AssertionMetadata.LONG_NAME, "Validate OData request against OData service metadata and scan for threats");

        // Add to palette folder(s) 
        //   accessControl, transportLayerSecurity, xmlSecurity, xml, routing, 
        //   misc, audit, policyLogic, threatProtection 
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[] { "threatProtection" });
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/SQLProtection16x16.gif");

        // Enable automatic policy advice (default is no advice unless a matching Advice subclass exists)
        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto");

        // Set up smart Getter for nice, informative policy node name, for GUI
        meta.put(AssertionMetadata.POLICY_NODE_ICON, "com/l7tech/console/resources/SQLProtection16x16.gif");
        meta.put(AssertionMetadata.PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.external.assertions.odatavalidation.console.OdataValidationDialog");
        meta.put(AssertionMetadata.PROPERTIES_ACTION_NAME, "OData Validation Properties");
        // request default feature set name for our class name, since we are a known optional module
        // that is, we want our required feature set to be "assertion:OdataValidation" rather than "set:modularAssertions"
        meta.put(AssertionMetadata.FEATURE_SET_NAME, "(fromClass)");
        meta.put(POLICY_VALIDATOR_FLAGS_FACTORY, new Functions.Unary<Set<ValidatorFlag>, OdataValidationAssertion>(){
            @Override
            public Set<ValidatorFlag> call(OdataValidationAssertion assertion) {
                return EnumSet.of(ValidatorFlag.PERFORMS_VALIDATION);
            }
        });

        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }
}
