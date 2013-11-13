package com.l7tech.server;

import com.l7tech.common.http.*;
import com.l7tech.common.http.prov.apache.components.HttpComponentsClient;
import com.l7tech.common.io.PermissiveX509TrustManager;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.policy.AssertionRegistry;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.wsp.WspConstants;
import com.l7tech.util.IOUtils;
import com.l7tech.util.NamespaceContextImpl;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.*;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509TrustManager;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.security.SecureRandom;
import java.util.*;

import static org.junit.Assert.*;

/**
 * Parent class for integration tests relevant to http routing.
 * <p/>
 * Change the {@link #BASE_URL} and {@link #PASSWORD_AUTHENTICATION} be relevant for the gateway you want to test against.
 */
public abstract class HttpRoutingIntegrationTest {
    protected static final String BASE_URL = "localhost";
    protected static final PasswordAuthentication PASSWORD_AUTHENTICATION = new PasswordAuthentication("admin", "password".toCharArray());
    private static final String ECHO_HEADERS_SERVICE_RESOURCE = "com/l7tech/server/wsman/createEchoHeadersService.xml";
    private static final String BASIC_ROUTING_SERVICE_RESOURCE = "com/l7tech/server/wsman/createBasicRoutingService.xml";
    private static final String SERVICE_TEMPLATE_RESOURCE = "com/l7tech/server/wsman/createServiceMessageTemplate.xml";
    private static final String DELETE_SERVICE_RESOURCE = "com/l7tech/server/wsman/deleteService.xml";
    protected static final String ECHO_HEADERS_URL = "http://" + BASE_URL + ":8080/echoHeaders";
    private static final String CREATE_ACTION = "http://schemas.xmlsoap.org/ws/2004/09/transfer/Create";
    private static final String DELETE_ACTION = "http://schemas.xmlsoap.org/ws/2004/09/transfer/Delete";
    protected static final String SERVICENAME = "===SERVICENAME===";
    protected static final String SERVICEURL = "===SERVICEURL===";
    protected static final String SERVICEPOLICY = "===SERVICEPOLICY===";
    protected static final String L7_USER_AGENT = "Layer7-SecureSpan-Gateway";
    protected static final String APACHE_USER_AGENT = "Apache-HttpClient/4.2.5 (java 1.5)";
    protected static final String APACHE_SERVER = "Apache-Coyote/1.1";
    protected static final String KEEP_ALIVE = "Keep-Alive";
    private static GenericHttpClient client;
    private static XPath xPath;
    private static List<String> classLevelCreatedServiceIds;
    protected static final String SOAP_BODY = "<s:Envelope xmlns:s=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\">\n" +
            "\t<s:Header>\n" +
            "\t\t<wsa:Action s:mustUnderstand=\"true\">bridgeSoapAction</wsa:Action>\n" +
            "\t\t<wsa:To s:mustUnderstand=\"true\">http://127.0.0.1:8080/wsman</wsa:To>\n" +
            "\t\t<wsa:MessageID s:mustUnderstand=\"true\">uuid:b2794ffb-7d39-1d39-8002-481688002100</wsa:MessageID>\n" +
            "\t\t<wsa:ReplyTo>\n" +
            "\t\t\t<wsa:Address>http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</wsa:Address>\n" +
            "\t\t</wsa:ReplyTo>\n" +
            "\t</s:Header>\n" +
            "\t<s:Body>\n" +
            "\t</s:Body>\n" +
            "</s:Envelope>";
    protected static final String BRIDGE_USER_AGENT = "L7 Bridge; Protocol v2.0";
    protected static final String SOAP_CONTENT_TYPE = "application/soap+xml;charset=UTF-8";
    protected List<String> testLevelCreatedServiceIds;

