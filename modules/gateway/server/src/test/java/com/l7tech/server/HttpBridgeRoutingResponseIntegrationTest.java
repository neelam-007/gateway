package com.l7tech.server;

import com.l7tech.common.http.GenericHttpResponse;
import com.l7tech.common.http.HttpMethod;
import com.l7tech.policy.assertion.BridgeRoutingAssertion;
import com.l7tech.policy.assertion.HttpPassthroughRule;
import com.l7tech.policy.assertion.TargetMessageType;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.wsp.WspWriter;
import org.junit.Ignore;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Routing integration tests specific to the bridge routed response.
 *
 * @see {@link HttpRoutingIntegrationTest for configuration}
 */
public class HttpBridgeRoutingResponseIntegrationTest extends HttpRoutingIntegrationTest {
    /**
     * If the route response sets a cookie, it should be passed through by default.
     */
    @Test
    public void responseSetCookieHeaderPassedByDefault() throws Exception {
        final BridgeRoutingAssertion bridge = new BridgeRoutingAssertion();
        bridge.setProtectedServiceUrl("http://" + BASE_URL + ":8080/setCookieService");
        bridge.setUseSslByDefault(false);
        final String routePolicy = WspWriter.getPolicyXml(new AllAssertion(Collections.singletonList(bridge)));
        final Map<String, String> routeParams = new HashMap<>();
        routeParams.put(SERVICENAME, "RouteToSetCookieService");
        routeParams.put(SERVICEURL, "/routeToSetCookieService");
        routeParams.put(SERVICEPOLICY, routePolicy);
        testLevelCreatedServiceIds.add(createServiceFromTemplate(routeParams));

        final String setCookiePolicy = WspWriter.getPolicyXml(new AllAssertion(assertionList(
                createEchoHeadersHardcodedResponseAssertion(),
                createAddHeaderAssertion(TargetMessageType.RESPONSE, "Set-Cookie", "foo=bar"))));
        final Map<String, String> setCookieParams = new HashMap<>();
        setCookieParams.put(SERVICENAME, "SetCookieService");
        setCookieParams.put(SERVICEURL, "/setCookieService");
        setCookieParams.put(SERVICEPOLICY, setCookiePolicy);
        testLevelCreatedServiceIds.add(createServiceFromTemplate(setCookieParams));

        final GenericHttpResponse response = sendRequest(createSoapParams("http://" + BASE_URL + ":8080/routeToSetCookieService"), HttpMethod.POST, SOAP_BODY);
        printResponseDetails(response);
        assertEquals(200, response.getStatus());
        final Map<String, Collection<String>> responseHeaders = getResponseHeaders(response);
        assertHeaderValues(responseHeaders, "Set-Cookie", "foo=bar; Domain=" + BASE_URL + "; Path=/routeToSetCookieService");
    }

    @Test
    public void responseInvalidSetCookieHeaderNotPassed() throws Exception {
        final BridgeRoutingAssertion bridge = new BridgeRoutingAssertion();
        bridge.setProtectedServiceUrl("http://" + BASE_URL + ":8080/setCookieService");
        bridge.setUseSslByDefault(false);
        final String routePolicy = WspWriter.getPolicyXml(new AllAssertion(Collections.singletonList(bridge)));
        final Map<String, String> routeParams = new HashMap<>();
        routeParams.put(SERVICENAME, "RouteToSetCookieService");
        routeParams.put(SERVICEURL, "/routeToSetCookieService");
        routeParams.put(SERVICEPOLICY, routePolicy);
        testLevelCreatedServiceIds.add(createServiceFromTemplate(routeParams));

        final String setCookiePolicy = WspWriter.getPolicyXml(new AllAssertion(assertionList(
                createEchoHeadersHardcodedResponseAssertion(),
                // set cookie header has empty value
                createAddHeaderAssertion(TargetMessageType.RESPONSE, "Set-Cookie", ""))));
        final Map<String, String> setCookieParams = new HashMap<>();
        setCookieParams.put(SERVICENAME, "SetCookieService");
        setCookieParams.put(SERVICEURL, "/setCookieService");
        setCookieParams.put(SERVICEPOLICY, setCookiePolicy);
        testLevelCreatedServiceIds.add(createServiceFromTemplate(setCookieParams));

        final GenericHttpResponse response = sendRequest(createSoapParams("http://" + BASE_URL + ":8080/routeToSetCookieService"), HttpMethod.POST, SOAP_BODY);
        printResponseDetails(response);
        assertEquals(200, response.getStatus());
        final Map<String, Collection<String>> responseHeaders = getResponseHeaders(response);
        assertFalse(responseHeaders.containsKey("Set-Cookie"));
    }

