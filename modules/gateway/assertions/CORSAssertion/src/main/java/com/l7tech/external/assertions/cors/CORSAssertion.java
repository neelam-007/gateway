package com.l7tech.external.assertions.cors;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;
import com.l7tech.policy.assertion.SetsVariables;
import com.l7tech.policy.validator.AssertionValidatorSupport;
import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.variable.VariableMetadata;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static com.l7tech.policy.assertion.AssertionMetadata.POLICY_VALIDATOR_CLASSNAME;

public class CORSAssertion extends Assertion implements SetsVariables {


    public static final String SUFFIX_IS_PREFLIGHT = "isPreflight";
    public static final String SUFFIX_IS_CORS = "isCORS";
    public static final Collection<String> VARIABLE_SUFFIXES = Collections.unmodifiableCollection(Arrays.asList(
            SUFFIX_IS_PREFLIGHT,
            SUFFIX_IS_CORS
    ));

    public CORSAssertion() {
        super();
    }

    public String getVariablePrefix() {
        return variablePrefix;
    }

    public void setVariablePrefix(String variablePrefix) {
        this.variablePrefix = variablePrefix;
    }

    // Null if accept any
    public List<String> getAcceptedHeaders() {
        return acceptedHeaders;
    }

    // Null if accept any
    public void setAcceptedHeaders(List<String> acceptedHeaders) {
        this.acceptedHeaders = acceptedHeaders;
    }

    public List<String> getAcceptedMethods() {
        return acceptedMethods;
    }

    public void setAcceptedMethods(List<String> acceptedMethods) {
        this.acceptedMethods = acceptedMethods;
    }

    // Null if accept any
    public List<String> getAcceptedOrigins() {
        return acceptedOrigins;
    }

    // Null if accept any
    public void setAcceptedOrigins(List<String> acceptedOrigins) {
        this.acceptedOrigins = acceptedOrigins;
    }

    // Null if expose all
    public List<String> getExposedHeaders() {
        return exposedHeaders;
    }

    // Null if expose all
    public void setExposedHeaders(List<String> exposedHeaders) {
        this.exposedHeaders = exposedHeaders;
    }

    public Long getResponseCacheTime() {
        return responseCacheTime;
    }

    public void setResponseCacheTime(Long responseCacheTime) {
        this.responseCacheTime = responseCacheTime;
    }

    public boolean isSupportsCredentials() {
        return supportsCredentials;
    }

    public void setSupportsCredentials(boolean supportsCredentials) {
        this.supportsCredentials = supportsCredentials;
    }

    public boolean isRequireCors() {
        return requireCors;
    }

    public void setRequireCors(boolean requireCors) {
        this.requireCors = requireCors;
    }

    @Override
    public VariableMetadata[] getVariablesSet() {
        VariableMetadata[] metadata;

        metadata = new VariableMetadata[]{
                new VariableMetadata(variablePrefix + "." + SUFFIX_IS_PREFLIGHT, false, false, null, false, DataType.BOOLEAN),
                new VariableMetadata(variablePrefix + "." + SUFFIX_IS_CORS, false, false, null, false, DataType.BOOLEAN),
        };

        return metadata;
    }

    //
    // Metadata
    //
    private static final String META_INITIALIZED = CORSAssertion.class.getName() + ".metadataInitialized";

    private String variablePrefix = "cors";
    private List<String> acceptedHeaders;
    private List<String> acceptedMethods;
    private List<String> acceptedOrigins;
    private List<String> exposedHeaders;
    private Long responseCacheTime;
    private boolean supportsCredentials = true;
    private boolean requireCors = true;


    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        // Set description for GUI
        meta.put(AssertionMetadata.SHORT_NAME, "Support CORS");
        meta.put(AssertionMetadata.LONG_NAME, "Support Cross-Origin Resource Sharing");

        // Add to palette folder(s)
        //   accessControl, transportLayerSecurity, xmlSecurity, xml, routing,
        //   misc, audit, policyLogic, threatProtection
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[]{"accessControl"});
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/Properties16.gif");

        // Enable automatic policy advice (default is no advice unless a matching Advice subclass exists)
        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto");
        meta.put(AssertionMetadata.PROPERTIES_ACTION_NAME, "Support CORS Properties");

        // Set up smart Getter for nice, informative policy node name, for GUI
        meta.put(AssertionMetadata.POLICY_NODE_ICON, "com/l7tech/console/resources/Properties16.gif");

        meta.put(POLICY_VALIDATOR_CLASSNAME, Validator.class.getName());

        // request default feature set name for our class name, since we are a known optional module
        // that is, we want our required feature set to be "assertion:CORS" rather than "set:modularAssertions"   "(fromClass)"
        meta.put(AssertionMetadata.FEATURE_SET_NAME, "set:modularAssertions");
//        meta.put(AssertionMetadata.FEATURE_SET_NAME, "(fromClass)");

        meta.put(META_INITIALIZED, Boolean.TRUE);

        return meta;
    }

    public static class Validator extends AssertionValidatorSupport<CORSAssertion> {

        public Validator(CORSAssertion assertion) {
            super(assertion);
        }
    }
}
