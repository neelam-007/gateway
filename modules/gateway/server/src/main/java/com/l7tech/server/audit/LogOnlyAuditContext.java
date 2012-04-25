package com.l7tech.server.audit;

import com.l7tech.common.log.HybridDiagnosticContext;
import com.l7tech.gateway.common.audit.AuditDetail;
import com.l7tech.gateway.common.audit.AuditDetailEvent;
import com.l7tech.gateway.common.audit.AuditDetailMessage;
import com.l7tech.gateway.common.audit.MessagesUtil;
import com.l7tech.util.Functions;
import com.l7tech.util.Pair;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * An audit context that (immediately) invokes the audit log listener for details but takes no other action
 * and in particular does not persist any audit records.
 */
public class LogOnlyAuditContext implements AuditContext {
    private final AuditLogListener listener;

    public LogOnlyAuditContext(AuditLogListener listener) {
        this.listener = listener;
    }

    @Override
    public void addDetail(AuditDetail detail, Object source) {
        addDetail( new AuditDetailEvent.AuditDetailWithInfo( source, detail, null, null ));
    }

    @Override
    public void addDetail(final AuditDetailEvent.AuditDetailWithInfo detailWithInfo) {
        if (listener != null) {
            int mid = detailWithInfo.getDetail().getMessageId();

            final Pair<Boolean,AuditDetailMessage> pair = MessagesUtil.getAuditDetailMessageByIdWithFilter(mid);
            if (!pair.left) {
                throw new RuntimeException("Cannot find the message (id=" + mid + ")" + " in the Message Map.");
            }

            final AuditDetailMessage message = pair.right;
            if (message == null){
                //audit has been filtered to NEVER.
                return;
            }

            final AuditLogFormatter formatter = new AuditLogFormatter(Collections.<String,Object>emptyMap());

            final String source = detailWithInfo.getSource().getClass().getName();

            HybridDiagnosticContext.doWithContext(detailWithInfo.getContext(), new Functions.Nullary<Void>() {
                @Override
                public Void call() {
                    listener.notifyDetailFlushed(source,
                                                        detailWithInfo.getLoggerName(),
                                                        message,
                                                        detailWithInfo.getDetail().getParams(),
                                                        formatter,
                                                        detailWithInfo.getException());
                    return null;
                }
            });
        }
    }

    @Override
    public Set getHints() {
        return Collections.emptySet();
    }

    @Override
    public Map<Object, List<AuditDetail>> getDetails() {
        return Collections.emptyMap();
    }

    @Override
    public void setContextVariables(Map<String, Object> variables) {
        throw new IllegalStateException("The log-only audit context does not support context variables (attempt to do message summary auditing without setting audit context?)");
    }

}
