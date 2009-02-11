package com.l7tech.identity.ldap;

import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.IdentityProviderType;

import javax.xml.bind.annotation.XmlRootElement;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Transient;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

import org.hibernate.annotations.Proxy;

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
@XmlRootElement
@Entity
@Proxy(lazy=false)
@DiscriminatorValue("2")
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

    @Transient
    public boolean isWritable() {
        return false;
    }

    /**
     * the ldap url for connecting to the directory.
     */
    @Transient
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
    @Transient
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
    @Transient
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
    @Transient
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
        return (GroupMappingConfig)grpMap.get(objClass.toLowerCase());
    }

    /**
     * @return all existing group mappings
     */
    @Transient
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
    @Transient
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
    @Transient
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
    @Transient
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
     * @return  TRUE if using client authentication or if no client auth was found (backward compatiblity) otherwise FALSE.
     */
    @Transient
    public boolean isClientAuthEnabled() {
        Boolean b = (Boolean) getProperty(CLIENT_AUTH_ENABLED);
        return b == null || b;
    }

    public void setClientAuthEnabled(boolean clientAuthEnabled) {
        setProperty(CLIENT_AUTH_ENABLED, clientAuthEnabled);
    }

    /**
     * @return  Keystore Id used for client auth or NULL for default key.
     */
    @Transient
    public Long getKeystoreId() {
        return (Long) getProperty(KEYSTORE_ID);
    }

    public void setKeystoreId(Long keystoreId) {
        setProperty(KEYSTORE_ID, keystoreId);
    }

    /**
     * @return  Key alias used for client auth or NULL for default key.
     */
    @Transient
    public String getKeyAlias() {
        return (String) getProperty(KEY_ALIAS);
    }

    public void setKeyAlias(String keyAlias) {
        setProperty(KEY_ALIAS, keyAlias);
    }

    /**
     * set by the template manager. dont override this value.
     */
    @Transient
    public String getTemplateName() {
        return (String)getProperty(BASE_TEMPLATE);
    }

    /**
     * set by the template manager. dont override this value.
     */
    public void setTemplateName(String name) {
        setProperty(BASE_TEMPLATE, name);
    }

    public static final String URL = "ldapurl";
    public static final String SEARCH_BASE = "ldapsearchbase";
    private static final String CUSTOM_GROUP_SEARCH_FILTER = "customgrpsearchfilter";
    private static final String CUSTOM_USER_SEARCH_FILTER = "customusersearchfilter";
    private static final String GROUP_MAPPINGS = "grpmappings";
    private static final String USER_MAPPINGS = "usrmappings";
    private static final String BIND_DN = "ldapBindDN";
    private static final String BIND_PASS = "ldapBindPass";
    private static final String BASE_TEMPLATE = "originalTemplateName";
    private static final String CLIENT_AUTH_ENABLED = "clientAuth";
    private static final String KEYSTORE_ID = "keystoreId";
    private static final String KEY_ALIAS = "keyAlias";
}
