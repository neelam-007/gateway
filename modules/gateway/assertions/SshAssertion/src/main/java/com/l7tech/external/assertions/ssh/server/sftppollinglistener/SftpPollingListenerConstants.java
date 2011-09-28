package com.l7tech.external.assertions.ssh.server.sftppollinglistener;

/**
 * SFTP Polling Listener constants
 */
public interface SftpPollingListenerConstants {
    // Context variables used
    public static final String SFTP_POLLING_MESSAGE_MAX_BYTES_PROPERTY = "l7.ssh.sftpPolling.messageMaxBytes";
    public static final String SFTP_POLLING_MESSAGE_MAX_BYTES_UI_PROPERTY = "sftpPolling.messageMaxBytes";
    public static final String SFTP_POLLING_MESSAGE_MAX_BYTES_DESC = "Maximum number of bytes permitted for an SFTP message, or 0 for unlimited (Integer)";

    public static final String LISTENER_THREAD_LIMIT_PROPERTY = "l7.ssh.sftpPolling.listenerThreadLimit";
    public static final String LISTENER_THREAD_LIMIT_UI_PROPERTY = "sftpPolling.listenerThreadLimit";
    public static final String LISTENER_THREAD_LIMIT_DESC = "The global limit on the number of processing threads that can be created to work off all SFTP polling listeners. Value must be >= 5.";

    public static final String SFTP_POLLING_CONNECT_ERROR_SLEEP_PROPERTY = "l7.ssh.sftpPolling.connectErrorSleep";
    public static final String SFTP_POLLING_CONNECT_ERROR_SLEEP_UI_PROPERTY = "sftpPolling.connectErrorSleep";
    public static final String SFTP_POLLING_CONNECT_ERROR_SLEEP_DESC = "Time to sleep after a connection error for an SFTP polling listener (timeunit)";

    public static final String SFTP_POLLING_CONFIGURATION_PROPERTY = "l7.ssh.sftpPolling.configuration";
    public static final String SFTP_POLLING_CONFIGURATION_UI_PROPERTY = "sftpPolling.configuration";
    public static final String SFTP_POLLING_CONFIGURATION_DESC = "Property that persists SFTP polling configuration";

    public static final String SFTP_POLLING_DOWNLOAD_THREAD_WAIT_SECONDS_PROPERTY = "l7.ssh.sftpPolling.downloadThreadWait";
    public static final String SFTP_POLLING_DOWNLOAD_THREAD_WAIT_SECONDS_UI_PROPERTY = "sftpPolling.downloadThreadWait";
    public static final String SFTP_POLLING_DOWNLOAD_THREAD_WAIT_SECONDS_DESC = "Maximum wait time limit for file download thread to run (in seconds).";

    /**
     * This is a complete list of cluster-wide properties used by the SFTP polling listener module.
     */
    public static final String[][] MODULE_CLUSTER_PROPERTIES = new String[][] {
            new String[] { SFTP_POLLING_MESSAGE_MAX_BYTES_PROPERTY, SFTP_POLLING_MESSAGE_MAX_BYTES_UI_PROPERTY, SFTP_POLLING_MESSAGE_MAX_BYTES_DESC, "5242880" },
            new String[] { LISTENER_THREAD_LIMIT_PROPERTY, LISTENER_THREAD_LIMIT_UI_PROPERTY, LISTENER_THREAD_LIMIT_DESC, "25" },
            new String[] { SFTP_POLLING_CONNECT_ERROR_SLEEP_PROPERTY, SFTP_POLLING_CONNECT_ERROR_SLEEP_UI_PROPERTY, SFTP_POLLING_CONNECT_ERROR_SLEEP_DESC, "10s",},
            new String[] { SFTP_POLLING_CONFIGURATION_PROPERTY, SFTP_POLLING_CONFIGURATION_UI_PROPERTY, SFTP_POLLING_CONFIGURATION_DESC, ""},
            new String[] { SFTP_POLLING_DOWNLOAD_THREAD_WAIT_SECONDS_PROPERTY, SFTP_POLLING_DOWNLOAD_THREAD_WAIT_SECONDS_UI_PROPERTY, SFTP_POLLING_DOWNLOAD_THREAD_WAIT_SECONDS_DESC, "3"},
    };
}
