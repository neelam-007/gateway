package com.l7tech.gateway.common;

import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.gateway.common.audit.CommonMessages;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.util.ExceptionUtils;

import java.text.MessageFormat;

/**
 * @author steve
*/
public class DefaultSyntaxErrorHandler implements Syntax.SyntaxErrorHandler {
    private final Audit audit;

    public DefaultSyntaxErrorHandler( final Audit audit ) {
        this.audit = audit;
    }

    @Override
    public String handleSuspiciousToString( final String remainingName, final String className ) {
        audit.logAndAudit(CommonMessages.TEMPLATE_SUSPICIOUS_TOSTRING, remainingName, className);
        return MessageFormat.format(CommonMessages.TEMPLATE_SUSPICIOUS_TOSTRING.getMessage(), remainingName, className);
    }

    @Override
    public String handleSubscriptOutOfRange( final int subscript, final String remainingName, final int length ) {
        audit.logAndAudit(CommonMessages.TEMPLATE_SUBSCRIPT_OUTOFRANGE, Integer.toString(subscript), remainingName, Integer.toString(length));
        return MessageFormat.format( CommonMessages.TEMPLATE_SUBSCRIPT_OUTOFRANGE.getMessage(), Integer.toString(subscript), remainingName, Integer.toString(length));
    }

    @Override
    public String handleBadVariable(String name) {
        audit.logAndAudit(CommonMessages.TEMPLATE_UNSUPPORTED_VARIABLE, name);
        return MessageFormat.format(CommonMessages.TEMPLATE_UNSUPPORTED_VARIABLE.getMessage(), name);
    }

    @Override
    public String handleBadVariable(String s, Throwable t) {
        audit.logAndAudit(CommonMessages.TEMPLATE_UNSUPPORTED_VARIABLE_WITH_EXCEPTION, new String[] { s, ExceptionUtils.getMessage(t) }, t);
        return MessageFormat.format(CommonMessages.TEMPLATE_UNSUPPORTED_VARIABLE.getMessage(), s, ExceptionUtils.getMessage(t));
    }
}
