/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.util;

import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.ArrayList;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;

/**
 * Utility methods for dealing with classes, class names and resources.
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

    /**
     * Strips the class name and returns just the package name from a fully-qualified class name.
     * For example, if passed "java.lang.String",
     * would return "java.lang"; and if passed "MumbleFrotz$Foofy$2$11", returns "".
     *
     * @param fullName  the fully qualified class name whose package to extract.  Required.
     * @return the package name, which may be empty (if this is in the default package) but will never be null.
     */
    public static String getPackageName(String fullName) {
        int di = fullName.lastIndexOf(".");
        if (di < 2)
            return "";
        return fullName.substring(0, di);
    }

    /**
     * Strips the class name and returns just the package name from a fully-qualified class name.
     * For example, if passed "java.lang.String",
     * would return "java.lang"; and if passed "MumbleFrotz$Foofy$2$11", returns "".
     *
     * @param clazz  the class whose package to extract.  Required.
     * @return the package name, which may be empty (if this is in the default package) but will never be null.
     */
    public static String getPackageName(Class clazz) {
        return getPackageName(clazz.getName());
    }

    /**
     * Strip the specified suffix, if the string ends with it.
     *
     * @param name     the string to strip, ie "com.yoyodyne.layer7.assertion"
     * @param suffix   the suffix to strip, ie ".assertion"
     * @return the name with any matching suffix stripped, ie "com.yoyodyne.layer7"
     */
    public static String stripSuffix(String name, String suffix) {
        if (name.endsWith(suffix))
            name = name.length() <= suffix.length() ? "" : name.substring(0, name.length() - suffix.length());
        return name;
    }

    /**
     * Strip the specified prefix, if the string begins with it.
     *
     * @param name     the string to strip, ie "com.yoyodyne.layer7.assertion.composite.grouped"
     * @param prefix   the suffix to strip, ie "com.yoyodyne.layer7.assertion."
     * @return the name with any matching prefix stripped, ie "composite.grouped"
     */
    public static String stripPrefix(String name, String prefix) {
        if (name.startsWith(prefix))
            name = name.length() <= prefix.length() ? "" : name.substring(prefix.length());
        return name;
    }

    /**
     * List the resources contained in the path.
     *
     * <p>WARNING: This should work for JAR / file resources, but will not work
     * in all scenarios.</p>
     *
     * @param baseClass The base class for resource resolution
     * @param resourcePath The path to the resource directory (must be a directory, use a "/")
     * @return The collection of resources (never null)
     */
    public static Collection<URL> listResources( final Class baseClass,
                                                 final String resourcePath ) throws IOException {
        URL resourceBaseUrl = baseClass.getResource( resourcePath );
        List<URL> resourceUrls = new ArrayList<URL>();
        BufferedReader reader = null;
        try {
            reader = new BufferedReader( new InputStreamReader(resourceBaseUrl.openStream()) );
            String name;
            while( (name = reader.readLine()) != null ) {
                resourceUrls.add( new URL(resourceBaseUrl, name) );
            }
        } finally {
            ResourceUtils.closeQuietly( reader );
        }

        return resourceUrls;
    }
}
