package com.l7tech.server.util;

import com.l7tech.test.BugId;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.DataException;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.hibernate3.SessionFactoryUtils;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.SQLException;

/**
 *
 */
public class ConstraintViolationThrowsAdviceTest {

    private static final ThrowingService SERVICE = new ThrowingService();

    /**
     * Expects hibernate exception being replaced by internal exception
     */
    @BugId("DE300397")
    @Test(expected = com.l7tech.objectmodel.ConstraintViolationException.class)
    public void testReplacementByConstraintViolationException () throws Exception {
        this.invokeIntercepted(ReflectionUtils.findMethod(ThrowingService.class,"throwConstraintViolationWithMethodThrows"), SERVICE);
    }

    /**
     * Expects that advice does nothing
     */
    @BugId("DE300397")
    @Test(expected = DataIntegrityViolationException.class)
    public void testNoReplacementByConstraintViolationException () throws Exception {
        this.invokeIntercepted(ReflectionUtils.findMethod(ThrowingService.class,"throwConstraintViolation"), SERVICE);
    }

    /**
     * Expect no 'SQL' text into the message and an internal constraint exception
     */
    @BugId("DE300397")
    @Test(expected = com.l7tech.objectmodel.ConstraintViolationException.class)
    public void testRemoveSQLStatementFromConstraintViolationMessage () throws Exception {
        try {
            this.invokeIntercepted(ReflectionUtils.findMethod(ThrowingService.class,"throwConstraintViolationWithMethodThrows"), SERVICE);
            Assert.fail();
        } catch (Exception e) {
            Assert.assertTrue(!e.getMessage().contains("SQL"));
            throw e;
        }
    }

    /**
     * Expect no 'SQL' text into the messages and spring data violation exception
     */
    @BugId("DE300397")
    @Test(expected = DataIntegrityViolationException.class)
    public void testRemoveSQLStatementFromMessage () throws Exception {
        try {
            this.invokeIntercepted(ReflectionUtils.findMethod(ThrowingService.class,"throwDataIntegrityViolationWithSQL"), SERVICE);
            Assert.fail();
        } catch (Exception e) {
            Assert.assertTrue(!e.getMessage().contains("SQL"));
            throw e;
        }
    }

    private void invokeIntercepted(Method method, Object service) throws Exception {
        try {
            method.invoke(service);
        } catch (InvocationTargetException e) {
            Exception target = (Exception) e.getTargetException();
            if (target instanceof DataIntegrityViolationException) {
                ConstraintViolationThrowsAdvice adv = new ConstraintViolationThrowsAdvice();
                adv.afterThrowing(method, null, service, (DataIntegrityViolationException) target);
            } else {
                throw target;
            }
        }
    }

    private static class ThrowingService {

        public void throwConstraintViolation() {
            ConstraintViolationException cve = new ConstraintViolationException("constraint violated", null, "insert into table (?, ?)", "any_constraint");
            throw SessionFactoryUtils.convertHibernateAccessException(cve);
        }

        public void throwConstraintViolationWithMethodThrows() throws com.l7tech.objectmodel.ConstraintViolationException {
            throwConstraintViolation();
        }

        public void throwDataIntegrityViolationWithSQL() {
            SQLException sqle = new SQLException("error inserting values", "42", 42);
            DataException de = new DataException(sqle.getMessage(), sqle, "insert into table (?,?)");
            throw SessionFactoryUtils.convertHibernateAccessException(de);
        }

    }
}
