package com.l7tech.server.util;

import java.util.logging.Logger;
import java.util.logging.Level;
import java.lang.reflect.Method;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.aop.ThrowsAdvice;

import com.l7tech.util.ArrayUtils;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.objectmodel.ConstraintViolationException;

/**
 * Advice that translates integrity constraint violations into client exceptions.
 *
 * <p>If a {@link DataIntegrityViolationException} is thrown from Spring and the
 * invoked method permits {@link ConstraintViolationException} to be thrown then
 * this advice will translate the exception.</p>
 *
 * @author Steve Jones
 */
public class ConstraintViolationThrowsAdvice implements ThrowsAdvice {

    //- PUBLIC

    public void afterThrowing(final Method method,
                              final Object[] args,
                              final Object target,
                              final DataIntegrityViolationException dive) throws ConstraintViolationException {
        Class[] canThrow = method.getExceptionTypes();
        if ( ArrayUtils.contains(canThrow, ConstraintViolationException.class) ) {
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE,
                        "Filtering DataIntegrityViolationException as ConstraintViolationException",
                        ExceptionUtils.getDebugException(dive));
            }

            //
            throw new ConstraintViolationException(dive.getMessage());            
        }
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(ConstraintViolationThrowsAdvice.class.getName());
}
