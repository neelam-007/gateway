package com.l7tech.server;

import com.l7tech.common.http.GenericHttpHeader;
import com.l7tech.common.http.GenericHttpRequestParams;
import com.l7tech.common.http.GenericHttpResponse;
import com.l7tech.common.http.HttpMethod;
import com.l7tech.policy.assertion.BridgeRoutingAssertion;
import com.l7tech.policy.assertion.HardcodedResponseAssertion;
import com.l7tech.policy.assertion.HttpPassthroughRule;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.wsp.WspWriter;
import com.l7tech.xml.soap.SoapUtil;
import org.junit.Ignore;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Routing integration tests specific to the bridge routed request.
 *
 * @see {@link HttpRoutingIntegrationTest for configuration}
 */
@Ignore
public class HttpBridgeRoutingRequestIntegrationTest extends HttpRoutingIntegrationTest {

    @Test
    public void defaultHeaderConfiguration() throws Exception {
        final BridgeRoutingAssertion bridge = new BridgeRoutingAssertion();
        bridge.setProtectedServiceUrl(ECHO_HEADERS_URL);
        bridge.setUseSslByDefault(false);

        final Map<String, String> routeParams = new HashMap<>();
        routeParams.put(SERVICENAME, "DefaultBridgeRouting");
        routeParams.put(SERVICEURL, "/defaultBridgeRouting");
        final String policyXml = WspWriter.getPolicyXml(new AllAssertion(Arrays.asList(bridge)));
        routeParams.put(SERVICEPOLICY, policyXml);
        testLevelCreatedServiceIds.add(createServiceFromTemplate(routeParams));

        final GenericHttpRequestParams requestParams = createSoapParams("http://" + BASE_URL + ":8080/defaultBridgeRouting");
        requestParams.addExtraHeader(new GenericHttpHeader("Cookie", "foo=bar"));
        // these headers should not be routed
        requestParams.addExtraHeader(new GenericHttpHeader("Date", "shouldNotBePassed"));
        requestParams.addExtraHeader(new GenericHttpHeader("Host", "shouldBeReplacedWithGatewayHost"));
        requestParams.addExtraHeader(new GenericHttpHeader("Server", "shouldNotBePassed"));
        requestParams.addExtraHeader(new GenericHttpHeader("Connection", "shouldBeReplaced"));
        requestParams.addExtraHeader(new GenericHttpHeader("ShouldNotBeRouted", "ShouldNotBeRoutedValue"));

        final GenericHttpResponse response = sendRequest(requestParams, HttpMethod.POST, SOAP_BODY);
        final String responseBody = printResponseDetails(response);
        assertEquals(200, response.getStatus());
        final Map<String, Collection<String>> responseHeaders = getResponseHeaders(response);
        assertEquals(4, responseHeaders.size());
        assertHeaderValues(responseHeaders, "Server", APACHE_SERVER);
        assertHeaderValues(responseHeaders, "Content-Type", "text/xml;charset=UTF-8");
        assertTrue(responseHeaders.containsKey("Content-Length"));
        assertTrue(responseHeaders.containsKey("Date"));

        final Map<String, Collection<String>> routedHeaders = parseHeaders(responseBody);
        assertEquals(8, routedHeaders.size());
        assertHeaderValues(routedHeaders, "user-agent", BRIDGE_USER_AGENT);
        assertHeaderValues(routedHeaders, "soapaction", "testSoapAction");
        assertHeaderValues(routedHeaders, "l7-original-url", ECHO_HEADERS_URL);
        assertHeaderValues(routedHeaders, "content-type", SOAP_CONTENT_TYPE);
        assertHeaderValues(routedHeaders, "host", BASE_URL + ":8080");
        assertHeaderValues(routedHeaders, "connection", KEEP_ALIVE);
        assertHeaderValues(routedHeaders, "cookie", "foo=bar");
        assertTrue(routedHeaders.containsKey("content-length"));
        assertFalse(routedHeaders.containsKey("ShouldNotBeRouted"));
    }

