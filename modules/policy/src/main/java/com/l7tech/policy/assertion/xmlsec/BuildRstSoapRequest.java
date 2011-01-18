package com.l7tech.policy.assertion.xmlsec;

import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.objectmodel.migration.MigrationMappingSelection;
import com.l7tech.objectmodel.migration.PropertyResolver;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;
import com.l7tech.policy.assertion.SetsVariables;
import com.l7tech.policy.assertion.UsesVariables;
import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.security.token.SecurityTokenType;
import com.l7tech.util.TimeUnit;
import com.l7tech.xml.soap.SoapVersion;

import static com.l7tech.objectmodel.ExternalEntityHeader.ValueType.TEXT_ARRAY;
import static com.l7tech.policy.assertion.AssertionMetadata.*;

/**
 *
 */
public class BuildRstSoapRequest extends Assertion implements UsesVariables, SetsVariables {

    //- PUBLIC

    public static final long SYSTEM_LIFETIME = -1;

    public static final String DEFAULT_VARIABLE_PREFIX = "requestBuilder";
    public static final String VARIABLE_RST_REQUEST = "rstRequest";
    public static final String VARIABLE_CLIENT_ENTROPY = "clientEntropy";

    public BuildRstSoapRequest(){
    }

    public SecurityTokenType getTokenType() {
        return tokenType;
    }

    public void setTokenType( final SecurityTokenType tokenType ) {
        this.tokenType = tokenType;
    }

    public SoapVersion getSoapVersion() {
        return soapVersion;
    }

    public void setSoapVersion( final SoapVersion soapVersion ) {
        this.soapVersion = soapVersion;
    }

    public String getWsTrustNamespace() {
        return wsTrustNamespace;
    }

    public void setWsTrustNamespace( final String wsTrustNamespace ) {
        this.wsTrustNamespace = wsTrustNamespace;
    }

    public String getIssuerAddress() {
        return issuerAddress;
    }

    public void setIssuerAddress( final String issuerAddress ) {
        this.issuerAddress = issuerAddress;
    }

    public String getAppliesToAddress() {
        return appliesToAddress;
    }

    public void setAppliesToAddress( final String appliesToAddress ) {
        this.appliesToAddress = appliesToAddress;
    }

    public Integer getKeySize() {
        return keySize;
    }

    public void setKeySize( final Integer keySize ) {
        this.keySize = keySize;
    }

    public Long getLifetime() {
        return lifetime;
    }

    public void setLifetime( final Long lifetime ) {
        this.lifetime = lifetime;
    }

    public TimeUnit getLifetimeTimeUnit() {
        return lifetimeTimeUnit;
    }

    public void setLifetimeTimeUnit( final TimeUnit lifetimeTimeUnit ) {
        this.lifetimeTimeUnit = lifetimeTimeUnit;
    }

    public boolean isIncludeClientEntropy() {
        return includeClientEntropy;
    }

    public void setIncludeClientEntropy( final boolean includeClientEntropy ) {
        this.includeClientEntropy = includeClientEntropy;
    }

    public String getVariablePrefix() {
        return variablePrefix;
    }

    public void setVariablePrefix( final String variablePrefix ) {
        this.variablePrefix = variablePrefix;
    }

    @Override
    public VariableMetadata[] getVariablesSet() {
        return new VariableMetadata[] {
            new VariableMetadata(variablePrefix + "." + VARIABLE_RST_REQUEST, false, false, null, false, DataType.MESSAGE),
            new VariableMetadata(variablePrefix + "." + VARIABLE_CLIENT_ENTROPY, false, false, null, false, DataType.STRING),
        };
    }

    @Migration(mapName = MigrationMappingSelection.NONE, mapValue = MigrationMappingSelection.REQUIRED, export = false, valueType = TEXT_ARRAY, resolver = PropertyResolver.Type.SERVER_VARIABLE)
    @Override
    public String[] getVariablesUsed() {
        return Syntax.getReferencedNames(issuerAddress, appliesToAddress);
    }

    public static String[] getVariableSuffixes() {
        return new String[] {
            VARIABLE_RST_REQUEST,
            VARIABLE_CLIENT_ENTROPY,
        };
    }

    @Override
    public AssertionMetadata meta() {
        final DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        // Set description for GUI
        meta.put(SHORT_NAME, ASSERTION_BASIC_NAME);
        meta.put(DESCRIPTION, "Build a SOAP request message containing a RequestSecurityToken element.");

        // Add to palette folder(s)
        meta.put(PALETTE_FOLDERS, new String[]{"xmlSecurity"});
        meta.put(PALETTE_NODE_ICON, "com/l7tech/console/resources/xmlelement.gif");

        // Enable automatic policy advice (default is no advice unless a matching Advice subclass exists)
        meta.put(POLICY_ADVICE_CLASSNAME, "auto");

        meta.put(POLICY_NODE_ICON, "com/l7tech/console/resources/xmlelement.gif");
        meta.put(PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.console.panels.BuildRstSoapRequestPropertiesDialog");
        meta.put(PROPERTIES_ACTION_NAME, "RST SOAP Request Builder Properties");

        meta.put(META_INITIALIZED, Boolean.TRUE);

        return meta;
    }

    //- PRIVATE

    private static final String ASSERTION_BASIC_NAME = "Build RST SOAP Request";
    private static final String META_INITIALIZED = BuildRstSoapRequest.class.getName() + ".metadataInitialized";

    private SecurityTokenType tokenType;
    private SoapVersion soapVersion;
    private String wsTrustNamespace;
    private String issuerAddress;
    private String appliesToAddress;
    private Integer keySize;
    private Long lifetime;
    private TimeUnit lifetimeTimeUnit; // for UI use only
    private boolean includeClientEntropy;
    private String variablePrefix = DEFAULT_VARIABLE_PREFIX;
}
