package com.l7tech.server.policy.bundle.ssgman;

import com.l7tech.util.Functions;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Logger;

/**
 * Common code useful for all policy bundle export and install.
 */
public abstract class BaseGatewayManagementInvoker {
    protected static final Logger logger = Logger.getLogger(BaseGatewayManagementInvoker.class.getName());

    @NotNull
    protected final Functions.Nullary<Boolean> cancelledCallback;

    @NotNull
    protected final GatewayManagementInvoker gatewayManagementInvoker;   // invoker which can invoke an actual gateway management server assertion

    public BaseGatewayManagementInvoker(@NotNull final Functions.Nullary<Boolean> cancelledCallback, @NotNull final GatewayManagementInvoker gatewayManagementInvoker) {
        this.cancelledCallback = cancelledCallback;
        this.gatewayManagementInvoker = gatewayManagementInvoker;
    }

    public void checkInterrupted() throws InterruptedException {
        if (cancelledCallback.call() || Thread.interrupted()) {
            throw new InterruptedException();
        }
    }
}