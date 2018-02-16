package com.l7tech.server.util;

import com.l7tech.objectmodel.ConstraintViolationException;
import com.l7tech.util.ArrayUtils;
import com.l7tech.util.ExceptionUtils;
import org.springframework.aop.ThrowsAdvice;
import org.springframework.dao.DataIntegrityViolationException;

import java.lang.reflect.Method;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Advice that translates integrity constraint violations into client exceptions.
 * <p>
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
        if (ArrayUtils.contains(canThrow, ConstraintViolationException.class)) {
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE,
                        "Filtering DataIntegrityViolationException as ConstraintViolationException",
                        ExceptionUtils.getDebugException(dive));
            }

            // Try to reach the original exception if available
            // Constraint violations are also translated by spring and the SQL is added to the message.
            org.hibernate.exception.ConstraintViolationException cve = ExceptionUtils.getCauseIfCausedBy(dive, org.hibernate.exception.ConstraintViolationException.class);
            if (cve != null) {
                throw new ConstraintViolationException(cve.getMessage());
            }
        }

        // If the exception is not a constraint violation we still modify it because it may have a SQL statement into the message.
        logger.log(Level.WARNING, ExceptionUtils.getDebugException(dive), new Supplier<String>() {
            @Override
            public String get() {
                return "Filtering DataIntegrityViolationException and removing SQL statement from the message: " + ExceptionUtils.getMessage(dive);
            }
        });
        throw new DataIntegrityViolationException("Database exception occurred due to data integrity violation.");

        // see SessionFactoryUtils#convertHibernateAccessException for the messages
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(ConstraintViolationThrowsAdvice.class.getName());
}
