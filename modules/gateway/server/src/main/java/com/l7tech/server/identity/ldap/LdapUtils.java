/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.server.identity.ldap;

import com.sun.jndi.ldap.LdapURL;
import com.l7tech.server.ServerConfig;

import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.DirContext;
import javax.naming.NamingException;
import javax.naming.Context;
import java.util.regex.Pattern;

/**
 * @author alex
 */
public final class LdapUtils {
    /**
     * Finds the attribute name within the LDAP URL query string.
     *
     * The query string must start with a question mark, then we capture one or more subsequent non-question mark
     * characters before the next question mark or the end of the string, whichever comes first.
     *
     * @see <a href="http://www.ietf.org/rfc/rfc2255.txt">LDAP URL syntax</a>
     */
    public static final Pattern LDAP_URL_QUERY_PATTERN = Pattern.compile("^\\?([^\\?]+).*$");
    public static final String LDAP_ATTRIBUTE_BINARY_SUFFIX = ";binary";

    private LdapUtils() { }

    static boolean attrContainsCaseIndependent(Attribute attr, String valueToLookFor) {
        if (valueToLookFor == null || "".equals(valueToLookFor))
            return true;

        boolean shouldBeCaseInsensitiveValue = Boolean.parseBoolean(
            ServerConfig.getInstance().getPropertyCached(ServerConfig.PARAM_LDAP_COMPARISON_CASE_INSENSITIVE));

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
            UnsynchronizedNamingProperties env = new UnsynchronizedNamingProperties();
            env.put("java.naming.ldap.version", "3");
            env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
            env.put(Context.PROVIDER_URL, url);
            if ( useConnectionPooling ) {
                env.put("com.sun.jndi.ldap.connect.pool", "true");
            }
            env.put("com.sun.jndi.ldap.connect.timeout", Long.toString(connectTimeout));
            env.put("com.sun.jndi.ldap.read.timeout", Long.toString(readTimeout));
            env.put( Context.REFERRAL, "follow" );

            if (lurl.useSsl()) {
                env.put("java.naming.ldap.factory.socket", LdapSslCustomizerSupport.getSSLSocketFactoryClassname( useClientAuth, keystoreId, keyAlias ));
                env.put(Context.SECURITY_PROTOCOL, "ssl");
            }

            if (login != null && login.length() > 0) {
                env.put(Context.SECURITY_AUTHENTICATION, "simple");
                env.put(Context.SECURITY_PRINCIPAL, login);
                env.put(Context.SECURITY_CREDENTIALS, pass);
            }
            env.lock();
            return new InitialDirContext(env);
        } finally {
            Thread.currentThread().setContextClassLoader( originalContextClassLoader );
        }
    }
}
