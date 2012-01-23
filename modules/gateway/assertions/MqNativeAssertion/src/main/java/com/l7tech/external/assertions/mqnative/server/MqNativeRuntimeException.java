package com.l7tech.external.assertions.mqnative.server;

/**
 *
 */
class MqNativeRuntimeException extends RuntimeException {

    public MqNativeRuntimeException( final String message ) {
        super( message );
    }

    public MqNativeRuntimeException( final String message, final Throwable cause ) {
        super( message, cause );
    }

    public MqNativeRuntimeException( final Throwable cause ) {
        super( cause );    //To change body of overridden methods use File | Settings | File Templates.
    }
}
