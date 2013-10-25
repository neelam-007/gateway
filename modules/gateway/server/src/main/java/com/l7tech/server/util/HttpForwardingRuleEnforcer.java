package com.l7tech.server.util;

import com.l7tech.common.http.*;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.message.*;
import com.l7tech.policy.assertion.HttpPassthroughRule;
import com.l7tech.policy.assertion.HttpPassthroughRuleSet;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.ServerBridgeRoutingAssertion;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.util.Pair;
import com.l7tech.xml.soap.SoapUtil;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class handles the runtime enforcement of the forwarding rules
 * for http headers/parameters defined in HttpRoutingAssertion.
 *
 * <p/>
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Jan 15, 2007<br/>
 */
public class HttpForwardingRuleEnforcer {
    private static final Logger logger = Logger.getLogger(HttpForwardingRuleEnforcer.class.getName());

    /**
     * For forwarding request http headers downstream (from routing assertion)
     *
     * @param sourceMessage     the sourceMessage
     * @param httpRequestParams Target for HTTP headers
     * @param context           the pec
     * @param targetDomain      name of domain used for cookie forwarding
     * @param rules             http rules dictating what headers should be forwarded and under which conditions
     * @param auditor           for runtime auditing
     * @param vars              pre-populated map of context variables (pec.getVariableMap) or null
     * @param varNames          the context variables used by the calling assertion used to populate vars if null
     */
    public static void handleRequestHeaders(final Message sourceMessage,
                                            final GenericHttpRequestParams httpRequestParams,
                                            final PolicyEnforcementContext context,
                                            final String targetDomain,
                                            final HttpPassthroughRuleSet rules,
                                            final Audit auditor,
                                            @Nullable Map<String, ?> vars,
                                            @Nullable final String[] varNames) throws IOException {
        // we should only forward def user-agent if the rules are not going to insert own
        if (rules.ruleForName(HttpConstants.HEADER_USER_AGENT) != HttpPassthroughRuleSet.BLOCK) {
            flushExisting(HttpConstants.HEADER_USER_AGENT, httpRequestParams);
        }

        // do not modify original headers knob
        final HeadersKnob copy = new HeadersKnobSupport();
        if (sourceMessage.getHeadersKnob() != null) {
            for (final Pair<String, Object> header : sourceMessage.getHeadersKnob().getHeaders()) {
                copy.addHeader(header.getKey(), header.getValue());
            }
        } else {
            logger.log(Level.WARNING, "HeadersKnob is missing from request.");
        }
        final Set<String> ruleHeaderNames = new HashSet<>();
        if (!rules.isForwardAll()) {
            // set custom values
            for (int i = 0; i < rules.getRules().length; i++) {
                final HttpPassthroughRule rule = rules.getRules()[i];
                final String ruleName = rule.getName();
                if (ruleName.isEmpty()) {
                    throw new IOException("HttpPassthroughRule contains an HTTP header with an empty name");
                }
                ruleHeaderNames.add(ruleName.toLowerCase());
                // Handled by virtual host
                // TODO we should probably handle virtual host here instead of ServerHttpRouteAssertion?
                if (HttpConstants.HEADER_HOST.equalsIgnoreCase(ruleName)) continue;
                if (rule.isUsesCustomizedValue()) {
                    // set header with custom value
                    String headerValue = rule.getCustomizeValue();
                    // resolve context variable if applicable
                    if (varNames != null && varNames.length > 0) {
                        if (vars == null) {
                            vars = context.getVariableMap(varNames, auditor);
                        }
                        headerValue = ExpandVariables.process(headerValue, vars, auditor);
                    }
                    copy.setHeader(ruleName, headerValue);
                }
            }

            // remove headers that are not in rules
            for (final String headerName : copy.getHeaderNames()) {
                if (!ruleHeaderNames.contains(headerName.toLowerCase())) {
                    copy.removeHeader(headerName);
                }
            }
        }

        writeHeaders(copy, httpRequestParams, context, targetDomain, auditor, ruleHeaderNames.contains("cookie"));
        //still try to get and set a SOAPAction If not already set
        if (!copy.containsHeader(SoapUtil.SOAPACTION)) {
            handleSoapActionHeader(httpRequestParams, sourceMessage, null);
        }
    }


