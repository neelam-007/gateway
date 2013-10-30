package com.l7tech.server;

import com.l7tech.common.http.GenericHttpHeader;
import com.l7tech.common.http.GenericHttpRequestParams;
import com.l7tech.common.http.GenericHttpResponse;
import com.l7tech.common.http.HttpMethod;
import com.l7tech.policy.assertion.AddHeaderAssertion;
import com.l7tech.policy.assertion.HttpPassthroughRule;
import com.l7tech.policy.assertion.HttpRoutingAssertion;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.wsp.WspWriter;
import com.l7tech.xml.soap.SoapUtil;
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
@Ignore
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
        assertHeaderValues(headers, "Content-Type", "text/plain;charset=UTF-8");
        assertTrue(headers.containsKey("Content-Length"));
        assertTrue(headers.containsKey("Date"));

        final Map<String, Collection<String>> routedHeaders = getRoutedHeaders(responseBody);
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

        final Map<String, Collection<String>> routedHeaders = getRoutedHeaders(responseBody);
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

        final Map<String, Collection<String>> routedHeaders = getRoutedHeaders(responseBody);
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
        final HttpRoutingAssertion routeAssertion = new HttpRoutingAssertion();
        routeAssertion.setProtectedServiceUrl("http://" + BASE_URL + ":8080/echoHeaders");
        routeAssertion.getRequestHeaderRules().setForwardAll(true);
        final Map<String, String> routeParams = new HashMap<>();
        routeParams.put(SERVICENAME, "PassThroughAllRequestHeaders");
        routeParams.put(SERVICEURL, "/passThroughAllRequestHeaders");
        final String policyXml = WspWriter.getPolicyXml(new AllAssertion(Collections.singletonList(routeAssertion)));
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
        final Map<String, Collection<String>> routedHeaders = getRoutedHeaders(responseBody);
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

        final HttpRoutingAssertion routeAssertion = new HttpRoutingAssertion();
        routeAssertion.setProtectedServiceUrl("http://" + BASE_URL + ":8080/echoHeaders");
        routeAssertion.getRequestHeaderRules().setForwardAll(false);
        routeAssertion.getRequestHeaderRules().setRules(rules.toArray(new HttpPassthroughRule[rules.size()]));
        final Map<String, String> routeParams = new HashMap<>();
        routeParams.put(SERVICENAME, "PassThroughSomeRequestHeaders");
        routeParams.put(SERVICEURL, "/passThroughSomeRequestHeaders");
        final String policyXml = WspWriter.getPolicyXml(new AllAssertion(Collections.singletonList(routeAssertion)));
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
        final Map<String, Collection<String>> routedHeaders = getRoutedHeaders(responseBody);
        assertEquals(4, routedHeaders.size());
        assertHeaderValues(routedHeaders, "foo", "customFoo");
        assertHeaderValues(routedHeaders, "user-agent", "testUserAgent");
        assertHeaderValues(routedHeaders, "host", BASE_URL + ":8080");
        assertHeaderValues(routedHeaders, "connection", KEEP_ALIVE);
    }

    @Test
    public void customizeRequestHostToPassThrough() throws Exception {
        final HttpRoutingAssertion routeAssertion = new HttpRoutingAssertion();
        routeAssertion.setProtectedServiceUrl("http://" + BASE_URL + ":8080/echoHeaders");
        routeAssertion.getRequestHeaderRules().setForwardAll(false);
        routeAssertion.getRequestHeaderRules().setRules(new HttpPassthroughRule[]{new HttpPassthroughRule("Host", true, "customHost")});
        final Map<String, String> routeParams = new HashMap<>();
        routeParams.put(SERVICENAME, "PassThroughCustomHost");
        routeParams.put(SERVICEURL, "/passThroughCustomHost");
        final String policyXml = WspWriter.getPolicyXml(new AllAssertion(Collections.singletonList(routeAssertion)));
        routeParams.put(SERVICEPOLICY, policyXml);
        testLevelCreatedServiceIds.add(createServiceFromTemplate(routeParams));

        final GenericHttpRequestParams params = new GenericHttpRequestParams(new URL("http://" + BASE_URL + ":8080/passThroughCustomHost"));
        params.addExtraHeader(new GenericHttpHeader("Host", "testHost"));

        final GenericHttpResponse response = sendRequest(params, HttpMethod.GET, null);
        assertEquals(200, response.getStatus());
        final String responseBody = printResponseDetails(response);
        final Map<String, Collection<String>> routedHeaders = getRoutedHeaders(responseBody);
        assertHeaderValues(routedHeaders, "host", "customHost:8080");
    }

    @Test
    public void customizeRequestHostWithPortToPassThrough() throws Exception {
        final HttpRoutingAssertion routeAssertion = new HttpRoutingAssertion();
        routeAssertion.setProtectedServiceUrl("http://" + BASE_URL + ":8080/echoHeaders");
        routeAssertion.getRequestHeaderRules().setForwardAll(false);
        routeAssertion.getRequestHeaderRules().setRules(new HttpPassthroughRule[]{new HttpPassthroughRule("Host", true, "customHost:8888")});
        final Map<String, String> routeParams = new HashMap<>();
        routeParams.put(SERVICENAME, "PassThroughCustomHostWithPort");
        routeParams.put(SERVICEURL, "/passThroughCustomHostWithPort");
        final String policyXml = WspWriter.getPolicyXml(new AllAssertion(Collections.singletonList(routeAssertion)));
        routeParams.put(SERVICEPOLICY, policyXml);
        testLevelCreatedServiceIds.add(createServiceFromTemplate(routeParams));

        final GenericHttpRequestParams params = new GenericHttpRequestParams(new URL("http://" + BASE_URL + ":8080/passThroughCustomHostWithPort"));
        params.addExtraHeader(new GenericHttpHeader("Host", "testHost"));

        final GenericHttpResponse response = sendRequest(params, HttpMethod.GET, null);
        assertEquals(200, response.getStatus());
        final String responseBody = printResponseDetails(response);
        final Map<String, Collection<String>> routedHeaders = getRoutedHeaders(responseBody);
        assertHeaderValues(routedHeaders, "host", "customHost:8888");
    }

    @Test
    public void passThroughOriginalRequestSoapAction() throws Exception {
        final HttpRoutingAssertion routeAssertion = new HttpRoutingAssertion();
        routeAssertion.setProtectedServiceUrl("http://" + BASE_URL + ":8080/echoHeaders");
        routeAssertion.getRequestHeaderRules().setForwardAll(false);
        routeAssertion.getRequestHeaderRules().setRules(new HttpPassthroughRule[]{new HttpPassthroughRule("SOAPAction", false, null)});
        final Map<String, String> routeParams = new HashMap<>();
        routeParams.put(SERVICENAME, "PassThroughOriginalSoapAction");
        routeParams.put(SERVICEURL, "/passThroughOriginalSoapAction");
        final String policyXml = WspWriter.getPolicyXml(new AllAssertion(Collections.singletonList(routeAssertion)));
        routeParams.put(SERVICEPOLICY, policyXml);
        testLevelCreatedServiceIds.add(createServiceFromTemplate(routeParams));

        final GenericHttpRequestParams params = new GenericHttpRequestParams(new URL("http://" + BASE_URL + ":8080/passThroughOriginalSoapAction"));
        params.addExtraHeader(new GenericHttpHeader("SOAPAction", "testSoapAction"));

        final GenericHttpResponse response = sendRequest(params, HttpMethod.GET, null);
        assertEquals(200, response.getStatus());
        final String responseBody = printResponseDetails(response);
        final Map<String, Collection<String>> routedHeaders = getRoutedHeaders(responseBody);
        assertHeaderValues(routedHeaders, "soapaction", "testSoapAction");
    }

    @Test
    public void customizeRequestSoapActionUsingRule() throws Exception {
        final HttpRoutingAssertion routeAssertion = new HttpRoutingAssertion();
        routeAssertion.setProtectedServiceUrl("http://" + BASE_URL + ":8080/echoHeaders");
        routeAssertion.getRequestHeaderRules().setForwardAll(false);
        routeAssertion.getRequestHeaderRules().setRules(new HttpPassthroughRule[]{new HttpPassthroughRule("SOAPAction", true, "customSoapAction")});
        final Map<String, String> routeParams = new HashMap<>();
        routeParams.put(SERVICENAME, "CustomizeSoapActionUsingRule");
        routeParams.put(SERVICEURL, "/customizeSoapActionUsingRule");
        final String policyXml = WspWriter.getPolicyXml(new AllAssertion(Collections.singletonList(routeAssertion)));
        routeParams.put(SERVICEPOLICY, policyXml);
        testLevelCreatedServiceIds.add(createServiceFromTemplate(routeParams));

        final GenericHttpRequestParams params = new GenericHttpRequestParams(new URL("http://" + BASE_URL + ":8080/customizeSoapActionUsingRule"));
        params.addExtraHeader(new GenericHttpHeader("SOAPAction", "shouldBeReplaced"));

        final GenericHttpResponse response = sendRequest(params, HttpMethod.GET, null);
        assertEquals(200, response.getStatus());
        final String responseBody = printResponseDetails(response);
        final Map<String, Collection<String>> routedHeaders = getRoutedHeaders(responseBody);
        assertHeaderValues(routedHeaders, "soapaction", "customSoapAction");
    }

    @Test
    public void customizeRequestSoapActionUsingAddHeaderAssertion() throws Exception {
        final AddHeaderAssertion addHeaderAssertion = new AddHeaderAssertion();
        addHeaderAssertion.setHeaderName(SoapUtil.SOAPACTION);
        addHeaderAssertion.setRemoveExisting(true);
        addHeaderAssertion.setHeaderValue("customSoapAction");
        final HttpRoutingAssertion routeAssertion = new HttpRoutingAssertion();
        routeAssertion.setProtectedServiceUrl("http://" + BASE_URL + ":8080/echoHeaders");
        routeAssertion.getRequestHeaderRules().setForwardAll(false);
        routeAssertion.getRequestHeaderRules().setRules(new HttpPassthroughRule[]{new HttpPassthroughRule("SOAPAction", false, null)});
        final Map<String, String> routeParams = new HashMap<>();
        routeParams.put(SERVICENAME, "CustomizeSoapActionUsingAddHeaderAssertion");
        routeParams.put(SERVICEURL, "/customizeSoapActionUsingAddHeaderAssertion");
        final String policyXml = WspWriter.getPolicyXml(new AllAssertion(Arrays.asList(addHeaderAssertion, routeAssertion)));
        routeParams.put(SERVICEPOLICY, policyXml);
        testLevelCreatedServiceIds.add(createServiceFromTemplate(routeParams));

        final GenericHttpRequestParams params = new GenericHttpRequestParams(new URL("http://" + BASE_URL + ":8080/customizeSoapActionUsingAddHeaderAssertion"));
        params.addExtraHeader(new GenericHttpHeader("SOAPAction", "shouldBeReplaced"));

        final GenericHttpResponse response = sendRequest(params, HttpMethod.GET, null);
        assertEquals(200, response.getStatus());
        final String responseBody = printResponseDetails(response);
        final Map<String, Collection<String>> routedHeaders = getRoutedHeaders(responseBody);
        assertHeaderValues(routedHeaders, "soapaction", "customSoapAction");
    }


}
