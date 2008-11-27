package com.l7tech.server.ems.standardreports;

/**
 *
 */
public class ReportException extends Exception {
    
    public ReportException(String message) {
        super(message);
    }

    public ReportException(String message, Throwable cause) {
        super(message, cause);
    }
}
