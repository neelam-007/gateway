package com.l7tech.external.assertions.ssh.server.sftppollinglistener;

/**
 * SFTP Polling Listener constants
 */
public interface SftpPollingListenerConstants {
    // Context variables used
    public static final String SFTP_POLLING_MESSAGE_MAX_BYTES_PROPERTY = "ioSftpPollingMessageMaxBytes";
    public static final String SFTP_POLLING_MESSAGE_MAX_BYTES_UI_PROPERTY = "io.sftpPollingMessageMaxBytes";
    public static final String SFTP_POLLING_MESSAGE_MAX_BYTES_DESC = "Maximum number of bytes permitted for an SFTP message, or 0 for unlimited (Integer)";

    public static final String LISTENER_THREAD_LIMIT_PROPERTY = "sftpPollingListenerThreadLimit";
    public static final String LISTENER_THREAD_LIMIT_UI_PROPERTY = "sftpPolling.listenerThreadLimit";
    public static final String LISTENER_THREAD_LIMIT_DESC = "The global limit on the number of processing threads that can be created to work off all SFTP polling listeners. Value must be >= 5.";

    public static final String SFTP_POLLING_CONNECT_ERROR_SLEEP_PROPERTY = "sftpPollingConnectErrorSleep";
    public static final String SFTP_POLLING_CONNECT_ERROR_SLEEP_UI_PROPERTY = "sftpPolling.connectErrorSleep";
    public static final String SFTP_POLLING_CONNECT_ERROR_SLEEP_DESC = "Time to sleep after a connection error for an SFTP polling listener (timeunit)";

    public static final String SFTP_POLLING_CONFIGURATION_PROPERTY = "sftpPollingConfig";
    public static final String SFTP_POLLING_CONFIGURATION_UI_PROPERTY = "sftpPolling.configuration";
    public static final String SFTP_POLLING_CONFIGURATION_DESC = "property that persists SFTP polling configuration";

    /**
     * This is a complete list of cluster-wide properties used by the SFTP polling listener module.
     */
    public static final String[][] MODULE_CLUSTER_PROPERTIES = new String[][] {
            new String[] { SFTP_POLLING_MESSAGE_MAX_BYTES_PROPERTY, SFTP_POLLING_MESSAGE_MAX_BYTES_UI_PROPERTY, SFTP_POLLING_MESSAGE_MAX_BYTES_DESC, "2621440" },
            new String[] { LISTENER_THREAD_LIMIT_PROPERTY, LISTENER_THREAD_LIMIT_UI_PROPERTY, LISTENER_THREAD_LIMIT_DESC, "25" },
            new String[] { SFTP_POLLING_CONNECT_ERROR_SLEEP_PROPERTY, SFTP_POLLING_CONNECT_ERROR_SLEEP_UI_PROPERTY, SFTP_POLLING_CONNECT_ERROR_SLEEP_DESC, "10s",},
            new String[] { SFTP_POLLING_CONFIGURATION_PROPERTY, SFTP_POLLING_CONFIGURATION_UI_PROPERTY, SFTP_POLLING_CONFIGURATION_DESC, ""},
    };
}
