package com.l7tech.server.identity.ldap;

import com.l7tech.identity.ldap.LdapUrlBasedIdentityProviderConfig;
import com.l7tech.server.ServerConfigParams;
import com.l7tech.util.ConfigFactory;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.ResourceUtils;
import com.sun.jndi.ldap.LdapURL;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.naming.*;
import javax.naming.directory.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * @author alex
 */
public final class LdapUtils {

    //- PUBLIC

    /**
     * Finds the attribute name within the LDAP URL query string.
     *
     * The query string must start with a question mark, then we capture one or more subsequent non-question mark
     * characters before the next question mark or the end of the string, whichever comes first.
     *
     * @see <a href="http://www.ietf.org/rfc/rfc2255.txt">LDAP URL syntax</a>
     */
    public static final Pattern LDAP_URL_QUERY_PATTERN = Pattern.compile("^\\?([^\\?]+).*$");
    public static final boolean LDAP_USES_UNSYNC_NAMING = ConfigFactory.getBooleanProperty( "com.l7tech.server.ldap.unsyncNaming", false );
    public static final boolean LDAP_PARSE_NAMES = ConfigFactory.getBooleanProperty( "com.l7tech.server.ldap.parseNames", false );
    public static final String LDAP_ENV_PREFIX = "com.l7tech.server.ldap.env.";
    public static final Map<String,String> LDAP_ENV_OVERRIDES;

    // Environment properties
    public static final String ENV_PROP_LDAP_VERSION = "java.naming.ldap.version";
    public static final String ENV_PROP_LDAP_CONNECT_POOL = "com.sun.jndi.ldap.connect.pool";
    public static final String ENV_PROP_LDAP_CONNECT_TIMEOUT = "com.sun.jndi.ldap.connect.timeout";
    public static final String ENV_PROP_LDAP_READ_TIMEOUT = "com.sun.jndi.ldap.read.timeout";
    public static final String ENV_PROP_LDAP_FACTORY_SOCKET = "java.naming.ldap.factory.socket";

    public static final String ENV_VALUE_INITIAL_CONTEXT_FACTORY = "com.sun.jndi.ldap.LdapCtxFactory";
    public static final String ENV_VALUE_LDAP_VERSION = "3";
    public static final String ENV_VALUE_REFERRAL = "follow";
    public static final String ENV_VALUE_SECURITY_PROTOCOL = "ssl";

    private static final int LDAP_MAX_RANGE_ITERATIONS = ConfigFactory.getIntProperty("com.l7tech.server.ldap.maxRangeIterations", 1000);
    private static final String RANGE_SPLIT_STRING = ";range=";

    private LdapUtils() { }

    /**
     * Convert the given name text to a Name.
     *
     * @param name The name as text.
     * @return The name
     * @throws InvalidNameException If the name is not valid.
     */
    public static Name name( final String name ) throws InvalidNameException {
        if ( LDAP_PARSE_NAMES ) {
            return new CompositeName( name );
        } else {
            return new CompositeName().add( name );
        }
    }

    static boolean attrContainsCaseIndependent(Attribute attr, String valueToLookFor) {
        if (valueToLookFor == null || "".equals(valueToLookFor))
            return true;

        final boolean shouldBeCaseInsensitiveValue =
                ConfigFactory.getBooleanProperty( ServerConfigParams.PARAM_LDAP_COMPARISON_CASE_INSENSITIVE, true );

        for (int i = 0; i < attr.size(); i++) {
            try {
                Object attrVal = attr.get(i);
                if (attrVal == null) continue;
                if (shouldBeCaseInsensitiveValue) {
                    if (valueToLookFor.equalsIgnoreCase(attrVal.toString()))
                        return true;
                } else {
                    if (valueToLookFor.equals(attrVal.toString()))
                        return true;
                }
            } catch (NamingException e) {
                // ignore this (non)value
            }
        }

        return false;
    }

    static Object extractOneAttributeValue(Attributes attributes, String attrName) throws NamingException {
        Attribute valuesWereLookingFor = attributes.get(attrName);
        if (valuesWereLookingFor != null && valuesWereLookingFor.size() > 0) {
            return valuesWereLookingFor.get(0);
        }
        return null;
    }