    /**
     * For forwarding request http headers downstream (from routing assertion)
     *
     * @deprecated              use {@link #handleRequestHeaders(com.l7tech.message.Message, com.l7tech.common.http.GenericHttpRequestParams, com.l7tech.server.message.PolicyEnforcementContext, String, com.l7tech.policy.assertion.HttpPassthroughRuleSet, com.l7tech.gateway.common.audit.Audit, java.util.Map, String[])}
     * @param headerSource Source for HTTP Headers (in addition to the request Message, may be null)
     * @param sourceMessage the sourceMessage
     * @param httpRequestParams Target for HTTP headers
     * @param context the pec
     * @param targetDomain name of domain used for cookie forwarding
     * @param rules http rules dictating what headers should be forwarded and under which conditions
     * @param auditor for runtime auditing
     * @param vars pre-populated map of context variables (pec.getVariableMap) or null
     * @param varNames the context variables used by the calling assertion used to populate vars if null
     */
    @Deprecated
    public static void handleRequestHeaders( @Nullable final HasOutboundHeaders headerSource,
                                             final Message sourceMessage,
                                             final GenericHttpRequestParams httpRequestParams,
                                             final PolicyEnforcementContext context,
                                             final String targetDomain,
                                             final HttpPassthroughRuleSet rules,
                                             final Audit auditor,
                                             @Nullable  Map<String,?> vars,
                                             @Nullable final String[] varNames) throws IOException {
        final HasOutboundHeaders source = headerSource == null ? new HttpOutboundRequestFacet() : headerSource;

        // we should only forward def user-agent if the rules are not going to insert own
        if (rules.ruleForName(HttpConstants.HEADER_USER_AGENT) != HttpPassthroughRuleSet.BLOCK) {
            flushExisting(HttpConstants.HEADER_USER_AGENT, httpRequestParams);
        }

        final HttpRequestKnob knob = sourceMessage.getKnob( HttpRequestKnob.class );
        if (rules.isForwardAll()) {
            // forward everything
            source.writeHeaders( httpRequestParams );
            if (knob == null) {
                logger.log(Level.FINE, "no headers to forward cause this is not an incoming http request");

                //but still try to get and set a SOAPAction If not already set.
                if ( !source.containsHeader( SoapUtil.SOAPACTION ) ) {
                    handleSoapActionHeader( httpRequestParams, sourceMessage, source );
                }
            } else {
                String[] headerNames = knob.getHeaderNames();
                boolean cookieAlreadyHandled = false; // cause all cookies are processed in one go (unlike other headers)
                for ( final String headerName : headerNames ) {
                    if (headerName.length() < 1)
                        throw new IOException("Request contains an HTTP header with an empty name");

                    if (headerShouldBeIgnored(headerName)) {
                        // some headers should just be ignored cause they are not 'application headers'
                        logger.fine("not passing through " + headerName);
                    } else if (headerName.equalsIgnoreCase(HttpConstants.HEADER_AUTHORIZATION) && httpRequestParams.getPasswordAuthentication() != null) {
                        logger.fine("not passing through authorization header because credentials are specified for back-end request"); // bug 10795
                    } else if (headerName.equalsIgnoreCase(HttpConstants.HEADER_COOKIE)) {
                        if ( !cookieAlreadyHandled ) {
                            // special cookie handling
                            List<HttpCookie> res = passableCookies(context, targetDomain, auditor);
                            if (!res.isEmpty()) {
                                httpRequestParams.addExtraHeader(new GenericHttpHeader(HttpConstants.HEADER_COOKIE,
                                                                                         HttpCookie.getCookieHeader(res)));
                            }

                            cookieAlreadyHandled = true;
                        }
                    } else if (headerName.equalsIgnoreCase(SoapUtil.SOAPACTION) && !source.containsHeader( SoapUtil.SOAPACTION )){
                        //special SOAPAction handling
                        handleSoapActionHeader( httpRequestParams, sourceMessage, source );
                    } else {
                        String[] values = knob.getHeaderValues(headerName);
                        for (String value : values) {
                            if ( !source.containsHeader(headerName) ) { // source overrides other values
                                httpRequestParams.addExtraHeader(new GenericHttpHeader(headerName, value));
                            }
                        }
                    }
                }
            }
        } else {
            if (knob == null) {
                logger.log(Level.FINE, "incoming headers wont be forwarded cause this is not an incoming http request");
            }
            for (int i = 0; i < rules.getRules().length; i++) {
                final HttpPassthroughRule rule = rules.getRules()[i];
                if (rule.isUsesCustomizedValue()) {
                    // set header with custom value
                    final String headerName = rule.getName();
                    if (headerName.length() < 1)
                        throw new IOException("Request contains an HTTP header with an empty name");
                    //Handle it by virtual host
                    if (HttpConstants.HEADER_HOST.equalsIgnoreCase(headerName)) continue;
                    String headerValue = rule.getCustomizeValue();
                    // resolve context variable if applicable
                    if (varNames != null && varNames.length > 0) {
                        if (vars == null) {
                            vars = context.getVariableMap(varNames, auditor);
                        }
                        headerValue = ExpandVariables.process(headerValue, vars, auditor);
                    }
                    httpRequestParams.addExtraHeader(new GenericHttpHeader(headerName, headerValue));
                } else {
                    final String headerNameFromRule = rule.getName();
                    if (headerNameFromRule.length() < 1)
                        throw new IOException("Request contains an HTTP header with an empty name");
                    // Use the route host instead of gateway host
                    if (HttpConstants.HEADER_HOST.equalsIgnoreCase(headerNameFromRule)) continue;

                    if (knob != null) {
                        // special cookie handling
                        if (headerNameFromRule.equalsIgnoreCase(HttpConstants.HEADER_COOKIE)) {
                            List<HttpCookie> res = passableCookies(context, targetDomain, auditor);
                            if (!res.isEmpty()) {
                                httpRequestParams.addExtraHeader(new GenericHttpHeader(HttpConstants.HEADER_COOKIE,
                                                                                         HttpCookie.getCookieHeader(res)));
                            }
                        } else if (headerNameFromRule.equalsIgnoreCase(SoapUtil.SOAPACTION)) {
                            handleSoapActionHeader( httpRequestParams, sourceMessage, source );
                        } else {
                            final String[] values = source.containsHeader(headerNameFromRule) ?
                                    source.getHeaderValues( headerNameFromRule ) :
                                    knob.getHeaderValues(headerNameFromRule);

                            for ( String value : values ) {
                                httpRequestParams.addExtraHeader(new GenericHttpHeader(headerNameFromRule, value));
                            }
                        }
                    } else {
                        //we should still try to set a SOAPAction if possible
                        if ( headerNameFromRule.equalsIgnoreCase(SoapUtil.SOAPACTION) ) {
                            handleSoapActionHeader( httpRequestParams, sourceMessage, source );
                        } else {
                            final String[] values = source.getHeaderValues(headerNameFromRule);
                            for ( final String value : values ) {
                                httpRequestParams.addExtraHeader(new GenericHttpHeader(headerNameFromRule, value));
                            }
                        }
                    }
                }
            }
        }
    }