    /**
     * If the route response sets any non-application headers, they should not be passed by default.
     */
    @Test
    public void nonApplicationResponseHeadersFilteredByDefault() throws Exception {
        final BridgeRoutingAssertion bridge = new BridgeRoutingAssertion();
        bridge.setProtectedServiceUrl("http://" + BASE_URL + ":8080/setNonApplicationHeadersService");
        bridge.setUseSslByDefault(false);
        final Map<String, String> routeParams = new HashMap<>();
        routeParams.put(SERVICENAME, "RouteToSetNonApplicationHeadersService");
        routeParams.put(SERVICEURL, "/routeToSetNonApplicationHeadersService");
        routeParams.put(SERVICEPOLICY, WspWriter.getPolicyXml(new AllAssertion(Collections.singletonList(bridge))));
        testLevelCreatedServiceIds.add(createServiceFromTemplate(routeParams));

        final String setNonApplicationHeadersPolicy = WspWriter.getPolicyXml(new AllAssertion(assertionList(
                createEchoHeadersHardcodedResponseAssertion(),
                createAddHeaderAssertion(TargetMessageType.RESPONSE, "Date", "0000"),
                createAddHeaderAssertion(TargetMessageType.RESPONSE, "Server", "shouldNotBePassed"),
                createAddHeaderAssertion(TargetMessageType.RESPONSE, "Connection", "shouldNotBePassed"),
                createAddHeaderAssertion(TargetMessageType.RESPONSE, "Host", "shouldNotBePassed"))));
        final Map<String, String> setHeadersParams = new HashMap<>();
        setHeadersParams.put(SERVICENAME, "SetNonApplicationHeadersService");
        setHeadersParams.put(SERVICEURL, "/setNonApplicationHeadersService");
        setHeadersParams.put(SERVICEPOLICY, setNonApplicationHeadersPolicy);
        testLevelCreatedServiceIds.add(createServiceFromTemplate(setHeadersParams));

        final GenericHttpResponse response = sendRequest(createSoapParams("http://" + BASE_URL + ":8080/routeToSetNonApplicationHeadersService"), HttpMethod.POST, SOAP_BODY);
        printResponseDetails(response);
        assertEquals(200, response.getStatus());
        final Map<String, Collection<String>> responseHeaders = getResponseHeaders(response);
        assertEquals(4, responseHeaders.size());
        assertTrue(responseHeaders.containsKey("Date"));
        assertEquals(1, responseHeaders.get("Date").size());
        assertFalse(responseHeaders.get("Date").iterator().next().equals("0000"));
        assertHeaderValues(responseHeaders, "Server", APACHE_SERVER);
        assertFalse(responseHeaders.containsKey("Connection"));
        assertFalse(responseHeaders.containsKey("Host"));
        assertHeaderValues(responseHeaders, "Content-Type", "text/plain");
        assertTrue(responseHeaders.containsKey("Content-Length"));
    }

