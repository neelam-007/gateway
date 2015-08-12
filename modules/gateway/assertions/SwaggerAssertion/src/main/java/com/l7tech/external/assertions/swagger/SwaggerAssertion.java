package com.l7tech.external.assertions.swagger;

import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.objectmodel.migration.MigrationMappingSelection;
import com.l7tech.objectmodel.migration.PropertyResolver;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.variable.VariableMetadata;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static com.l7tech.objectmodel.ExternalEntityHeader.ValueType.TEXT_ARRAY;

/**
 * 
 */
public class SwaggerAssertion extends Assertion implements MessageTargetable, UsesVariables, SetsVariables {
    protected static final Logger logger = Logger.getLogger(SwaggerAssertion.class.getName());

    public static final String DEFAULT_PREFIX = "sw";
    public static final String RESULT_CODE = ".result.code";
    public static final String RESULT_RESPONSE_CODE = ".result.response.code";
    public static final String RESULT_RESPONSE_TYPE = ".result.response.contentType";
    public static final String RESULT_MESSAGE = ".result.message";
    public static final String SWAGGER_BASE_URI = ".baseUri";
    public static final String SWAGGER_DOC = ".document";
    public static final String SWAGGER_HOST = ".host";
    public static final String SWAGGER_API_URI = ".apiUri";

    private final MessageTargetableSupport messageTargetableSupport;
    private String prefix = DEFAULT_PREFIX;
    private String swaggerDoc;
    private String serviceBase = "";
    private boolean validateMethod = true;
    private boolean validatePath = true;
    private boolean validateScheme = true;
    private boolean validateRequestArguments = true;
    private boolean requireSecurityCredentials = true;

    public SwaggerAssertion() {
        this( TargetMessageType.REQUEST );
    }

    public SwaggerAssertion(TargetMessageType defaultTargetMessageType) {
        this.messageTargetableSupport = new MessageTargetableSupport(defaultTargetMessageType, false);
    }

    public boolean isValidateMethod() {
        return validateMethod;
    }

    public void setValidateMethod(boolean validateMethod) {
        this.validateMethod = validateMethod;
    }

    public boolean isValidatePath() {
        return validatePath;
    }

    public void setValidatePath(boolean validatePath) {
        this.validatePath = validatePath;
    }

    public boolean isValidateScheme() {
        return validateScheme;
    }

    public void setValidateScheme(boolean validateScheme) {
        this.validateScheme = validateScheme;
    }

    public boolean isValidateRequestArguments() {
        return validateRequestArguments;
    }

    public void setValidateRequestArguments(boolean validateRequestArguments) {
        this.validateRequestArguments = validateRequestArguments;
    }

    public boolean isRequireSecurityCredentials() {
        return requireSecurityCredentials;
    }

    public void setRequireSecurityCredentials(boolean requireSecurityCredentials) {
        this.requireSecurityCredentials = requireSecurityCredentials;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getSwaggerDoc() {
        return swaggerDoc;
    }

    public void setSwaggerDoc(String swaggerDoc) {
        this.swaggerDoc = swaggerDoc;
    }


    public String getServiceBase() {
        return serviceBase;
    }

    public void setServiceBase(String serviceBase) {
        this.serviceBase = serviceBase;
    }


    @Migration(mapName = MigrationMappingSelection.NONE, mapValue = MigrationMappingSelection.REQUIRED, export = false, valueType = TEXT_ARRAY, resolver = PropertyResolver.Type.SERVER_VARIABLE)
    @Override
    public String[] getVariablesUsed() {
        List<String> varsUsed = new ArrayList<>();
        varsUsed.add(getSwaggerDoc());
        return varsUsed.toArray(new String[varsUsed.size()]);
    }

    //
    // Metadata
    //
    private static final String META_INITIALIZED = SwaggerAssertion.class.getName() + ".metadataInitialized";

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

        // Set description for GUI
        meta.put(AssertionMetadata.SHORT_NAME, "Validate Against Swagger Document");
        meta.put(AssertionMetadata.LONG_NAME, "Validate API call against Swagger Document");

        // Add to palette folder(s) 
        //   accessControl, transportLayerSecurity, xmlSecurity, xml, routing, 
        //   misc, audit, policyLogic, threatProtection 
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[] { "xml" });
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/external/assertions/swagger/console/resources/swagger-16x16.png");

        // Enable automatic policy advice (default is no advice unless a matching Advice subclass exists)
        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto");

        // Set up smart Getter for nice, informative policy node name, for GUI
        meta.put(AssertionMetadata.POLICY_NODE_ICON, "com/l7tech/external/assertions/swagger/console/resources/swagger-16x16.png");