    private static void handleSoapActionHeader( final GenericHttpRequestParams httpRequestParams,
                                                final Message sourceMessage,
                                                final HasOutboundHeaders source ) {
        final String soapAction = getSoapActionIfPossible(sourceMessage, source);
        if (soapAction != null) {
            //add the soap action header, using the original header case
            httpRequestParams.addExtraHeader(new GenericHttpHeader( SoapUtil.SOAPACTION, soapAction));
        }
    }

    private static String getSoapActionIfPossible( final Message sourceMessage,
                                                   final HasOutboundHeaders headerSource ) {
        String soapAction = null;

        if (headerSource != null) {
            String[] soapActions = headerSource.getHeaderValues(SoapUtil.SOAPACTION);
            if ( soapActions.length > 0 ) {
                soapAction = soapActions[0];
            }
        }

        MimeKnob mk = sourceMessage.getKnob(MimeKnob.class);
        boolean streaming = mk != null && mk.isBufferingDisallowed();

        if ( soapAction == null && !streaming ) {
            try {
                if (sourceMessage.isSoap()) {
                    try {
                        HasSoapAction haver = sourceMessage.getKnob(HasSoapAction.class);
                        if (haver != null) {
                            soapAction = haver.getSoapAction();
                            if (soapAction == null)
                                soapAction = "";
                        }
                    } catch (IOException e) {
                        logger.info("Request message SOAPAction is multivalued. Not setting SOAPAction.");
                    }
                }
            } catch (IOException e) {
                logger.info("Will not add a SOAPAction to the message since the request message was not SOAP.");
            } catch (SAXException e1) {
                logger.info("Will not add a SOAPAction to the message since the request message was not SOAP.");
            }
        }

        if ( soapAction == null ) {
            // Maybe the default request is a non-SOAP or even non-XML content type that just happens to have a SOAPAction header (Bug #10629)
            HttpRequestKnob hrk = sourceMessage.getKnob(HttpRequestKnob.class);
            if ( hrk != null ) {
                soapAction = hrk.getHeaderFirstValue(SoapUtil.SOAPACTION);
            }
        }

        return soapAction;
    }

