
package com.l7tech.server.util;

import com.l7tech.common.http.*;
import com.l7tech.common.audit.AssertionMessages;
import com.l7tech.common.audit.Auditor;
import com.l7tech.common.message.HttpRequestKnob;
import com.l7tech.common.message.HttpResponseKnob;
import com.l7tech.policy.assertion.HttpPassthroughRule;
import com.l7tech.policy.assertion.HttpPassthroughRuleSet;
import com.l7tech.policy.variable.ExpandVariables;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.ServerBridgeRoutingAssertion;

import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.Map;
import java.util.logging.Logger;
import java.util.logging.Level;

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
    private static final String[] HEADERS_NOT_TO_IMPLICITELY_FORWARD = {"content-length", "user-agent", "host",
                                                                        "accept", "accept-language", "accept-encoding",
                                                                        "accept-charset", "keep-alive", "connection",
                                                                        "server", "content-type", "date"};

    /**
     * for forwarding request http headers downstream (from routing assertion)
     * @param routedRequestParams httpclientproperty
     * @param context the pec
     * @param targetDomain name of domain used for cookie forwarding
     * @param rules http rules dictating what headers should be forwarded and under which conditions
     * @param auditor for runtime auditing
     * @param vars pre-populated map of context variables (pec.getVariableMap) or null
     * @param varNames the context variables used by the calling assertion used to populate vars if null
     */
    public static void handleRequestHeaders(GenericHttpRequestParams routedRequestParams, PolicyEnforcementContext context,
                                            String targetDomain, HttpPassthroughRuleSet rules, Auditor auditor,
                                            Map vars, String[] varNames) {
        if (rules.isForwardAll()) {
            // forward everything
            HttpRequestKnob knob;
            try {
                knob = context.getRequest().getHttpRequestKnob();
            } catch (IllegalStateException e) {
                logger.log(Level.FINE, "no header to forward cause this is not an incoming http request");
                return;
            }
            String[] headerNames = knob.getHeaderNames();
            boolean cookieAlreadyHandled = false; // cause all cookies are processed in one go (unlike other headers)
            for (String headername : headerNames) {
                if (headerShouldBeIgnored(headername)) {
                    // some headers should just be ignored cause they are not 'application headers'
                    logger.fine("not passing through " + headername);
                } else if (headername.equals(HttpConstants.HEADER_COOKIE) && !cookieAlreadyHandled) {
                    // special cookie handling
                    List<HttpCookie> res = passableCookies(context, targetDomain, auditor);
                    if (!res.isEmpty()) {
                        routedRequestParams.addExtraHeader(new GenericHttpHeader(HttpConstants.HEADER_COOKIE,
                                                                                 HttpCookie.getCookieHeader(res)));
                    }
                    cookieAlreadyHandled = true;
                } else {
                    String[] values = knob.getHeaderValues(headername);
                    for (String value : values) {
                        routedRequestParams.addExtraHeader(new GenericHttpHeader(headername, value));
                    }
                }
            }
        } else {
            HttpRequestKnob knob;
            try {
                knob = context.getRequest().getHttpRequestKnob();
            } catch (IllegalStateException e) {
                logger.log(Level.FINE, "incoming headers wont be forwarded cause this is not an incoming http request");
                knob = null;
            }
            for (int i = 0; i < rules.getRules().length; i++) {
                HttpPassthroughRule rule = rules.getRules()[i];
                if (rule.isUsesCustomizedValue()) {
                    // set header with custom value
                    String headername = rule.getName();
                    String headervalue = rule.getCustomizeValue();
                    // resolve context variable if applicable
                    if (varNames != null && varNames.length > 0) {
                        if (vars == null) {
                            vars = context.getVariableMap(varNames, auditor);
                        }
                        headervalue = ExpandVariables.process(headervalue, vars);
                    }
                    routedRequestParams.addExtraHeader(new GenericHttpHeader(headername, headervalue));
                } else if (knob != null) {
                    // set header with incoming value if it's present
                    String headername = rule.getName();
                    // special cookie handling
                    if (headername.equals(HttpConstants.HEADER_COOKIE)) {
                        List<HttpCookie> res = passableCookies(context, targetDomain, auditor);
                        if (!res.isEmpty()) {
                            routedRequestParams.addExtraHeader(new GenericHttpHeader(HttpConstants.HEADER_COOKIE,
                                                                                     HttpCookie.getCookieHeader(res)));
                        }
                    } else {
                        String[] values = knob.getHeaderValues(headername);
                        for (String value : values) {
                            routedRequestParams.addExtraHeader(new GenericHttpHeader(headername, value));
                        }
                    }
                }
            }
        }
    }

    public static void handleRequestParameters() {
        // todo
        // see PostMethod.addparam, removeparam, etc
        // GetMethod does not have those
    }

    public static void handleResponseHeaders(HttpResponseKnob targetForResponseHeaders, Auditor auditor,
                                             ServerBridgeRoutingAssertion.HeaderHolder hh,
                                             HttpPassthroughRuleSet rules, Map vars, String[] varNames,
                                             PolicyEnforcementContext context) {
        if (rules.isForwardAll()) {
            HttpHeader[] headers = hh.getHeaders().toArray();
            for (HttpHeader h : headers) {
                if (headerShouldBeIgnored(h.getName())) {
                    logger.fine("ignoring header " + h.getName() + " with value " + h.getFullValue());
                } else if (HttpConstants.HEADER_SET_COOKIE.equals(h.getName())) {
                    // special cookie handling happens outside this class by the ServerBRA
                } else {
                    targetForResponseHeaders.setHeader(h.getName(), h.getFullValue());
                }
            }
        } else {
            for (int i = 0; i < rules.getRules().length; i++) {
                HttpPassthroughRule rule = rules.getRules()[i];
                if (rule.isUsesCustomizedValue()) {
                    String headervalue = rule.getCustomizeValue();
                    // resolve context variable if applicable
                    if (varNames != null && varNames.length > 0) {
                        if (vars == null) {
                            vars = context.getVariableMap(varNames, auditor);
                        }
                        headervalue = ExpandVariables.process(headervalue, vars);
                    }
                    targetForResponseHeaders.setHeader(rule.getName(), headervalue);
                } else {
                    if (HttpConstants.HEADER_SET_COOKIE.equals(rule.getName())) {
                        // special cookie handling happens outside this class by the ServerBRA
                    } else {
                        List vals = hh.getHeaders().getValues(rule.getName());
                        if (vals != null && vals.size() > 0) {
                            for (Object valo : vals) {
                                String val = (String) valo;
                                targetForResponseHeaders.setHeader(rule.getName(), val);
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
     * @param targetForResponseHeaders the response message to put the headers into
     * @param context the pec
     * @param rules http rules dictating what headers should be forwarded and under which conditions
     * @param auditor for runtime auditing
     * @param routedRequestParams httpclientproperty
     * @param vars pre-populated map of context variables (pec.getVariableMap) or null
     * @param varNames the context variables used by the calling assertion used to populate vars if null
     */
    public static void handleResponseHeaders(GenericHttpResponse sourceOfResponseHeaders,
                                             HttpResponseKnob targetForResponseHeaders, Auditor auditor,
                                             HttpPassthroughRuleSet rules, PolicyEnforcementContext context,
                                             GenericHttpRequestParams routedRequestParams, Map vars,
                                             String[] varNames) {
        boolean passIncomingCookies = false;
        if (rules.isForwardAll()) {
            HttpHeader[] responseHeaders = sourceOfResponseHeaders.getHeaders().toArray();
            for (HttpHeader h : responseHeaders) {
                if (headerShouldBeIgnored(h.getName())) {
                    logger.fine("ignoring header " + h.getName() + " with value " + h.getFullValue());
                } else if (HttpConstants.HEADER_SET_COOKIE.equals(h.getName())) {
                    // special cookie handling happens outside this loop (see below)
                } else {
                    targetForResponseHeaders.setHeader(h.getName(), h.getFullValue());
                }
            }
            passIncomingCookies = true;
        } else {
            for (int i = 0; i < rules.getRules().length; i++) {
                HttpPassthroughRule rule = rules.getRules()[i];
                if (rule.isUsesCustomizedValue()) {
                    String headervalue = rule.getCustomizeValue();
                    // resolve context variable if applicable
                    if (varNames != null && varNames.length > 0) {
                        if (vars == null) {
                            vars = context.getVariableMap(varNames, auditor);
                        }
                        headervalue = ExpandVariables.process(headervalue, vars);
                    }
                    targetForResponseHeaders.setHeader(rule.getName(), headervalue);
                } else {
                    if (HttpConstants.HEADER_SET_COOKIE.equals(rule.getName())) {
                        // special cookie handling outside this loop (see below)
                        passIncomingCookies = true;
                    } else {
                        List vals = sourceOfResponseHeaders.getHeaders().getValues(rule.getName());
                        if (vals != null && vals.size() > 0) {
                            for (Object valo : vals) {
                                String val = (String) valo;
                                targetForResponseHeaders.setHeader(rule.getName(), val);
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
            List setCookieValues = sourceOfResponseHeaders.getHeaders().getValues(HttpConstants.HEADER_SET_COOKIE);
            List<HttpCookie> newCookies = new ArrayList<HttpCookie>();
            for (Object setCookieValue1 : setCookieValues) {
                String setCookieValue = (String) setCookieValue1;
                try {
                    newCookies.add(new HttpCookie(routedRequestParams.getTargetUrl(), setCookieValue));
                } catch (HttpCookie.IllegalFormatException hcife) {
                    auditor.logAndAudit(AssertionMessages.HTTPROUTE_INVALIDCOOKIE, new String[]{setCookieValue});
                }
            }
            for (HttpCookie routedCookie : newCookies) {
                HttpCookie ssgResponseCookie = new HttpCookie(routedCookie.getCookieName(),
                                                              routedCookie.getCookieValue(),
                                                              routedCookie.getVersion(), null, null);
                context.addCookie(ssgResponseCookie);
            }
        }

    }

    private static boolean headerShouldBeIgnored(String headerName) {
        headerName = headerName.toLowerCase();
        for (String ignoreme : HEADERS_NOT_TO_IMPLICITELY_FORWARD) {
            if (ignoreme.equals(headerName)) return true;
        }
        return false;
    }

    private static List<HttpCookie> passableCookies(PolicyEnforcementContext context, String targetDomain, Auditor auditor) {
        List<HttpCookie> output = new ArrayList<HttpCookie>();
        Set<HttpCookie> contextCookies = context.getCookies();

        for (HttpCookie ssgc : contextCookies) {
            if (CookieUtils.isPassThroughCookie(ssgc)) {
                if (ssgc.isNew()) {
                    auditor.logAndAudit(AssertionMessages.HTTPROUTE_ADDCOOKIE_VERSION,
                                        new String[]{ssgc.getCookieName(), String.valueOf(ssgc.getVersion())});
                } else {
                    auditor.logAndAudit(AssertionMessages.HTTPROUTE_UPDATECOOKIE, new String[]{ssgc.getCookieName()});
                }
                HttpCookie newCookie = new HttpCookie(
                        ssgc.getCookieName(),
                        ssgc.getCookieValue(),
                        ssgc.getVersion(),
                        "/",
                        targetDomain,
                        ssgc.getMaxAge(),
                        ssgc.isSecure(),
                        ssgc.getComment()
                );
                // attach and record
                output.add(newCookie);
            }
        }
        return output;
    }
}
