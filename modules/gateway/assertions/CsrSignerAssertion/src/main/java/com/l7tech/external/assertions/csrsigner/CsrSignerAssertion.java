package com.l7tech.external.assertions.csrsigner;

import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.util.GoidUpgradeMapper;
import org.jetbrains.annotations.Nullable;

/**
 * Assertion that can create a signed certificate from a CSR.
 */
public class CsrSignerAssertion extends Assertion implements UsesVariables, SetsVariables, PrivateKeyable {
    public static final String VAR_CERT = "certificate";
    public static final String VAR_CHAIN = "chain";

    private final PrivateKeyableSupport pks = new PrivateKeyableSupport();
    private String csrVariableName;
    private String outputPrefix;

    /**
     * @return the name of a context variable expected to contain a PKCS#10 certificate signing request
     *         in either PEM or DER format.
     */
    public String getCsrVariableName() {
        return csrVariableName;
    }

    /**
     * @param csrVariableName the name of a context variable expected to contain a PKCS#10 certificate signing request
     *                        in either PEM or DER format.
     */
    public void setCsrVariableName(String csrVariableName) {
        this.csrVariableName = csrVariableName;
    }

    /**
     * @return the prefix prepended to output context variables created by this assertion, or null if output variables are unprefixed.
     */
    public String getOutputPrefix() {
        return outputPrefix;
    }

    /**
     * @param outputPrefix a prefix to prepend to output context variables created by this assertion, or null to avoid prefixing them.
     */
    public void setOutputPrefix(@Nullable String outputPrefix) {
        this.outputPrefix = outputPrefix;
    }

    @Override
    public boolean isUsesDefaultKeyStore() {
        return pks.isUsesDefaultKeyStore();
    }

    @Override
    public void setUsesDefaultKeyStore(boolean usesDefault) {
        pks.setUsesDefaultKeyStore(usesDefault);
    }

    @Override
    public Goid getNonDefaultKeystoreId() {
        return pks.getNonDefaultKeystoreId();
    }

    @Override
    public void setNonDefaultKeystoreId(Goid nonDefaultId) {
        pks.setNonDefaultKeystoreId(nonDefaultId);
    }

    @Deprecated
    public void setNonDefaultKeystoreId(long nonDefaultId) {
        pks.setNonDefaultKeystoreId(GoidUpgradeMapper.mapOid(EntityType.SSG_KEYSTORE, nonDefaultId));
    }

    @Override
    public String getKeyAlias() {
        return pks.getKeyAlias();
    }

    @Override
    public void setKeyAlias(String keyid) {
        pks.setKeyAlias(keyid);
    }

    @Override
    public String[] getVariablesUsed() {
        return csrVariableName == null ? new String[0] : new String[] { csrVariableName };
    }

    @Override
    public VariableMetadata[] getVariablesSet() {
        return new VariableMetadata[] {
            new VariableMetadata(prefix(VAR_CERT), false, false, prefix(VAR_CERT), false, DataType.CERTIFICATE),
            new VariableMetadata(prefix(VAR_CHAIN), false, true, prefix(VAR_CHAIN), false, DataType.CERTIFICATE)
        };
    }

    public String prefix(String var) {
        return outputPrefix == null || outputPrefix.trim().length() < 1 ? var : (outputPrefix + "." + var);
    }

    //
    // Metadata
    //
    private static final String META_INITIALIZED = CsrSignerAssertion.class.getName() + ".metadataInitialized";

    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        // Set description for GUI
        meta.put(AssertionMetadata.SHORT_NAME, "Sign Certificate");
        meta.put(AssertionMetadata.LONG_NAME, "Sign Certificate Request");
        meta.put(AssertionMetadata.DESCRIPTION, "Process a PKCS#10 Certificate Signing Request and create a signed X.509 certificate using a specified CA private key.  " +
                                                "The CSR must be in binary DER format.  Use the Encode / Decode Data assertion if necessary.");

        meta.put(AssertionMetadata.PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.external.assertions.csrsigner.console.CsrSignerAssertionPropertiesDialog");

        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[] { "misc" });
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/cert16.gif");

        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto");

        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }
}
