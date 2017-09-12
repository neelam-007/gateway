package com.l7tech.external.assertions.cors;

import com.l7tech.policy.assertion.*;
import com.l7tech.policy.validator.AssertionValidatorSupport;
import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.policy.variable.VariableMetadata;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static com.l7tech.policy.assertion.AssertionMetadata.POLICY_VALIDATOR_CLASSNAME;

public class CORSAssertion extends Assertion implements SetsVariables, UsesVariables {

    public static final String PREFIX_DEFAULT = "cors";
    public static final String SUFFIX_IS_PREFLIGHT = "isPreflight";
    public static final String SUFFIX_IS_CORS = "isCORS";
    public static final Collection<String> VARIABLE_SUFFIXES = Collections.unmodifiableCollection(Arrays.asList(
            SUFFIX_IS_PREFLIGHT,
            SUFFIX_IS_CORS
    ));

    public CORSAssertion() {
        super();
    }

    /**
     * @return the prefix to give to the context variables set by this assertion
     */
    public String getVariablePrefix() {
        return variablePrefix;
    }

    public void setVariablePrefix(String variablePrefix) {
        this.variablePrefix = variablePrefix;
    }

    /**
     * @return a list of accepted requested header names, or null if all should headers be accepted
     */
    public @Nullable List<String> getAcceptedHeaders() {
        return acceptedHeaders;
    }

    public void setAcceptedHeaders(@Nullable List<String> acceptedHeaders) {
        this.acceptedHeaders = acceptedHeaders;
    }

    /**
     * @return a list of accepted method names
     */
    public List<String> getAcceptedMethods() {
        return acceptedMethods;
    }

    public void setAcceptedMethods(List<String> acceptedMethods) {
        this.acceptedMethods = acceptedMethods;
    }

    /**
     * @return a list of accepted origins, or null if all origins should be accepted
     */
    public @Nullable List<String> getAcceptedOrigins() {
        return acceptedOrigins;
    }

    public void setAcceptedOrigins(@Nullable List<String> acceptedOrigins) {
        this.acceptedOrigins = acceptedOrigins;
    }

    /**
     * @return a list of header names for the value of the Access-Control-Expose-Headers header, or null if none exposed
     */
    public @Nullable List<String> getExposedHeaders() {
        return exposedHeaders;
    }

    public void setExposedHeaders(@Nullable List<String> exposedHeaders) {
        this.exposedHeaders = exposedHeaders;
    }

    /**
     * @return the value for the Access-Control-Max-Age header (seconds), or null if the header is not to be added
     */
    public String getResponseCacheTime() {
        return responseCacheTime;
    }

    public void setResponseCacheTime(String responseCacheTime) {
        this.responseCacheTime = responseCacheTime;
    }

    /**
     * @return true if an Access-Control-Allow-Credentials header should be added to the response
     */
    public boolean isSupportsCredentials() {
        return supportsCredentials;
    }

    public void setSupportsCredentials(boolean supportsCredentials) {
        this.supportsCredentials = supportsCredentials;
    }

    /**
     * @return true if the assertion should fail when an Origin header is not present in the request message
     */
    public boolean isRequireCors() {
        return requireCors;
    }

    public void setRequireCors(boolean requireCors) {
        this.requireCors = requireCors;
    }

    /**
     * @return true if any non-standard HTTP method should be considered allowed
     */
    public boolean isAllowNonStandardMethods() {
        return allowNonStandardMethods;
    }

    public void setAllowNonStandardMethods(boolean allowNonStandardMethods) {
        this.allowNonStandardMethods = allowNonStandardMethods;
    }

    public boolean isAcceptSameOriginRequests() {
        return acceptSameOriginRequests;
    }

    public void setAcceptSameOriginRequests(boolean acceptSameOriginRequests) {
        this.acceptSameOriginRequests = acceptSameOriginRequests;
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

    @Override
    public String[] getVariablesUsed() {
        return Syntax.getReferencedNames(responseCacheTime);
    }

    //
    // Metadata
    //
    private static final String META_INITIALIZED = CORSAssertion.class.getName() + ".metadataInitialized";

    private String variablePrefix = PREFIX_DEFAULT;
    private List<String> acceptedHeaders;
    private List<String> acceptedMethods;
    private List<String> acceptedOrigins;
    private List<String> exposedHeaders;
    private String responseCacheTime;
    private boolean supportsCredentials = true;
    private boolean requireCors = true;
    private boolean allowNonStandardMethods = false;
    private boolean acceptSameOriginRequests = false;

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        // Set description for GUI
        meta.put(AssertionMetadata.SHORT_NAME, "Process CORS Request");
        meta.put(AssertionMetadata.LONG_NAME, "Process Cross-Origin Resource Sharing Request");

        // Add to palette folder(s)
        //   accessControl, transportLayerSecurity, xmlSecurity, xml, routing,
        //   misc, audit, policyLogic, threatProtection
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[]{"accessControl"});
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/Properties16.gif");

        // Enable automatic policy advice (default is no advice unless a matching Advice subclass exists)
        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto");
        meta.put(AssertionMetadata.PROPERTIES_ACTION_NAME, "Process CORS Request Properties");

        // Set up smart Getter for nice, informative policy node name, for GUI
        meta.put(AssertionMetadata.POLICY_NODE_ICON, "com/l7tech/console/resources/Properties16.gif");

        meta.put(POLICY_VALIDATOR_CLASSNAME, Validator.class.getName());

        meta.put(AssertionMetadata.FEATURE_SET_NAME, "(fromClass)");

        meta.put(META_INITIALIZED, Boolean.TRUE);

        return meta;
    }

    public static class Validator extends AssertionValidatorSupport<CORSAssertion> {

        public Validator(CORSAssertion assertion) {
            super(assertion);
        }
    }
}
