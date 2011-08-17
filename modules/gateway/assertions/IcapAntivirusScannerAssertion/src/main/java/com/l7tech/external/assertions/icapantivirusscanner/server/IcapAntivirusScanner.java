package com.l7tech.external.assertions.icapantivirusscanner.server;

import com.l7tech.external.assertions.icapantivirusscanner.IcapConnectionDetail;
import com.l7tech.util.ExceptionUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>
 * A simple ICAP client implemtation.  This client connects to the specified server and issue the 'RESPMOD' function
 * to request the server for a scan of the uploaded contents.
 * </p>
 *
 * @author Ken Diep
 */
public final class IcapAntivirusScanner {

    private static final Logger logger = Logger.getLogger(IcapAntivirusScanner.class.getName());
    private static final Pattern STATUS_LINE = Pattern.compile("(?i)((?:ICAP|HTTP).*)\\s(\\d+)\\s(.*)");
    private static final String DEFAULT_ICAP_VERSION = "ICAP/1.0";
    private static final long MAX_REUSE_PERIOD = 30000;
    private static final String DEFAULT_ICAP_METHOD = "RESPMOD";
    private static final String ENCAPSULATED_HEADER = "Encapsulated";
    private static final Pattern ENCAPSULATED_BODY_LENGTH = Pattern.compile(".*-body=(\\d+).*");
    private static final String CONTENT_LENGTH_HEADER = "Content-Length";
    private static final Pattern X_VIOLATIONS_FOUND = Pattern.compile("(?i)X-Violations?-Found.*");
    private static final String CRLF = "\r\n";
    private static ThreadLocal<Socket> socketHolder = new ThreadLocal<Socket>();
    private static ThreadLocal<Long> socketLastUsed = new ThreadLocal<Long>();
    private final IcapConnectionDetail connectionDetail;

    /**
     * Construct a new client with the given {@link IcapConnectionDetail}.
     *
     * @param connectionDetail the bean containing the server information to connect to.
     */
    public IcapAntivirusScanner(final IcapConnectionDetail connectionDetail) {
        if (connectionDetail == null) {
            throw new IllegalArgumentException("connection detail is required");
        }
        this.connectionDetail = connectionDetail;
    }

    /**
     * Disconnect and release any resources from the connected server.
     */
    public void disconnect() {
        Socket sock = socketHolder.get();
        if (sock != null && !sock.isClosed()) {
            try {
                sock.close();
            } catch (IOException e) {
                logger.warning("Error closing Socket: " + ExceptionUtils.getMessage(e));
            }
        }
    }

    private Socket getSocket() throws IOException {
        Socket sock = socketHolder.get();
        if (sock == null || sock.isClosed() || hasSocketExpired()) {
            if (sock != null) {
                try {
                    sock.close();
                } catch (IOException e) {
                    logger.warning("Error closing Socket: " + ExceptionUtils.getMessage(e));
                }
            }
            sock = new Socket();
            sock.connect(new InetSocketAddress(connectionDetail.getHostname(), connectionDetail.getPort()),
                    connectionDetail.getTimeout());
            socketHolder.set(sock);
        }
        socketLastUsed.set(System.currentTimeMillis());
        return sock;
    }

    private boolean hasSocketExpired() {
        boolean expired = true;
        final Long lastUsed = socketLastUsed.get();
        if (lastUsed != null) {
            expired = (System.currentTimeMillis() - lastUsed) > MAX_REUSE_PERIOD;
        }
        return expired;
    }

    private String getIcapUri() {
        StringBuilder sb = new StringBuilder(String.format("icap://%s:%d/%s", connectionDetail.getHostname(), connectionDetail.getPort(), connectionDetail.getServiceName()));
        if (!connectionDetail.getServiceParameters().isEmpty()) {
            sb.append("?");
            for (Map.Entry<String, String> ent : connectionDetail.getServiceParameters().entrySet()) {
                sb.append(ent.getKey()).append("=").append(ent.getValue()).append("&");
            }
            sb = sb.delete(sb.length() - 1, sb.length());
        }
        return sb.toString();
    }

