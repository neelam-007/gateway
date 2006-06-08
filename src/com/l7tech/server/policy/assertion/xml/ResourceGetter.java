/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.server.policy.assertion.xml;

import com.l7tech.common.audit.AssertionMessages;
import com.l7tech.common.audit.Auditor;
import com.l7tech.common.http.GenericHttpClient;
import com.l7tech.common.http.GenericHttpRequestParams;
import com.l7tech.common.http.GenericHttpResponse;
import com.l7tech.common.http.cache.HttpObjectCache;
import com.l7tech.common.http.prov.apache.CommonsHttpClient;
import com.l7tech.common.util.CausedIOException;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.common.util.ResourceUtils;
import com.l7tech.common.util.TextUtils;
import com.l7tech.common.xml.ElementCursor;
import com.l7tech.policy.AssertionResourceInfo;
import com.l7tech.policy.MessageUrlResourceInfo;
import com.l7tech.policy.SingleUrlResourceInfo;
import com.l7tech.policy.StaticResourceInfo;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.HttpRoutingAssertion;
import com.l7tech.policy.assertion.UsesResourceInfo;
import com.l7tech.policy.variable.ExpandVariables;
import com.l7tech.server.KeystoreUtils;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.policy.ServerPolicyException;
import com.l7tech.server.transport.http.SslClientTrustManager;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.springframework.context.ApplicationContext;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.text.ParseException;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * An object that is created with a ResourceInfo and can thereafter fetch a resource given a message.
 * XXX If the same URL is used to point to multiple user object types the cache will thrash as they overwrite one another, Bug #2535
 */
abstract class ResourceGetter {
    private static final Logger logger = Logger.getLogger(ResourceGetter.class.getName());

    public abstract void close();

    // Some handy exceptions to ensure that information of interest is preserved until it gets back up
    // to the assertion to be audited

    /** Superclass for all exceptions that involve an external URL. */
    protected static class UrlResourceException extends Exception {
        private final String url;
        public UrlResourceException(String message, String url) {
            super(message);
            this.url = url;
        }

        public UrlResourceException(String message, Throwable cause, String url) {
            super(message, cause);
            this.url = url;
        }

        public UrlResourceException(Throwable cause, String url) {
            super(cause);
            this.url = url;
        }

        public String getUrl() {
            return url;
        }
    }

    /** Thrown if a Message cannot be examined for a resource URL because the message format is invalid. */
    public static class InvalidMessageException extends Exception {
        public InvalidMessageException() { super(); }
        public InvalidMessageException(String message) { super(message); }
        public InvalidMessageException(String message, Throwable cause) { super(message, cause); }
        public InvalidMessageException(Throwable cause) { super(cause); }
    }

    /** Thrown if a resource URL found inside a message is malformed. */
    public static class MalformedResourceUrlException extends UrlResourceException {
        public MalformedResourceUrlException(String message, String url) { super(message, url); }
    }

    /** Thrown if a resource URL found inside a message does not match the regexp whitelist. */
    public static class UrlNotPermittedException extends UrlResourceException {
        public UrlNotPermittedException(String message, String url) { super(message, url); }
    }

    /** Thrown if there is an error while parsing an external resource. */
    public static class ResourceParseException extends UrlResourceException {
        public ResourceParseException(Throwable cause, String url) { super(cause, url); }
    }

    public static class UrlNotFoundException extends Exception {
        public UrlNotFoundException() {
            super("No external resource URL was found in this message");
        }
    }

    /**
     * Thrown if there is an IOException while fetching an external resource.  This does not use IOException
     * so that the consumer can tell IOExceptions reading the request message apart from IOExceptions
     * reading the external resource.
     */
    public static class ResourceIOException extends UrlResourceException {
        public ResourceIOException(IOException cause, String url) { super(cause, url); }

        /** @return the wrapped IOException */
        public IOException getIOException() { return (IOException)getCause(); }
    }


    // ---------- BEGIN INSTANCE METHOD -----------

