package com.l7tech.server.util;

import java.util.logging.Logger;
import java.util.logging.Level;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.dao.DataIntegrityViolationException;

import com.l7tech.common.util.ArrayUtils;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.objectmodel.ConstraintViolationException;

/**
 * MethodInterceptor that translates server exceptions to client exceptions.
 * 
 * @author Steve Jones
 */
public class ExceptionFilteringInterceptor implements MethodInterceptor {

    //- PUBLIC

    public Object invoke(MethodInvocation methodInvocation) throws Throwable {
        try {
            return methodInvocation.proceed();
        } catch (DataIntegrityViolationException dive) {
            Class[] canThrow = methodInvocation.getMethod().getExceptionTypes();
            if ( ArrayUtils.contains(canThrow, ConstraintViolationException.class) ) {
                if (logger.isLoggable(Level.FINE)) {
                    logger.log(Level.FINE,
                            "Filtering DataIntegrityViolationException as ConstraintViolationException",
                            ExceptionUtils.getDebugException(dive));
                }
                throw new ConstraintViolationException(dive.getMessage());
            }

            throw dive;
        }
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(ExceptionFilteringInterceptor.class.getName());
}
