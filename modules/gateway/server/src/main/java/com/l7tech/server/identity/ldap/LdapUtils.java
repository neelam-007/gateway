package com.l7tech.server.identity.ldap;

import com.l7tech.server.ServerConfigParams;
import com.l7tech.util.ConfigFactory;
import com.l7tech.util.ExceptionUtils;
import com.sun.jndi.ldap.LdapURL;
import com.l7tech.util.ResourceUtils;

import javax.naming.CompositeName;
import javax.naming.InvalidNameException;
import javax.naming.Name;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchResult;
import javax.naming.directory.SearchControls;
import javax.naming.NamingException;
import javax.naming.Context;
import javax.naming.NamingEnumeration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.Hashtable;
import java.util.Set;

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
        return getLdapContext(url, true, null, null, login, pass, connectTimeout, readTimeout, true);
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
     * @param useConnectionPooling True to use connection pool
     * @return The context
     * @throws NamingException If an error occurs
     */
    public static DirContext getLdapContext( final String url,
                                             final boolean useClientAuth,
                                             final Long keystoreId,
                                             final String keyAlias,
                                             final String login,
                                             final String pass,
                                             final long connectTimeout,
                                             final long readTimeout,
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
            env.put(Context.REFERRAL, ENV_VALUE_REFERRAL);

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
            while ( results.hasMore() ) {
                if (!searchResult( results.next() )) break;
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
                listener.attributes( dn, context.getAttributes( name(dn), returningAttributes ));
            } finally {
                ResourceUtils.closeQuietly( context );
            }
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
