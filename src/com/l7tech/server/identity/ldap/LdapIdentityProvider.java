package com.l7tech.server.identity.ldap;

import EDU.oswego.cs.dl.util.concurrent.ReadWriteLock;
import EDU.oswego.cs.dl.util.concurrent.Sync;
import EDU.oswego.cs.dl.util.concurrent.WriterPreferenceReadWriteLock;
import com.l7tech.identity.AuthenticationException;
import com.l7tech.identity.*;
import com.l7tech.identity.cert.ClientCertManager;
import com.l7tech.identity.ldap.GroupMappingConfig;
import com.l7tech.identity.ldap.LdapIdentityProviderConfig;
import com.l7tech.identity.ldap.LdapUser;
import com.l7tech.identity.ldap.UserMappingConfig;
import com.l7tech.identity.mapping.IdentityMapping;
import com.l7tech.identity.mapping.LdapAttributeMapping;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityHeaderComparator;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.assertion.credential.CredentialFormat;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.credential.http.HttpDigest;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.identity.DigestAuthenticator;
import com.l7tech.server.identity.AuthenticationResult;
import com.l7tech.server.identity.cert.CertificateAuthenticator;
import com.l7tech.common.security.kerberos.KerberosServiceTicket;

import org.springframework.beans.factory.InitializingBean;

import javax.naming.*;
import javax.naming.directory.*;
import java.math.BigInteger;
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
public class LdapIdentityProvider implements IdentityProvider, InitializingBean {
    /**
     * LDAP connection attempts will fail after 5 seconds' wait
     */
    public static final String LDAP_CONNECT_TIMEOUT = Integer.toString(5 * 1000);
    /**
     * An unused LDAP connection will be closed after 30 seconds of inactivity
     */
    public static final String LDAP_POOL_IDLE_TIMEOUT = Integer.toString(30 * 1000);

    public LdapIdentityProvider(IdentityProviderConfig config) {
        this.config = (LdapIdentityProviderConfig)config;
        if (this.config.getLdapUrl() == null || this.config.getLdapUrl().length < 1) {
            throw new IllegalArgumentException("This config does not contain an ldap url"); // should not happen
        }
    }

    /** for subclassing such as class proxying */
    protected LdapIdentityProvider() {
    }

    public void setServerConfig(ServerConfig serverConfig) {
        this.serverConfig = serverConfig;
    }

    public void setCertificateAuthenticator(CertificateAuthenticator certificateAuthenticator) {
        this.certificateAuthenticator = certificateAuthenticator;
    }


    public void initializeFallbackMechanism() {
        // configure timeout period
        String property = serverConfig.getProperty("ldap.reconnect.timeout");
        if (property == null || property.length() < 1) {
            retryFailedConnectionTimeout = 60000;
            logger.warning("ldap.reconnect.timeout server property not set. using default");
        } else {
            try {
                retryFailedConnectionTimeout = Long.parseLong(property);
            } catch (NumberFormatException e) {
                logger.log(Level.WARNING, "ldap.reconnect.timeout property not configured properly. using default", e);
                retryFailedConnectionTimeout = 60000;
            }
        }
        // build a table of ldap urls status
        ldapUrls = config.getLdapUrl();
        urlStatus = new Long[ldapUrls.length];
        lastSuccessfulLdapUrl = ldapUrls[0];
    }

    public IdentityProviderConfig getConfig() {
        return config;
    }

    public UserManager getUserManager() {
        return userManager;
    }

    public GroupManager getGroupManager() {
        return groupManager;
    }

    /**
     * @return The ldap url that was last used to successfully connect to the ldap directory. May be null if
     *         previous attempt failed on all available urls.
     */
    String getLastWorkingLdapUrl() {
        Sync read = fallbackLock.readLock();
        try {
            read.acquire();
            return lastSuccessfulLdapUrl;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            if (read != null) read.release();
        }
    }

