package com.l7tech.common.http;

import com.l7tech.common.mime.MimeUtil;

/**
 * Constants useful when working with HTTP, add as required ...
 *
 * @author $Author$
 * @version $Revision$
 */
public final class HttpConstants {

    // headers
    public static final String HEADER_CONTENT_LENGTH = MimeUtil.CONTENT_LENGTH;
    public static final String HEADER_CONTENT_TYPE = MimeUtil.CONTENT_TYPE;
    public static final String HEADER_COOKIE = "Cookie";
    public static final String HEADER_SET_COOKIE = "Set-Cookie";
    public static final String HEADER_HOST = "Host";
    public static final String HEADER_LOCATION = "Location";
    public static final String HEADER_USER_AGENT = "User-Agent";
    public static final String HEADER_IF_MODIFIED_SINCE = "If-Modified-Since";
    public static final String HEADER_LAST_MODIFIED = "Last-Modified";
    public static final String HEADER_WWW_AUTHENTICATE = "WWW-Authenticate";
    public static final String HEADER_AUTHORIZATION = "Authorization";
    public static final String HEADER_ACCEPT = "Accept";
    public static final String HEADER_ACCEPT_ENCODING = "Accept-Encoding";
    public static final String HEADER_CONTENT_ENCODING = "Content-Encoding";

    // status codes
    public static final int STATUS_OK = 200;
    public static final int STATUS_NO_CONTENT = 204;
    public static final int STATUS_FOUND = 302;
    public static final int STATUS_SEE_OTHER = 303;
    public static final int STATUS_NOT_MODIFIED = 304;
    public static final int STATUS_UNAUTHORIZED = 401;
    public static final int STATUS_SERVER_ERROR = 500;
    public static final int STATUS_SERVICE_UNAVAILABLE = 503;

    public static final int STATUS_ERROR_RANGE_START = 400; // inclusive
    public static final int STATUS_ERROR_RANGE_END = 600; // non inclusive

    // methods
    public static final String METHOD_GET = "GET";
    public static final String METHOD_POST = "POST";

    // encodings
    public static final String ENCODING_UTF8 = "UTF-8";
}
