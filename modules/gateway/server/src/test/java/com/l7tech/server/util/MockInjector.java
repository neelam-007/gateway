package com.l7tech.server.util;

/**
 * Injector that does nothing
 */
public class MockInjector implements Injector {
    @Override
    public void inject( final Object target ) {
    }
}
