package com.l7tech.external.assertions.cors.server;


import com.l7tech.common.http.HttpMethod;
import com.l7tech.external.assertions.cors.CORSAssertion;
import com.l7tech.message.HeadersKnob;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.util.Functions;

import java.io.IOException;
import java.util.List;

import static com.l7tech.message.HeadersKnob.HEADER_TYPE_HTTP;

public class ServerCORSAssertion extends AbstractServerAssertion<CORSAssertion> {

    public static final String PREFLIGHT_ORIGIN_HEADER = "Origin";
    public static final String PREFLIGHT_REQUEST_METHOD_HEADER = "Access-Control-Request-Method";
    public static final String PREFLIGHT_REQUEST_HEADERS_HEADER = "Access-Control-Request-Headers";

    public static final String PREFLIGHT_RESPONSE_ALLOW_ORIGIN = "Access-Control-Allow-Origin";
    public static final String PREFLIGHT_RESPONSE_ALLOW_CREDENTIALS = "Access-Control-Allow-Credentials";
    public static final String PREFLIGHT_RESPONSE_EXPOSE_HEADERS = "Access-Control-Expose-Headers";
    public static final String PREFLIGHT_RESPONSE_CACHE_TIME = "access-control-max-age";
    public static final String PREFLIGHT_RESPONSE_ALLOW_METHODS = "access-control-allow-methods";
    public static final String PREFLIGHT_RESPONSE_ALLOW_ACCESS_HEADERS = "access-control-allow-headers";

    public ServerCORSAssertion(final CORSAssertion assertion) {
        super(assertion);
    }

    @Override
    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        final Message request = context.getRequest();
        final HeadersKnob requestHeadersKnob = request.getHeadersKnob();

        // set default variable values
        context.setVariable(assertion.getVariablePrefix() + "." + CORSAssertion.SUFFIX_IS_PREFLIGHT, false);
        context.setVariable(assertion.getVariablePrefix() + "." + CORSAssertion.SUFFIX_IS_CORS, false);

        // is a CORS request if "Origin" header is present
        if (requestHeadersKnob.containsHeader(PREFLIGHT_ORIGIN_HEADER, HEADER_TYPE_HTTP)) {
            // check origin
            final String origin = requestHeadersKnob.getHeaderValues(PREFLIGHT_ORIGIN_HEADER, HEADER_TYPE_HTTP)[0];

            if (assertion.getAcceptedOrigins() != null) {
                if(!Functions.exists(assertion.getAcceptedOrigins(), new Functions.Unary<Boolean, String>() {
                    @Override
                    public Boolean call(String s) {
                        return s.equals(origin);
                    }
                })) {
                    return AssertionStatus.NONE;
                }
            }

            context.setVariable(assertion.getVariablePrefix() + "." + CORSAssertion.SUFFIX_IS_CORS, true);

            final boolean isPreflight = request.getHttpRequestKnob().getMethod().equals(HttpMethod.OPTIONS) &&
                    requestHeadersKnob.containsHeader(PREFLIGHT_REQUEST_METHOD_HEADER, HEADER_TYPE_HTTP);

            final HeadersKnob responseHeadersKnob = context.getResponse().getHeadersKnob();

            if (isPreflight) {
                context.setVariable(assertion.getVariablePrefix() + "." + CORSAssertion.SUFFIX_IS_PREFLIGHT, true);

                // check PREFLIGHT_REQUEST_METHOD_HEADER
                String[] methods = requestHeadersKnob.getHeaderValues(PREFLIGHT_REQUEST_METHOD_HEADER, HEADER_TYPE_HTTP);
                // check PREFLIGHT_ACCESS_CONTROL_HEADERS_HEADER if exist
                String[] accessHeaders = requestHeadersKnob.getHeaderValues(PREFLIGHT_REQUEST_HEADERS_HEADER, HEADER_TYPE_HTTP);

                // set response Access-Control-Allow-Methods
                // echo back the method, actual request will not be resolved by the gateway if method is not allowed
                addHeaders(responseHeadersKnob, PREFLIGHT_RESPONSE_ALLOW_METHODS, methods);

                // set response Access-Control-Allow-Headers
                if (assertion.getAcceptedHeaders() == null) {
                    // echo back if all headers are accepted
                    addHeaders(responseHeadersKnob, PREFLIGHT_RESPONSE_ALLOW_ACCESS_HEADERS, accessHeaders);
                } else {
                    addHeaders(responseHeadersKnob, PREFLIGHT_RESPONSE_ALLOW_ACCESS_HEADERS, assertion.getAcceptedHeaders());
                }

                if (assertion.getExposedHeaders() != null) {
                    addHeaders(responseHeadersKnob, PREFLIGHT_RESPONSE_EXPOSE_HEADERS, assertion.getExposedHeaders());
                }

                if (assertion.getResponseCacheTime() != null) {
                    setHeader(responseHeadersKnob, PREFLIGHT_RESPONSE_CACHE_TIME, assertion.getResponseCacheTime().toString());
                }
            }

            setHeader(responseHeadersKnob, PREFLIGHT_RESPONSE_ALLOW_ORIGIN, origin);

            // always allow user credentials in the request
            setHeader(responseHeadersKnob, PREFLIGHT_RESPONSE_ALLOW_CREDENTIALS, "true");
        }

        return AssertionStatus.NONE;
    }

    private void setHeader(HeadersKnob headersKnob, String headerName, String headerValue) {
        headersKnob.setHeader(headerName, headerValue, HEADER_TYPE_HTTP);
    }

    private void addHeaders(HeadersKnob headersKnob, String header, List<String> values) {
        for (String val : values) {
            headersKnob.addHeader(header, val, HEADER_TYPE_HTTP);
        }
    }

    private void addHeaders(HeadersKnob headersKnob, String header, String[] values) {
        for (String val : values) {
            headersKnob.addHeader(header, val, HEADER_TYPE_HTTP);
        }
    }
}
