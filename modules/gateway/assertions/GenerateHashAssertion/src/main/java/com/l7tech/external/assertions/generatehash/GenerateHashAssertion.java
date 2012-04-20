package com.l7tech.external.assertions.generatehash;

import com.l7tech.policy.assertion.*;
import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.policy.variable.VariableMetadata;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * <p>
 * A modular assertion to generate a hash or keyed hash for non-binary data.
 * </p>
 * <p>
 * This assertion supports the following keyed hash algorithm;
 * <ul>
 *     <li>HmacSHA1</li>
 *     <li>HmacSHA256</li>
 *     <li>HmacSHA384</li>
 *     <li>HmacSHA512</li>     
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
public class GenerateHashAssertion extends Assertion implements UsesVariables, SetsVariables {

    /**
     * The default output variable name.
     */
    public static final String DEFAULT_VARIABLE_NAME = "hash.output";

    /**
     * The pattern to determine if an alogorithm is an HMAC algorithm or not.
     */
    public static final Pattern HMAC_ALGORITHM = Pattern.compile("(?i)Hmac.*");

    /**
     * The default algorithm.
     */
    public static final String DEFAULT_ALGORITHM = "HmacSHA1";

    private static final String META_INITIALIZED = GenerateHashAssertion.class.getName() + ".metadataInitialized";

    private String keyText;
    private String dataToSignText;
    private String targetOutputVariable = DEFAULT_VARIABLE_NAME;
    private String algorithm = DEFAULT_ALGORITHM;

    private static final List<String> SUPPORTED_ALGORITHM;
    
    static {       
        List<String> l = new ArrayList<String>();
        l.add("HmacSHA1");
        l.add("HmacSHA256");
        l.add("HmacSHA384");
        l.add("HmacSHA512");
        l.add("MD5");
        l.add("SHA-1");
        l.add("SHA-256");
        l.add("SHA-384");
        l.add("SHA-512");
        SUPPORTED_ALGORITHM = Collections.unmodifiableList(l);
    }

    /**
     * 
     * @return a list of supported algorithms.
     */
    public static List<String> getSupportedAlgorithm(){
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
    public String getDataToSignText() {
        return dataToSignText;
    }

    /**
     * 
     * @param dataToSignText the non-binary data to sign.
     */
    public void setDataToSignText(final String dataToSignText) {
        this.dataToSignText = dataToSignText;
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

    @Override
    public String[] getVariablesUsed() {
        final StringBuffer sb = new StringBuffer();
        sb.append(this.getKeyText());
        sb.append(" ").append(getDataToSignText());
        sb.append(" ").append(getTargetOutputVariable());
        return Syntax.getReferencedNames(sb.toString());
    }

    @Override
    public VariableMetadata[] getVariablesSet() {
        return new VariableMetadata[]{
                new VariableMetadata(getTargetOutputVariable(), false, false, null, true, DataType.STRING)
        };
    }

    public AssertionMetadata meta() {
        final DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        meta.put(AssertionMetadata.SHORT_NAME, "Generate Security Hash Assertion");
        meta.put(AssertionMetadata.LONG_NAME, "Generate a hash or keyed hash (HMAC) for non-binary data.");
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[]{"xmlSecurity"});
        meta.put(AssertionMetadata.PALETTE_NODE_SORT_PRIORITY, 999);
        meta.put(AssertionMetadata.PROPERTIES_ACTION_NAME, "Generate Security Hash Assertion Properties");
        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto");
        meta.put(AssertionMetadata.POLICY_NODE_ICON, "com/l7tech/console/resources/xmlencryption.gif");
        meta.put(AssertionMetadata.FEATURE_SET_NAME, "set:modularAssertions");
        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }
}
