package com.l7tech.external.assertions.asymmetrickeyencryptiondecryption;

import com.l7tech.external.assertions.asymmetrickeyencryptiondecryption.server.BlockAsymmetricAlgorithm;
import com.l7tech.external.assertions.asymmetrickeyencryptiondecryption.server.RsaModePaddingOption;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.wsp.Java5EnumTypeMapping;
import com.l7tech.policy.wsp.SimpleTypeMappingFinder;
import com.l7tech.policy.wsp.TypeMapping;
import com.l7tech.util.GoidUpgradeMapper;

import javax.crypto.Cipher;
import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Logger;

import static com.l7tech.policy.assertion.AssertionMetadata.POLICY_NODE_NAME_FACTORY;
import static com.l7tech.policy.assertion.AssertionMetadata.WSP_SUBTYPE_FINDER;

public class AsymmetricKeyEncryptionDecryptionAssertion extends Assertion implements UsesVariables, UsesEntities {

    protected static final Logger logger = Logger.getLogger(AsymmetricKeyEncryptionDecryptionAssertion.class.getName());

    private String inputVariable = "";
    private String outputVariable = "";

    private String keyName = "";
    private Goid keyGoid = Goid.DEFAULT_GOID;

    private int mode = 0;
    private RsaModePaddingOption modePaddingOption = null;
    private String algorithm;

    public String[] getVariablesUsed() {
        return new String[]{inputVariable, outputVariable};
    }

    private static final String baseName = "Asymmetric Key Encryption / Decryption Assertion";

    private static final AssertionNodeNameFactory<AsymmetricKeyEncryptionDecryptionAssertion> nodeNameFactory = new AssertionNodeNameFactory<AsymmetricKeyEncryptionDecryptionAssertion>() {
        @Override
        public String getAssertionName(AsymmetricKeyEncryptionDecryptionAssertion assertion, boolean decorate) {
            if (!decorate) return baseName;

            StringBuilder sb = new StringBuilder(baseName);
            if (assertion.getMode() == Cipher.ENCRYPT_MODE) {
                sb.append(" Cert: ").append(assertion.getKeyName());
            } else {
                sb.append(" Key: ").append(assertion.getKeyName());
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
        meta.put(AssertionMetadata.SHORT_NAME, baseName);

        // Add to palette folder(s) 
        //   accessControl, transportLayerSecurity, xmlSecurity, xml, routing, 
        //   misc, audit, policyLogic, threatProtection
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[]{"xmlSecurity"});
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/xmlencryption.gif");

        //setup hooks for properties editor dialog
        meta.put(AssertionMetadata.PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.external.assertions.asymmetrickeyencryptiondecryption.console.AsymmetricKeyEncryptionDecryptionAssertionDialog");
        meta.put(AssertionMetadata.PROPERTIES_ACTION_NAME, "Asymmetric Key Encryption / Decryption Assertion Properties");

        Collection<TypeMapping> othermappings = new ArrayList<TypeMapping>();
        othermappings.add(new Java5EnumTypeMapping(RsaModePaddingOption.class, "rsaModePaddingOption"));
        meta.put(WSP_SUBTYPE_FINDER, new SimpleTypeMappingFinder(othermappings));

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

    public Goid getKeyGoid() {
        return keyGoid;
    }

    public void setKeyGoid(Goid keyGoid) {
        this.keyGoid = keyGoid;
    }

    /**
     * Needed for backwards serializing compatibility (TAC-444).
     */
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
        if (mode == Cipher.ENCRYPT_MODE) {
            if (oldEntityHeader.getType().equals(EntityType.TRUSTED_CERT) && oldEntityHeader.getGoid().equals(keyGoid) &&
                    newEntityHeader.getType().equals(EntityType.TRUSTED_CERT)) {
                keyGoid = newEntityHeader.getGoid();
            }
        }
    }
}
