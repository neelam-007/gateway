/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.server.policy.assertion.xml;

import com.l7tech.common.audit.Auditor;
import com.l7tech.common.http.GenericHttpClient;
import com.l7tech.common.http.cache.HttpObjectCache;
import com.l7tech.common.http.cache.UserObject;
import com.l7tech.common.http.prov.apache.CommonsHttpClient;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.common.util.ResourceUtils;
import com.l7tech.common.util.TextUtils;
import com.l7tech.common.xml.ElementCursor;
import com.l7tech.policy.AssertionResourceInfo;
import com.l7tech.policy.MessageUrlResourceInfo;
import com.l7tech.policy.SingleUrlResourceInfo;
import com.l7tech.policy.StaticResourceInfo;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.UsesResourceInfo;
import com.l7tech.policy.variable.ExpandVariables;
import com.l7tech.server.policy.ServerPolicyException;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.text.ParseException;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * An object that is created with a ResourceInfo and can thereafter fetch a resource given a message.
 */
abstract class ResourceGetter<UT extends UserObject> {
    private static final Pattern MATCH_ALL = Pattern.compile(".");

    private static final GenericHttpClient httpClient;

    static {
        MultiThreadedHttpConnectionManager connectionManager = new MultiThreadedHttpConnectionManager();
        connectionManager.setMaxConnectionsPerHost(100);
        connectionManager.setMaxTotalConnections(1000);
        httpClient = new CommonsHttpClient(connectionManager);
    }

    protected final HttpObjectCache<UT> httpObjectCache;

    protected ResourceGetter(HttpObjectCache<UT> httpObjectCache) {
        this.httpObjectCache = httpObjectCache;
        if (httpObjectCache == null) throw new NullPointerException();
    }

    public abstract void close();

    /** @return the HTTP client we are using with the object cache. */
    public GenericHttpClient getHttpClient() {
        return httpClient;
    }

    public abstract Pattern[] getUrlWhitelist();

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
    public abstract UT getResource(ElementCursor message, Map vars)
            throws IOException, InvalidMessageException, UrlNotFoundException, MalformedResourceUrlException,
                   UrlNotPermittedException, ResourceIOException, ResourceParseException, GeneralSecurityException;

    // ---------- END INSTANCE METHOD -----------


    /**
     * Interface that converts raw resource bytes into an object for caching and reuse.  Implemented by
     * the consumer of the ResourceGetter services, and provided to the createResourceGetter() method.
     */
    interface ResourceObjectFactory<UT extends UserObject> {
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
        UT createResourceObject(String url, String resourceContent) throws ParseException;
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
     * @param auditor        auditor to use to log warnings when a previously-cached resource object is reused after there is a network problem fetching an up-to-date copy.
     *                       May be null to disable such warnings.
     * @return a ResourceGetter that will fetch resources for future messages.  Never null.
     * @throws NullPointerException if assertion or rof is null
     * @throws ServerPolicyException if the assertion contains no ResourceInfo
     * @throws ServerPolicyException if a ResourceGetter could not be created from the provided configuration
     * @throws IllegalArgumentException if the resource info type if MessageUrlResourceInfo but urlFinder is null
     */
    public static <AC extends Assertion & UsesResourceInfo, UT extends UserObject>
    ResourceGetter<UT> createResourceGetter(AC assertion, ResourceObjectFactory<UT> rof, UrlFinder urlFinder, HttpObjectCache<UT> httpObjectCache, Auditor auditor)
            throws ServerPolicyException
    {
        AssertionResourceInfo ri = assertion.getResourceInfo();
        if (ri == null) throw new ServerPolicyException(assertion, "Assertion contains no ResourceInfo provided");
        if (rof == null) throw new NullPointerException("No ResourceObjectFactory provided");
        if (httpObjectCache == null) throw new NullPointerException("No HttpObjectCache provided");

        try {
            if (ri instanceof MessageUrlResourceInfo) {
                if (urlFinder == null) throw new IllegalArgumentException("MessageUrlResourceInfo requested but no UrlFinder provided");
                return new MessageUrlResourceGetter<UT>((MessageUrlResourceInfo)ri, rof, urlFinder, httpObjectCache, auditor);
            } else if (ri instanceof SingleUrlResourceInfo) {
                return new SingleUrlResourceGetter<UT>(assertion, (SingleUrlResourceInfo)ri, rof, httpObjectCache, auditor);
            } else if (ri instanceof StaticResourceInfo) {
                return new StaticResourceGetter<UT>(assertion, (StaticResourceInfo)ri, rof, httpObjectCache);
            } else
                throw new ServerPolicyException(assertion, "Unsupported XSLT resource info: " + ri.getClass().getName());
        } catch (PatternSyntaxException e) {
            throw new ServerPolicyException(assertion, "Couldn't compile regular expression '" + e.getPattern() + "'", e);
        }
    }

