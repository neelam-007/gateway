package com.l7tech.external.assertions.cors.server;


import com.l7tech.common.http.HttpMethod;
import com.l7tech.external.assertions.cors.CORSAssertion;
import com.l7tech.gateway.common.audit.AssertionMessages;
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

    public static final String REQUEST_ORIGIN_HEADER = "Origin";
    public static final String REQUEST_METHOD_HEADER = "Access-Control-Request-Method";
    public static final String REQUEST_HEADERS_HEADER = "Access-Control-Request-Headers";

    public static final String RESPONSE_ALLOW_ORIGIN = "Access-Control-Allow-Origin";
    public static final String RESPONSE_ALLOW_CREDENTIALS = "Access-Control-Allow-Credentials";
    public static final String RESPONSE_EXPOSE_HEADERS = "Access-Control-Expose-Headers";
    public static final String RESPONSE_CACHE_TIME = "Access-Control-Max-Age";
    public static final String RESPONSE_ALLOW_METHODS = "Access-Control-Allow-Methods";
    public static final String RESPONSE_ALLOW_ACCESS_HEADERS = "Access-Control-Allow-Headers";

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
        if (requestHeadersKnob.containsHeader(REQUEST_ORIGIN_HEADER, HEADER_TYPE_HTTP)) {
            // check origin is accepted
            final String origin = requestHeadersKnob.getHeaderValues(REQUEST_ORIGIN_HEADER, HEADER_TYPE_HTTP)[0];

            if (assertion.getAcceptedOrigins() != null) {
                if (!findCaseSensitive(origin, assertion.getAcceptedOrigins())) {
                    logAndAudit(AssertionMessages.CORS_ORIGIN_NOT_ALLOWED, origin);
                    return AssertionStatus.FALSIFIED;
                }
            }

            // all origins allowed, or origin found in accepted origin list
            logAndAudit(AssertionMessages.CORS_ORIGIN_ALLOWED, origin);

            context.setVariable(assertion.getVariablePrefix() + "." + CORSAssertion.SUFFIX_IS_CORS, true);

            final boolean isPreflight = request.getHttpRequestKnob().getMethod().equals(HttpMethod.OPTIONS) &&
                    requestHeadersKnob.containsHeader(REQUEST_METHOD_HEADER, HEADER_TYPE_HTTP);

            final HeadersKnob responseHeadersKnob = context.getResponse().getHeadersKnob();

            if (isPreflight) {
                context.setVariable(assertion.getVariablePrefix() + "." + CORSAssertion.SUFFIX_IS_PREFLIGHT, true);

                // check REQUEST_METHOD_HEADER
                String[] methods = requestHeadersKnob.getHeaderValues(REQUEST_METHOD_HEADER, HEADER_TYPE_HTTP);
                
                if (methods.length != 1) {
                    // TODO jwilliams: some kind of error - a single method must be specified
                    return AssertionStatus.FALSIFIED;
                }

                String requestMethod = methods[0];
                
                try {
                    HttpMethod.valueOf(requestMethod);
                } catch (IllegalArgumentException e) {
                    return AssertionStatus.FALSIFIED;
                    // TODO jwilliams: unrecognized HTTP method
                }
                
                // TODO jwilliams: may be multiple Access-Control-Request-Headers headers, each with comma-delimited values
                // check ACCESS_CONTROL_HEADERS_HEADER if exist
                String[] accessHeaders = requestHeadersKnob.getHeaderValues(REQUEST_HEADERS_HEADER, HEADER_TYPE_HTTP);


                // if the method is not found (case-sensitive) in the accepted methods, fail
                if (null != assertion.getAcceptedMethods() &&
                        !findCaseSensitive(requestMethod, assertion.getAcceptedMethods())) {
                    // TODO jwilliams: log error
                    return AssertionStatus.FALSIFIED;
                }

                // if any header field name is not found in the accepted headers, fail
                if (null != assertion.getAcceptedMethods()) {
                    for (String headerName : accessHeaders) {
                        if (!findCaseInsensitive(headerName, assertion.getAcceptedHeaders())) {
                            // TODO jwilliams: log error
                            return AssertionStatus.FALSIFIED;
                        }
                    }
                }

                // TODO jwilliams: skip if a simple method?
                // set response Access-Control-Allow-Methods
                addHeaders(responseHeadersKnob, RESPONSE_ALLOW_METHODS, requestMethod);

                // TODO jwilliams: skip if all headers are simple and Content-Type not present?
                // set response Access-Control-Allow-Headers
                addHeaders(responseHeadersKnob, RESPONSE_ALLOW_ACCESS_HEADERS, accessHeaders);

                if (assertion.getResponseCacheTime() != null) {
                    setHeader(responseHeadersKnob, RESPONSE_CACHE_TIME, assertion.getResponseCacheTime().toString());
                }
            } else {
                // set the Access-Control-Expose-Headers
                if (assertion.getExposedHeaders() != null) {
                    addHeaders(responseHeadersKnob, RESPONSE_EXPOSE_HEADERS, assertion.getExposedHeaders());
                }
            }

            // if credentials are supported, set the header to indicate and allow requested origin
            if (assertion.isSupportsCredentials()) {
                setHeader(responseHeadersKnob, RESPONSE_ALLOW_CREDENTIALS, "true");
                setHeader(responseHeadersKnob, RESPONSE_ALLOW_ORIGIN, origin);
            } else if (null == assertion.getAcceptedOrigins()) {    // otherwise, if all origins accepted, indicate so
                setHeader(responseHeadersKnob, RESPONSE_ALLOW_ORIGIN, "*");
            } else {    // otherwise, only show requested origin as allowed
                setHeader(responseHeadersKnob, RESPONSE_ALLOW_ORIGIN, origin);
            }
            
            if (isPreflight) {
                // TODO jwilliams: return HTTP 200, no body
            } else {
                // TODO jwilliams: continue to further request processing
            }
        } else if (assertion.isRequireCors()) {
            // TODO jwilliams: Not a valid CORS request
            return AssertionStatus.FALSIFIED;
        }

        return AssertionStatus.NONE;
    }

    private boolean findCaseSensitive(final String term, final Iterable<String> list) {
        return Functions.exists(list, new Functions.Unary<Boolean, String>() {
            @Override
            public Boolean call(String s) {
                return s.equals(term);
            }
        });
    }

    private boolean findCaseInsensitive(final String term, final Iterable<String> list) {
        return Functions.exists(list, new Functions.Unary<Boolean, String>() {
            @Override
            public Boolean call(String s) {
                return s.equalsIgnoreCase(term);
            }
        });
    }

    private void setHeader(HeadersKnob headersKnob, String headerName, String headerValue) {
        headersKnob.setHeader(headerName, headerValue, HEADER_TYPE_HTTP);
    }

    // TODO jwilliams: rewrite so values are concatenated, comma-delimited; set single header
    private void addHeaders(HeadersKnob headersKnob, String header, List<String> values) {
        for (String val : values) {
            headersKnob.addHeader(header, val, HEADER_TYPE_HTTP);
        }
    }

    private void addHeaders(HeadersKnob headersKnob, String header, String ... values) {
        for (String val : values) {
            headersKnob.addHeader(header, val, HEADER_TYPE_HTTP);
        }
    }
}