    @Test
    public void passThroughAllRequestHeaders() throws Exception {
        final Map<String, String> routeParams = new HashMap<>();
        routeParams.put(SERVICENAME, "PassThroughAllRequestHeaders");
        routeParams.put(SERVICEURL, "/passThroughAllRequestHeaders");
        final String policyXml = WspWriter.getPolicyXml(new AllAssertion(Collections.singletonList(createBridgeRouteAssertion(ECHO_HEADERS_URL, true))));
        routeParams.put(SERVICEPOLICY, policyXml);
        testLevelCreatedServiceIds.add(createServiceFromTemplate(routeParams));

        final GenericHttpRequestParams params = createSoapParams("http://" + BASE_URL + ":8080/passThroughAllRequestHeaders");
        params.addExtraHeader(new GenericHttpHeader("foo", "bar"));
        params.addExtraHeader(new GenericHttpHeader("foo", "foo2"));
        params.addExtraHeader(new GenericHttpHeader("User-Agent", "testUserAgent"));
        params.addExtraHeader(new GenericHttpHeader("Cookie", "foo=bar"));
        // non-application headers should still be filtered
        params.addExtraHeader(new GenericHttpHeader("Server", "shouldNotBePassed"));
        params.addExtraHeader(new GenericHttpHeader("Date", "shouldNotBePassed"));
        params.addExtraHeader(new GenericHttpHeader("Connection", "shouldNotBePassed"));
        params.addExtraHeader(new GenericHttpHeader("Host", "shouldNotBePassed"));

        final GenericHttpResponse response = sendRequest(params, HttpMethod.POST, SOAP_BODY);
        assertEquals(200, response.getStatus());
        final String responseBody = printResponseDetails(response);
        final Map<String, Collection<String>> routedHeaders = parseHeaders(responseBody);
        assertEquals(9, routedHeaders.size());
        assertHeaderValues(routedHeaders, "foo", "bar", "foo2");
        assertHeaderValues(routedHeaders, "user-agent", BRIDGE_USER_AGENT, "testUserAgent");
        assertHeaderValues(routedHeaders, "cookie", "foo=bar");
        assertHeaderValues(routedHeaders, "soapaction", "testSoapAction");
        assertHeaderValues(routedHeaders, "host", BASE_URL + ":8080");
        assertHeaderValues(routedHeaders, "connection", KEEP_ALIVE);
        assertHeaderValues(routedHeaders, "l7-original-url", ECHO_HEADERS_URL);
        assertHeaderValues(routedHeaders, "content-type", SOAP_CONTENT_TYPE);
        assertTrue(routedHeaders.containsKey("content-length"));
    }

    @Test
    public void customizeRequestHeadersToPassThrough() throws Exception {
        // headers to pass through
        final List<HttpPassthroughRule> rules = new ArrayList<>();
        rules.add(new HttpPassthroughRule("foo", true, "customFoo"));
        rules.add(new HttpPassthroughRule("User-Agent", false, null));

        final Map<String, String> routeParams = new HashMap<>();
        routeParams.put(SERVICENAME, "PassThroughSomeRequestHeaders");
        routeParams.put(SERVICEURL, "/passThroughSomeRequestHeaders");
        final String policyXml = WspWriter.getPolicyXml(new AllAssertion(Collections.singletonList(createBridgeRouteAssertion(ECHO_HEADERS_URL, false, rules))));
        routeParams.put(SERVICEPOLICY, policyXml);
        testLevelCreatedServiceIds.add(createServiceFromTemplate(routeParams));

        final GenericHttpRequestParams params = createSoapParams("http://" + BASE_URL + ":8080/passThroughSomeRequestHeaders");
        params.addExtraHeader(new GenericHttpHeader("foo", "shouldBeReplaced"));
        params.addExtraHeader(new GenericHttpHeader("foo", "shouldAlsoBeReplaced"));
        params.addExtraHeader(new GenericHttpHeader("User-Agent", "testUserAgent"));
        params.addExtraHeader(new GenericHttpHeader("Cookie", "shouldNotBePassed"));
        params.addExtraHeader(new GenericHttpHeader("Host", "testHost"));
        params.addExtraHeader(new GenericHttpHeader("ShouldNotBePassed", "ShouldNotBePassed"));

        final GenericHttpResponse response = sendRequest(params, HttpMethod.POST, SOAP_BODY);
        assertEquals(200, response.getStatus());
        final String responseBody = printResponseDetails(response);
        final Map<String, Collection<String>> routedHeaders = parseHeaders(responseBody);
        assertEquals(8, routedHeaders.size());
        assertHeaderValues(routedHeaders, "foo", "customFoo");
        assertHeaderValues(routedHeaders, "user-agent", BRIDGE_USER_AGENT, "testUserAgent");
        assertHeaderValues(routedHeaders, "soapaction", "testSoapAction");
        assertHeaderValues(routedHeaders, "host", BASE_URL + ":8080");
        assertHeaderValues(routedHeaders, "connection", KEEP_ALIVE);
        assertHeaderValues(routedHeaders, "l7-original-url", ECHO_HEADERS_URL);
        assertHeaderValues(routedHeaders, "content-type", SOAP_CONTENT_TYPE);
        assertTrue(routedHeaders.containsKey("content-length"));
        assertFalse(routedHeaders.containsKey("ShouldNotBePassed"));
    }

