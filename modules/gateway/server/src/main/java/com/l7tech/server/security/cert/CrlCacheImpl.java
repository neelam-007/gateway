/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.server.security.cert;

import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.gateway.common.audit.SystemMessages;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.server.url.AbstractUrlObjectCache;
import com.l7tech.server.url.HttpObjectCache;
import com.l7tech.server.url.LdapUrlObjectCache;
import com.l7tech.util.*;
import com.l7tech.common.io.WhirlycacheFactory;
import com.l7tech.common.io.CertUtils;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.identity.ldap.LdapIdentityProvider;
import com.l7tech.server.util.HttpClientFactory;
import com.l7tech.server.util.ServerCertUtils;
import com.whirlycott.cache.Cache;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.security.cert.CRLException;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * A read-through cache of X.509 CRLs that knows how to retrieve them from http(s) and ldap(s) URLs
 * @author alex
 */
public class CrlCacheImpl implements CrlCache {

    private static final int ONE_HOUR = TimeUnit.HOURS.getMultiplier();
    private static final int RETRIEVAL_OFFSET = TimeUnit.MINUTES.getMultiplier(); // try to fetch new CRL one minute before expiry

    private static final String PROP_EXPIRY_DEFAULT = "pkixCRL.expiry";
    private static final String PROP_EXPIRY_MIN = "pkixCRL.minExpiryAge";
    private static final String PROP_EXPIRY_MAX = "pkixCRL.maxExpiryAge";

    /** Cache&lt;String, CrlCacheEntry&gt; where String is a CRL URL */
    private final Cache crlCache;
    /** Cache&lt;X509Certificate, CertCacheEntry&gt; */
    private final Cache certCache;

    private final LdapUrlObjectCache<X509CRL> ldapUrlObjectCache;
    private final HttpObjectCache<X509CRL> httpObjectCache;
    private final ServerConfig serverConfig;
    private static final long MAX_CACHE_AGE_VALUE = 30000;
    private static final int DEFAULT_MAX_HTTP_CACHE_OBJECTS_SIZE = 1000;
    private static final String MAX_HTTP_CACHE_OBJECTS_PROP = "com.l7tech.server.security.cert.httpCacheObjectsSize";

    public CrlCacheImpl(HttpClientFactory httpClientFactory, ServerConfig serverConfig) throws Exception {
        this.crlCache = WhirlycacheFactory.createCache(CrlCache.class.getSimpleName() + ".crlCache", 100, 1800, WhirlycacheFactory.POLICY_LRU);
        this.certCache = WhirlycacheFactory.createCache(CrlCache.class.getSimpleName() + ".certCache", 1000, 1800, WhirlycacheFactory.POLICY_LRU);

        // TODO support configuration of login, password and LDAP timeouts
        this.serverConfig = serverConfig;

        long connectTimeout = serverConfig.getTimeUnitPropertyCached(ServerConfig.PARAM_LDAP_CONNECTION_TIMEOUT, LdapIdentityProvider.DEFAULT_LDAP_CONNECTION_TIMEOUT, MAX_CACHE_AGE_VALUE);
        long readTimeout = serverConfig.getTimeUnitPropertyCached(ServerConfig.PARAM_LDAP_READ_TIMEOUT, LdapIdentityProvider.DEFAULT_LDAP_READ_TIMEOUT, MAX_CACHE_AGE_VALUE);
        ldapUrlObjectCache = new LdapUrlObjectCache<X509CRL>(300000, AbstractUrlObjectCache.WAIT_LATEST, null, null, connectTimeout, readTimeout, true);

        int httpObjectCacheSize = SyspropUtil.getInteger(MAX_HTTP_CACHE_OBJECTS_PROP, DEFAULT_MAX_HTTP_CACHE_OBJECTS_SIZE) ;
        if (httpObjectCacheSize <= 0 || httpObjectCacheSize < DEFAULT_MAX_HTTP_CACHE_OBJECTS_SIZE) {
            httpObjectCacheSize = DEFAULT_MAX_HTTP_CACHE_OBJECTS_SIZE;
        }
        httpObjectCache = new HttpObjectCache<X509CRL>(httpObjectCacheSize, 30000, httpClientFactory, new CrlHttpObjectFactory(), AbstractUrlObjectCache.WAIT_INITIAL);
    }

