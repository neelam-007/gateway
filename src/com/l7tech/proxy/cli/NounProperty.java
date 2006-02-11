/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.proxy.cli;

import java.io.PrintStream;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

/**
 * Represents a configurable property of a configuration noun.
 * Simple properties will just use this class, and set and get with reflection.
 * More complex properties will subclass NounProperty for more complex behavior.
 */
class NounProperty extends Word {
    private final String methodName;
    private final Object targetObject;
    private Method accessor;

    public NounProperty(Object targetObject, String name, String methodName, String desc) {
        super(name, desc);
        this.targetObject = targetObject;
        this.methodName = methodName;
    }

    /**
     * Print the human-displayed value of this property.
     *
     * @param out          the PrintStream to print to.  Must not be null.
     * @param singleLine   true if the display needs to be very short, a single line at most
     */
    public void printValue(PrintStream out, boolean singleLine) {
        out.print(getValue());
        if (!singleLine) out.println();
    }

    /**
     * Attempt to get the accessor method for our target object.
     *
     * @return the accessor method.  Never null.
     * @throws RuntimeException if an accessor method couldn't be found.
     */
    protected Method getAccessor() {
        if (accessor == null) {
            try {
                try {
                    accessor = targetObject.getClass().getMethod("get" + methodName, new Class[0]);
                } catch (NoSuchMethodException e) {
                    accessor = targetObject.getClass().getMethod("is" + methodName, new Class[0]);
                }
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e); // can't happen
            }
        }
        return accessor;
    }

    /**
     * Attempt to get the value by invoking our accessor method on our target object.
     *
     * @return the Object returned from calling our accessor on our target object, which may be null.
     * @throws RuntimeException if the accessor throws an exception.
     */
    protected Object getValue() {
        try {
            return getAccessor().invoke(targetObject, new Object[0]);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e); // can't happen
        } catch (InvocationTargetException e) {
            throw new RuntimeException("Unable to read property " + getName() + ": " + e.getMessage(), e);
        }
    }
}
