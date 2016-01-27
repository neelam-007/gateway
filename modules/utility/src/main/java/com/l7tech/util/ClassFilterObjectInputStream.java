package com.l7tech.util;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.util.logging.Logger;

/**
 * Use this object input stream to add class filtering when deserializing objects.<br/>
 * Should be a replacement for {@link java.io.ObjectInputStream}.
 */
public class ClassFilterObjectInputStream extends ObjectInputStream {
    private static final Logger logger = Logger.getLogger(ClassFilterObjectInputStream.class.getName());

    @NotNull
    private final ClassFilter classFilter;

    public ClassFilterObjectInputStream(final InputStream in, @NotNull final ClassFilter classFilter) throws IOException {
        super(in);
        this.classFilter = classFilter;
    }

    @NotNull
    public ClassFilter getClassFilter() {
        return classFilter;
    }

    @Override
    protected Class<?> resolveClass(final ObjectStreamClass classDesc) throws IOException, ClassNotFoundException {
        if (getClassFilter().permitClass(classDesc.getName())) {
            return super.resolveClass(classDesc);
        }

        logger.warning("Attempt to load restricted class '" + classDesc.getName() + "'.");
        throw new ClassNotFoundException(classDesc.getName());
    }
}
