package com.l7tech.gateway.common.siteminder;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * All supported SiteMinder FIPS Modes.
 *
 * @author Jamie Williams - wilja33 - jamie.williams2@ca.com
 */
public enum SiteMinderFipsModeOption {

    UNSPECIFIED(0, "UNSPECIFIED"),  // Not a FIPS mode, but it is possible to register a host without specifying the mode
    COMPAT(1, "COMPAT"),        // FIPS compatibility mode
    MIGRATE(2, "MIGRATE"),      // FIPS migration mode
    ONLY(3, "ONLY");            // FIPS only
//    ONLY_MD5(4, "ONLY MD5");  // UNKNOWN: mentioned in SiteMinder code, but name and use unknown - unavailable until need determined

    private static final Map<String, SiteMinderFipsModeOption> nameLookup;
    private static final Map<Integer, SiteMinderFipsModeOption> codeLookup;

    // create reverse lookup maps
    static {
        SiteMinderFipsModeOption[] values = values();

        HashMap<String, SiteMinderFipsModeOption> nameMap = new HashMap<>(values.length);
        HashMap<Integer, SiteMinderFipsModeOption> codeMap = new HashMap<>(values.length);

        for (SiteMinderFipsModeOption mode : values) {
            nameMap.put(mode.getName(), mode);
            codeMap.put(mode.getCode(), mode);
        }

        nameLookup = Collections.unmodifiableMap(nameMap);
        codeLookup = Collections.unmodifiableMap(codeMap);
    }

    private final int code;
    private final String name;

    private SiteMinderFipsModeOption(int code, String name) {
        this.code = code;
        this.name = name;
    }

    public int getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    /**
     * Lookup SiteMinderFipsMode enum by its code value.
     *
     * @param name the name to lookup the mode by
     * @return the SiteMinderFipsMode instance with the specified name, or null iff no instance with that name exists
     */
    public static SiteMinderFipsModeOption getByName(@NotNull String name) {
        return nameLookup.get(name);
    }

    /**
     * Lookup SiteMinderFipsMode enum by its name value.
     *
     * @param code the code to lookup the mode by
     * @return the SiteMinderFipsMode instance with the specified code, or null iff no instance with that code exists
     */
    public static SiteMinderFipsModeOption getByCode(int code) {
        return codeLookup.get(code);
    }

    @Override
    public String toString() {
        return name;
    }
}
