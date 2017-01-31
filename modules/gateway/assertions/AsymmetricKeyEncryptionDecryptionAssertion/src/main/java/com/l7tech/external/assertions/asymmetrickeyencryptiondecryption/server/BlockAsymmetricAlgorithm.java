package com.l7tech.external.assertions.asymmetrickeyencryptiondecryption.server;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * BlockAsymmetricAlgorithm defines the constants for Block Asymmetric Ciphers
 */
public class BlockAsymmetricAlgorithm implements Serializable {

    public static final String NAME_RSA = "RSA";

    public static final String MODE_ECB = "ECB";
    public static final String MODE_NONE = "NONE";

    public static final String PADDING_NO_PADDING = "NoPadding";
    public static final String PADDING_PKCS1_PADDING = "PKCS1Padding";
    public static final String PADDING_OAEP_WITH_MD5_AND_MGF1_PADDING    = "OAEPWithMD5AndMGF1Padding";
    public static final String PADDING_OAEP_WITH_SHA1_AND_MGF1_PADDING   = "OAEPWithSHA1AndMGF1Padding";
    public static final String PADDING_OAEP_WITH_SHA224_AND_MGF1_PADDING = "OAEPWithSHA224AndMGF1Padding";
    public static final String PADDING_OAEP_WITH_SHA256_AND_MGF1_PADDING = "OAEPWithSHA256AndMGF1Padding";
    public static final String PADDING_OAEP_WITH_SHA384_AND_MGF1_PADDING = "OAEPWithSHA384AndMGF1Padding";
    public static final String PADDING_OAEP_WITH_SHA512_AND_MGF1_PADDING = "OAEPWithSHA512AndMGF1Padding";

    public static final String PADDING_OAEP_WITH_SHA3_224_AND_MGF1_PADDING = "OAEPWithSHA3-224AndMGF1Padding";
    public static final String PADDING_OAEP_WITH_SHA3_256_AND_MGF1_PADDING = "OAEPWithSHA3-256AndMGF1Padding";
    public static final String PADDING_OAEP_WITH_SHA3_384_AND_MGF1_PADDING = "OAEPWithSHA3-384AndMGF1Padding";
    public static final String PADDING_OAEP_WITH_SHA3_512_AND_MGF1_PADDING = "OAEPWithSHA3-512AndMGF1Padding";

    public static final String PADDING_ISO9796_1_PADDING = "ISO9796-1Padding";

    public static final List<String> names = new ArrayList<>();
    public static final List<String> modes = new ArrayList<>();
    public static final List<String> paddings = new ArrayList<>();
    static {
        names.add(NAME_RSA);

        modes.add(MODE_ECB);
        modes.add(MODE_NONE);

        paddings.add(PADDING_NO_PADDING);
        paddings.add(PADDING_PKCS1_PADDING);
        paddings.add(PADDING_OAEP_WITH_MD5_AND_MGF1_PADDING);
        paddings.add(PADDING_OAEP_WITH_SHA1_AND_MGF1_PADDING);
        paddings.add(PADDING_OAEP_WITH_SHA224_AND_MGF1_PADDING);
        paddings.add(PADDING_OAEP_WITH_SHA256_AND_MGF1_PADDING);
        paddings.add(PADDING_OAEP_WITH_SHA384_AND_MGF1_PADDING);
        paddings.add(PADDING_OAEP_WITH_SHA512_AND_MGF1_PADDING);

        // BC supported list
        // The following are adding in BC 1.55
//        paddings.add(PADDING_OAEP_WITH_SHA3_224_AND_MGF1_PADDING);
//        paddings.add(PADDING_OAEP_WITH_SHA3_256_AND_MGF1_PADDING);
//        paddings.add(PADDING_OAEP_WITH_SHA3_384_AND_MGF1_PADDING);
//        paddings.add(PADDING_OAEP_WITH_SHA3_512_AND_MGF1_PADDING);
        paddings.add(PADDING_ISO9796_1_PADDING);
    }

    private static final Pattern algorithmPattern = Pattern.compile("\\w/\\w/\\w");

    /**
     * Get all known algorithm names
     * @return List of known algorithm names
     */
    public static List<String> getKnownNames() {
        return Collections.unmodifiableList(names);
    }

    /**
     * Get all known modes
     * @return List of know modes string
     */
    public static List<String> getKnownModes() {
        return Collections.unmodifiableList(modes);
    }

    /**
     * Get all known algorithms
     * @return List of known paddings string
     */
    public static List<String> getKnownPaddings() {
        return Collections.unmodifiableList(paddings);
    }

    public static String getAlgorithm(String name, String mode, String padding) {
        return name + "/" + mode + "/" + padding;
    }

    public static String[] parseAlgorithm(String algorithm) {
        String[] parsed = algorithm.split("/");

        if (!algorithmPattern.matcher(algorithm).matches() && parsed.length != 3) {
            throw new IllegalArgumentException("Algorithm pattern must match \"Algorithm Cipher Name/Algorithm Cipher Mode/Algorithm Cipher Padding\"");
        }

        return parsed;
    }
}
