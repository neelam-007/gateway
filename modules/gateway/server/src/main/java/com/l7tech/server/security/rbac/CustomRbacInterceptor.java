package com.l7tech.server.security.rbac;

import com.l7tech.identity.User;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.jetbrains.annotations.NotNull;

/**
 * Interface implemented by a custom interceptor class.
 * <p/>
 * Implementers of this interface instantiated by the SecuredMethodInterceptor will be created by Spring and will
 * have Spring beans injected.  Spring will locate a constructor for autowiring if necessary.
 * <p/>
 * Having at least an @Inject RbacServices field is recommended in order to be able to do some kind of RBAC checking.
 */
public interface CustomRbacInterceptor extends MethodInterceptor {
    /**
     * Set the current user, which will likely be needed in order to make access decisions.
     *
     * @param user a User instance.  Required.
     */
    void setUser(@NotNull User user);

    /**
     * Invoke a custom RBAC interceptor to process the specified method invocation, informed by the specified
     * parameters gathered from any
     *
     * @param invocation invocation to invoke (if before-invocation checks pass).  Required.
     * @return the (possibly-filtered) return value from invoking the method, if after-invocation checks pass.
     *         May be null if the method may return null.
     * @throws Throwable a (possibly-filtered) exception thrown by the invoked method, if after-invocation checks pass;
     *         or else a PermissionDeniedException if a pre- or post-invocation check fails;
     *         or else IllegalStateException if a required setter (setUser) has not been invoked.
     */
    @Override
    Object invoke(@NotNull MethodInvocation invocation) throws Throwable;
}
