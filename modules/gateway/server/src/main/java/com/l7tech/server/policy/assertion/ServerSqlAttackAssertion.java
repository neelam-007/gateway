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
import java.util.regex.Pattern;

/**
 * Server side implementation of the SqlAttackAssertion all-in-one convenience assertion.
 * Internally this is implemented, essentially, as just zero or more regexp assertions.
 */
public class ServerSqlAttackAssertion extends ServerInjectionThreatProtectionAssertion<SqlAttackAssertion> {
    public ServerSqlAttackAssertion(SqlAttackAssertion assertion) {
        super(assertion);
    }

    @Override
    protected AssertionStatus doCheckRequest(final PolicyEnforcementContext context,
                                             final Message msg,
                                             final String targetName,
                                             final AuthenticationContext authContext)
            throws IOException, PolicyAssertionException {
        return applyThreatProtection(context, msg, targetName);
    }

    @Override
    protected AssertionStatus scanBody(final Message message, final String targetName) throws IOException {
        logAndAudit(AssertionMessages.SQLATTACK_SCANNING_BODY_TEXT, targetName);

        final ContentTypeHeader contentType = message.getMimeKnob().getOuterContentType();
        final String where = targetName + " message body";
        final MimeKnob mimeKnob = message.getMimeKnob();

        try {
            final byte[] bodyBytes = IOUtils.slurpStream(mimeKnob.getEntireMessageBodyAsInputStream());
            final String bodyString = new String(bodyBytes, contentType.getEncoding());
            final StringBuilder evidence = new StringBuilder();
            final String protectionViolated = scan(bodyString, evidence);

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

    @Override
    protected String scan(final String s, final StringBuilder evidence) {
        String protectionViolated = null;
        int minIndex = -1;

        for (String protection : assertion.getProtections()) {
            Pattern protectionPattern = SqlAttackAssertion.getProtectionPattern(protection);

            if (null == protectionPattern) {
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

    @Override
    protected void logAndAuditRequestAlreadyRouted() {
        logAndAudit(AssertionMessages.SQLATTACK_ALREADY_ROUTED);
    }

    @Override
    protected void logAndAuditResponseNotRouted() {
        logAndAudit(AssertionMessages.SQLATTACK_SKIP_RESPONSE_NOT_ROUTED);
    }

    @Override
    protected void logAndAuditMessageNotHttp() {
        logAndAudit(AssertionMessages.SQLATTACK_NOT_HTTP);
    }

    @Override
    protected void logAndAuditScanningUrlPath() {
        logAndAudit(AssertionMessages.SQLATTACK_SCANNING_URL_PATH);
    }

    @Override
    protected void logAndAuditAttackDetectedInUrlPath(StringBuilder evidence, String urlPath, String protectionViolated) {
        logAndAudit(AssertionMessages.SQLATTACK_DETECTED_PATH,
                "Request URL", urlPath, evidence.toString(), protectionViolated);
    }

    @Override
    protected void logAndAuditScanningUrlQueryString() {
        logAndAudit(AssertionMessages.SQLATTACK_SCANNING_URL_QUERY_STRING);
    }

    @Override
    protected void logAndAuditAttackDetectedInQueryParameter(StringBuilder evidence, String urlParamName, String protectionViolated) {
        logAndAudit(AssertionMessages.SQLATTACK_DETECTED_PARAM,
                "Request URL", urlParamName, evidence.toString(), protectionViolated);
    }

    @Override
    protected void logAndAuditAttackRejected() {
        logAndAudit(AssertionMessages.SQLATTACK_REJECTED, assertion.getTargetName());
    }
}
