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

    // status codes
    public static final int STATUS_OK = 200;
    public static final int STATUS_FOUND = 302;
    public static final int STATUS_SEE_OTHER = 303;

    // encodings
    public static final String ENCODING_UTF8 = "UTF-8";

}