    @Test
    public void cannotCustomizeRequestHostToPassThrough() throws Exception {
        final Map<String, String> routeParams = new HashMap<>();
        routeParams.put(SERVICENAME, "PassThroughCustomHost");
        routeParams.put(SERVICEURL, "/passThroughCustomHost");
        final String policyXml = WspWriter.getPolicyXml(new AllAssertion(Collections.singletonList(
                createBridgeRouteAssertion(ECHO_HEADERS_URL, false, new HttpPassthroughRule("Host", true, "customHost")))));
        routeParams.put(SERVICEPOLICY, policyXml);
        testLevelCreatedServiceIds.add(createServiceFromTemplate(routeParams));

        final GenericHttpRequestParams params = createSoapParams("http://" + BASE_URL + ":8080/passThroughCustomHost");
        params.addExtraHeader(new GenericHttpHeader("Host", "testHost"));

        final GenericHttpResponse response = sendRequest(params, HttpMethod.POST, SOAP_BODY);
        assertEquals(200, response.getStatus());
        final String responseBody = printResponseDetails(response);
        final Map<String, Collection<String>> routedHeaders = parseHeaders(responseBody);
        assertHeaderValues(routedHeaders, "host", BASE_URL + ":8080");
    }

    @Test
    public void passThroughOriginalRequestSoapAction() throws Exception {
        final Map<String, String> routeParams = new HashMap<>();
        routeParams.put(SERVICENAME, "PassThroughOriginalSoapAction");
        routeParams.put(SERVICEURL, "/passThroughOriginalSoapAction");
        final String policyXml = WspWriter.getPolicyXml(new AllAssertion(Collections.singletonList(
                createBridgeRouteAssertion(ECHO_HEADERS_URL, false, new HttpPassthroughRule("SOAPAction", false, null)))));
        routeParams.put(SERVICEPOLICY, policyXml);
        testLevelCreatedServiceIds.add(createServiceFromTemplate(routeParams));

        final GenericHttpRequestParams params = createSoapParams("http://" + BASE_URL + ":8080/passThroughOriginalSoapAction");

        final GenericHttpResponse response = sendRequest(params, HttpMethod.POST, SOAP_BODY);
        assertEquals(200, response.getStatus());
        final String responseBody = printResponseDetails(response);
        final Map<String, Collection<String>> routedHeaders = parseHeaders(responseBody);
        assertHeaderValues(routedHeaders, "soapaction", "testSoapAction");
    }