    /**
     * Remember that the passed URL could not be used to connect normally and get the next ldap URL from
     * the list that should be tried to connect with. Will return null if all known urls have failed to
     * connect in the last while. (last while being a configurable timeout period defined in
     * serverconfig.properties under ldap.reconnect.timeout in ms)
     *
     * @param urlThatFailed the url that failed to connect, or null if no url was previously available
     * @return the next url in the list or null if all urls were marked as failure within the last while
     */
    String markCurrentUrlFailureAndGetFirstAvailableOne(String urlThatFailed) {
        Sync write = fallbackLock.writeLock();
        try {
            write.acquire();
            //noinspection StringEquality
            if (urlThatFailed != lastSuccessfulLdapUrl) return lastSuccessfulLdapUrl;
            if (urlThatFailed != null) {
                int failurePos = 0;
                for (int i = 0; i < ldapUrls.length; i++) {
                    //noinspection StringEquality
                    if (ldapUrls[i] == urlThatFailed) {
                        failurePos = i;
                        urlStatus[i] = new Long(System.currentTimeMillis());
                        logger.info("Blacklisting url for next " + (retryFailedConnectionTimeout / 1000) +
                          " seconds : " + ldapUrls[i]);
                    }
                }
                if (failurePos > (ldapUrls.length - 1)) {
                    throw new RuntimeException("passed a url not in list"); // this should not happen
                }
            }
            // find first available url
            for (int i = 0; i < ldapUrls.length; i++) {
                boolean thisoneok = false;
                if (urlStatus[i] == null) {
                    thisoneok = true;
                    logger.fine("Try url not on blacklist yet " + ldapUrls[i]);
                } else {
                    long howLong = System.currentTimeMillis() - urlStatus[i].longValue();
                    if (howLong > retryFailedConnectionTimeout) {
                        thisoneok = true;
                        urlStatus[i] = null;
                        logger.fine("Ldap URL has been blacklisted long enough. Trying it again: " + ldapUrls[i]);
                    }
                }
                if (thisoneok) {
                    logger.info("Trying to recover using this url: " + ldapUrls[i]);
                    lastSuccessfulLdapUrl = ldapUrls[i];
                    return lastSuccessfulLdapUrl;
                }
            }
            logger.fine("All ldap urls are blacklisted.");
            lastSuccessfulLdapUrl = null;
            return null;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            if (write != null) write.release();
        }
    }

    public AuthenticationResult authenticate(LoginCredentials pc) throws AuthenticationException {
        LdapUser realUser = null;
        if (pc.getFormat() == CredentialFormat.KERBEROSTICKET) {
            KerberosServiceTicket ticket = (KerberosServiceTicket) pc.getPayload();

            Collection headers = null;
            String upn = ticket.getClientPrincipalName();
            try {
                headers = search(true, false, getKerberosLdapAttributeMapping(), upn);
                if (headers.size() > 1) {
                    throw new AuthenticationException("Found multiple LDAP users with userPrincipalName '" + upn + "'.");
                }
                else if (!headers.isEmpty()){
                    for (Iterator i = headers.iterator(); i.hasNext();) {
                        EntityHeader header = (EntityHeader) i.next();
                        if (header.getType() == EntityType.USER) {
                            realUser = (LdapUser) userManager.findByPrimaryKey(header.getStrId());
                        }
                    }
                    return new AuthenticationResult(realUser);
                }
            } catch (FindException e) {
                throw new AuthenticationException("Couldn't find LDAP user with userPrincipalName '" + upn + "'.", e);
            }
        } else {
            try {
                realUser = (LdapUser) userManager.findByLogin(pc.getLogin());
            } catch (FindException e) {
                throw new AuthenticationException("Couldn't authenticate credentials", e);
            }
        }

        if (realUser == null) return null;

        final CredentialFormat format = pc.getFormat();
        if (format == CredentialFormat.CLEARTEXT) {
            return authenticatePasswordCredentials(pc, realUser);
        } else if (format == CredentialFormat.DIGEST) {
            return DigestAuthenticator.authenticateDigestCredentials(pc, realUser);
        } else {
            if (format == CredentialFormat.CLIENTCERT || format == CredentialFormat.SAML) {
                return certificateAuthenticator.authenticateX509Credentials(pc, realUser);
            } else {
                String msg = "Attempt to authenticate using unsupported method on this provider: " + pc.getFormat();
                logger.log(Level.SEVERE, msg);
                throw new AuthenticationException(msg);
            }
        }
    }

