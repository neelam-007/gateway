package com.l7tech.server.util;

import java.lang.reflect.Method;
import java.util.logging.Logger;
import java.util.logging.Level;

import com.mysql.jdbc.exceptions.MySQLTransientException;
import com.l7tech.admin.GatewayRuntimeException;
import com.l7tech.common.util.ExceptionUtils;

/**
 * @author: ghuang
 */
public class MySqlFailureThrowsAdvice extends ThrowsAdviceSupport {
    private static final Logger logger = Logger.getLogger(MySqlFailureThrowsAdvice.class.getName());

    public void afterThrowing(final Method method,
                              final Object[] args,
                              final Object target,
                              final Throwable throwable) throws GatewayRuntimeException {
        if (ExceptionUtils.causedBy(throwable, MySQLTransientException.class)) {
            if (logger.isLoggable(Level.WARNING)) {
                logger.log(Level.WARNING, "MySQLTransientException occurs.", throwable);
            }

            Throwable detail = null;
            if (isSendStackToClient()) {
                detail = ExceptionUtils.textReplace(throwable, true);
            }
            
            throw new GatewayRuntimeException(detail);
        }
    }
}
