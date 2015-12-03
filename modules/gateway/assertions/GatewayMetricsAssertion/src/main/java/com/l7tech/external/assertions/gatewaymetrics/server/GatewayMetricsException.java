package com.l7tech.external.assertions.gatewaymetrics.server;

/**
 * Created with IntelliJ IDEA.
 * User: cirving
 * Date: 7/26/12
 * Time: 9:54 AM
 * To change this template use File | Settings | File Templates.
 */
public class GatewayMetricsException extends Exception {
    public GatewayMetricsException(String message) {
        super(message);
    }

    public GatewayMetricsException(String message, Throwable cause) {
        super(message, cause);
    }
}
