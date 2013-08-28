package com.l7tech.gateway.common.siteminder;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * All supported SiteMinder FIPS Modes.
 *
 * @author Jamie Williams - wilja33 - jamie.williams2@ca.com
 */
public enum SiteMinderFipsMode {

    UNSET(0, "UNSET"),
    COMPAT(1, "COMPAT"),
    MIGRATE(2, "MIGRATE"),
    ONLY(3, "ONLY");
//    ONLY_MD5(4, "ONLY MD5");  // unavailable until need determined - also need to find the proper name

    private static final Map<String, SiteMinderFipsMode> nameLookup;
    private static final Map<Integer, SiteMinderFipsMode> codeLookup;

    // create reverse lookup maps
    static {
        SiteMinderFipsMode[] values = values();

        HashMap<String, SiteMinderFipsMode> nameMap = new HashMap<>(values.length);
        HashMap<Integer, SiteMinderFipsMode> codeMap = new HashMap<>(values.length);

        for (SiteMinderFipsMode mode : values) {
            nameMap.put(mode.getName(), mode);
            codeMap.put(mode.getCode(), mode);
        }

        nameLookup = Collections.unmodifiableMap(nameMap);
        codeLookup = Collections.unmodifiableMap(codeMap);
    }

    private final int code;
    private final String name;

    private SiteMinderFipsMode(int code, String name) {
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
    public static SiteMinderFipsMode getByName(String name) {
        return nameLookup.get(name);
    }

    /**
     * Lookup SiteMinderFipsMode enum by its name value.
     *
     * @param code the code to lookup the mode by
     * @return the SiteMinderFipsMode instance with the specified code, or null iff no instance with that code exists
     */
    public static SiteMinderFipsMode getByCode(int code) {
        return codeLookup.get(code);
    }

    @Override
    public String toString() {
        return name;
    }
}