    private static class CrlHttpObjectFactory implements AbstractUrlObjectCache.UserObjectFactory<X509CRL> {
        public X509CRL createUserObject(String url, AbstractUrlObjectCache.UserObjectSource response) throws IOException {
            ContentTypeHeader cth = response.getContentType();
            String encoding = "UTF-8";
            if (cth != null && cth.isText()) {
                encoding = cth.getEncoding();
            }

            byte[] data = response.getBytes();
            byte[] pemPrefix = "-----".getBytes(encoding);
            byte[] der = null;

            if ( ArrayUtils.compareArrays(data, 0, pemPrefix, 0, pemPrefix.length) ) {
                BufferedReader br = new BufferedReader(new StringReader(new String(data,encoding)));
                StringBuilder base64 = new StringBuilder();
                if (!br.readLine().trim().equals(CertUtils.PEM_CRL_BEGIN_MARKER)) throw new IllegalArgumentException("First line doesn't look like PEM");
                String line;
                while (!(line = br.readLine().trim()).equals( CertUtils.PEM_CRL_END_MARKER)) {
                    base64.append(line).append("\n");
                }

                der = HexUtils.decodeBase64(base64.toString());
            } else {
                der = data;
            }

            try {
                return (X509CRL)CertUtils.getFactory().generateCRL(new ByteArrayInputStream(der));
            } catch (CRLException e) {
                throw new CausedIOException("Invalid CRL", e);
            }
        }
    }

    public String[] getCrlUrlsFromCertificate(X509Certificate cert, Audit audit) throws IOException {
        String[] urls = getCachedCrlUrls(cert);
        if (urls == null) throw new IllegalStateException();
        return urls;
    }

    public X509CRL getCrl(String crlUrl, Audit auditor) throws CRLException, IOException {
        X509CRL crl;
        X509CRL cachedCRL = null;
        Lock read = null, write = null;
        CrlCacheEntry crlCacheEntry = (CrlCacheEntry) crlCache.retrieve(crlUrl);

        try {
            if (crlCacheEntry == null) {
                auditor.logAndAudit(SystemMessages.CERTVAL_REV_CACHE_MISS, "CRL", crlUrl);
                crlCacheEntry = new CrlCacheEntry();
                crlCache.store(crlUrl, crlCacheEntry);
                crl = null;

                read = crlCacheEntry.lock.readLock();
                read.lock();
            } else {
                read = crlCacheEntry.lock.readLock();
                read.lock();

                //grab the cached CRL first
                cachedCRL = crlCacheEntry.getCrl();

                if (crlCacheEntry.refresh < System.currentTimeMillis()) {
                    auditor.logAndAudit(SystemMessages.CERTVAL_REV_CACHE_STALE, "CRL", crlUrl, new Date(crlCacheEntry.refresh).toString());
                    crl = null;
                } else {
                    auditor.logAndAudit(SystemMessages.CERTVAL_REV_CACHE_FRESH, "CRL", crlUrl, new Date(crlCacheEntry.refresh).toString());
                    crl = crlCacheEntry.getCrl();
                }
            }

            if (crl == null) {
                try {
                    //attempt to grab the CRL through LDAP(s) or HTTP(s)
                    if (crlUrl.startsWith("ldap:") || crlUrl.startsWith("ldaps:")) {
                        crl = getCrlFromLdap(crlUrl);
                    } else if (crlUrl.startsWith("http:") || crlUrl.startsWith("https:")) {
                        crl = getCrlFromHttp(crlUrl);
                    } else {
                        throw new CRLException("Unsupported CRL URL scheme: " + crlUrl);
                    }
                }
                catch (IOException ioe){
                    //failed to grab CRL from LDAP(s) or HTTP(s), so we should try to reuse the cache version
                    if ( cachedCRL != null ) {

                        //we do have a cached CRL, we should use it
                        auditor.logAndAudit(SystemMessages.CERTVAL_REV_USE_CACHE, "CRL", crlUrl, new Date(crlCacheEntry.refresh).toString());
                        crl = cachedCRL;    //return the cached CRL to be used
                    }
                    else {
                        //we do not have a cached CRL, so we need to throw IOException
                        throw ioe;
                    }
                }

                read.unlock(); read = null; // Upgrade to write lock
                write = crlCacheEntry.lock.writeLock();
                write.lock();
                crlCacheEntry.setCrl(crl);
                write.unlock(); write = null;

                crlCache.store(crlUrl, crlCacheEntry);
            }

            return crl;
        } finally {
            if (write != null) write.unlock();
            if (read != null) read.unlock();
        }
    }

