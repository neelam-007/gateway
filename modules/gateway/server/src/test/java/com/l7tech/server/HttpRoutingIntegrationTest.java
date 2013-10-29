package com.l7tech.server;

import com.l7tech.common.http.*;
import com.l7tech.common.http.prov.apache.components.HttpComponentsClient;
import com.l7tech.common.io.PermissiveX509TrustManager;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.policy.AssertionRegistry;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.wsp.WspConstants;
import com.l7tech.policy.wsp.WspWriter;
import com.l7tech.util.HexUtils;
import com.l7tech.util.IOUtils;
import com.l7tech.util.NamespaceContextImpl;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
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
import java.net.PasswordAuthentication;
import java.net.URL;
import java.security.SecureRandom;
import java.util.*;

import static org.junit.Assert.*;

/**
 * Integration tests for http routing.
 * <p/>
 * Change the {@link #BASE_URL} and {@link #PASSWORD_AUTHENTICATION} be relevant for the gateway you want to test against.
 */
@Ignore
public class HttpRoutingIntegrationTest {
    private static final String BASE_URL = "localhost";
    private static final PasswordAuthentication PASSWORD_AUTHENTICATION = new PasswordAuthentication("admin", "password".toCharArray());
    private static final String ECHO_HEADERS_SERVICE_RESOURCE = "com/l7tech/server/wsman/createEchoHeadersService.xml";
    private static final String BASIC_ROUTING_SERVICE_RESOURCE = "com/l7tech/server/wsman/createBasicRoutingService.xml";
    private static final String SERVICE_TEMPLATE_RESOURCE = "com/l7tech/server/wsman/createServiceMessageTemplate.xml";
    private static final String DELETE_SERVICE_RESOURCE = "com/l7tech/server/wsman/deleteService.xml";
    private static final String CREATE_ACTION = "http://schemas.xmlsoap.org/ws/2004/09/transfer/Create";
    private static final String DELETE_ACTION = "http://schemas.xmlsoap.org/ws/2004/09/transfer/Delete";
    private static GenericHttpClient client;
    private static XPath xPath;
    private static List<String> createdServiceIds;
    private static final String SERVICENAME = "===SERVICENAME===";
    private static final String SERVICEURL = "===SERVICEURL===";
    private static final String SERVICEPOLICY = "===SERVICEPOLICY===";

    @BeforeClass
    public static void setup() throws Exception {
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
        createdServiceIds = new ArrayList<>();
        createdServiceIds.add(createService(ECHO_HEADERS_SERVICE_RESOURCE));
        createdServiceIds.add(createRoutingService(BASIC_ROUTING_SERVICE_RESOURCE, "http://" + BASE_URL + ":8080/echoHeaders"));
        final AssertionRegistry assertionRegistry = new AssertionRegistry();
        assertionRegistry.setApplicationContext(null);
        assertionRegistry.registerAssertion(HardcodedResponseAssertion.class);
        WspConstants.setTypeMappingFinder(assertionRegistry);
    }

    @AfterClass
    public static void teardown() throws Exception {
        for (final String createdServiceId : createdServiceIds) {
            deleteService(createdServiceId);
        }
    }

    /**
     * Extra request headers should not be passed through by default.
     */
    @Test
    public void basicRouteWithDefaultRoutingAssertionConfiguration() throws Exception {
        final GenericHttpRequestParams params = new GenericHttpRequestParams(new URL("http://" + BASE_URL + ":8080/basicRoutingService"));
        // header should not be routed
        params.addExtraHeader(new GenericHttpHeader("Extra-Header", "Should Not Be Routed"));

        final GenericHttpResponse response = sendRequest(params, HttpMethod.GET, null);
        final String responseBody = printResponseDetails(response);
        assertEquals(200, response.getStatus());

        final Map<String, Collection<String>> headers = getResponseHeaders(response);
        assertEquals(4, headers.size());
        assertSingleHeaderValue(headers, "Server", "Apache-Coyote/1.1");
        assertSingleHeaderValue(headers, "Content-Type", "text/plain;charset=UTF-8");
        assertTrue(headers.containsKey("Content-Length"));
        assertTrue(headers.containsKey("Date"));

        final Map<String, Collection<String>> routedHeaders = getRoutedHeaders(responseBody);
        assertEquals(3, routedHeaders.size());
        assertEquals(1, routedHeaders.get("user-agent").size());
        assertTrue(routedHeaders.get("user-agent").iterator().next().contains("Layer7-SecureSpan-Gateway"));
        assertSingleHeaderValue(routedHeaders, "host", BASE_URL + ":8080");
        assertSingleHeaderValue(routedHeaders, "connection", "Keep-Alive");
        assertFalse(routedHeaders.containsKey("Extra-Header"));
    }

