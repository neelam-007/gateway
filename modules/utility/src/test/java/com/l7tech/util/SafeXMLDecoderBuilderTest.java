package com.l7tech.util;

import org.junit.After;
import org.junit.Test;

import java.beans.ExceptionListener;

import static org.junit.Assert.*;

/**
 * Unit test for SafeXMLDecoderBuilder.
 */
public class SafeXMLDecoderBuilderTest {
    @After
    public void cleanup() {
        SyspropUtil.clearProperties(
            SafeXMLDecoderBuilder.PROP_ERRORS_FATAL_BY_DEFAULT
        );
    }

    @Test(expected = RuntimeException.class)
    public void testFatalListener() throws Exception {
        SafeXMLDecoderBuilder.getFatalExceptionListener().exceptionThrown(new CausedIOException("blah"));
    }

    @Test
    public void testLoggingListener() throws Exception {
        SafeXMLDecoderBuilder.getLoggingExceptionListener().exceptionThrown(new CausedIOException("blah"));
        // OK, assume it worked (not worth setting up a mock logger just to test this)
    }

    @Test
    public void testDefaultListener() throws Exception {
        SyspropUtil.clearProperties(SafeXMLDecoderBuilder.PROP_ERRORS_FATAL_BY_DEFAULT);
        ExceptionListener d = SafeXMLDecoderBuilder.getDefaultExceptionListener();
        assertEquals(SafeXMLDecoderBuilder.getLoggingExceptionListener().getClass(), d.getClass());
    }

    @Test
    public void testDefaultListener_logging() throws Exception {
        SyspropUtil.setProperty(SafeXMLDecoderBuilder.PROP_ERRORS_FATAL_BY_DEFAULT, "false");
        ExceptionListener d = SafeXMLDecoderBuilder.getDefaultExceptionListener();
        assertEquals(SafeXMLDecoderBuilder.getLoggingExceptionListener().getClass(), d.getClass());
    }

    @Test
    public void testDefaultListener_fatal() throws Exception {
        SyspropUtil.setProperty(SafeXMLDecoderBuilder.PROP_ERRORS_FATAL_BY_DEFAULT, "true");
        ExceptionListener d = SafeXMLDecoderBuilder.getDefaultExceptionListener();
        assertEquals(SafeXMLDecoderBuilder.getFatalExceptionListener().getClass(), d.getClass());
    }
}
