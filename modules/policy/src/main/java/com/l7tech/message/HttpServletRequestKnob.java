package com.l7tech.message;

import com.l7tech.common.http.*;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.MimeUtil;
import com.l7tech.util.ConfigFactory;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.IOUtils;
import com.l7tech.util.IteratorEnumeration;
import com.l7tech.xml.soap.SoapUtil;
import org.apache.commons.lang.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.security.cert.X509Certificate;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implementation of {@link HttpRequestKnob} that knows how to obtain the HTTP request transport metadata
 * from a servlet request.
 */
public class HttpServletRequestKnob implements HttpRequestKnob {
    private static final Logger logger = Logger.getLogger(HttpServletRequestKnob.class.getName());

    /** parameters found in the URL query string. */
    private Map<String, String[]> queryParams;
    /** parameters found in the request message body. */
    private Map<String, String[]> requestBodyParams;
    /** all request parameters; i.e., union of {@link #queryParams} and {@link #requestBodyParams}. */
    private Map<String, String[]> allParams;

    private final HttpServletRequest request;
    private final HttpMethod method;

    private static final Map<String, HttpMethod> nameMap = Collections.unmodifiableMap(new TreeMap<String, HttpMethod>(String.CASE_INSENSITIVE_ORDER) {{
        for (HttpMethod httpMethod : HttpMethod.values()) {
            put(httpMethod.name(), httpMethod);
        }
    }});

    private URL url;
    private static final String SERVLET_REQUEST_ATTR_X509CERTIFICATE = "javax.servlet.request.X509Certificate";
    private static final String SERVLET_REQUEST_ATTR_CONNECTION_ID = "com.l7tech.server.connectionIdentifierObject";
    private static final String PARAM_MAX_FORM_POST = "io.httpParamsMaxFormPostBytes";
    private static final boolean VALIDATE_PARAMETERS = ConfigFactory.getBooleanProperty( "com.l7tech.message.httpParamsValidate", true );

    public HttpServletRequestKnob(HttpServletRequest request) {
        this.request = request;
        HttpMethod method = nameMap.get(request.getMethod());
        if (method == null) method = HttpMethod.OTHER;
        this.method = method;
    }

    @Override
    public HttpCookie[] getCookies() {
        return CookieUtils.fromServletCookies(request.getCookies(), false);
    }

    @Override
    public HttpMethod getMethod() {
        return method;
    }

    @Override
    public String getMethodAsString() {
        return request.getMethod();
    }

    @Override
    public String getRequestUri() {
        return request.getRequestURI();
    }

    @Override
    public String getParameter(String name) throws IOException {
        if (queryParams == null || requestBodyParams == null) {
            collectParameters();
        }
        String[] values = allParams.get(name);
        if (values != null && values.length >= 1) {
            return values[0];
        } else {
            return null;
        }
    }

