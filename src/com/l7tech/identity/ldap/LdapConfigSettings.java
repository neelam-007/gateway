package com.l7tech.identity.ldap;

import com.l7tech.identity.IdentityProviderConfig;

/**
 * Settings names for an identity provider of type LDAP.
 *
 * <br/><br/>
 * Layer 7 Technologies, inc.<br/>
 * User: flascelles<br/>
 * Date: Jun 23, 2003
 */
public class LdapConfigSettings {
    public static final String LDAP_HOST_URL = "ldapHostURL";
    public static final String LDAP_SEARCH_BASE = "ldapSearchBase";
    public static final String LDAP_BIND_DN = "ldapBindDN";
    public static final String LDAP_BIND_PASS = "ldapBindPass";

    /**
     * checks the format of an ldap id provider config.
     * makes sure all the necessary elements are there
     */
    public static boolean isValidConfigObject(IdentityProviderConfig arg) {
        String hostUrl = arg.getProperty(LDAP_HOST_URL);
        String sbase = arg.getProperty(LDAP_SEARCH_BASE);
        if (hostUrl == null || sbase == null) return false;
        if (hostUrl.length() < 1 || sbase.length() < 1) return false;
        return true;
    }
}
