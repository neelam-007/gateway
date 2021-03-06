package com.l7tech.server.util;

import java.lang.reflect.Method;
import java.util.logging.Logger;
import java.util.logging.Level;

import com.l7tech.gateway.common.admin.GatewayRuntimeException;
import com.l7tech.util.ExceptionUtils;
import com.mchange.v2.resourcepool.CannotAcquireResourceException;

import org.hibernate.exception.JDBCConnectionException;

/**
 * Handle exceptions due to MySQL not being available.
 *
 * @author: ghuang
 */
public class MySqlFailureThrowsAdvice extends ThrowsAdviceSupport {

    //- PUBLIC

    public void afterThrowing( final Throwable throwable ) throws GatewayRuntimeException {
        if ( isMySqlFailure( throwable ) ) {
            doMySQLFailure(throwable);
        }
    }

    //- PACKAGE

    static boolean isMySqlFailure( final Throwable throwable ) {
        boolean mysql = false;

        if ( ExceptionUtils.causedBy(throwable, CannotAcquireResourceException.class) ) {
            mysql = true;
        } else if (MYSQL_COMMUNICATIONS != null) {
            if ( !ExceptionUtils.causedBy(throwable, MYSQL_PACKET_TOO_BIG) &&
                 ExceptionUtils.causedBy(throwable, MYSQL_COMMUNICATIONS)) {
                // handle communication failure if not caused by large packet,
                // if there is a JDBCConnectionException check the SQLState.
                JDBCConnectionException jce = ExceptionUtils.getCauseIfCausedBy(throwable, JDBCConnectionException.class);
                if (jce == null || CONNECT_FAILURE_STATE.equals(jce.getSQLState())) {
                    mysql = true;
                }
            } else if (ExceptionUtils.causedBy(throwable, MYSQL_TRANSIENT)) {
                // handle any transient failure
                mysql = true;
            }
        }

        return mysql;
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

        // Even though a DB failure is not really transient we want to tell the user to "try again"
        throw new GatewayRuntimeException( GatewayRuntimeException.MESSAGE_TRANSIENT, detail);        
    }
}
