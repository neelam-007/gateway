/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.server.identity.ldap;

import com.sun.jndi.ldap.LdapURL;
import com.l7tech.server.transport.http.SslClientSocketFactory;

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
        return attr.contains(valueToLookFor) || attr.contains(valueToLookFor.toLowerCase());
    }

    static Object extractOneAttributeValue(Attributes attributes, String attrName) throws NamingException {
        Attribute valuesWereLookingFor = attributes.get(attrName);
        if (valuesWereLookingFor != null && valuesWereLookingFor.size() > 0) {
            return valuesWereLookingFor.get(0);
        }
        return null;
    }

    public static DirContext getLdapContext(String url) throws NamingException {
        return getLdapContext(url, null, null, 30000, 30000);
    }

    public static DirContext getLdapContext(String url, String login, String pass, int connectTimeout, int poolTimeout) throws NamingException {
        return getLdapContext(url, login, pass, connectTimeout, poolTimeout, connectTimeout);
    }

    public static DirContext getLdapContext(String url, String login, String pass, int connectTimeout, int poolTimeout, int readTimeout) throws NamingException {
        LdapURL lurl = new LdapURL(url);
        UnsynchronizedNamingProperties env = new UnsynchronizedNamingProperties();
        env.put("java.naming.ldap.version", "3");
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, url);
        env.put("com.sun.jndi.ldap.connect.pool", "true");
        env.put("com.sun.jndi.ldap.connect.timeout", Integer.toString(connectTimeout));
        env.put("com.sun.jndi.ldap.read.timeout", Integer.toString(readTimeout));
        env.put("com.sun.jndi.ldap.connect.pool.timeout", Integer.toString(poolTimeout));
        env.put( Context.REFERRAL, "follow" );

        if (lurl.useSsl()) {
            env.put("java.naming.ldap.factory.socket", SslClientSocketFactory.class.getName());
            env.put(Context.SECURITY_PROTOCOL, "ssl");
        }

        if (login != null && login.length() > 0) {
            env.put(Context.SECURITY_AUTHENTICATION, "simple");
            env.put(Context.SECURITY_PRINCIPAL, login);
            env.put(Context.SECURITY_CREDENTIALS, pass);
        }
        env.lock();
        return new InitialDirContext(env);
    }
}
