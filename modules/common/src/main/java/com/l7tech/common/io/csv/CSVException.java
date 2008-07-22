/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.common.io.csv;

/**
 * Signals parsing error.
 *
 * @since SecureSpan 4.4
 * @author rmak
 * @see CSVPreference
 * @see CSVReader
 */
public class CSVException extends RuntimeException {
    public CSVException(final String message) {
        super(message);
    }
}
