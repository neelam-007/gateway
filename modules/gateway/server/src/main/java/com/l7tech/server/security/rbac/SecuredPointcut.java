/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.server.security.rbac;

import com.l7tech.gateway.common.security.rbac.Secured;
import org.springframework.aop.ClassFilter;
import org.springframework.aop.MethodMatcher;
import org.springframework.aop.Pointcut;

import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Provides a {@link ClassFilter} and {@link MethodMatcher} for detecting use of the {@link Secured}
 * annotation.
 */
public class SecuredPointcut implements Pointcut {
    private static final Logger logger = Logger.getLogger(SecuredPointcut.class.getName());

    public SecuredPointcut() {
        logger.info(getClass().getName() + " initialized");
    }

    public ClassFilter getClassFilter() {
        return CLASS_FILTER;
    }

    public MethodMatcher getMethodMatcher() {
        return METHOD_MATCHER;
    }

    private static final SecuredClassFilter CLASS_FILTER = new SecuredClassFilter();
    private static final SecuredMethodMatcher METHOD_MATCHER = new SecuredMethodMatcher();

    /**
     * Matches any class with the {@link com.l7tech.gateway.common.security.rbac.Secured} annotation.
     * Methods must also be annotated with {@link Secured}.
     */
    private static class SecuredClassFilter implements ClassFilter {
        @SuppressWarnings({"unchecked"})
        public boolean matches(Class clazz) {
            if (clazz.getAnnotation(Secured.class) != null) {
                logger.log(Level.FINE, "Security declaration found in class {0}", clazz.getName());
                return true;
            }

            for (Class intf : clazz.getInterfaces()) {
                if (intf.getAnnotation(Secured.class) != null) {
                    logger.log(Level.FINE, "Security declaration found in interface {0}", intf.getName());
                    return true;
                } else for (Class superIntf : intf.getInterfaces()) {
                    if (superIntf.getAnnotation(Secured.class) != null) {
                        logger.log(Level.FINE, "Security declaration found in interface {0}", superIntf.getName());
                        return true;
                    }
                }
            }

            throw new IllegalArgumentException("No security declaration found for class " + clazz.getName());
        }
    }

    /**
     * Matches any method that directly annotated with {@link Secured}.  The class must also be annotated
     * with {@link Secured}.
     */
    private static class SecuredMethodMatcher implements MethodMatcher {
        public boolean matches(Method method, Class targetClass) {
            if (method.getAnnotation(Secured.class) != null) {
                logger.log(Level.FINE, "Security declaration found in method {0}.{1}", new Object[] { method.getDeclaringClass().getSimpleName(), method.getName() });                return true;
            } else {
                logger.log(Level.FINE, "No security declaration found for method {0}.{1}", new Object[] { method.getDeclaringClass().getSimpleName(), method.getName() });
                return false;
            }
        }

        public boolean isRuntime() {
            return false;
        }

        public boolean matches(Method method, Class targetClass, Object[] args) {
            return true;
        }
    }

}
