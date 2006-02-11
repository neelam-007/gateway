/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.common.util;

/**
 * Utility methods for dealing with classes and class names.
 */
public class ClassUtils {
    /**
     * Strips the package name and any enclosing class names from a fully-qualified class name.
     * For example, if passed "java.lang.String",
     * would return "String"; if passed "com.example.Foo.Bar", returns "Bar".
     *
     * @param fullName the fully-qualified class name.  Must not be null or empty.
     * @return the class part only of the name.  Never null, but might be empty if fullName is empty or ends in a dot.
     * @throws NullPointerException if fullName is null
     */
    public static String getClassName(String fullName) {
        if (fullName == null) throw new NullPointerException();
        if (fullName.length() < 2) return fullName;

        int dotpos = fullName.lastIndexOf('.');
        if (dotpos < 0) return fullName;
        return fullName.substring(dotpos + 1);
    }

    /**
     * Strips the package name and any enclosing class names from a fully-qualified class name.
     * For example, if passed "java.lang.String",
     * would return "String"; if passed "com.example.Foo.Bar", returns "Bar".
     *
     * @param clazz the class whose name to extract.  Must not be null.
     * @return the class part only of the name.  Never null, but might be empty if the input class has a pathological name.
     * @throws NullPointerException if clazz is null
     */
    public static String getClassName(Class clazz) {
        return getClassName(clazz.getName());
    }
}