    private AuthenticationResult authenticatePasswordCredentials(LoginCredentials pc, LdapUser realUser) throws BadCredentialsException {
        // basic authentication
        boolean res = userManager.authenticateBasic(realUser.getDn(), new String(pc.getCredentials()));
        if (res) {
            // success
            return new AuthenticationResult(realUser);
        }
        logger.info("credentials did not authenticate for " + pc.getLogin());
        throw new BadCredentialsException("credentials did not authenticate");
    }

    long getMaxSearchResultSize() {
        if (maxSearchResultSize <= 0) {
            String tmp = serverConfig.getProperty(ServerConfig.MAX_LDAP_SEARCH_RESULT_SIZE);
            if (tmp == null) {
                logger.info(ServerConfig.MAX_LDAP_SEARCH_RESULT_SIZE + " is not set. using default value.");
                maxSearchResultSize = 100;
            } else {
                long tmpl;
                try {
                    tmpl = Long.parseLong(tmp);
                    if (tmpl <= 0) {
                        logger.info(ServerConfig.MAX_LDAP_SEARCH_RESULT_SIZE + " has invalid value: " + tmp +
                                    ". using default value.");
                        maxSearchResultSize = 100;
                    } else {
                        logger.info("read system value " + ServerConfig.MAX_LDAP_SEARCH_RESULT_SIZE + " of " + tmp);
                        maxSearchResultSize = tmpl;
                    }
                } catch (NumberFormatException e) {
                    logger.log(Level.WARNING, "the property " + ServerConfig.MAX_LDAP_SEARCH_RESULT_SIZE +
                                              " has an invalid format. falling back on default value.", e);
                    maxSearchResultSize = 100;
                }
            }
        }
        return maxSearchResultSize;
    }

    /**
     * searches the ldap provider for identities
     *
     * @param types        any combination of EntityType.USER and or EntityType.GROUP
     * @param searchString the search string for the users and group names, use "*" for all
     * @return a collection containing EntityHeader objects
     */
    public Collection search(EntityType[] types, String searchString) throws FindException {
        if (types == null || types.length < 1) {
            throw new IllegalArgumentException("must pass at least one type");
        }

        boolean wantUsers = false;
        boolean wantGroups = false;

        for (int i = 0; i < types.length; i++) {
            if (types[i] == EntityType.USER)
                wantUsers = true;
            else if (types[i] == EntityType.GROUP) wantGroups = true;
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
            filter = userSearchFilterWithParam(searchString);
        } else
            filter = groupSearchFilterWithParam(searchString);

        // no group mapping is now allowed
        if (filter == null) {
            return Collections.EMPTY_SET;
        }

        return doSearch(filter);
    }

    private Collection doSearch(String filter) {
        Collection output = new TreeSet(new EntityHeaderComparator());
        DirContext context = null;
        try {
            NamingEnumeration answer;
            // search string for users and or groups based on passed types wanted
            SearchControls sc = new SearchControls();
            sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
            sc.setCountLimit(getMaxSearchResultSize());
            context = getBrowseContext();
            answer = context.search(config.getSearchBase(), filter, sc);
            while (answer.hasMore()) {
                // get this item
                SearchResult sr = (SearchResult)answer.next();
                // set the dn (unique id)
                String dn = sr.getName() + "," + config.getSearchBase();
                EntityHeader header = searchResultToHeader(sr, dn);
                // if we successfully constructed a header, add it to result list
                if (header != null)
                    output.add(header);
                else
                    logger.warning("entry not valid or objectclass not supported for dn=" + dn + ". this " +
                      "entry will not be presented as part of the search results.");
            }
            answer.close();
        } catch (SizeLimitExceededException e) {
            // add something to the result that indicates the fact that the search criteria is too wide
            logger.log(Level.FINE, "the search results exceede the maximum. adding a " +
                                   "EntityType.MAXED_OUT_SEARCH_RESULT to the results",
                                   e);
            EntityHeader maxExceeded = new EntityHeader("noid",
                                                        EntityType.MAXED_OUT_SEARCH_RESULT,
                                                        "Search criterion too wide",
                                                        "This search yields too many entities. " +
                                                        "Please narrow your search criterion.");
            output.add(maxExceeded);
            // dont throw here, we still want to return what we got
        } catch (NamingException e) {
            logger.log(Level.WARNING, "error searching with filter: " + filter, e);
        } finally {
            if (context != null) {
                try {
                    context.close();
                } catch (NamingException e) {
                    logger.log(Level.WARNING, "Caught NamingException while closing LDAP Context", e);
                }
            }
        }
        return output;
    }