    @Test
    public void passThroughAllResponseHeaders() throws Exception {
        final Map<String, String> routeParams = new HashMap<>();
        routeParams.put(SERVICENAME, "RouteToSetHeadersService");
        routeParams.put(SERVICEURL, "/routeToSetHeadersService");
        routeParams.put(SERVICEPOLICY, WspWriter.getPolicyXml(new AllAssertion(Collections.singletonList(
                createResponseBridgeRouteAssertion("http://" + BASE_URL + ":8080/setHeadersService", true)))));
        testLevelCreatedServiceIds.add(createServiceFromTemplate(routeParams));

        final String setHeadersPolicy = WspWriter.getPolicyXml(new AllAssertion(assertionList(
                createEchoHeadersHardcodedResponseAssertion(),
                createAddHeaderAssertion(TargetMessageType.RESPONSE, "foo", "bar"),
                createAddHeaderAssertion(TargetMessageType.RESPONSE, "foo", "foo2"),
                createAddHeaderAssertion(TargetMessageType.RESPONSE, "Server", "shouldBeReplaced"),
                createAddHeaderAssertion(TargetMessageType.RESPONSE, "Date", "shouldBeReplaced"),
                createAddHeaderAssertion(TargetMessageType.RESPONSE, "Connection", "testConnection"))));
        final Map<String, String> setHeadersParams = new HashMap<>();
        setHeadersParams.put(SERVICENAME, "SetHeadersService");
        setHeadersParams.put(SERVICEURL, "/setHeadersService");
        setHeadersParams.put(SERVICEPOLICY, setHeadersPolicy);
        testLevelCreatedServiceIds.add(createServiceFromTemplate(setHeadersParams));

        final GenericHttpResponse response = sendRequest(createSoapParams("http://" + BASE_URL + ":8080/routeToSetHeadersService"), HttpMethod.POST, SOAP_BODY);
        printResponseDetails(response);
        assertEquals(200, response.getStatus());
        final Map<String, Collection<String>> responseHeaders = getResponseHeaders(response);
        assertEquals(5, responseHeaders.size());
        // this is probably a bug - only 2nd header value is returned in the response
        assertHeaderValues(responseHeaders, "foo", "foo2");
        assertHeaderValues(responseHeaders, "Server", APACHE_SERVER);
        assertHeaderValues(responseHeaders, "Content-Type", "text/plain");
        assertTrue(responseHeaders.containsKey("Date"));
        assertTrue(responseHeaders.containsKey("Content-Length"));
    }

    @Test
    public void customizeResponseHeadersToPassThrough() throws Exception {
        final List<HttpPassthroughRule> rules = new ArrayList<>();
        // use original value
        rules.add(new HttpPassthroughRule("foo", false, null));
        // use custom value
        rules.add(new HttpPassthroughRule("customHeader", true, "customValue"));
        final Map<String, String> routeParams = new HashMap<>();
        routeParams.put(SERVICENAME, "RouteToSetHeadersService");
        routeParams.put(SERVICEURL, "/routeToSetHeadersService");
        routeParams.put(SERVICEPOLICY, WspWriter.getPolicyXml(new AllAssertion(Collections.singletonList(
                createResponseBridgeRouteAssertion("http://" + BASE_URL + ":8080/setHeadersService", false, rules)))));
        testLevelCreatedServiceIds.add(createServiceFromTemplate(routeParams));

        final String setHeadersPolicy = WspWriter.getPolicyXml(new AllAssertion(assertionList(
                createEchoHeadersHardcodedResponseAssertion(),
                createAddHeaderAssertion(TargetMessageType.RESPONSE, "foo", "shouldBePassed"),
                createAddHeaderAssertion(TargetMessageType.RESPONSE, "foo", "shouldBePassed2"),
                createAddHeaderAssertion(TargetMessageType.RESPONSE, "customHeader", "shouldBeReplaced"),
                createAddHeaderAssertion(TargetMessageType.RESPONSE, "shouldNotBePassed", "shouldNotBePassed"))));
        final Map<String, String> setHeadersParams = new HashMap<>();
        setHeadersParams.put(SERVICENAME, "SetHeadersService");
        setHeadersParams.put(SERVICEURL, "/setHeadersService");
        setHeadersParams.put(SERVICEPOLICY, setHeadersPolicy);
        testLevelCreatedServiceIds.add(createServiceFromTemplate(setHeadersParams));

        final GenericHttpResponse response = sendRequest(createSoapParams("http://" + BASE_URL + ":8080/routeToSetHeadersService"), HttpMethod.POST, SOAP_BODY);
        printResponseDetails(response);
        assertEquals(200, response.getStatus());
        final Map<String, Collection<String>> responseHeaders = getResponseHeaders(response);
        assertEquals(6, responseHeaders.size());
        // this is probably a bug - only 2nd header value is returned in the response
        assertHeaderValues(responseHeaders, "foo", "shouldBePassed2");
        assertHeaderValues(responseHeaders, "customHeader", "customValue");
        assertFalse(responseHeaders.containsKey("shouldNotBePassed"));
        assertHeaderValues(responseHeaders, "Server", APACHE_SERVER);
        assertHeaderValues(responseHeaders, "Content-Type", "text/plain");
        assertTrue(responseHeaders.containsKey("Date"));
    }

