package com.l7tech.identity.ldap;

import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.IdentityProviderType;

import java.util.HashMap;
import java.util.Collection;
import java.util.Iterator;
import java.io.Serializable;
import java.io.IOException;

/**
 * General LDAP connector config.
 *
 * This can be used to describe a MSAD connector, an OID connector (any ldap implementation).
 *
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Jan 20, 2004<br/>
 * $Id$<br/>
 *
 */
public class LdapIdentityProviderConfig extends IdentityProviderConfig implements Serializable {

    public LdapIdentityProviderConfig(IdentityProviderConfig toto) throws IOException {
        super(IdentityProviderType.LDAP);
        this._version = toto.getVersion();
	    this._oid = toto.getOid();
        copyFrom(toto);
    }

    public LdapIdentityProviderConfig() {
        super(IdentityProviderType.LDAP);
    }

    public String getLdapUrl() {
        return (String)props.get(URL);
    }

    public void setLdapUrl(String ldapUrl) {
        props.put(URL, ldapUrl);
    }

    public String getSearchBase() {
        return (String)props.get(SEARCH_BASE);
    }

    public void setSearchBase(String searchBase) {
        props.put(SEARCH_BASE, searchBase);
    }

    public String getCustomGrpFilter() {
        return (String)props.get(CUSTOM_GROUP_SEARCH_FILTER);
    }

    public void setCustomGrpFilter(String filter) {
        props.put(CUSTOM_GROUP_SEARCH_FILTER, filter);
    }

    public void setCustomUsrFilter(String filter) {
        props.put(CUSTOM_USER_SEARCH_FILTER, filter);
    }

    public String getCustomUsrFilter() {
        return (String)props.get(CUSTOM_USER_SEARCH_FILTER);
    }

    public GroupMappingConfig getGroupMapping(String objClass) {
        HashMap grpMap = (HashMap)props.get(GROUP_MAPPINGS);
        if (grpMap == null) return null;
        GroupMappingConfig output = (GroupMappingConfig)grpMap.get(objClass.toLowerCase());
        return output;
    }

    public GroupMappingConfig[] getGroupMappings() {
        HashMap grpMap = (HashMap)props.get(GROUP_MAPPINGS);
        if (grpMap == null) return new GroupMappingConfig[0];
        Collection allmappings = grpMap.values();
        if (allmappings == null) return new GroupMappingConfig[0];
        GroupMappingConfig[] output = new GroupMappingConfig[allmappings.size()];
        int i = 0;
        for (Iterator it = allmappings.iterator(); it.hasNext(); i++) {
            output[i] = (GroupMappingConfig)it.next();
        }
        return output;
    }

    public void setGroupMapping(GroupMappingConfig cfg) {
        HashMap grpMap = (HashMap)props.get(GROUP_MAPPINGS);
        if (grpMap == null) {
            grpMap = new HashMap();
            props.put(GROUP_MAPPINGS, grpMap);
        }
        grpMap.put(cfg.getObjClass().toLowerCase(), cfg);
    }

    public UserMappingConfig getUserMapping(String objClass) {
        HashMap usrMap = (HashMap)props.get(USER_MAPPINGS);
        if (usrMap == null) return null;
        UserMappingConfig output = (UserMappingConfig)usrMap.get(objClass.toLowerCase());
        return output;
    }

    public UserMappingConfig[] getUserMappings() {
        HashMap usrMap = (HashMap)props.get(USER_MAPPINGS);
        if (usrMap == null) return new UserMappingConfig[0];
        Collection allmappings = usrMap.values();
        if (allmappings == null) return new UserMappingConfig[0];
        UserMappingConfig[] output = new UserMappingConfig[allmappings.size()];
        int i = 0;
        for (Iterator it = allmappings.iterator(); it.hasNext(); i++) {
            output[i] = (UserMappingConfig)it.next();
        }
        return output;
    }

    public void setUserMapping(UserMappingConfig cfg) {
        HashMap usrMap = (HashMap)props.get(USER_MAPPINGS);
        if (usrMap == null) {
            usrMap = new HashMap();
            props.put(USER_MAPPINGS, usrMap);
        }
        usrMap.put(cfg.getObjClass().toLowerCase(), cfg);
    }

    public String getBindDN() {
        return (String)props.get(BIND_DN);
    }

    public void setBindDN(String binddn) {
        props.put(BIND_DN, binddn);
    }

    public String getBindPasswd() {
        return (String)props.get(BIND_PASS);
    }

    public void setBindPasswd(String bindpasswd) {
        props.put(BIND_PASS, bindpasswd);
    }

    private static final String URL = "ldapurl";
    private static final String SEARCH_BASE = "ldapsearchbase";
    private static final String CUSTOM_GROUP_SEARCH_FILTER = "customgrpsearchfilter";
    private static final String CUSTOM_USER_SEARCH_FILTER = "customusersearchfilter";
    private static final String GROUP_MAPPINGS = "grpmappings";
    private static final String USER_MAPPINGS = "usrmappings";
    private static final String BIND_DN = "ldapBindDN";
    private static final String BIND_PASS = "ldapBindPass";
}