    private static void flushExisting( final String headerName,
                                       final GenericHttpRequestParams routedRequestParams ) {
        final List existingHeaders = routedRequestParams.getExtraHeaders();
        for (Iterator iterator = existingHeaders.iterator(); iterator.hasNext();) {
            GenericHttpHeader gh = (GenericHttpHeader) iterator.next();
            if (headerName.compareToIgnoreCase(gh.getName()) == 0) {
                logger.finest("removing " + headerName + " user agent value " + gh.getFullValue());
                iterator.remove();
            }
        }
    }

    /**
     * Handle request headers for Bridge Routing.
     *
     * @param headerSource The source for extra headers (may be null)
     * @param sourceMessage The source message (required)
     * @param httpRequestParams The target for outbound headers (required)
     * @param context The current context (required)
     * @param rules The header processing rules (required)
     * @param auditor The auditor to use (required)
     * @param vars The relevant variables (may be null)
     * @param varNames The variables used in header rules (may be null)
     */
    public static void handleRequestHeaders( @Nullable final HasOutboundHeaders headerSource,
                                             final Message sourceMessage,
                                             final GenericHttpRequestParams httpRequestParams,
                                             final PolicyEnforcementContext context,
                                             final HttpPassthroughRuleSet rules,
                                             final Audit auditor,
                                             @Nullable Map<String,?> vars,
                                             @Nullable final String[] varNames ) {
        final HasOutboundHeaders source = headerSource == null ? new HttpOutboundRequestFacet() : headerSource;
        final HttpRequestKnob knob = sourceMessage.getKnob( HttpRequestKnob.class );

        if (rules.isForwardAll()) {
            // forward everything
            source.writeHeaders( httpRequestParams );
            if ( knob == null ) {
                logger.log(Level.FINE, "no header to forward cause this is not an incoming http request");
                return;
            }
            String[] headerNames = knob.getHeaderNames();
            boolean cookieAlreadyHandled = false; // cause all cookies are processed in one go (unlike other headers)
            for ( final String headerName : headerNames ) {
                if (headerShouldBeIgnored(headerName)) {
                    // some headers should just be ignored cause they are not 'application headers'
                    logger.fine("not passing through " + headerName);
                } else if (headerName.equalsIgnoreCase(HttpConstants.HEADER_COOKIE) && !cookieAlreadyHandled) {
                    // cookies are handled separately by the ServerBRA
                }  else if (headerName.equalsIgnoreCase(SoapUtil.SOAPACTION) && !cookieAlreadyHandled) {
                    // the bridge has it's own handling for soap action
                } else {
                    final String[] values = knob.getHeaderValues(headerName);
                    for ( final String value : values ) {
                        if ( !source.containsHeader(headerName) ) { // source overrides other values
                            httpRequestParams.addExtraHeader(new GenericHttpHeader(headerName, value));
                        }
                    }
                }
            }
        } else {
            if ( knob == null ) {
                logger.log(Level.FINE, "incoming headers wont be forwarded cause this is not an incoming http request");
            }
            for (int i = 0; i < rules.getRules().length; i++) {
                HttpPassthroughRule rule = rules.getRules()[i];
                if ( rule.isUsesCustomizedValue() ) {
                    // set header with custom value
                    String headerName = rule.getName();
                    String headerValue = rule.getCustomizeValue();
                    //Handle it by virtual host
                    if (HttpConstants.HEADER_HOST.equalsIgnoreCase(headerName)) continue;
                    // resolve context variable if applicable
                    if (varNames != null && varNames.length > 0) {
                        if (vars == null) {
                            vars = context.getVariableMap(varNames, auditor);
                        }
                        headerValue = ExpandVariables.process(headerValue, vars, auditor);
                    }
                    httpRequestParams.replaceExtraHeader(new GenericHttpHeader(headerName, headerValue));
                } else {
                    // set header with incoming value if it's present
                    final String headerName = rule.getName();
                    // special cookie handling
                    if (headerName.equalsIgnoreCase(HttpConstants.HEADER_COOKIE)) {
                        // outgoing cookies are handled separately by the ServerBra
                    } else if (headerName.equalsIgnoreCase(SoapUtil.SOAPACTION)) {
                        // the bridge already has its own handling for soap action
                    } else if (HttpConstants.HEADER_HOST.equalsIgnoreCase(headerName)) {
                        // Use the route host instead of gateway host
                    } else {
                        final String[] values = source.containsHeader(headerName) || knob == null ?
                                source.getHeaderValues( headerName ) :
                                knob.getHeaderValues( headerName );

                        for (String value : values) {
                            httpRequestParams.addExtraHeader(new GenericHttpHeader(headerName, value));
                        }
                    }
                }
            }
        }
    }