    @Test
    public void addResponseHeaderUsingAddHeaderAssertion() throws Exception {
        final Map<String, String> routeParams = new HashMap<>();
        routeParams.put(SERVICENAME, "RouteToEchoHeadersService");
        routeParams.put(SERVICEURL, "/routeToEchoHeadersService");
        routeParams.put(SERVICEPOLICY, WspWriter.getPolicyXml(new AllAssertion(assertionList(
                createResponseBridgeRouteAssertion(ECHO_HEADERS_URL, true),
                createAddHeaderAssertion(TargetMessageType.RESPONSE, "foo", "shouldBeAdded")))));
        testLevelCreatedServiceIds.add(createServiceFromTemplate(routeParams));

        final GenericHttpResponse response = sendRequest(createSoapParams("http://" + BASE_URL + ":8080/routeToEchoHeadersService"), HttpMethod.POST, SOAP_BODY);
        printResponseDetails(response);
        assertEquals(200, response.getStatus());
        final Map<String, Collection<String>> responseHeaders = getResponseHeaders(response);
        assertEquals(5, responseHeaders.size());
        assertHeaderValues(responseHeaders, "foo", "shouldBeAdded");
        assertHeaderValues(responseHeaders, "Server", APACHE_SERVER);
        assertHeaderValues(responseHeaders, "Content-Type", "text/xml;charset=UTF-8");
        assertTrue(responseHeaders.containsKey("Date"));
    }

    @Test
    public void addResponseHeaderUsingAddHeaderAssertionNameAlreadyExists() throws Exception {
        final Map<String, String> routeParams = new HashMap<>();
        routeParams.put(SERVICENAME, "RouteToSetHeadersService");
        routeParams.put(SERVICEURL, "/routeToSetHeadersService");
        routeParams.put(SERVICEPOLICY, WspWriter.getPolicyXml(new AllAssertion(assertionList(
                createResponseBridgeRouteAssertion("http://" + BASE_URL + ":8080/setHeadersService", true),
                createAddHeaderAssertion(TargetMessageType.RESPONSE, "foo", "valueFromRouteService", false)))));
        testLevelCreatedServiceIds.add(createServiceFromTemplate(routeParams));

        final String setHeadersPolicy = WspWriter.getPolicyXml(new AllAssertion(assertionList(
                createEchoHeadersHardcodedResponseAssertion(),
                createAddHeaderAssertion(TargetMessageType.RESPONSE, "foo", "valueFromSetHeaderService"))));
        final Map<String, String> setHeadersParams = new HashMap<>();
        setHeadersParams.put(SERVICENAME, "SetHeadersService");
        setHeadersParams.put(SERVICEURL, "/setHeadersService");
        setHeadersParams.put(SERVICEPOLICY, setHeadersPolicy);
        testLevelCreatedServiceIds.add(createServiceFromTemplate(setHeadersParams));

        final GenericHttpResponse response = sendRequest(createSoapParams("http://" + BASE_URL + ":8080/routeToSetHeadersService"), HttpMethod.POST, SOAP_BODY);
        printResponseDetails(response);
        assertEquals(200, response.getStatus());
        final Map<String, Collection<String>> responseHeaders = getResponseHeaders(response);
        assertEquals(5, responseHeaders.size());
        assertHeaderValues(responseHeaders, "foo", "valueFromSetHeaderService", "valueFromRouteService");
        assertHeaderValues(responseHeaders, "Server", APACHE_SERVER);
        assertHeaderValues(responseHeaders, "Content-Type", "text/plain");
        assertTrue(responseHeaders.containsKey("Content-Length"));
        assertTrue(responseHeaders.containsKey("Date"));
    }

