package com.l7tech.gateway.common.spring.remoting.rmi;

import java.lang.reflect.InvocationTargetException;
import java.security.AccessControlException;
import java.security.Principal;
import java.util.Set;
import javax.security.auth.Subject;

import org.springframework.remoting.support.RemoteInvocation;
import org.springframework.remoting.support.RemoteInvocationExecutor;

import com.l7tech.gateway.common.admin.AdminLogin;
import com.l7tech.identity.User;
import com.l7tech.gateway.common.spring.remoting.RemotingProvider;

/**
 * Secure invoker.
 *
 * @author $Author$
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
    public void setRemotingProvider( RemotingProvider remotingProvider ) {
        if ( remotingProvider == null ) throw new NullPointerException();
        this.remotingProvider = remotingProvider;
    }

    /**
     * Redefined here to be visible to RmiInvocationWrapper.
     * Simply delegates to the corresponding superclass method.
     */
    public Object invoke(RemoteInvocation invocation, Object targetObject)
      throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {

        String methodName = invocation.getMethodName();
        if ((AdminLogin.class.isAssignableFrom(targetObject.getClass())
           && ("resume".equals(methodName) || "login".equals(methodName) || "getServerCertificate".equals(methodName)))) {
            // Only a few methods can be unauthenticated
            return invocation.invoke(targetObject);
        }

        AdminSessionRemoteInvocation adminInvocation = (AdminSessionRemoteInvocation) invocation;

        // All other invocations must carry the session cookie established by the login
        //  NOTE: passing the Subject object has a side effect that allows the IdentityAdminImpl#getRoles(Subject) to work
        Subject administrator = adminInvocation.getSubject();
        if(administrator==null) {
            throw new AccessControlException("No subject passed, authentication failed.");
        }

        Set principals = administrator.getPrincipals();
        if(principals==null || principals.isEmpty()) {
            throw new AccessControlException("No principal(s) available, authentication failed.");
        }
        Object principal = principals.iterator().next();
        if(!(principal instanceof User)) {
            throw new AccessControlException("Principal type is incorrect, authentication failed.");
        }
        User user = (User) principal;

        String cookie = user.getLogin();
        Principal authUser = remotingProvider.getPrincipalForCookie( cookie );
        if (authUser == null) throw new AccessControlException("Session cookie did not refer to a previously-established session");

        administrator.getPrincipals().clear();
        administrator.getPrincipals().add(authUser);
        administrator.getPrivateCredentials().clear(); // not necessary but couldn't hurt
        administrator.getPublicCredentials().clear(); // ditto

        return adminInvocation.invoke(targetObject);
    }

    //- PRIVATE

    private RemotingProvider remotingProvider;
}
