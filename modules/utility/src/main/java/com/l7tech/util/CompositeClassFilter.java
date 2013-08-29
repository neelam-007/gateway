package com.l7tech.util;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

/**
 * A ClassFilter that succeeds if any one of its delegate class filters permit the specified class,
 * constructor, or method.
 */
public class CompositeClassFilter implements ClassFilter {

    private final List<ClassFilter> delegates;

    /**
     * Create a CompositeClassFilter that delegates to the specified delegates.
     *
     * @param delegates class filters to which to delegate queries.
     */
    public CompositeClassFilter(ClassFilter... delegates) {
        if (delegates.length < 1)
            throw new IllegalArgumentException("At least one delegate must be specified.");
        for (ClassFilter delegate : delegates) {
            if (delegate == null)
                throw new NullPointerException("None of the delegates may be null");
        }
        this.delegates = Arrays.asList(delegates);
    }

    @Override
    public boolean permitClass(@NotNull String classname) {
        for (ClassFilter delegate : delegates) {
            if (delegate.permitClass(classname))
                return true;
        }
        return false;
    }

    @Override
    public boolean permitConstructor(@NotNull Constructor constructor) {
        for (ClassFilter delegate : delegates) {
            if (delegate.permitConstructor(constructor))
                return true;
        }
        return false;
    }

    @Override
    public boolean permitMethod(@NotNull Method method) {
        for (ClassFilter delegate : delegates) {
            if (delegate.permitMethod(method))
                return true;
        }
        return false;
    }
}