    @Test
    public void replaceResponseHeaderUsingAddHeaderAssertion() throws Exception {
        final Map<String, String> routeParams = new HashMap<>();
        routeParams.put(SERVICENAME, "RouteToSetHeadersService");
        routeParams.put(SERVICEURL, "/routeToSetHeadersService");
        routeParams.put(SERVICEPOLICY, WspWriter.getPolicyXml(new AllAssertion(assertionList(
                createResponseBridgeRouteAssertion("http://" + BASE_URL + ":8080/setHeadersService", true),
                createAddHeaderAssertion(TargetMessageType.RESPONSE, "foo", "valueFromRouteService", true)))));
        testLevelCreatedServiceIds.add(createServiceFromTemplate(routeParams));

        final String setHeadersPolicy = WspWriter.getPolicyXml(new AllAssertion(assertionList(
                createEchoHeadersHardcodedResponseAssertion(),
                createAddHeaderAssertion(TargetMessageType.RESPONSE, "foo", "valueFromSetHeaderService"))));
        final Map<String, String> setHeadersParams = new HashMap<>();
        setHeadersParams.put(SERVICENAME, "SetHeadersService");
        setHeadersParams.put(SERVICEURL, "/setHeadersService");
        setHeadersParams.put(SERVICEPOLICY, setHeadersPolicy);
        testLevelCreatedServiceIds.add(createServiceFromTemplate(setHeadersParams));

        final GenericHttpResponse response = sendRequest(createSoapParams("http://" + BASE_URL + ":8080/routeToSetHeadersService"), HttpMethod.POST, SOAP_BODY);
        printResponseDetails(response);
        assertEquals(200, response.getStatus());
        final Map<String, Collection<String>> responseHeaders = getResponseHeaders(response);
        assertEquals(5, responseHeaders.size());
        assertHeaderValues(responseHeaders, "foo", "valueFromRouteService");
        assertHeaderValues(responseHeaders, "Server", APACHE_SERVER);
        assertHeaderValues(responseHeaders, "Content-Type", "text/plain");
        assertTrue(responseHeaders.containsKey("Content-Length"));
        assertTrue(responseHeaders.containsKey("Date"));
    }

    @Test
    public void multipleAddHeaderAssertionsSameName() throws Exception {
        final Map<String, String> routeParams = new HashMap<>();
        routeParams.put(SERVICENAME, "RouteToEchoHeadersService");
        routeParams.put(SERVICEURL, "/routeToEchoHeadersService");
        routeParams.put(SERVICEPOLICY, WspWriter.getPolicyXml(new AllAssertion(assertionList(
                createResponseBridgeRouteAssertion(ECHO_HEADERS_URL, true),
                createAddHeaderAssertion(TargetMessageType.RESPONSE, "foo", "bar"),
                createAddHeaderAssertion(TargetMessageType.RESPONSE, "foo", "bar2")))));
        testLevelCreatedServiceIds.add(createServiceFromTemplate(routeParams));

        final GenericHttpResponse response = sendRequest(createSoapParams("http://" + BASE_URL + ":8080/routeToEchoHeadersService"), HttpMethod.POST, SOAP_BODY);
        printResponseDetails(response);
        assertEquals(200, response.getStatus());
        final Map<String, Collection<String>> responseHeaders = getResponseHeaders(response);
        assertEquals(5, responseHeaders.size());
        assertHeaderValues(responseHeaders, "foo", "bar", "bar2");
        assertHeaderValues(responseHeaders, "Server", APACHE_SERVER);
        assertHeaderValues(responseHeaders, "Content-Type", "text/xml;charset=UTF-8");
        assertTrue(responseHeaders.containsKey("Date"));
    }

