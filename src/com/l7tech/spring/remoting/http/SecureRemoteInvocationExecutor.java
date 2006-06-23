package com.l7tech.spring.remoting.http;

import java.lang.reflect.InvocationTargetException;
import java.util.Set;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.security.AccessController;
import java.security.AccessControlException;

import javax.security.auth.Subject;

import org.springframework.remoting.support.RemoteInvocationExecutor;
import org.springframework.remoting.support.RemoteInvocation;
import org.hibernate.HibernateException;

import com.l7tech.server.admin.AdminSessionManager;
import com.l7tech.admin.AdminLogin;
import com.l7tech.identity.User;
import com.l7tech.common.util.ExceptionUtils;

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

        try {
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
        catch(InvocationTargetException ite) {
            throw replaceIfNotSupported(ite);
        }
    }

    //- PRIVATE

    private static final String SYS_PROP_INCLUDE_STACK_FOR_CLIENT = "com.l7tech.spring.remoting.http.sendStack";
    private static final String DEFAULT_INCLUDE_STACK = Boolean.FALSE.toString();
    private static final Logger logger = Logger.getLogger(SecureRemoteInvocationExecutor.class.getName());

    private AdminSessionManager sessionManager;
    private static final boolean sendStackToClient = Boolean.valueOf(System.getProperty(SYS_PROP_INCLUDE_STACK_FOR_CLIENT, DEFAULT_INCLUDE_STACK));

    private static InvocationTargetException replaceIfNotSupported(InvocationTargetException ite)  {
        InvocationTargetException exception = ite;
        Throwable cause = ite.getCause();
        Throwable replacement = ExceptionUtils.filter(cause, new Class[]{HibernateException.class}, sendStackToClient);

        if (cause != replacement) {
            exception = new InvocationTargetException(replacement, ite.getMessage());
            exception.setStackTrace(ite.getStackTrace());

            if (logger.isLoggable(Level.WARNING))
                logger.log(Level.WARNING, "An exception during a remote invocation is not supported by the client.", cause);
        }

        return exception;
    }
}
