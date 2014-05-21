package com.l7tech.gateway.common;


import org.junit.Test;

import java.io.IOException;

import static org.mockito.Mockito.*;

/**
 * Created with IntelliJ IDEA.
 * User: rballantyne
 * Date: 5/20/14
 * Time: 4:12 PM
 */
public class QueuingSyntaxErrorHandlerTest {

    @Test
    public void testHandleBadVariable() {
        final String badVar = "badVar";
        DefaultSyntaxErrorHandler dseh = mock(DefaultSyntaxErrorHandler.class);
        QueuingSyntaxErrorHandler qseh = new QueuingSyntaxErrorHandler(dseh);
        qseh.handleBadVariable(badVar);
        qseh.flushLogAuditEvents();
        verify(dseh).handleBadVariable(badVar);
    }

    @Test
    public void testHandleSuspiciousString() {
        final String stringVar = "someString";
        final String className = "com.l7tech.SomeClass";
        DefaultSyntaxErrorHandler dseh = mock(DefaultSyntaxErrorHandler.class);
        QueuingSyntaxErrorHandler qseh = new QueuingSyntaxErrorHandler(dseh);
        qseh.handleSuspiciousToString(stringVar,className);
        qseh.flushLogAuditEvents();
        verify(dseh).handleSuspiciousToString(stringVar,className);
    }

    @Test
    public void testHandleSubscriptOutOfRange() {
        final String stringVar = "someString";
        DefaultSyntaxErrorHandler dseh = mock(DefaultSyntaxErrorHandler.class);
        QueuingSyntaxErrorHandler qseh = new QueuingSyntaxErrorHandler(dseh);
        qseh.handleSubscriptOutOfRange(23,stringVar,99);
        qseh.flushLogAuditEvents();
        verify(dseh).handleSubscriptOutOfRange(23,stringVar,99);
    }

    @Test
    public void testHandleBadVariableWithExceptions() {
        final String badVar = "someString";
        final Exception e = new IOException("SomeException");
        DefaultSyntaxErrorHandler dseh = mock(DefaultSyntaxErrorHandler.class);
        QueuingSyntaxErrorHandler qseh = new QueuingSyntaxErrorHandler(dseh);
        qseh.handleBadVariable(badVar,e);
        qseh.flushLogAuditEvents();
        verify(dseh).handleBadVariable(badVar,e);
    }

    @Test
    public void testMultipleEvents() {
        final String badVar = "someString";
        final String someClass = "com.l7tech.SomeClass";
        final Exception e = new IOException("SomeException");
        DefaultSyntaxErrorHandler dseh = mock(DefaultSyntaxErrorHandler.class);
        QueuingSyntaxErrorHandler qseh = new QueuingSyntaxErrorHandler(dseh);
        qseh.handleBadVariable(badVar);
        qseh.handleBadVariable(badVar,e);
        qseh.handleSuspiciousToString(badVar,someClass);
        qseh.handleSubscriptOutOfRange(23,badVar,99);
        qseh.flushLogAuditEvents();
        verify(dseh).handleBadVariable(eq(badVar));
        verify(dseh).handleBadVariable(eq(badVar),eq(e));
        verify(dseh).handleSuspiciousToString(eq(badVar),eq(someClass));
        verify(dseh).handleSubscriptOutOfRange(eq(23),eq(badVar),eq(99));
    }

    @Test
    public void testNoFlush() {
        final String badVar = "something";
        final Exception e = new IOException("SomeException");
        DefaultSyntaxErrorHandler dseh = mock(DefaultSyntaxErrorHandler.class);
        QueuingSyntaxErrorHandler qseh = new QueuingSyntaxErrorHandler(dseh);
        qseh.handleBadVariable(badVar);
        verify(dseh,never()).handleBadVariable(anyString());
        verify(dseh,never()).handleBadVariable(anyString(),eq(e));
        verify(dseh,never()).handleSubscriptOutOfRange(anyInt(),anyString(),anyInt());
        verify(dseh,never()).handleSuspiciousToString(anyString(),anyString());
    }

}