    @BeforeClass
    public static void setupClass() throws Exception {
        client = new HttpComponentsClient();
        xPath = XPathFactory.newInstance().newXPath();
        final HashMap<String, String> map = new HashMap<>();
        map.put("env", "http://www.w3.org/2003/05/soap-envelope");
        map.put("mdo", "http://schemas.wiseman.dev.java.net/metadata/messagetypes");
        map.put("mex", "http://schemas.xmlsoap.org/ws/2004/09/mex");
        map.put("wsa", "http://schemas.xmlsoap.org/ws/2004/08/addressing");
        map.put("wse", "http://schemas.xmlsoap.org/ws/2004/08/eventing");
        map.put("wsen", "http://schemas.xmlsoap.org/ws/2004/09/enumeration");
        map.put("wsman", "http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd");
        map.put("wsmeta", "http://schemas.dmtf.org/wbem/wsman/1/wsman/version1.0.0.a/default-addressing-model.xsd");
        map.put("wxf", "http://schemas.xmlsoap.org/ws/2004/09/transfer");
        map.put("xs", "http://www.w3.org/2001/XMLSchema");
        xPath.setNamespaceContext(new NamespaceContextImpl(map));
        classLevelCreatedServiceIds = new ArrayList<>();
        classLevelCreatedServiceIds.add(createService(ECHO_HEADERS_SERVICE_RESOURCE));
        classLevelCreatedServiceIds.add(createRoutingService(BASIC_ROUTING_SERVICE_RESOURCE, "http://" + BASE_URL + ":8080/echoHeaders"));
        final AssertionRegistry assertionRegistry = new AssertionRegistry();
        assertionRegistry.setApplicationContext(null);
        assertionRegistry.registerAssertion(HardcodedResponseAssertion.class);
        WspConstants.setTypeMappingFinder(assertionRegistry);
    }

    @AfterClass
    public static void teardownClass() throws Exception {
        for (final String createdServiceId : classLevelCreatedServiceIds) {
            deleteService(createdServiceId);
        }
    }

    @Before
    public void setup() {
        testLevelCreatedServiceIds = new ArrayList<>();
    }

    @After
    public void teardown() throws Exception {
        for (final String createdServiceId : testLevelCreatedServiceIds) {
            deleteService(createdServiceId);
        }
    }

    protected static String createServiceFromTemplate(final Map<String, String> toReplace) throws Exception {
        final InputStream resourceStream = HttpRoutingRequestIntegrationTest.class.getClassLoader().getResourceAsStream(SERVICE_TEMPLATE_RESOURCE);
        String request = new String(IOUtils.slurpStream(resourceStream));
        for (final Map.Entry<String, String> entry : toReplace.entrySet()) {
            request = request.replace(entry.getKey(), entry.getValue());
        }
        final GenericHttpResponse response = callWsman(request, CREATE_ACTION);
        final Document doc = XmlUtil.parse(response.getInputStream());
        final String id = getValue(doc, "//wsman:Selector");
        System.out.println("Created Service from resource " + SERVICE_TEMPLATE_RESOURCE + " with id=" + id);
        return id;
    }

    private static String createService(final String resource) throws Exception {
        final InputStream resourceStream = HttpRoutingRequestIntegrationTest.class.getClassLoader().getResourceAsStream(resource);
        final String request = new String(IOUtils.slurpStream(resourceStream));
        final GenericHttpResponse response = callWsman(request, CREATE_ACTION);
        final Document doc = XmlUtil.parse(response.getInputStream());
        final String id = getValue(doc, "//wsman:Selector");
        System.out.println("Created Service from resource " + resource + " with id=" + id);
        return id;
    }

    private static String createRoutingService(final String resource, final String routeUrl) throws Exception {
        final InputStream resourceStream = HttpRoutingRequestIntegrationTest.class.getClassLoader().getResourceAsStream(resource);
        final String baseRequest = new String(IOUtils.slurpStream(resourceStream));
        final String request = baseRequest.replaceAll("===ROUTEURL===", routeUrl);
        final GenericHttpResponse response = callWsman(request, CREATE_ACTION);
        final Document doc = XmlUtil.parse(response.getInputStream());
        final String id = getValue(doc, "//wsman:Selector");
        System.out.println("Created Service from resource " + resource + " with id=" + id);
        return id;
    }

    private static void deleteService(@NotNull final String id) throws Exception {
        final InputStream resourceStream = HttpRoutingRequestIntegrationTest.class.getClassLoader().getResourceAsStream(DELETE_SERVICE_RESOURCE);
        final String baseRequest = new String(IOUtils.slurpStream(resourceStream));
        final String request = baseRequest.replaceAll("===SERVICEID===", id);
        callWsman(request, DELETE_ACTION);
        System.out.println("Deleted Service with id=" + id);
    }

    private static String getValue(final Document document, final String xpathExpression) throws Exception {
        final XPathExpression expression = xPath.compile(xpathExpression);
        final Node node = (Node) expression.evaluate(document, XPathConstants.NODE);
        return node.getTextContent();
    }

