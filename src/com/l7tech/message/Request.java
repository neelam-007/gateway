/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.message;

import com.l7tech.credential.PrincipalCredentials;

import java.io.*;

/**
 * @author alex
 */
public interface Request extends Message {
    static final String PREFIX             = "com.l7tech.message.request";
    static final String PREFIX_HTTP        = PREFIX + ".http";
    static final String PREFIX_HTTP_HEADER = PREFIX_HTTP + ".header";

    public static final String PARAM_SERVICE                = PREFIX + ".service";

    public static final String PARAM_SOAP_URN               = PREFIX + ".soap.urn";

    public static final String PARAM_HTTP_AUTH_REALM        = PREFIX_HTTP + ".auth-realm";

    public static final String PARAM_HTTP_SOAPACTION        = PREFIX_HTTP_HEADER + ".soapaction";
    public static final String PARAM_HTTP_AUTHORIZATION     = PREFIX_HTTP_HEADER + ".authorization";
    public static final String PARAM_HTTP_WWWAUTHENTICATE   = PREFIX_HTTP_HEADER + ".www-authenticate";
    public static final String PARAM_HTTP_CONTENT_TYPE      = PREFIX_HTTP_HEADER + ".content-type";
    public static final String PARAM_HTTP_CONTENT_LENGTH    = PREFIX_HTTP_HEADER + ".content-length";
    public static final String PARAM_HTTP_HOST              = PREFIX_HTTP_HEADER + ".host";
    public static final String PARAM_HTTP_USER_AGENT        = PREFIX_HTTP_HEADER + ".user-agent";
    public static final String PARAM_HTTP_VIA               = PREFIX_HTTP_HEADER + ".via";
    public static final String PARAM_HTTP_CONTENT_ENCODING  = PREFIX_HTTP_HEADER + ".content-encoding";
    public static final String PARAM_HTTP_TRANSFER_ENCODING = PREFIX_HTTP_HEADER + ".transfer-encoding";
    public static final String PARAM_HTTP_ACCEPT            = PREFIX_HTTP_HEADER + ".accept";
    public static final String PARAM_HTTP_ACCEPT_CHARSET    = PREFIX_HTTP_HEADER + ".accept-charset";
    public static final String PARAM_HTTP_ACCEPT_ENCODING   = PREFIX_HTTP_HEADER + ".accept-encoding";
    public static final String PARAM_HTTP_DATE              = PREFIX_HTTP_HEADER + ".date";
    public static final String PARAM_HTTP_X509CERT          = "javax.servlet.request.X509Certificate";

    public static final String PARAM_HTTP_REQUEST_URI       = PREFIX_HTTP + ".request-uri";
    public static final String PARAM_HTTP_AUTH_TYPE         = PREFIX_HTTP + ".auth-type";
    public static final String PARAM_HTTP_REMOTE_ADDR       = PREFIX_HTTP + ".remote-addr";
    public static final String PARAM_HTTP_REQUEST_METHOD    = PREFIX_HTTP + ".request-method";
    public static final String PARAM_HTTP_SERVER_NAME       = PREFIX_HTTP + ".server-name";
    public static final String PARAM_HTTP_SERVER_PROTOCOL   = PREFIX_HTTP + ".server-protocol";
    public static final String PARAM_HTTP_SERVER_PORT       = PREFIX_HTTP + ".server-port";

    String getRequestXml() throws IOException;
    void setRequestXml( String xml );

    // TODO: Support multiple sets of credentials?
    PrincipalCredentials getPrincipalCredentials();

    void setPrincipalCredentials( PrincipalCredentials pc );

    boolean isAuthenticated();
    void setAuthenticated( boolean authenticated );

    boolean isRouted();
    void setRouted( boolean routed );
}