    /**
     * Add header assertion replaces response header rule replaces original response header.
     */
    @Test
    public void addHeaderAssertionReplacesPassThroughResponseRule() throws Exception {
        final Map<String, String> routeParams = new HashMap<>();
        routeParams.put(SERVICENAME, "RouteToSetHeadersService");
        routeParams.put(SERVICEURL, "/routeToSetHeadersService");
        routeParams.put(SERVICEPOLICY, WspWriter.getPolicyXml(new AllAssertion(assertionList(
                createResponseBridgeRouteAssertion("http://" + BASE_URL + ":8080/setHeadersService", false, new HttpPassthroughRule("foo", true, "valueFromRule")),
                createAddHeaderAssertion(TargetMessageType.RESPONSE, "foo", "valueFromAddHeader", true)))));
        testLevelCreatedServiceIds.add(createServiceFromTemplate(routeParams));

        final String setHeadersPolicy = WspWriter.getPolicyXml(new AllAssertion(assertionList(
                createEchoHeadersHardcodedResponseAssertion(),
                createAddHeaderAssertion(TargetMessageType.RESPONSE, "foo", "valueFromSetHeaderService"))));
        final Map<String, String> setHeadersParams = new HashMap<>();
        setHeadersParams.put(SERVICENAME, "SetHeadersService");
        setHeadersParams.put(SERVICEURL, "/setHeadersService");
        setHeadersParams.put(SERVICEPOLICY, setHeadersPolicy);
        testLevelCreatedServiceIds.add(createServiceFromTemplate(setHeadersParams));

        final GenericHttpResponse response = sendRequest(createSoapParams("http://" + BASE_URL + ":8080/routeToSetHeadersService"), HttpMethod.POST, SOAP_BODY);
        printResponseDetails(response);
        assertEquals(200, response.getStatus());
        final Map<String, Collection<String>> responseHeaders = getResponseHeaders(response);
        assertEquals(5, responseHeaders.size());
        assertHeaderValues(responseHeaders, "foo", "valueFromAddHeader");
        assertHeaderValues(responseHeaders, "Server", APACHE_SERVER);
        assertHeaderValues(responseHeaders, "Content-Type", "text/plain");
        assertTrue(responseHeaders.containsKey("Content-Length"));
        assertTrue(responseHeaders.containsKey("Date"));
    }

    @Test
    public void addHeaderFromRuleAndAssertion() throws Exception {
        final Map<String, String> routeParams = new HashMap<>();
        routeParams.put(SERVICENAME, "RouteToSetHeadersService");
        routeParams.put(SERVICEURL, "/routeToSetHeadersService");
        routeParams.put(SERVICEPOLICY, WspWriter.getPolicyXml(new AllAssertion(assertionList(
                createResponseBridgeRouteAssertion("http://" + BASE_URL + ":8080/setHeadersService", false, new HttpPassthroughRule("foo", true, "valueFromRule")),
                createAddHeaderAssertion(TargetMessageType.RESPONSE, "foo", "valueFromAddHeader", false)))));
        testLevelCreatedServiceIds.add(createServiceFromTemplate(routeParams));

        final String setHeadersPolicy = WspWriter.getPolicyXml(new AllAssertion(assertionList(
                createEchoHeadersHardcodedResponseAssertion(),
                createAddHeaderAssertion(TargetMessageType.RESPONSE, "foo", "valueFromSetHeaderService"))));
        final Map<String, String> setHeadersParams = new HashMap<>();
        setHeadersParams.put(SERVICENAME, "SetHeadersService");
        setHeadersParams.put(SERVICEURL, "/setHeadersService");
        setHeadersParams.put(SERVICEPOLICY, setHeadersPolicy);
        testLevelCreatedServiceIds.add(createServiceFromTemplate(setHeadersParams));

        final GenericHttpResponse response = sendRequest(createSoapParams("http://" + BASE_URL + ":8080/routeToSetHeadersService"), HttpMethod.POST, SOAP_BODY);
        printResponseDetails(response);
        assertEquals(200, response.getStatus());
        final Map<String, Collection<String>> responseHeaders = getResponseHeaders(response);
        assertEquals(5, responseHeaders.size());
        assertHeaderValues(responseHeaders, "foo", "valueFromRule", "valueFromAddHeader");
        assertHeaderValues(responseHeaders, "Server", APACHE_SERVER);
        assertHeaderValues(responseHeaders, "Content-Type", "text/plain");
        assertTrue(responseHeaders.containsKey("Content-Length"));
        assertTrue(responseHeaders.containsKey("Date"));
    }

