package com.l7tech.server.identity.ldap;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

/**
 * MethodInterceptor that sets the context classloader for LDAP
 */
public class LdapClassLoaderMethodInterceptor implements MethodInterceptor {

    @Override
    public Object invoke( final MethodInvocation methodInvocation ) throws Throwable {
        final ClassLoader originalContextClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader( LdapSslCustomizerSupport.getSSLSocketFactoryClassLoader() );
            return methodInvocation.proceed();
        } finally {
            Thread.currentThread().setContextClassLoader( originalContextClassLoader );
        }
    }
}
