package com.l7tech.spring.remoting.http;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.security.AccessController;
import java.security.AccessControlException;

import javax.security.auth.Subject;

import org.springframework.remoting.support.RemoteInvocationExecutor;
import org.springframework.remoting.support.RemoteInvocation;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.InvalidResultSetAccessException;
import org.springframework.jdbc.JdbcUpdateAffectedIncorrectNumberOfRowsException;
import org.springframework.jdbc.LobRetrievalFailureException;
import org.springframework.jdbc.SQLWarningException;
import org.springframework.jdbc.UncategorizedSQLException;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.orm.hibernate3.HibernateSystemException;

import com.l7tech.admin.Administrative;
import com.l7tech.identity.User;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.common.LicenseException;
import com.l7tech.common.LicenseManager;
import com.l7tech.server.GatewayFeatureSets;

/**
 * Secure invoker.
 *
 * @author $Author$
 * @version $Revision$
 */
public final class SecureRemoteInvocationExecutor implements RemoteInvocationExecutor {
    Logger log = Logger.getLogger(SecureRemoteInvocationExecutor.class.getName());

    //- PUBLIC

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
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {

        try {
            // Check if the method can be invoked when not authenticated (login method, etc)
            Administrative adminAnno =
                    getAdministrativeAnnotation(invocation.getMethodName(), invocation.getParameterTypes(), targetObject.getClass());

            if ( adminAnno!=null && adminAnno.authenticated()==false ) {
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

            if (adminAnno == null || adminAnno.licensed()==true) {
                checkLicense(invocation.getMethodName());
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

    private static final boolean sendStackToClient = Boolean.valueOf(System.getProperty(SYS_PROP_INCLUDE_STACK_FOR_CLIENT, DEFAULT_INCLUDE_STACK));

    private final LicenseManager licenseManager;

    private void checkLicense(String methodName) {
        try {
            licenseManager.requireFeature(GatewayFeatureSets.SERVICE_ADMIN);
        } catch (LicenseException e) {
            // New exception to conceal original stack trace from LicenseManager
            log.log(Level.WARNING, "License checking failed when invoking the method, " + methodName, e);
            throw new RuntimeException(ExceptionUtils.getMessage(e), new LicenseException(e.getMessage()));
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

    private static InvocationTargetException replaceIfNotSupported(InvocationTargetException ite)  {
        InvocationTargetException exception = ite;
        Throwable cause = ite.getCause();
        Throwable replacement = ExceptionUtils.filter(cause,
                new Class[]{// spring jdbc errors
                            BadSqlGrammarException.class,
                            CannotGetJdbcConnectionException.class,
                            InvalidResultSetAccessException.class,
                            JdbcUpdateAffectedIncorrectNumberOfRowsException.class,
                            LobRetrievalFailureException.class,
                            SQLWarningException.class,
                            UncategorizedSQLException.class,
                            ObjectOptimisticLockingFailureException.class,
                            // spring hibernate dao
                            HibernateSystemException.class,
                            // connection pooling exceptions
                            com.mchange.lang.PotentiallySecondaryException.class,
                            com.mchange.util.AssertException.class,
                            com.mchange.v2.ser.UnsupportedVersionException.class,
                            com.mchange.v2.util.ResourceClosedException.class,
                            // mysql exceptions
                            com.mysql.jdbc.exceptions.MySQLNonTransientException.class,
                },
                sendStackToClient);

        if (cause != replacement) {
            exception = new InvocationTargetException(replacement, ite.getMessage());
            exception.setStackTrace(ite.getStackTrace());

            if (logger.isLoggable(Level.WARNING))
                logger.log(Level.WARNING, "An exception during a remote invocation is not supported by the client.", cause);
        }

        return exception;
    }
}
