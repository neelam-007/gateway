/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.server.security.rbac;

import org.springframework.aop.Pointcut;
import org.springframework.aop.ClassFilter;
import org.springframework.aop.MethodMatcher;
import com.l7tech.common.security.rbac.Secured;

import java.lang.reflect.Method;
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
     * Matches any class with the {@link com.l7tech.common.security.rbac.Secured} annotation.
     * Methods must also be annotated with {@link Secured}.
     */
    private static class SecuredClassFilter implements ClassFilter {
        @SuppressWarnings({"unchecked"})
        public boolean matches(Class clazz) {
            if (clazz.getAnnotation(Secured.class) != null) return true;
            for (Class intf : clazz.getInterfaces()) {
                if (intf.getAnnotation(Secured.class) != null) return true;
            }
            return false;
        }
    }

    /**
     * Matches any method that directly annotated with {@link Secured}.  The class must also be annotated
     * with {@link Secured}.
     */
    private static class SecuredMethodMatcher implements MethodMatcher {
        public boolean matches(Method method, Class targetClass) {
            return method.getAnnotation(Secured.class) != null;
        }

        public boolean isRuntime() {
            return false;
        }

        public boolean matches(Method method, Class targetClass, Object[] args) {
            return true;
        }
    }

}
