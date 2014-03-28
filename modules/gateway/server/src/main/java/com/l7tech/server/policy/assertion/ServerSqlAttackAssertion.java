package com.l7tech.server.policy.assertion;

import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.message.*;
import com.l7tech.policy.assertion.*;
import com.l7tech.server.message.AuthenticationContext;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.IOUtils;
import com.l7tech.util.TextUtils;

import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Server side implementation of the SqlAttackAssertion all-in-one convenience assertion.
 * Internally this is implemented, essentially, as just zero or more regexp assertions.
 */
public class ServerSqlAttackAssertion extends AbstractMessageTargetableServerAssertion<SqlAttackAssertion> {
    public ServerSqlAttackAssertion(SqlAttackAssertion assertion) {
        super(assertion);
    }

    @Override
    protected AssertionStatus doCheckRequest( final PolicyEnforcementContext context,
                                              final Message msg,
                                              final String targetName,
                                              final AuthenticationContext authContext )
            throws IOException, PolicyAssertionException {
        boolean routed = context.isPostRouting();
        HttpServletRequestKnob httpServletRequestKnob = null;

        if (isRequest()) {
            if (routed) {
                logAndAudit(AssertionMessages.SQLATTACK_ALREADY_ROUTED);
                return AssertionStatus.FAILED;
            }

            httpServletRequestKnob = msg.getKnob(HttpServletRequestKnob.class);
        } else if (isResponse() && !routed) {
            logAndAudit(AssertionMessages.SQLATTACK_SKIP_RESPONSE_NOT_ROUTED);
            return AssertionStatus.NONE;
        }

        AssertionStatus result = AssertionStatus.NONE;

        if (null != httpServletRequestKnob) { // if the message is HTTP and thereby has a request URL
            if (assertion.isIncludeUrlPath()) {
                result = scanHttpRequestUrlPath(httpServletRequestKnob);

                if (result != AssertionStatus.NONE) {
                    logAndAudit(AssertionMessages.SQLATTACK_REJECTED, assertion.getTargetName());
                    return result;
                }
            }

            if (assertion.isIncludeUrlQueryString()) {
                result = scanHttpRequestUrlQueryString(httpServletRequestKnob);

                if (result != AssertionStatus.NONE) {
                    logAndAudit(AssertionMessages.SQLATTACK_REJECTED, assertion.getTargetName());
                    return result;
                }
            }
        } else if (assertion.isIncludeUrlPath() || assertion.isIncludeUrlQueryString()) {
            logAndAudit(AssertionMessages.SQLATTACK_NOT_HTTP);
        }

        if (assertion.isIncludeBody()) {
            result = scanBody(msg, targetName);
        }

        if (result != AssertionStatus.NONE) {
            logAndAudit(AssertionMessages.SQLATTACK_REJECTED, assertion.getTargetName());
        }

        return result;
    }

    private AssertionStatus scanHttpRequestUrlPath(final HttpServletRequestKnob httpServletRequestKnob) throws IOException {
        logAndAudit(AssertionMessages.SQLATTACK_SCANNING_URL_PATH);

        final StringBuilder evidence = new StringBuilder();
        final String urlPath = httpServletRequestKnob.getRequestUri();

        final String protectionViolated = scan(urlPath, assertion.getProtections(), evidence);

        if (protectionViolated != null) {
            logAndAudit(AssertionMessages.SQLATTACK_DETECTED_PATH,
                    "Request URL", urlPath, evidence.toString(), protectionViolated);
            return getBadMessageStatus();
        }

        return AssertionStatus.NONE;
    }

    private AssertionStatus scanHttpRequestUrlQueryString(final HttpServletRequestKnob httpServletRequestKnob)
            throws IOException {
        logAndAudit(AssertionMessages.SQLATTACK_SCANNING_URL_QUERY_STRING);

        final StringBuilder evidence = new StringBuilder();
        final Map<String, String[]> urlParams = httpServletRequestKnob.getQueryParameterMap();

        for (Map.Entry<String, String[]> entry : urlParams.entrySet()) {
            final String urlParamName = entry.getKey();

            for (String urlParamValue : entry.getValue()) {
                final String protectionViolated = scan(urlParamValue, assertion.getProtections(), evidence);

                if (protectionViolated != null) {
                    logAndAudit(AssertionMessages.SQLATTACK_DETECTED_PARAM,
                            "Request URL", urlParamName, evidence.toString(), protectionViolated);
                    return getBadMessageStatus();
                }
            }
        }

        return AssertionStatus.NONE;
    }

    private AssertionStatus scanBody(final Message message, final String targetName) throws IOException {
        logAndAudit(AssertionMessages.SQLATTACK_SCANNING_BODY_TEXT, targetName);

        final ContentTypeHeader contentType = message.getMimeKnob().getOuterContentType();
        final String where = targetName + " message body";
        final MimeKnob mimeKnob = message.getMimeKnob();

        try {
            final byte[] bodyBytes = IOUtils.slurpStream(mimeKnob.getEntireMessageBodyAsInputStream());
            final String bodyString = new String(bodyBytes, contentType.getEncoding());
            final StringBuilder evidence = new StringBuilder();
            final String protectionViolated = scan(bodyString, assertion.getProtections(), evidence);

            if (protectionViolated != null) {
                logAndAudit(AssertionMessages.SQLATTACK_DETECTED, where, evidence.toString(), protectionViolated);
                return getBadMessageStatus();
            }
        } catch (NoSuchPartException e) {
            logAndAudit(AssertionMessages.SQLATTACK_CANNOT_PARSE,
                    new String[] {where, "text"}, ExceptionUtils.getDebugException(e));
            return getBadMessageStatus();
        }

        return AssertionStatus.NONE;
    }

    /**
     * Scans for code injection pattern.
     *
     * @param s             string to scan
     * @param protections   protection types to apply
     * @param evidence      for passing back snippet of string surrounding the
     *                      first match (if found), for logging purpose
     * @return the first protection type violated if found (<code>evidence</code> is then populated);
     *         <code>null</code> if none found
     */
    private String scan(final String s, final Set<String> protections, final StringBuilder evidence) {
        String protectionViolated = null;
        int minIndex = -1;

        for (String protection : protections) {
            Pattern protectionPattern = SqlAttackAssertion.getProtectionPattern(protection);

            if(null == protectionPattern) {
                logAndAudit(AssertionMessages.SQLATTACK_UNRECOGNIZED_PROTECTION, protection);
                throw new AssertionStatusException(AssertionStatus.FAILED, "Unrecognized protection pattern.");
            }

            final StringBuilder tmpEvidence = new StringBuilder();
            final int index = TextUtils.scanAndRecordMatch(s, protectionPattern, tmpEvidence);
            if (index != -1 && (minIndex == -1 || index < minIndex)) {
                minIndex = index;
                evidence.setLength(0);
                evidence.append(tmpEvidence);
                protectionViolated = protection;
            }
        }

        return protectionViolated;
    }
}