    @Test
    public void customizeRequestSoapActionUsingRule() throws Exception {
        final Map<String, String> routeParams = new HashMap<>();
        routeParams.put(SERVICENAME, "CustomizeSoapActionUsingRule");
        routeParams.put(SERVICEURL, "/customizeSoapActionUsingRule");
        final String policyXml = WspWriter.getPolicyXml(new AllAssertion(Collections.singletonList(
                createBridgeRouteAssertion(ECHO_HEADERS_URL, false, new HttpPassthroughRule("SOAPAction", true, "customSoapAction")))));
        routeParams.put(SERVICEPOLICY, policyXml);
        testLevelCreatedServiceIds.add(createServiceFromTemplate(routeParams));

        final GenericHttpRequestParams params = createSoapParams("http://" + BASE_URL + ":8080/customizeSoapActionUsingRule");

        final GenericHttpResponse response = sendRequest(params, HttpMethod.POST, SOAP_BODY);
        assertEquals(200, response.getStatus());
        final String responseBody = printResponseDetails(response);
        final Map<String, Collection<String>> routedHeaders = parseHeaders(responseBody);
        assertHeaderValues(routedHeaders, "soapaction", "customSoapAction");
    }

    @Test
    public void cannotCustomizeRequestSoapActionUsingAddHeaderAssertion() throws Exception {
        final Map<String, String> routeParams = new HashMap<>();
        routeParams.put(SERVICENAME, "CustomizeSoapActionUsingAddHeaderAssertion");
        routeParams.put(SERVICEURL, "/customizeSoapActionUsingAddHeaderAssertion");
        final String policyXml = WspWriter.getPolicyXml(new AllAssertion(Arrays.asList(
                createAddHeaderAssertion(SoapUtil.SOAPACTION, "customSoapAction", true),
                createBridgeRouteAssertion(ECHO_HEADERS_URL, false, new HttpPassthroughRule("SOAPAction", false, null)))));
        routeParams.put(SERVICEPOLICY, policyXml);
        testLevelCreatedServiceIds.add(createServiceFromTemplate(routeParams));

        final GenericHttpRequestParams params = createSoapParams("http://" + BASE_URL + ":8080/customizeSoapActionUsingAddHeaderAssertion");

        final GenericHttpResponse response = sendRequest(params, HttpMethod.POST, SOAP_BODY);
        final String responseBody = printResponseDetails(response);
        assertEquals(200, response.getStatus());
        final Map<String, Collection<String>> routedHeaders = parseHeaders(responseBody);
        assertHeaderValues(routedHeaders, "soapaction", "testSoapAction");
    }

    @Test
    public void addRequestHeaderUsingAddHeaderAssertion() throws Exception {
        final Map<String, String> routeParams = new HashMap<>();
        routeParams.put(SERVICENAME, "AddHeaderToRequest");
        routeParams.put(SERVICEURL, "/addHeaderToRequest");
        final String policyXml = WspWriter.getPolicyXml(new AllAssertion(Arrays.asList(
                createAddHeaderAssertion("foo", "bar"),
                createBridgeRouteAssertion(ECHO_HEADERS_URL, true))));
        routeParams.put(SERVICEPOLICY, policyXml);
        testLevelCreatedServiceIds.add(createServiceFromTemplate(routeParams));

        final GenericHttpRequestParams requestParams = createSoapParams("http://" + BASE_URL + ":8080/addHeaderToRequest");

        final GenericHttpResponse response = sendRequest(requestParams, HttpMethod.POST, SOAP_BODY);
        assertEquals(200, response.getStatus());
        final String responseBody = printResponseDetails(response);
        final Map<String, Collection<String>> routedHeaders = parseHeaders(responseBody);
        assertEquals(8, routedHeaders.size());
        assertHeaderValues(routedHeaders, "foo", "bar");
        assertHeaderValues(routedHeaders, "user-agent", BRIDGE_USER_AGENT, APACHE_USER_AGENT);
        assertHeaderValues(routedHeaders, "host", BASE_URL + ":8080");
        assertHeaderValues(routedHeaders, "connection", KEEP_ALIVE);
        assertHeaderValues(routedHeaders, "soapaction", "testSoapAction");
        assertHeaderValues(routedHeaders, "l7-original-url", ECHO_HEADERS_URL);
        assertHeaderValues(routedHeaders, "content-type", SOAP_CONTENT_TYPE);
        assertTrue(routedHeaders.containsKey("content-length"));
    }

