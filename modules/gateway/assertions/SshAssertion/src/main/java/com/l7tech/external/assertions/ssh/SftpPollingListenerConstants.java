package com.l7tech.external.assertions.ssh;

/**
 * SFTP Polling Listener constants
 */
public interface SftpPollingListenerConstants {
    String SFTP_POLLING_DEFAULT_PORT = "22";

    // Context variables used
    String SFTP_POLLING_MESSAGE_MAX_BYTES_PROPERTY = "l7.ssh.sftpPolling.messageMaxBytes";
    String SFTP_POLLING_MESSAGE_MAX_BYTES_UI_PROPERTY = "sftpPolling.messageMaxBytes";
    String SFTP_POLLING_MESSAGE_MAX_BYTES_DESC = "Maximum number of bytes permitted for an SFTP message, or 0 for unlimited (Integer)";

    String LISTENER_THREAD_LIMIT_PROPERTY = "l7.ssh.sftpPolling.listenerThreadLimit";
    String LISTENER_THREAD_LIMIT_UI_PROPERTY = "sftpPolling.listenerThreadLimit";
    String LISTENER_THREAD_LIMIT_DESC = "The global limit on the number of processing threads that can be created to work off all SFTP polling listeners. Value must be >= 5.";

    String SFTP_POLLING_CONNECT_ERROR_SLEEP_PROPERTY = "l7.ssh.sftpPolling.connectErrorSleep";
    String SFTP_POLLING_CONNECT_ERROR_SLEEP_UI_PROPERTY = "sftpPolling.connectErrorSleep";
    String SFTP_POLLING_CONNECT_ERROR_SLEEP_DESC = "Time to sleep after a connection error for an SFTP polling listener (timeunit)";

    String SFTP_POLLING_DOWNLOAD_THREAD_WAIT_SECONDS_PROPERTY = "l7.ssh.sftpPolling.downloadThreadWait";
    String SFTP_POLLING_DOWNLOAD_THREAD_WAIT_SECONDS_UI_PROPERTY = "sftpPolling.downloadThreadWait";
    String SFTP_POLLING_DOWNLOAD_THREAD_WAIT_SECONDS_DESC = "Maximum wait time limit for file download thread to run (in seconds).";

    String SFTP_POLLING_IGNORED_FILE_EXTENSION_LIST_PROPERTY = "l7.ssh.sftpPolling.ignoredFileExtensionList";
    String SFTP_POLLING_IGNORED_FILE_EXTENSION_LIST_UI_PROPERTY = "sftpPolling.ignoredFileExtensionList";
    String SFTP_IGNORED_FILE_EXTENSION_LIST_DESC = "List of file extension(s) to ignore (comma delimited).";

    /**
     * This is a complete list of cluster-wide properties used by the SFTP polling listener module.
     */
    String[][] MODULE_CLUSTER_PROPERTIES = new String[][] {
            new String[] { SFTP_POLLING_MESSAGE_MAX_BYTES_PROPERTY, SFTP_POLLING_MESSAGE_MAX_BYTES_UI_PROPERTY, SFTP_POLLING_MESSAGE_MAX_BYTES_DESC, "5242880" },
            new String[] { LISTENER_THREAD_LIMIT_PROPERTY, LISTENER_THREAD_LIMIT_UI_PROPERTY, LISTENER_THREAD_LIMIT_DESC, "25" },
            new String[] { SFTP_POLLING_CONNECT_ERROR_SLEEP_PROPERTY, SFTP_POLLING_CONNECT_ERROR_SLEEP_UI_PROPERTY, SFTP_POLLING_CONNECT_ERROR_SLEEP_DESC, "10s",},
            new String[] { SFTP_POLLING_DOWNLOAD_THREAD_WAIT_SECONDS_PROPERTY, SFTP_POLLING_DOWNLOAD_THREAD_WAIT_SECONDS_UI_PROPERTY, SFTP_POLLING_DOWNLOAD_THREAD_WAIT_SECONDS_DESC, "3"},
            new String[] { SFTP_POLLING_IGNORED_FILE_EXTENSION_LIST_PROPERTY, SFTP_POLLING_IGNORED_FILE_EXTENSION_LIST_UI_PROPERTY, SFTP_IGNORED_FILE_EXTENSION_LIST_DESC, ".filepart"},
    };
}