    /**
     * Non-application headers such as Date should not be passed through by default.
     */
    @Test
    public void basicRouteWithDefaultRoutingAssertionConfigurationFiltersNonApplicationHeaders() throws Exception {
        final GenericHttpRequestParams params = new GenericHttpRequestParams(new URL("http://" + BASE_URL + ":8080/basicRoutingService"));
        // these headers should not be routed
        params.addExtraHeader(new GenericHttpHeader("Date", "shouldNotBePassed"));
        params.addExtraHeader(new GenericHttpHeader("Host", "shouldBeReplacedWithGatewayHost"));
        params.addExtraHeader(new GenericHttpHeader("Server", "shouldNotBePassed"));
        params.addExtraHeader(new GenericHttpHeader("Connection", "shouldBeReplaced"));

        final GenericHttpResponse response = sendRequest(params, HttpMethod.GET, null);
        final String responseBody = printResponseDetails(response);
        assertEquals(200, response.getStatus());

        final Map<String, Collection<String>> routedHeaders = getRoutedHeaders(responseBody);
        assertEquals(3, routedHeaders.size());
        assertEquals(1, routedHeaders.get("user-agent").size());
        assertTrue(routedHeaders.get("user-agent").iterator().next().contains("Layer7-SecureSpan-Gateway"));
        assertSingleHeaderValue(routedHeaders, "host", BASE_URL + ":8080");
        assertSingleHeaderValue(routedHeaders, "connection", "Keep-Alive");
        assertFalse(routedHeaders.containsKey("Date"));
        assertFalse(routedHeaders.containsKey("Server"));
    }

    /**
     * Cookie and SOAPAction request headers should be passed through by default.
     */
    @Test
    public void basicRouteWithDefaultRoutingAssertionConfigurationPassesCookieAndSoapAction() throws Exception {
        final GenericHttpRequestParams params = new GenericHttpRequestParams(new URL("http://" + BASE_URL + ":8080/basicRoutingService"));
        params.addExtraHeader(new GenericHttpHeader("Cookie", "foo=bar"));
        params.addExtraHeader(new GenericHttpHeader("SOAPAction", "testSoapAction"));

        final GenericHttpResponse response = sendRequest(params, HttpMethod.GET, null);
        final String responseBody = printResponseDetails(response);
        assertEquals(200, response.getStatus());

        final Map<String, Collection<String>> routedHeaders = getRoutedHeaders(responseBody);
        assertEquals(5, routedHeaders.size());
        assertEquals(1, routedHeaders.get("user-agent").size());
        assertTrue(routedHeaders.get("user-agent").iterator().next().contains("Layer7-SecureSpan-Gateway"));
        assertSingleHeaderValue(routedHeaders, "host", BASE_URL + ":8080");
        assertSingleHeaderValue(routedHeaders, "connection", "Keep-Alive");
        assertSingleHeaderValue(routedHeaders, "cookie", "foo=bar");
        assertSingleHeaderValue(routedHeaders, "soapaction", "testSoapAction");
    }

