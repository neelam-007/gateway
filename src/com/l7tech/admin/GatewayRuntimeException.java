package com.l7tech.admin;

/**
 * @author: ghuang
 */
public class GatewayRuntimeException extends RuntimeAdminException {

    public GatewayRuntimeException() {
    }


    public GatewayRuntimeException(String string) {
        super(string);
    }

    public GatewayRuntimeException(Throwable throwable) {
        super(throwable);
    }

    public GatewayRuntimeException(String string, Throwable throwable) {
        super(string, throwable);
    }
}