    @Test
    public void addRequestHeaderUsingAddHeaderAssertionNameAlreadyExists() throws Exception {
        final Map<String, String> routeParams = new HashMap<>();
        routeParams.put(SERVICENAME, "AddHeaderToRequestAlreadyExists");
        routeParams.put(SERVICEURL, "/addHeaderToRequestAlreadyExists");
        final String policyXml = WspWriter.getPolicyXml(new AllAssertion(Arrays.asList(
                createAddHeaderAssertion("foo", "bar"),
                createBridgeRouteAssertion(ECHO_HEADERS_URL, true))));
        routeParams.put(SERVICEPOLICY, policyXml);
        testLevelCreatedServiceIds.add(createServiceFromTemplate(routeParams));

        final GenericHttpRequestParams params = createSoapParams("http://" + BASE_URL + ":8080/addHeaderToRequestAlreadyExists");
        params.addExtraHeader(new GenericHttpHeader("foo", "existingFoo"));

        final GenericHttpResponse response = sendRequest(params, HttpMethod.POST, SOAP_BODY);
        assertEquals(200, response.getStatus());
        final String responseBody = printResponseDetails(response);
        final Map<String, Collection<String>> routedHeaders = parseHeaders(responseBody);
        assertEquals(8, routedHeaders.size());
        assertHeaderValues(routedHeaders, "foo", "existingFoo", "bar");
        assertHeaderValues(routedHeaders, "user-agent", BRIDGE_USER_AGENT, APACHE_USER_AGENT);
        assertHeaderValues(routedHeaders, "host", BASE_URL + ":8080");
        assertHeaderValues(routedHeaders, "connection", KEEP_ALIVE);
        assertHeaderValues(routedHeaders, "soapaction", "testSoapAction");
        assertHeaderValues(routedHeaders, "l7-original-url", ECHO_HEADERS_URL);
        assertHeaderValues(routedHeaders, "content-type", SOAP_CONTENT_TYPE);
        assertTrue(routedHeaders.containsKey("content-length"));
    }

    @Test
    public void replaceRequestHeaderUsingAddHeaderAssertion() throws Exception {
        final Map<String, String> routeParams = new HashMap<>();
        routeParams.put(SERVICENAME, "ReplaceHeaderOnRequest");
        routeParams.put(SERVICEURL, "/replaceHeaderOnRequest");
        final String policyXml = WspWriter.getPolicyXml(new AllAssertion(Arrays.asList(
                createAddHeaderAssertion("foo", "bar", true),
                createBridgeRouteAssertion(ECHO_HEADERS_URL, true))));
        routeParams.put(SERVICEPOLICY, policyXml);
        testLevelCreatedServiceIds.add(createServiceFromTemplate(routeParams));

        final GenericHttpRequestParams params = createSoapParams("http://" + BASE_URL + ":8080/replaceHeaderOnRequest");
        params.addExtraHeader(new GenericHttpHeader("foo", "shouldBeReplaced"));

        final GenericHttpResponse response = sendRequest(params, HttpMethod.POST, SOAP_BODY);
        assertEquals(200, response.getStatus());
        final String responseBody = printResponseDetails(response);
        final Map<String, Collection<String>> routedHeaders = parseHeaders(responseBody);
        assertEquals(8, routedHeaders.size());
        assertHeaderValues(routedHeaders, "foo", "bar");
        assertHeaderValues(routedHeaders, "user-agent", BRIDGE_USER_AGENT, APACHE_USER_AGENT);
        assertHeaderValues(routedHeaders, "host", BASE_URL + ":8080");
        assertHeaderValues(routedHeaders, "connection", KEEP_ALIVE);
        assertHeaderValues(routedHeaders, "soapaction", "testSoapAction");
        assertHeaderValues(routedHeaders, "l7-original-url", ECHO_HEADERS_URL);
        assertHeaderValues(routedHeaders, "content-type", SOAP_CONTENT_TYPE);
        assertTrue(routedHeaders.containsKey("content-length"));
    }