    /**
     * If the route response sets a cookie, it should be passed through by default.
     */
    @Test
    public void responseSetCookieHeaderPassedByDefault() throws Exception {
        final HttpRoutingAssertion routeAssertion = new HttpRoutingAssertion();
        routeAssertion.setProtectedServiceUrl("http://" + BASE_URL + ":8080/setCookieService");
        final String routePolicy = WspWriter.getPolicyXml(new AllAssertion(Collections.singletonList(routeAssertion)));
        final Map<String, String> routeParams = new HashMap<>();
        routeParams.put(SERVICENAME, "RouteToSetCookieService");
        routeParams.put(SERVICEURL, "/routeToSetCookieService");
        routeParams.put(SERVICEPOLICY, routePolicy);
        createdServiceIds.add(createServiceFromTemplate(routeParams));

        final HardcodedResponseAssertion templateResponseAssertion = new HardcodedResponseAssertion();
        templateResponseAssertion.setResponseContentType("text/plain");
        templateResponseAssertion.setBase64ResponseBody(HexUtils.encodeBase64(new String("${request.http.allheadervalues}").getBytes()));
        final AddHeaderAssertion addHeaderAssertion = new AddHeaderAssertion();
        addHeaderAssertion.setTarget(TargetMessageType.RESPONSE);
        addHeaderAssertion.setHeaderName("Set-Cookie");
        addHeaderAssertion.setHeaderValue("foo=bar");
        final String setCookiePolicy = WspWriter.getPolicyXml(new AllAssertion(Arrays.asList(new Assertion[]{templateResponseAssertion, addHeaderAssertion})));
        final Map<String, String> setCookieParams = new HashMap<>();
        setCookieParams.put(SERVICENAME, "SetCookieService");
        setCookieParams.put(SERVICEURL, "/setCookieService");
        setCookieParams.put(SERVICEPOLICY, setCookiePolicy);
        createdServiceIds.add(createServiceFromTemplate(setCookieParams));

        final GenericHttpResponse response = sendRequest(new GenericHttpRequestParams(new URL("http://" + BASE_URL + ":8080/routeToSetCookieService")), HttpMethod.GET, null);
        printResponseDetails(response);
        assertEquals(200, response.getStatus());
        final Map<String, Collection<String>> responseHeaders = getResponseHeaders(response);
        assertSingleHeaderValue(responseHeaders, "Set-Cookie", "foo=bar; Domain=" + BASE_URL + "; Path=/routeToSetCookieService");
    }

    private static String createServiceFromTemplate(final Map<String, String> toReplace) throws Exception {
        final InputStream resourceStream = HttpRoutingIntegrationTest.class.getClassLoader().getResourceAsStream(SERVICE_TEMPLATE_RESOURCE);
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

    private Map<String, Collection<String>> getRoutedHeaders(final String responseBody) throws IOException {
        final Map<String, Collection<String>> routedRequestHeaders = new HashMap<>();
        final String[] headersFromBody = StringUtils.split(responseBody, ",");
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

    private void assertSingleHeaderValue(final Map<String, Collection<String>> headers, final String headerName, final String headerValue) {
        assertEquals(1, headers.get(headerName).size());
        assertEquals(headerValue, headers.get(headerName).iterator().next());
    }

    private Map<String, Collection<String>> getResponseHeaders(final GenericHttpResponse response) {
        final Map<String, Collection<String>> headersMap = new HashMap<>();
        for (final HttpHeader httpHeader : response.getHeaders().toArray()) {
            if (!headersMap.containsKey(httpHeader.getName())) {
                headersMap.put(httpHeader.getName(), new ArrayList<String>());
            }
            headersMap.get(httpHeader.getName()).add(httpHeader.getFullValue());
        }
        return headersMap;
    }

    private String printResponseDetails(final GenericHttpResponse response) throws IOException {
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

    private static String createService(final String resource) throws Exception {
        final InputStream resourceStream = HttpRoutingIntegrationTest.class.getClassLoader().getResourceAsStream(resource);
        final String request = new String(IOUtils.slurpStream(resourceStream));
        final GenericHttpResponse response = callWsman(request, CREATE_ACTION);
        final Document doc = XmlUtil.parse(response.getInputStream());
        final String id = getValue(doc, "//wsman:Selector");
        System.out.println("Created Service from resource " + resource + " with id=" + id);
        return id;
    }

    private static String createRoutingService(final String resource, final String routeUrl) throws Exception {
        final InputStream resourceStream = HttpRoutingIntegrationTest.class.getClassLoader().getResourceAsStream(resource);
        final String baseRequest = new String(IOUtils.slurpStream(resourceStream));
        final String request = baseRequest.replaceAll("===ROUTEURL===", routeUrl);
        final GenericHttpResponse response = callWsman(request, CREATE_ACTION);
        final Document doc = XmlUtil.parse(response.getInputStream());
        final String id = getValue(doc, "//wsman:Selector");
        System.out.println("Created Service from resource " + resource + " with id=" + id);
        return id;
    }

    private static void deleteService(@NotNull final String id) throws Exception {
        final InputStream resourceStream = HttpRoutingIntegrationTest.class.getClassLoader().getResourceAsStream(DELETE_SERVICE_RESOURCE);
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

    private static GenericHttpResponse sendRequest(final GenericHttpRequestParams params, final HttpMethod method, final String body) throws Exception {
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
}