    private X509CRL getCrlFromHttp(String crlUrl) throws IOException {
        AbstractUrlObjectCache.FetchResult<X509CRL> fr = httpObjectCache.fetchCached(crlUrl, AbstractUrlObjectCache.WAIT_INITIAL);
        if (fr.getResult() == AbstractUrlObjectCache.RESULT_DOWNLOAD_SUCCESS ||
            fr.getResult() == AbstractUrlObjectCache.RESULT_USED_CACHED ) {
            X509CRL crl = fr.getUserObject();
            if (crl == null) {
                IOException ioe = fr.getException();
                if (ioe != null) {
                    throw ioe;
                }
                throw new CausedIOException("Unable to access CRL from HTTP cache, status is: " + fr.getResult());
            }
            return crl;
        } else {
            IOException ioe = fr.getException();
            if (ioe == null) {
                throw new CausedIOException("Unable to access CRL from HTTP cache, status is: " + fr.getResult(), ioe);
            }
            throw ioe;
        }
    }

    private X509CRL getCrlFromLdap(String url) throws IOException, CRLException {
        AbstractUrlObjectCache.FetchResult<LdapUrlObjectCache.LdapCacheEntry<X509CRL>> fr = ldapUrlObjectCache.fetchCached(url, AbstractUrlObjectCache.WAIT_INITIAL);
        String attrName;
        List<Object> vals;
        if (fr.getResult() != AbstractUrlObjectCache.RESULT_DOWNLOAD_FAILED) {
            LdapUrlObjectCache.LdapCacheEntry<X509CRL> lce = fr.getUserObject();
            synchronized(lce) {
                X509CRL crl = lce.getPayload();
                if (crl == null) {
                    attrName = lce.getInterestingAttributeName();
                    vals = lce.getInterestingAttributeValues();
                    if (attrName == null) throw new IOException("No query string in LDAP URL: " + url);
                    if (vals == null || vals.isEmpty()) throw new IOException("LDAP attribute value not present");
                    if (vals.size() > 1) throw new IllegalStateException("LDAP attribute has multiple values");
                    byte[] bytes = (byte[]) vals.get(0);
                    crl = (X509CRL) CertUtils.getFactory().generateCRL(new ByteArrayInputStream(bytes));
                    lce.setPayload(crl);
                }
                return crl;
            }
        } else {
            throw fr.getException();
        }
    }

    private class CrlCacheEntry {
        private X509CRL crl;
        private long timestamp;
        private long refresh;
        private final ReadWriteLock lock = new ReentrantReadWriteLock();

        /**
         * A CrlCacheEntry representing a failed (or not-yet-attempted) CRL retrieval ({@link #refresh} will be set 30s in the future)
         */
        private CrlCacheEntry() {
            this.timestamp = System.currentTimeMillis();
            this.refresh = timestamp + (30 * 1000); // Try again in 30s
        }

        public synchronized X509CRL getCrl() {
            return crl;
        }

        public synchronized void setCrl(X509CRL crl) {
            this.crl = crl;
            this.timestamp = System.currentTimeMillis();
            this.refresh = getNextUpdate(timestamp, crl.getNextUpdate());
        }

        private long getNextUpdate(final long timeNow, final Date updateDate) {
            long nextUpdate;

            if ( updateDate == null ) {
                nextUpdate = timeNow + getDefaultExpiry();
            } else {
                long updateTime = updateDate.getTime() - RETRIEVAL_OFFSET;
                long updatePeriod = updateTime - timeNow;

                if ( updatePeriod > getMaxExpiry() ) {
                    nextUpdate = timeNow + getMaxExpiry();
                } else if ( updatePeriod < getMinExpiry() ) {
                    nextUpdate = timeNow + getMinExpiry();                        
                } else {
                    nextUpdate = updateTime;
                }
            }

            return nextUpdate;
        }

        private long getDefaultExpiry() {
            return getExpiry(PROP_EXPIRY_DEFAULT);
        }

        private long getMinExpiry() {
            return getExpiry(PROP_EXPIRY_MIN);
        }

        private long getMaxExpiry() {
            return getExpiry(PROP_EXPIRY_MAX);
        }

        private long getExpiry(String name) {
            return serverConfig.getTimeUnitPropertyCached(name, ONE_HOUR, 30000);
        }
    }

    private String[] getCachedCrlUrls(final X509Certificate subjectCert) throws IOException {
        final String subjectDn = subjectCert.getSubjectDN().getName();
        //noinspection unchecked
        String[] urls = (String[]) certCache.retrieve(subjectDn);

        if (urls == null) {
            urls = ServerCertUtils.getCrlUrls(subjectCert);
            certCache.store(subjectDn, urls);
        }
        return urls;
    }

}
