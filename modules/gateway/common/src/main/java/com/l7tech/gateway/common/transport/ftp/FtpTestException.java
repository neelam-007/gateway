/*
 * Copyright (C) 2007 Layer 7 Technologies, Inc.
 */

package com.l7tech.gateway.common.transport.ftp;

/**
 * Exception thrown when a FTP connection test failed.
 *
 * @author rmak
 * @since SecureSpan 4.0
 */
public class FtpTestException extends Exception {
    private String _sessionLog;

    public FtpTestException() {
        super();
    }

    public FtpTestException(String message, String sessionLog) {
        super(message);
        _sessionLog = sessionLog;
    }

    /**
     * @return a session log containing the FTP server responses
     */
    public String getSessionLog() {
        return _sessionLog;
    }
}
