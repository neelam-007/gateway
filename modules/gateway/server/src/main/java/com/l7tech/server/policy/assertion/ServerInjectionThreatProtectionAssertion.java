package com.l7tech.server.policy.assertion;

import com.l7tech.message.HttpServletRequestKnob;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.InjectionThreatProtectionAssertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.message.PolicyEnforcementContext;

import java.io.IOException;
import java.util.Map;

/**
 * @author Jamie Williams - jamie.williams2@ca.com
 */
public abstract class ServerInjectionThreatProtectionAssertion<AT extends InjectionThreatProtectionAssertion>
        extends AbstractMessageTargetableServerAssertion<AT> {
    public ServerInjectionThreatProtectionAssertion(AT assertion) {
        super(assertion);
    }

    protected AssertionStatus applyThreatProtection(PolicyEnforcementContext context,
                                                    Message msg, String targetName) throws IOException {
        boolean routed = context.isPostRouting();
        HttpServletRequestKnob httpServletRequestKnob = null;

        if (isRequest()) {
            if (routed) {
                logAndAuditRequestAlreadyRouted();
                return AssertionStatus.FAILED;
            }

            httpServletRequestKnob = msg.getKnob(HttpServletRequestKnob.class);
        } else if (isResponse() && !routed) {
            logAndAuditResponseNotRouted();
            return AssertionStatus.NONE;
        }

        AssertionStatus result = AssertionStatus.NONE;

        if (null != httpServletRequestKnob) { // if the message is HTTP and thereby has a request URL
            if (assertion.isIncludeUrlPath()) {
                result = scanHttpRequestUrlPath(httpServletRequestKnob);

                if (result != AssertionStatus.NONE) {
                    logAndAuditAttackRejected();
                    return result;
                }
            }

            if (assertion.isIncludeUrlQueryString()) {
                result = scanHttpRequestUrlQueryString(httpServletRequestKnob);

                if (result != AssertionStatus.NONE) {
                    logAndAuditAttackRejected();
                    return result;
                }
            }
        } else if (assertion.isIncludeUrlPath() || assertion.isIncludeUrlQueryString()) {
            logAndAuditMessageNotHttp();
        }

        if (assertion.isIncludeBody()) {
            result = scanBody(msg, targetName);
        }

        if (result != AssertionStatus.NONE) {
            logAndAuditAttackRejected();
        }

        return result;
    }

    protected AssertionStatus scanHttpRequestUrlPath(final HttpServletRequestKnob httpServletRequestKnob) throws IOException {
        logAndAuditScanningUrlPath();

        final StringBuilder evidence = new StringBuilder();
        final String urlPath = httpServletRequestKnob.getRequestUri();

        final String protectionViolated = scan(urlPath, evidence);

        if (protectionViolated != null) {
            logAndAuditAttackDetectedInUrlPath(evidence, urlPath, protectionViolated);
            return getBadMessageStatus();
        }

        return AssertionStatus.NONE;
    }

    protected AssertionStatus scanHttpRequestUrlQueryString(final HttpServletRequestKnob httpServletRequestKnob)
            throws IOException {
        logAndAuditScanningUrlQueryString();

        final StringBuilder evidence = new StringBuilder();
        final Map<String, String[]> urlParams = httpServletRequestKnob.getQueryParameterMap();

        for (Map.Entry<String, String[]> entry : urlParams.entrySet()) {
            final String urlParamName = entry.getKey();

            for (String urlParamValue : entry.getValue()) {
                final String protectionViolated = scan(urlParamValue, evidence);

                if (protectionViolated != null) {
                    logAndAuditAttackDetectedInQueryParameter(evidence, urlParamName, protectionViolated);
                    return getBadMessageStatus();
                }
            }
        }

        return AssertionStatus.NONE;
    }

    protected abstract AssertionStatus scanBody(Message message, String targetName) throws IOException;

    /**
     * Scans for code injection pattern.
     *
     * @param s             string to scan
     * @param evidence      for passing back snippet of string surrounding the
     *                      first match (if found), for logging purpose
     * @return the first protection type violated if found (<code>evidence</code> is then populated);
     *         <code>null</code> if none found
     */
    protected abstract String scan(String s, StringBuilder evidence);

    protected abstract void logAndAuditRequestAlreadyRouted();

    protected abstract void logAndAuditResponseNotRouted();

    protected abstract void logAndAuditMessageNotHttp();

    protected abstract void logAndAuditScanningUrlPath();

    protected abstract void logAndAuditAttackDetectedInUrlPath(StringBuilder evidence, String urlPath, String protectionViolated);

    protected abstract void logAndAuditScanningUrlQueryString();

    protected abstract void logAndAuditAttackDetectedInQueryParameter(StringBuilder evidence, String urlParamName, String protectionViolated);

    protected abstract void logAndAuditAttackRejected();
}
