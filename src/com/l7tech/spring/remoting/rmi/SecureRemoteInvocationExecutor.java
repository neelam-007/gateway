package com.l7tech.spring.remoting.rmi;

import java.lang.reflect.InvocationTargetException;
import java.security.Principal;
import java.util.Set;
import javax.security.auth.Subject;

import org.springframework.remoting.support.RemoteInvocation;
import org.springframework.remoting.support.RemoteInvocationExecutor;

import com.l7tech.admin.AdminLogin;
import com.l7tech.identity.User;
import com.l7tech.server.admin.AdminSessionManager;

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
    public void setAdminSessionManager(AdminSessionManager sessionManager) {
        if (sessionManager == null) throw new NullPointerException();
        this.sessionManager = sessionManager;
    }

    /**
     * Redefined here to be visible to RmiInvocationWrapper.
     * Simply delegates to the corresponding superclass method.
     */
    public Object invoke(RemoteInvocation invocation, Object targetObject)
      throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {

        String methodName = invocation.getMethodName();
        if ((AdminLogin.class.isAssignableFrom(targetObject.getClass())
           && ("login".equals(methodName) || "getServerCertificate".equals(methodName)))) {
            // Only a few methods can be unauthenticated
            return invocation.invoke(targetObject);
        }

        AdminSessionRemoteInvocation adminInvocation = (AdminSessionRemoteInvocation) invocation;

        // All other invocations must carry the session cookie established by the login
        //  NOTE: passing the Subject object has a side effect that allows the IdentityAdminImpl#getRoles(Subject) to work
        Subject administrator = adminInvocation.getSubject();
        if(administrator==null) {
            throw new IllegalStateException("No subject passed, authentication failed.");
        }

        Set principals = administrator.getPrincipals();
        if(principals==null || principals.isEmpty()) {
            throw new IllegalStateException("No principal(s) available, authentication failed.");
        }
        Object principal = principals.iterator().next();
        if(!(principal instanceof User)) {
            throw new IllegalStateException("Principal type is incorrect, authentication failed.");
        }
        User user = (User) principal;

        String cookie = user.getLogin();
        Principal authUser = sessionManager.resumeSession(cookie);
        if (authUser == null) throw new IllegalStateException("Session cookie did not refer to a previously-established session");

        administrator.getPrincipals().clear();
        administrator.getPrincipals().add(authUser);
        administrator.getPrivateCredentials().clear(); // not necessary but couldn't hurt
        administrator.getPublicCredentials().clear(); // ditto

        return adminInvocation.invoke(targetObject);
    }

    //- PRIVATE

    private AdminSessionManager sessionManager;
}
