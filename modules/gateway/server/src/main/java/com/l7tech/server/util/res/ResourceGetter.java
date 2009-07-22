/*
 * Copyright (C) 2005-2007 Layer 7 Technologies Inc.
 */

package com.l7tech.server.util.res;

import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.server.url.UrlResolver;
import com.l7tech.xml.ElementCursor;
import com.l7tech.policy.AssertionResourceInfo;
import com.l7tech.policy.MessageUrlResourceInfo;
import com.l7tech.policy.SingleUrlResourceInfo;
import com.l7tech.policy.StaticResourceInfo;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.server.policy.ServerPolicyException;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.CausedIOException;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * An object that is created with a ResourceInfo and can thereafter fetch a resource given a message.
 */
public abstract class ResourceGetter<R> {
    protected static final Pattern MATCH_ALL = Pattern.compile(".");
    protected final Audit audit;

    protected ResourceGetter(Audit audit) {
        this.audit = audit;
    }

    public abstract void close();

    // Some handy exceptions to ensure that information of interest is preserved until it gets back up
    // to the assertion to be audited

    /** Superclass for all exceptions that involve an external URL. */
    public static class UrlResourceException extends Exception {
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
        public ResourceParseException(String message, Throwable cause, String url) { super(message, cause, url); }
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

        public ResourceIOException(String message, String url) { super(message, url); }

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
    public abstract R getResource(ElementCursor message, Map vars)
            throws IOException, InvalidMessageException, UrlNotFoundException, MalformedResourceUrlException,
                   UrlNotPermittedException, ResourceIOException, ResourceParseException, GeneralSecurityException;

    // ---------- END INSTANCE METHOD -----------


    // ---------- BEGIN STATIC FACTORY METHOD ----------

    /**
     * Create a ResourceGetter that will obtain resource objects for messages using the given configuration
     * (AssertionResourceInfo and ResourceObjectFactory).
     *
     * @param assertion the assertion bean that owns the configuration of the type and paramaters for fetching the resource
     *                  (static, single URL, URL from message, etc).  Must not be null.
     * @param ri      instance of AssertionResourceInfo that describes how to fetch the resource.  Must not be null.
     * @param rof  strategy for converting the raw resource strings into the consumer's preferred resource object format
     *             for caching/reuse/metadata etc.  Must not be null.
     * @param urlFinder strategy for finding a URL within a message.  Must not be null if assertion.getResourceInfo()
     *                   might be MessageUrlResourceInfo.
     * @param urlResolver  strategy for converting a URL into the consumer's preferred resource object format,
     *                     possibly by downloading and parsing it
     * @return a ResourceGetter that will fetch resources for future messages.  Never null.
     * @throws NullPointerException if assertion or rof is null
     * @throws ServerPolicyException if the assertion contains no ResourceInfo
     * @throws ServerPolicyException if a ResourceGetter could not be created from the provided configuration
     * @throws IllegalArgumentException if the resource info type if MessageUrlResourceInfo but urlFinder is null
     */
    public static <R>
    ResourceGetter<R> createResourceGetter(Assertion assertion,
                                           AssertionResourceInfo ri,
                                           ResourceObjectFactory<R> rof,
                                           UrlFinder urlFinder,
                                           UrlResolver<R> urlResolver,
                                           Audit audit)
            throws ServerPolicyException
    {
        if (ri == null) throw new ServerPolicyException(assertion, "Assertion contains no ResourceInfo provided");

        // TODO move this entire factory method to an instance method of ResourceInfo, as soon as generics are
        // allowed inside the top-level policy package.
        try {
            if (ri instanceof MessageUrlResourceInfo) {
                return new MessageUrlResourceGetter<R>((MessageUrlResourceInfo)ri, urlResolver, urlFinder, audit);
            } else if (ri instanceof SingleUrlResourceInfo) {
                return new SingleUrlResourceGetter<R>(assertion, (SingleUrlResourceInfo)ri, urlResolver, audit);
            } else if (ri instanceof StaticResourceInfo) {
                return new StaticResourceGetter<R>(assertion, (StaticResourceInfo)ri, rof, audit);
            } else
                throw new ServerPolicyException(assertion, "Unsupported XSLT resource info: " + ri.getClass().getName());
        } catch (PatternSyntaxException e) {
            throw new ServerPolicyException(assertion, "Couldn't compile regular expression '" + e.getPattern() + "'", e);
        }
    }

    /**
     * Create a ResourceGetter that will throw the given exception whenever the resource is accessed.
     *
     * <p>The given exception will be wrapped in an "IOException" if it is not one of the
     * permitted exceptions for the "getResource" method..</p>
     *
     * @param exception The exception to throw.
     * @return The resource getter, never null
     */
    public static <R> ResourceGetter<R> createErrorResourceGetter( final Exception exception ) {
        return new ResourceGetter<R>(null){
            @Override
            public void close() {
            }

            @Override
            public R getResource( ElementCursor message, Map vars ) throws IOException, InvalidMessageException, UrlNotFoundException, MalformedResourceUrlException, UrlNotPermittedException, ResourceIOException, ResourceParseException, GeneralSecurityException {
                if ( exception instanceof IOException ) {
                    throw (IOException) exception;
                } else if ( exception instanceof InvalidMessageException ) {
                    throw (InvalidMessageException) exception;
                } else if ( exception instanceof UrlNotFoundException ) {
                    throw (UrlNotFoundException) exception;
                } else if ( exception instanceof MalformedResourceUrlException ) {
                    throw (MalformedResourceUrlException) exception;
                } else if ( exception instanceof UrlNotPermittedException ) {
                    throw (UrlNotPermittedException) exception;
                } else if ( exception instanceof ResourceIOException ) {
                    throw (ResourceIOException) exception;
                } else if ( exception instanceof ResourceParseException ) {
                    throw (ResourceParseException) exception;
                } else if ( exception instanceof GeneralSecurityException ) {
                    throw (GeneralSecurityException) exception;
                } else {
                    throw new CausedIOException( ExceptionUtils.getMessage( exception ), exception );
                }
            }
        };
    }

    // ---------- END STATIC FACTORY METHOD ----------


}
