package com.l7tech.server.uddi;

import com.l7tech.uddi.UDDIException;
import com.l7tech.gateway.common.audit.AuditDetailMessage;

/**
 * Factory for UDDI Tasks.
 */
public abstract class UDDITaskFactory {

    /**
     * Build a task for the given event if it is supported.
     *
     * @param event The event to build a task for
     * @return The task or null
     */
    public abstract UDDITask buildUDDITask( UDDIEvent event );

    public interface UDDITaskContext {
        String getSubscriptionNotificationURL( long registryOid );
        String getSubscriptionBindingKey( long registryOid );
        void notifyEvent( UDDIEvent event );
        void logAndAudit( AuditDetailMessage msg, Throwable e, String... params );
        void logAndAudit( AuditDetailMessage msg, String... params );
        void logAndAudit( AuditDetailMessage msg );
        int getMaxRetryAttempts();
    }

    public static abstract class UDDITask {

        /**
         * Run the task.
         *
         * @param context The context for the task
         */
        public abstract void apply( UDDITaskContext context ) throws UDDITaskException;
    }

    public static class UDDITaskException extends Exception{
        public UDDITaskException(String message) {
            super(message);
        }

        public UDDITaskException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
