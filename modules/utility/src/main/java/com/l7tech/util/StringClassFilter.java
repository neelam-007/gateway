package com.l7tech.util;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

/**
 * A ClassFilter that takes a set of string patterns for classes, methods, and constructors to allow.
 */
public class StringClassFilter implements ClassFilter {
    private static final Logger logger = Logger.getLogger(StringClassFilter.class.getName());

    final Set<String> classes;
    final Set<String> constructors;
    final Set<String> methods;

    public StringClassFilter(@NotNull Set<String> classes, @NotNull Set<String> constructors, @NotNull Set<String> methods) {
        this.classes = new HashSet<String>(classes);
        this.constructors = new HashSet<String>(constructors);
        this.methods = new HashSet<String>(methods);
    }

    @Override
    public boolean permitClass(@NotNull String classname) {
        return classes.contains(classname);
    }

    @Override
    public boolean permitConstructor(@NotNull Constructor constructor) {
        String name = nameForConstructor(constructor);
        if (!constructors.contains(name)) {
            logger.fine("constructor not permitted: " + name);
            return false;
        }
        return true;
    }

    @Override
    public boolean permitMethod(@NotNull Method method) {
        String name = nameForMethod(method);
        if (!methods.contains(name)) {
            logger.fine("method not permitted: " + name);
            return false;
        }
        return true;
    }

    public String nameForMethod(@NotNull Method method) {
        StringBuilder sb = new StringBuilder();
        sb.append(ClassUtils.getJavaTypeName(method.getDeclaringClass()));
        sb.append('.');
        sb.append(method.getName());
        sb.append('(');
        boolean first = true;
        for (Class<?> type : method.getParameterTypes()) {
            if (!first)
                sb.append(',');
            sb.append(ClassUtils.getJavaTypeName(type));
            first = false;
        }
        sb.append(')');
        return sb.toString();
    }

    public String nameForConstructor(@NotNull Constructor constructor) {
        StringBuilder sb = new StringBuilder();
        sb.append(ClassUtils.getJavaTypeName(constructor.getDeclaringClass()));
        sb.append('(');
        boolean first = true;
        for (Class<?> type : constructor.getParameterTypes()) {
            if (!first)
                sb.append(',');
            sb.append(ClassUtils.getJavaTypeName(type));
            first = false;
        }
        sb.append(')');
        return sb.toString();
    }
}