    /**
     * Send a scan request to the connected anti-virus scanner via ICAP and parsed the returned response.
     *
     * @param contentName the content name of the payload.
     * @param headers     any custom (HTTP) headers to send to the server.
     * @param payload     the payload to be scanned
     * @return {@link IcapResponse} the response as received and parsed from the server.
     * @throws IOException if any I/O error(s) occur.
     */
    public IcapResponse scan(final String contentName, final Map<String, String> headers, final byte[] payload) throws IOException {
        final String requestLine = String.format("%1$s %2$s %3$s%4$s", DEFAULT_ICAP_METHOD, getIcapUri(), DEFAULT_ICAP_VERSION, CRLF);
        final String requestHeader = new StringBuilder().append("Host: ")
                .append(connectionDetail.getHostname()).append(":").append(connectionDetail.getPort()).append(CRLF)
                .append("Allow: 204").append(CRLF).toString();
        final String getRequest = String.format("GET http://scapi.foo.com/%1$s HTTP/1.0%2$s%2$s", contentName, CRLF);
        final String responseHeader = createResponseHeader(headers);

        final String encapsulatedHeader = new StringBuilder("Encapsulated: req-hdr=0, res-hdr=")
                .append(getRequest.length()).append(", res-body=").append(getRequest.length() + responseHeader.length())
                .append(CRLF).append(CRLF).toString();

        final Socket sock = getSocket();
        final OutputStream out = sock.getOutputStream();
        out.write(requestLine.getBytes());
        out.write(requestHeader.getBytes());
        out.write(encapsulatedHeader.getBytes());
        out.write(getRequest.getBytes());
        out.write(responseHeader.getBytes());
        out.write(Long.toHexString(payload.length).getBytes());
        out.write(CRLF.getBytes());
        out.write(payload);
        out.write(String.format("%1$s0%1$s%1$s", CRLF).getBytes());
        return readResponse(sock.getInputStream());
    }

    /**
     * Test a server connection by issuing an OPTIONS command.  The connection is deemed successful if the response
     * can be read and the status code is 200.
     *
     * @return true if the client has successfully issed the OPTIONS command, false otherwise.
     */
    public boolean testConnection() {
        boolean success = false;
        try {
            final Socket socket = getSocket();
            final OutputStream out = socket.getOutputStream();
            out.write(String.format("OPTIONS %1$s %2$s %3$s%3$s", getIcapUri(), DEFAULT_ICAP_VERSION, CRLF).getBytes());
            IcapResponse resp = readResponse(socket.getInputStream());
            String code = resp.getIcapHeader(IcapResponse.STATUS_CODE);
            success = "200".equals(code);
        } catch (IOException e) {
            logger.warning("Error connecting to specified ICAP server: " + e.getMessage());
        }
        return success;
    }

    private IcapResponse readResponse(final InputStream is) throws IOException {
        final Map<String, String> icap = parseStatusLine(is);
        icap.putAll(parseHeader(is));

        Map<String, String> contentBody = new HashMap<String, String>();
        InputStream stream = null;
        String code = icap.get(IcapResponse.STATUS_CODE);
        if ("200".equals(code) || "204".equals(code)) {
            final String encapsulated = icap.get(ENCAPSULATED_HEADER);
            final Matcher matcher = ENCAPSULATED_BODY_LENGTH.matcher(encapsulated);
            if (matcher.matches()) {
                final int length = Integer.parseInt(matcher.group(1).trim());
                if (length > 0) {
                    contentBody.putAll(parseStatusLine(is));
                    contentBody.putAll(parseHeader(is));
                    int contentLength = Integer.parseInt(contentBody.get(CONTENT_LENGTH_HEADER));
                    stream = getContentBody(is, contentLength);
                }
            }
        }
        return new IcapResponse(icap, contentBody, stream);
    }

    private InputStream getContentBody(final InputStream is, final int contentLength) throws IOException {
        InputStream stream = null;
        if (contentLength > 0) {
            //discard the first line, which is the content length in hex
            readLine(is);
            final byte[] content = new byte[contentLength];
            int bread = is.read(content, 0, contentLength);
            if (bread > 0) {
                stream = new ByteArrayInputStream(content);
            }
        }
        return stream;
    }

    private String readLine(final InputStream is) throws IOException {
        final StringBuilder sb = new StringBuilder();
        for (int i = is.read(); i > 0; i = is.read()) {
            if (i == '\r') continue;
            if (i == '\n') break;
            sb.append((char) i);
        }
        return sb.toString();
    }

    private Map<String, String> parseStatusLine(final InputStream is) throws IOException {
        final Map<String, String> headers = new HashMap<String, String>();

        final String line = readLine(is);
        if (line == null) return headers;
        final Matcher matcher = STATUS_LINE.matcher(line);
        if (!matcher.matches()) {
            throw new IOException("protocol error - unrecognized status line [" + line + "]");
        }
        final String protocol = matcher.group(1).trim();
        final String statusCode = matcher.group(2).trim();
        final String statusText = matcher.group(3).trim();
        headers.put("PROTOCOL", protocol);
        headers.put(IcapResponse.STATUS_CODE, statusCode);
        headers.put(IcapResponse.STATUS_TEXT, statusText);
        return headers;
    }

