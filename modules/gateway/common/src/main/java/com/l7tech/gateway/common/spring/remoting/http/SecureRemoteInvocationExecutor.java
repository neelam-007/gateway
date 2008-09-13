package com.l7tech.gateway.common.spring.remoting.http;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.annotation.Annotation;

import org.springframework.remoting.support.RemoteInvocationExecutor;
import org.springframework.remoting.support.RemoteInvocation;

import com.l7tech.gateway.common.spring.remoting.RemotingProvider;

/**
 * Secure invoker.
 *
 * @author $Author$
 * @version $Revision$
 */
public final class SecureRemoteInvocationExecutor<T extends Annotation> implements RemoteInvocationExecutor {
    private final String facility;
    private final Class<T> annotationClass;
    private final RemotingProvider<T> remotingProvider;

    /**
     *
     */
    public SecureRemoteInvocationExecutor( final String facility, final Class<T> annotationClass, final RemotingProvider<T> remotingProvider ) {
        if ( remotingProvider == null )
            throw new IllegalArgumentException("remotingProvider is required");

        this.facility = facility;
        this.annotationClass = annotationClass;
        this.remotingProvider = remotingProvider;
    }

    /**
     *
     */
    public Object invoke(RemoteInvocation invocation, Object targetObject)
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException
    {
        // Check if the method can be invoked when not authenticated (login method, etc)
        T adminAnno =
                getRemotingAnnotation(invocation.getMethodName(), invocation.getParameterTypes(), targetObject.getClass());

        remotingProvider.checkPermitted( adminAnno, facility, invocation.getClass().getName() + "#" + invocation.getMethodName() );

        return invocation.invoke(targetObject);
    }

    private T getRemotingAnnotation(String methodName, Class[] methodParameterTypes, Class targetClass) {
        T adminAnno = null;

        try {
            Method method = targetClass.getMethod(methodName, methodParameterTypes);
            adminAnno = method.getAnnotation(annotationClass);

            if ( adminAnno == null ) {
                // Check interfaces
                Class[] interfaces = targetClass.getInterfaces();
                for ( Class interfaceClass : interfaces ) {
                    try {
                        method = interfaceClass.getMethod(methodName, methodParameterTypes);
                        adminAnno = method.getAnnotation(annotationClass);
                        if  (adminAnno != null)
                            break;
                    } catch (NoSuchMethodException nsme) {
                        // continue
                    }
                }
            }
        } catch (NoSuchMethodException nsme) {
            // ok, for now              
        }

        return adminAnno;
    }
}
