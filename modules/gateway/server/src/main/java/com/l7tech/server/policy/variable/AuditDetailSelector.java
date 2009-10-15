package com.l7tech.server.policy.variable;

import com.l7tech.gateway.common.audit.AuditDetail;
import com.l7tech.policy.variable.Syntax;

/**
 *
 */
public class AuditDetailSelector implements ExpandVariables.Selector<AuditDetail> {
    @Override
    public Selection select(AuditDetail detail, String name, Syntax.SyntaxErrorHandler handler, boolean strict) {
        if (detail == null)
            return null;
        if ("componentId".equalsIgnoreCase(name)) {
            return new Selection(detail.getComponentId());
        } else if ("messageId".equalsIgnoreCase(name)) {
            return new Selection(String.valueOf(detail.getMessageId()));
        } else if ("exception".equalsIgnoreCase(name)) {
            return new Selection(detail.getException());
        } else if ("ordinal".equalsIgnoreCase(name)) {
            return new Selection(detail.getOrdinal());
        } else if ("params".equalsIgnoreCase(name)) {
            return new Selection(detail.getParams());
        }
        return null;
    }

    @Override
    public Class<AuditDetail> getContextObjectClass() {
        return AuditDetail.class;
    }
}
