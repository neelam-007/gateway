package com.l7tech.server.policy.bundle.ssgman;

import com.l7tech.identity.User;
import com.l7tech.identity.UserBean;
import com.l7tech.server.util.JaasUtils;
import com.l7tech.util.Functions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
    @Nullable
    private UserBean authenticatedUser;

    public BaseGatewayManagementInvoker(@NotNull final Functions.Nullary<Boolean> cancelledCallback,
                                        @NotNull final GatewayManagementInvoker gatewayManagementInvoker) {
        this.cancelledCallback = cancelledCallback;
        this.gatewayManagementInvoker = gatewayManagementInvoker;
    }

    public void checkInterrupted() throws InterruptedException {
        if (cancelledCallback.call() || Thread.interrupted()) {
            throw new InterruptedException();
        }
    }

    public void setAuthenticatedUser(@Nullable UserBean authenticatedUser) {
        this.authenticatedUser = authenticatedUser;
    }

    @Nullable
    public UserBean getAuthenticatedUser() {
        if (authenticatedUser == null) {
            final User currentUser = JaasUtils.getCurrentUser();
            if (currentUser != null) {
                // convert logged on user into a UserBean as if the user was authenticated via policy.
                this.authenticatedUser = new UserBean(currentUser.getProviderId(), currentUser.getLogin());
                this.authenticatedUser.setUniqueIdentifier(currentUser.getId());
            } else {
                this.authenticatedUser = null;
            }
        }
        return authenticatedUser;
    }
}