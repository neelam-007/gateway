package com.l7tech.identity.ldap;

import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.IdentityProviderType;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

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

    public LdapIdentityProviderConfig(IdentityProviderConfig toto) {
        super(IdentityProviderType.LDAP);
        this._version = toto.getVersion();
	    this._oid = toto.getOid();
        copyFrom(toto);
    }

    public LdapIdentityProviderConfig() {
        super(IdentityProviderType.LDAP);
    }

    public boolean isWritable() {
        return false;
    }

    /**
     * the ldap url for connecting to the directory.
     */
    public String[] getLdapUrl() {
        Object prop = getProperty(URL);
        // Backward compatibility
        if (prop instanceof String) {
            return new String[]{(String)prop};
        } else {
            return (String[])prop;
        }
    }

    /**
     * the ldap url for connecting to the directory.
     */
    public void setLdapUrl(String[] ldapUrl) {
        setProperty(URL, ldapUrl);
    }

    /**
     * the search base for users and groups
     */
    public String getSearchBase() {
        return (String)getProperty(SEARCH_BASE);
    }

    /**
     * the search base for users and groups
     */
    public void setSearchBase(String searchBase) {
        setProperty(SEARCH_BASE, searchBase);
    }

    /**
     * overrides the search filter for group objects
     * currently unused
     */
    public String getCustomGrpFilter() {
        return (String)getProperty(CUSTOM_GROUP_SEARCH_FILTER);
    }

    /**
     * overrides the search filter for group objects
     * currently unused
     */
    public void setCustomGrpFilter(String filter) {
        setProperty(CUSTOM_GROUP_SEARCH_FILTER, filter);
    }

    /**
     * overrides the search filter for user objects
     * currently unused
     */
    public void setCustomUsrFilter(String filter) {
        setProperty(CUSTOM_USER_SEARCH_FILTER, filter);
    }

    /**
     * overrides the search filter for user objects
     * currently unused
     */
    public String getCustomUsrFilter() {
        return (String)getProperty(CUSTOM_USER_SEARCH_FILTER);
    }

    /**
     * get the mapping for a specified group object class
     * returns null if there is no mapping declared for the passed object class
     */
    public GroupMappingConfig getGroupMapping(String objClass) {
        HashMap grpMap = (HashMap)getProperty(GROUP_MAPPINGS);
        if (grpMap == null) return null;
        GroupMappingConfig output = (GroupMappingConfig)grpMap.get(objClass.toLowerCase());
        return output;
    }

    /**
     * @return all existing group mappings
     */
    public GroupMappingConfig[] getGroupMappings() {
        HashMap grpMap = (HashMap)getProperty(GROUP_MAPPINGS);
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

    /**
     * add or overrides the mapping for a specific group object class
     */
    public void setGroupMapping(GroupMappingConfig cfg) {
        HashMap grpMap = (HashMap)getProperty(GROUP_MAPPINGS);
        if (grpMap == null) {
            grpMap = new HashMap();
            setProperty(GROUP_MAPPINGS, grpMap);
        }
        grpMap.put(cfg.getObjClass().toLowerCase(), cfg);
    }

    /**
     * overrides all group class mappings at once
     */
    public void setGroupMappings(GroupMappingConfig[] cfgs) {
        HashMap grpMap = (HashMap)getProperty(GROUP_MAPPINGS);
        if (grpMap == null) {
            grpMap = new HashMap();
            setProperty(GROUP_MAPPINGS, grpMap);
        } else {
            grpMap.clear();
        }
        for (int i = 0; i < cfgs.length; i++) {
            grpMap.put(cfgs[i].getObjClass().toLowerCase(), cfgs[i]);
        }
    }

    /**
     * get the mapping for a specified user object class
     * returns null if there is no mapping declared for the passed object class
     */
    public UserMappingConfig getUserMapping(String objClass) {
        HashMap usrMap = (HashMap)getProperty(USER_MAPPINGS);
        if (usrMap == null) return null;
        UserMappingConfig output = (UserMappingConfig)usrMap.get(objClass.toLowerCase());
        return output;
    }

    /**
     * @return all existing user mappings
     */
    public UserMappingConfig[] getUserMappings() {
        HashMap usrMap = (HashMap)getProperty(USER_MAPPINGS);
        if (usrMap == null) return new UserMappingConfig[0];
        Collection allmappings = usrMap.values();
        if (allmappings == null) return new UserMappingConfig[0];
        UserMappingConfig[] output = new UserMappingConfig[allmappings.size()];
        int i = 0;
        for (Iterator it = allmappings.iterator(); it.hasNext(); i++) {
            Object obj = it.next();
            output[i] = (UserMappingConfig)obj;
        }
        return output;
    }

    /**
     * add or overrides the mapping for a specific user object class
     */
    public void setUserMapping(UserMappingConfig cfg) {
        HashMap usrMap = (HashMap)getProperty(USER_MAPPINGS);
        if (usrMap == null) {
            usrMap = new HashMap();
            setProperty(USER_MAPPINGS, usrMap);
        }
        usrMap.put(cfg.getObjClass().toLowerCase(), cfg);
    }

    /**
     * overrides all user class mappings at once
     */
    public void setUserMappings(UserMappingConfig[] cfgs) {
        HashMap usrMap = (HashMap)getProperty(USER_MAPPINGS);
        if (usrMap == null) {
            usrMap = new HashMap();
            setProperty(USER_MAPPINGS, usrMap);
        } else {
            usrMap.clear();
        }
        for (int i = 0; i < cfgs.length; i++) {
            usrMap.put(cfgs[i].getObjClass().toLowerCase(), cfgs[i]);
        }
    }

    /**
     * the bind dn for searching
     */
    public String getBindDN() {
        return (String)getProperty(BIND_DN);
    }

    /**
     * the bind dn for searching
     */
    public void setBindDN(String binddn) {
        setProperty(BIND_DN, binddn);
    }

    /**
     * the bind passwd for searching
     */
    public String getBindPasswd() {
        return (String)getProperty(BIND_PASS);
    }

    /**
     * the bind passwd for searching
     */
    public void setBindPasswd(String bindpasswd) {
        setProperty(BIND_PASS, bindpasswd);
    }

    /**
     * set by the template manager. dont override this value.
     */
    public String getTemplateName() {
        return (String)getProperty(BASE_TEMPLATE);
    }

    /**
     * set by the template manager. dont override this value.
     */
    public void setTemplateName(String name) {
        setProperty(BASE_TEMPLATE, name);
    }

    private static final String URL = "ldapurl";
    private static final String SEARCH_BASE = "ldapsearchbase";
    private static final String CUSTOM_GROUP_SEARCH_FILTER = "customgrpsearchfilter";
    private static final String CUSTOM_USER_SEARCH_FILTER = "customusersearchfilter";
    private static final String GROUP_MAPPINGS = "grpmappings";
    private static final String USER_MAPPINGS = "usrmappings";
    private static final String BIND_DN = "ldapBindDN";
    private static final String BIND_PASS = "ldapBindPass";
    private static final String BASE_TEMPLATE = "originalTemplateName";
}
