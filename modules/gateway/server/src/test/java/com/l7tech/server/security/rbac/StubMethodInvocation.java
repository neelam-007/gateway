package com.l7tech.server.security.rbac;

import org.aopalliance.intercept.MethodInvocation;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Method;

/**
 * This is used to stub a method invocation
 */
public class StubMethodInvocation implements MethodInvocation {
    private Method method;
    private Object[] arguments;
    private Object rtn;
    private Object t;

    public StubMethodInvocation(final Method method, final Object[] arguments, final Object rtn, final Object t) {
        this.method = method;
        this.arguments = arguments;
        this.rtn = rtn;
        this.t = t;
    }

    @Override
    public Method getMethod() {
        return method;
    }

    @Override
    public Object[] getArguments() {
        return arguments;
    }

    @Override
    public Object proceed() throws Throwable {
        return rtn;
    }

    @Override
    public Object getThis() {
        return t;
    }

    @Override
    public AccessibleObject getStaticPart() {
        return null;
    }
}
