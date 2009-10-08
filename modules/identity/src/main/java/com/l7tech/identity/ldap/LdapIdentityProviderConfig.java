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
import java.util.TreeSet;
import java.util.Set;
import java.util.Arrays;

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

    /**
     * Create a new LdapIdentityProviderConfig with default settings.
     *
     * @return The new configuration
     */
    public static LdapIdentityProviderConfig newLdapIdentityProviderConfig() {
        LdapIdentityProviderConfig config = new LdapIdentityProviderConfig();
        config.setReturningAttributes( new String[0] );
        return config;
    }

    @Override
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
        HashMap<String,GroupMappingConfig> grpMap = getProperty(GROUP_MAPPINGS);
        if (grpMap == null) {
            grpMap = new HashMap<String,GroupMappingConfig>();
            setProperty(GROUP_MAPPINGS, grpMap);
        }
        grpMap.put(cfg.getObjClass().toLowerCase(), cfg);
    }

    /**
     * overrides all group class mappings at once
     */
    public void setGroupMappings(GroupMappingConfig[] cfgs) {
        HashMap<String,GroupMappingConfig> grpMap = getProperty(GROUP_MAPPINGS);
        if (grpMap == null) {
            grpMap = new HashMap<String,GroupMappingConfig>();
            setProperty(GROUP_MAPPINGS, grpMap);
        } else {
            grpMap.clear();
        }
        for ( GroupMappingConfig cfg : cfgs ) {
            grpMap.put( cfg.getObjClass().toLowerCase(), cfg );
        }
    }

    /**
     * get the mapping for a specified user object class
     * returns null if there is no mapping declared for the passed object class
     */
    public UserMappingConfig getUserMapping(String objClass) {
        HashMap usrMap = (HashMap)getProperty(USER_MAPPINGS);
        if (usrMap == null) return null;
        return (UserMappingConfig)usrMap.get(objClass.toLowerCase());
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
        HashMap<String,UserMappingConfig> usrMap = getProperty(USER_MAPPINGS);
        if (usrMap == null) {
            usrMap = new HashMap<String,UserMappingConfig>();
            setProperty(USER_MAPPINGS, usrMap);
        }
        usrMap.put(cfg.getObjClass().toLowerCase(), cfg);
    }

    /**
     * overrides all user class mappings at once
     */
    public void setUserMappings(UserMappingConfig[] cfgs) {
        HashMap<String,UserMappingConfig> usrMap = getProperty(USER_MAPPINGS);
        if (usrMap == null) {
            usrMap = new HashMap<String,UserMappingConfig>();
            setProperty(USER_MAPPINGS, usrMap);
        } else {
            usrMap.clear();
        }
        for ( UserMappingConfig cfg : cfgs ) {
            usrMap.put( cfg.getObjClass().toLowerCase(), cfg );
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

    @Override
    @Transient
    protected String[] getUnexportablePropKeys() {
        return new String[]{BIND_PASS};
    }

    @Override
    @Transient
    public boolean canIssueCertificates() {
        return getUserCertificateUseType() == UserCertificateUseType.NONE;
    }

    /**
     * Get the search filter to use with custom certificate indexing.
     *
     * @return The search filter or null
     */
    @Transient
    public String getUserCertificateIndexSearchFilter() {
        return getProperty(USER_CERT_CUSTOM_INDEX);
    }

    /**
     *  Set the search filter to use with custom certificate indexing.
     *
     * @param searchFilter the search filter to use
     */
    public void setUserCertificateIndexSearchFilter( final String searchFilter ) {
        setProperty(USER_CERT_CUSTOM_INDEX, searchFilter);
    }

    /**
     * Get the search filter to use for certificate lookup by issuer name and serial number.
     *
     * @return The search filter or null
     */
    @Transient
    public String getUserCertificateIssuerSerialSearchFilter() {
        return getProperty(USER_CERT_SEARCH_ISSUER_SERIAL);
    }

    /**
     *  Set the search filter to use for certificate lookup by issuer name and serial number.
     *
     * @param searchFilter the search filter to use
     */
    public void setUserCertificateIssuerSerialSearchFilter( final String searchFilter ) {
        setProperty(USER_CERT_SEARCH_ISSUER_SERIAL, searchFilter);
    }

    /**
     * Get the search filter to use for certificate lookup by SKI.
     *
     * @return The search filter or null
     */
    @Transient
    public String getUserCertificateSKISearchFilter() {
        return getProperty(USER_CERT_SEARCH_SKI);
    }

    /**
     *  Set the search filter to use for certificate lookup by SKI.
     *
     * @param searchFilter the search filter to use
     */
    public void setUserCertificateSKISearchFilter( final String searchFilter ) {
        setProperty(USER_CERT_SEARCH_SKI, searchFilter);
    }

    /**
     * Get the usage for client certificates.
     *
     * @return the client certificate usage type (never null)
     */
    @Transient
    public UserCertificateUseType getUserCertificateUseType() {
        UserCertificateUseType type;

        String typeStr = getProperty(USER_CERT_USE_TYPE);
        if ( typeStr == null ) {
            Boolean b = (Boolean) getProperty(USERCERTS_ENABLED);
            type = (b != null && b) ? UserCertificateUseType.INDEX : UserCertificateUseType.NONE;
        } else {
            type = getEnumProperty(USER_CERT_USE_TYPE, UserCertificateUseType.NONE, UserCertificateUseType.class);
        }

        return type;
    }

    /**
     * Set the usage for client certificates.
     */
    public void setUserCertificateUseType( final UserCertificateUseType userCertificateUseType ) {
        setProperty(USER_CERT_USE_TYPE, userCertificateUseType.toString());
        setProperty(USERCERTS_ENABLED, null); // remove deprecated property
    }

    /**
     * Attributes to access from LDAP in addition to mapped / standard attributes.
     *
     * @return The attributes, or null for all attributes
     */
    @Transient
    public String[] getReturningAttributes() {
        String[] attributes = null;

        Object attributesObj = getProperty(RETURNING_ATTRIBUTES);
        if ( attributesObj instanceof String[] ) {
            attributes = (String[])attributesObj;
        }

        return attributes;
    }

    /**
     * Set the attributes to access from LDAP in addition to mapped / standard attributes.
     *
     * @param attributes The attributes to retrieve (null for all)
     */
    public void setReturningAttributes( final String[] attributes ) {
        if ( attributes == null ) {
            setProperty( RETURNING_ATTRIBUTES, null );
        } else {
            Set<String> attrSet = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
            attrSet.addAll( Arrays.asList(attributes) );
            setProperty( RETURNING_ATTRIBUTES, attrSet.toArray(new String[attrSet.size()]));
        }
    }

    public enum UserCertificateUseType { NONE, INDEX, INDEX_CUSTOM, SEARCH }

    public static final String URL = "ldapurl";
    public static final String SEARCH_BASE = "ldapsearchbase";
    private static final String GROUP_MAPPINGS = "grpmappings";
    private static final String USER_MAPPINGS = "usrmappings";
    private static final String BIND_DN = "ldapBindDN";
    private static final String BIND_PASS = "ldapBindPass";
    private static final String BASE_TEMPLATE = "originalTemplateName";
    private static final String CLIENT_AUTH_ENABLED = "clientAuth";
    private static final String KEYSTORE_ID = "keystoreId";
    private static final String KEY_ALIAS = "keyAlias";
    private static final String USERCERTS_ENABLED = "userCertsEnabled"; // deprecated
    private static final String USER_CERT_USE_TYPE = "userCertUseType";
    private static final String USER_CERT_CUSTOM_INDEX = "userCertIndex";
    private static final String USER_CERT_SEARCH_ISSUER_SERIAL = "userCertSearchIssuerSerial";
    private static final String USER_CERT_SEARCH_SKI = "userCertSearchSKI";
    private static final String RETURNING_ATTRIBUTES = "returningAttributes";    
}
