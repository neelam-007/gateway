/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.server.communityschemas;

import org.w3c.dom.ls.LSResourceResolver;
import org.w3c.dom.ls.LSInput;
import com.l7tech.common.http.cache.HttpObjectCache;
import com.l7tech.common.http.GenericHttpClient;
import com.l7tech.common.http.GenericHttpRequestParams;
import com.l7tech.common.http.GenericHttpResponse;
import com.l7tech.common.util.LSInputImpl;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.util.TextUtils;

import java.util.regex.Pattern;
import java.util.logging.Logger;
import java.net.URL;
import java.net.MalformedURLException;
import java.io.IOException;

/**
 * LSResourceResolver used by the CompiledSchemaManager.
 */
public class CachingLSResourceResolver implements LSResourceResolver {
    private static final Logger logger = Logger.getLogger(CachingLSResourceResolver.class.getName());

    /** This LSInput will be returned to indicate "Resource not resolved, and don't try to get it over the network unless you know what you are doing" */
    public static final LSInput LSINPUT_UNRESOLVED = new LSInputImpl();

    private final LSResourceResolver delegate;
    private final GenericHttpClient httpClient;
    private final HttpObjectCache cache;
    private final LSInputHaverMaker lsInputHaverMaker;
    private final Pattern[] urlWhitelist;

    /** Interface implemented by objects which can represent themselves as an LSInput. */
    public static interface LSInputHaver {
        /** @return the LSInput representation of this object. */
        LSInput getLSInput();
    }

    public static interface LSInputHaverMaker {
        /**
         * Fetch a possibly-cached, cacheable object that will produce an LSInput for this an future invocations.
         *
         * @param url  the URL that was fetched.  Never null.
         * @param response  a non-null GenericHttpResponse, which might have any result code (not just 200).
         *                  Factory can consume its InputStream.
         * @return the user Object to enter into the cache.  Should not be null; throw IOException instead.
         * @throws IOException if this response was not accepted for caching, in which case this request will
         *                     be treated as a failure.
         */
        LSInputHaver makeLSInputHaver(String url, GenericHttpResponse response) throws IOException;
    }

    public CachingLSResourceResolver(LSResourceResolver delegate,
                                     GenericHttpClient httpClient,
                                     HttpObjectCache cache,
                                     LSInputHaverMaker lsInputHaverMaker,
                                     Pattern[] urlWhitelist) {
        this.delegate = delegate;
        this.cache = cache;
        this.lsInputHaverMaker = lsInputHaverMaker;
        this.urlWhitelist = urlWhitelist;
        this.httpClient = httpClient;
        if (httpClient == null || lsInputHaverMaker == null) throw new NullPointerException();
        if (cache == null || urlWhitelist == null || delegate == null) throw new NullPointerException();
    }

    public LSInput resolveResource(String type, String namespaceURI, String publicId, String systemId, String baseURI) {
        LSInput result = delegate.resolveResource(type, namespaceURI, publicId, systemId, baseURI);
        if (result != null && result != LSINPUT_UNRESOLVED) return result;

        if(!XmlUtil.W3C_XML_SCHEMA.equals(type)) {
            logger.info("Refusing remote reference to non-schema object systemId " + systemId + " of type " + type);
            return LSINPUT_UNRESOLVED;
        }

        // Try a carefully managed, perfectly safe, regex-protected, scientifically proven remote network call
        final URL baseUrl;
        try {
            baseUrl = new URL(baseURI);
        } catch (MalformedURLException e) {
            logger.info("Refusing remote schema reference to invalid base URL " + baseURI);
            return LSINPUT_UNRESOLVED;
        }

        try {
            final String proto = baseUrl.getProtocol();
            if (!proto.equals("http") && !proto.equals("https")) {
                logger.info("Refusing remote schema reference with non-HTTP(S) base URL: " + baseURI);
                return LSINPUT_UNRESOLVED;
            }
            URL fullUrl = new URL(baseUrl, systemId);

            String url = fullUrl.toExternalForm();

            if (!TextUtils.matchesAny(url, urlWhitelist)) {
                logger.info("Refusing remote schema reference to non-whitelisted resource URL: " + url);
                return LSINPUT_UNRESOLVED;
            }


            GenericHttpRequestParams requestParams = new GenericHttpRequestParams(fullUrl);

            Object got = cache.fetchCached(httpClient, requestParams, false, new HttpObjectCache.UserObjectFactory() {
                public Object createUserObject(String url, GenericHttpResponse response) throws IOException {
                    return lsInputHaverMaker.makeLSInputHaver(url, response);
                }
            });

            if (!(got instanceof LSInputHaver)) {
                logger.warning("Refusing remote schema reference becuase the URL was fetched recently and found not to contain a schema: url = " + url);
                return LSINPUT_UNRESOLVED;
            }

            LSInputHaver lih = (LSInputHaver)got;
            return lih.getLSInput();

        } catch (MalformedURLException e) {
            logger.info("Refusing remote schema reference to invalid systemId " + systemId);
            return LSINPUT_UNRESOLVED;
        }
    }
}
