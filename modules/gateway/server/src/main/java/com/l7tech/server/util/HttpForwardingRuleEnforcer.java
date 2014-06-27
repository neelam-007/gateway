package com.l7tech.server.util;

import com.l7tech.common.http.*;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.message.*;
import com.l7tech.policy.assertion.HttpPassthroughRule;
import com.l7tech.policy.assertion.HttpPassthroughRuleSet;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.variable.ExpandVariables;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.message.HeadersKnob.HEADER_TYPE_HTTP;

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
        final HeadersKnob copy = copyAndFilterNonPassThroughHeaders(sourceMessage.getHeadersKnob());
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
                    if (ruleName.equalsIgnoreCase(HttpConstants.HEADER_HOST)) {
                        // use virtual host - SSG-6543
                        httpRequestParams.setVirtualHost(headerValue);
                        copy.removeHeader(HttpConstants.HEADER_HOST, HEADER_TYPE_HTTP);
                        logger.fine("virtual-host override set: " + headerValue);
                    } else {
                        copy.setHeader(ruleName, headerValue, HEADER_TYPE_HTTP);
                    }
                }
            }

            // remove headers that are not in rules
            for (final String headerName : copy.getHeaderNames(HEADER_TYPE_HTTP)) {
                if (!ruleHeaderNames.contains(headerName.toLowerCase())) {
                    copy.removeHeader(headerName, HEADER_TYPE_HTTP);
                }
            }
        }

        writeHeaders(copy, httpRequestParams, targetDomain, auditor, rules.isForwardAll() || ruleHeaderNames.contains("cookie"));
    }

    private static HeadersKnob copyAndFilterNonPassThroughHeaders(final HeadersKnob toCopy) {
        final HeadersKnob copy = new HeadersKnobSupport();
        for (final Header header : toCopy.getHeaders(HEADER_TYPE_HTTP, false)) {
            copy.addHeader(header.getKey(), header.getValue(), HEADER_TYPE_HTTP, header.isPassThrough());
        }
        return copy;
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

    /**
     * Handle response http headers from a response
     * @param sourceOfResponseHeaders the response from the routing assertion
     * @param targetMessage the response Message to put the pass-through headers into
     * @param context the pec
     * @param rules http rules dictating what headers should be forwarded and under which conditions
     * @param auditor for runtime auditing
     * @param routedRequestParams httpclientproperty
     * @param vars pre-populated map of context variables (pec.getVariableMap) or null
     * @param varNames the context variables used by the calling assertion used to populate vars if null
     */
    public static void handleResponseHeaders( final HttpInboundResponseKnob sourceOfResponseHeaders,
                                             final Message targetMessage,
                                             final Audit auditor,
                                             final HttpPassthroughRuleSet rules,
                                             final PolicyEnforcementContext context,
                                             final GenericHttpRequestParams routedRequestParams,
                                              @Nullable Map<String,?> vars,
                                             @Nullable final String[] varNames) {
        if (targetMessage != null && targetMessage.getHeadersKnob() != null) {
            final HeadersKnob targetForResponseHeaders = targetMessage.getHeadersKnob();
            final URL requestUrl = routedRequestParams.getTargetUrl();
            if (rules.isForwardAll()) {
                final boolean logFine = logger.isLoggable(Level.FINE);
                for (final HttpHeader headerFromResponse : sourceOfResponseHeaders.getHeadersArray()) {
                    if (HttpConstants.HEADER_SET_COOKIE.equalsIgnoreCase(headerFromResponse.getName())) {
                        // special cookie handling
                        String setCookieValue = headerFromResponse.getFullValue();
                        addCookie2HeadersKnob(context, targetForResponseHeaders, requestUrl, setCookieValue);
                    } else {
                        final boolean passThrough = !headerShouldBeIgnored(headerFromResponse.getName());
                        if (!passThrough && logFine) {
                            logger.fine("Adding non-passThrough header " + headerFromResponse.getName() + " with value " + headerFromResponse.getFullValue());
                        }
                        targetForResponseHeaders.addHeader(headerFromResponse.getName(), headerFromResponse.getFullValue(), HEADER_TYPE_HTTP, passThrough);
                    }
                }
            } else {
                for (final HttpPassthroughRule rule : rules.getRules()) {
                    if (rule.isUsesCustomizedValue()) {
                        String headerValue = rule.getCustomizeValue();
                        // resolve context variable if applicable
                        if (varNames != null && varNames.length > 0) {
                            if (vars == null) {
                                vars = context.getVariableMap(varNames, auditor);
                            }
                            headerValue = ExpandVariables.process(headerValue, vars, auditor);
                        }
                        targetForResponseHeaders.addHeader(rule.getName(), headerValue, HEADER_TYPE_HTTP);
                    } else {
                        final String[] values = sourceOfResponseHeaders.getHeaderValues(rule.getName());
                        if (values != null && values.length > 0) {
                            for (final String val : values) {
                                if (HttpConstants.HEADER_SET_COOKIE.equalsIgnoreCase(rule.getName())) {
                                    // special cookie handling
                                    addCookie2HeadersKnob(context,targetForResponseHeaders,requestUrl, val);
                                }
                                else {
                                    targetForResponseHeaders.addHeader(rule.getName(), val, HEADER_TYPE_HTTP);
                                }
                            }
                        } else {
                            logger.fine("there is a custom rule for forwarding header " + rule.getName() + " with " +
                                    "incoming value but this header is not present.");
                        }
                    }
                }
            }
        } else {
            logger.log(Level.WARNING, "Unable to forward response headers because headers knob is null.");
        }
    }

    private static void addCookie2HeadersKnob(PolicyEnforcementContext context, HeadersKnob targetForResponseHeaders, URL requestUrl, String setCookieValue) {
        StringBuffer sb = new StringBuffer();
        String domain =  context.isOverwriteResponseCookieDomain() ? CookieUtils.DOMAIN + CookieUtils.EQUALS + requestUrl.getHost() : null;
        String path = context.isOverwriteResponseCookiePath() ? CookieUtils.PATH + CookieUtils.EQUALS + requestUrl.getPath() : null;
        List<String> cookies = CookieUtils.splitCookieHeader(setCookieValue);
        for(int i = 0; i < cookies.size(); i++ ) {
            if(i > 0) sb.append(CookieUtils.ATTRIBUTE_DELIMITER);
            sb.append(CookieUtils.addCookieDomainAndPath(cookies.get(i), domain, path));
        }
        String modifiedCookie = sb.toString();
        targetForResponseHeaders.addHeader(HttpConstants.HEADER_SET_COOKIE,modifiedCookie, HeadersKnob.HEADER_TYPE_HTTP);
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

    /**
     * Filters gateway-specific cookies and strips out all cookie attributes except name=value.
     */
    private static Collection<String> passableCookieHeaders(HttpCookiesKnob cookiesKnob, Audit auditor) {
        final List<String> passableCookies = new ArrayList<>();
        for (final String cookieHeader : cookiesKnob.getCookiesAsHeaders()) {
            try {
                final HttpCookie httpCookie = new HttpCookie(cookieHeader);
                if (CookieUtils.isPassThroughCookie(httpCookie)) {
                    auditor.logAndAudit(AssertionMessages.HTTPROUTE_ADDCOOKIE_VERSION, httpCookie.getCookieName(), String.valueOf(httpCookie.getVersion()));
                    // currently only passes the name and value cookie attributes
                    final String nameAndValue = CookieUtils.getNameAndValue(cookieHeader);
                    if (nameAndValue != null) {
                        passableCookies.add(nameAndValue);
                    } else {
                        // cannot extract name and value - pass it through anyways
                        passableCookies.add(cookieHeader);
                    }
                }
            } catch (final HttpCookie.IllegalFormatException e) {
                // cannot parse - pass it through anyways
                passableCookies.add(cookieHeader);
            }
        }
        return passableCookies;
    }

    /**
     * @param headersKnob   request header source
     * @param requestParams request header destination (existing headers may be replaced)
     * @throws              IOException if a header with an empty name is encountered
     */
    private static void writeHeaders(final HeadersKnob headersKnob, final GenericHttpRequestParams requestParams, final String targetDomain, final Audit auditor, final boolean retrieveCookiesFromContext) throws IOException {
        final List<String> processedHeaders = new ArrayList<>();
        final HttpCookiesKnob cookiesKnob = new HttpCookiesKnobImpl(headersKnob, HttpConstants.HEADER_COOKIE);
        boolean cookieAlreadyHandled = false;
        for (final String name : headersKnob.getHeaderNames(HEADER_TYPE_HTTP)) {
            if (StringUtils.isNotBlank(name)) {
                if (name.equalsIgnoreCase(HttpConstants.HEADER_AUTHORIZATION) && requestParams.getPasswordAuthentication() != null) {
                    logger.fine("not passing through authorization header because credentials are specified for back-end request"); // bug 10795
                } else if (name.equalsIgnoreCase(HttpConstants.HEADER_COOKIE)) {
                    // special cookie handling
                    // all cookies are processed in one go (unlike other headers)
                    if (!cookieAlreadyHandled) {
                        setCookiesHeader(requestParams, cookiesKnob, auditor);
                        cookieAlreadyHandled = true;
                    }
                } else {
                    for (final String value : headersKnob.getHeaderValues(name, HEADER_TYPE_HTTP)) {
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
            setCookiesHeader(requestParams, cookiesKnob, auditor);
        }

    }

    private static void setCookiesHeader(final GenericHttpRequestParams requestParams, final HttpCookiesKnob cookiesKnob, final Audit auditor) {
        final Collection<String> result = passableCookieHeaders(cookiesKnob, auditor);
        if (!result.isEmpty()) {
            final String joined = StringUtils.join(result, "; ");
            requestParams.addExtraHeader(new GenericHttpHeader(HttpConstants.HEADER_COOKIE, joined));
        }
    }
}