    public static class Param {
        Param( final String name, final String value) {
            this.name = name;
            this.value = value;
        }
        public final String name;
        public final String value;
    }

    public static List<Param> handleRequestParameters( final PolicyEnforcementContext context,
                                                       final Message sourceMessage,
                                                       final HttpPassthroughRuleSet rules,
                                                       final Audit auditor,
                                                       @Nullable Map<String,?> vars,
                                                       @Nullable final String[] varNames ) throws IOException {
        // 1st, make sure we have a HttpServletRequestKnob
        HttpServletRequestKnob knob = sourceMessage.getKnob( HttpServletRequestKnob.class );
        if (knob == null) {
            logger.log(Level.FINE, "no parameter to forward cause the incoming request is not http");
            return null;
        }

        // 2nd, look the for a content type which would include payload parameters "application/x-www-form-urlencoded"
        if (!isItAForm(knob)) {
            logger.log(Level.FINE, "this request is not a form post so the parameter rules are being ignored");
            return null;
        }

        ArrayList<Param> output = new ArrayList<Param>();

        // 3rd, apply the rules
        Map<String, String[]> requestBodyParameterMap = knob.getRequestBodyParameterMap();
        if (rules.isForwardAll()) {
            for (Map.Entry<String, String[]> parameter : requestBodyParameterMap.entrySet()) {
                String parameterName = parameter.getKey();
                String[] values = parameter.getValue();
                for (String value : values) {
                    output.add(new Param(parameterName, value));
                }
            }
        } else {
            for (int i = 0; i < rules.getRules().length; i++) {
                HttpPassthroughRule rule = rules.getRules()[i];
                String parameterName = rule.getName();
                if (rule.isUsesCustomizedValue()) {
                    // set parameter with custom value
                    String value = rule.getCustomizeValue();
                    // resolve context variable if applicable
                    if (varNames != null && varNames.length > 0) {
                        if (vars == null) {
                            vars = context.getVariableMap(varNames, auditor);
                        }
                        value = ExpandVariables.process(value, vars, auditor);
                    }
                    output.add(new Param(parameterName, value));
                } else {
                    String[] values = requestBodyParameterMap.get(parameterName);
                    if (values != null && values.length > 0) {
                        for (String value : values) {
                            output.add(new Param(parameterName, value));
                        }
                    }
                }
            }
        }

        return output;
    }

