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
     * @param rules             http rules dictating what headers should be forwarded and under which conditions
     * @param auditor           for runtime auditing
     * @param vars              pre-populated map of context variables (pec.getVariableMap) or null
     * @param varNames          the context variables used by the calling assertion used to populate vars if null
     */
    public static void handleRequestHeaders(final Message sourceMessage,
                                            final GenericHttpRequestParams httpRequestParams,
                                            final PolicyEnforcementContext context,
                                            final HttpPassthroughRuleSet rules,
                                            final Audit auditor,
                                            @Nullable Map<String, ?> vars,
                                            @Nullable final String[] varNames) throws IOException {
        // we should only forward def user-agent if the rules are not going to insert own
        if (rules.ruleForName(HttpConstants.HEADER_USER_AGENT) != HttpPassthroughRuleSet.BLOCK) {
            flushExisting(HttpConstants.HEADER_USER_AGENT, httpRequestParams);
        }

        final List<String> cookieHeaders = new ArrayList<>();
        if (!rules.isForwardAll()) {
            final Set<String> processedHeaders = new HashSet<>();
            for (int i = 0; i < rules.getRules().length; i++) {
                final HttpPassthroughRule rule = rules.getRules()[i];
                final String ruleName = rule.getName();
                if (ruleName.isEmpty()) {
                    throw new IOException("HttpPassthroughRule contains an HTTP header with an empty name");
                }
                if (rule.isUsesCustomizedValue()) {
                    // set header with custom value
                    String headerValue = rule.getCustomizeValue();
                    if (varNames != null && varNames.length > 0) {
                        if (vars == null) {
                            vars = context.getVariableMap(varNames, auditor);
                        }
                        headerValue = ExpandVariables.process(headerValue, vars, auditor);
                    }
                    if (ruleName.equalsIgnoreCase(HttpConstants.HEADER_HOST)) {
                        // use virtual host - SSG-6543
                        httpRequestParams.setVirtualHost(headerValue);
                        httpRequestParams.removeExtraHeader(HttpConstants.HEADER_HOST);
                        logger.fine("virtual-host override set: " + headerValue);
                    } else if (ruleName.equalsIgnoreCase(HttpConstants.HEADER_COOKIE)) {
                        cookieHeaders.addAll(CookieUtils.splitCookieHeader(headerValue));
                    } else {
                        writeRequestHeader(ruleName, new String[]{headerValue}, !processedHeaders.contains(ruleName.toLowerCase()), httpRequestParams);
                    }
                } else {
                    final String[] headerValues = sourceMessage.getHeadersKnob().getHeaderValues(ruleName, HEADER_TYPE_HTTP, false);
                    // use original value(s)
                    if (ruleName.equalsIgnoreCase(HttpConstants.HEADER_COOKIE)) {
                        for (final String headerValue : headerValues) {
                            cookieHeaders.addAll(CookieUtils.splitCookieHeader(headerValue));
                        }
                    } else {
                        writeRequestHeader(ruleName, headerValues, !processedHeaders.contains(ruleName.toLowerCase()), httpRequestParams);
                    }
                }
                processedHeaders.add(ruleName.toLowerCase());
            }
        } else {
            final Set<String> processedHeaders = new HashSet<>();
            for (final String name : sourceMessage.getHeadersKnob().getHeaderNames(HEADER_TYPE_HTTP, false, false)) {
                if (!name.equalsIgnoreCase(HttpConstants.HEADER_COOKIE)) {
                    writeRequestHeader(name, sourceMessage.getHeadersKnob().getHeaderValues(name, HEADER_TYPE_HTTP, false), !processedHeaders.contains(name), httpRequestParams);
                    processedHeaders.add(name);
                }
            }
            cookieHeaders.addAll(sourceMessage.getHttpCookiesKnob().getCookiesAsHeaders());
        }
        setCookiesHeader(httpRequestParams, cookieHeaders, auditor);
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
    private static Collection<String> passableCookieHeaders(Collection<String> cookieHeaders, Audit auditor) {
        final List<String> passableCookies = new ArrayList<>();
        for (final String cookieHeader : cookieHeaders) {
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

    private static void writeRequestHeader(final String headerName, final String[] headerValues, final boolean replace, final GenericHttpRequestParams requestParams) throws IOException {
        if (StringUtils.isBlank(headerName)) {
            throw new IOException("Encountered header with an empty name");
        }
        if (headerName.equalsIgnoreCase(HttpConstants.HEADER_AUTHORIZATION) && requestParams.getPasswordAuthentication() != null) {
            logger.fine("not passing through authorization header because credentials are specified for back-end request"); // bug 10795
        } else {
            for (int i = 0; i < headerValues.length; i++) {
                final String value = headerValues[i];
                if (replace && i == 0) {
                    requestParams.replaceExtraHeader(new GenericHttpHeader(headerName, value));
                } else {
                    requestParams.addExtraHeader(new GenericHttpHeader(headerName, value));
                }
            }
        }
    }

    private static void setCookiesHeader(final GenericHttpRequestParams requestParams, final Collection<String> cookieHeaders, final Audit auditor) {
        final Collection<String> result = passableCookieHeaders(cookieHeaders, auditor);
        if (!result.isEmpty()) {
            final String joined = StringUtils.join(result, "; ");
            requestParams.addExtraHeader(new GenericHttpHeader(HttpConstants.HEADER_COOKIE, joined));
        }
    }
}
