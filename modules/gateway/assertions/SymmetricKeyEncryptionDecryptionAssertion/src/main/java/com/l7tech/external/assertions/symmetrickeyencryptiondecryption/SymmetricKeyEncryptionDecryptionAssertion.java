package com.l7tech.external.assertions.symmetrickeyencryptiondecryption;

import com.l7tech.policy.assertion.*;
import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.policy.variable.VariableMetadata;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 *
 */
public class SymmetricKeyEncryptionDecryptionAssertion extends Assertion implements UsesVariables, SetsVariables {
    protected static final Logger logger = Logger.getLogger(SymmetricKeyEncryptionDecryptionAssertion.class.getName());

    private static final boolean IS_ENCRYPT_VALUE = true;
    private static final boolean IS_ASCII_ARMOUR = false;
    private static final boolean DEFAULT_IS_PGP_PUB_KEY_ENCRYPT = false;

    public static final String DEFAULT_VARIABLE_NAME = "symmetricEncrypDecrypOutput";
    public static final String DEFAULT_TRANS_SEPERATOR = "/";
    public static final String ALGORITHM_AES = "AES";
    public static final String ALGORITHM_DES = "DES";
    public static final String ALGORITHM_TRIPLE_DES = "DESede";
    public static final String BLOCK_MODE_CBC = "CBC";
    public static final String BLOCK_MODE_GCM = "GCM";
    public static final String BLOCK_MODE_ECB = "ECB";
    public static final int GCM_AUTHENTICATION_TAG_LENGTH_BITS = 128; // Default is 16 bytes (which is 128 bits)

    private boolean isEncrypt = IS_ENCRYPT_VALUE;
    private boolean asciiArmourEnabled = IS_ASCII_ARMOUR;

    private String text;
    private String Algorithm;
    private String key;
    private String iv;
    private String pgpPassPhrase;
    private boolean isPgpKeyEncryption = DEFAULT_IS_PGP_PUB_KEY_ENCRYPT;
    private String outputVariableName = DEFAULT_VARIABLE_NAME;

    // AES Transformation
    public static final String TRANS_AES_CBC_PKCS5Padding = "AES/CBC/PKCS5Padding";

    public static final String TRANS_AES_ECB_PKCS5Padding = "AES/ECB/PKCS5Padding";

    public static final String TRANS_AES_ECB_PKCS7Padding = "AES/ECB/PKCS7Padding";

    public static final String TRANS_AES_GCM_NoPadding = "AES/GCM/NoPadding";

    // DES Transformation
    public static final String TRANS_DES_CBC_PKCS5Padding = "DES/CBC/PKCS5Padding";

    // Triple DES also known as DESede transformations
    public static final String TRANS_DESede_CBC_PKCS5Padding = "DESede/CBC/PKCS5Padding";

    // PGP Transformation
    public static final String TRANS_PGP = "PGP";

    //PGP Encryption types
    public static final String PGP_PASS_ENCRYPT = "Passphrase";
    public static final String PGP_PUBLIC_KEY_ENCRYPT = "Public Key";

    public boolean getIsPgpKeyEncryption() {return isPgpKeyEncryption ; }

    public void setIsPgpKeyEncryption(final boolean isPgpKeyEncryption){
        this.isPgpKeyEncryption = isPgpKeyEncryption;
     }

    public Boolean getIsEncrypt() {
        return isEncrypt;
    }

    public void setIsEncrypt(Boolean encrypt) {
        isEncrypt = encrypt;
    }

    public boolean isAsciiArmourEnabled() {
        return asciiArmourEnabled;
    }

    public void setAsciiArmourEnabled(final boolean asciiArmourEnabled) {
        this.asciiArmourEnabled = asciiArmourEnabled;
    }
    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getAlgorithm() {
        return Algorithm;
    }

    public void setAlgorithm(String algorithm) {
        Algorithm = algorithm;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getIv() {
        return iv;
    }

    public void setIv(String iv) {
        this.iv = iv;
    }

    public String getOutputVariableName() {
        return outputVariableName;
    }

    public void setOutputVariableName(String outputVariableName) {
        this.outputVariableName = outputVariableName;
    }


    public String getPgpPassPhrase() {
        return pgpPassPhrase;
    }

    public void setPgpPassPhrase(String pgpPassPhrase) {
        this.pgpPassPhrase = pgpPassPhrase;
    }

    public String[] getVariablesUsed() {
        StringBuffer sb = new StringBuffer();
        sb.append(text);
        sb.append(" ").append(key);
        sb.append(" ").append(iv);
        sb.append(" ").append(pgpPassPhrase);
        return Syntax.getReferencedNames(sb.toString());
    }

    public VariableMetadata[] getVariablesSet() {
        return new VariableMetadata[]{new VariableMetadata(this.outputVariableName, false, false, null, true, DataType.STRING)};
    }

    //
    // Metadata
    //
    private static final String META_INITIALIZED = SymmetricKeyEncryptionDecryptionAssertion.class.getName() + ".metadataInitialized";

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
        meta.put(AssertionMetadata.SHORT_NAME, "Symmetric Key Encryption / Decryption Assertion");
        meta.put(AssertionMetadata.LONG_NAME, "Symmetric Key Encryption / Decryption Assertion");

        meta.put(AssertionMetadata.PALETTE_NODE_SORT_PRIORITY, 999);
        meta.put(AssertionMetadata.PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.external.assertions.symmetrickeyencryptiondecryption.console.SymmetricKeyEncryptionDecryptionAssertionDialog");
        meta.put(AssertionMetadata.PROPERTIES_ACTION_NAME, "Symmetric Key Encryption / Decryption Assertion Properties");

        // Add to palette folder(s) 
        //   accessControl, transportLayerSecurity, xmlSecurity, xml, routing, 
        //   misc, audit, policyLogic, threatProtection 
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[]{"xmlSecurity"});
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/xmlencryption.gif");

        // Enable automatic policy advice (default is no advice unless a matching Advice subclass exists)
        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto");

        // request default feature set name for our class name, since we are a known optional module
        // that is, we want our required feature set to be "assertion:SymmetricKeyEncryptionDecryption" rather than "set:modularAssertions"
        meta.put(AssertionMetadata.FEATURE_SET_NAME, "set:modularAssertions");

        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }

}