    public static void handleResponseHeaders( final HttpResponseKnob targetForResponseHeaders,
                                              final Audit auditor,
                                              final ServerBridgeRoutingAssertion.HeaderHolder hh,
                                              final HttpPassthroughRuleSet rules,
                                              @Nullable Map<String,?> vars,
                                              @Nullable final String[] varNames,
                                              final PolicyEnforcementContext context ) {
        if (rules.isForwardAll()) {
            final HttpHeader[] headers = hh.getHeaders().toArray();
            for ( final HttpHeader h : headers ) {
                if (headerShouldBeIgnored(h.getName())) {
                    logger.fine("ignoring header " + h.getName() + " with value " + h.getFullValue());
                } else if (HttpConstants.HEADER_SET_COOKIE.toLowerCase().equals(h.getName().toLowerCase())) {
                    // special cookie handling happen
                    // s outside this class by the ServerBRA
                } else {
                    targetForResponseHeaders.setHeader(h.getName(), h.getFullValue());
                }
            }
        } else {
            for (int i = 0; i < rules.getRules().length; i++) {
                final HttpPassthroughRule rule = rules.getRules()[i];
                if (rule.isUsesCustomizedValue()) {
                    String headerValue = rule.getCustomizeValue();
                    // resolve context variable if applicable
                    if ( varNames != null && varNames.length > 0 ) {
                        if (vars == null) {
                            vars = context.getVariableMap(varNames, auditor);
                        }
                        headerValue = ExpandVariables.process(headerValue, vars, auditor);
                    }
                    targetForResponseHeaders.setHeader(rule.getName(), headerValue);
                } else {
                    if (HttpConstants.HEADER_SET_COOKIE.toLowerCase().equals(rule.getName().toLowerCase())) {
                        // special cookie handling happens outside this class by the ServerBRA
                    } else {
                        final List<String> values = hh.getHeaders().getValues(rule.getName());
                        if ( values != null && values.size() > 0 ) {
                            for ( final String value : values ) {
                                targetForResponseHeaders.setHeader(rule.getName(), value);
                            }
                        } else {
                            logger.fine("there is a custom rule for forwarding header " + rule.getName() + " with " +
                                        "incoming value but this header is not present.");
                        }
                    }
                }
            }
        }
    }

