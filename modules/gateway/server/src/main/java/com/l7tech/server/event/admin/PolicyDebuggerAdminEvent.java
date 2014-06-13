package com.l7tech.server.event.admin;

import com.l7tech.objectmodel.Goid;

import java.text.MessageFormat;
import java.util.logging.Level;

/**
 * Event fired when policy debugger is started or stopped.
 */
public class PolicyDebuggerAdminEvent extends AdminEvent {
    private static final String MESSAGE = "{0} Service Debugger for policy ''{1}''.";
    private static final String STARTING = "Starting";
    private static final String STOPPING = "Stopping";

    /**
     * Creates <code>PolicyDebuggerAdminEvent</code>.
     *
     * @param source the source
     * @param isStarting true if policy debugger is starting. false if policy debugger is stopping.
     * @param policyGoid the policy GOID
     */
    public PolicyDebuggerAdminEvent(Object source, boolean isStarting, Goid policyGoid) {
        super(source, formatMessage(isStarting, policyGoid), Level.FINE);
    }

    private static String formatMessage(boolean isStarting, Goid policyGoid) {
        return MessageFormat.format(MESSAGE, isStarting ? STARTING : STOPPING, policyGoid.toString());
    }
}