    private void collectParameters() throws IOException {
        String q = getQueryString();
        if (q == null || q.length() == 0) {
            queryParams = Collections.emptyMap();
        } else {
            final TreeMap<String, String[]> newmap = new TreeMap<String, String[]>(String.CASE_INSENSITIVE_ORDER);
            try {
                newmap.putAll(ParameterizedString.parseQueryString(q, VALIDATE_PARAMETERS));
            } catch (IllegalArgumentException iae) {
                logger.warning( "Ignoring query string parameters due to invalid content " + ExceptionUtils.getMessage(iae) );
            }
            this.queryParams = Collections.unmodifiableMap(newmap);
        }

        // Check for PUT or POST; otherwise there can't be body params
        if ( !"POST".equals(request.getMethod()) &&
             !"PUT".equals(request.getMethod()) ) {
            nobody();
            return;
        }


        ContentTypeHeader ctype = null;

        //check if the Content-Type header is present
        if(StringUtils.isNotBlank(request.getHeader(MimeUtil.CONTENT_TYPE))) {
            // If it's not an HTTP form post, don't go looking for trouble
            ctype = ContentTypeHeader.parseValue(request.getHeader(MimeUtil.CONTENT_TYPE));
        }
        //Any HTTP/1.1 message containing an entity-body SHOULD include a Content-Type header field defining
        // the media type of that body. If and only if the media type is not given by a Content-Type field,
        // the recipient MAY attempt to guess the media type via inspection of its content and/or the name
        // extension(s) of the URI used to identify the resource. If the media type remains unknown,
        // the recipient SHOULD treat it as type "application/octet-stream".
        if (ctype != null && !ctype.matches(ContentTypeHeader.APPLICATION_X_WWW_FORM_URLENCODED)) {
            // This stanza is copied because we don't want to parse the Content-Type unnecessarily
            nobody();
            return;
        }

        // Check size
        int len = request.getContentLength();
        int maxLen = ConfigFactory.getIntProperty( PARAM_MAX_FORM_POST, 512 * 1024 );
        if (len > maxLen) throw new IOException(MessageFormat.format("Request too long (Content-Length = {0} bytes)", len));
        if (len == -1) {
            nobody();
            return;
        }

        Charset enc = ctype != null? ctype.getEncoding():ContentTypeHeader.OCTET_STREAM_DEFAULT.getEncoding();
        byte[] buf = IOUtils.slurpStream(request.getInputStream());
        String blob = new String(buf, enc);
        requestBodyParams = new TreeMap<String, String[]>(String.CASE_INSENSITIVE_ORDER);
        try {
            ParameterizedString.parseParameterString(requestBodyParams, blob, VALIDATE_PARAMETERS);
        } catch (IllegalArgumentException iae) {
            logger.warning( "Ignoring form parameters due to invalid content " + ExceptionUtils.getMessage(iae) );
        }

        if (queryParams.isEmpty() && requestBodyParams.isEmpty()) {
            // Nothing left to do
            allParams = Collections.emptyMap();
            return;
        }

        // Combines queryParams and requestBodyParams into allParams.
        Map<String, String[]> allParams = new TreeMap<String, String[]>(String.CASE_INSENSITIVE_ORDER);
        allParams.putAll(queryParams);
        for (String name : requestBodyParams.keySet()) {
            String[] bodyValues = requestBodyParams.get(name);
            String[] queryValues = queryParams.get(name);
            if (queryValues == null) {
                allParams.put(name, bodyValues);
            } else {
                String[] allValues = new String[queryValues.length + bodyValues.length];
                System.arraycopy(queryValues, 0, allValues, 0, queryValues.length);
                System.arraycopy(bodyValues, 0, allValues, queryValues.length, bodyValues.length);
                allParams.put(name, allValues);
            }
        }
        this.allParams = Collections.unmodifiableMap(allParams);
    }

    /**
     * Signal not to try to parse parameters again -- request is not an acceptable form post
     */
    private void nobody() {
        requestBodyParams = Collections.emptyMap();
        allParams = queryParams;
    }

    @Override
    public String getQueryString() {
        return request.getQueryString();
    }

    /**
     * @return this request's parameters
     */
    @Override
    public Map<String, String[]> getParameterMap() throws IOException {
        if (allParams == null) collectParameters();
        return allParams;
    }

    @Override
    public String[] getParameterValues(String s) throws IOException {
        if (allParams == null) collectParameters();
        return allParams.get(s);
    }

    @Override
    public Enumeration<String> getParameterNames() throws IOException {
        if (allParams == null) collectParameters();
        return new IteratorEnumeration<String>(allParams.keySet().iterator());
    }

    /**
     * @return the Map&lt;String, String[]&gt; of parameters found in the URL query string.
     * @since SecureSpan 3.7
     * @throws java.io.IOException if unable to read parameters
     */
    public Map<String, String[]> getQueryParameterMap() throws IOException {
        if (queryParams == null) collectParameters();
        return queryParams;
    }

    /**
     * @return the Map&lt;String, String[]&gt; of parameters found in the request message body.
     * @since SecureSpan 3.7
     * @throws java.io.IOException if unable to read parameters
     */
    public Map<String, String[]> getRequestBodyParameterMap() throws IOException {
        if (requestBodyParams == null) collectParameters();
        return requestBodyParams;
    }