    /**
     * Handle response http headers from a response
     * @param sourceOfResponseHeaders the response gotten from the routing assertion
     * @param targetForResponseHeaders the response message to put the pass-through headers into
     * @param context the pec
     * @param rules http rules dictating what headers should be forwarded and under which conditions
     * @param passThroughSpecialHeaders whether to pass through headers in the list {@link HttpPassthroughRuleSet#HEADERS_NOT_TO_IMPLICITLY_FORWARD}
     * @param auditor for runtime auditing
     * @param routedRequestParams httpclientproperty
     * @param vars pre-populated map of context variables (pec.getVariableMap) or null
     * @param varNames the context variables used by the calling assertion used to populate vars if null
     */
    public static void handleResponseHeaders( final HttpInboundResponseKnob sourceOfResponseHeaders,
                                              final HttpResponseKnob targetForResponseHeaders,
                                              final Audit auditor,
                                              final HttpPassthroughRuleSet rules,
                                              final boolean passThroughSpecialHeaders,
                                              final PolicyEnforcementContext context,
                                              final GenericHttpRequestParams routedRequestParams,
                                              Map<String,?> vars,
                                              final String[] varNames) {
        boolean passIncomingCookies = false;
        if (rules.isForwardAll()) {
            final boolean logFine = logger.isLoggable(Level.FINE);
            for (HttpHeader h : sourceOfResponseHeaders.getHeadersArray()) {
                if (!passThroughSpecialHeaders && headerShouldBeIgnored(h.getName())) {
                    if (logFine) logger.fine("ignoring header " + h.getName() + " with value " + h.getFullValue());
                } else if (HttpConstants.HEADER_SET_COOKIE.equals(h.getName())) {
                    // special cookie handling happens outside this loop (see below)
                } else {
                    targetForResponseHeaders.addHeader(h.getName(), h.getFullValue());
                }
            }
            passIncomingCookies = true;
        } else {
            if (passThroughSpecialHeaders) {
                for (HttpHeader h : sourceOfResponseHeaders.getHeadersArray()) {
                    if (headerShouldBeIgnored(h.getName())) {
                        targetForResponseHeaders.addHeader(h.getName(), h.getFullValue());
                    }
                }
            }

            for (int i = 0; i < rules.getRules().length; i++) {
                HttpPassthroughRule rule = rules.getRules()[i];
                if (rule.isUsesCustomizedValue()) {
                    String headerValue = rule.getCustomizeValue();
                    // resolve context variable if applicable
                    if (varNames != null && varNames.length > 0) {
                        if (vars == null) {
                            vars = context.getVariableMap(varNames, auditor);
                        }
                        headerValue = ExpandVariables.process(headerValue, vars, auditor);
                    }
                    targetForResponseHeaders.addHeader(rule.getName(), headerValue);
                } else {
                    if (HttpConstants.HEADER_SET_COOKIE.equals(rule.getName())) {
                        // special cookie handling outside this loop (see below)
                        passIncomingCookies = true;
                    } else {
                        String[] values = sourceOfResponseHeaders.getHeaderValues(rule.getName());
                        if (values != null && values.length > 0) {
                            for (String val : values) {
                                targetForResponseHeaders.addHeader(rule.getName(), val);
                            }
                        } else {
                            logger.fine("there is a custom rule for forwarding header " + rule.getName() + " with " +
                                        "incoming value but this header is not present.");
                        }
                    }
                }
            }
        }

        // handle cookies separately
        if (passIncomingCookies) {
            String[] setCookieValues = sourceOfResponseHeaders.getHeaderValues(HttpConstants.HEADER_SET_COOKIE);
            for (String setCookieValue : setCookieValues) {
                try {
                    context.addCookie(new HttpCookie(routedRequestParams.getTargetUrl(), setCookieValue));
                } catch (HttpCookie.IllegalFormatException e) {
                    auditor.logAndAudit(AssertionMessages.HTTPROUTE_INVALIDCOOKIE, setCookieValue);
                }
            }
        }
    }

    private static boolean isItAForm( final HttpRequestKnob knob ) {
        String[] contentTypeHeaders = knob.getHeaderValues("content-type");
        if (contentTypeHeaders != null) {
            for (String val : contentTypeHeaders) {
                if (val.toLowerCase().contains("form")) return true;
            }
        }
        return false;
    }

