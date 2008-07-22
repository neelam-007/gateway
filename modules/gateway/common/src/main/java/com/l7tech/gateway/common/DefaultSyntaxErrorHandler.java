package com.l7tech.gateway.common;

import com.l7tech.policy.variable.Syntax;
import com.l7tech.gateway.common.audit.CommonMessages;
import com.l7tech.gateway.common.audit.Audit;

import java.text.MessageFormat;

/**
 * @author steve
*/
public class DefaultSyntaxErrorHandler implements Syntax.SyntaxErrorHandler {
    private final Audit audit;

    public DefaultSyntaxErrorHandler( final Audit audit ) {
        this.audit = audit;
    }

    public String handleSuspiciousToString( final String remainingName, final String className ) {
        audit.logAndAudit(CommonMessages.TEMPLATE_SUSPICIOUS_TOSTRING, remainingName, className);
        return MessageFormat.format(CommonMessages.TEMPLATE_SUSPICIOUS_TOSTRING.getMessage(), remainingName, className);
    }

    public String handleSubscriptOutOfRange( final int subscript, final String remainingName, final int length ) {
        audit.logAndAudit(CommonMessages.TEMPLATE_SUBSCRIPT_OUTOFRANGE, Integer.toString(subscript), remainingName, Integer.toString(length));
        return MessageFormat.format( CommonMessages.TEMPLATE_SUBSCRIPT_OUTOFRANGE.getMessage(), Integer.toString(subscript), remainingName, Integer.toString(length));
    }
}
