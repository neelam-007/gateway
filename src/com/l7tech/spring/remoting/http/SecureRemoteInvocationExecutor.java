package com.l7tech.spring.remoting.http;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.security.AccessController;
import java.security.AccessControlException;

import javax.security.auth.Subject;
import javax.servlet.http.HttpServletRequest;

import org.springframework.remoting.support.RemoteInvocationExecutor;
import org.springframework.remoting.support.RemoteInvocation;

import com.l7tech.admin.Administrative;
import com.l7tech.admin.LicenseRuntimeException;
import com.l7tech.identity.User;
import com.l7tech.common.LicenseException;
import com.l7tech.common.LicenseManager;
import com.l7tech.common.transport.SsgConnector;
import com.l7tech.server.GatewayFeatureSets;
import com.l7tech.server.transport.http.HttpTransportModule;
import com.l7tech.spring.remoting.RemoteUtils;

/**
 * Secure invoker.
 *
 * @author $Author$
 * @version $Revision$
 */
public final class SecureRemoteInvocationExecutor implements RemoteInvocationExecutor {
    private Logger log = Logger.getLogger(SecureRemoteInvocationExecutor.class.getName());
    private final LicenseManager licenseManager;

    /**
     *
     */
    public SecureRemoteInvocationExecutor(final LicenseManager licenseManager) {
        if (licenseManager == null)
            throw new IllegalArgumentException("licenseManager is required");
        
        this.licenseManager = licenseManager;
    }

    /**
     *
     */
    public Object invoke(RemoteInvocation invocation, Object targetObject)
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException
    {
        requireAdminPort();

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
            checkLicense(invocation.getMethodName());
        }

        return invocation.invoke(targetObject);
    }

    /**
     * Assert that this request arrived over a port that enables either ADMIN_APPLET or ADMIN_REMOTE.
     */
    private void requireAdminPort() {
        HttpServletRequest hreq = RemoteUtils.getHttpServletRequest();
        if (hreq == null)
            throw new AccessControlException("Admin request disallowed: No request context available");
        SsgConnector connector = HttpTransportModule.getConnector(hreq);
        if (connector == null)
            throw new AccessControlException("Admin request disallowed: Unable to determine which connector this request came in on");
        if (!connector.offersEndpoint(SsgConnector.Endpoint.ADMIN_APPLET) && !connector.offersEndpoint(SsgConnector.Endpoint.ADMIN_REMOTE))
            throw new AccessControlException("Request not permitted on this port");
    }

    private void checkLicense(String methodName) {
        try {
            licenseManager.requireFeature(GatewayFeatureSets.SERVICE_ADMIN);
        } catch (LicenseException e) {
            log.log(Level.WARNING, "License checking failed when invoking the method, " + methodName);
            throw new LicenseRuntimeException(e);
        }
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