    /**
     * Fetch the resource for this message.  The resource may come from a cache, may be created fresh by downloading
     * from a URL, or may be created and then reused for every message -- caller doesn't need to care.
     * <p/>
     * The returned resource object should be safe to use simultaneously in multiple threads.
     *
     * @param message the message to inspect.  Must not be null if the original AssertionResourceInfo was
     *        {@link com.l7tech.policy.MessageUrlResourceInfo}.  The cursor might be moved by this method.
     * @param vars a Map&lt;String,Object&gt; of variable names and values for interpolation into URLs etc.  May be null.
     * @return the resource object to use for this Message, or null if no resource object is required for this message.
     *         Specifically, this can return null if and only if the resource info is Message Url and no resource url
     *         was found in this message.
     * @throws IOException if there was a problem reading the message in order to produce a resource
     * @throws InvalidMessageException if the message format is invalid
     * @throws UrlNotFoundException if no resource URL was found inside the message, and the policy does not permit messages without one
     * @throws MalformedResourceUrlException if a resource URL found inside a message is malformed
     * @throws UrlNotPermittedException if a resource URL found insdie a message is not matched in the whitelist
     * @throws ResourceIOException if an external resource can't be fetched.  Call getIOException to retrieve the underlying cause.
     * @throws ResourceParseException if an external resource is fetched but is found to be invalid
     * @throws GeneralSecurityException  if an SSL context is needed but cannot be created
     */
    public abstract Object getResource(ElementCursor message, Map vars)
            throws IOException, InvalidMessageException, UrlNotFoundException, MalformedResourceUrlException,
                   UrlNotPermittedException, ResourceIOException, ResourceParseException, GeneralSecurityException;

    // ---------- END INSTANCE METHOD -----------


    /**
     * Interface that converts raw resource bytes into an object for caching and reuse.  Implemented by
     * the consumer of the ResourceGetter services, and provided to the createResourceGetter() method.
     */
    interface ResourceObjectFactory {
        /**
         * Convert the specified bytes into a resource object that can be cached and reused, including use by
         * multiple threads simulataneously.  The Object will be returned back through getResource(), and
         * the consumer can downcast it to the appropriate type.
         *
         * @param url  the URL the content was loaded from, or null
         * @param  resourceContent the content of the resource document @return the object to cache and reuse.  Returning null will be considered the same as throwing IOException
         *         with no message.
         * @throws ParseException if the specified resource bytes could not be converted into a resource object.
         */
        Object createResourceObject(String url, String resourceContent) throws ParseException;
    }


    /**
     * Interface that finds the appropriate resource URL inside a message.
     */
    interface UrlFinder {
        /**
         * Inspect the specified message and return a resource URL, or null if no resource URL could be found.
         * This method is not responsible for matching any such URL against the regular expression whitelist (if any) --
         * the ResourceGetter itself has that responsibility.
         *
         * @param message  the message to inspect.  Never null.  The cursor may be moved by this method.
         * @return a URL in String form, or null if no resource URL was found in this message.
         * @throws IOException if there was a problem reading the message
         * @throws InvalidMessageException if the message format is invalid
         */
        String findUrl(ElementCursor message) throws IOException, InvalidMessageException;
    }


    // ---------- BEGIN STATIC FACTORY METHOD ----------

    /**
     * Create a ResourceGetter that will obtain resource objects for messages using the given configuration
     * (AssertionResourceInfo and ResourceObjectFactory).
     *
     * @param assertion the assertion bean that owns the configuration of the type and paramaters for fetching the resource
     *                  (static, single URL, URL from message, etc).  Must not be null.  assertion.getResourceInfo() must
     * @param rof  strategy for converting the raw resource bytes into the consumer's preferred resource object format
     *             for caching/reuse/metadata etc.  Must not be null.
     * @param urlFinder strategy for finding a URL within a message.  Must not be null if assertion.getResourceInfo()
     *                   might be MessageUrlResourceInfo.
     * @param springContext  application context in case an SSLContext needs to be created.  Must not be null if SSL will be used.
     * @param auditor        auditor to use to log warnings when a previously-cached resource object is reused after there is a network problem fetching an up-to-date copy.
     *                       May be null to disable such warnings.
     * @return a ResourceGetter that will fetch resources for future messages.  Never null.
     * @throws NullPointerException if assertion or rof is null
     * @throws ServerPolicyException if the assertion contains no ResourceInfo
     * @throws ServerPolicyException if a ResourceGetter could not be created from the provided configuration
     * @throws IllegalArgumentException if the resource info type if MessageUrlResourceInfo but urlFinder is null
     */
    public static <AC extends Assertion & UsesResourceInfo>
    ResourceGetter createResourceGetter(AC assertion, ResourceObjectFactory rof, UrlFinder urlFinder, ApplicationContext springContext, Auditor auditor)
            throws ServerPolicyException
    {
        AssertionResourceInfo ri = assertion.getResourceInfo();
        if (ri == null) throw new ServerPolicyException(assertion, "Assertion contains no ResourceInfo provided");
        if (rof == null) throw new NullPointerException("No ResourceObjectFactory provided");

        try {
            if (ri instanceof MessageUrlResourceInfo) {
                if (urlFinder == null) throw new IllegalArgumentException("MessageUrlResourceInfo requested but no UrlFinder provided");
                return new MessageUrlResourceGetter((MessageUrlResourceInfo)ri, rof, urlFinder, springContext, auditor);
            } else if (ri instanceof SingleUrlResourceInfo) {
                return new SingleUrlResourceGetter(assertion, (SingleUrlResourceInfo)ri, rof, springContext, auditor);
            } else if (ri instanceof StaticResourceInfo) {
                return new StaticResourceGetter(assertion, (StaticResourceInfo)ri, rof);
            } else
                throw new ServerPolicyException(assertion, "Unsupported XSLT resource info: " + ri.getClass().getName());
        } catch (PatternSyntaxException e) {
            throw new ServerPolicyException(assertion, "Couldn't compile regular expression '" + e.getPattern() + "'", e);
        }
    }

