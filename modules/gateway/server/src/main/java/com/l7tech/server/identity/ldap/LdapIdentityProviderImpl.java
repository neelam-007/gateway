package com.l7tech.server.identity.ldap;

import com.l7tech.common.io.CertUtils;
import com.l7tech.gateway.common.audit.SystemMessages;
import com.l7tech.identity.AuthenticationException;
import com.l7tech.identity.*;
import com.l7tech.identity.cert.ClientCertManager;
import com.l7tech.identity.ldap.*;
import com.l7tech.kerberos.KerberosServiceTicket;
import com.l7tech.objectmodel.*;
import com.l7tech.policy.assertion.credential.CredentialFormat;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.variable.VariableNameSyntaxException;
import com.l7tech.security.token.NtlmToken;
import com.l7tech.server.Lifecycle;
import com.l7tech.server.LifecycleException;
import com.l7tech.server.ServerConfigParams;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.identity.AuthenticationResult;
import com.l7tech.server.identity.ConfigurableIdentityProvider;
import com.l7tech.server.identity.DigestAuthenticator;
import com.l7tech.server.identity.SessionAuthenticator;
import com.l7tech.server.identity.cert.CertificateAuthenticator;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.server.policy.variable.ServerVariables;
import com.l7tech.util.*;
import com.sun.jndi.ldap.LdapURL;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import javax.naming.*;
import javax.naming.directory.*;
import javax.security.auth.x500.X500Principal;
import java.math.BigInteger;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Server-side implementation of the LDAP provider.
 * <p/>
 * This handles any type of directory thgough a LdapIdentityProviderConfig
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Jan 21, 2004<br/>
 */
