package com.l7tech.gateway.common.spring.remoting.http;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Set;
import java.security.AccessController;
import java.security.AccessControlException;

import javax.security.auth.Subject;

import org.springframework.remoting.support.RemoteInvocationExecutor;
import org.springframework.remoting.support.RemoteInvocation;

import com.l7tech.gateway.common.admin.Administrative;
import com.l7tech.identity.User;
import com.l7tech.gateway.common.spring.remoting.RemotingProvider;

/**
 * Secure invoker.
 *
 * @author $Author$
 * @version $Revision$
 */
public final class SecureRemoteInvocationExecutor implements RemoteInvocationExecutor {
    private final RemotingProvider remotingProvider;

    /**
     *
     */
    public SecureRemoteInvocationExecutor( final RemotingProvider remotingProvider ) {
        if ( remotingProvider == null )
            throw new IllegalArgumentException("remotingProvider is required");
        
        this.remotingProvider = remotingProvider;
    }

    /**
     *
     */
    public Object invoke(RemoteInvocation invocation, Object targetObject)
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException
    {
        checkAdminEnabled();

        // Check if the method can be invoked when not authenticated (login method, etc)
        Administrative adminAnno =
                getAdministrativeAnnotation(invocation.getMethodName(), invocation.getParameterTypes(), targetObject.getClass());

        if (adminAnno != null && ! adminAnno.authenticated()) {
            return invocation.invoke(targetObject);
        }

        // All other invocations must be secured
        Subject administrator = Subject.getSubject(AccessController.getContext());
        if (administrator == null) {
            throw new AccessControlException("No subject passed, authentication failed.");
        }

        Set principals = administrator.getPrincipals();
        if (principals == null || principals.isEmpty()) {
            throw new AccessControlException("No principal(s) available, authentication failed.");
        }

        Object principal = principals.iterator().next();
        if (!(principal instanceof User)) {
            throw new AccessControlException("Principal type is incorrect, authentication failed.");
        }

        if (adminAnno == null || adminAnno.licensed()) {
            checkLicense(invocation.getClass().getName(), invocation.getMethodName());
        }

        return invocation.invoke(targetObject);
    }

    private void checkAdminEnabled() {
        remotingProvider.enforceAdminEnabled();
    }

    private void checkLicense(String className, String methodName) {
        remotingProvider.enforceLicensed( className, methodName );
    }

    private Administrative getAdministrativeAnnotation(String methodName, Class[] methodParameterTypes, Class targetClass) {
        Administrative adminAnno = null;

        try {
            Method method = targetClass.getMethod(methodName, methodParameterTypes);
            adminAnno = method.getAnnotation(Administrative.class);

            if ( adminAnno == null ) {
                // Check interfaces
                Class[] interfaces = targetClass.getInterfaces();
                for ( Class interfaceClass : interfaces ) {
                    try {
                        method = interfaceClass.getMethod(methodName, methodParameterTypes);
                        adminAnno = method.getAnnotation(Administrative.class);
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