    // ---------- END STATIC FACTORY METHOD ----------


    /**
     * Superclass for ResourceGetters that fetch the resource from an external URL.
     */
    public static abstract class UrlResourceGetter extends ResourceGetter {
        protected static final ThreadLocal httpRequest = new ThreadLocal() {
            protected Object initialValue() {
                return new GenericHttpRequestParams();
            }
        };

        private static volatile SSLContext sslContext; // initialized lazily
        private static final GenericHttpClient httpClient;
        static {
            MultiThreadedHttpConnectionManager connectionManager = new MultiThreadedHttpConnectionManager();
            connectionManager.setMaxConnectionsPerHost(100);
            connectionManager.setMaxTotalConnections(1000);
            httpClient = new CommonsHttpClient(connectionManager);
        }

        protected static int getIntProperty(String propName, int emergencyDefault) {
            String strval = ServerConfig.getInstance().getProperty(propName);
            int val;
            try {
                val = Integer.parseInt(strval);
            } catch (NumberFormatException e) {
                logger.warning("Parameter " + propName + " value '" + strval + "' not a valid number; using " + emergencyDefault + " instead");
                val = emergencyDefault;
            }
            return val;
        }

        // --- Member fields
        private final ResourceObjectFactory resourceObjectFactory;
        private final ApplicationContext springContext;  // may be null
        private final Auditor auditor; // may be null
        private final Pattern[] urlWhitelist; // may be null

        /**
         * Initialize common code for URL based resource getters.
         *
         * @param resourceObjectFactory  strategy for converting resource bytes into cachable user objects
         * @param springContext the Spring context to use in case an SSLContext must be created.  Must not be null if href is https.
         * @param auditor  auditor to use for logging a warning if a previously-cached version of the resource is used due to
         *                 a network problem fetching the latest version.  This auditor won't be used for any other
         *                 purpose.
         */
        protected UrlResourceGetter(ResourceObjectFactory resourceObjectFactory,
                                    Pattern[] urlWhitelist,
                                    ApplicationContext springContext,
                                    Auditor auditor)
        {
            this.resourceObjectFactory = resourceObjectFactory;
            this.urlWhitelist = urlWhitelist;
            this.springContext = springContext;
            this.auditor = auditor;
        }

        /** @return the maximum age for cached objects, in milliseconds. */
        protected abstract int getMaxCacheAge();

        /** @return the {@link HttpObjectCache} through which to fetch the external URLs. */
        public abstract HttpObjectCache getHttpObjectCache();

        /** @return the HTTP client we are using with the object cache. */
        public GenericHttpClient getHttpClient() {
            return httpClient;
        }

        /** @return the whitelist of remote URLs to allow, or null. */
        public Pattern[] getUrlWhitelist() {
            return urlWhitelist;
        }