    protected static GenericHttpResponse sendRequest(final GenericHttpRequestParams params, final HttpMethod method, final String body) throws Exception {
        System.out.println("======Sending request to " + params.getTargetUrl() + "======");
        final GenericHttpRequest request = client.createRequest(method, params);
        if (body != null) {
            request.setInputStream(new ByteArrayInputStream(body.getBytes()));
        }
        return request.getResponse();
    }

    private static GenericHttpResponse callWsman(final String requestBody, final String soapAction) throws Exception {
        final GenericHttpRequestParams params = new GenericHttpRequestParams(new URL("https://" + BASE_URL + ":8443/wsman"));
        params.setPasswordAuthentication(PASSWORD_AUTHENTICATION);
        params.setSslSocketFactory(getSSLSocketFactory());
        params.addExtraHeader(new GenericHttpHeader("Content-Type", "application/soap+xml;charset=UTF-8"));
        params.addExtraHeader(new GenericHttpHeader("SOAPAction", soapAction));
        final GenericHttpResponse response = sendRequest(params, HttpMethod.POST, requestBody);
        if (response.getStatus() != 200) {
            System.out.println("WSMAN call failed with response status=" + response.getStatus());
            System.out.println(new String(IOUtils.slurpStream(response.getInputStream())));
            fail("WSMAN call failed with response status=" + response.getStatus());
        }
        return response;
    }

    public static SSLSocketFactory getSSLSocketFactory() throws Exception {
        final SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(new KeyManager[]{}, new X509TrustManager[]{new PermissiveX509TrustManager()}, new SecureRandom());
        return sslContext.getSocketFactory();
    }

    protected List<Assertion> assertionList(final Assertion... assertions) {
        final List<Assertion> assertionList = new ArrayList<>();
        for (final Assertion assertion : assertions) {
            assertionList.add(assertion);
        }
        return assertionList;
    }

    protected AddHeaderAssertion createAddHeaderAssertion(final String name, final String value) {
        return createAddHeaderAssertion(TargetMessageType.REQUEST, name, value, false);
    }

    protected AddHeaderAssertion createAddHeaderAssertion(final String name, final String value, final boolean removeExisting) {
        return createAddHeaderAssertion(TargetMessageType.REQUEST, name, value, removeExisting);
    }

    protected AddHeaderAssertion createAddHeaderAssertion(final TargetMessageType target, final String name, final String value) {
        return createAddHeaderAssertion(target, name, value, false);
    }

    protected AddHeaderAssertion createAddHeaderAssertion(final TargetMessageType target, final String name, final String value, final boolean removeExisting) {
        final AddHeaderAssertion addHeaderAssertion = new AddHeaderAssertion();
        addHeaderAssertion.setTarget(target);
        addHeaderAssertion.setHeaderName(name);
        addHeaderAssertion.setHeaderValue(value);
        addHeaderAssertion.setRemoveExisting(removeExisting);
        return addHeaderAssertion;
    }

    protected HardcodedResponseAssertion createEchoHeadersHardcodedResponseAssertion() {
        return createHardcodedResponseAssertion("text/plain", "${request.http.allheadervalues}");
    }

    protected HardcodedResponseAssertion createHardcodedResponseAssertion(final String body) {
        return createHardcodedResponseAssertion(null, body);
    }

    protected HardcodedResponseAssertion createHardcodedResponseAssertion(final String contentType, final String body) {
        final HardcodedResponseAssertion assertion = new HardcodedResponseAssertion();
        if (contentType != null) {
            assertion.setResponseContentType(contentType);
        }
        assertion.responseBodyString(body);
        return assertion;
    }

    protected HttpRoutingAssertion createRouteAssertion(final String url, final boolean forwardAllRequestHeaders) {
        return createRouteAssertion(url, forwardAllRequestHeaders, (HttpPassthroughRule[]) null);
    }

    protected HttpRoutingAssertion createRouteAssertion(final String url, final boolean forwardAllRequestHeaders, final Collection<HttpPassthroughRule> requestRules) {
        return createRouteAssertion(url, forwardAllRequestHeaders, requestRules == null ? null : requestRules.toArray(new HttpPassthroughRule[requestRules.size()]));
    }

    protected HttpRoutingAssertion createRouteAssertion(final String url, final boolean forwardAllRequestHeaders, final HttpPassthroughRule... requestRules) {
        return createRouteAssertion(url, forwardAllRequestHeaders, null, requestRules);
    }

