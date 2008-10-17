package com.l7tech.test.util;

/**
 *  Exception thrown if there is a problem running GUI tests with InteractiveGuiTester.
 */
public class GuiTestException extends Exception {
    public GuiTestException() {
    }

    public GuiTestException(String message) {
        super(message);
    }

    public GuiTestException(String message, Throwable cause) {
        super(message, cause);
    }

    public GuiTestException(Throwable cause) {
        super(cause);
    }
}
