/**
 * Copyright (C) 2006-2007 Layer 7 Technologies Inc.
 */
package com.l7tech.server.url;

import com.l7tech.util.CausedIOException;
import com.l7tech.util.ResourceUtils;
import com.l7tech.common.io.WhirlycacheFactory;
import com.l7tech.server.identity.ldap.LdapUtils;
import com.sun.jndi.ldap.LdapURL;
import com.whirlycott.cache.Cache;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;

/**
 * PT is the payload type
 * @param <PT> payload type, i.e. the Java type of the deserialized attribute values 
 * @author alex
 */
public class LdapUrlObjectCache<PT> extends AbstractUrlObjectCache<LdapUrlObjectCache.LdapCacheEntry<PT>> {
    private final String bindDn;
    private final String bindPassword;
    private final int connectTimeout;
    private final int poolTimeout;
    private final boolean binary;

    private final Cache cache = WhirlycacheFactory.createCache(this.getClass().getSimpleName() + ".cache", 100, 1800, WhirlycacheFactory.POLICY_LRU);

    /**
     * Construct a new AbstractUrlObjectCache.
     *
     * @param maxCacheAge
     * @param defaultWaitMode
     * @param interestingAttributeIsBinary
     */
    public LdapUrlObjectCache(long maxCacheAge, WaitMode defaultWaitMode, String login, String pass, int connectTimeout, int poolTimeout, boolean interestingAttributeIsBinary) {
        super(maxCacheAge, defaultWaitMode);
        this.bindDn = login;
        this.bindPassword = pass;
        this.connectTimeout = connectTimeout;
        this.poolTimeout = poolTimeout;
        this.binary = interestingAttributeIsBinary;
    }

    private final Lock lock = new ReentrantLock();
    protected Lock getReadLock() { return lock; }
    protected Lock getWriteLock() { return lock; }

    protected AbstractCacheEntry<LdapCacheEntry<PT>> cacheGet(String url) {
        //noinspection unchecked
        return (AbstractCacheEntry<LdapCacheEntry<PT>>)cache.retrieve(url);
    }

    protected void cachePut(String url, AbstractCacheEntry abstractCacheEntry) {
        cache.store(url, abstractCacheEntry);
    }

    protected AbstractCacheEntry<LdapCacheEntry<PT>> cacheRemove(String url) {
        //noinspection unchecked
        return (AbstractCacheEntry<LdapCacheEntry<PT>>) cache.remove(url);
    }

    protected DatedUserObject<LdapCacheEntry<PT>> doGet(String urlStr, String lastModifiedStr, long lastSuccessfulPollStarted) throws IOException {
        try {
            LdapURL ldapUrl = new LdapURL(urlStr);
            String query = ldapUrl.getQuery();

            if (query != null) {
                Matcher matcher = LdapUtils.LDAP_URL_QUERY_PATTERN.matcher(query);
                if (!matcher.matches() || matcher.groupCount() == 0) {
                    throw new IOException("Malformed query string in LDAP URL: " + urlStr);
                }
                query = matcher.group(1);
            }

            DirContext context = null;
            try {
                context = LdapUtils.getLdapContext(urlStr, bindDn, bindPassword, connectTimeout, poolTimeout);
                Attributes attrs = context.getAttributes("");
                return new DatedUserObject<LdapCacheEntry<PT>>(new LdapCacheEntry<PT>(attrs, query, binary), Long.toString(System.currentTimeMillis()));
            } finally {
                ResourceUtils.closeQuietly(context);
            }
        } catch (NamingException e) {
            throw new CausedIOException("Coudln't establish LDAP context", e);
        }
    }

    public static class LdapCacheEntry<PT> {
        private final String interestingAttributeName;
        private final List<Object> interestingAttributeValues;
        private final Attributes attributes;
        private PT payload;

        private LdapCacheEntry(Attributes attributes, String attrName, boolean binary) throws NamingException {
            this.attributes = attributes;
            this.interestingAttributeName = attrName;
            if (attrName == null) {
                interestingAttributeValues = Collections.emptyList();
            } else {
                String name = interestingAttributeName + (binary ? ";binary" : ""); 
                Attribute attr = attributes.get(name);
                List<Object> vals = new ArrayList<Object>();
                for (NamingEnumeration<?> ne = attr.getAll(); ne.hasMore();) {
                    Object o = ne.next();
                    vals.add(o);
                }
                this.interestingAttributeValues = Collections.unmodifiableList(vals);
            }
        }

        public String getInterestingAttributeName() {
            return interestingAttributeName;
        }

        public List<Object> getInterestingAttributeValues() {
            return interestingAttributeValues;
        }

        public Attributes getAttributes() {
            return attributes;
        }
        
        public synchronized PT getPayload() {
            return payload;
        }

        public synchronized void setPayload(PT payload) {
            this.payload = payload;
        }
    }

}

