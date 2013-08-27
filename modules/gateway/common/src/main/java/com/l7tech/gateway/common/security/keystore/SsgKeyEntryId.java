package com.l7tech.gateway.common.security.keystore;

import com.l7tech.objectmodel.Goid;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Represents a keystore ID and a key alias.
 */
public class SsgKeyEntryId {
    private static final Pattern PATTERN = Pattern.compile("^([0-9a-fA-F]{32}|\\d{1,20}):(.*)$", Pattern.DOTALL);

    private final Goid keystoreId;
    private final String alias;

    /**
     * Create an SsgKeyEntryId from the specified keystoreId and alias.
     *
     * @param keystoreId  the GOID of the SsgKeyFinder that owns this alias.  Required.
     * @param alias       the unique alias within the SsgKeyFinder.  Required.
     */
    public SsgKeyEntryId(Goid keystoreId, String alias) {
        this.keystoreId = keystoreId;
        this.alias = alias;
    }

    /**
     * Create an SsgKeyEntryId from the specified key ID string.
     *
     * @param keyId a key ID in the format keystoreId + ":" + key alias.  Required.
     * @throws IllegalArgumentException if keyId is not in the correct format.
     */
    public SsgKeyEntryId(String keyId) throws IllegalArgumentException {
        Matcher matcher = PATTERN.matcher(keyId);
        String ksis = null;
        String alias = null;
        if (matcher.matches()) {
            ksis = matcher.group(1);
            alias = matcher.group(2);
        }

        if (ksis == null || alias == null)
            throw new IllegalArgumentException("Bad SsgKeyEntryId String format (should be \"keystoreId:alias\")");

        this.keystoreId = Goid.parseGoid(ksis);
        this.alias = alias;
    }

    /**
     * Create an SsgKeyEntryId from the specified key ID string.
     *
     * @param keyId a key ID in the format keystoreId + ":" + key alias.  Required.
     * @return the corresponding SsgKeyEntryId instance.  Never null.
     * @throws IllegalArgumentException if keyId is not in the correct format.
     */
    public static SsgKeyEntryId fromString(String keyId) throws IllegalArgumentException {
        return new SsgKeyEntryId(keyId);
    }
}
