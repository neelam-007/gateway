package com.l7tech.identity.ldap;

import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.IdentityProviderType;
import com.l7tech.search.Dependency;
import com.l7tech.util.TimeUnit;
import org.hibernate.annotations.Proxy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Transient;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.*;

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
public class LdapIdentityProviderConfig extends LdapUrlBasedIdentityProviderConfig implements Serializable {
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
        config.setGroupCacheSize( 100 );
        config.setGroupMaxNesting( 5 );
        return config;
    }

    @Override
    @Transient
    public boolean isWritable() {
        return false;
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
    @Dependency(methodReturnType = Dependency.MethodReturnType.VARIABLE, type = Dependency.DependencyType.SECURE_PASSWORD)
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

    /**
     * Get the group cache size.
     *
     * @return The size or null if not set.
     */
    @Transient
    public Integer getGroupCacheSize() {
        return getProperty(GROUP_CACHE_SIZE);
    }

    /**
     * Set the group cache size to use.
     *
     * @param size The size to use (null for default size, 0 for no caching)
     */
    public void setGroupCacheSize( final Integer size ) {
        setProperty(GROUP_CACHE_SIZE, size);
    }

    /**
     * Get the maximum age in milliseconds for cached group information.
     *
     * @return The maximum age or null
     */
    @Transient
    public Long getGroupCacheMaxAge() {
        // convert from integer if necessary for backwards compatibility pre-5.4
        Object value = getProperty(GROUP_CACHE_MAX_AGE);
        if (value == null) return null;
        return value instanceof Integer ? ((Integer)value).longValue() :  (Long)value;
    }

    /**
     * Set the maximum age in milliseconds for cached group information.
     *
     * @param maxAge The maximum age (null for default, 0 for no caching)
     */
    public void setGroupCacheMaxAge( final Long maxAge ) {
        setProperty(GROUP_CACHE_MAX_AGE, maxAge);
    }


    @Transient
    public TimeUnit getGroupCacheMaxAgeUnit() {        
        Object value = getProperty(GROUP_CACHE_MAX_AGE_UNIT);
        if (value == null) return TimeUnit.MINUTES;
        return TimeUnit.fromAbbreviation((String)value);        
    }

    /**
     * Set the maximum age in milliseconds for cached group information.
     *
     * @param maxAgeUnit The maximum age unit
     */
    public void setGroupCacheMaxAgeUnit( final TimeUnit maxAgeUnit ) {
        setProperty(GROUP_CACHE_MAX_AGE_UNIT, maxAgeUnit.getAbbreviation());
    }

    /**
     * Get the group maximum nesting depth.
     *
     * <p>This limits the depth when checking of nested groups.</p>
     *
     * @return The maximum nesting or null if not set.
     */
    @Transient
    public Integer getGroupMaxNesting() {
        return getProperty(GROUP_MAX_NESTING);
    }

    /**
     * Set the group maximum nesting depth.
     *
     * @param depth The depth to use (null or 0 for no limit)
     */
    public void setGroupMaxNesting( final Integer depth ) {
        setProperty(GROUP_MAX_NESTING, depth);
    }

    /**
     * Get the case insensitivity flag for group membership checks.
     *
     * <p>The default value is false (checks are case sensitive), which is
     * appropriate for most LDAP servers.</p>
     *
     * @return True if membership tests should be case insensitive.
     */
    @Transient
    public boolean isGroupMembershipCaseInsensitive() {
        return getBooleanProperty(GROUP_MEMBERSHIP_CASE_INSENSITIVE, false);
    }

    /**
     * Set the case insensitivity flag for group membership checks.
     *
     * @param caseInsensitive True for case insensitive tests.
     */
    public void setGroupMembershipCaseInsensitive( final boolean caseInsensitive ) {
        setProperty(GROUP_MEMBERSHIP_CASE_INSENSITIVE, caseInsensitive);
    }

    @Transient
    public UserLookupByCertMode getUserLookupByCertMode() {
        return getEnumProperty(USER_LOOKUP_BY_CERT_MODE, UserLookupByCertMode.LOGIN, UserLookupByCertMode.class);
    }

    public void setUserLookupByCertMode(@Nullable UserLookupByCertMode mode) {
        setProperty(USER_LOOKUP_BY_CERT_MODE, mode == null ? UserLookupByCertMode.LOGIN.toString() : mode.toString());
    }

   @Transient
   @Dependency(methodReturnType = Dependency.MethodReturnType.OID, type = Dependency.DependencyType.SECURE_PASSWORD, key = "service.passwordOid")
   public Map<String, String> getNtlmAuthenticationProviderProperties() {
       TreeMap<String, String> props = getProperty(NTLM_AUTHENTICATION_PROVIDER_PROPERTIES);
       if(props != null){
           return props;
       }
       return Collections.emptyMap();
   }
    
    public void  setNtlmAuthenticationProviderProperties(@NotNull final Map<String, String> props) {
        setProperty(NTLM_AUTHENTICATION_PROVIDER_PROPERTIES, props);
    }

    public enum UserCertificateUseType {
        @XmlEnumValue("None") NONE,
        @XmlEnumValue("Index") INDEX,
        @XmlEnumValue("Custom Index") INDEX_CUSTOM,
        @XmlEnumValue("Search") SEARCH
    }

    public enum UserLookupByCertMode {
        @XmlEnumValue("Common Name from Certificate") LOGIN,
        @XmlEnumValue("Entire Certificate") CERT
    }

    public static final String SEARCH_BASE = "ldapsearchbase";
    private static final String GROUP_MAPPINGS = "grpmappings";
    private static final String USER_MAPPINGS = "usrmappings";
    private static final String BIND_DN = "ldapBindDN";
    private static final String BIND_PASS = "ldapBindPass";
    private static final String BASE_TEMPLATE = "originalTemplateName";
    private static final String USERCERTS_ENABLED = "userCertsEnabled"; // deprecated
    private static final String USER_CERT_USE_TYPE = "userCertUseType";
    private static final String USER_CERT_CUSTOM_INDEX = "userCertIndex";
    private static final String USER_CERT_SEARCH_ISSUER_SERIAL = "userCertSearchIssuerSerial";
    private static final String USER_CERT_SEARCH_SKI = "userCertSearchSKI";
    private static final String USER_LOOKUP_BY_CERT_MODE = "userLookupByCertMode";
    private static final String RETURNING_ATTRIBUTES = "returningAttributes";    
    private static final String GROUP_CACHE_SIZE = "groupCacheSize";
    private static final String GROUP_CACHE_MAX_AGE = "groupCacheMaxAge";
    private static final String GROUP_CACHE_MAX_AGE_UNIT = "groupCacheMaxAgeUnit";
    private static final String GROUP_MAX_NESTING = "groupMaxNesting";
    private static final String GROUP_MEMBERSHIP_CASE_INSENSITIVE = "groupMembershipCaseInsensitive";
    public static final String NTLM_AUTHENTICATION_PROVIDER_PROPERTIES = "ntlmProviderProperties";
}
