package com.l7tech.external.assertions.xmlsec;

import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.objectmodel.migration.MigrationMappingSelection;
import com.l7tech.objectmodel.migration.PropertyResolver;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.validator.ElementSelectingXpathBasedAssertionValidator;
import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.util.FullQName;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Immediately verify one or more signed Elements in a non-SOAP XML message.
 */
public class NonSoapVerifyElementAssertion extends NonSoapSecurityAssertionBase implements SetsVariables, HasVariablePrefix, UsesEntities {
    private static final String META_INITIALIZED = NonSoapVerifyElementAssertion.class.getName() + ".metadataInitialized";
    public static final String VAR_ELEMENTS_VERIFIED = "elementsVerified";
    public static final String VAR_SIGNATURE_METHOD_URIS = "signatureMethodUris";
    public static final String VAR_DIGEST_METHOD_URIS = "digestMethodUris";
    public static final String VAR_SIGNING_CERTIFICATES = "signingCertificates";
    public static final String VAR_SIGNATURE_VALUES = "signatureValues";
    public static final String VAR_SIGNATURE_ELEMENTS = "signatureElements";

    public static final Set<FullQName> DEFAULT_ID_ATTRS = Collections.unmodifiableSet(new LinkedHashSet<FullQName>() {{
        add(new FullQName("http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd", null, "Id"));
        add(new FullQName("http://schemas.xmlsoap.org/ws/2002/07/utility", null, "Id"));
        add(new FullQName("http://schemas.xmlsoap.org/ws/2003/06/utility", null, "Id"));
        add(new FullQName("http://schemas.xmlsoap.org/ws/2003/06/utility", null, "Id"));
        add(new FullQName("urn:oasis:names:tc:SAML:1.0:assertion", "local", "AssertionID"));
        add(new FullQName("urn:oasis:names:tc:SAML:2.0:assertion", "local", "ID"));
        add(new FullQName(null, null, "Id"));
        add(new FullQName(null, null, "id"));
        add(new FullQName(null, null, "ID"));
    }});

    protected String variablePrefix = "";
    private String verifyCertificateName;
    private long verifyCertificateOid;
    private boolean ignoreKeyInfo;
    private FullQName[] customIdAttrs;

    public NonSoapVerifyElementAssertion() {
        super(TargetMessageType.REQUEST, false);
        setXpathExpression(createDefaultXpathExpression(false, null));
    }

    public boolean isIgnoreKeyInfo() {
        return ignoreKeyInfo;
    }

    public void setIgnoreKeyInfo(boolean ignoreKeyInfo) {
        this.ignoreKeyInfo = ignoreKeyInfo;
    }

    @Override
    public String getVariablePrefix() {
        return variablePrefix;
    }

    @Override
    public void setVariablePrefix(String variablePrefix) {
        this.variablePrefix = variablePrefix;
    }

    @Override
    public String[] suffixes(){
        return new String[]{VAR_ELEMENTS_VERIFIED,
                            VAR_SIGNATURE_METHOD_URIS,
                            VAR_DIGEST_METHOD_URIS,
                            VAR_SIGNING_CERTIFICATES,
                            VAR_SIGNATURE_VALUES,
                            VAR_SIGNATURE_ELEMENTS};

    }

    public String getVerifyCertificateName() {
        return verifyCertificateName;
    }

    public void setVerifyCertificateName(String verifyCertificateName) {
        this.verifyCertificateName = verifyCertificateName;
    }

    public long getVerifyCertificateOid() {
        return verifyCertificateOid;
    }

    public void setVerifyCertificateOid(long verifyCertificateOid) {
        this.verifyCertificateOid = verifyCertificateOid;
    }

    public FullQName[] getCustomIdAttrs() {
        return customIdAttrs;
    }

    public void setCustomIdAttrs(FullQName[] customIdAttrs) {
        this.customIdAttrs = customIdAttrs;
    }