    private Map<String, String> parseHeader(final InputStream is) throws IOException {
        final Map<String, String> headers = new HashMap<String, String>();
        for (String line = readLine(is); !line.isEmpty(); line = readLine(is)) {
            if (X_VIOLATIONS_FOUND.matcher(line).matches()) {
                headers.putAll(parseViolationsFoundHeader(is));
            } else {
                final String[] header = line.split(":", 2);
                headers.put(header[0].trim(), header[1].trim());
            }
        }
        return headers;
    }

    private Map<String, String> parseViolationsFoundHeader(final InputStream is) {
        final Map<String, String> violationsFound = new HashMap<String, String>();
        try {
            final String filename = readLine(is);
            final String violationName = readLine(is);
            final String violationId = readLine(is);

            //disposition is a number returned by the server and it is mapped to a text description
            //as per the Symantec SDK
            String disposition = readLine(is).trim();
            if ("0".equals(disposition)) {
                disposition = "was not fixed";
            } else if ("1".equals(disposition)) {
                disposition = "was not repaired";
            } else if ("2".equals(disposition)) {
                disposition = "was not deleted";
            }

            violationsFound.put("X-Violation-Filename", filename.trim());
            violationsFound.put("X-Violation-Name", violationName.trim());
            violationsFound.put("X-Violation-ID", violationId.trim());
            violationsFound.put("X-Violation-Disposition", disposition);
        } catch (IOException e) {
            logger.warning("Error occurred while parsing violation headers: " + e.getMessage());
        }
        return violationsFound;
    }

    private String createResponseHeader(final Map<String, String> headers) {
        final StringBuilder sb = new StringBuilder("HTTP/1.0 200 OK").append(CRLF);
        if (headers != null) {
            for (Map.Entry<String, String> ent : headers.entrySet()) {
                final String key = ent.getKey().trim();
                final String value = ent.getValue().trim();
                sb.append(key).append(": ").append(value).append(CRLF);
            }
        }
        return sb.append(CRLF).toString();
    }

    /**
     * This class represents an ICAP response.
     */
    public final class IcapResponse {

        /**
         * The Status Code key.
         */
        public static final String STATUS_CODE = "STATUS_CODE";

        /**
         * The Status Value key.
         */
        public static final String STATUS_TEXT = "STATUS_TEXT";

        /**
         * The service header key.
         */
        public static final String SERVICE_NAME = "Service";

        /**
         * The violation name key.
         */
        public static final String VIOLATION_NAME = "X-Violation-Name";

        /**
         * The violation id key.
         */
        public static final String VIOLATION_ID = "X-Violation-ID";

        /**
         * The violation disposition key.
         */
        public static final String VIOLATION_DISPOSITION = "X-Violation-Disposition";

        private final Map<String, String> icapHeaders;
        private final Map<String, String> encapsulatedHeaders;
        private final InputStream bodyContent;

        private IcapResponse(final Map<String, String> icapHeaders, final Map<String, String> encapsulatedHeaders, final InputStream bodyContent) {
            this.icapHeaders = icapHeaders == null ? Collections.<String, String>emptyMap() : Collections.unmodifiableMap(icapHeaders);
            this.encapsulatedHeaders = encapsulatedHeaders == null ? Collections.<String, String>emptyMap() : Collections.unmodifiableMap(encapsulatedHeaders);
            this.bodyContent = bodyContent;
        }

        /**
         * Retrieve the content body as returned by the server (if any), it is the responsibility of the caller to
         * close the underlying stream after use.
         *
         * @return the content body as an {@link InputStream} if available, null otherwise.
         */
        public InputStream getBodyContent() {
            return bodyContent;
        }

        /**
         * @param headerName the header name to retreive.
         * @return the value of the header if it exist, null otherwise.
         */
        public String getIcapHeader(final String headerName) {
            return icapHeaders.get(headerName);
        }

        /**
         * @param headerName the encapsulated (HTTP) header name to retrieve.
         * @return the value of the header if it exist, null otherwise.
         */
        public String getEncapsulatedHeader(final String headerName) {
            return encapsulatedHeaders.get(headerName);
        }

        /**
         * @return a map containing the ICAP headers.
         */
        public Map<String, String> getIcapHeaders() {
            return Collections.unmodifiableMap(icapHeaders);
        }

        /**
         * @return a map containing the encapsulated headers.
         */
        public Map<String, String> getEncapsulatedHeader() {
            return Collections.unmodifiableMap(encapsulatedHeaders);
        }
    }

}