    private static boolean headerShouldBeIgnored( String headerName ) {
        return HttpPassthroughRuleSet.HEADERS_NOT_TO_IMPLICITLY_FORWARD.contains( headerName.toLowerCase() );
    }

    private static List<HttpCookie> passableCookies(PolicyEnforcementContext context, String targetDomain, Audit auditor) {
        List<HttpCookie> output = new ArrayList<HttpCookie>();
        Set<HttpCookie> contextCookies = context.getCookies();

        for ( final HttpCookie httpCookie : contextCookies ) {
            if (CookieUtils.isPassThroughCookie(httpCookie)) {
                if (httpCookie.isNew()) {
                    auditor.logAndAudit(AssertionMessages.HTTPROUTE_ADDCOOKIE_VERSION, httpCookie.getCookieName(), String.valueOf(httpCookie.getVersion()));
                } else {
                    auditor.logAndAudit(AssertionMessages.HTTPROUTE_UPDATECOOKIE, httpCookie.getCookieName());
                }
                HttpCookie newCookie = new HttpCookie(
                        httpCookie.getCookieName(),
                        httpCookie.getCookieValue(),
                        httpCookie.getVersion(),
                        "/",
                        targetDomain,
                        httpCookie.getMaxAge(),
                        httpCookie.isSecure(),
                        httpCookie.getComment()
                );
                // attach and record
                output.add(newCookie);
            }
        }
        return output;
    }

    /**
     * @param headersKnob   header source
     * @param requestParams header destination (existing headers may be replaced)
     * @throws              IOException if a header with an empty name is encountered
     */
    private static void writeHeaders(final HeadersKnob headersKnob, final GenericHttpRequestParams requestParams, final PolicyEnforcementContext context, final String targetDomain, final Audit auditor, final boolean retrieveCookiesFromContext) throws IOException {
        final List<String> processedHeaders = new ArrayList<>();
        boolean cookieAlreadyHandled = false;
        for (final String name : headersKnob.getHeaderNames()) {
            if (StringUtils.isNotBlank(name)) {
                if (name.equalsIgnoreCase(HttpConstants.HEADER_AUTHORIZATION) && requestParams.getPasswordAuthentication() != null) {
                    logger.fine("not passing through authorization header because credentials are specified for back-end request"); // bug 10795
                } else if (name.equalsIgnoreCase(HttpConstants.HEADER_COOKIE)) {
                    // special cookie handling
                    // all cookies are processed in one go (unlike other headers)
                    if (!cookieAlreadyHandled) {
                        cookieAlreadyHandled = setCookieHeadersFromContext(requestParams, context, targetDomain, auditor);
                    }
                } else {
                    for (final String value : headersKnob.getHeaderValues(name)) {
                        if (processedHeaders.contains(name)) {
                            requestParams.addExtraHeader(new GenericHttpHeader(name, value));
                        } else {
                            // only replace existing headers on request params
                            requestParams.replaceExtraHeader(new GenericHttpHeader(name, value));
                            processedHeaders.add(name);
                        }
                    }
                }
            } else {
                throw new IOException("HeadersKnob contains a header with an empty name");
            }
        }
        if (retrieveCookiesFromContext && !cookieAlreadyHandled) {
            setCookieHeadersFromContext(requestParams, context, targetDomain, auditor);
        }

    }

    private static boolean setCookieHeadersFromContext(GenericHttpRequestParams requestParams, PolicyEnforcementContext context, String targetDomain, Audit auditor) {
        boolean cookieAlreadyHandled;
        final List<HttpCookie> res = passableCookies(context, targetDomain, auditor);
        if (!res.isEmpty()) {
            // currently only passes the name and value cookie attributes
            requestParams.addExtraHeader(new GenericHttpHeader(HttpConstants.HEADER_COOKIE, HttpCookie.getCookieHeader(res)));
        }
        cookieAlreadyHandled = true;
        return cookieAlreadyHandled;
    }
}
