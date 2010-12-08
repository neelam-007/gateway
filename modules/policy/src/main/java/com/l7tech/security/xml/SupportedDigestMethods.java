package com.l7tech.security.xml;

import java.util.*;

/**
 * Mesasge digest algorithms supported by the gateway.
 *
 * @see com.l7tech.security.xml.SupportedSignatureMethods
 */
public enum SupportedDigestMethods {

    // - PUBLIC

    SHA1("SHA-1", "http://www.w3.org/2000/09/xmldsig#sha1", "SHA", "SHA1"),
    // todo: SHA224("SHA-224", ),
    SHA256("SHA-256", "http://www.w3.org/2001/04/xmlenc#sha256", "SHA256"),
    SHA384("SHA-384", "http://www.w3.org/2001/04/xmldsig-more#sha384", "SHA384"),
    SHA512("SHA-512", "http://www.w3.org/2001/04/xmlenc#sha512", "SHA512");

    SupportedDigestMethods(String canonicalName, String identifier, String... aliases) {
        this.canonicalName = canonicalName;
        this.identifier = identifier;
        this.aliases = new ArrayList<String>(Arrays.asList(aliases));
        this.aliases.add(0, canonicalName);
    }

    /**
     * @return the message digest method's canonical name, e.g. "SHA-1"
     */
    public String getCanonicalName() {
        return canonicalName;
    }

    /**
     * @return the message digest method identifier, e.g. http://www.w3.org/2000/09/xmldsig#sha1
     */
    public String getIdentifier() {
        return identifier;
    }

    /**
     * @return a list of known aliases for the message digest method, with the canonical name on the first position
     */
    public List<String> getAliases() {
        return aliases;
    }

    public static List<String> getAlgorithmIds() {
        List<String> result = new ArrayList<String>();
        for(SupportedDigestMethods digest : values()) {
            result.add(digest.getIdentifier());
        }
        return result;
    }

    /**
     * Get list of digest algorithm names that it would be reasonable to offer in a GUI for selecting a signature and digest method.
     *
     * @return an array of canonical algorithm names (ie, { "SHA-1", "SHA-256" } ).  Never null or empty.
     */
    public static String[] getDigestNames() {
        List<String> result = new ArrayList<String>();
        for(SupportedDigestMethods digest : values()) {
            result.add(digest.getCanonicalName());
        }
        return result.toArray(new String[result.size()]);
    }

    /**
     * Get a collection of known aliases for the specified JCE digest algorithm name.
     *
     * @param digestName a digest algorithm name, ie "SHA-1".  Required.
     * @return  a collection of recognized aliases for the specified digest, ie { "SHA-1", "SHA1", "SHA" }, or null.
     */
    public static List<String> getAliases(String digestName) {
        SupportedDigestMethods digest = allAliases.get(digestName.toUpperCase());
        return digest == null ? null : digest.getAliases();
    }

    /**
     * Get the message digest method for the provided alias.
     *
     * @param digestAlias the digest method alias, e.g. "SHA1"
     * @return the message digest method
     * @throws IllegalArgumentException if the provided digestAlias is not supported
     */
    public static SupportedDigestMethods fromAlias(String digestAlias) {
        SupportedDigestMethods digest = allAliases.get(digestAlias);
        if (digest == null)
            throw new IllegalArgumentException("Unsupported digest method: " + digestAlias);
        return digest;
    }

    /**
     * Get the message digest for the provided digest algorithm identifier.
     * @param algorithm the digest algorithm identifier
     * @return the message digest
     * @throws IllegalArgumentException if the provided digest algorithm is not supported
     */
    public static SupportedDigestMethods fromAlgorithmId(String algorithm) {
        for(SupportedDigestMethods digest : values()) {
            if (digest.getIdentifier().equalsIgnoreCase(algorithm))
                return digest;
        }
        throw new IllegalArgumentException("Unsupported digest algorithm: " + algorithm);
    }

    // - PRIVATE

    private final String canonicalName;
    private final String identifier;
    private final List<String> aliases;

    private static final Map<String, SupportedDigestMethods> allAliases = new HashMap<String, SupportedDigestMethods>();
    static {
        for(SupportedDigestMethods digest : SupportedDigestMethods.values()) {
            for(String alias : digest.getAliases()) {
                allAliases.put(alias, digest);
            }
        }
    }
}
