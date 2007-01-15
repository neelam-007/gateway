package com.l7tech.server.util;

import com.l7tech.common.http.*;
import com.l7tech.common.audit.AssertionMessages;
import com.l7tech.common.audit.Auditor;
import com.l7tech.policy.assertion.HttpPassthroughRule;
import com.l7tech.policy.assertion.HttpPassthroughRuleSet;
import com.l7tech.server.message.PolicyEnforcementContext;

import java.util.List;
import java.util.ArrayList;
import java.util.Set;
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

    /**
     * for forwarding request http headers downstream (from routing assertion)
     * @param routedRequestParams httpclientproperty
     * @param context the pec
     * @param targetDomain name of domain used for cookie forwarding
     * @param rules http rules dictating what headers should be forwarded and under which conditions
     * @param auditor for runtime auditing
     */
    public static void handleRequestHeaders(GenericHttpRequestParams routedRequestParams, PolicyEnforcementContext context,
                                            String targetDomain, HttpPassthroughRuleSet rules, Auditor auditor) {
        if (rules.isForwardAll()) {
            // forward everything
            // todo (see above)
        } else {
            for (int i = 0; i < rules.getRules().length; i++) {
                HttpPassthroughRule rule = rules.getRules()[i];
                if (rule.isUsesCustomizedValue()) {
                    // set header with custom value
                    String headername = rule.getName();
                    String headervalue = rule.getCustomizeValue();
                    // resolve context variable if applicable
                    // todo (see above)
                    routedRequestParams.addExtraHeader(new GenericHttpHeader(headername, headervalue));
                } else {
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
                        try {
                            String[] values = context.getRequest().getHttpRequestKnob().getHeaderValues(headername);
                            for (String value : values) {
                                routedRequestParams.addExtraHeader(new GenericHttpHeader(headername, value));
                            }
                        } catch (IllegalStateException e) {
                            logger.log(Level.FINE, "cannot get http header " + headername, e);
                        }
                    }
                }
            }
        }
    }

    public static void handleRequestParameters() {
        // todo
    }

    public static void handleResponseHeaders() {
        // todo
    }

    public static void handleResponseParameters() {
        // todo
    }

    private static List<HttpCookie> passableCookies(PolicyEnforcementContext context, String targetDomain, Auditor auditor) {
        List<HttpCookie> output = new ArrayList<HttpCookie>();
        Set<HttpCookie> contextCookies = context.getCookies();

        for (HttpCookie ssgc : contextCookies) {
            if (CookieUtils.isPassThroughCookie(ssgc)) {
                if (ssgc.isNew()) {
                    auditor.logAndAudit(AssertionMessages.HTTPROUTE_ADDCOOKIE_VERSION, new String[]{ssgc.getCookieName(), String.valueOf(ssgc.getVersion())});
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