    protected HttpRoutingAssertion createRouteAssertion(final String url, final boolean forwardAllRequestHeaders, final String responseVar, final HttpPassthroughRule... requestRules) {
        final HttpRoutingAssertion routeAssertion = new HttpRoutingAssertion();
        routeAssertion.setProtectedServiceUrl(url);
        routeAssertion.getRequestHeaderRules().setForwardAll(forwardAllRequestHeaders);
        if (requestRules != null) {
            routeAssertion.getRequestHeaderRules().setRules(requestRules);
        }
        if (responseVar != null) {
            routeAssertion.setResponseMsgDest(responseVar);
        }
        return routeAssertion;
    }

    protected HttpRoutingAssertion createResponseRouteAssertion(final String url, final boolean forwardAllResponseHeaders, final Collection<HttpPassthroughRule> responseRules) {
        return createResponseRouteAssertion(url, forwardAllResponseHeaders, responseRules == null ? null : responseRules.toArray(new HttpPassthroughRule[responseRules.size()]));
    }

    protected HttpRoutingAssertion createResponseRouteAssertion(final String url, final boolean forwardAllResponseHeaders) {
        return createResponseRouteAssertion(url, forwardAllResponseHeaders, (HttpPassthroughRule[]) null);
    }

    protected HttpRoutingAssertion createResponseRouteAssertion(final String url, final boolean forwardAllResponseHeaders, final HttpPassthroughRule... responseRules) {
        final HttpRoutingAssertion routeAssertion = new HttpRoutingAssertion();
        routeAssertion.setProtectedServiceUrl(url);
        routeAssertion.getResponseHeaderRules().setForwardAll(forwardAllResponseHeaders);
        if (responseRules != null) {
            routeAssertion.getResponseHeaderRules().setRules(responseRules);
        }
        return routeAssertion;
    }

    protected BridgeRoutingAssertion createResponseBridgeRouteAssertion(final String url, final boolean forwardAllResponseHeaders) {
        return createResponseBridgeRouteAssertion(url, false, forwardAllResponseHeaders, (HttpPassthroughRule[]) null);
    }

    protected BridgeRoutingAssertion createResponseBridgeRouteAssertion(final String url, final boolean forwardAllResponseHeaders, final List<HttpPassthroughRule> responseRules) {
        return createResponseBridgeRouteAssertion(url, false, forwardAllResponseHeaders, responseRules.toArray(new HttpPassthroughRule[responseRules.size()]));
    }

    protected BridgeRoutingAssertion createResponseBridgeRouteAssertion(final String url, final boolean forwardAllResponseHeaders, final HttpPassthroughRule... responseRules) {
        return createResponseBridgeRouteAssertion(url, false, forwardAllResponseHeaders, responseRules);
    }

    protected BridgeRoutingAssertion createResponseBridgeRouteAssertion(final String url, final boolean useSsl, final boolean forwardAllResponseHeaders, final HttpPassthroughRule... responseRules) {
        final BridgeRoutingAssertion bridge = new BridgeRoutingAssertion();
        bridge.setProtectedServiceUrl(url);
        bridge.setUseSslByDefault(useSsl);
        bridge.getResponseHeaderRules().setForwardAll(forwardAllResponseHeaders);
        if (responseRules != null) {
            bridge.getResponseHeaderRules().setRules(responseRules);
        }
        return bridge;
    }

    protected BridgeRoutingAssertion createBridgeRouteAssertion(final String url, final boolean useSsl, final boolean forwardAllRequestHeaders, final String responseVar, final HttpPassthroughRule... requestRules) {
        final BridgeRoutingAssertion bridge = new BridgeRoutingAssertion();
        bridge.setProtectedServiceUrl(url);
        bridge.setUseSslByDefault(useSsl);
        bridge.getRequestHeaderRules().setForwardAll(forwardAllRequestHeaders);
        if (responseVar != null) {
            bridge.setResponseMsgDest(responseVar);
        }
        if (requestRules != null) {
            bridge.getRequestHeaderRules().setRules(requestRules);
        }
        return bridge;
    }

    protected BridgeRoutingAssertion createBridgeRouteAssertion(final String url, final boolean forwardAllRequestHeaders, final HttpPassthroughRule... requestRules) {
        return createBridgeRouteAssertion(url, false, forwardAllRequestHeaders, null, requestRules);
    }

    protected BridgeRoutingAssertion createBridgeRouteAssertion(final String url, final boolean forwardAllRequestHeaders, final List<HttpPassthroughRule> requestRules) {
        return createBridgeRouteAssertion(url, false, forwardAllRequestHeaders, null, requestRules.toArray(new HttpPassthroughRule[requestRules.size()]));
    }


