package com.l7tech.external.assertions.generatesecurityhash;

import com.l7tech.policy.assertion.*;
import com.l7tech.policy.assertion.annotation.Base64Value;
import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.util.Charsets;
import com.l7tech.util.HexUtils;

import java.util.*;
import java.util.regex.Pattern;

/**
 * <p>
 * A modular assertion to generate a hash or keyed hash for non-binary data.
 * </p>
 * <p>
 * This assertion supports the following keyed hash algorithm;
 * <ul>
 *     <li>HMAC-SHA1</li>
 *     <li>HMAC-SHA256</li>
 *     <li>HMAC-SHA384</li>
 *     <li>HMAC-SHA512</li>
 * </ul>
 * and the following hash algorithm;
 * <ul>
 *     <li>MD5</li>
 *     <li>SHA-1</li>
 *     <li>SHA-256</li>
 *     <li>SHA-384</li>
 *     <li>SHA-512</li>
 * </ul>
 * </p>
 * 
 */
public class GenerateSecurityHashAssertion extends Assertion implements UsesVariables, SetsVariables {

    /**
     * The pattern to determine if an alogorithm is an HMAC algorithm or not.
     */
    public static final Pattern HMAC_ALGORITHM = Pattern.compile("(?i)HMAC.*");

    /**
     * The default algorithm.
     */
    public static final String DEFAULT_ALGORITHM = "HMAC-SHA1";

    private static final String META_INITIALIZED = GenerateSecurityHashAssertion.class.getName() + ".metadataInitialized";

    private String base64Data;
    private String keyText;
    private String targetOutputVariable;
    private String algorithm = DEFAULT_ALGORITHM;

    private static final Map<String, String> SUPPORTED_ALGORITHM;
    
    static {
        Map<String, String> m = new LinkedHashMap<String, String>();
        m.put("HMAC-SHA1", "HmacSHA1");
        m.put("HMAC-SHA256", "HmacSHA256");
        m.put("HMAC-SHA384", "HmacSHA384");
        m.put("HMAC-SHA512", "HmacSHA512");
        m.put("HMAC-MD5", "HmacMD5");
        m.put("MD5", "MD5");
        m.put("SHA-1", "SHA-1");
        m.put("SHA-256", "SHA-256");
        m.put("SHA-384", "SHA-384");
        m.put("SHA-512", "SHA-512");
        SUPPORTED_ALGORITHM = Collections.unmodifiableMap(m);
    }

    private LineBreak lineBreak;

    /**
     * 
     * @return a map of supported algorithms.
     */
    public static Map<String, String> getSupportedAlgorithm(){
        return SUPPORTED_ALGORITHM;
    }

    /**
     * 
     * @return the target output variable name.
     */
    public String getTargetOutputVariable() {
        return targetOutputVariable;
    }

    /**
     * 
     * @param targetOutputVariable the target output variable name to use to store the generated signature.
     */
    public void setTargetOutputVariable(final String targetOutputVariable) {
        this.targetOutputVariable = targetOutputVariable;
    }

    /**
     * 
     * @return the key to use for HMAC algorithm.
     */
    public String getKeyText() {
        return keyText;
    }

    /**
     * 
     * @param keyText the key to use for HMAC algorithm.
     */
    public void setKeyText(final String keyText) {
        this.keyText = keyText;
    }

    /**
     * 
     * @return the non-binary data to sign.
     */
    public String dataToSignText() {
        if(base64Data != null){
            return new String(HexUtils.decodeBase64(base64Data, true), Charsets.UTF8);
        }
        return base64Data;
    }

    /**
     * 
     * @param dataToSignText the non-binary data to sign.
     */
    public void setDataToSignText(final String dataToSignText) {
        if(dataToSignText != null){
            setBase64Data(HexUtils.encodeBase64(HexUtils.encodeUtf8(dataToSignText), true));
        }
    }

    /**
     *
     * @return the base 64 encoded data.
     */
    @Base64Value(decodeMethodName = "dataToSignText")
    public String getBase64Data() {
        return base64Data;
    }

    /**
     * @param base64Data the base 64 encoded text data.
     */
    public void setBase64Data(final String base64Data) {
        this.base64Data = base64Data;
    }

    /**
     * 
     * @return the algorithm being used.
     */
    public String getAlgorithm() {
        return algorithm;
    }

    /**
     * 
     * @param algorithm the algorithm to use.
     */
    public void setAlgorithm(final String algorithm) {
        this.algorithm = algorithm;
    }

    /**
     *
     * @return the line break character.
     */
    public LineBreak getLineBreak() {
        return lineBreak;
    }

    /**
     *
     * @param lineBreak the line break character to use.
     */
    public void setLineBreak(final LineBreak lineBreak) {
        this.lineBreak = lineBreak;
    }

    @Override
    public String[] getVariablesUsed() {
        final StringBuilder sb = new StringBuilder();
        sb.append(this.getKeyText());
        sb.append(" ").append(dataToSignText());
        return Syntax.getReferencedNames(sb.toString());
    }

    @Override
    public VariableMetadata[] getVariablesSet() {
        if(targetOutputVariable == null || targetOutputVariable.trim().isEmpty())return new VariableMetadata[0];
        return new VariableMetadata[]{
                new VariableMetadata(targetOutputVariable, false, false, null, true, DataType.STRING)
        };
    }

    public AssertionMetadata meta() {
        final DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        meta.put(AssertionMetadata.SHORT_NAME, "Generate Security Hash");
        meta.put(AssertionMetadata.LONG_NAME, "Generate a hash or keyed hash (HMAC) for non-binary data.");
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[]{"xmlSecurity"});
        meta.put(AssertionMetadata.PALETTE_NODE_SORT_PRIORITY, 999);
        meta.put(AssertionMetadata.PROPERTIES_ACTION_NAME, "Generate Security Hash Properties");
        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto");
        meta.put(AssertionMetadata.POLICY_NODE_ICON, "com/l7tech/console/resources/xmlencryption.gif");
        meta.put(AssertionMetadata.FEATURE_SET_NAME, "(fromClass)");
        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }
}
