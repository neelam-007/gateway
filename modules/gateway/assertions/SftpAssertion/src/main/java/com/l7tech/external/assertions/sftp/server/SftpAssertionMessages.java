package com.l7tech.external.assertions.sftp.server;

import com.l7tech.gateway.common.audit.Messages;

/**
 * This is the central place that consolidates all messages used by the SFTP Assertion for exceptions or logging.
 */
public class SftpAssertionMessages extends Messages{

    /* Warning messages */
    public static final String SFTP_NO_SUCH_PART_ERROR = "Connection established but Failed to SFTP to server with NoSuchPartException from input: ";
    public static final String SFTP_EXCEPTION_ERROR = "Connection established but Failed to SFTP to server with SFTPException. SFTP Client issue: ";
    public static final String SFTP_DIR_DOESNT_EXIST_ERROR = "Connection established but Failed to SFTP to server with SFTPException. Ensure Directory exists and user has correct permissions: ";
    public static final String SFTP_IO_EXCEPTION = "Connection established but Failed to SFTP to server with IOException: ";
    public static final String SFTP_SOCKET_EXCEPTION = "Error while attempting to connect to SFTP server.''{0}'' port ''{1}''  username ''{2}''. Failing Assertion with socket exception ";
    public static final String SFTP_CONNECTION_EXCEPTION = "Error while attempting to connect to SFTP server.''{0}'' port ''{1}''  username ''{2}''. Failing Assertion with exception ";
    public static final String SFTP_CERT_ISSUE_EXCEPTION = "Error while attempting to decode the serverkey - ensure the key is copied correctly into the UI ";

    public static final String SFTP_ALGO_NOT_SUPPORTED_EXCEPTION = "The SSH Server Key algorithm entered ''{0}'' is not supported - only RSA and DSA keys are supported";
    public static final String SFTP_WRONG_FORMAT_SUPPORTED_EXCEPTION = "The SSH Server Key must be in the format 'ssh-algorithm serverykey' as per RSA or DSA public cert contents";
    public static final String SFTP_INVALID_CERT_EXCEPTION = "SFTP server trusted cert is INVALID - The SSH Server Key must be in the format 'ssh-algorithm serverykey' as per RSA or DSA public cert contents";
}