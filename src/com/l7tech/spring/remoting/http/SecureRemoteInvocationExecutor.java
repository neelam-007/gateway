package com.l7tech.spring.remoting.http;

import java.lang.reflect.InvocationTargetException;
import java.util.Set;
import java.security.AccessController;
import java.security.AccessControlException;

import javax.security.auth.Subject;

import org.springframework.remoting.support.RemoteInvocationExecutor;
import org.springframework.remoting.support.RemoteInvocation;

import com.l7tech.server.admin.AdminSessionManager;
import com.l7tech.admin.AdminLogin;
import com.l7tech.identity.User;

/**
 * Secure invoker.
 *
 * @author $Author$
 * @version $Revision$
 */
public final class SecureRemoteInvocationExecutor implements RemoteInvocationExecutor {

    //- PUBLIC

    /**
     *
     */
    public SecureRemoteInvocationExecutor() {
    }

    /**
     *
     */
    public Object invoke(RemoteInvocation invocation, Object targetObject)
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {

        // Only a few methods can be unauthenticated
        String methodName = invocation.getMethodName();
        if ((AdminLogin.class.isAssignableFrom(targetObject.getClass())
                && ("login".equals(methodName) || "getServerCertificate".equals(methodName)))) {
            return invocation.invoke(targetObject);
        }

        // All other invocations must be secured
        Subject administrator = Subject.getSubject(AccessController.getContext());;
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

        return invocation.invoke(targetObject);
    }

    //- PRIVATE

    private AdminSessionManager sessionManager;
}