        /**
         * Do the actual fetch of a (possibly cached) user object from this URL, through the current object cache.
         *
         * @param href     the URL to fetch.  Must not be null.
         * @return the user object.  Never null.
         * @throws ParseException  if an external resource was fetched but it could not be parsed
         * @throws GeneralSecurityException  if an SSLContext is needed but cannot be initialized
         * @throws MalformedResourceUrlException  if the provided href is not a well-formed URL
         * @throws ResourceIOException  if there is an IOException while fetching the external resource
         * @throws NullPointerException if href or spring is null
         */
        protected Object fetchObject(final String href)
                throws ParseException, GeneralSecurityException, ResourceIOException, MalformedResourceUrlException {
            if (href == null) throw new NullPointerException("no href provided");
            GenericHttpRequestParams params = (GenericHttpRequestParams)httpRequest.get();
            final URL url;
            try {
                url = new URL(href);
            } catch (MalformedURLException e) {
                throw new MalformedResourceUrlException("Invalid resource URL: " + href, href);
            }
            params.setTargetUrl(url);
            if (url.getProtocol().equals("https")) {
                if (springContext == null) throw new GeneralSecurityException("Unable to create SSL context: no Spring context provided");
                params.setSslSocketFactory(getSslContext(springContext).getSocketFactory());
            }

            final HttpObjectCache.UserObjectFactory userObjectFactory = new HttpObjectCache.UserObjectFactory() {
                public Object createUserObject(String surl, GenericHttpResponse response) throws IOException {
                    String thing = response.getAsString();
                    logger.fine("Downloaded resource from " + surl);
                    try {
                        Object obj = resourceObjectFactory.createResourceObject(href, thing);
                        if (obj == null)
                            throw new IOException("Unable to create resource from HTTP response: ResourceObjectFactory returned null");
                        return obj;
                    } catch (ParseException e) {
                        // Wrap in IOException and we'll unwrap it below
                        throw new CausedIOException(e);
                    }
                }
            };

            // Get cached, possibly checking if-modified-since against server, possibly downloading a new stylesheet
            HttpObjectCache.FetchResult result = getHttpObjectCache().fetchCached(getHttpClient(),
                                                                                  params,
                                                                                  false,
                                                                                  userObjectFactory);

            Object userObject = result.getUserObject();
            IOException err = result.getException();

            if (userObject == null) {
                // Didn't manage to get a user object.  See if we got an error instead.
                // If it's actually a ParseException, we can just unwrap and rethrow it
                Throwable pe = ExceptionUtils.getCauseIfCausedBy(err, ParseException.class);
                if (pe != null)
                    throw (ParseException)pe;

                throw new ResourceIOException(err, href);
            }

            // Got a userObject.  See if we need to log any warnings.
            if (err != null && auditor != null)
                auditor.logAndAudit(AssertionMessages.XSLT_CANT_READ_XSL2, new String[] { href, ExceptionUtils.getMessage(err) });

            return userObject;
        }

        /**
         * Get the process-wide shared SSL context, creating it if this thread is the first one
         * to need it.
         *
         * @param springContext the spring context, in case one needs to be created.  Must not be null.
         * @return the current SSL context.  Never null.
         * @throws java.security.GeneralSecurityException  if an SSL context is needed but can't be created because the current server
         *                                   configuration is incomplete or invalid (keystores, truststores, and whatnot)
         */
        private static SSLContext getSslContext(ApplicationContext springContext) throws GeneralSecurityException
        {
            // no harm done if multiple threads try to create it the very first time.  s'all good.
            if (sslContext != null) return sslContext;
            synchronized(ServerXslTransformation.class) {
                if (sslContext != null) return sslContext;
            }
            SSLContext sc = SSLContext.getInstance("SSL");
            KeystoreUtils keystore = (KeystoreUtils)springContext.getBean("keystore");
            SslClientTrustManager trustManager = (SslClientTrustManager)springContext.getBean("httpRoutingAssertionTrustManager");
            KeyManager[] keyman = keystore.getSSLKeyManagerFactory().getKeyManagers();
            sc.init(keyman, new TrustManager[]{trustManager}, null);
            final int timeout = Integer.getInteger(HttpRoutingAssertion.PROP_SSL_SESSION_TIMEOUT,
                                                   HttpRoutingAssertion.DEFAULT_SSL_SESSION_TIMEOUT);
            sc.getClientSessionContext().setSessionTimeout(timeout);
            synchronized(ServerXslTransformation.class) {
                return sslContext = sc;
            }
        }
    }


    /**
     * A ResourceGetter that finds URLs inside the message, then fetches the appropriate resource (with caching),
     * corresponding to {@link MessageUrlResourceInfo}.
     */
    private static class MessageUrlResourceGetter extends UrlResourceGetter {
        /** Cache age for all URL-from-message resources, system-wide. */
        private static final int maxCacheAge = getIntProperty(ServerConfig.PARAM_MESSAGEURL_MAX_CACHE_AGE, 300000);
        private final UrlFinder urlFinder;
        private final boolean allowMessagesWithoutUrl;

        protected int getMaxCacheAge() { return maxCacheAge; }

        /** Shared cache for all URL-from-message resources, system-wide. */
        private static final HttpObjectCache httpObjectCache =
                new HttpObjectCache(getIntProperty(ServerConfig.PARAM_MESSAGEURL_MAX_CACHE_ENTRIES, 100), maxCacheAge);
        public HttpObjectCache getHttpObjectCache() { return httpObjectCache; }

        // --- Instance fields ---

