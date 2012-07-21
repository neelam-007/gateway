package com.l7tech.xml.xpath;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Typesafe enum representing a (possibly-unspecified) XPath version.
 */
public enum XpathVersion {
    UNSPECIFIED(null),
    XPATH_1_0("1.0"),
    XPATH_2_0("2.0"),
    ;

    private final String versionString;

    private XpathVersion(String versionString) {
        this.versionString = versionString;
    }

    /**
     * @return the version string, such as "1.0" or "2.0", or null if this is the UNSPECIFIED version.
     */
    public @Nullable String getVersionString() {
        return versionString;
    }

    /**
     * Get an XPath version for the specified version string, which may be null.
     *
     * @param versionString a version string such as "1.0" or "2.0", or null to return the UNSPECIFIED version.
     * @return an XpathVersion.  Never null.
     * @throws IllegalArgumentException if the version string is unrecognized
     */
    public static @NotNull XpathVersion fromVersionString(@Nullable String versionString) throws IllegalArgumentException {
        if (null == versionString)
            return UNSPECIFIED;
        else if ("1.0".equals(versionString))
            return XPATH_1_0;
        else if ("2.0".equals(versionString))
            return XPATH_2_0;
        throw new IllegalArgumentException("Unrecognized XPath version string: " + versionString);
    }
}