    protected BridgeRoutingAssertion createBridgeRouteAssertion(final String url, final boolean forwardAllRequestHeaders) {
        return createBridgeRouteAssertion(url, false, forwardAllRequestHeaders, null, (HttpPassthroughRule[]) null);
    }

    /**
     * Parses headers from the 'all' XML element in the response body.
     */
    protected Map<String, Collection<String>> parseHeaders(final String responseBody) throws IOException {
        final Map<String, Collection<String>> routedRequestHeaders = new HashMap<>();
        final String allHeadersString = responseBody.substring(responseBody.indexOf("<all>") + "<all>".length(), responseBody.indexOf("</all>"));
        final String[] headersFromBody = StringUtils.split(allHeadersString, ",");
        String previousHeaderName = null;
        for (final String headerFromBody : headersFromBody) {
            if (headerFromBody.contains(":")) {
                final int colonIndex = headerFromBody.indexOf(":");
                final String name = headerFromBody.substring(0, colonIndex).trim();
                final String value = headerFromBody.substring(colonIndex + 1, headerFromBody.length()).trim();
                if (!routedRequestHeaders.containsKey(name)) {
                    routedRequestHeaders.put(name, new ArrayList<String>());
                }
                routedRequestHeaders.get(name).add(value);
                previousHeaderName = name;
            } else if (previousHeaderName != null) {
                // most likely a multi-valued header
                routedRequestHeaders.get(previousHeaderName).add(headerFromBody.trim());
            }
        }
        return routedRequestHeaders;
    }

    /**
     * Parses headers from the 'byName' XML element in the response body.
     */
    protected Map<String, Collection<String>> parseHeadersByName(final String responseBody) throws IOException {
        final Map<String, Collection<String>> routedRequestHeaders = new HashMap<>();
        final String allHeadersString = responseBody.substring(responseBody.indexOf("<byName>") + "<byName>".length(), responseBody.indexOf("</byName>"));
        final String[] headersFromBody = StringUtils.split(allHeadersString, "\n");
        for (final String headerFromBody : headersFromBody) {
            if (headerFromBody.contains(":")) {
                final int colonIndex = headerFromBody.indexOf(":");
                final String name = headerFromBody.substring(0, colonIndex).trim();
                final String value = headerFromBody.substring(colonIndex + 1, headerFromBody.length()).trim();
                if (!routedRequestHeaders.containsKey(name)) {
                    routedRequestHeaders.put(name, new ArrayList<String>());
                }
                routedRequestHeaders.get(name).add(value);
            }
        }
        return routedRequestHeaders;
    }

    protected void assertHeaderValues(final Map<String, Collection<String>> headers, final String headerName, final String... headerValues) {
        final Collection<String> foundValues = headers.get(headerName);
        assertEquals(headerValues.length, foundValues.size());
        for (final String headerValue : headerValues) {
            assertTrue(foundValues.contains(headerValue));
        }
    }

    protected Map<String, Collection<String>> getResponseHeaders(final GenericHttpResponse response) {
        final Map<String, Collection<String>> headersMap = new HashMap<>();
        for (final HttpHeader httpHeader : response.getHeaders().toArray()) {
            if (!headersMap.containsKey(httpHeader.getName())) {
                headersMap.put(httpHeader.getName(), new ArrayList<String>());
            }
            headersMap.get(httpHeader.getName()).add(httpHeader.getFullValue());
        }
        return headersMap;
    }

    protected String printResponseDetails(final GenericHttpResponse response) throws IOException {
        System.out.println("Received response with status: " + response.getStatus());
        System.out.println("Response headers:");
        for (final HttpHeader header : response.getHeaders().toArray()) {
            System.out.println(header.getName() + ":" + header.getFullValue());
        }
        System.out.println("Response body:");
        final String responseBody = new String(IOUtils.slurpStream(response.getInputStream()));
        System.out.println(responseBody);
        return responseBody;
    }

    protected GenericHttpRequestParams createSoapParams(final String url) throws MalformedURLException {
        final GenericHttpRequestParams params = new GenericHttpRequestParams(new URL(url));
        params.addExtraHeader(new GenericHttpHeader("SOAPAction", "testSoapAction"));
        params.addExtraHeader(new GenericHttpHeader("Content-Type", SOAP_CONTENT_TYPE));
        return params;
    }
}