    /**
     * Escape characters for use in an LDAP search filter
     *
     * @param value The text to escape
     * @return The escaped text
     */
    public static String filterEscape( final String value ) {
        String escaped = value;

        if ( value != null ) {
            StringBuilder builder = new StringBuilder( (int)(value.length() * 1.3) );
            for ( char c : value.toCharArray() ) {
                switch ( c ) {
                    case 0:
                        builder.append( "\\00" );
                        break;
                    case '(':
                        builder.append( "\\28" );
                        break;
                    case ')':
                        builder.append( "\\29" );
                        break;
                    case '*':
                        builder.append( "\\2a" );
                        break;
                    case '\\':
                        builder.append( "\\5c" );
                        break;
                    default:
                        builder.append( c );
                        break;
                }
            }

            escaped = builder.toString();
        }

        return escaped;
    }

    /**
     * Escape characters for use in an LDAP search filter
     *
     * <p>This escapes most special LDAP characters, but leaves "*".</p>
     *
     * @param value The text to escape
     * @return The escaped text
     */
    public static String filterMatchEscape( final String value ) {
        String escaped = value;

        if ( value != null ) {
            StringBuilder builder = new StringBuilder( (int)(value.length() * 1.3) );
            for ( char c : value.toCharArray() ) {
                switch ( c ) {
                    case 0:
                        builder.append( "\\00" );
                        break;
                    case '(':
                        builder.append( "\\28" );
                        break;
                    case ')':
                        builder.append( "\\29" );
                        break;
                    case '\\':
                        builder.append( "\\5c" );
                        break;
                    default:
                        builder.append( c );
                        break;
                }
            }

            escaped = builder.toString();
        }

        return escaped;
    }

    public static Hashtable<? super String,? super String> newEnvironment() {
        if ( LDAP_USES_UNSYNC_NAMING ) {
            return new UnsynchronizedNamingProperties();
        } else {
            return new Hashtable<String,String>();
        }
    }

    public static void lock( final Hashtable environment ) {
        if ( environment instanceof UnsynchronizedNamingProperties ) {
            UnsynchronizedNamingProperties unsynchronizedNamingProperties = (UnsynchronizedNamingProperties) environment;
            unsynchronizedNamingProperties.lock();
        }
    }

    /**
     * Get a DirContext created with the given parameters.
     *
     * <p>For LDAPS connections, the default keystore will be used for client certificate.</p>
     *
     * @param url The LDAP(S) url
     * @param login The login to use (may be null)
     * @param pass  The password to use (may be null)
     * @param connectTimeout The TCP connection timeout
     * @param readTimeout The TCP read timeout
     * @return The context
     * @throws NamingException If an error occurs
     */
    public static DirContext getLdapContext( final String url,
                                             final String login,
                                             final String pass,
                                             final long connectTimeout,
                                             final long readTimeout ) throws NamingException {
        return getLdapContext(url, true, null, null, login, pass, connectTimeout, readTimeout, null, true);
    }

