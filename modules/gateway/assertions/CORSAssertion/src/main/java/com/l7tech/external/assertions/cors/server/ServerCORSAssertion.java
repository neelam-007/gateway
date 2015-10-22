package com.l7tech.external.assertions.cors.server;


import com.l7tech.common.http.HttpMethod;
import com.l7tech.external.assertions.cors.CORSAssertion;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.message.Header;
import com.l7tech.message.HeadersKnob;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.util.Functions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
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

        // is a CORS request if "Origin" header is present
        if (requestHeadersKnob.containsHeader(REQUEST_ORIGIN_HEADER, HEADER_TYPE_HTTP)) {
            context.setVariable(assertion.getVariablePrefix() + "." + CORSAssertion.SUFFIX_IS_CORS, true);

            final boolean isPreflight = request.getHttpRequestKnob().getMethod().equals(HttpMethod.OPTIONS) &&
                    requestHeadersKnob.containsHeader(REQUEST_METHOD_HEADER, HEADER_TYPE_HTTP);

            context.setVariable(assertion.getVariablePrefix() + "." + CORSAssertion.SUFFIX_IS_PREFLIGHT, isPreflight);

            // check origin is accepted
            final String origin = requestHeadersKnob.getHeaderValues(REQUEST_ORIGIN_HEADER, HEADER_TYPE_HTTP)[0];

            if (assertion.getAcceptedOrigins() != null) {
                if (!findCaseSensitive(origin, assertion.getAcceptedOrigins())) {
                    logAndAudit(AssertionMessages.USERDETAIL_WARNING, "Origin not allowed: " + origin);
                    return AssertionStatus.FALSIFIED;
                }
            }

            // all origins allowed, or origin found in accepted origin list
            logAndAudit(AssertionMessages.USERDETAIL_FINE, "Origin allowed: " + origin);

            final HeadersKnob responseHeadersKnob = context.getResponse().getHeadersKnob();

            if (isPreflight) {
                // check REQUEST_METHOD_HEADER
                String[] methods = requestHeadersKnob.getHeaderValues(REQUEST_METHOD_HEADER, HEADER_TYPE_HTTP);
                
                if (methods.length != 1) {
                    logAndAudit(AssertionMessages.USERDETAIL_WARNING,
                            "The request must contain exactly one " + REQUEST_METHOD_HEADER + " header.");
                    return AssertionStatus.BAD_REQUEST;
                }

                String requestedMethod = methods[0];
                
                try {
                    HttpMethod.valueOf(requestedMethod);
                } catch (IllegalArgumentException e) {
                    logAndAudit(AssertionMessages.USERDETAIL_WARNING, "The value of the " +
                            REQUEST_METHOD_HEADER + " header is not a recognized HTTP method: " + requestedMethod);
                    return AssertionStatus.BAD_REQUEST;
                }
                
                // check ACCESS_CONTROL_HEADERS_HEADER if exist (may be multiple)
                Collection<Header> requestHeadersHeaders =
                        requestHeadersKnob.getHeaders(REQUEST_HEADERS_HEADER, HEADER_TYPE_HTTP);

                ArrayList<String> requestedHeaderFieldNames = new ArrayList<>();

                // parse the comma delimited values of any request headers into header names
                if (!requestHeadersHeaders.isEmpty()) {
                    for (Header header : requestHeadersHeaders) {
                        String headerValue = (String) header.getValue();

                        for (String fieldName : headerValue.split(",")) {
                            if (!fieldName.trim().isEmpty()) {
                                requestedHeaderFieldNames.add(fieldName.trim());
                            }
                        }
                    }
                }

                // if the method is not found (case-sensitive) in the accepted methods, fail
                if (null != assertion.getAcceptedMethods() &&
                        !findCaseSensitive(requestedMethod, assertion.getAcceptedMethods())) {
                    logAndAudit(AssertionMessages.USERDETAIL_WARNING, requestedMethod + " is not an accepted method.");
                    return AssertionStatus.FALSIFIED;
                }

                // if any header field name is not found in the accepted headers, fail
                if (null != assertion.getAcceptedHeaders()) {
                    for (String headerName : requestedHeaderFieldNames) {
                        if (!findCaseInsensitive(headerName, assertion.getAcceptedHeaders())) {
                            logAndAudit(AssertionMessages.USERDETAIL_WARNING,
                                    headerName + " is not an accepted header.");
                            return AssertionStatus.FALSIFIED;
                        }
                    }
                }

                // set response Access-Control-Allow-Methods; only list requested method (sufficient as per spec)
                setHeader(responseHeadersKnob, RESPONSE_ALLOW_METHODS, requestedMethod);

                // set response Access-Control-Allow-Headers; only list requested headers (sufficient as per spec)
                addHeaders(responseHeadersKnob, RESPONSE_ALLOW_ACCESS_HEADERS, requestedHeaderFieldNames);

                if (assertion.getResponseCacheTime() != null) {
                    setHeader(responseHeadersKnob, RESPONSE_CACHE_TIME, assertion.getResponseCacheTime().toString());
                }
            } else {
                // set the Access-Control-Expose-Headers
                if (assertion.getExposedHeaders() != null) {
                    addHeaders(responseHeadersKnob, RESPONSE_EXPOSE_HEADERS, assertion.getExposedHeaders());
                }
            }

            // if credentials are supported, set the headers to indicate such and allow the requested origin
            if (assertion.isSupportsCredentials()) {
                setHeader(responseHeadersKnob, RESPONSE_ALLOW_CREDENTIALS, "true");
                setHeader(responseHeadersKnob, RESPONSE_ALLOW_ORIGIN, origin);
            } else if (null == assertion.getAcceptedOrigins()) {    // otherwise, if all origins accepted, indicate so
                setHeader(responseHeadersKnob, RESPONSE_ALLOW_ORIGIN, "*");
            } else {    // otherwise, only show requested origin as allowed
                setHeader(responseHeadersKnob, RESPONSE_ALLOW_ORIGIN, origin);
            }
        } else {
            context.setVariable(assertion.getVariablePrefix() + "." + CORSAssertion.SUFFIX_IS_CORS, false);
            context.setVariable(assertion.getVariablePrefix() + "." + CORSAssertion.SUFFIX_IS_PREFLIGHT, false);

            if (assertion.isRequireCors()) {
                logAndAudit(AssertionMessages.USERDETAIL_WARNING, "Request must be a valid CORS request.");
                return AssertionStatus.BAD_REQUEST;
            }
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

    private void addHeaders(HeadersKnob headersKnob, String header, List<String> values) {
        for (String val : values) {
            headersKnob.addHeader(header, val, HEADER_TYPE_HTTP);
        }
    }
}
