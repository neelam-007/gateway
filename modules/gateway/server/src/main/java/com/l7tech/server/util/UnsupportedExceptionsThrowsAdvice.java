package com.l7tech.server.util;

import com.l7tech.util.ExceptionUtils;
import org.springframework.jdbc.*;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.orm.hibernate3.HibernateSystemException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author: ghuang
 */
public class UnsupportedExceptionsThrowsAdvice extends ThrowsAdviceSupport {

    private static final Logger logger = Logger.getLogger(UnsupportedExceptionsThrowsAdvice.class.getName());

    private static final String[] OPTIONAL_BLACKLIST = new String[] {
            // mysql specific exceptions
            "com.mysql.jdbc.exceptions.MySQLNonTransientException",
            "com.mysql.jdbc.exceptions.MySQLTimeoutException",
            "com.mysql.jdbc.AssertionFailedException",
            "com.mysql.jdbc.CommunicationsException",
            "com.mysql.jdbc.MysqlDataTruncation",
            "com.mysql.jdbc.OperationNotSupportedException",
            "com.mysql.jdbc.PacketTooBigException",
            "com.mysql.jdbc.RowDataDynamic$OperationNotSupportedException",

            // Luna v4 exceptions
            "com.chrysalisits.crypto.LunaCryptokiException",
            "com.chrysalisits.crypto.LunaException",
            "com.chrysalisits.crypto.LunaPartitionFullException",

            // Luna v5 exceptions
            "com.safenetinc.luna.LunaCryptokiException",
            "com.safenetinc.luna.LunaException",
            "com.safenetinc.luna.LunaPartitionFullException",
    };

    @SuppressWarnings( { "deprecation" } )
    private static final Class[] REQUIRED_BLACKLIST = new Class[] {
            // our own server side exceptions
            com.l7tech.server.policy.ServerPolicyException.class,
            com.l7tech.server.identity.ldap.BindOnlyLdapUserManager.BadUsernamePatternException.class,
            // java
            javax.naming.NamingException.class, // can contain non-serializable objects, see bug 4738
            // java internals
            sun.security.krb5.KrbException.class,
            // spring jdbc errors
            BadSqlGrammarException.class,
            CannotGetJdbcConnectionException.class,
            InvalidResultSetAccessException.class,
            JdbcUpdateAffectedIncorrectNumberOfRowsException.class,
            LobRetrievalFailureException.class,
            SQLWarningException.class,
            UncategorizedSQLException.class,
            ObjectOptimisticLockingFailureException.class,
            org.springframework.transaction.TransactionException.class,
            org.hibernate.exception.JDBCConnectionException.class,
            // spring hibernate dao
            HibernateSystemException.class,
            // connection pooling exceptions
            com.mchange.lang.PotentiallySecondaryException.class,
            com.mchange.util.AssertException.class,
            com.mchange.v2.ser.UnsupportedVersionException.class,
            com.mchange.v2.util.ResourceClosedException.class,
            com.mchange.v2.resourcepool.ResourcePoolException.class,
    };

    private static Class[] blacklist;

    public void afterThrowing(final Throwable throwable) throws Throwable {
        Throwable t = replaceIfNotSupported(throwable);
        if (t != null) {
            throw t;
        }
    }

    private Throwable replaceIfNotSupported(Throwable cause)  {
        Throwable replacement = ExceptionUtils.filter(cause, getClassBlacklist(), isSendStackToClient());

        if (cause != replacement) {
            if (logger.isLoggable(Level.WARNING))
                logger.log(Level.WARNING, "An exception during a remote invocation is not supported by the client.", cause);

            return replacement;
        }
        return null;
    }

    private static Class[] getClassBlacklist() {
        Class[] list = blacklist;

        if (list == null) {
            List<Class> allListed = new ArrayList<Class>();
            allListed.addAll(Arrays.asList(REQUIRED_BLACKLIST));

            for (String className : OPTIONAL_BLACKLIST) {
                try {
                    allListed.add(Class.forName(className));
                } catch(ClassNotFoundException cnfe) {
                    logger.fine("Unable to load optional class '"+className+"', ignoring.");
                }
            }

            list = allListed.toArray(new Class[allListed.size()]);
            blacklist = list;
        }

        return list;
    }
}