    /**
     * Get a DirContext created with the given parameters.
     *
     * <p>For LDAPS connections, the default keystore will be used for client certificate.</p>
     *
     * @param url The LDAP(S) url
     * @param useClientAuth True to enable client authentication
     * @param keystoreId The keystore identifier (null for default)
     * @param keyAlias The key alias (null for default)
     * @param login The login to use (may be null)
     * @param pass  The password to use (may be null)
     * @param connectTimeout The TCP connection timeout
     * @param readTimeout The TCP read timeout
     * @param referral whether to ignore, follow, or throw on referrals, or null to use the previous default behavior.
     * @param useConnectionPooling True to use connection pool
     * @return The context
     * @throws NamingException If an error occurs
     */
    public static DirContext getLdapContext( final String url,
                                             final boolean useClientAuth,
                                             @Nullable final Long keystoreId,
                                             @Nullable final String keyAlias,
                                             final String login,
                                             final String pass,
                                             final long connectTimeout,
                                             final long readTimeout,
                                             @Nullable String referral,
                                             final boolean useConnectionPooling ) throws NamingException {
        final ClassLoader originalContextClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader( LdapSslCustomizerSupport.getSSLSocketFactoryClassLoader() );

            LdapURL lurl = new LdapURL(url);
            Hashtable<? super String,? super String> env = newEnvironment();
            env.put(ENV_PROP_LDAP_VERSION, ENV_VALUE_LDAP_VERSION);
            env.put(Context.INITIAL_CONTEXT_FACTORY, ENV_VALUE_INITIAL_CONTEXT_FACTORY);
            env.put(Context.PROVIDER_URL, url);
            if ( useConnectionPooling ) {
                env.put(ENV_PROP_LDAP_CONNECT_POOL, "true");
            }
            env.put(ENV_PROP_LDAP_CONNECT_TIMEOUT, Long.toString(connectTimeout));
            env.put(ENV_PROP_LDAP_READ_TIMEOUT, Long.toString(readTimeout));
            if (null == referral) referral = ConfigFactory.getProperty(ServerConfigParams.PARAM_LDAP_REFERRAL, LdapUtils.ENV_VALUE_REFERRAL);
            env.put(Context.REFERRAL, referral );

            if (lurl.useSsl()) {
                env.put(ENV_PROP_LDAP_FACTORY_SOCKET, LdapSslCustomizerSupport.getSSLSocketFactoryClassname( useClientAuth, keystoreId, keyAlias ));
                env.put(Context.SECURITY_PROTOCOL, ENV_VALUE_SECURITY_PROTOCOL);
            }

            if (login != null && login.length() > 0) {
                env.put(Context.SECURITY_AUTHENTICATION, "simple");
                env.put(Context.SECURITY_PRINCIPAL, login);
                env.put(Context.SECURITY_CREDENTIALS, pass);
            }
            env.putAll(LDAP_ENV_OVERRIDES);
            lock( env );
            return new InitialDirContext(env);
        } finally {
            Thread.currentThread().setContextClassLoader( originalContextClassLoader );
        }
    }

    /**
     * Attempt simple password-based authentication by attempting to bind the specified DN using the specified LDAP
     * settings.
     *
     * @param urlProvider        Object that keeps track of which LDAP(S) URL was last reported to work.  Required.
     * @param providerConfig     Provider of configuration information such as all LDAP(S) URLs and any TLS client cert settings.  Required.
     * @param ldapRuntimeConfig  LDAP runtime properties to use for connection.  Required.
     * @param logger             Logger to use for logging authentication failures and successes.  Required.
     * @param dn                 full DN to attempt to bind.  Required.
     * @param passwd             password to use for bind attempt.  Authentication will automatically fail if this is null or empty.
     * @return true if we were able to successfully bind the specified DN using the specified password.
     */
    public static boolean authenticateBasic(LdapUrlProvider urlProvider, LdapUrlBasedIdentityProviderConfig providerConfig, LdapRuntimeConfig ldapRuntimeConfig, Logger logger, String dn, String passwd) {
        if (passwd == null || passwd.length() < 1) {
            logger.info("User: " + dn + " refused authentication because empty password provided.");
            return false;
        }
        String ldapurl = urlProvider.getLastWorkingLdapUrl();
        if (ldapurl == null) {
            ldapurl = urlProvider.markCurrentUrlFailureAndGetFirstAvailableOne(null);
        }
        while (ldapurl != null) {
            DirContext userCtx = null;
            try {
                boolean clientAuth = providerConfig.isClientAuthEnabled();
                Long keystoreId = providerConfig.getKeystoreId();
                String keyAlias = providerConfig.getKeyAlias();
                userCtx = getLdapContext(ldapurl, clientAuth, keystoreId, keyAlias, dn, passwd, ldapRuntimeConfig.getLdapConnectionTimeout(), ldapRuntimeConfig.getLdapReadTimeout(), null, false);
                logger.info("User: " + dn + " authenticated successfully in provider " + providerConfig.getName());
                return true;
            } catch (CommunicationException e) {
                logger.log(Level.INFO, "Could not establish context using LDAP URL " + ldapurl, e);
                ldapurl = urlProvider.markCurrentUrlFailureAndGetFirstAvailableOne(ldapurl);
            } catch (AuthenticationException e) {
                // when you get bad credentials
                logger.info("User failed to authenticate: " + dn + " in provider " + providerConfig.getName());
                return false;
            } catch (NamingException e) {
                logger.log(Level.WARNING, "General naming failure for user: " + dn + " in provider " + providerConfig.getName(), e);
                return false;
            } finally {
                ResourceUtils.closeQuietly(userCtx);
            }
        }
        logger.warning("Could not establish context on any of the ldap urls.");
        return false;
    }

    /**
     * Ignore or rethrow the specified exception, depending on whether it is a PartialResultException and
     * whether we are configured to ignore such exceptions.
     *
     * @param e a NamingException to examine.  Required.
     * @throws NamingException rethrown e unless the e exception is a {@link PartialResultException} and {@link ServerConfigParams#PARAM_LDAP_IGNORE_PARTIAL_RESULTS} is true.
     */
    public static void handlePartialResultException(@NotNull NamingException e) throws NamingException {
        if (e instanceof PartialResultException && isIgnorePartialResultException()) {
            logger.fine("Ignoring PartialResultException (ignored referral?)");
            return;
        }
        throw e;
    }

    public static boolean isIgnorePartialResultException() {
        return ConfigFactory.getBooleanProperty(ServerConfigParams.PARAM_LDAP_IGNORE_PARTIAL_RESULTS, true);
    }

    //- PACKAGE

    static final String LDAP_ATTR_USER_ACCOUNT_CONTROL = "userAccountControl";
    static final String LDAP_ATTR_ACCOUNT_EXPIRES = "accountExpires";
    static final String LDAP_ATTR_USER_CERTIFICATE = "userCertificate;binary";

    /**
     * Listener for results from an LdapTemplate
     */
    static class LdapListener {
        /**
         * Notification of a SearchResults enumeration
         *
         * <p>This implementation calls {@link #searchResult} for each item.</p>
         *
         * @param results The results to process
         * @throws NamingException If an error occurs
         */
        void searchResults( final NamingEnumeration<SearchResult> results ) throws NamingException {
            try {
                while ( results.hasMore() ) {
                    if (!searchResult( results.next() )) break;
                }
            } catch (PartialResultException e) {
                handlePartialResultException(e);
            }
        }

        /**
         * Notification of a SearchResult.
         *
         * <p>This implementation calls {@link #attributes} and returns true.</p>
         *
         * @param sr The Search result
         * @return true to recieve more results
         * @throws NamingException If an error occurs
         */
        boolean searchResult( final SearchResult sr ) throws NamingException {
            attributes( sr.getNameInNamespace(), sr.getAttributes() );
            return true;
        }

        /**
         * Notification of an attributes.
         *
         * <p>This implementation does nothing.</p>
         *
         * @param dn The DN associated with the attributes.
         * @param attributes The attributes
         * @throws NamingException If an error occurs
         */
        void attributes( final String dn, final Attributes attributes ) throws NamingException {
        }
    }

    /**
     * Template to use with searching and attributes access.
     *
     * <p>This handles the common functionality such as resource access and
     * disposal and configuration of the search controls for returned
     * attributes and count limit.</p> 
     */
    abstract static class LdapTemplate {
        private final String searchBase;
        private final String[] returningAttributes;

        /**
         * Create a new tempalte with the given default options.
         *
         * @param searchBase The default search base
         * @param returningAttributes The default returning attributes
         */
        LdapTemplate( final String searchBase, final String[] returningAttributes ) {
            this.searchBase = searchBase;
            this.returningAttributes = returningAttributes;                        
        }

        /**
         * Search the default DirContext with the supplied filter and options.
         *
         * @param filter The filter to use
         * @param countLimit The count limit (0 for no limit)
         * @param returningAttributes The attributes to return (null for default)
         * @param listener The listener for the searchResults callback (null for no callback)
         * @throws NamingException If an error occurs
         */
        void search( final String filter,
                     final long countLimit,
                     final Set<String> returningAttributes,
                     final LdapListener listener ) throws NamingException {
            search( (String) null, filter, countLimit, returningAttributes, listener );
        }

        /**
         * Search the default DirContext with the supplied filter and options.
         *
         * @param searchBase The search base to use (null for default)
         * @param filter The filter to use
         * @param countLimit The count limit (0 for no limit)
         * @param returningAttributes The attributes to return (null for default)
         * @param listener The listener for the searchResults callback (null for no callback)
         * @throws NamingException If an error occurs
         */
        void search( final String searchBase,
                     final String filter,
                     final long countLimit,
                     final Set<String> returningAttributes,
                     final LdapListener listener ) throws NamingException {
            DirContext context = null;

            try {
                context = getDirContext();
                search( context, searchBase, filter, countLimit, returningAttributes, listener );
            } finally {
                ResourceUtils.closeQuietly( context );
            }
        }

        /**
         * Search the given DirContext with the supplied filter and options.
         *
         * @param context The context to search
         * @param filter The filter to use
         * @param countLimit The count limit (0 for no limit)
         * @param returningAttributes The attributes to return (null for default)
         * @param listener The listener for the searchResults callback (null for no callback)
         * @throws NamingException If an error occurs
         */
        void search( final DirContext context,
                     final String filter,
                     final long countLimit,
                     final Set<String> returningAttributes,
                     final LdapListener listener ) throws NamingException {
            search( context, null, filter, countLimit, returningAttributes, listener );
        }

        /**
         * Search the given DirContext with the supplied filter and options.
         *
         * @param context The context to search
         * @param searchBase The search base to use (null for default)
         * @param filter The filter to use
         * @param countLimit The count limit (0 for no limit)
         * @param returningAttributes The attributes to return (null for default)
         * @param listener The listener for the searchResults callback (null for no callback)
         * @throws NamingException If an error occurs
         */
        void search( final DirContext context,
                     final String searchBase,
                     final String filter,
                     final long countLimit,
                     final Set<String> returningAttributes,
                     final LdapListener listener ) throws NamingException {
            String base = searchBase;
            if (base == null) {
                base = this.searchBase;
            }

            SearchControls sc = new SearchControls();
            sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
            if (returningAttributes==null) {
                sc.setReturningAttributes(this.returningAttributes);
            } else {
                sc.setReturningAttributes(returningAttributes.toArray( new String[returningAttributes.size()] ));
            }
            sc.setCountLimit( countLimit );

            NamingEnumeration<SearchResult> answer = null;
            try {
                answer = context.search( name(base), filter, sc);
                if ( listener != null ) {
                    listener.searchResults( answer );
                }
            } finally {
                ResourceUtils.closeQuietly(answer);
            }
        }

        /**
         * Access attributes for the given DN using the default DirContext.
         *
         * @param dn The DN of the attributes
         * @param listener The listener for the attributes callback.
         * @throws NamingException If an error occurs.
         */
        void attributes( final String dn, final LdapListener listener ) throws NamingException {
            DirContext context = null;

            try {
                context = getDirContext();
                attributes( context, dn, listener );
            } finally {
                ResourceUtils.closeQuietly( context );
            }
        }

        /**
         * Access attributes for the given DN.
         *
         * @param context The context to use
         * @param dn The DN of the attributes
         * @param listener The listener for the attributes callback.
         * @throws NamingException If an error occurs.
         */
        void attributes( final DirContext context, final String dn, final LdapListener listener ) throws NamingException {
            try {
                Attributes responseAttributes =  context.getAttributes(name(dn), returningAttributes);
                // memberRange is like "member;range=100-200" if LDAP responded with a range - null otherwise
                String memberRange = rangedResponseOfAttribute("member", responseAttributes);
                if ( memberRange != null ) {
                    NamingEnumeration<String> rangeValues = (NamingEnumeration<String>) responseAttributes.get(memberRange).getAll();
                    String attributeValues[] = obtainAllRangedResponsesOfAttribute(memberRange,rangeValues,context,dn);
                    Attribute member = responseAttributes.get("member");
                    for ( String s : attributeValues ) {
                        member.add(s);
                    }
                }
                listener.attributes( dn, responseAttributes );
            } finally {
                ResourceUtils.closeQuietly( context );
            }
        }

        /**
         * Searches input attributes for an LDAP attribute range response for the attribute
         * with name attributeName. For example, if the attribute is "member" the ranged
         * response will look like "member;range=100-200"
         *
         * If LDAP responded with a range of the potential response this returns
         * the name of the ranged attribute, ie "member;range=100-200"
         *
         * The key point is that LDAP treats "member;range=100-200" as if it were an actual
         * attribute.
         *
         * @param attributeName  attribute name to check and see if there is a ranged response ("member")
         * @param attributes     attributes previously obtained from LDAP.
         * @return               attribute name of the actual ranged response ("member;range=0-200")
         * @throws NamingException
         */
        String rangedResponseOfAttribute(String attributeName,Attributes attributes) throws NamingException {

            NamingEnumeration<String> responseAttributeNames = attributes.getIDs();

            while ( responseAttributeNames.hasMore() ) {
                String responseAttributeName = responseAttributeNames.next();
                String[] test = responseAttributeName.split(RANGE_SPLIT_STRING);
                if ( test.length != 1 && attributeName.equals(test[0]) ) {
                    return responseAttributeName;
                }
            }
            return null;
        }

        /**
         * Uses the ranged attribute name string ("member;range=0-1000") to make multiple requests
         * to LDAP for all of the ranges of the member attribute associated with a particular dn.
         * Assembles all the values into a single array of String.
         *
         * @param rangedResponseAttributeName like "member;range=0-1000"
         * @param firstRange this should be the Attribute Values that correspond to the rangedResponseAttributeName
         *                   LDAP returned these with the response that signaled it was going to respond in
         *                   ranges.  This is to avoid asking LDAP for the same information multiple times.
         * @param context    LDAP DirContext - for queries
         * @param dn         the name of the object we're getting values of.
         * @return           an array of all the String values from all of the ranges returned from LDAP
         * @throws NamingException
         */
        String[] obtainAllRangedResponsesOfAttribute(String rangedResponseAttributeName,
                                                     NamingEnumeration<String> firstRange,
                                                     final DirContext context,
                                                     final String dn) throws NamingException {

            String[] responseId = rangedResponseAttributeName.split(RANGE_SPLIT_STRING);
            String[] rangeStrings = responseId[1].split("-");

            String baseAttributeName = responseId[0];
            int resultBeginRange = Integer.parseInt(rangeStrings[0]);
            int resultEndRange = Integer.parseInt(rangeStrings[1]);

            int pageSize = (resultEndRange-resultBeginRange)+1;

            ArrayList<String> results = new ArrayList<String>();

            // copy in the initial response
            while ( firstRange.hasMore() ) {
                results.add(firstRange.next());
            }

            /* Repeat asking LDAP for more rang responses of page size until
             * a range end value is '*' -- ie  'member;range=3500-*' indicating
             * the last page.  Protect ourselves by iterating up to a configurable
               maximum.
             */
            for ( int count = 0; count < LDAP_MAX_RANGE_ITERATIONS; count++ ) {
                resultBeginRange = resultEndRange + 1;
                resultEndRange += pageSize;
                String rangedAttributeName = String.format("%s;range=%d-%d",baseAttributeName,resultBeginRange,resultEndRange);
                String[] returningAttributes = { rangedAttributeName };
                Attributes attributes = context.getAttributes(name(dn),returningAttributes);

                // There should only be one attribute returned
                String nextResponseName =  attributes.getIDs().next();
                NamingEnumeration<String> nextResponse = (NamingEnumeration<String>) attributes.get(nextResponseName).getAll();

                // add additional responses for each range
                while ( nextResponse.hasMore() ) {
                    results.add(nextResponse.next());
                }

                responseId = nextResponseName.split(RANGE_SPLIT_STRING);
                rangeStrings = responseId[1].split("-");
                if (rangeStrings[1].equals("*")) {
                    // we're done!
                    return results.toArray(new String[results.size()]);
                }
                resultEndRange = Integer.parseInt(rangeStrings[1]);
            }

            logger.warning("Failed to obtain all values of attribute '"+ baseAttributeName + "' from object '" + dn + "'. Giving up after obtaining " + LDAP_MAX_RANGE_ITERATIONS + " response blocks from LDAP.");
            throw new NamingException("Exceeded configured max number LDAP ranged attribute requests");
        }

        /**
         * Get the DirContext to use.
         *
         * @return The DirContext
         * @throws NamingException If an error occurs.
         */
        abstract DirContext getDirContext() throws NamingException;
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger( LdapUtils.class.getName() );

    static {
        LDAP_ENV_OVERRIDES = readEnvironmentOverrides();
    }

    /**
     * Load extra/replacement environment properties.
     *
     * If this is used we should consider adding UI to LDAP providers since
     * the settings should really be per provider.
     */
    private static Map<String,String> readEnvironmentOverrides() {
        final Map<String,String> overrides = new HashMap<String,String>();

        try {
            final Properties systemProperties = System.getProperties();
            for ( final String name : systemProperties.stringPropertyNames() ) {
                if ( name.startsWith( LDAP_ENV_PREFIX ) ) {
                    overrides.put(
                            name.substring( LDAP_ENV_PREFIX.length() ),
                            systemProperties.getProperty( name ) );
                }
            }
        } catch ( SecurityException e ) {
            logger.log( Level.WARNING, 
                    "Permission denied when loading LDAP environment overrides.",
                    ExceptionUtils.getDebugException(e) );
        }

        if ( !overrides.isEmpty() ) {
            logger.config( "Loaded LDAP environment overrides : " + overrides );
        }

        return Collections.unmodifiableMap( overrides );
    }
}