    // ---------- END STATIC FACTORY METHOD ----------


    /**
     * A ResourceGetter that finds URLs inside the message, then fetches the appropriate resource (with caching),
     * corresponding to {@link MessageUrlResourceInfo}.
     */
    private static class MessageUrlResourceGetter<UT extends UserObject> extends UrlResourceGetter<UT> {
        private final UrlFinder urlFinder;
        private final boolean allowMessagesWithoutUrl;

        // --- Instance fields ---

        private MessageUrlResourceGetter(MessageUrlResourceInfo ri,
                                         ResourceObjectFactory<UT> rof,
                                         UrlFinder urlFinder,
                                         HttpObjectCache<UT> httpObjectCache,
                                         Auditor auditor)
                throws PatternSyntaxException
        {
            super(rof, httpObjectCache, ri.makeUrlPatterns(), auditor);
            this.urlFinder = urlFinder;
            this.allowMessagesWithoutUrl = ri.isAllowMessagesWithoutUrl();

            if (rof == null || urlFinder == null) throw new NullPointerException(); // can't happen
        }

        public void close() {
            // Nothing we can do except wait for the finalizer -- userObject(s) may be in use
        }

        public UT getResource(ElementCursor message, Map vars)
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
                return fetchObject(httpObjectCache, url);
            } catch (ParseException e) {
                throw new ResourceParseException(e, url);
            }
        }
    }


    /**
     * A ResourceGetter that has a single statically-configured URL that it watches to download the latest value of
     * the resource, corresponding to {@link SingleUrlResourceInfo}.
     */
    private static class SingleUrlResourceGetter<UT extends UserObject> extends UrlResourceGetter<UT> {
        private final String url;

        private SingleUrlResourceGetter(Assertion assertion,
                                        SingleUrlResourceInfo ri,
                                        ResourceObjectFactory<UT> rof,
                                        HttpObjectCache<UT> httpObjectCache,
                                        Auditor auditor)
                throws ServerPolicyException
        {
            super(rof, httpObjectCache, ri.makeUrlPatterns(), auditor);
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

        public UT getResource(ElementCursor message, Map vars) throws IOException, ResourceParseException, GeneralSecurityException, ResourceIOException, MalformedResourceUrlException {
            String actualUrl = vars == null ? url : ExpandVariables.process(url, vars);
            try {
                return fetchObject(httpObjectCache, actualUrl);
            } catch (ParseException e) {
                throw new ResourceParseException(e, actualUrl);
            }
        }
    }


    /**
     * A ResourceGetter that owns a single statically-configured value for the resource, with no network
     * communication needed, and corresponding to {@link StaticResourceInfo}.
     */
    private static class StaticResourceGetter<UT extends UserObject> extends ResourceGetter<UT> {
        private final UT userObject;

        private StaticResourceGetter(Assertion assertion,
                                     StaticResourceInfo ri,
                                     ResourceObjectFactory<UT> rof,
                                     HttpObjectCache<UT> httpObjectCache)
                throws ServerPolicyException
        {
            super(httpObjectCache);
            String doc = ri.getDocument();
            if (doc == null) throw new ServerPolicyException(assertion, "Empty static document");
            try {
                UT userObject = rof.createResourceObject("", doc);
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

        public Pattern[] getUrlWhitelist() {
            return new Pattern[] { MATCH_ALL };
        }

        public UT getResource(ElementCursor message, Map vars) throws IOException {
            return userObject;
        }
    }
}
