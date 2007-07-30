/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.server.security.cert;

import com.l7tech.common.audit.Auditor;
import com.l7tech.common.audit.SystemMessages;
import com.l7tech.common.urlcache.AbstractUrlObjectCache;
import com.l7tech.common.urlcache.HttpObjectCache;
import com.l7tech.common.urlcache.LdapUrlObjectCache;
import com.l7tech.common.util.CausedIOException;
import com.l7tech.common.util.CertUtils;
import com.l7tech.common.util.HexUtils;
import com.l7tech.common.util.WhirlycacheFactory;
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
    /** Cache&lt;String, CrlCacheEntry&gt; where String is a CRL URL */
    private final Cache crlCache;
    /** Cache&lt;X509Certificate, CertCacheEntry&gt; */
    private final Cache certCache;

    private final LdapUrlObjectCache<X509CRL> ldapUrlObjectCache;
    private final HttpObjectCache<X509CRL> httpObjectCache;

    public CrlCacheImpl(HttpClientFactory httpClientFactory) throws Exception {
        this.crlCache = WhirlycacheFactory.createCache(CrlCache.class.getSimpleName() + ".crlCache", 100, 1800, WhirlycacheFactory.POLICY_LRU);
        this.certCache = WhirlycacheFactory.createCache(CrlCache.class.getSimpleName() + ".certCache", 1000, 1800, WhirlycacheFactory.POLICY_LRU);

        // TODO support configuration of login, password and LDAP timeouts
        ldapUrlObjectCache = new LdapUrlObjectCache<X509CRL>(300000, AbstractUrlObjectCache.WAIT_LATEST, null, null, 5000, 30000, true);
        httpObjectCache = new HttpObjectCache<X509CRL>(300000, 30000, httpClientFactory, new CrlHttpObjectFactory(), AbstractUrlObjectCache.WAIT_INITIAL);
    }

    private static class CrlHttpObjectFactory implements AbstractUrlObjectCache.UserObjectFactory<X509CRL> {
        public X509CRL createUserObject(String url, String response) throws IOException {
            BufferedReader br = new BufferedReader(new StringReader(response));
            StringBuilder base64 = new StringBuilder();
            if (!br.readLine().trim().equals(CertUtils.PEM_CRL_BEGIN_MARKER)) throw new IllegalArgumentException("First line doesn't look like PEM");
            String line;
            while (!(line = br.readLine().trim()).equals(CertUtils.PEM_CRL_END_MARKER)) {
                base64.append(line).append("\n");
            }

            byte[] der = HexUtils.decodeBase64(base64.toString());
            try {
                return (X509CRL)CertUtils.getFactory().generateCRL(new ByteArrayInputStream(der));
            } catch (CRLException e) {
                throw new CausedIOException("Invalid CRL", e);
            }
        }
    }

    public String[] getCrlUrlsFromCertificate(X509Certificate cert, Auditor auditor) throws IOException {
        String[] urls = getCachedCrlUrls(cert);
        if (urls == null) throw new IllegalStateException();
        return urls;
    }

    public X509CRL getCrl(String crlUrl, Auditor auditor) throws CRLException, IOException {
        X509CRL crl;
        Lock read = null, write = null;
        CrlCacheEntry crlCacheEntry = (CrlCacheEntry) crlCache.retrieve(crlUrl);
        try {
            if (crlCacheEntry == null) {
                auditor.logAndAudit(SystemMessages.CERTVAL_REV_CACHE_MISS, crlUrl);
                crlCacheEntry = new CrlCacheEntry();
                crlCache.store(crlUrl, crlCacheEntry);
                crl = null;

                read = crlCacheEntry.lock.readLock();
                read.lock();
            } else {
                read = crlCacheEntry.lock.readLock();
                read.lock();

                if (crlCacheEntry.refresh < System.currentTimeMillis()) {
                    auditor.logAndAudit(SystemMessages.CERTVAL_REV_CACHE_STALE, crlUrl, new Date(crlCacheEntry.refresh).toString());
                    crl = null;
                } else {
                    auditor.logAndAudit(SystemMessages.CERTVAL_REV_CACHE_FRESH, crlUrl, new Date(crlCacheEntry.refresh).toString());
                    crl = crlCacheEntry.getCrl();
                }
            }

            if (crl == null) {
                if (crlUrl.startsWith("ldap:") || crlUrl.startsWith("ldaps:")) {
                    crl = getCrlFromLdap(crlUrl);
                } else if (crlUrl.startsWith("http:") || crlUrl.startsWith("https:")) {
                    crl = getCrlFromHttp(crlUrl);
                } else {
                    throw new CRLException("Unsupported CRL URL scheme: " + crlUrl);
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
        if (fr.getResult() != AbstractUrlObjectCache.RESULT_DOWNLOAD_FAILED) {
            return fr.getUserObject();
        } else {
            throw fr.getException();
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

    private static class CrlCacheEntry {
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
            this.refresh = crl.getNextUpdate().getTime();
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
