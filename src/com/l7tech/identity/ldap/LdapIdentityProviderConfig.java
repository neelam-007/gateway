package com.l7tech.identity.ldap;

import com.l7tech.objectmodel.imp.NamedEntityImp;
import com.l7tech.identity.IdentityProviderConfig;

/**
 * Layer 7 Technologies, inc.
 * User: flascelles
 * Date: Jun 13, 2003
 *
 * Holds the configuration of a ldap id provider.
 */
public class LdapIdentityProviderConfig extends NamedEntityImp implements IdentityProviderConfig {

    public LdapIdentityProviderConfig() {
        desc = "";
    }

    public String getDescription() {
        return desc;
    }

    public void setDescription(String description) {
        desc = description;
    }

    /**
     * The ldap host and port where the directory is located.
     * For example "ldap://authenticationDirectory.acme.com:389/"
     */
    public String getLdapHostURL() {
        return ldapHostURL;
    }

    /**
     * The ldap host and port where the directory is located.
     * For example "ldap://authenticationDirectory.acme.com:389/"
     */
    public void setLdapHostURL(String ldapHostURL) {
        this.ldapHostURL = ldapHostURL;
    }

    /**
     * Optional parameter.
     * The administrator can use this to restrict the use of the directory
     * The search base for the user entries in the ldap directory.
     * For example "dc=yourcompany,dc=com" or "o=yourorganization"
     */
    public String getSearchBase() {
        return searchBase;
    }

    /**
     * Optional parameter.
     * The administrator can use this to restrict the use of the directory
     * The search base for the user entries in the ldap directory.
     * For example "dc=yourcompany,dc=com" or "o=yourorganization"
     */
    public void setSearchBase(String searchBase) {
        this.searchBase = searchBase;
    }

    // ************************************************
    // PRIVATES
    // ************************************************

    private String desc;
    private String ldapHostURL;
    private String searchBase;
}