    /**
     * Request header rule replaces add header assertion replaces original request header.
     */
    @Test
    public void passThroughRequestHeaderRuleReplacesAddHeaderAssertion() throws Exception {
        final Map<String, String> routeParams = new HashMap<>();
        routeParams.put(SERVICENAME, "PassThroughRequestHeaderRuleReplacesAddHeaderAssertion");
        routeParams.put(SERVICEURL, "/passThroughRequestHeaderRuleReplacesAddHeaderAssertion");
        final String policyXml = WspWriter.getPolicyXml(new AllAssertion(Arrays.asList(
                createAddHeaderAssertion("foo", "addHeaderValueShouldBeReplaced", true),
                createBridgeRouteAssertion(ECHO_HEADERS_URL, false, new HttpPassthroughRule("foo", true, "bar")))));
        routeParams.put(SERVICEPOLICY, policyXml);
        testLevelCreatedServiceIds.add(createServiceFromTemplate(routeParams));

        final GenericHttpRequestParams params = createSoapParams("http://" + BASE_URL + ":8080/passThroughRequestHeaderRuleReplacesAddHeaderAssertion");
        params.addExtraHeader(new GenericHttpHeader("foo", "originalHeaderValueShouldBeReplaced"));

        final GenericHttpResponse response = sendRequest(params, HttpMethod.POST, SOAP_BODY);
        assertEquals(200, response.getStatus());
        final String responseBody = printResponseDetails(response);
        final Map<String, Collection<String>> routedHeaders = parseHeaders(responseBody);
        assertEquals(8, routedHeaders.size());
        assertHeaderValues(routedHeaders, "foo", "bar");
        assertHeaderValues(routedHeaders, "user-agent", BRIDGE_USER_AGENT);
        assertHeaderValues(routedHeaders, "soapaction", "testSoapAction");
        assertHeaderValues(routedHeaders, "l7-original-url", ECHO_HEADERS_URL);
        assertHeaderValues(routedHeaders, "content-type", SOAP_CONTENT_TYPE);
        assertHeaderValues(routedHeaders, "host", BASE_URL + ":8080");
        assertHeaderValues(routedHeaders, "connection", KEEP_ALIVE);
        assertTrue(routedHeaders.containsKey("content-length"));
    }

    @Test
    public void multipleAddHeaderAssertionsSameName() throws Exception {
        final Map<String, String> routeParams = new HashMap<>();
        routeParams.put(SERVICENAME, "AddHeaderToRequest");
        routeParams.put(SERVICEURL, "/addHeaderToRequest");
        final String policyXml = WspWriter.getPolicyXml(new AllAssertion(Arrays.asList(
                createAddHeaderAssertion("foo", "bar"),
                createAddHeaderAssertion("foo", "bar2"),
                createBridgeRouteAssertion(ECHO_HEADERS_URL, true))));
        routeParams.put(SERVICEPOLICY, policyXml);
        testLevelCreatedServiceIds.add(createServiceFromTemplate(routeParams));

        final GenericHttpRequestParams params = createSoapParams("http://" + BASE_URL + ":8080/addHeaderToRequest");

        final GenericHttpResponse response = sendRequest(params, HttpMethod.POST, SOAP_BODY);
        assertEquals(200, response.getStatus());
        final String responseBody = printResponseDetails(response);
        final Map<String, Collection<String>> routedHeaders = parseHeaders(responseBody);
        assertEquals(8, routedHeaders.size());
        assertHeaderValues(routedHeaders, "foo", "bar", "bar2");
        assertHeaderValues(routedHeaders, "user-agent", BRIDGE_USER_AGENT, APACHE_USER_AGENT);
        assertHeaderValues(routedHeaders, "soapaction", "testSoapAction");
        assertHeaderValues(routedHeaders, "l7-original-url", ECHO_HEADERS_URL);
        assertHeaderValues(routedHeaders, "content-type", SOAP_CONTENT_TYPE);
        assertHeaderValues(routedHeaders, "host", BASE_URL + ":8080");
        assertHeaderValues(routedHeaders, "connection", KEEP_ALIVE);
        assertTrue(routedHeaders.containsKey("content-length"));
    }