    @Override
    public VariableMetadata[] getVariablesSet() {
        return mergeVariablesSet(new VariableMetadata[] {
                new VariableMetadata(prefix(VAR_ELEMENTS_VERIFIED), false, true, prefix(VAR_ELEMENTS_VERIFIED), false, DataType.ELEMENT),
                new VariableMetadata(prefix(VAR_SIGNATURE_METHOD_URIS), false, true, prefix(VAR_SIGNATURE_METHOD_URIS), false, DataType.STRING),
                new VariableMetadata(prefix(VAR_DIGEST_METHOD_URIS), false, true, prefix(VAR_DIGEST_METHOD_URIS), false, DataType.STRING),
                new VariableMetadata(prefix(VAR_SIGNING_CERTIFICATES), false, true, prefix(VAR_SIGNING_CERTIFICATES), false, DataType.CERTIFICATE),
                new VariableMetadata(prefix(VAR_SIGNATURE_VALUES), false, true, prefix(VAR_SIGNATURE_VALUES), false, DataType.STRING),
                new VariableMetadata(prefix(VAR_SIGNATURE_ELEMENTS), false, true, prefix(VAR_SIGNATURE_ELEMENTS), false, DataType.ELEMENT),
        });
    }

    /**
     * Prepend the current variable prefix, if any, to the specified variable name.  If the current prefix is
     * null or empty this will return the input variable name unchanged.
     *
     * @param var  the variable name to prefix.  Required.
     * @return the variable name with the current prefix prepended, along with a dot; or the variable name unchanged if the prefix is currently null or empty.
     */
    public String prefix(String var) {
        String prefix = getVariablePrefix();
        return prefix == null || prefix.trim().length() < 1 ? var : prefix.trim() + "." + var;
    }

    private final static String baseName = "(Non-SOAP) Verify XML Element";

    @Override
    public String getDefaultXpathExpressionString() {
        return "//ds:Signature";
    }

    @Override
    public String getDisplayName() {
        return baseName;
    }

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        meta.put(AssertionMetadata.SHORT_NAME, baseName);
        meta.put(META_PROP_VERB, "verify");
        meta.put(AssertionMetadata.DESCRIPTION, "Immediately verify one or more signatures of the message.  " +
                                                "This does not require a SOAP Envelope and does not examine or produce WS-Security processor results.  " +
                                                "Instead, this assertion examines the target message immediately.  The XPath should match the Signature elements " +
                                                "which are to be verified.");
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[]{"xmlSecurity"});
        meta.put(AssertionMetadata.PALETTE_NODE_SORT_PRIORITY, -1110);
        meta.put(AssertionMetadata.PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.external.assertions.xmlsec.console.NonSoapVerifyElementAssertionPropertiesDialog");
        meta.put(AssertionMetadata.PROPERTIES_ACTION_NAME, "(Non-SOAP) XML Element Verification Properties");

        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/xmlencryption.gif");
        meta.put(AssertionMetadata.POLICY_NODE_NAME_FACTORY, policyNameFactory);

        meta.put(AssertionMetadata.POLICY_VALIDATOR_CLASSNAME, ElementSelectingXpathBasedAssertionValidator.class.getName());
        meta.put(AssertionMetadata.FEATURE_SET_NAME, "(fromClass)");

        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }

    @Override
    @Migration(mapName = MigrationMappingSelection.REQUIRED, resolver = PropertyResolver.Type.ASSERTION)
    public EntityHeader[] getEntitiesUsed() {
        EntityHeader[] headers = new EntityHeader[0];

        if (verifyCertificateOid > 0) {
            headers = new EntityHeader[] {new EntityHeader(verifyCertificateOid, EntityType.TRUSTED_CERT, null, null)};
        }

        return headers;
    }

    @Override
    public void replaceEntity(EntityHeader oldEntityHeader, EntityHeader newEntityHeader) {
        if( oldEntityHeader.getType() == EntityType.TRUSTED_CERT &&
            newEntityHeader.getType() == EntityType.TRUSTED_CERT &&
            verifyCertificateOid == oldEntityHeader.getOid()) {
            verifyCertificateOid = newEntityHeader.getOid();
        }
    }
}