    public Collection search(boolean getusers, boolean getgroups, IdentityMapping mapping, Object attValue) throws FindException {
        if (mapping instanceof LdapAttributeMapping) {
            LdapAttributeMapping lam = (LdapAttributeMapping) mapping;
            String attName = lam.getAttributeName();

            if (!(getusers || getgroups)) {
                logger.info("Nothing to search for - specified EntityType not supported by this IdentityMapping");
                return Collections.EMPTY_LIST;
            }

            String userFilter = null;
            String groupFilter = null;
            if (getusers) {
                ArrayList terms = new ArrayList();
                for (int i = 0; i < config.getUserMappings().length; i++) {
                    UserMappingConfig userMappingConfig = config.getUserMappings()[i];
                    terms.add(new LdapSearchTerm(userMappingConfig.getObjClass(), attName, attValue.toString()));
                }
                userFilter = makeSearchFilter((LdapSearchTerm[])terms.toArray(new LdapSearchTerm[0]));
            }

            if (getusers) {
                ArrayList terms = new ArrayList();
                for (int i = 0; i < config.getGroupMappings().length; i++) {
                    GroupMappingConfig groupMappingConfig = config.getGroupMappings()[i];
                    terms.add(new LdapSearchTerm(groupMappingConfig.getObjClass(), attName, attValue.toString()));
                }
                groupFilter = makeSearchFilter((LdapSearchTerm[])terms.toArray(new LdapSearchTerm[0]));
            }

            String filter;
            if (getusers && getgroups) {
                filter = "(|" + userFilter + groupFilter + ")";
            } else if (getusers) {
                filter = userFilter;
            } else {
                filter = groupFilter;
            }

            return doSearch(filter);
        } else {
            throw new FindException("Unsupported AttributeMapping type");
        }
    }

    public String getAuthRealm() {
        return HttpDigest.REALM;
    }

    /**
     * builds a search filter for all user object classes based on the config object
     */
    public String userSearchFilterWithParam(String param) {
        if (config == null) throw new IllegalStateException("this provider needs a config!");

        UserMappingConfig[] userTypes = config.getUserMappings();
        ArrayList terms = new ArrayList();

        // Find all known classes of user, by both name and login
        for (int i = 0; i < userTypes.length; i++) {
            UserMappingConfig userType = userTypes[i];
            terms.add(new LdapSearchTerm(userType.getObjClass(), userType.getLoginAttrName(), param));
            terms.add(new LdapSearchTerm(userType.getObjClass(), userType.getNameAttrName(), param));
        }
        return makeSearchFilter(((LdapSearchTerm[])terms.toArray(new LdapSearchTerm[0])));
    }

    private static class LdapSearchTerm {
        public LdapSearchTerm(String objectclass, String searchAttribute, String searchValue) {
            this.objectclass = objectclass;
            this.searchAttribute = searchAttribute;
            this.searchValue = searchValue;
        }

        private final String objectclass;
        private final String searchAttribute;
        private final String searchValue;
    }

