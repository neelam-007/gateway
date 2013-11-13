package com.l7tech.server;

import com.l7tech.common.http.GenericHttpHeader;
import com.l7tech.common.http.GenericHttpRequestParams;
import com.l7tech.common.http.GenericHttpResponse;
import com.l7tech.common.http.HttpMethod;
import com.l7tech.policy.assertion.HardcodedResponseAssertion;
import com.l7tech.policy.assertion.HttpPassthroughRule;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.wsp.WspWriter;
import com.l7tech.xml.soap.SoapUtil;
import org.apache.commons.lang.StringUtils;
import org.junit.Ignore;
import org.junit.Test;

import java.net.URL;
import java.util.*;

import static org.junit.Assert.*;

/**
 * Routing integration tests specific to the routed request.
 *
 * @see {@link HttpRoutingIntegrationTest for configuration}
 */
public class HttpRoutingRequestIntegrationTest extends HttpRoutingIntegrationTest {

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
        assertHeaderValues(headers, "Server", APACHE_SERVER);
        assertHeaderValues(headers, "Content-Type", "text/xml;charset=UTF-8");
        assertTrue(headers.containsKey("Content-Length"));
        assertTrue(headers.containsKey("Date"));

        final Map<String, Collection<String>> routedHeaders = parseHeaders(responseBody);
        assertEquals(3, routedHeaders.size());
        assertEquals(1, routedHeaders.get("user-agent").size());
        assertTrue(routedHeaders.get("user-agent").iterator().next().contains(L7_USER_AGENT));
        assertHeaderValues(routedHeaders, "host", BASE_URL + ":8080");
        assertHeaderValues(routedHeaders, "connection", KEEP_ALIVE);
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

        final Map<String, Collection<String>> routedHeaders = parseHeaders(responseBody);
        assertEquals(3, routedHeaders.size());
        assertEquals(1, routedHeaders.get("user-agent").size());
        assertTrue(routedHeaders.get("user-agent").iterator().next().contains(L7_USER_AGENT));
        assertHeaderValues(routedHeaders, "host", BASE_URL + ":8080");
        assertHeaderValues(routedHeaders, "connection", KEEP_ALIVE);
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