        // request default feature set name for our class name, since we are a known optional module
        // that is, we want our required feature set to be "assertion:Swagger" rather than "set:modularAssertions"
        meta.put(AssertionMetadata.FEATURE_SET_NAME, "(fromClass)");

        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }

    /**
     * Get a description of the variables this assertion sets.  The general expectation is that these
     * variables will exist and be assigned values after the server assertion's checkRequest method
     * has returned.
     * <p/>
     * If an assertion requires a variable to already exist, but modifies it in-place, it should delcare it
     * in both SetsVariables and {@link com.l7tech.policy.assertion.UsesVariables}.
     * <p/>
     * The following example changes <strong>are not</strong> considered as Message modifications for the purposes of this contract:
     * <ul>
     * <li>Changes to a Message's associated AuthenticationContext within the ProcessingContext.</li>
     * <li>Read-only access to a Message's MIME body or parts, even if this might internally require reading and stashing the message bytes.</li>
     * <li>Read-only access to an XML Message, even if this might internally require parsing the document.</li>
     * <li>Reading transport level headers or pending response headers.</li>
     * <li>Checking current pending decoration requirements .</li>
     * <li>Matching an XPath against a document, or validating its schema.</li>
     * </ul>
     * <p/>
     * The following example changes <strong>are</strong> considered as Message modifications for the purposes of this contract:
     * <ul>
     * <li>Changes to the message content type, MIME body, or parts, including by total replacement</li>
     * <li>Changes to an XML document</li>
     * <li>Addition of pending decoration requirements, even if decoration is not performed immediately.</li>
     * <li>Applying an XSL transformation.</li>
     * </ul>
     *
     * @return an array of VariableMetadata instances.  May be empty, but should never be null.
     * @throws com.l7tech.policy.variable.VariableNameSyntaxException
     *          (unchecked) if one of the variable names
     *          currently configured on this object does not use the correct syntax.
     */
    @Override
    public VariableMetadata[] getVariablesSet() {
        return new VariableMetadata[] {
        new VariableMetadata(prefix + RESULT_CODE, false, true, prefix + RESULT_CODE, false, DataType.INTEGER),
                new VariableMetadata(prefix + RESULT_RESPONSE_CODE, false, true, prefix + RESULT_MESSAGE, false, DataType.STRING),
                new VariableMetadata(prefix + RESULT_MESSAGE, false, true, prefix + RESULT_RESPONSE_CODE, false, DataType.STRING),
                new VariableMetadata(prefix + RESULT_RESPONSE_TYPE, false, true, prefix + RESULT_RESPONSE_TYPE, false, DataType.STRING),
                new VariableMetadata(prefix + SWAGGER_HOST, false, false, prefix + SWAGGER_HOST, false, DataType.STRING),
                new VariableMetadata(prefix + SWAGGER_BASE_URI, false, false, prefix + SWAGGER_BASE_URI, false, DataType.STRING),
                new VariableMetadata(prefix + SWAGGER_API_URI, false, false, prefix + SWAGGER_API_URI, false, DataType.STRING)
        };
    }


    /**
     * The type of message this assertion targets.  Defaults to {@link com.l7tech.policy.assertion.TargetMessageType#REQUEST}. Never null.
     */
    @Override
    public TargetMessageType getTarget() {
        return messageTargetableSupport.getTarget();
    }

    /**
     * The type of message this assertion targets.  Defaults to {@link com.l7tech.policy.assertion.TargetMessageType#REQUEST}. Never null.
     */
    @Override
    public void setTarget(TargetMessageType target) {
        messageTargetableSupport.setTarget(target);
    }

    /**
     * If {@link #getTarget} is {@link com.l7tech.policy.assertion.TargetMessageType#OTHER}, the name of some other message-typed variable to use as
     * this assertion's target.
     */
    @Override
    public String getOtherTargetMessageVariable() {
        return messageTargetableSupport.getOtherTargetMessageVariable();
    }

    /**
     * If {@link #getTarget} is {@link com.l7tech.policy.assertion.TargetMessageType#OTHER}, the name of some other message-typed variable to use as
     * this assertion's target.
     */
    @Override
    public void setOtherTargetMessageVariable(String otherMessageVariable) {
        messageTargetableSupport.setOtherTargetMessageVariable(otherMessageVariable);
    }

    /**
     * A short, descriptive name for the target, i.e. "request", "response" or {@link #getOtherTargetMessageVariable()}
     * <p/>
     * <p>Almost all MessageTargetable implementations will never return null,
     * in a few null is necessary for backwards compatibility.</p>
     *
     * @return the target name or null if no target is set.
     */
    @Override
    public String getTargetName() {
        return messageTargetableSupport.getTargetName();
    }

    /**
     * @return true if the target message might be modified; false if the assertion only reads the target message.
     */
    @Override
    public boolean isTargetModifiedByGateway() {
        return false;
    }

}