        private MessageUrlResourceGetter(MessageUrlResourceInfo ri,
                                         ResourceObjectFactory rof,
                                         UrlFinder urlFinder,
                                         ApplicationContext springContext,
                                         Auditor auditor)
                throws PatternSyntaxException
        {
            super(rof, ri.makeUrlPatterns(), springContext, auditor);
            this.urlFinder = urlFinder;
            this.allowMessagesWithoutUrl = ri.isAllowMessagesWithoutUrl();

            if (rof == null || urlFinder == null) throw new NullPointerException(); // can't happen
        }

        public void close() {
            // Nothing we can do except wait for the finalizer -- userObject(s) may be in use
        }

        public Object getResource(ElementCursor message, Map vars)
                throws IOException, MalformedResourceUrlException, UrlNotPermittedException,
                ResourceIOException, InvalidMessageException, ResourceParseException, GeneralSecurityException, UrlNotFoundException
        {
            final String url;
            url = urlFinder.findUrl(message);
            if (url == null) {
                if (allowMessagesWithoutUrl)
                    return null;

                throw new UrlNotFoundException();
            }

            // match against URL patterns

            if (!TextUtils.matchesAny(url, getUrlWhitelist()))
                throw new UrlNotPermittedException("External resource URL not permitted by whitelist: " + url, url);

            try {
                return fetchObject(url);
            } catch (ParseException e) {
                throw new ResourceParseException(e, url);
            }
        }
    }


    /**
     * A ResourceGetter that has a single statically-configured URL that it watches to download the latest value of
     * the resource, corresponding to {@link SingleUrlResourceInfo}.
     */
    private static class SingleUrlResourceGetter extends UrlResourceGetter {
        /** Cache age for all from-static-URL resources, system-wide. */
        private static final int maxCacheAge = getIntProperty(ServerConfig.PARAM_SINGLEURL_MAX_CACHE_AGE, 300000);
        protected int getMaxCacheAge() { return maxCacheAge; }

        /** Shared cache for all from-static-URL resources, system-wide. */
        private static final HttpObjectCache httpObjectCache =
                new HttpObjectCache(getIntProperty(ServerConfig.PARAM_SINGLEURL_MAX_CACHE_ENTRIES, 100), maxCacheAge);
        public HttpObjectCache getHttpObjectCache() { return httpObjectCache; }

        private final String url;

        private SingleUrlResourceGetter(Assertion assertion,
                                        SingleUrlResourceInfo ri,
                                        ResourceObjectFactory rof,
                                        ApplicationContext springContext,
                                        Auditor auditor)
                throws ServerPolicyException
        {
            super(rof, ri.makeUrlPatterns(), springContext, auditor);
            String url = ri.getUrl();
            if (url == null) throw new ServerPolicyException(assertion, "Missing resource url");
            this.url = url;

            // Ensure URL is well-formed
            try {
                new URL(url);
            } catch (MalformedURLException e) {
                throw new ServerPolicyException(assertion, "Invalid resource URL: " + url);
            }
        }

        public void close() {
            // Nothing we can do except wait for the finalizer -- userObject(s) may be in use
        }

        public Object getResource(ElementCursor message, Map vars) throws IOException, ResourceParseException, GeneralSecurityException, ResourceIOException, MalformedResourceUrlException {
            String actualUrl = vars == null ? url : ExpandVariables.process(url, vars);
            try {
                return fetchObject(actualUrl);
            } catch (ParseException e) {
                throw new ResourceParseException(e, actualUrl);
            }
        }
    }


    /**
     * A ResourceGetter that owns a single statically-configured value for the resource, with no network
     * communication needed, and corresponding to {@link StaticResourceInfo}.
     */
    private static class StaticResourceGetter extends ResourceGetter {
        private final Object userObject;

        private StaticResourceGetter(Assertion assertion, StaticResourceInfo ri, ResourceObjectFactory rof)
                throws ServerPolicyException
        {
            String doc = ri.getDocument();
            if (doc == null) throw new ServerPolicyException(assertion, "Empty static document");
            try {
                Object userObject = rof.createResourceObject("", doc);
                if (userObject == null)
                    throw new ServerPolicyException(assertion, "Unable to create static user object: ResourceObjectFactory returned null");
                this.userObject = userObject;
            } catch (ParseException e) {
                throw new ServerPolicyException(assertion, "Unable to create static user object: " + ExceptionUtils.getMessage(e), e);
            }
        }

        public void close() {
            ResourceUtils.closeQuietly(userObject);
        }

        public Object getResource(ElementCursor message, Map vars) throws IOException {
            return userObject;
        }
    }
}
