package com.l7tech.test.performance.xmlbenchmark;

/**
 * Base exception for XML benchmark exceptions.
 *
 * @user: vchan
 */
public class BenchmarkException extends Exception {

    public BenchmarkException() {
    }

    public BenchmarkException(String message) {
        super(message);
    }

    public BenchmarkException(String message, Throwable cause) {
        super(message, cause);
    }

    public BenchmarkException(Throwable cause) {
        super(cause);
    }
}