    @Test
    public void multipleRoutesAndAddHeaders() throws Exception {
        final Map<String, String> routeParams = new HashMap<>();
        routeParams.put(SERVICENAME, "MultipleRoutesAndAddHeaders");
        routeParams.put(SERVICEURL, "/multipleRoutesAndAddHeaders");

        // this policy will collect the routed headers for each route and return them in the response
        final HardcodedResponseAssertion echoResponseAssertion = new HardcodedResponseAssertion();
        echoResponseAssertion.responseBodyString("<firstRouteResponse>${firstRouteResponse.mainpart}</firstRouteResponse><secondRouteResponse>${secondRouteResponse.mainpart}</secondRouteResponse>");
        final String policyXml = WspWriter.getPolicyXml(new AllAssertion(Arrays.asList(
                createAddHeaderAssertion("foo", "addedBeforeFirstRoute"),
                // store first route response in context variable
                createBridgeRouteAssertion(ECHO_HEADERS_URL, false, true, "firstRouteResponse", null),
                createAddHeaderAssertion("bar", "addedAfterFirstRoute"),
                // store second route response in context variable
                createBridgeRouteAssertion(ECHO_HEADERS_URL, false, true, "secondRouteResponse", null),
                echoResponseAssertion)));
        routeParams.put(SERVICEPOLICY, policyXml);
        testLevelCreatedServiceIds.add(createServiceFromTemplate(routeParams));

        final GenericHttpResponse response = sendRequest(createSoapParams("http://" + BASE_URL + ":8080/multipleRoutesAndAddHeaders"), HttpMethod.POST, SOAP_BODY);
        final String responseBody = printResponseDetails(response);
        assertEquals(200, response.getStatus());

        final String firstRouteResponseBody = responseBody.substring(responseBody.indexOf("<firstRouteResponse>") + "<firstRouteResponse>".length(), responseBody.indexOf("</firstRouteResponse"));
        final Map<String, Collection<String>> firstHeadersRouted = parseHeaders(firstRouteResponseBody);
        assertEquals(8, firstHeadersRouted.size());
        assertHeaderValues(firstHeadersRouted, "foo", "addedBeforeFirstRoute");
        assertFalse(firstHeadersRouted.containsKey("bar"));
        assertHeaderValues(firstHeadersRouted, "user-agent", BRIDGE_USER_AGENT, APACHE_USER_AGENT);
        assertHeaderValues(firstHeadersRouted, "host", BASE_URL + ":8080");
        assertHeaderValues(firstHeadersRouted, "connection", KEEP_ALIVE);
        assertHeaderValues(firstHeadersRouted, "soapaction", "testSoapAction");
        assertHeaderValues(firstHeadersRouted, "l7-original-url", ECHO_HEADERS_URL);
        assertHeaderValues(firstHeadersRouted, "content-type", SOAP_CONTENT_TYPE);
        assertTrue(firstHeadersRouted.containsKey("content-length"));

        final String secondRouteResponseBody = responseBody.substring(responseBody.indexOf("<secondRouteResponse>") + "<secondRouteResponse>".length(), responseBody.indexOf("</secondRouteResponse"));
        final Map<String, Collection<String>> secondHeadersRouted = parseHeaders(secondRouteResponseBody);
        assertEquals(9, secondHeadersRouted.size());
        assertHeaderValues(secondHeadersRouted, "foo", "addedBeforeFirstRoute");
        assertHeaderValues(secondHeadersRouted, "bar", "addedAfterFirstRoute");
        assertHeaderValues(secondHeadersRouted, "user-agent", BRIDGE_USER_AGENT, APACHE_USER_AGENT);
        assertHeaderValues(secondHeadersRouted, "host", BASE_URL + ":8080");
        assertHeaderValues(secondHeadersRouted, "connection", KEEP_ALIVE);
        assertHeaderValues(secondHeadersRouted, "soapaction", "testSoapAction");
        assertHeaderValues(secondHeadersRouted, "l7-original-url", ECHO_HEADERS_URL);
        assertHeaderValues(secondHeadersRouted, "content-type", SOAP_CONTENT_TYPE);
        assertTrue(secondHeadersRouted.containsKey("content-length"));
    }

}
