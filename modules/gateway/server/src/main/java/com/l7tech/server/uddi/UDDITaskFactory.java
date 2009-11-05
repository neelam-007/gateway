package com.l7tech.server.uddi;

import com.l7tech.uddi.UDDIException;

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
    }

    public static abstract class UDDITask {

        /**
         * Run the task.
         *
         * @param context The context for the task
         * @throws UDDIException If an error occurs.
         */
        public abstract void apply( UDDITaskContext context ) throws UDDIException;
    }
}
