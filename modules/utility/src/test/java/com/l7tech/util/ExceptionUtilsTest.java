/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.util;

import org.junit.Test;
import static org.junit.Assert.*;


import java.util.logging.Logger;

import com.l7tech.util.ExceptionUtils;

/**
 * Tests for exception utilities.
 *
 * User: mike
 * Date: Sep 5, 2003
 * Time: 12:03:48 PM
 */
public class ExceptionUtilsTest {
    private static Logger log = Logger.getLogger(ExceptionUtilsTest.class.getName());

    private static class SizeException extends Exception {
        SizeException() {}
        SizeException(Throwable cause) { super(cause); }
    }

    private static class WidthException extends SizeException {
        WidthException() {}
        WidthException(Throwable cause) { super(cause); }
    }

    private static class MaterialException extends Exception {
        MaterialException() {}
        MaterialException(Throwable cause) { super(cause); }
    }

    private static class SmellException extends MaterialException {
        SmellException() {}
        SmellException(Throwable cause) { super(cause); }
    }

    private static class MovingException extends Exception {
        MovingException() {}
        MovingException(Throwable cause) { super(cause); }
    }

    @Test
    public void testGetCause() throws Exception {
        WidthException fat = new WidthException();
        RuntimeException re = new RuntimeException(fat);
        assertTrue( ExceptionUtils.getCauseIfCausedBy(re, WidthException.class) == fat);
    }

    @Test
    public void testCausedBy() throws Exception {
        WidthException fat = new WidthException();
        SmellException smelly = new SmellException();
        MovingException cantmove = new MovingException(fat);
        RuntimeException re = new RuntimeException(cantmove);

        assertTrue(ExceptionUtils.causedBy(re, SizeException.class));
        assertFalse(ExceptionUtils.causedBy(re, MaterialException.class));
        assertFalse(ExceptionUtils.causedBy(null, null));
        assertFalse(ExceptionUtils.causedBy(null, Object.class));
        assertTrue(ExceptionUtils.causedBy(re, Object.class));
        assertTrue(ExceptionUtils.causedBy(re, Throwable.class));
        assertTrue(ExceptionUtils.causedBy(re, Exception.class));
        assertFalse(ExceptionUtils.causedBy(re, Integer.class));

        Exception e = new Exception();
        Throwable t = new Throwable();
        Throwable te = new Throwable(new Exception());
        assertTrue(ExceptionUtils.causedBy(t, Throwable.class));
        assertTrue(ExceptionUtils.causedBy(te, Throwable.class));
        assertTrue(ExceptionUtils.causedBy(te, Exception.class));
        assertFalse(ExceptionUtils.causedBy(t, Exception.class));
        assertTrue(ExceptionUtils.causedBy(e, Exception.class));
        assertTrue(ExceptionUtils.causedBy(e, Throwable.class));
        assertTrue(ExceptionUtils.causedBy(e, Object.class));
        assertFalse(ExceptionUtils.causedBy(e, MovingException.class));

        assertTrue(ExceptionUtils.causedBy(cantmove, Exception.class));
        assertTrue(ExceptionUtils.causedBy(cantmove, MovingException.class));
        assertTrue(ExceptionUtils.causedBy(cantmove, SizeException.class));
        assertTrue(ExceptionUtils.causedBy(cantmove, WidthException.class));
        assertFalse(ExceptionUtils.causedBy(cantmove, SmellException.class));
        assertFalse(ExceptionUtils.causedBy(cantmove, smelly.getClass()));
    }
}