    private String makeSearchFilter(LdapSearchTerm[] terms) {
        StringBuffer output = new StringBuffer();
        if (terms.length > 1) output.append("(|");

        for (int i = 0; i < terms.length; i++) {
            output.append("(&");
            output.append(  "(objectClass=").append(terms[i].objectclass).append(")");
            output.append(  "(").append(terms[i].searchAttribute).append("=").append(terms[i].searchValue).append(")");
            output.append(")");
        }

        if (terms.length > 1) output.append(")");
        return output.toString();
    }

    /**
     * builds a search filter for all group object classes based on the config object
     *
     * @return the search filter or null if no group mappings are declared for this config
     */
    public String groupSearchFilterWithParam(String param) {
        if (config == null) throw new IllegalStateException("this provider needs a config!");
        GroupMappingConfig[] groupTypes = config.getGroupMappings();
        if (groupTypes == null || groupTypes.length <= 0) return null;

        ArrayList terms = new ArrayList();
        for (int i = 0; i < groupTypes.length; i++) {
            GroupMappingConfig groupType = groupTypes[i];
            terms.add(new LdapSearchTerm(groupType.getObjClass(), groupType.getNameAttrName(), param));
        }

        return makeSearchFilter((LdapSearchTerm[])terms.toArray(new LdapSearchTerm[0]));
    }

    public DirContext getBrowseContext() throws NamingException {
        String ldapurl = getLastWorkingLdapUrl();
        if (ldapurl == null) {
            ldapurl = markCurrentUrlFailureAndGetFirstAvailableOne(ldapurl);
        }
        while (ldapurl != null) {
            UnsynchronizedNamingProperties env = new UnsynchronizedNamingProperties();
            // fla note: this is weird. new BrowseContext objects are created at every operation so they
            // should not cross threads.
            env.put("java.naming.ldap.version", "3");
            env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
            // when getting javax.naming.CommunicationException at
            env.put(Context.PROVIDER_URL, ldapurl);
            env.put("com.sun.jndi.ldap.connect.pool", "true");
            env.put("com.sun.jndi.ldap.connect.timeout", LDAP_CONNECT_TIMEOUT);
            env.put("com.sun.jndi.ldap.connect.pool.timeout", LDAP_POOL_IDLE_TIMEOUT);
            env.put( Context.REFERRAL, "follow" );
            String dn = config.getBindDN();
            if (dn != null && dn.length() > 0) {
                String pass = config.getBindPasswd();
                env.put(Context.SECURITY_AUTHENTICATION, "simple");
                env.put(Context.SECURITY_PRINCIPAL, dn);
                env.put(Context.SECURITY_CREDENTIALS, pass);
            }
            env.lock();

            try {
                // Create the initial directory context.
                return new InitialDirContext(env);
            } catch (CommunicationException e) {
                logger.log(Level.INFO, "Could not establish context using LDAP URL " + ldapurl, e);
                ldapurl = markCurrentUrlFailureAndGetFirstAvailableOne(ldapurl);
            } catch (RuntimeException e) {
                logger.log(Level.INFO, "Could not establish context using LDAP URL " + ldapurl, e);
                ldapurl = markCurrentUrlFailureAndGetFirstAvailableOne(ldapurl);
            }
        }
        throw new CommunicationException("Could not establish context on any of the ldap urls.");
    }