@LdapClassLoaderRequired
public class LdapIdentityProviderImpl
        implements LdapIdentityProvider, InitializingBean, ApplicationContextAware, ConfigurableIdentityProvider, Lifecycle
{

    public LdapIdentityProviderImpl() {
    }

    @Override
    public void setIdentityProviderConfig( final IdentityProviderConfig configuration ) throws InvalidIdProviderCfgException {
        if (this.config != null) {
            throw new InvalidIdProviderCfgException("Provider is already configured");
        }
        this.config = (LdapIdentityProviderConfig) configuration;
        if (this.config.getLdapUrl() == null || this.config.getLdapUrl().length < 1) {
            throw new InvalidIdProviderCfgException("This config does not contain an ldap url"); // should not happen
        }

        if ( userManager == null ) {
            throw new InvalidIdProviderCfgException("UserManager is not set");
        }
        if ( groupManager == null ) {
            throw new InvalidIdProviderCfgException("GroupManager is not set");
        }

        if ( this.config.getReturningAttributes() != null ) {
            returningAttributes = buildReturningAttributes();
        }

        userManager.configure( this );
        groupManager.configure( this );

        sessionAuthenticator = new SessionAuthenticator( config.getOid() );

        ldapTemplate = new LdapUtils.LdapTemplate(config.getSearchBase(), returningAttributes){
            @Override
            DirContext getDirContext() throws NamingException {
                return getBrowseContext();
            }
        };

        urlProvider = new LdapUrlProviderImpl(config.getLdapUrl(), ldapRuntimeConfig);

        String description = config.getName() + "(#" + config.getOid() + "," + config.getVersion() + ")";
        ldapCertificateCache = new LdapCertificateCache(
                config,
                configManager, 
                ldapRuntimeConfig,
                ldapTemplate,
                getDistinctCertificateAttributeNames(),
                description,
                certIndexEnabled());
    }

    private Set<String> getDistinctCertificateAttributeNames() {
        final Set<String> distinctCertAttributeNames = new HashSet<String>();
        final UserMappingConfig[] mappings = config.getUserMappings();
        for (UserMappingConfig mapping : mappings) {
            String certAttributeName = mapping.getUserCertAttrName();
            if (certAttributeName == null || certAttributeName.trim().length() < 1) {
                logger.fine("User mapping for class " + mapping.getObjClass() +
                            " contained empty cert attribute name. This mapping will be " +
                            "ignored as part of the indexing process.");
            } else {
                distinctCertAttributeNames.add( certAttributeName.trim() );
            }
        }
        return distinctCertAttributeNames;
    }

    private boolean certsAreEnabled() {
        return config.getUserCertificateUseType() != LdapIdentityProviderConfig.UserCertificateUseType.NONE;
    }

    private boolean certIndexEnabled() {
        return config.getUserCertificateUseType() == LdapIdentityProviderConfig.UserCertificateUseType.INDEX ||
               config.getUserCertificateUseType() == LdapIdentityProviderConfig.UserCertificateUseType.INDEX_CUSTOM;
    }

    public void setLdapRuntimeConfig( final LdapRuntimeConfig ldapRuntimeConfig ) {
        this.ldapRuntimeConfig = ldapRuntimeConfig;
    }

    public void setCertificateAuthenticator( final CertificateAuthenticator certificateAuthenticator ) {
        this.certificateAuthenticator = certificateAuthenticator;
    }

    @Override
    public LdapIdentityProviderConfig getConfig() {
        return config;
    }

    @Override
    public LdapUserManager getUserManager() {
        return userManager;
    }

    @Override
    public LdapGroupManager getGroupManager() {
        return groupManager;
    }

    @Override
    public AuthenticationResult authenticate(final LoginCredentials pc) throws AuthenticationException {
        LdapUser realUser;
        try {
            realUser = findUserByCredential( pc );
        } catch (FindException e) {
            if (pc.getFormat() == CredentialFormat.KERBEROSTICKET) {
                KerberosServiceTicket ticket = (KerberosServiceTicket) pc.getPayload();
                throw new AuthenticationException("Couldn't find LDAP user for kerberos principal '" + ticket.getClientPrincipalName() + "'.", e);
            } else {
                throw new AuthenticationException("Couldn't authenticate credentials", e);
            }
        }

        if (realUser == null) return null;

        final CredentialFormat format = pc.getFormat();
        if (format == CredentialFormat.CLEARTEXT) {
            return authenticatePasswordCredentials(pc, realUser);
        } else if (format == CredentialFormat.DIGEST) {
            return DigestAuthenticator.authenticateDigestCredentials(pc, realUser);
        } else if (format  == CredentialFormat.KERBEROSTICKET) {
            return new AuthenticationResult( realUser, pc.getSecurityTokens() );
        } else if (format == CredentialFormat.NTLMTOKEN) {
            return new AuthenticationResult( realUser, pc.getSecurityToken());
        } else if (format == CredentialFormat.SESSIONTOKEN) {
            return sessionAuthenticator.authenticateSessionCredentials( pc, realUser );
        } else {
            if (format == CredentialFormat.CLIENTCERT || format == CredentialFormat.SAML) {

                    //get the LDAP cert for this user if LDAP certs are enabled for this provider
                    if (certsAreEnabled() && realUser.getLdapCertBytes() != null) {
                        try {
                            return certificateAuthenticator.authenticateX509Credentials(pc,
                                                                             realUser.getCertificate(),
                                                                             realUser,
                                                                             config.getCertificateValidationType(),
                                                                             auditor, false);
                        } catch (CertificateException e) {
                            throw new AuthenticationException("Problem decoding cert located in LDAP: " + ExceptionUtils.getMessage(e), e);
                        }
                    } else {
                        return certificateAuthenticator.authenticateX509Credentials(pc, realUser, config.getCertificateValidationType(), auditor);
                    }
            } else {
                String msg = "Attempt to authenticate using unsupported method on this provider: " + pc.getFormat();
                logger.log(Level.SEVERE, msg);
                throw new AuthenticationException(msg);
            }
        }
    }

    @Override
    public AuthenticationResult authenticate(LoginCredentials pc, boolean allowUserUpgrade) throws AuthenticationException {
        return authenticate(pc);
    }

    /**
     * This provider does not support lookup of users by credential.
     */
    @Override
    public LdapUser findUserByCredential( LoginCredentials pc ) throws FindException {
        LdapUser user = null;

        if (pc.getFormat() == CredentialFormat.KERBEROSTICKET) {
            KerberosServiceTicket ticket = (KerberosServiceTicket) pc.getPayload();
            user = findLdapUser(search(ticket), "Found multiple LDAP users for kerberos principal '" + ticket.getClientPrincipalName() + "'.");
            
        } else if (pc.getFormat() == CredentialFormat.SESSIONTOKEN) {
            final String id = sessionAuthenticator.getUserId( pc );
            if ( id!=null ) {
                user = userManager.findByPrimaryKey( id );
            }
        } else if (pc.getFormat() == CredentialFormat.CLIENTCERT && certsAreEnabled() && LdapIdentityProviderConfig.UserLookupByCertMode.CERT.equals(config.getUserLookupByCertMode())) {
            try {
                String userDn = ldapCertificateCache.findUserDnByCert(pc.getClientCert());
                user = userDn == null ? null : userManager.findByPrimaryKey(userDn);
            } catch (CertificateEncodingException e) {
                throw new FindException("Invalid user certificate: " + ExceptionUtils.getMessage(e), e);
            }
        } else if (pc.getFormat() == CredentialFormat.NTLMTOKEN) {
            NtlmToken token = (NtlmToken)pc.getSecurityToken();
            user = findLdapUser(search(token), "Found multiple LDAP users for one NTLM token'" + token.getLogin() + "'.");
        } else if ( pc.getLogin() != null ) {
            user = userManager.findByLogin(pc.getLogin());
        }

        return user;
    }

    private LdapUser findLdapUser(Collection<IdentityHeader> headers, String errmsg) throws FindException {
        LdapUser user = null;
        
        if(headers != null) {
            if (headers.size() > 1) {
                throw new FindException(errmsg);
            }
            else if (!headers.isEmpty()){
                for (IdentityHeader header : headers) {
                    if (header.getType() == EntityType.USER) {
                        user = userManager.findByPrimaryKey(header.getStrId());
                    }
                }
            }
        }
        return user;
    }


    private AuthenticationResult authenticatePasswordCredentials(LoginCredentials pc, LdapUser realUser) throws BadCredentialsException {
        // basic authentication
        boolean res = userManager.authenticateBasic(realUser.getDn(), new String(pc.getCredentials()));
        if (res) {
            // success
            return new AuthenticationResult(realUser, pc.getSecurityTokens());
        }
        logger.info("credentials did not authenticate for " + pc.getLogin());
        throw new BadCredentialsException("credentials did not authenticate");
    }

    @Override
    public Collection<String> getReturningAttributes() {
        Collection<String> attributes = null;

        if ( returningAttributes != null ) {
            attributes = Collections.unmodifiableCollection( Arrays.asList( returningAttributes ) );           
        }

        return attributes;
    }

    /**
     * searches the ldap provider for identities
     *
     * @param types        any combination of EntityType.USER and or EntityType.GROUP
     * @param searchString the search string for the users and group names, use "*" for all
     * @return a collection containing EntityHeader objects
     */
    @Override
    public EntityHeaderSet<IdentityHeader> search(EntityType[] types, String searchString) throws FindException {
        if (types == null || types.length < 1) {
            throw new IllegalArgumentException("must pass at least one type");
        }

        boolean wantUsers = false;
        boolean wantGroups = false;

        for (EntityType type : types) {
            if (type == EntityType.USER)
                wantUsers = true;
            else if (type == EntityType.GROUP) wantGroups = true;
        }
        if (!wantUsers && !wantGroups) {
            throw new IllegalArgumentException("types must contain users and or groups");
        }

        String userFilter = userSearchFilterWithParam(searchString);
        String grpFilter = groupSearchFilterWithParam(searchString);

        String filter;
        if (wantUsers && wantGroups) {
            // no group mapping is now allowed
            if (grpFilter == null) {
                filter = userFilter;
            } else
                filter = "(|" + userFilter + grpFilter + ")";
        } else if (wantUsers) {
            filter = userFilter;
        } else {
            filter = grpFilter;
        }

        // no group mapping is now allowed
        if (filter == null) return EntityHeaderSet.empty();

        return doSearch(filter);
    }

    /**
     * Find the LDAP users that correspond to the given ticket
     */
    private Collection<IdentityHeader> search( final KerberosServiceTicket ticket ) throws FindException {
        String principal = ticket.getClientPrincipalName();

        int index1 = principal.indexOf( '@' );
        int index2 = principal.lastIndexOf( '@' );
        boolean isEnterprise = index1 != index2;
        String value = principal;
        if ( index2 != -1 ) {
            value = principal.substring( 0, index2 );
        }

        if ( value.length() == 0 ) {
            throw new FindException( "Error processing kerberos principal name '"+principal+"', cannot determine REALM." );
        }

        if ( logger.isLoggable( Level.FINEST ) ) {
            logger.log( Level.FINEST, "Performing LDAP search by kerberos principal ''{1}'' (enterprise:{0})", new Object[]{isEnterprise, principal} );
        }

        ArrayList<LdapSearchTerm> terms = new ArrayList<LdapSearchTerm>();
        for (int i = 0; i < config.getUserMappings().length; i++) {
            UserMappingConfig userMappingConfig = config.getUserMappings()[i];

            String mappingName = null;
            if ( isEnterprise ) {
                mappingName = userMappingConfig.getKerberosEnterpriseAttrName();
            }

            if ( mappingName == null ) {
                mappingName = userMappingConfig.getKerberosAttrName();

                if ( mappingName == null ) {
                    mappingName = userMappingConfig.getLoginAttrName();
                }
            }

            terms.add( new LdapSearchTerm( userMappingConfig.getObjClass(), mappingName, value ) );
        }

        String filter = makeSearchFilter(terms.toArray(new LdapSearchTerm[terms.size()]), true);

        return doSearch(filter);
    }

    private Collection<IdentityHeader> search(final NtlmToken token) throws FindException {
        //get search by objectSid in LDAP
        String filter = "(objectSid=" + token.getUserSid() + ")";
        return doSearch(filter);
    }

    @Override
    public X509Certificate findCertBySki( final String ski ) {
        X509Certificate lookedupCert = null;

        if ( certsAreEnabled() ) {
            // look for presence of cert in index
            lookedupCert = ldapCertificateCache.findCertBySki( ski );

            if ( lookedupCert == null ) {
                if ( !certIndexEnabled() ) { // then search for cert and add to cache if found
                    lookedupCert = getCertificateBySki( ski );
                }
            }
        }

        return lookedupCert;
    }

    @Override
    public X509Certificate findCertByIssuerAndSerial( final X500Principal issuerDN, final BigInteger certSerial ) {
        X509Certificate lookedupCert = null;

        if ( certsAreEnabled() ) {
            lookedupCert = ldapCertificateCache.findCertByIssuerAndSerial( issuerDN, certSerial );

            if ( lookedupCert == null ) {
                if ( !certIndexEnabled() ) { // then search for cert and add to cache if found
                    lookedupCert = getCertificateByIssuerAndSerial( issuerDN, certSerial );
                }
            }
        }
        
        return lookedupCert;
    }

    @Override
    public X509Certificate findCertByThumbprintSHA1( final String thumbprintSHA1 ) throws FindException {
        X509Certificate lookedupCert = null;

        if ( certsAreEnabled() ) {
            lookedupCert = ldapCertificateCache.findCertByThumbprintSHA1( thumbprintSHA1 );
        }

        return lookedupCert;
    }

    @Override
    public X509Certificate findCertBySubjectDn( final X500Principal subjectDn ) throws FindException {
        X509Certificate lookedupCert = null;

        if ( certsAreEnabled() ) {
            lookedupCert = ldapCertificateCache.findCertBySubjectDn( subjectDn );
        }

        return lookedupCert;
    }

    private X509Certificate getCertificateBySki( final String ski ) {
        X509Certificate certificate = null;

        try {
            String filter = config.getUserCertificateSKISearchFilter();
            if ( filter != null && !filter.isEmpty() ) {
                certificate = getCertificateWithFilter( filter, null, null, ski );
            }
        } catch (FindException fe) {
            logger.log(Level.WARNING, "Error looking up certificate by SKI in directory.", fe);
        }

        return certificate;
    }

    private X509Certificate getCertificateByIssuerAndSerial( final X500Principal issuerDN, final BigInteger serial ) {
        X509Certificate certificate = null;

        try {
            String filter = config.getUserCertificateIssuerSerialSearchFilter();
            if ( filter != null && !filter.isEmpty() ) {
                certificate = getCertificateWithFilter( filter, issuerDN, serial, null );
            }
        } catch (FindException fe) {
            logger.log(Level.WARNING, "Error looking up certificate by issuer/serial in directory.", fe);
        }

        return certificate;
    }

    /**
     * Get certificate from LDAP using the given filter and add to indexes and cache.
     */
    private X509Certificate getCertificateWithFilter( final String filterTemplate,
                                                      final X500Principal issuer,
                                                      final BigInteger serial,
                                                      final String ski ) throws FindException {
        X509Certificate certificate = null;

        final String filter = formatCertificateSearchFilter( filterTemplate, issuer, serial, ski );

        try {
            final Set<String> distinctCertAttributeNames = getDistinctCertificateAttributeNames();
            final String[] dnHolder = new String[1];
            final List<X509Certificate> certs = new ArrayList<X509Certificate>();

            ldapTemplate.search( filter, 1, distinctCertAttributeNames, new LdapUtils.LdapListener(){
                @Override
                boolean searchResult( final SearchResult sr ) throws NamingException {
                    final Attributes attributes = sr.getAttributes();
                    dnHolder[0] = sr.getNameInNamespace();

                    for ( String certAttributeName : distinctCertAttributeNames ) {
                        final Object certificateObj = LdapUtils.extractOneAttributeValue(attributes, certAttributeName);
                        if (certificateObj instanceof byte[]) {
                            try {
                                certs.add( CertUtils.decodeCert((byte[])certificateObj) );
                            } catch ( CertificateException ce ) {
                                logger.log(Level.WARNING,
                                           "Error processing certificate for dn '"+dnHolder[0]+"', '"+ExceptionUtils.getMessage(ce)+"'.",
                                           ExceptionUtils.getDebugException(ce));
                            }
                        }
                    }

                    return false;
                }
            } );

            if ( !certs.isEmpty() ) {
                for ( X509Certificate cert : certs ) {
                    // Check if this the target certificate
                    if ( issuer != null && serial != null ) {
                        if ( issuer.equals(cert.getIssuerX500Principal()) && serial.equals(cert.getSerialNumber())) {
                            certificate = cert;
                        }
                    } else if ( ski != null ) {
                        if ( ski.equals( CertUtils.getSki(cert))) {
                            certificate = cert;
                        }
                    }

                    if ( certificate != null ) break;
                }

                X509Certificate[] certificates = certs.toArray( new X509Certificate[certs.size()] );
                ldapCertificateCache.cacheAndIndexCertificates( dnHolder[0], certificates );
            }
        } catch (NamingException e) {
            throw new FindException("LDAP search error with filter " + filter, e);
        } catch (CertificateException e) {
            throw new FindException("LDAP search error with filter " + filter, e);
        }

        if ( certificate == null ) {
            ldapCertificateCache.clearIndex( issuer, serial, ski );
        }

        return certificate;
    }

    private EntityHeaderSet<IdentityHeader> doSearch( final String filter ) throws FindException {
        final EntityHeaderSet<IdentityHeader> output = new EntityHeaderSet<IdentityHeader>(new TreeSet<IdentityHeader>());
        try {
            ldapTemplate.search( filter, ldapRuntimeConfig.getMaxSearchResultSize(), null, new LdapUtils.LdapListener(){
                @Override
                boolean searchResult( final SearchResult sr ) {
                    IdentityHeader header = searchResultToHeader(sr);
                    // if we successfully constructed a header, add it to result list
                    if (header != null)
                        output.add(header);
                    else
                        logger.info("entry not valid or objectclass not supported for dn=" + sr.getNameInNamespace() + ". this " +
                          "entry will not be presented as part of the search results.");

                    return true;
                }
            } );
        } catch (SizeLimitExceededException e) {
            // add something to the result that indicates the fact that the search criteria is too wide
            logger.log(Level.FINE, "the search results exceeded the maximum: '" + e.getMessage() + "'");
            output.setMaxExceeded(ldapRuntimeConfig.getMaxSearchResultSize());
            // dont throw here, we still want to return what we got
        } catch (javax.naming.AuthenticationException ae) {
            throw new FindException("LDAP search error: Authentication failed.", ae);
        } catch (PartialResultException pre) {
            logger.log(Level.WARNING, "LDAP search error, partial result.", pre);
            // don't throw, return the partial result
        } catch (NamingException e) {
            String msg = "LDAP search error with filter " + filter;
            logger.log(Level.INFO, msg, ExceptionUtils.getDebugException(e));
            throw new FindException(msg, ExceptionUtils.getDebugException(e));
        }
        return output;
    }

    @Override
    public String getAuthRealm() {
        return HexUtils.REALM;
    }

    /**
     * builds a search filter for all user object classes based on the config object
     */
    @Override
    public String userSearchFilterWithParam(String param) {
        if (config == null) throw new IllegalStateException("this provider needs a config!");

        UserMappingConfig[] userTypes = config.getUserMappings();
        ArrayList<LdapSearchTerm> terms = new ArrayList<LdapSearchTerm>();

        String safeParam = LdapUtils.filterMatchEscape( param );

        // Find all known classes of user, by both name and login
        for (UserMappingConfig userType : userTypes) {
            terms.add(new LdapSearchTerm(userType.getObjClass(), userType.getLoginAttrName(), safeParam));
            terms.add(new LdapSearchTerm(userType.getObjClass(), userType.getNameAttrName(), safeParam));
        }
        return makeSearchFilter(terms.toArray(new LdapSearchTerm[terms.size()]), false);
    }

    private String makeSearchFilter( final LdapSearchTerm[] terms, final boolean escapeTerms ) {
        LdapSearchFilter filter = new LdapSearchFilter();
        if (terms.length > 1) filter.or();

        for (LdapSearchTerm term : terms) {
            filter.and();
              filter.objectClass(term.objectclass);
              if (escapeTerms) {
                filter.attrEquals(term.searchAttribute, term.searchValue);
              } else {
                filter.attrEqualsUnsafe(term.searchAttribute, term.searchValue);
              }
            filter.end();
        }

        if (terms.length > 1) filter.end();

        return filter.buildFilter();
    }

    /**
     * builds a search filter for all group object classes based on the config object
     *
     * @return the search filter or null if no group mappings are declared for this config
     */
    @Override
    public String groupSearchFilterWithParam(String param, MemberStrategy... strategies ) {
        if (config == null) throw new IllegalStateException("this provider needs a config!");
        GroupMappingConfig[] groupTypes = config.getGroupMappings();
        if (groupTypes == null || groupTypes.length <= 0) return null;

        String safeParam = LdapUtils.filterMatchEscape( param );

        ArrayList<LdapSearchTerm> terms = new ArrayList<LdapSearchTerm>();
        for (GroupMappingConfig groupType : groupTypes) {
            if ( strategies == null || strategies.length == 0 || ArrayUtils.contains(strategies, groupType.getMemberStrategy())) {
                terms.add(new LdapSearchTerm(groupType.getObjClass(), groupType.getNameAttrName(), safeParam));
            }
        }

        return makeSearchFilter(terms.toArray(new LdapSearchTerm[terms.size()]), false);
    }

    @Override
    public DirContext getBrowseContext() throws NamingException {
        String ldapurl = getLastWorkingLdapUrl();
        if (ldapurl == null) {
            ldapurl = markCurrentUrlFailureAndGetFirstAvailableOne(null);
        }
        while (ldapurl != null) {
            Hashtable<? super String, ? super String> env = LdapUtils.newEnvironment();
            env.put(LdapUtils.ENV_PROP_LDAP_VERSION, LdapUtils.ENV_VALUE_LDAP_VERSION);
            env.put(Context.INITIAL_CONTEXT_FACTORY, LdapUtils.ENV_VALUE_INITIAL_CONTEXT_FACTORY);
            env.put(Context.PROVIDER_URL, ldapurl);
            env.put(LdapUtils.ENV_PROP_LDAP_CONNECT_POOL, "true");
            env.put(LdapUtils.ENV_PROP_LDAP_CONNECT_TIMEOUT, Long.toString(ldapRuntimeConfig.getLdapConnectionTimeout()));
            env.put(LdapUtils.ENV_PROP_LDAP_READ_TIMEOUT, Long.toString(ldapRuntimeConfig.getLdapReadTimeout()));
            env.put( Context.REFERRAL, ConfigFactory.getProperty(ServerConfigParams.PARAM_LDAP_REFERRAL, LdapUtils.ENV_VALUE_REFERRAL) );
            String dn = config.getBindDN();
            if (dn != null && dn.length() > 0) {
                final String pass;
                try {
                    pass = ServerVariables.expandSinglePasswordOnlyVariable(auditor, config.getBindPasswd());
                } catch (FindException e) {
                    throw (ConfigurationException)new ConfigurationException("Unable to expand LDAP bind password: " + ExceptionUtils.getMessage(e)).initCause(e);
                }
                env.put(Context.SECURITY_AUTHENTICATION, "simple");
                env.put(Context.SECURITY_PRINCIPAL, dn);
                env.put(Context.SECURITY_CREDENTIALS, pass);
            }

            try {
                LdapURL url = new LdapURL(ldapurl);
                if (url.useSsl()) {
                    env.put(LdapUtils.ENV_PROP_LDAP_FACTORY_SOCKET, LdapSslCustomizerSupport.getSSLSocketFactoryClassname( config.isClientAuthEnabled(), config.getKeystoreId(), config.getKeyAlias() ) );
                    env.put(Context.SECURITY_PROTOCOL, LdapUtils.ENV_VALUE_SECURITY_PROTOCOL);
                }
            } catch (NamingException e) {
                logger.log(Level.WARNING, "Malformed LDAP URL " + ldapurl + ": " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
                ldapurl = markCurrentUrlFailureAndGetFirstAvailableOne(ldapurl);
                continue;
            } catch (IllegalArgumentException e) {
                logger.log(Level.WARNING, "Malformed LDAP URL " + ldapurl + ": " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
                ldapurl = markCurrentUrlFailureAndGetFirstAvailableOne(ldapurl);
                continue;
            }

            env.putAll(LdapUtils.LDAP_ENV_OVERRIDES);
            LdapUtils.lock( env );

            try {
                // Create the initial directory context.
                return new InitialDirContext(env);
            } catch (CommunicationException e) {
                logger.log(Level.WARNING, "Could not establish context using LDAP URL " + ldapurl + ". " + ExceptionUtils.getMessageWithCause(e), ExceptionUtils.getDebugException(e));
                ldapurl = markCurrentUrlFailureAndGetFirstAvailableOne(ldapurl);
            } catch (RuntimeException e) {
                logger.log(Level.WARNING, "Could not establish context using LDAP URL " + ldapurl + ". " + ExceptionUtils.getMessageWithCause(e), ExceptionUtils.getDebugException(e));
                ldapurl = markCurrentUrlFailureAndGetFirstAvailableOne(ldapurl);
            }
        }
        throw new CommunicationException("Could not establish context on any of the ldap urls.");
    }

    @Override
    public void test(final boolean quick, String testUser, char[] testPassword) throws InvalidIdProviderCfgException {
        DirContext context = null;
        try {
            // make sure we can connect
            try {
                context = getBrowseContext();
            } catch (NamingException e) {
                String msg;
                if (e instanceof javax.naming.AuthenticationException) {
                    msg = "Cannot connect to this directory, authentication failed.";
                    logger.log(Level.INFO, "LDAP configuration test failure. " + msg, ExceptionUtils.getDebugException(e));
                } else {
                    msg = "Cannot connect to this directory '"+ExceptionUtils.getMessage(e)+"'.";
                    logger.log(Level.INFO, "LDAP configuration test failure. " + msg, ExceptionUtils.getDebugException(e));
                }

                // note. i am not embedding the NamingException because it sometimes
                // contains com.sun.jndi.ldap.LdapCtx which does not implement serializable
                throw new InvalidIdProviderCfgException(msg);
            }

            // That's all for a quick test
            if ( quick ) return;

            try {
                ldapTemplate.search( context, "(objectClass=*)", 1, null, null );
            } catch (NamingException e) {
                // note. i am not embedding the NamingException because it sometimes
                // contains com.sun.jndi.ldap.LdapCtx which does not implement serializable
                throw new InvalidIdProviderCfgException("Cannot search using base: " + config.getSearchBase());
            }

            // check user mappings. make sure they work
            boolean atLeastOneUser = false;
            final UserMappingConfig[] userTypes = config.getUserMappings();
            Collection<UserMappingConfig> offensiveUserMappings = new ArrayList<UserMappingConfig>();
            Collection<UserMappingConfig> userMappingsWithoutLoginAttribute = new ArrayList<UserMappingConfig>();
            for (UserMappingConfig userType : userTypes) {
                if (userType.getLoginAttrName() == null || userType.getLoginAttrName().length() < 1) {
                    userMappingsWithoutLoginAttribute.add(userType);
                    continue;
                }
                LdapSearchFilter filter = new LdapSearchFilter();
                filter.or();
                  filter.and();
                    filter.objectClass( userType.getObjClass() );
                    filter.attrPresent( userType.getLoginAttrName() );
                  filter.end();
                  filter.and();
                    filter.objectClass( userType.getObjClass() );
                    filter.attrPresent( userType.getLoginAttrName() );
                  filter.end();
                filter.end();

                try {
                    final boolean foundUserHolder[] = new boolean[]{false};
                    ldapTemplate.search( context, filter.buildFilter(), 0, null, new LdapUtils.LdapListener(){
                        @Override
                        boolean searchResult( final SearchResult sr ) {
                            EntityHeader header = searchResultToHeader(sr);
                            if ( header != null ) {
                                foundUserHolder[0] = true;
                                return false;
                            } else {
                                return true;
                            }
                        }
                    } );
                    atLeastOneUser |= foundUserHolder[0];
                } catch (NamingException e) {
                    offensiveUserMappings.add(userType);
                    logger.log(Level.FINE, "error testing user mapping" + userType.getObjClass(), e);
                }
            }

            // check group mappings. make sure they work
            GroupMappingConfig[] groupTypes = config.getGroupMappings();
            Collection<GroupMappingConfig> offensiveGroupMappings = new ArrayList<GroupMappingConfig>();
            boolean atLeastOneGroup = false;
            for (GroupMappingConfig groupType : groupTypes) {
                LdapSearchFilter filter = new LdapSearchFilter();
                filter.and();
                  filter.objectClass( groupType.getObjClass() );
                  filter.attrPresent( groupType.getNameAttrName() );
                filter.end();

                try {
                    final boolean foundGroupHolder[] = new boolean[]{false};
                    ldapTemplate.search( context, filter.buildFilter(), 0, null, new LdapUtils.LdapListener(){
                        @Override
                        boolean searchResult( final SearchResult sr ) {
                            EntityHeader header = searchResultToHeader(sr);
                            if ( header != null ) {
                                foundGroupHolder[0] = true;
                                return false;
                            } else {
                                return true;
                            }
                        }
                    });
                    atLeastOneGroup |= foundGroupHolder[0];
                } catch (NamingException e) {
                    offensiveGroupMappings.add(groupType);
                    logger.log(Level.FINE, "error testing group mapping" + groupType.getObjClass(), e);
                }
            }

            // test search filters if any
            boolean atLeastOneCertIndexed = true;
            boolean validSearchByIssuerSerial = true;
            boolean validSearchBySKI = true;
            switch ( config.getUserCertificateUseType() ) {
                case INDEX_CUSTOM:
                    {
                        final boolean foundCertHolder[] = new boolean[]{false};
                        try {
                            ldapTemplate.search( context, config.getUserCertificateIndexSearchFilter(), 1000, null, new LdapUtils.LdapListener(){
                                @Override
                                boolean searchResult( final SearchResult sr ) {
                                    final Attributes atts = sr.getAttributes();

                                    for ( UserMappingConfig userType : userTypes ) {
                                        String attrName = userType.getUserCertAttrName();
                                        if ( attrName==null || attrName.trim().isEmpty() ) continue;
                                        if ( atts.get( attrName ) != null ) {
                                            foundCertHolder[0] = true;
                                            return false;
                                        }
                                    }

                                    return true;
                                }
                            } );
                        } catch (Exception e) { // Search with "(" for AIOOBE, ")" for IllegalStateException
                            logger.log(Level.FINE, "Error testing certificate index search filter", e);
                        }
                        atLeastOneCertIndexed = foundCertHolder[0];
                    }
                    break;
                case SEARCH:
                    String issuerSerialSearchFilter = config.getUserCertificateIssuerSerialSearchFilter();
                    if ( issuerSerialSearchFilter!=null && !issuerSerialSearchFilter.isEmpty() ) {
                        validSearchByIssuerSerial = false;
                        try {
                            String searchFilter = formatCertificateSearchFilter(issuerSerialSearchFilter);
                            ldapTemplate.search( context, searchFilter, 1, null, null );
                            validSearchByIssuerSerial = true;
                        } catch (Exception e) { // Search with "(" for AIOOBE, ")" for IllegalStateException
                            logger.log(Level.FINE, "Error testing certificate index search filter", e);
                        }
                    }
                    String skiSearchFilter = config.getUserCertificateSKISearchFilter();
                    if ( skiSearchFilter!=null && !skiSearchFilter.isEmpty() ) {
                        validSearchBySKI = false;
                        try {
                            String searchFilter = formatCertificateSearchFilter(skiSearchFilter);
                            ldapTemplate.search( context, searchFilter, 1, null, null );
                            validSearchBySKI = true;
                        } catch (NamingException e) {
                            logger.log(Level.FINE, "Error testing certificate index search filter", e);
                        }
                    }
                    break;
                default:
            }

            // merge all errors in a special report
            StringBuilder error = new StringBuilder();

            if (userMappingsWithoutLoginAttribute.size() > 0) {
                if (error.length() > 0) error.append('\n');
                error.append("The following user mapping(s) do not define login attribute.");
                for (UserMappingConfig userMappingConfig : userMappingsWithoutLoginAttribute) {
                    error.append(" ").append(userMappingConfig.getObjClass());
                }
            }

            if (offensiveUserMappings.size() > 0 || offensiveGroupMappings.size() > 0) {
                if (error.length() > 0) error.append('\n');
                error.append("The following mappings caused errors:");
                for (UserMappingConfig offensiveUserMapping : offensiveUserMappings) {
                    error.append(" User mapping ").append(offensiveUserMapping.getObjClass());
                }
                for (GroupMappingConfig offensiveGroupMapping : offensiveGroupMappings) {
                    error.append(" Group mapping ").append(offensiveGroupMapping.getObjClass());
                }
            }

            if (!atLeastOneUser) {
                if (error.length() > 0) error.append('\n');
                error.append("This configuration did not yield any users");
            }

            if (!atLeastOneGroup && groupTypes.length > 0) {
                if (error.length() > 0) error.append('\n');
                error.append("This configuration did not yield any group");
            }

            if ( !atLeastOneCertIndexed ) {
                if (error.length() > 0) error.append('\n');
                error.append("The search filter for certificate indexing did not yield any user certificates");
            }

            if ( !validSearchByIssuerSerial ) {
                if (error.length() > 0) error.append('\n');
                error.append("The search filter for user certificates by issuer name and serial number is invalid.");
            }

            if ( !validSearchBySKI ) {
                if (error.length() > 0) error.append('\n');
                error.append("The search filter for user certificates by subject key identifier is invalid.");
            }

            if (error.length() > 0) {
                logger.fine("Test produced following error(s): " + error.toString());
                throw new InvalidIdProviderCfgException(error.toString());
            } else
                logger.finest("this ldap config was tested successfully");
        } finally {
            ResourceUtils.closeQuietly(context);
        }
    }

    @Override
    public void preSaveClientCert(LdapUser user, X509Certificate[] certChain) throws ClientCertManager.VetoSave {
        // ClientCertManagerImp's default rules are OK
    }

    @Override
    public void setUserManager(LdapUserManager userManager) {
        this.userManager = userManager;
    }

    @Override
    public void setGroupManager(LdapGroupManager groupManager) {
        this.groupManager = groupManager;
    }

    @Override
    public boolean hasClientCert(String login) throws AuthenticationException {
        try {
            LdapUser ldapUser = userManager.findByLogin(login);
            if (ldapUser != null) {
                //check where we should be looking for the cert, the cert could reside either in LDAP or gateway
                if (certsAreEnabled()) {
                    //telling use to search cert through ldap
                    return ldapUser.getLdapCertBytes() != null;
                } else {
                    //telling use to search the gateway
                    return clientCertManager.getUserCert(ldapUser) != null;
                }
            } else {
                //we cant even find the user, so there won't be a cert available
                return false;
            }
        } catch (FindException fe) {
            throw new AuthenticationException(String.format("Couldn't find user '%s'", login), fe);
        }
    }

    public void setClientCertManager(final ClientCertManager clientCertManager) {
        this.clientCertManager = clientCertManager;
    }

    public void setConfigManager(final IdentityProviderConfigManager configManager) {
        this.configManager = configManager;
    }

    /**
     * Invoked by a BeanFactory after it has set all bean properties supplied
     *
     * @throws Exception in the event of misconfiguration (such
     *                   as failure to set an essential property) or if initialization fails.
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        if (clientCertManager == null) throw new IllegalStateException("The Client Certificate Manager is required");
        if (auditor == null) throw new IllegalStateException("Auditor has not been initialized");
        if (userManager == null) throw new IllegalStateException("UserManager has not been initialized");
        if (groupManager == null) throw new IllegalStateException("GroupManager has not been initialized");
    }

    @Override
    public void start() throws LifecycleException {
        ldapCertificateCache.start();
        if ( groupManager instanceof Lifecycle ) {
            ((Lifecycle)groupManager).start();
        }
    }

    @Override
    public void stop() throws LifecycleException {
        ldapCertificateCache.stop();
        if ( groupManager instanceof Lifecycle ) {
            ((Lifecycle)groupManager).stop();
        }
    }

    /**
     * In MSAD, there is an attribute named userAccountControl which contains information
     * regarding the validity of the account pointed to by the ldap entry. This method will return false
     * IF the attribute is present AND IF one of those attributes is set:
     * 0x00000002 	The user account is disabled.
     * 0x00000010 	The account is currently locked out.
     * 0x00800000 	The user password has expired.
     * <p/>
     * Otherwise, will return true.
     * <p/>
     * See bugzilla #1116, #1466 for the justification of this check.
     */
    @Override
    public boolean isValidEntryBasedOnUserAccountControlAttribute(String userDn, Attributes attibutes) {
        final long DISABLED_FLAG = 0x00000002;
        final long LOCKED_FLAG = 0x00000010;
        final long EXPIRED_FLAG = 0x00800000; // add a check for this flag in an attempt to fix 1466
        Attribute userAccountControlAttr = attibutes.get( LdapUtils.LDAP_ATTR_USER_ACCOUNT_CONTROL );
        if (userAccountControlAttr != null && userAccountControlAttr.size() > 0) {
            Object found = null;
            try {
                found = userAccountControlAttr.get(0);
            } catch (NamingException e) {
                logger.log(Level.SEVERE, "Problem accessing the userAccountControl attribute");
            }
            Long value = null;
            if (found instanceof String) {
                value = new Long((String)found);
            } else if (found instanceof Long) {
                value = (Long)found;
            } else if (found != null) {
                logger.severe("FOUND userAccountControl attribute but " +
                  "is of unexpected type: " + found.getClass().getName());
            }
            if (value != null) {
                if ((value & DISABLED_FLAG) == DISABLED_FLAG) {
                    auditor.logAndAudit(SystemMessages.AUTH_USER_DISABLED, userDn);
                    return false;
                } else if ((value & LOCKED_FLAG) == LOCKED_FLAG) {
                    auditor.logAndAudit(SystemMessages.AUTH_USER_LOCKED, userDn);
                    return false;
                }  else if ((value & EXPIRED_FLAG) == EXPIRED_FLAG) {
                    auditor.logAndAudit(SystemMessages.AUTH_USER_EXPIRED, userDn);
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * this check is only relevent to msad directories, it should be generalized and parametrable when
     * we discover other types of account expiration mechanism in other directory types.
     *
     * this looks at the attribute accountExpires if present and checks if the specific account
     * is expired based on that value.
     *
     * @return true is account is expired, false otherwise
     */
    @Override
    public boolean checkExpiredMSADAccount(String userDn, Attributes attibutes) {
        Attribute accountExpiresAttr = attibutes.get( LdapUtils.LDAP_ATTR_ACCOUNT_EXPIRES );
        if (accountExpiresAttr != null && accountExpiresAttr.size() > 0) {
            Object found = null;
            try {
                found = accountExpiresAttr.get(0);
            } catch (NamingException e) {
                logger.log(Level.SEVERE, "Problem accessing the accountExpiresAttr attribute");
            }
            if (found != null) {
                if (found instanceof String) {
                    String value = (String)found;
                    if (value.equals("0")) {
                        return false;
                    } else {
                        BigInteger thisvalue = new BigInteger(value);
                        BigInteger result = thisvalue.subtract(fileTimeConversionfactor).
                                                            divide(fileTimeConversionfactor2);
                        if (System.currentTimeMillis() > result.longValue()) {
                            auditor.logAndAudit(SystemMessages.AUTH_USER_EXPIRED, userDn);
                            return true;
                        }
                    }
                } else {
                    logger.severe("the attribute accountExpires was present but yielded a type not " +
                                  "supported: " + found.getClass().getName());
                }
            }
        }
        return false;
    }

    /**
     * Constructs an EntityHeader for the dn passed.
     *
     * @param sr The search result
     * @return the EntityHeader for the dn or null if the object class is not supported or if the entity
     *         should be ignored (perhaps disabled)
     */
    @Override
    public IdentityHeader searchResultToHeader(SearchResult sr) {
        final Attributes atts = sr.getAttributes();
        final String dn = sr.getNameInNamespace();
        // is it user or group ?
        Attribute objectclasses = atts.get( OBJECTCLASS_ATTRIBUTE_NAME );
        // check if it's a user
        UserMappingConfig[] userTypes = config.getUserMappings();
        for (UserMappingConfig userType : userTypes) {
            String userclass = userType.getObjClass();
            if (LdapUtils.attrContainsCaseIndependent(objectclasses, userclass)) {
                if (!isValidEntryBasedOnUserAccountControlAttribute(dn, atts) || checkExpiredMSADAccount(dn, atts)) {
                    return null;
                }
                Object tmp;
                String login = null;
                try {
                    tmp = LdapUtils.extractOneAttributeValue(atts, userType.getLoginAttrName());
                } catch (NamingException e) {
                    logger.log(Level.WARNING, "cannot extract user login", e);
                    tmp = null;
                }
                if (tmp != null) {
                    login = tmp.toString();
                }

                // if cn is present use it
                String cn = null;
                try{
                    tmp = LdapUtils.extractOneAttributeValue(atts, userType.getNameAttrName());
                }catch(NamingException e){
                    logger.log(Level.WARNING, "cannot extract cn", e);
                    tmp = null;
                }
                if(tmp != null){
                    cn = tmp.toString();
                }

                // if description attribute present, use it
                String description = null;
                try {
                    tmp = LdapUtils.extractOneAttributeValue(atts, DESCRIPTION_ATTRIBUTE_NAME);
                } catch (NamingException e) {
                    logger.log(Level.FINEST, "no description for this entry", e);
                    tmp = null;
                }
                if (tmp != null) {
                    description = tmp.toString();
                }
                if (login != null) {
                    return new IdentityHeader(config.getOid(), dn, EntityType.USER, login, description, cn, null);
                } else {
                    return null;
                }
            }
        }
        // check that it's a group
        GroupMappingConfig[] groupTypes = config.getGroupMappings();
        for (GroupMappingConfig groupType : groupTypes)
            if (LdapUtils.attrContainsCaseIndependent(objectclasses, groupType.getObjClass())) {
                String groupName = null;
                Attribute valuesWereLookingFor = atts.get(groupType.getNameAttrName());
                if (valuesWereLookingFor != null && valuesWereLookingFor.size() > 0) {
                    try {
                        groupName = valuesWereLookingFor.get(0).toString();
                    } catch (NamingException e) {
                        logger.log( Level.WARNING, "cannot extract name from this group", ExceptionUtils.getDebugException( e ) );
                    }
                }
                if (groupName == null) groupName = dn;
                // if description attribute present, use it
                Object tmp;
                String description = null;
                try {
                    tmp = LdapUtils.extractOneAttributeValue(atts, DESCRIPTION_ATTRIBUTE_NAME);
                } catch (NamingException e) {
                    logger.log(Level.FINEST, "no description for this entry", e);
                    tmp = null;
                }
                if (tmp != null) {
                    description = tmp.toString();
                }
                return new IdentityHeader(config.getOid(), dn, EntityType.GROUP, groupName, description, null, null);
            }
        return null;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.auditor = new Auditor(this, applicationContext, logger);
    }

    /*
     * ValidationException exceptions do not state that the user belongs to an ldap or in which
     * ldap the user was not found
     **/
    @Override
    public void validate(LdapUser u) throws ValidationException {
        User validatedUser;
        try{
            validatedUser = userManager.findByPrimaryKey(u.getId());
        }
        catch (FindException e){
            throw new ValidationException("User " + u.getLogin()+" did not validate", e);
        }

        if(validatedUser == null){
            throw new ValidationException("IdentityProvider User " + u.getLogin()+" not found");
        }
    }

    @Override
    public String getLastWorkingLdapUrl() {
        return urlProvider.getLastWorkingLdapUrl();
    }

    @Override
    public String markCurrentUrlFailureAndGetFirstAvailableOne(@Nullable String urlThatFailed) {
        return urlProvider.markCurrentUrlFailureAndGetFirstAvailableOne(urlThatFailed);
    }

    private String formatCertificateSearchFilter( final String filter ) throws InvalidIdProviderCfgException {
        try {
            return formatCertificateSearchFilter( filter, new X500Principal("cn=Test Issuer, ou=Test Organization, c=Test"), BigInteger.ZERO, HexUtils.encodeBase64(new byte[1])) ;
        } catch (FindException fe) {
            throw new InvalidIdProviderCfgException( fe.getMessage() );
        }
    }

    private String formatCertificateSearchFilter( final String filter, final X500Principal issuer, final BigInteger serial, final String ski ) throws FindException  {
        Map<String,String> varMap = new HashMap<String,String>();

        if ( issuer != null ) {
            varMap.put( "issuer", issuer.getName() );
            varMap.put( "issuer.canonical", issuer.getName(X500Principal.CANONICAL));
            varMap.put( "issuer.rfc2253", issuer.getName() );
        }

        if ( serial != null ) {
            varMap.put( "serialnumber", serial.toString() );
        }

        if ( ski != null ) {
            varMap.put( "subjectkeyidentifier", ski );
            varMap.put( "subjectkeyidentifier.hex", HexUtils.hexDump(HexUtils.decodeBase64(ski)));
        }

        try {
            return ExpandVariables.process( filter, varMap, auditor, true, new Functions.Unary<String,String>(){
                @Override
                public String call( final String value ) {
                    return LdapUtils.filterEscape( value );
                }
            } );
        } catch ( VariableNameSyntaxException vnse ) {
            throw new FindException("Search filter variable error '"+ExceptionUtils.getMessage(vnse)+"'.");
        } catch ( IllegalArgumentException iae ) {
            throw new FindException("Search filter variable processing error '"+ExceptionUtils.getMessage(iae)+"'.");
        }
    }

    private String[] buildReturningAttributes() {
        Set<String> attributeNames = new LinkedHashSet<String>();

        // Various hard coded attributes that we use
        attributeNames.add( OBJECTCLASS_ATTRIBUTE_NAME );
        attributeNames.add( DESCRIPTION_ATTRIBUTE_NAME );
        attributeNames.add( LdapUtils.LDAP_ATTR_USER_ACCOUNT_CONTROL );
        attributeNames.add( LdapUtils.LDAP_ATTR_ACCOUNT_EXPIRES );

        // User mapping attributes
        UserMappingConfig[] userTypes = config.getUserMappings();
        for (UserMappingConfig userType : userTypes) {
            addValidName( attributeNames, userType.getEmailNameAttrName() );
            addValidName( attributeNames, userType.getFirstNameAttrName() );
            addValidName( attributeNames, userType.getKerberosAttrName() );
            addValidName( attributeNames, userType.getKerberosEnterpriseAttrName() );
            addValidName( attributeNames, userType.getLastNameAttrName() );
            addValidName( attributeNames, userType.getLoginAttrName() );
            addValidName( attributeNames, userType.getNameAttrName() );
            addValidName( attributeNames, userType.getPasswdAttrName() );
            addValidName( attributeNames, userType.getUserCertAttrName(), LdapUtils.LDAP_ATTR_USER_CERTIFICATE );
        }

        // Group mapping attributes
        GroupMappingConfig[] groupTypes = config.getGroupMappings();
        for (GroupMappingConfig groupType : groupTypes) {
            addValidName( attributeNames, groupType.getMemberAttrName() );
            addValidName( attributeNames, groupType.getNameAttrName() );
        }

        // Configured attributes
        String[] attributes = config.getReturningAttributes();
        if ( attributes != null ) {
            attributeNames.addAll( Arrays.asList(attributes) );
        }

        return attributeNames.toArray( new String[attributeNames.size()] );
    }

    private void addValidName( final Collection<? super String> names, final String name ) {
        addValidName( names, name, null );
    }

    private void addValidName( final Collection<? super String> names, final String name, final String defaultValue ) {
        if ( name != null && !name.isEmpty() ) {
            names.add( name.trim() );
        } else if ( defaultValue != null ) {
            names.add( defaultValue );           
        }
    }

    private static final Logger logger = Logger.getLogger(LdapIdentityProviderImpl.class.getName());

    private static final BigInteger fileTimeConversionfactor = new BigInteger("116444736000000000");
    private static final BigInteger fileTimeConversionfactor2 = new BigInteger("10000");

    private Auditor auditor;
    private LdapRuntimeConfig ldapRuntimeConfig;
    private LdapIdentityProviderConfig config;
    private IdentityProviderConfigManager configManager;
    private ClientCertManager clientCertManager;
    private LdapUserManager userManager;
    private LdapGroupManager groupManager;
    private CertificateAuthenticator certificateAuthenticator;
    private SessionAuthenticator sessionAuthenticator;

    private LdapUrlProviderImpl urlProvider;
    private String[] returningAttributes;
    private LdapUtils.LdapTemplate ldapTemplate;
    private LdapCertificateCache ldapCertificateCache;
}
