package com.l7tech.external.assertions.ssh.server;

import com.l7tech.gateway.common.audit.Messages;

/**
 * This is the central place that consolidates all messages used by the SCP and SFTP Assertion for exceptions or logging.
 */
public class SshAssertionMessages extends Messages{

    /* Warning messages */
    public static final String SSH_NO_SUCH_PART_ERROR = "Connection established, but operation failed with NoSuchPartException from input: ";
    public static final String SSH_EXCEPTION_ERROR = "Connection established, but failed with SCPException/SFTPException. SSH client issue: ";
    public static final String SSH_DIR_DOESNT_EXIST_ERROR = "Connection established, but failed with SCPException/SFTPException. Ensure directory exists and user has correct permissions";
    public static final String SSH_IO_EXCEPTION = "Connection established, but failed with IOException: ";
    public static final String SSH_SOCKET_EXCEPTION = "Error while attempting to connect to SSH server. ''{0}'' port ''{1}'' username ''{2}''. Failing Assertion with socket exception";
    public static final String SSH_CONNECTION_EXCEPTION = "Error while attempting to connect to SSH server.''{0}'' port ''{1}'' username ''{2}''. Failing Assertion with exception";
    public static final String SSH_CERT_ISSUE_EXCEPTION = "Error while attempting to decode the serverkey - ensure the key is copied correctly into the UI";
    public static final String SSH_INVALID_PUBLIC_KEY_FINGERPRINT_EXCEPTION = "SSH server trusted public key fingerprint is invalid. Format is a sequence of 16 octets printed " +
            "as hexadecimal with lowercase letters and separated by colons (e.g. c1:b1:30:29:d7:b8:de:6c:97:77:10:d7:46:41:63:87)";
}