    public void test() throws InvalidIdProviderCfgException {
        // make sure we can connect
        DirContext context;
        try {
            context = getBrowseContext();
        } catch (NamingException e) {
            // note. i am not embedding the NamingException because it sometimes
            // contains com.sun.jndi.ldap.LdapCtx which does not implement serializable
            String msg = "Cannot connect to this directory.";
            logger.log(Level.INFO, "ldap config test failure " + msg, e);
            throw new InvalidIdProviderCfgException(msg, e);
        }

        NamingEnumeration answer = null;
        String filter;
        SearchControls sc = new SearchControls();
        sc.setSearchScope(SearchControls.SUBTREE_SCOPE);

        // make sure the base DN is valid and contains at least one entry
        try {
            answer = context.search(config.getSearchBase(), "(objectClass=*)", sc);
        } catch (NamingException e) {
            String msg = "Cannot search using base: " + config.getSearchBase();
            logger.log(Level.INFO, "ldap config test failure " + msg, e);

            // cleanup and leave test with exception
            try {
                if (answer != null) answer.close();
            } catch (NamingException e2) {
                logger.finest("Could not close answer " + e2.getMessage());
            }
            try {
                context.close();
            } catch (NamingException e2) {
                logger.finest("Could not close context " + e2.getMessage());
            }

            throw new InvalidIdProviderCfgException(msg);
        }

        // check user mappings. make sure they work
        boolean atLeastOneUser = false;
        UserMappingConfig[] userTypes = config.getUserMappings();
        Collection offensiveUserMappings = new ArrayList();
        Collection userMappingsWithoutLoginAttribute = new ArrayList();
        for (int i = 0; i < userTypes.length; i++) {
            if (userTypes[i].getLoginAttrName() == null || userTypes[i].getLoginAttrName().length() < 1) {
                userMappingsWithoutLoginAttribute.add(userTypes[i]);
                continue;
            }
            filter = "(|" +
              "(&" +
              "(objectClass=" + userTypes[i].getObjClass() + ")" +
              "(" + userTypes[i].getLoginAttrName() + "=*)" +
              ")" +
              "(&" +
              "(objectClass=" + userTypes[i].getObjClass() + ")" +
              "(" + userTypes[i].getNameAttrName() + "=*)" +
              ")" +
              ")";
            try {
                answer = context.search(config.getSearchBase(), filter, sc);
                while (answer.hasMore()) {
                    SearchResult sr = (SearchResult)answer.next();
                    // set the dn (unique id)
                    String dn = sr.getName() + "," + config.getSearchBase();
                    EntityHeader header = searchResultToHeader(sr, dn);
                    // if we successfully constructed a header, add it to result list
                    if (header != null) {
                        atLeastOneUser = true;
                        break;
                    }
                }
                answer.close();
            } catch (NamingException e) {
                offensiveUserMappings.add(userTypes[i]);
                logger.log(Level.FINE, "error testing user mapping" + userTypes[i].getObjClass(), e);
            }
        }

        // check group mappings. make sure they work
        GroupMappingConfig[] groupTypes = config.getGroupMappings();
        Collection offensiveGroupMappings = new ArrayList();
        boolean atLeastOneGroup = false;
        for (int i = 0; i < groupTypes.length; i++) {
            filter = "(&" +
              "(objectClass=" + groupTypes[i].getObjClass() + ")" +
              "(" + groupTypes[i].getNameAttrName() + "=*)" +
              ")";
            try {
                answer = context.search(config.getSearchBase(), filter, sc);
                while (answer.hasMore()) {
                    SearchResult sr = (SearchResult)answer.next();
                    // set the dn (unique id)
                    String dn = sr.getName() + "," + config.getSearchBase();
                    EntityHeader header = searchResultToHeader(sr, dn);
                    // if we successfully constructed a header, add it to result list
                    if (header != null) {
                        atLeastOneGroup = true;
                        break;
                    }
                }
                answer.close();
            } catch (NamingException e) {
                offensiveGroupMappings.add(groupTypes[i]);
                logger.log(Level.FINE, "error testing group mapping" + groupTypes[i].getObjClass(), e);
            }
        }
        try {
            context.close();
        } catch (NamingException e) {
            logger.log(Level.INFO, "error closing context", e);
        }

        // merge all errors in a special report
        StringBuffer error = new StringBuffer();

        if (userMappingsWithoutLoginAttribute.size() > 0) {
            if (error.length() > 0) error.append('\n');
            error.append("The following user mapping(s) do not define login attribute.");
            for (Iterator iterator = userMappingsWithoutLoginAttribute.iterator(); iterator.hasNext();) {
                UserMappingConfig userMappingConfig = (UserMappingConfig)iterator.next();
                error.append(" ").append(userMappingConfig.getObjClass());
            }
        }

        if (offensiveUserMappings.size() > 0 || offensiveGroupMappings.size() > 0) {
            if (error.length() > 0) error.append('\n');
            error.append("The following mappings caused errors:");
            for (Iterator iterator = offensiveUserMappings.iterator(); iterator.hasNext();) {
                UserMappingConfig userMappingConfig = (UserMappingConfig)iterator.next();
                error.append(" User mapping ").append(userMappingConfig.getObjClass());
            }
            for (Iterator iterator = offensiveGroupMappings.iterator(); iterator.hasNext();) {
                GroupMappingConfig groupMappingConfig = (GroupMappingConfig)iterator.next();
                error.append(" Group mapping ").append(groupMappingConfig.getObjClass());

            }
        }

        if (!atLeastOneUser) {
            if (error.length() > 0) error.append('\n');
            error.append("This configuration did not yeild any users");
        }

        if (!atLeastOneGroup && groupTypes.length > 0) {
            if (error.length() > 0) error.append('\n');
            error.append("This configuration did not yeild any group");
        }


        if (error.length() > 0) {
            logger.fine("Test produced following error(s): " + error.toString());
            throw new InvalidIdProviderCfgException(error.toString());
        } else
            logger.finest("this ldap config was tested successfully");
    }

