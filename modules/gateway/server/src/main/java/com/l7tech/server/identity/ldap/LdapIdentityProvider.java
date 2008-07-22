/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.server.identity.ldap;

import com.l7tech.identity.ldap.LdapUser;
import com.l7tech.identity.ldap.LdapGroup;
import com.l7tech.objectmodel.IdentityHeader;
import com.l7tech.server.identity.AuthenticatingIdentityProvider;

import javax.naming.directory.DirContext;
import javax.naming.directory.SearchResult;
import javax.naming.directory.Attributes;
import javax.naming.NamingException;

/**
 * @author alex
 */
public interface LdapIdentityProvider extends AuthenticatingIdentityProvider<LdapUser, LdapGroup, LdapUserManager, LdapGroupManager> {
    /**
     * LDAP connection attempts will fail after 5 seconds' wait
     */
    static final int LDAP_CONNECT_TIMEOUT = 5 * 1000;
    /**
     * LDAP reads will fail after 30 seconds' wait
     */
    static final int LDAP_READ_TIMEOUT = 30 * 1000;
    /**
     * An unused LDAP connection will be closed after 30 seconds of inactivity
     */
    static final int LDAP_POOL_IDLE_TIMEOUT = 30 * 1000;
    String DESCRIPTION_ATTRIBUTE_NAME = "description";

    String userSearchFilterWithParam(String param);

    String groupSearchFilterWithParam(String param);

    DirContext getBrowseContext() throws NamingException;

    long getMaxSearchResultSize();

    IdentityHeader searchResultToHeader(SearchResult sr);

    boolean isValidEntryBasedOnUserAccountControlAttribute(String userDn, Attributes attributes);

    boolean checkExpiredMSADAccount(String userDn, Attributes attributes);

    String getLastWorkingLdapUrl();

    String markCurrentUrlFailureAndGetFirstAvailableOne(String ldapurl);
}