    @Override
    public String getRequestUrl() {
        return request.getRequestURL().toString(); // NPE here if servlet is bogus
    }

    @Override
    public URL getRequestURL() {
        if (url != null)
            return url;
        try {
            return url = new URL(request.getRequestURL().toString());
        } catch (MalformedURLException e) {
            throw new RuntimeException("HttpServletRequest had invalid URL: " + ExceptionUtils.getMessage(e), e);
        }
    }

    @Override
    public long getDateHeader(String name) throws ParseException {
        return request.getDateHeader(name);
    }

    @Override
    public int getIntHeader(String name) {
        try {
            return request.getIntHeader(name);
        } catch (NumberFormatException nfe) {
            return -1;
        }
    }

    @Override
    public String getHeaderFirstValue(String name) {
        return request.getHeader(name);
    }

    @Override
    public String getHeaderSingleValue(String name) throws IOException {
        Enumeration en = request.getHeaders(name);
        if (en.hasMoreElements()) {
            String value = (String)en.nextElement();
            if (en.hasMoreElements())
                throw new IOException("More than one value found for HTTP request header " + name);
            return value;
        }
        return null;
    }

    @Override
    public String[] getHeaderNames() {
        Enumeration names = request.getHeaderNames();
        List<String> out = new ArrayList<String>();
        while (names.hasMoreElements()) {
            String name = (String)names.nextElement();
            out.add(name);
        }
        return out.toArray(new String[out.size()]);
    }

    @Override
    public String[] getHeaderValues(String name) {
        Enumeration values = request.getHeaders(name);
        List<String> out = new ArrayList<String>();
        while (values.hasMoreElements()) {
            String value = (String)values.nextElement();
            out.add(value);
        }
        return out.toArray(new String[out.size()]);
    }

    @Override
    public X509Certificate[] getClientCertificate() throws IOException {
        Object param = request.getAttribute(SERVLET_REQUEST_ATTR_X509CERTIFICATE);
        if (param == null)
            return null;
        if (param instanceof X509Certificate)
            return new X509Certificate[] { (X509Certificate)param };
        if (param instanceof X509Certificate[])
            return (X509Certificate[])param;
        throw new IOException("Request X509Certificate was unsupported type " + param.getClass());
    }

    @Override
    public Object getConnectionIdentifier() {
        return request.getAttribute(SERVLET_REQUEST_ATTR_CONNECTION_ID);
    }

    @Override
    public boolean isSecure() {
        return request.isSecure();
    }

    @Override
    public String getRemoteAddress() {
        return request.getRemoteAddr();
    }

    @Override
    public String getRemoteHost() {
        return request.getRemoteHost();
    }

    @Override
    public int getRemotePort() {
        return request.getRemotePort();
    }

    @Override
    public String getLocalAddress() {
        return request.getLocalAddr();
    }

    @Override
    public String getLocalHost() {
        return request.getLocalName();
    }

    @Override
    public int getLocalPort() {
        return request.getServerPort();
    }

    @Override
    public int getLocalListenerPort() {
        return request.getLocalPort();
    }

    /** @return the raw HttpServletRequest instance. */
    public HttpServletRequest getHttpServletRequest() {
        return request;
    }

    private static Pattern SOAP_1_2_ACTION_PATTERN = Pattern.compile(";\\s*action=([^;]+)(?:;|$)");

    @Override
    public String getSoapAction() throws IOException {
        String soapAction = getHeaderSingleValue(SoapUtil.SOAPACTION);
        if(soapAction == null || soapAction.trim().length() == 0) {
            String contentType = getHeaderSingleValue(HttpConstants.HEADER_CONTENT_TYPE);
            if(contentType != null) {
                Matcher m = SOAP_1_2_ACTION_PATTERN.matcher(contentType);
                if(m.find()) {
                    soapAction = m.group(1);
                }
            }
        }

        return soapAction;
    }
}
