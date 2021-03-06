package com.l7tech.external.assertions.asymmetrickeyencryptiondecryption;

import com.l7tech.external.assertions.asymmetrickeyencryptiondecryption.server.BlockAsymmetricAlgorithm;
import com.l7tech.external.assertions.asymmetrickeyencryptiondecryption.server.RsaModePaddingOption;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.policy.wsp.Java5EnumTypeMapping;
import com.l7tech.policy.wsp.SimpleTypeMappingFinder;
import com.l7tech.policy.wsp.TypeMapping;
import com.l7tech.util.ArrayUtils;
import com.l7tech.util.GoidUpgradeMapper;

import javax.crypto.Cipher;
import java.util.ArrayList;
import java.util.Collection;

import static com.l7tech.policy.assertion.AssertionMetadata.POLICY_NODE_NAME_FACTORY;
import static com.l7tech.policy.assertion.AssertionMetadata.WSP_SUBTYPE_FINDER;

public class AsymmetricKeyEncryptionDecryptionAssertion extends Assertion implements UsesVariables, UsesEntities {

    private static final int DEFAULT_MODE = Cipher.ENCRYPT_MODE;
    private static final KeySource DEFAULT_KEY_TYPE= KeySource.FROM_STORE;

    private String inputVariable = "";
    private String outputVariable = "";

    private KeySource keySource = DEFAULT_KEY_TYPE;
    private String rsaKeyValue = "";

    private String keyName = "";
    private Goid keyGoid = Goid.DEFAULT_GOID;

    private int mode = DEFAULT_MODE;
    private RsaModePaddingOption modePaddingOption = null;
    private String algorithm;

    public enum KeySource {
        FROM_STORE,
        FROM_VALUE
    }

    public String[] getVariablesUsed() {
        final String[] refKeyValue = Syntax.getReferencedNames(rsaKeyValue);
        return ArrayUtils.concat(new String[]{inputVariable, outputVariable}, refKeyValue);
    }

    private static final String ASSERTION_BASE_NAME = "Asymmetric Key Encryption / Decryption Assertion";
    private static final String ASSERTION_ENCRYPT_TITLE = "Asymmetric Key Encrypt";
    private static final String ASSERTION_DECRYPT_TITLE = "Asymmetric Key Decrypt";

    private static final AssertionNodeNameFactory<AsymmetricKeyEncryptionDecryptionAssertion> nodeNameFactory = new AssertionNodeNameFactory<AsymmetricKeyEncryptionDecryptionAssertion>() {
        @Override
        public String getAssertionName(AsymmetricKeyEncryptionDecryptionAssertion assertion, boolean decorate) {
            if (!decorate) return ASSERTION_BASE_NAME;

            StringBuilder sb = new StringBuilder();

            if (assertion.getMode() == Cipher.ENCRYPT_MODE) {
                sb.append(ASSERTION_ENCRYPT_TITLE);
            } else {
                sb.append(ASSERTION_DECRYPT_TITLE);
            }
            sb.append(" using ");

            if (assertion.getKeySource() == KeySource.FROM_VALUE) {
                final String value = assertion.getRsaKeyValue().length() > 40 ?
                            assertion.getRsaKeyValue().substring(0, 40).concat("...") : assertion.getRsaKeyValue();
                sb.append("Value: ").append(value);
            } else {
                // Because default is FROM_STORE store. "Key" represents public or private key
                sb.append("Key: ").append(assertion.getKeyName());
            }

            return sb.toString();
        }
    };

    //
    // Metadata
    //
    private static final String META_INITIALIZED = AsymmetricKeyEncryptionDecryptionAssertion.class.getName() + ".metadataInitialized";

    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        // Set description for GUI
        meta.put(AssertionMetadata.SHORT_NAME, ASSERTION_BASE_NAME);