    @Test
    public void multipleRoutesAggregatesHeaders() throws Exception {
        final Map<String, String> routeParams = new HashMap<>();
        routeParams.put(SERVICENAME, "MultipleRoutes");
        routeParams.put(SERVICEURL, "/multipleRoutes");
        routeParams.put(SERVICEPOLICY, WspWriter.getPolicyXml(new AllAssertion(assertionList(
                createResponseBridgeRouteAssertion("http://" + BASE_URL + ":8080/firstSetHeadersService", true),
                createResponseBridgeRouteAssertion("http://" + BASE_URL + ":8080/secondSetHeadersService", true)))));
        testLevelCreatedServiceIds.add(createServiceFromTemplate(routeParams));

        final Map<String, String> firstSetHeadersParams = new HashMap<>();
        firstSetHeadersParams.put(SERVICENAME, "FirstSetHeadersService");
        firstSetHeadersParams.put(SERVICEURL, "/firstSetHeadersService");
        firstSetHeadersParams.put(SERVICEPOLICY, WspWriter.getPolicyXml(new AllAssertion(assertionList(
                createHardcodedResponseAssertion("text/plain", "responseFromFirstRoute"),
                createAddHeaderAssertion(TargetMessageType.RESPONSE, "foo", "firstFoo"),
                createAddHeaderAssertion(TargetMessageType.RESPONSE, "Date", "dateFromFirstRoute"),
                createAddHeaderAssertion(TargetMessageType.RESPONSE, "setByFirstRoute", "firstValue")))));
        testLevelCreatedServiceIds.add(createServiceFromTemplate(firstSetHeadersParams));

        final Map<String, String> secondSetHeadersParams = new HashMap<>();
        secondSetHeadersParams.put(SERVICENAME, "SecondSetHeadersService");
        secondSetHeadersParams.put(SERVICEURL, "/secondSetHeadersService");
        secondSetHeadersParams.put(SERVICEPOLICY, WspWriter.getPolicyXml(new AllAssertion(assertionList(
                createHardcodedResponseAssertion("text/plain", "responseFromSecondRoute"),
                createAddHeaderAssertion(TargetMessageType.RESPONSE, "foo", "secondFoo"),
                createAddHeaderAssertion(TargetMessageType.RESPONSE, "Date", "dateFromSecondRoute"),
                createAddHeaderAssertion(TargetMessageType.RESPONSE, "setBySecondRoute", "secondValue")))));
        testLevelCreatedServiceIds.add(createServiceFromTemplate(secondSetHeadersParams));

        final GenericHttpResponse response = sendRequest(createSoapParams("http://" + BASE_URL + ":8080/multipleRoutes"), HttpMethod.POST, SOAP_BODY);
        final String responseBody = printResponseDetails(response);
        assertEquals("responseFromSecondRoute", responseBody);
        assertEquals(200, response.getStatus());
        final Map<String, Collection<String>> responseHeaders = getResponseHeaders(response);
        assertEquals(7, responseHeaders.size());
        // this is probably a bug - only 2nd header value is returned in the response
        assertHeaderValues(responseHeaders, "foo", "secondFoo");
        assertHeaderValues(responseHeaders, "setByFirstRoute", "firstValue");
        assertHeaderValues(responseHeaders, "setBySecondRoute", "secondValue");
        assertHeaderValues(responseHeaders, "Server", APACHE_SERVER);
        assertHeaderValues(responseHeaders, "Content-Type", "text/plain");
        assertTrue(responseHeaders.containsKey("Content-Length"));
        assertTrue(responseHeaders.containsKey("Date"));
        final String dateHeader = responseHeaders.get("Date").iterator().next();
        assertFalse(dateHeader.equals("dateFromFirstRoute"));
        assertFalse(dateHeader.equals("dateFromSecondRoute"));
    }
}
