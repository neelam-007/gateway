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
import com.l7tech.common.util.ExceptionUtils;

import java.util.regex.Pattern;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.net.URL;
import java.net.MalformedURLException;
import java.io.IOException;

/**
 * LSResourceResolver used by the SchemaManagerImpl.
 */
class CachingLSResourceResolver implements LSResourceResolver {
    private static final Logger logger = Logger.getLogger(CachingLSResourceResolver.class.getName());

    /** This LSInput will be returned to indicate "Resource not resolved, and don't try to get it over the network unless you know what you are doing" */
    public static final LSInput LSINPUT_UNRESOLVED = new LSInputImpl();

    private final SchemaFinder schemaFinder;
    private final GenericHttpClient httpClient;
    private final HttpObjectCache cache;
    private final SchemaCompiler schemaCompiler;
    private final ImportListener importListener;
    private final Pattern[] urlWhitelist;

    /** Gets called when we are checking for an existing well-known schema. */
    public static interface SchemaFinder {
        /** @return an already-existing schema, or null if not found. */
        SchemaHandle getSchema(String namespaceURI, String systemId, String baseURI);
    }

    /** Gets called only if we need to compile a new schema. */
    public static interface SchemaCompiler {
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
        SchemaHandle getSchema(String url, GenericHttpResponse response) throws IOException;
    }

    /** Gets called no matter what, on any import we find. */
    public static interface ImportListener {
        void foundImport(SchemaHandle imported);
    }

    public CachingLSResourceResolver(SchemaFinder schemaFinder,
                                     GenericHttpClient httpClient,
                                     HttpObjectCache cache,
                                     Pattern[] urlWhitelist,
                                     SchemaCompiler schemaCompiler,
                                     ImportListener importListener)
    {
        this.schemaFinder = schemaFinder;
        this.cache = cache;
        this.schemaCompiler = schemaCompiler;
        this.urlWhitelist = urlWhitelist;
        this.httpClient = httpClient;
        this.importListener = importListener;
        if (schemaFinder == null || importListener == null || schemaCompiler == null)
            throw new NullPointerException();
        if (cache == null || urlWhitelist == null || httpClient == null)
            throw new IllegalArgumentException("HTTP-fetching parameters must be provided");
    }

    /**
     * We must find or create a SchemaHandle for this import, then notify the ImportListener of the dependency,
     * and then return the LSInput for this import.
     *
     * @param type
     * @param namespaceURI
     * @param publicId
     * @param systemId
     * @param baseURI
     * @return an LSInput.  Never null.
     */
    public LSInput resolveResource(String type, String namespaceURI, String publicId, String systemId, String baseURI) {
        if(!XmlUtil.W3C_XML_SCHEMA.equals(type)) {
            logger.info("Refusing remote reference to non-schema object systemId " + systemId + " of type " + type);
            return LSINPUT_UNRESOLVED;
        }

        // First look for well-known schema
        SchemaHandle schemaHandle = schemaFinder.getSchema(namespaceURI, systemId, baseURI);

        if (schemaHandle != null) {
            importListener.foundImport(schemaHandle);
            return schemaHandle.getLSInput();
        }

        // No existing well-known schema -- we'll try to fetch a remote one (or look it up in the object cache)

        // Try a carefully managed, perfectly safe, regex-protected, scientifically proven remote network call
        final URL baseUrl;
        try {
            logger.finest("remote schema base url=" + baseURI + "  publicId=" + publicId + "  systemId=" + systemId);
            baseUrl = new URL(baseURI);
        } catch (MalformedURLException e) {
            logger.warning("Refusing remote schema reference to invalid base URL " + baseURI);
            return LSINPUT_UNRESOLVED;
        }

        try {
            final String proto = baseUrl.getProtocol();
            if (!proto.equals("http") && !proto.equals("https")) {
                logger.warning("Refusing remote schema reference with non-HTTP(S) base URL: " + baseURI);
                return LSINPUT_UNRESOLVED;
            }
            URL fullUrl = new URL(baseUrl, systemId);

            String url = fullUrl.toExternalForm();
            logger.finest("remote schema full url=" + url);

            if (!TextUtils.matchesAny(url, urlWhitelist)) {
                logger.warning("Refusing remote schema reference to non-whitelisted resource URL: " + url);
                return LSINPUT_UNRESOLVED;
            }

            GenericHttpRequestParams requestParams = new GenericHttpRequestParams(fullUrl);

            HttpObjectCache.FetchResult got = cache.fetchCached(httpClient, requestParams, false, new HttpObjectCache.UserObjectFactory() {
                public Object createUserObject(String url, GenericHttpResponse response) throws IOException {
                    return schemaCompiler.getSchema(url, response);
                }
            });

            Object userObj = got.getUserObject();
            if (userObj == null) {
                logger.log(Level.WARNING, "Unable to fetch remote schema reference: " + ExceptionUtils.getMessage(got.getException()), got.getException());
                return LSINPUT_UNRESOLVED;
            }

            if (!(userObj instanceof SchemaHandle)) {
                logger.warning("Refusing remote schema reference becuase the URL was fetched recently and found not to contain a schema: url = " + url);
                return LSINPUT_UNRESOLVED;
            }

            return ((SchemaHandle)userObj).getLSInput();

        } catch (MalformedURLException e) {
            logger.warning("Refusing remote schema reference to invalid systemId " + systemId);
            return LSINPUT_UNRESOLVED;
        }
    }
}