        // Add to palette folder(s) 
        //   accessControl, transportLayerSecurity, xmlSecurity, xml, routing, 
        //   misc, audit, policyLogic, threatProtection
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[]{"xmlSecurity"});
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/xmlencryption.gif");

        //setup hooks for properties editor dialog
        meta.put(AssertionMetadata.PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.external.assertions.asymmetrickeyencryptiondecryption.console.AsymmetricKeyEncryptionDecryptionAssertionDialog");
        meta.put(AssertionMetadata.PROPERTIES_ACTION_NAME, "Asymmetric Key Encryption / Decryption Assertion Properties");

        Collection<TypeMapping> otherTypeMappings = new ArrayList<>();
        otherTypeMappings.add(new Java5EnumTypeMapping(RsaModePaddingOption.class, "rsaModePaddingOption"));
        otherTypeMappings.add(new Java5EnumTypeMapping(KeySource.class, "keySource"));
        meta.put(WSP_SUBTYPE_FINDER, new SimpleTypeMappingFinder(otherTypeMappings));

        // Enable automatic policy advice (default is no advice unless a matching Advice subclass exists)
        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto");
        meta.put(POLICY_NODE_NAME_FACTORY, nodeNameFactory);

        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }

    public String getInputVariable() {
        return inputVariable;
    }

    public void setInputVariable(String inputVariable) {
        this.inputVariable = inputVariable;
    }

    public String getOutputVariable() {
        return outputVariable;
    }

    public void setOutputVariable(String outputVariable) {
        this.outputVariable = outputVariable;
    }

    public int getMode() {
        return mode;
    }

    public void setMode(int mode) {
        this.mode = mode;
    }

    public String getKeyName() {
        return keyName;
    }

    public void setKeyName(String keyName) {
        this.keyName = keyName;
    }

    public String getRsaKeyValue() {
        return rsaKeyValue;
    }

    public void setRsaKeyValue(String rsaKeyValue) {
        this.rsaKeyValue = rsaKeyValue;
    }

    public KeySource getKeySource() {
        return keySource;
    }

    public void setKeySource(KeySource keySource) {
        this.keySource = keySource;
    }

    public Goid getKeyGoid() {
        return keyGoid;
    }

    public void setKeyGoid(Goid keyGoid) {
        this.keyGoid = keyGoid;
    }

    /**
     * Needed for backwards serializing compatibility (TAC-444).
     */
    @SuppressWarnings("unused")
    public void setKeyId(long keyId) {
        this.keyGoid = GoidUpgradeMapper.mapOid(EntityType.SSG_KEY_ENTRY, keyId);
    }

    public RsaModePaddingOption getModePaddingOption() {
        return modePaddingOption;
    }

    public void setModePaddingOption(RsaModePaddingOption modePaddingOption) {

        switch (modePaddingOption) {
            case ECB_NO_PADDING:
                setAlgorithm(BlockAsymmetricAlgorithm.getAlgorithm(
                        BlockAsymmetricAlgorithm.NAME_RSA, BlockAsymmetricAlgorithm.MODE_ECB, BlockAsymmetricAlgorithm.PADDING_NO_PADDING));
                break;
            case NO_MODE_NO_PADDING: // Tested SunJCE/BC to be mapped to PKCS1
            case ECB_PKCS1_PADDING:
                setAlgorithm(BlockAsymmetricAlgorithm.getAlgorithm(
                        BlockAsymmetricAlgorithm.NAME_RSA, BlockAsymmetricAlgorithm.MODE_ECB, BlockAsymmetricAlgorithm.PADDING_PKCS1_PADDING));
                break;
            case ECP_OAEP_WITH_SHA1_AND_MDG1_PADDING:
                setAlgorithm(BlockAsymmetricAlgorithm.getAlgorithm(
                        BlockAsymmetricAlgorithm.NAME_RSA, BlockAsymmetricAlgorithm.MODE_ECB, BlockAsymmetricAlgorithm.PADDING_OAEP_WITH_SHA1_AND_MGF1_PADDING));
                break;
            default:
                setAlgorithm(BlockAsymmetricAlgorithm.getAlgorithm(
                        BlockAsymmetricAlgorithm.NAME_RSA, BlockAsymmetricAlgorithm.MODE_ECB, BlockAsymmetricAlgorithm.PADDING_NO_PADDING));

        }

        this.modePaddingOption = null;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    @Override
    public EntityHeader[] getEntitiesUsed() {
        if (mode == Cipher.ENCRYPT_MODE) {
            return new EntityHeader[]{new EntityHeader(Goid.toString(keyGoid), EntityType.TRUSTED_CERT, null, null)};
        }

        return new EntityHeader[]{};
    }

    @Override
    public void replaceEntity(EntityHeader oldEntityHeader, EntityHeader newEntityHeader) {
        if ((mode == Cipher.ENCRYPT_MODE)
                && oldEntityHeader.getType().equals(EntityType.TRUSTED_CERT)
                && oldEntityHeader.getGoid().equals(keyGoid)
                && newEntityHeader.getType().equals(EntityType.TRUSTED_CERT)) {
            keyGoid = newEntityHeader.getGoid();
        }
    }
}
