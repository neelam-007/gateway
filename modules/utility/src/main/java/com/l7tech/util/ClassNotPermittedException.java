package com.l7tech.util;

import org.jetbrains.annotations.NotNull;

/**
 * Class not permitted {@link ClassFilterException}
 */
public class ClassNotPermittedException extends ClassFilterException {
    @NotNull
    private final String className;

    public ClassNotPermittedException(@NotNull final String className) {
        super("Class not permitted as it is not whitelisted: " + className);
        this.className = className;
    }

    public ClassNotPermittedException(final String msg, @NotNull final String className) {
        super(msg);
        this.className = className;
    }

    /**
     * @return the name of the class that was not whitelisted
     */
    @NotNull
    public String getClassName() {
        return className;
    }
}
