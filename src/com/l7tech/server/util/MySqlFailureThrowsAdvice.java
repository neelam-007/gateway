package com.l7tech.server.util;

import java.lang.reflect.Method;
import java.util.logging.Logger;
import java.util.logging.Level;

import com.l7tech.admin.GatewayRuntimeException;
import com.l7tech.common.util.ExceptionUtils;
import com.mchange.v2.resourcepool.CannotAcquireResourceException;

import org.hibernate.exception.JDBCConnectionException;

/**
 * Handle exceptions due to MySQL not being available.
 *
 * @author: ghuang
 */
public class MySqlFailureThrowsAdvice extends ThrowsAdviceSupport {

    //- PUBLIC

    @SuppressWarnings({"UnusedDeclaration"})
    public void afterThrowing(final Method method,
                              final Object[] args,
                              final Object target,
                              final Throwable throwable) throws GatewayRuntimeException {
        if ( ExceptionUtils.causedBy(throwable, CannotAcquireResourceException.class) ) {
            doMySQLFailure(throwable);
        } else if (MYSQL_COMMUNICATIONS != null) {
            if ( !ExceptionUtils.causedBy(throwable, MYSQL_PACKET_TOO_BIG) &&
                 ExceptionUtils.causedBy(throwable, MYSQL_COMMUNICATIONS)) {
                // handle communication failure if not caused by large packet,
                // if there is a JDBCConnectionException check the SQLState.
                JDBCConnectionException jce = ExceptionUtils.getCauseIfCausedBy(throwable, JDBCConnectionException.class);
                if (jce == null || CONNECT_FAILURE_STATE.equals(jce.getSQLState())) {
                    doMySQLFailure(throwable);
                } 
            } else if (ExceptionUtils.causedBy(throwable, MYSQL_TRANSIENT)) {
                // handle any transient failure
                doMySQLFailure(throwable);
            }
        }
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(MySqlFailureThrowsAdvice.class.getName());

    private static final String CONNECT_FAILURE_STATE = "08S01";

    // MySQL lib may not be present, which is OK so just turn off in that case
    private static final String MYSQL_PACKET_CLASSNAME = "com.mysql.jdbc.PacketTooBigException";
    private static final String MYSQL_TRANSIENT_CLASSNAME = "com.mysql.jdbc.exceptions.MySQLTransientException";
    private static final String MYSQL_COMMUNICATIONS_CLASSNAME = "com.mysql.jdbc.CommunicationsException";

    private static final Class MYSQL_PACKET_TOO_BIG;
    private static final Class MYSQL_TRANSIENT;
    private static final Class MYSQL_COMMUNICATIONS;

    // Load classes if available
    static {
        Class CLASS_MYSQL_PACKET_TOO_BIG = null;
        Class CLASS_MYSQL_TRANSIENT = null;
        Class CLASS_MYSQL_COMMUNICATIONS = null;

        try {
            CLASS_MYSQL_PACKET_TOO_BIG = Class.forName(MYSQL_PACKET_CLASSNAME);
            CLASS_MYSQL_TRANSIENT = Class.forName(MYSQL_TRANSIENT_CLASSNAME);
            CLASS_MYSQL_COMMUNICATIONS = Class.forName(MYSQL_COMMUNICATIONS_CLASSNAME);
        } catch (ClassNotFoundException cnfe) {
            logger.log(Level.INFO, "MySQL class not found ''{0}'', handler not active.", cnfe.getMessage());    
        }

        MYSQL_PACKET_TOO_BIG = CLASS_MYSQL_PACKET_TOO_BIG;
        MYSQL_TRANSIENT = CLASS_MYSQL_TRANSIENT;
        MYSQL_COMMUNICATIONS = CLASS_MYSQL_COMMUNICATIONS;
    }

    private void doMySQLFailure(Throwable throwable) {
        Throwable detail = null;

        if (isSendStackToClient()) {
            detail = ExceptionUtils.textReplace(throwable, true);
        }

        throw new GatewayRuntimeException(detail);        
    }
}