        final Map<String, Collection<String>> routedHeaders = parseHeaders(responseBody);
        assertEquals(5, routedHeaders.size());
        assertEquals(1, routedHeaders.get("user-agent").size());
        assertTrue(routedHeaders.get("user-agent").iterator().next().contains(L7_USER_AGENT));
        assertHeaderValues(routedHeaders, "host", BASE_URL + ":8080");
        assertHeaderValues(routedHeaders, "connection", KEEP_ALIVE);
        assertHeaderValues(routedHeaders, "cookie", "foo=bar");
        assertHeaderValues(routedHeaders, "soapaction", "testSoapAction");
    }

    @Test
    public void passThroughAllRequestHeaders() throws Exception {
        final Map<String, String> routeParams = new HashMap<>();
        routeParams.put(SERVICENAME, "PassThroughAllRequestHeaders");
        routeParams.put(SERVICEURL, "/passThroughAllRequestHeaders");
        final String policyXml = WspWriter.getPolicyXml(new AllAssertion(Collections.singletonList(createRouteAssertion(ECHO_HEADERS_URL, true))));
        routeParams.put(SERVICEPOLICY, policyXml);
        testLevelCreatedServiceIds.add(createServiceFromTemplate(routeParams));

        final GenericHttpRequestParams params = new GenericHttpRequestParams(new URL("http://" + BASE_URL + ":8080/passThroughAllRequestHeaders"));
        params.addExtraHeader(new GenericHttpHeader("foo", "bar"));
        params.addExtraHeader(new GenericHttpHeader("foo", "foo2"));
        params.addExtraHeader(new GenericHttpHeader("User-Agent", "testUserAgent"));
        params.addExtraHeader(new GenericHttpHeader("Cookie", "foo=bar"));
        params.addExtraHeader(new GenericHttpHeader("SOAPAction", "testSoapAction"));
        // non-application headers should still be filtered
        params.addExtraHeader(new GenericHttpHeader("Server", "shouldNotBePassed"));
        params.addExtraHeader(new GenericHttpHeader("Date", "shouldNotBePassed"));
        params.addExtraHeader(new GenericHttpHeader("Connection", "shouldNotBePassed"));
        params.addExtraHeader(new GenericHttpHeader("Host", "shouldNotBePassed"));

        final GenericHttpResponse response = sendRequest(params, HttpMethod.GET, null);
        assertEquals(200, response.getStatus());
        final String responseBody = printResponseDetails(response);
        final Map<String, Collection<String>> routedHeaders = parseHeaders(responseBody);
        assertEquals(6, routedHeaders.size());
        assertHeaderValues(routedHeaders, "foo", "bar", "foo2");
        assertHeaderValues(routedHeaders, "user-agent", "testUserAgent");
        assertHeaderValues(routedHeaders, "cookie", "foo=bar");
        assertHeaderValues(routedHeaders, "soapaction", "testSoapAction");
        assertHeaderValues(routedHeaders, "host", BASE_URL + ":8080");
        assertHeaderValues(routedHeaders, "connection", KEEP_ALIVE);
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
        final String policyXml = WspWriter.getPolicyXml(new AllAssertion(Collections.singletonList(createRouteAssertion(ECHO_HEADERS_URL, false, rules))));
        routeParams.put(SERVICEPOLICY, policyXml);
        testLevelCreatedServiceIds.add(createServiceFromTemplate(routeParams));

        final GenericHttpRequestParams params = new GenericHttpRequestParams(new URL("http://" + BASE_URL + ":8080/passThroughSomeRequestHeaders"));
        params.addExtraHeader(new GenericHttpHeader("foo", "shouldBeReplaced"));
        params.addExtraHeader(new GenericHttpHeader("foo", "shouldAlsoBeReplaced"));
        params.addExtraHeader(new GenericHttpHeader("User-Agent", "testUserAgent"));
        params.addExtraHeader(new GenericHttpHeader("Cookie", "shouldNotBePassed"));
        params.addExtraHeader(new GenericHttpHeader("SOAPAction", "shouldBeReplaced"));
        params.addExtraHeader(new GenericHttpHeader("Host", "testHost"));

        final GenericHttpResponse response = sendRequest(params, HttpMethod.GET, null);
        assertEquals(200, response.getStatus());
        final String responseBody = printResponseDetails(response);
        final Map<String, Collection<String>> routedHeaders = parseHeaders(responseBody);
        assertEquals(4, routedHeaders.size());
        assertHeaderValues(routedHeaders, "foo", "customFoo");
        assertHeaderValues(routedHeaders, "user-agent", "testUserAgent");
        assertHeaderValues(routedHeaders, "host", BASE_URL + ":8080");
        assertHeaderValues(routedHeaders, "connection", KEEP_ALIVE);
    }

    @Test
    public void customizeRequestHostToPassThrough() throws Exception {
        final Map<String, String> routeParams = new HashMap<>();
        routeParams.put(SERVICENAME, "PassThroughCustomHost");
        routeParams.put(SERVICEURL, "/passThroughCustomHost");
        final String policyXml = WspWriter.getPolicyXml(new AllAssertion(Collections.singletonList(
                createRouteAssertion(ECHO_HEADERS_URL, false, new HttpPassthroughRule("Host", true, "customHost")))));
        routeParams.put(SERVICEPOLICY, policyXml);
        testLevelCreatedServiceIds.add(createServiceFromTemplate(routeParams));

        final GenericHttpRequestParams params = new GenericHttpRequestParams(new URL("http://" + BASE_URL + ":8080/passThroughCustomHost"));
        params.addExtraHeader(new GenericHttpHeader("Host", "testHost"));

        final GenericHttpResponse response = sendRequest(params, HttpMethod.GET, null);
        assertEquals(200, response.getStatus());
        final String responseBody = printResponseDetails(response);
        final Map<String, Collection<String>> routedHeaders = parseHeaders(responseBody);
        assertHeaderValues(routedHeaders, "host", "customHost:8080");
    }

    @Test
    public void customizeRequestHostWithPortToPassThrough() throws Exception {
        final Map<String, String> routeParams = new HashMap<>();
        routeParams.put(SERVICENAME, "PassThroughCustomHostWithPort");
        routeParams.put(SERVICEURL, "/passThroughCustomHostWithPort");
        final String policyXml = WspWriter.getPolicyXml(new AllAssertion(Collections.singletonList(
                createRouteAssertion(ECHO_HEADERS_URL, false, new HttpPassthroughRule("Host", true, "customHost:8888")))));
        routeParams.put(SERVICEPOLICY, policyXml);
        testLevelCreatedServiceIds.add(createServiceFromTemplate(routeParams));

        final GenericHttpRequestParams params = new GenericHttpRequestParams(new URL("http://" + BASE_URL + ":8080/passThroughCustomHostWithPort"));
        params.addExtraHeader(new GenericHttpHeader("Host", "testHost"));

        final GenericHttpResponse response = sendRequest(params, HttpMethod.GET, null);
        assertEquals(200, response.getStatus());
        final String responseBody = printResponseDetails(response);
        final Map<String, Collection<String>> routedHeaders = parseHeaders(responseBody);
        assertHeaderValues(routedHeaders, "host", "customHost:8888");
    }

    @Test
    public void passThroughOriginalRequestSoapAction() throws Exception {
        final Map<String, String> routeParams = new HashMap<>();
        routeParams.put(SERVICENAME, "PassThroughOriginalSoapAction");
        routeParams.put(SERVICEURL, "/passThroughOriginalSoapAction");
        final String policyXml = WspWriter.getPolicyXml(new AllAssertion(Collections.singletonList(
                createRouteAssertion(ECHO_HEADERS_URL, false, new HttpPassthroughRule("SOAPAction", false, null)))));
        routeParams.put(SERVICEPOLICY, policyXml);
        testLevelCreatedServiceIds.add(createServiceFromTemplate(routeParams));

        final GenericHttpRequestParams params = new GenericHttpRequestParams(new URL("http://" + BASE_URL + ":8080/passThroughOriginalSoapAction"));
        params.addExtraHeader(new GenericHttpHeader("SOAPAction", "testSoapAction"));

        final GenericHttpResponse response = sendRequest(params, HttpMethod.GET, null);
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
                createRouteAssertion(ECHO_HEADERS_URL, false, new HttpPassthroughRule("SOAPAction", true, "customSoapAction")))));
        routeParams.put(SERVICEPOLICY, policyXml);
        testLevelCreatedServiceIds.add(createServiceFromTemplate(routeParams));

        final GenericHttpRequestParams params = new GenericHttpRequestParams(new URL("http://" + BASE_URL + ":8080/customizeSoapActionUsingRule"));
        params.addExtraHeader(new GenericHttpHeader("SOAPAction", "shouldBeReplaced"));

        final GenericHttpResponse response = sendRequest(params, HttpMethod.GET, null);
        assertEquals(200, response.getStatus());
        final String responseBody = printResponseDetails(response);
        final Map<String, Collection<String>> routedHeaders = parseHeaders(responseBody);
        assertHeaderValues(routedHeaders, "soapaction", "customSoapAction");
    }

    @Test
    public void customizeRequestSoapActionUsingAddHeaderAssertion() throws Exception {
        final Map<String, String> routeParams = new HashMap<>();
        routeParams.put(SERVICENAME, "CustomizeSoapActionUsingAddHeaderAssertion");
        routeParams.put(SERVICEURL, "/customizeSoapActionUsingAddHeaderAssertion");
        final String policyXml = WspWriter.getPolicyXml(new AllAssertion(Arrays.asList(
                createAddHeaderAssertion(SoapUtil.SOAPACTION, "customSoapAction", true),
                createRouteAssertion(ECHO_HEADERS_URL, false, new HttpPassthroughRule("SOAPAction", false, null)))));
        routeParams.put(SERVICEPOLICY, policyXml);
        testLevelCreatedServiceIds.add(createServiceFromTemplate(routeParams));

        final GenericHttpRequestParams params = new GenericHttpRequestParams(new URL("http://" + BASE_URL + ":8080/customizeSoapActionUsingAddHeaderAssertion"));
        params.addExtraHeader(new GenericHttpHeader("SOAPAction", "shouldBeReplaced"));

        final GenericHttpResponse response = sendRequest(params, HttpMethod.GET, null);
        assertEquals(200, response.getStatus());
        final String responseBody = printResponseDetails(response);
        final Map<String, Collection<String>> routedHeaders = parseHeaders(responseBody);
        assertHeaderValues(routedHeaders, "soapaction", "customSoapAction");
    }

    @Test
    public void addRequestHeaderUsingAddHeaderAssertion() throws Exception {
        final Map<String, String> routeParams = new HashMap<>();
        routeParams.put(SERVICENAME, "AddHeaderToRequest");
        routeParams.put(SERVICEURL, "/addHeaderToRequest");
        final String policyXml = WspWriter.getPolicyXml(new AllAssertion(Arrays.asList(
                createAddHeaderAssertion("foo", "bar"),
                createRouteAssertion(ECHO_HEADERS_URL, true))));
        routeParams.put(SERVICEPOLICY, policyXml);
        testLevelCreatedServiceIds.add(createServiceFromTemplate(routeParams));

        final GenericHttpResponse response = sendRequest(new GenericHttpRequestParams(new URL("http://" + BASE_URL + ":8080/addHeaderToRequest")), HttpMethod.GET, null);
        assertEquals(200, response.getStatus());
        final String responseBody = printResponseDetails(response);
        final Map<String, Collection<String>> routedHeaders = parseHeaders(responseBody);
        assertEquals(4, routedHeaders.size());
        assertHeaderValues(routedHeaders, "foo", "bar");
        assertHeaderValues(routedHeaders, "user-agent", APACHE_USER_AGENT);
        assertHeaderValues(routedHeaders, "host", BASE_URL + ":8080");
        assertHeaderValues(routedHeaders, "connection", KEEP_ALIVE);
    }

    @Test
    public void addRequestHeaderUsingAddHeaderAssertionNameAlreadyExists() throws Exception {
        final Map<String, String> routeParams = new HashMap<>();
        routeParams.put(SERVICENAME, "AddHeaderToRequestAlreadyExists");
        routeParams.put(SERVICEURL, "/addHeaderToRequestAlreadyExists");
        final String policyXml = WspWriter.getPolicyXml(new AllAssertion(Arrays.asList(
                createAddHeaderAssertion("foo", "bar"),
                createRouteAssertion(ECHO_HEADERS_URL, true))));
        routeParams.put(SERVICEPOLICY, policyXml);
        testLevelCreatedServiceIds.add(createServiceFromTemplate(routeParams));

        final GenericHttpRequestParams params = new GenericHttpRequestParams(new URL("http://" + BASE_URL + ":8080/addHeaderToRequestAlreadyExists"));
        params.addExtraHeader(new GenericHttpHeader("foo", "existingFoo"));
        final GenericHttpResponse response = sendRequest(params, HttpMethod.GET, null);
        assertEquals(200, response.getStatus());
        final String responseBody = printResponseDetails(response);
        final Map<String, Collection<String>> routedHeaders = parseHeaders(responseBody);
        assertEquals(4, routedHeaders.size());
        assertHeaderValues(routedHeaders, "foo", "existingFoo", "bar");
        assertHeaderValues(routedHeaders, "user-agent", APACHE_USER_AGENT);
        assertHeaderValues(routedHeaders, "host", BASE_URL + ":8080");
        assertHeaderValues(routedHeaders, "connection", KEEP_ALIVE);
    }

    @Test
    public void replaceRequestHeaderUsingAddHeaderAssertion() throws Exception {
        final Map<String, String> routeParams = new HashMap<>();
        routeParams.put(SERVICENAME, "ReplaceHeaderOnRequest");
        routeParams.put(SERVICEURL, "/replaceHeaderOnRequest");
        final String policyXml = WspWriter.getPolicyXml(new AllAssertion(Arrays.asList(
                createAddHeaderAssertion("foo", "bar", true),
                createRouteAssertion(ECHO_HEADERS_URL, true))));
        routeParams.put(SERVICEPOLICY, policyXml);
        testLevelCreatedServiceIds.add(createServiceFromTemplate(routeParams));

        final GenericHttpRequestParams params = new GenericHttpRequestParams(new URL("http://" + BASE_URL + ":8080/replaceHeaderOnRequest"));
        params.addExtraHeader(new GenericHttpHeader("foo", "shouldBeReplaced"));
        final GenericHttpResponse response = sendRequest(params, HttpMethod.GET, null);
        assertEquals(200, response.getStatus());
        final String responseBody = printResponseDetails(response);
        final Map<String, Collection<String>> routedHeaders = parseHeaders(responseBody);
        assertEquals(4, routedHeaders.size());
        assertHeaderValues(routedHeaders, "foo", "bar");
        assertHeaderValues(routedHeaders, "user-agent", APACHE_USER_AGENT);
        assertHeaderValues(routedHeaders, "host", BASE_URL + ":8080");
        assertHeaderValues(routedHeaders, "connection", KEEP_ALIVE);
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
                createRouteAssertion(ECHO_HEADERS_URL, false, new HttpPassthroughRule("foo", true, "bar")))));
        routeParams.put(SERVICEPOLICY, policyXml);
        testLevelCreatedServiceIds.add(createServiceFromTemplate(routeParams));

        final GenericHttpRequestParams params = new GenericHttpRequestParams(new URL("http://" + BASE_URL + ":8080/passThroughRequestHeaderRuleReplacesAddHeaderAssertion"));
        params.addExtraHeader(new GenericHttpHeader("foo", "originalHeaderValueShouldBeReplaced"));
        final GenericHttpResponse response = sendRequest(params, HttpMethod.GET, null);
        assertEquals(200, response.getStatus());
        final String responseBody = printResponseDetails(response);
        final Map<String, Collection<String>> routedHeaders = parseHeaders(responseBody);
        assertEquals(4, routedHeaders.size());
        assertHeaderValues(routedHeaders, "foo", "bar");
        assertTrue(routedHeaders.get("user-agent").iterator().next().contains(L7_USER_AGENT));
        assertHeaderValues(routedHeaders, "host", BASE_URL + ":8080");
        assertHeaderValues(routedHeaders, "connection", KEEP_ALIVE);
    }

    @Test
    public void multipleAddHeaderAssertionsSameName() throws Exception {
        final Map<String, String> routeParams = new HashMap<>();
        routeParams.put(SERVICENAME, "AddHeaderToRequest");
        routeParams.put(SERVICEURL, "/addHeaderToRequest");
        final String policyXml = WspWriter.getPolicyXml(new AllAssertion(Arrays.asList(
                createAddHeaderAssertion("foo", "bar"),
                createAddHeaderAssertion("foo", "bar2"),
                createRouteAssertion(ECHO_HEADERS_URL, true))));
        routeParams.put(SERVICEPOLICY, policyXml);
        testLevelCreatedServiceIds.add(createServiceFromTemplate(routeParams));

        final GenericHttpResponse response = sendRequest(new GenericHttpRequestParams(new URL("http://" + BASE_URL + ":8080/addHeaderToRequest")), HttpMethod.GET, null);
        assertEquals(200, response.getStatus());
        final String responseBody = printResponseDetails(response);
        final Map<String, Collection<String>> routedHeaders = parseHeaders(responseBody);
        assertEquals(4, routedHeaders.size());
        assertHeaderValues(routedHeaders, "foo", "bar", "bar2");
        assertHeaderValues(routedHeaders, "user-agent", APACHE_USER_AGENT);
        assertHeaderValues(routedHeaders, "host", BASE_URL + ":8080");
        assertHeaderValues(routedHeaders, "connection", KEEP_ALIVE);
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
                createRouteAssertion(ECHO_HEADERS_URL, true, "firstRouteResponse", null),
                createAddHeaderAssertion("bar", "addedAfterFirstRoute"),
                // store second route response in context variable
                createRouteAssertion(ECHO_HEADERS_URL, true, "secondRouteResponse", null),
                echoResponseAssertion)));
        routeParams.put(SERVICEPOLICY, policyXml);
        testLevelCreatedServiceIds.add(createServiceFromTemplate(routeParams));

        final GenericHttpResponse response = sendRequest(new GenericHttpRequestParams(new URL("http://" + BASE_URL + ":8080/multipleRoutesAndAddHeaders")), HttpMethod.GET, null);
        assertEquals(200, response.getStatus());
        final String responseBody = printResponseDetails(response);

        final String firstRouteResponseBody = responseBody.substring(responseBody.indexOf("<firstRouteResponse>") + "<firstRouteResponse>".length(), responseBody.indexOf("</firstRouteResponse"));
        final Map<String, Collection<String>> firstHeadersRouted = parseHeaders(firstRouteResponseBody);
        assertEquals(4, firstHeadersRouted.size());
        assertHeaderValues(firstHeadersRouted, "foo", "addedBeforeFirstRoute");
        assertFalse(firstHeadersRouted.containsKey("bar"));
        assertHeaderValues(firstHeadersRouted, "user-agent", APACHE_USER_AGENT);
        assertHeaderValues(firstHeadersRouted, "host", BASE_URL + ":8080");
        assertHeaderValues(firstHeadersRouted, "connection", KEEP_ALIVE);

        final String secondRouteResponseBody = responseBody.substring(responseBody.indexOf("<secondRouteResponse>") + "<secondRouteResponse>".length(), responseBody.indexOf("</secondRouteResponse"));
        final Map<String, Collection<String>> secondHeadersRouted = parseHeaders(secondRouteResponseBody);
        assertEquals(5, secondHeadersRouted.size());
        assertHeaderValues(secondHeadersRouted, "foo", "addedBeforeFirstRoute");
        assertHeaderValues(secondHeadersRouted, "bar", "addedAfterFirstRoute");
        assertHeaderValues(secondHeadersRouted, "user-agent", APACHE_USER_AGENT);
        assertHeaderValues(secondHeadersRouted, "host", BASE_URL + ":8080");
        assertHeaderValues(secondHeadersRouted, "connection", KEEP_ALIVE);
    }

    /**
     * request.http.allheadervalues only looks at incoming request headers.
     */
    @Test
    public void requestAllHeaderValuesContextVariable() throws Exception {
        final Map<String, String> routeParams = new HashMap<>();
        routeParams.put(SERVICENAME, "GetHeadersService");
        routeParams.put(SERVICEURL, "/getHeadersService");
        final String policyXml = WspWriter.getPolicyXml(new AllAssertion(Arrays.asList(
                createAddHeaderAssertion("foo", "assertionFoo", true),
                createAddHeaderAssertion("addedByAssertion", "addedByAssertionValue"),
                createHardcodedResponseAssertion("<all>${request.http.allheadervalues}</all>"))));
        routeParams.put(SERVICEPOLICY, policyXml);
        testLevelCreatedServiceIds.add(createServiceFromTemplate(routeParams));

        final GenericHttpRequestParams requestParams = new GenericHttpRequestParams(new URL("http://" + BASE_URL + ":8080/getHeadersService"));
        requestParams.addExtraHeader(new GenericHttpHeader("setOnOriginalRequest", "setOnOriginalRequestValue"));
        requestParams.addExtraHeader(new GenericHttpHeader("foo", "originalFoo"));
        final GenericHttpResponse response = sendRequest(requestParams, HttpMethod.GET, null);
        assertEquals(200, response.getStatus());
        final String responseBody = printResponseDetails(response);
        final Map<String, Collection<String>> headers = parseHeaders(responseBody);
        assertHeaderValues(headers, "foo", "originalFoo");
        assertFalse(headers.containsKey("addedbyassertion"));
        assertFalse(headers.containsKey("addedByAssertion"));
        assertHeaderValues(headers, "setonoriginalrequest", "setOnOriginalRequestValue");
    }

    /**
     * request.http.headernames only looks at incoming request headers.
     */
    @Test
    public void requestHeaderNamesContextVariable() throws Exception {
        final Map<String, String> routeParams = new HashMap<>();
        routeParams.put(SERVICENAME, "GetHeadersService");
        routeParams.put(SERVICEURL, "/getHeadersService");
        final String policyXml = WspWriter.getPolicyXml(new AllAssertion(Arrays.asList(
                createAddHeaderAssertion("foo", "assertionFoo", true),
                createAddHeaderAssertion("addedByAssertion", "addedByAssertionValue"),
                createHardcodedResponseAssertion("${request.http.headernames}"))));
        routeParams.put(SERVICEPOLICY, policyXml);
        testLevelCreatedServiceIds.add(createServiceFromTemplate(routeParams));

        final GenericHttpRequestParams requestParams = new GenericHttpRequestParams(new URL("http://" + BASE_URL + ":8080/getHeadersService"));
        requestParams.addExtraHeader(new GenericHttpHeader("setOnOriginalRequest", "setOnOriginalRequestValue"));
        requestParams.addExtraHeader(new GenericHttpHeader("foo", "originalFoo"));
        final GenericHttpResponse response = sendRequest(requestParams, HttpMethod.GET, null);
        assertEquals(200, response.getStatus());
        final String responseBody = printResponseDetails(response);
        final List<String> headerNames = Arrays.asList(StringUtils.split(responseBody, ", "));
        assertTrue(headerNames.contains("foo"));
        assertTrue(headerNames.contains("setonoriginalrequest"));
        assertFalse(headerNames.contains("addedByAssertion"));
        assertFalse(headerNames.contains("addedbyassertion"));
    }

    /**
     * request.http.header.headerName looks beyond incoming request headers if there is no header with that name on the incoming request.
     */
    @Test
    public void requestHeaderValuesByNameContextVariable() throws Exception {
        final Map<String, String> routeParams = new HashMap<>();
        routeParams.put(SERVICENAME, "GetHeadersService");
        routeParams.put(SERVICEURL, "/getHeadersService");
        final String policyXml = WspWriter.getPolicyXml(new AllAssertion(Arrays.asList(
                createAddHeaderAssertion("foo", "assertionFoo", true),
                createAddHeaderAssertion("addedByAssertion", "addedByAssertionValue"),
                createHardcodedResponseAssertion("<all>foo:${request.http.header.foo}, addedByAssertion:${request.http.header.addedByAssertion}, setOnOriginalRequest:${request.http.header.setOnOriginalRequest}</all>"))));
        routeParams.put(SERVICEPOLICY, policyXml);
        testLevelCreatedServiceIds.add(createServiceFromTemplate(routeParams));

        final GenericHttpRequestParams requestParams = new GenericHttpRequestParams(new URL("http://" + BASE_URL + ":8080/getHeadersService"));
        requestParams.addExtraHeader(new GenericHttpHeader("setOnOriginalRequest", "setOnOriginalRequestValue"));
        requestParams.addExtraHeader(new GenericHttpHeader("foo", "originalFoo"));
        final GenericHttpResponse response = sendRequest(requestParams, HttpMethod.GET, null);
        assertEquals(200, response.getStatus());
        final String responseBody = printResponseDetails(response);
        final Map<String, Collection<String>> headers = parseHeaders(responseBody);
        assertHeaderValues(headers, "foo", "originalFoo");
        assertHeaderValues(headers, "addedByAssertion", "addedByAssertionValue");
        assertHeaderValues(headers, "setOnOriginalRequest", "setOnOriginalRequestValue");
    }
}