    public void preSaveClientCert(User user, X509Certificate[] certChain) throws ClientCertManager.VetoSave {
        // ClientCertManagerImp's default rules are OK
    }

    public void setUserManager(LdapUserManager userManager) {
        this.userManager = userManager;
    }

    public void setGroupManager(LdapGroupManager groupManager) {
        this.groupManager = groupManager;
    }

    public void setClientCertManager(ClientCertManager clientCertManager) {
        this.clientCertManager = clientCertManager;
    }

    /**
     * Invoked by a BeanFactory after it has set all bean properties supplied
     *
     * @throws Exception in the event of misconfiguration (such
     *                   as failure to set an essential property) or if initialization fails.
     */
    public void afterPropertiesSet() throws Exception {
        if (clientCertManager == null) {
            throw new IllegalArgumentException("The Client Certificate Manager is required");
        }
        initializeFallbackMechanism();
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
    boolean isValidEntryBasedOnUserAccountControlAttribute(Attributes attibutes) {
        final long DISABLED_FLAG = 0x00000002;
        final long LOCKED_FLAG = 0x00000010;
        final long EXPIRED_FLAG = 0x00800000; // add a check for this flag in an attempt to fix 1466
        Attribute userAccountControlAttr = attibutes.get("userAccountControl");
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
                if ((value.longValue() & DISABLED_FLAG) == DISABLED_FLAG) {
                    logger.info("Disabled flag encountered. This account is disabled.");
                    return false;
                } else if ((value.longValue() & LOCKED_FLAG) == LOCKED_FLAG) {
                    logger.info("Locked flag encountered. This account is locked.");
                    return false;
                }  else if ((value.longValue() & EXPIRED_FLAG) == EXPIRED_FLAG) {
                    logger.info("Expired flag encountered. This account has expired.");
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
    boolean checkExpiredMSADAccount(Attributes attibutes) {
        Attribute accountExpiresAttr = attibutes.get("accountExpires");
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
                            logger.info("The account is expired according to accountExpiresAttr value");
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
     * @param sr
     * @return the EntityHeader for the dn or null if the object class is not supported or if the entity
     *         should be ignored (perhaps disabled)
     */
    EntityHeader searchResultToHeader(SearchResult sr, String dn) {
        Attributes atts = sr.getAttributes();
        // is it user or group ?
        Attribute objectclasses = atts.get("objectclass");
        // check if it's a user
        UserMappingConfig[] userTypes = config.getUserMappings();
        for (int i = 0; i < userTypes.length; i++) {
            String userclass = userTypes[i].getObjClass();
            if (attrContainsCaseIndependent(objectclasses, userclass)) {
                if (!isValidEntryBasedOnUserAccountControlAttribute(atts)) {
                    logger.fine("Account " + dn + " is disabled or blocked.");
                    return null;
                } else if (checkExpiredMSADAccount(atts)) {
                    logger.fine("Account " + dn + " is expired.");
                    return null;
                }
                Object tmp;
                String login = null;
                try {
                    tmp = extractOneAttributeValue(atts, userTypes[i].getLoginAttrName());
                } catch (NamingException e) {
                    logger.log(Level.WARNING, "cannot extract user login", e);
                    tmp = null;
                }
                if (tmp != null) {
                    login = tmp.toString();
                }
                // if description attribute present, use it
                String description = null;
                try {
                    tmp = extractOneAttributeValue(atts, DESCRIPTION_ATTRIBUTE_NAME);
                } catch (NamingException e) {
                    logger.log(Level.FINEST, "no description for this entry", e);
                    tmp = null;
                }
                if (tmp != null) {
                    description = tmp.toString();
                }
                if (login != null) {
                    return new EntityHeader(dn, EntityType.USER, login, description);
                } else {
                    return null;
                }
            }
        }
        // check that it's a group
        GroupMappingConfig[] groupTypes = config.getGroupMappings();
        for (int i = 0; i < groupTypes.length; i++)
            if (attrContainsCaseIndependent(objectclasses, groupTypes[i].getObjClass())) {
                String groupName = null;
                Attribute valuesWereLookingFor = atts.get(groupTypes[i].getNameAttrName());
                if (valuesWereLookingFor != null && valuesWereLookingFor.size() > 0) {
                    try {
                        groupName = valuesWereLookingFor.get(0).toString();
                    } catch (NamingException e) {
                        logger.warning("cannot extract name from this group");
                    }
                }
                if (groupName == null) groupName = dn;
                // if description attribute present, use it
                Object tmp;
                String description = null;
                try {
                    tmp = extractOneAttributeValue(atts, DESCRIPTION_ATTRIBUTE_NAME);
                } catch (NamingException e) {
                    logger.log(Level.FINEST, "no description for this entry", e);
                    tmp = null;
                }
                if (tmp != null) {
                    description = tmp.toString();
                }
                return new EntityHeader(dn, EntityType.GROUP, groupName, description);
            }
        return null;
    }

    static boolean attrContainsCaseIndependent(Attribute attr, String valueToLookFor) {
        return attr.contains(valueToLookFor) || attr.contains(valueToLookFor.toLowerCase());
    }

    static Object extractOneAttributeValue(Attributes attributes, String attrName) throws NamingException {
        Attribute valuesWereLookingFor = attributes.get(attrName);
        if (valuesWereLookingFor != null && valuesWereLookingFor.size() > 0) {
            return valuesWereLookingFor.get(0);
        }
        return null;
    }

    private LdapAttributeMapping getKerberosLdapAttributeMapping(){
        LdapAttributeMapping lmap = kerberosLdapAttributeMapping;

        if (lmap == null) {
            lmap = new LdapAttributeMapping();
            lmap.setAttributeName("userPrincipalName"); // TODO make this configurable in LdapIdentityProviderConfig
            kerberosLdapAttributeMapping = lmap;
        }

        return lmap;
    }

    public static final String DESCRIPTION_ATTRIBUTE_NAME = "description";

    private LdapIdentityProviderConfig config;
    private LdapAttributeMapping kerberosLdapAttributeMapping;
    private ClientCertManager clientCertManager;
    private LdapUserManager userManager;
    private LdapGroupManager groupManager;
    private CertificateAuthenticator certificateAuthenticator;
    private String lastSuccessfulLdapUrl;
    private long retryFailedConnectionTimeout;
    private final ReadWriteLock fallbackLock = new WriterPreferenceReadWriteLock();
    private String[] ldapUrls;
    private Long[] urlStatus;
    private final Logger logger = Logger.getLogger(getClass().getName());
    private ServerConfig serverConfig;
    private final BigInteger fileTimeConversionfactor = new BigInteger("116444736000000000");
    private final BigInteger fileTimeConversionfactor2 = new BigInteger("10000");
    private long maxSearchResultSize = -1;
}
