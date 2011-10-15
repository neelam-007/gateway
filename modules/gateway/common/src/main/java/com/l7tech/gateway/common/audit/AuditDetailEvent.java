package com.l7tech.gateway.common.audit;

import com.l7tech.common.log.HybridDiagnosticContext;
import com.l7tech.common.log.HybridDiagnosticContext.SavedDiagnosticContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.context.ApplicationEvent;

/**
 * An {@link ApplicationEvent} that records the creation of an {@link AuditDetail}
 * TODO this should be moved to its own separate event channel rather than the default ApplicationEvent channel.
 */
public class AuditDetailEvent extends ApplicationEvent {
    private final AuditDetailWithInfo info;

    public AuditDetailEvent( final Object source,
                             final AuditDetail detail,
                             final Throwable exception,
                             final String loggerName ) {
        super(source);
        this.info = new AuditDetailWithInfo( source, detail, exception, loggerName );
    }

    @NotNull
    public AuditDetailWithInfo getDetailWithInfo() {
        return info;
    }

    public final static class AuditDetailWithInfo {
        private final Object source;
        private final AuditDetail detail;
        private final Throwable exception;
        private final String loggerName;
        private final SavedDiagnosticContext context = HybridDiagnosticContext.save();

        public AuditDetailWithInfo( @NotNull  final Object source,
                                    @NotNull  final AuditDetail detail,
                                    @Nullable final Throwable exception,
                                    @Nullable final String loggerName ) {
            this.source = source;
            this.detail = detail;
            this.exception = exception;
            this.loggerName = loggerName;
        }

        @NotNull
        public AuditDetail getDetail() {
            return detail;
        }

        @Nullable
        public Throwable getException() {
            return exception;
        }

        @Nullable
        public String getLoggerName() {
            return loggerName;
        }

        @NotNull
        public Object getSource() {
            return source;
        }

        @NotNull
        public SavedDiagnosticContext getContext() {
            return context;
        }
    }
}
