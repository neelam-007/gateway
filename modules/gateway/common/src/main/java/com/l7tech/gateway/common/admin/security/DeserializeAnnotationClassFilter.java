package com.l7tech.gateway.common.admin.security;

import com.l7tech.util.AnnotationClassFilter;
import com.l7tech.util.DeserializeSafe;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Collection;

/**
 * {@link AnnotationClassFilter} handling {@link DeserializeSafe}
 */
public final class DeserializeAnnotationClassFilter extends AnnotationClassFilter {
    /**
     * Create an AnnotationClassFilter that will use the specified class loader for loading classes
     * to check their annotations, only loading classes whose names start with one of the specified
     * prefixes.
     *
     * @param classPrefixes prefixes of classes to load and check annotations on.  Required.
     */
    public DeserializeAnnotationClassFilter(final @NotNull Collection<String> classPrefixes) {
        super(null, classPrefixes);
    }

    @Override
    protected boolean permitClass(@NotNull final Class<?> clazz) {
        final DeserializeSafe classSafety = clazz.getAnnotation(DeserializeSafe.class);
        if (classSafety != null)
            return classSafety.safe();
        return false;
    }

    @Override
    public boolean permitConstructor(@NotNull final Constructor<?> constructor) {
        // don't care about constructors
        return false;
    }

    @Override
    public boolean permitMethod(@NotNull final Method method) {
        // don't care about methods
        return false;
    }
}
