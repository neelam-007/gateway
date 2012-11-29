package com.l7tech.server.security.rbac;

import com.l7tech.identity.User;
import org.aopalliance.intercept.MethodInvocation;
import org.jetbrains.annotations.NotNull;


/**
 * This is used by the SecureMethodInterceptorTest. It needs to the here the JVM couldn't find it when it was a subclass of SecureMethodInterceptorTest
 */
public class MockCustomRbacInterceptor implements CustomRbacInterceptor {

    @Override
    public void setUser(@NotNull User user) {
    }

    @Override
    public Object invoke(@NotNull MethodInvocation invocation) throws Throwable {
        return SecureMethodInterceptorTest.genericString;
    }
}
