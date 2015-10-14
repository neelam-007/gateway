package com.l7tech.external.assertions.cors.server;


import com.l7tech.common.http.HttpMethod;
import com.l7tech.external.assertions.cors.CORSAssertion;
import com.l7tech.message.HeadersKnob;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.StashManagerFactory;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.util.Functions;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.util.List;

public class ServerCORSAssertion extends AbstractServerAssertion<CORSAssertion> {

    public static String preflightOriginHeader = "origin";
    public static String preflightMethodHeader = "Access-Control-Request-Method";
    public static String preflightAccessControlHeadersHeader = "Access-Control-Request-Headers";

    public static String preflightResponseAllowOrigin = "Access-Control-Allow-Origin";
    public static String preflightResponseAllowCredientials = "Access-Control-Allow-Credentials";
    public static String preflightResponseExposeHeaders = "Access-Control-Expose-Headers";
    public static String preflightResponseCacheTime = "access-control-max-age";
    public static String preflightResponseAllowMethods = "access-control-allow-methods";
    public static String preflightResponseAllowAccessHeaders = "access-control-allow-headers";

    public ServerCORSAssertion(CORSAssertion assertion,
                               final ApplicationContext springContext) throws PolicyAssertionException {
        super(assertion);
    }

    protected ServerCORSAssertion(final CORSAssertion assertion,
                                  final StashManagerFactory stashManagerFactory) {
        super(assertion);
    }

    @Override
    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        final Message request = context.getRequest();
        final HeadersKnob requestHeadersKnob = request.getHeadersKnob();

        // is a CORS request if "Origin" header is present
        context.setVariable(assertion.getVariablePrefix() + "." + CORSAssertion.SUFFIX_IS_PREFLIGHT, false);
        context.setVariable(assertion.getVariablePrefix() + "." + CORSAssertion.SUFFIX_IS_CORS, false);

        if (requestHeadersKnob.containsHeader(preflightOriginHeader, HeadersKnob.HEADER_TYPE_HTTP)) {

            // check origin
            final String origin = requestHeadersKnob.getHeaderValues(preflightOriginHeader, HeadersKnob.HEADER_TYPE_HTTP)[0];
            if (assertion.getAcceptedOrigins() != null) {
                    if(!Functions.exists(assertion.getAcceptedOrigins(), new Functions.Unary<Boolean, String>() {
                        @Override
                        public Boolean call(String s) {
                            return s.equals(origin);
                        }
                    })){

                        return AssertionStatus.NONE;
                }
            }

            context.setVariable(assertion.getVariablePrefix() + "." + CORSAssertion.SUFFIX_IS_CORS, true);

            final boolean isPreflight = request.getHttpRequestKnob().getMethod().equals(HttpMethod.OPTIONS) &&
                    requestHeadersKnob.containsHeader(preflightMethodHeader, HeadersKnob.HEADER_TYPE_HTTP);

            final Message response = context.getResponse();
            if (isPreflight) {

                context.setVariable(assertion.getVariablePrefix() + "." + CORSAssertion.SUFFIX_IS_PREFLIGHT, true);

                // check preflightMethodHeader
                String[] methods = requestHeadersKnob.getHeaderValues(preflightMethodHeader, HeadersKnob.HEADER_TYPE_HTTP);
                // check preflightAccessControlHeadersHeader if exist
                String[] accessHeaders = requestHeadersKnob.getHeaderValues(preflightAccessControlHeadersHeader, HeadersKnob.HEADER_TYPE_HTTP);

                // set response Access-Control-Allow-Methods
                // echo back the method, actual request will not be resolved by the gateway if method is not allowed
                addHeaders(response.getHeadersKnob(), preflightResponseAllowMethods, methods);

                // set response Access-Control-Allow-Headers
                if (assertion.getAcceptedHeaders() == null) {
                    // echo back if all headers are accepted
                    addHeaders(response.getHeadersKnob(), preflightResponseAllowAccessHeaders, accessHeaders);
                } else {
                    addHeaders(response.getHeadersKnob(), preflightResponseAllowAccessHeaders, assertion.getAcceptedHeaders());
                }


                if (assertion.getResponseCacheTime() != null) {
                    response.getHeadersKnob().setHeader(preflightResponseCacheTime, assertion.getResponseCacheTime().toString(), HeadersKnob.HEADER_TYPE_HTTP);
                }

                if(assertion.getExposedHeaders()!=null){
                    addHeaders(response.getHeadersKnob(), preflightResponseExposeHeaders, assertion.getExposedHeaders());
                }
            }

            response.getHeadersKnob().setHeader( preflightResponseAllowOrigin, origin, HeadersKnob.HEADER_TYPE_HTTP);

            // always allow user credentials  in the request
            response.getHeadersKnob().setHeader(preflightResponseAllowCredientials, "true", HeadersKnob.HEADER_TYPE_HTTP);

        }

        return AssertionStatus.NONE;
    }

    private void addHeaders(HeadersKnob headersKnob, String header, List<String> values) {
        for (String val : values) {
            headersKnob.addHeader(header, val, HeadersKnob.HEADER_TYPE_HTTP);
        }
    }

    private void addHeaders(HeadersKnob headersKnob, String header, String[] values) {
        for (String val : values) {
            headersKnob.addHeader(header, val, HeadersKnob.HEADER_TYPE_HTTP);
        }
    }


}
