/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.message;

import com.l7tech.policy.assertion.credential.PrincipalCredentials;

import java.io.*;

/**
 * @author alex
 */
public interface Request extends Message {
    /** The name of the parameter containing the PublishedService associated with this request. */
    public static final String PARAM_SERVICE                = PREFIX + ".service";

    /** The name of the parameter containing the SOAP Body Namespace URI from this request.  Currently unused. */
    public static final String PARAM_SOAP_URN               = PREFIX + ".soap.urn";

    /** The name of the parameter containing the HTTP authentication realm for this request. */
    public static final String PARAM_HTTP_AUTH_REALM        = PREFIX_HTTP + ".auth-realm";

    /** The name of the parameter containing a Map of HTTP Authorization header parameters */
    public static final String PARAM_HTTP_AUTH_PARAMS       = PREFIX_HTTP + ".auth-params";

    /** The name of the parameter containing the HTTP SOAPAction header */
    public static final String PARAM_HTTP_SOAPACTION        = PREFIX_HTTP_HEADER + ".SOAPAction";
    /** The name of the parameter containing the HTTP Authorization header */
    public static final String PARAM_HTTP_AUTHORIZATION     = PREFIX_HTTP_HEADER + ".Authorization";
    /** The name of the parameter containing the HTTP Host header */
    public static final String PARAM_HTTP_HOST              = PREFIX_HTTP_HEADER + ".Host";
    /** The name of the parameter containing the HTTP User-Agent header */
    public static final String PARAM_HTTP_USER_AGENT        = PREFIX_HTTP_HEADER + ".User-Agent";
    /** The name of the parameter containing the HTTP Via header */
    public static final String PARAM_HTTP_VIA               = PREFIX_HTTP_HEADER + ".Via";
    /** The name of the parameter containing the HTTP Content-Encoding header */
    public static final String PARAM_HTTP_CONTENT_ENCODING  = PREFIX_HTTP_HEADER + ".Content-Encoding";
    /** The name of the parameter containing the HTTP Transfer-Encoding header */
    public static final String PARAM_HTTP_TRANSFER_ENCODING = PREFIX_HTTP_HEADER + ".Transfer-Encoding";
    /** The name of the parameter containing the HTTP Accept header */
    public static final String PARAM_HTTP_ACCEPT            = PREFIX_HTTP_HEADER + ".Accept";
    /** The name of the parameter containing the HTTP Accept-Charset header */
    public static final String PARAM_HTTP_ACCEPT_CHARSET    = PREFIX_HTTP_HEADER + ".Accept-Charset";
    /** The name of the parameter containing the HTTP Accept-Encoding header */
    public static final String PARAM_HTTP_ACCEPT_ENCODING   = PREFIX_HTTP_HEADER + ".Accept-Encoding";

    /** The name of the parameter containing the X.509 client certificate associated with this <code>Request</code>'s underlying <code>HttpServletRequest</code>. */
    public static final String PARAM_HTTP_X509CERT          = "javax.servlet.request.X509Certificate";

    /** The name of the parameter containing the HTTP method (i.e. GET, POST, PUT, etc.) */
    public static final String PARAM_HTTP_METHOD            = PREFIX_HTTP + ".method";
    /** The name of the parameter containing the HTTP URI (i.e. /ssg/soap) */
    public static final String PARAM_HTTP_REQUEST_URI       = PREFIX_HTTP + ".request-uri";
    /** The name of the parameter containing the client's hostname or IP address */
    public static final String PARAM_HTTP_REMOTE_ADDR       = PREFIX_HTTP + ".remote-addr";
    /** The name of the parameter containing the server's hostname for this <code>Request</code> */
    public static final String PARAM_HTTP_SERVER_NAME       = PREFIX_HTTP + ".server-name";
    /** The name of the parameter containing the protocol used for this <code>Request</code>. */
    public static final String PARAM_HTTP_SERVER_PROTOCOL   = PREFIX_HTTP + ".server-protocol";
    /** The name of the parameter containing the TCP port on which this <code>Request</code> was served. */
    public static final String PARAM_HTTP_SERVER_PORT       = PREFIX_HTTP + ".server-port";

    /**
     * Returns a String containing the XML document of the <code>Request</code>.
     *
     * This method consumes any underlying <code>InputStream</code> if necessary
     * the first time it's called.  Subsequent calls will return the cached XML
     * document, leaving the InputStream unchanged.
     *
     * Note that SocketInputStreams are not resettable!
     *
     * @return a String containing the XML document of the Request.
     * @throws IOException
     */
    String getRequestXml() throws IOException;

    /**
     * Sets the String containing the XML document for this <code>Request</code>.
     *
     * Implementors <b>MUST</b> clear any cached data structures that are dependent on the
     * XML content of the request (e.g. DOM Documents) when this method is called.
     *
     * @param xml
     */
    void setRequestXml( String xml );

    /**
     * Returns the <code>PrincipalCredentials</code> associated with this request,
     * if any are present.
     *
     * <b>NOTE:</b> the presence of <code>PrincipalCredentials</code> does <b>NOT</b>
     * imply that the request has been successfully authenticated, <b>ONLY</b> that
     * credentials were supplied and found.
     *
     * @return The <code>PrincipalCredentials</code> associated with this request if they have already been found, or null otherwise.
     */
    PrincipalCredentials getPrincipalCredentials();

    /**
     * Sets the <code>PrincipalCredentials</code> associated with this <code>Request</code>.
     * @param pc
     */
    void setPrincipalCredentials( PrincipalCredentials pc );

    /**
     * Returns <code>true</code> if this request has been <b>SUCCESSFULLY</b> authenticated.
     *
     * <code>true</code> implies that the <code>PrincipalCredentials</code> will be non-null.
     * @return
     */
    boolean isAuthenticated();

    /**
     * Sets the authenticated flag.
     *
     * Callers <b>MUST NOT</b> set this flag to <code>true</code> unless the request has been <b>SUCCESSFULLY</b> authenticated.
     * @param authenticated
     */
    void setAuthenticated( boolean authenticated );

    /**
     * A flag indicating that the request was routed at least once.
     *
     * Implementations <b>MUST</b> return <code>false</code> until and unless <code>setRouted(true)</code> is called.
     *
     * @return
     */
    boolean isRouted();

    /**
     * Sets the routed flag, indicating that the request was routed, if true.
     * @param routed
     */
    void setRouted( boolean routed );
}
