/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.spring.remoting.rmi.codebase;


import sun.security.action.GetPropertyAction;

import java.net.MalformedURLException;
import java.rmi.server.RMIClassLoader;
import java.rmi.server.RMIClassLoaderSpi;
import java.util.logging.Logger;
import java.util.Collection;
import java.util.ArrayList;

/**
 * The <code>ClassSetAnnotation</code> is the implementation of the
 * {@link java.rmi.server.RMIClassLoaderSpi} service that allows
 * excluuded and included classes patterns for class annotation.
 * On match the classes are annotated with the  value of "java.rmi.server.codebase"
 * property.
 * <p/>
 *
 * @author emil
 * @version Aug 31, 2004
 * @see sun.misc.Service
 */
public final class ClassSetAnnotation extends RMIClassLoaderSpi {
    /**
     * "default" provider instance
     */
    private static final RMIClassLoaderSpi defaultProvider = RMIClassLoader.getDefaultProviderInstance();
    /**
     * value of "java.rmi.server.codebase" property, as cached at class
     * initialization time.  It may contain malformed URLs.
     */
    private static String codebaseProperty = null;
    static {
	String prop = (String) java.security.AccessController.doPrivileged(
            new GetPropertyAction("java.rmi.server.codebase"));
	if (prop != null && prop.trim().length() > 0) {
	    codebaseProperty = prop;
	}
    }

    /**
     * Include patterns, may include ?, *, **
     */
    private static Collection includePatterns = new ArrayList();
    /**
     * Exclude patterns, may include ?, *, **
     */
    private static Collection excludePatterns = new ArrayList();
    /**
     * logger
     */
    private static final Logger logger = Logger.getLogger(ClassSetAnnotation.class.getName());

    /**
     * Returns the annotation string (representing a location for the class definition) that
     * RMI will use to annotate the class descriptor when marshalling objects of the given class.
     *
     * @param cl the class to obtain the annotation for
     * @return a string to be used to annotate the given class when it gets marshalled, or null
     */
    public String getClassAnnotation(Class cl) {
        String name = cl.getName();
        int nameLength = name.length();
        if (nameLength > 0 && name.charAt(0) == '[') {
            // skip past all '[' characters (see bugid 4211906)
            int i = 1;
            while (nameLength > i && name.charAt(i) == '[') {
                i++;
            }
            if (nameLength > i && name.charAt(i) != 'L') {
                return null;
            }
        }
        String annotation = null;
        return annotation;
    }

    /**
     * Provides the implementation for
     * {@link java.rmi.server.RMIClassLoader#getClassLoader(String)}.
     * <p/>
     * Returns a class loader that loads classes from the given codebase
     * URL path.
     * <p/>
     * <p>If there is a security manger, its <code>checkPermission</code>
     * method will be invoked with a
     * <code>RuntimePermission("getClassLoader")</code> permission;
     * this could result in a <code>SecurityException</code>.
     * The implementation of this method may also perform further security
     * checks to verify that the calling context has permission to connect
     * to all of the URLs in the codebase URL path.
     *
     * @return a class loader that loads classes from the given codebase URL
     *         path
     * @param	codebase the list of URLs (space-separated) from which
     * the returned class loader will load classes from, or <code>null</code>
     * @throws	java.net.MalformedURLException if <code>codebase</code> is
     * non-<code>null</code> and contains an invalid URL, or
     * if <code>codebase</code> is <code>null</code> and the system
     * property <code>java.rmi.server.codebase</code> contains an
     * invalid URL
     * @throws	SecurityException if there is a security manager and the
     * invocation of its <code>checkPermission</code> method fails, or
     * if the caller does not have permission to connect to all of the
     * URLs in the codebase URL path
     */
    public ClassLoader getClassLoader(String codebase)
      throws MalformedURLException {
        return defaultProvider.getClassLoader(codebase);
    }

    /**
     * Provides the implementation for
     * {@link java.rmi.server.RMIClassLoader#loadClass(java.net.URL,String)},
     * {@link java.rmi.server.RMIClassLoader#loadClass(String,String)}, and
     * {@link java.rmi.server.RMIClassLoader#loadClass(String,String,ClassLoader)}.
     * <p/>
     * Loads a class from a codebase URL path, optionally using the
     * supplied loader.
     * <p/>
     * Typically, a provider implementation will attempt to
     * resolve the named class using the given <code>defaultLoader</code>,
     * if specified, before attempting to resolve the class from the
     * codebase URL path.
     * <p/>
     * <p>An implementation of this method must either return a class
     * with the given name or throw an exception.
     *
     * @param	codebase the list of URLs (separated by spaces) to load
     * the class from, or <code>null</code>
     * @param	name the name of the class to load
     * @param	defaultLoader additional contextual class loader
     * to use, or <code>null</code>
     * @return	the <code>Class</code> object representing the loaded class
     * @throws	java.net.MalformedURLException if <code>codebase</code> is
     * non-<code>null</code> and contains an invalid URL, or
     * if <code>codebase</code> is <code>null</code> and the system
     * property <code>java.rmi.server.codebase</code> contains an
     * invalid URL
     * @throws	ClassNotFoundException if a definition for the class
     * could not be found at the specified location
     */
    public Class loadClass(String codebase, String name, ClassLoader defaultLoader)
      throws MalformedURLException, ClassNotFoundException {
        return defaultProvider.loadClass(codebase, name, defaultLoader);
    }

    /**
     * Provides the implementation for
     * {@link java.rmi.server.RMIClassLoader#loadProxyClass(String,String[],ClassLoader)}.
     * <p/>
     * Loads a dynamic proxy class (see {@link java.lang.reflect.Proxy}
     * that implements a set of interfaces with the given names
     * from a codebase URL path, optionally using the supplied loader.
     * <p/>
     * <p>An implementation of this method must either return a proxy
     * class that implements the named interfaces or throw an exception.
     *
     * @param	codebase the list of URLs (space-separated) to load
     * classes from, or <code>null</code>
     * @param	interfaces the names of the interfaces for the proxy class
     * to implement
     * @return	a dynamic proxy class that implements the named interfaces
     * @param	defaultLoader additional contextual class loader
     * to use, or <code>null</code>
     * @throws	java.net.MalformedURLException if <code>codebase</code> is
     * non-<code>null</code> and contains an invalid URL, or
     * if <code>codebase</code> is <code>null</code> and the system
     * property <code>java.rmi.server.codebase</code> contains an
     * invalid URL
     * @throws	ClassNotFoundException if a definition for one of
     * the named interfaces could not be found at the specified location,
     * or if creation of the dynamic proxy class failed (such as if
     * {@link java.lang.reflect.Proxy#getProxyClass(ClassLoader,Class[])}
     * would throw an <code>IllegalArgumentException</code> for the given
     * interface list)
     */
    public Class loadProxyClass(String codebase, String[] interfaces, ClassLoader defaultLoader)
      throws MalformedURLException, ClassNotFoundException {
        return defaultProvider.loadProxyClass(codebase, interfaces, defaultLoader);
    }

    /**
     * Tests whether or not a given class matches a given pattern.
     *
     * @param pattern The pattern to match against. Must not be
     *                <code>null</code>.
     * @param className     The class to match, as a String. Must not be
     *                <code>null</code>.
     * @return <code>true</code> if the pattern matches against the string,
     *         or <code>false</code> otherwise.
     */
    private static boolean matchClassName(String pattern, String className) {

        String[] patDirs = tokenizeClassNameAsArray(pattern);
        String[] classNameDirs = tokenizeClassNameAsArray(className);

        int patIdxStart = 0;
        int patIdxEnd = patDirs.length - 1;
        int strIdxStart = 0;
        int strIdxEnd = classNameDirs.length - 1;

        // up to first '**'
        while (patIdxStart <= patIdxEnd && strIdxStart <= strIdxEnd) {
            String patDir = patDirs[patIdxStart];
            if (patDir.equals("**")) {
                break;
            }
            if (!match(patDir, classNameDirs[strIdxStart])) {
                patDirs = null;
                classNameDirs = null;
                return false;
            }
            patIdxStart++;
            strIdxStart++;
        }
        if (strIdxStart > strIdxEnd) {
            // String is exhausted
            for (int i = patIdxStart; i <= patIdxEnd; i++) {
                if (!patDirs[i].equals("**")) {
                    patDirs = null;
                    classNameDirs = null;
                    return false;
                }
            }
            return true;
        } else {
            if (patIdxStart > patIdxEnd) {
                // String not exhausted, but pattern is. Failure.
                patDirs = null;
                classNameDirs = null;
                return false;
            }
        }

        // up to last '**'
        while (patIdxStart <= patIdxEnd && strIdxStart <= strIdxEnd) {
            String patDir = patDirs[patIdxEnd];
            if (patDir.equals("**")) {
                break;
            }
            if (!match(patDir, classNameDirs[strIdxEnd])) {
                patDirs = null;
                classNameDirs = null;
                return false;
            }
            patIdxEnd--;
            strIdxEnd--;
        }
        if (strIdxStart > strIdxEnd) {
            // String is exhausted
            for (int i = patIdxStart; i <= patIdxEnd; i++) {
                if (!patDirs[i].equals("**")) {
                    patDirs = null;
                    classNameDirs = null;
                    return false;
                }
            }
            return true;
        }

        while (patIdxStart != patIdxEnd && strIdxStart <= strIdxEnd) {
            int patIdxTmp = -1;
            for (int i = patIdxStart + 1; i <= patIdxEnd; i++) {
                if (patDirs[i].equals("**")) {
                    patIdxTmp = i;
                    break;
                }
            }
            if (patIdxTmp == patIdxStart + 1) {
                // '**/**' situation, so skip one
                patIdxStart++;
                continue;
            }
            // Find the pattern between padIdxStart & padIdxTmp in className between
            // strIdxStart & strIdxEnd
            int patLength = (patIdxTmp - patIdxStart - 1);
            int strLength = (strIdxEnd - strIdxStart + 1);
            int foundIdx = -1;
            strLoop:
            for (int i = 0; i <= strLength - patLength; i++) {
                for (int j = 0; j < patLength; j++) {
                    String subPat = patDirs[patIdxStart + j + 1];
                    String subStr = classNameDirs[strIdxStart + i + j];
                    if (!match(subPat, subStr)) {
                        continue strLoop;
                    }
                }

                foundIdx = strIdxStart + i;
                break;
            }

            if (foundIdx == -1) {
                patDirs = null;
                classNameDirs = null;
                return false;
            }

            patIdxStart = patIdxTmp;
            strIdxStart = foundIdx + patLength;
        }

        for (int i = patIdxStart; i <= patIdxEnd; i++) {
            if (!patDirs[i].equals("**")) {
                patDirs = null;
                classNameDirs = null;
                return false;
            }
        }

        return true;
    }


    /**
     * Tests whether or not a string matches against a pattern.
     * The pattern may contain two special characters:<br>
     * '*' means zero or more characters<br>
     * '?' means one and only one character
     *
     * @param pattern The pattern to match against.
     *                Must not be <code>null</code>.
     * @param str     The string which must be matched against the pattern.
     *                Must not be <code>null</code>.
     * @return <code>true</code> if the string matches against the pattern,
     *         or <code>false</code> otherwise.
     */
    private static boolean match(String pattern, String str) {
        char[] patArr = pattern.toCharArray();
        char[] strArr = str.toCharArray();
        int patIdxStart = 0;
        int patIdxEnd = patArr.length - 1;
        int strIdxStart = 0;
        int strIdxEnd = strArr.length - 1;
        char ch;

        boolean containsStar = false;
        for (int i = 0; i < patArr.length; i++) {
            if (patArr[i] == '*') {
                containsStar = true;
                break;
            }
        }

        if (!containsStar) {
            // No '*'s, so we make a shortcut
            if (patIdxEnd != strIdxEnd) {
                return false; // Pattern and string do not have the same size
            }
            for (int i = 0; i <= patIdxEnd; i++) {
                ch = patArr[i];
                if (ch != '?') {
                    if (ch != strArr[i]) {
                        return false; // Character mismatch
                    }
                }
            }
            return true; // String matches against pattern
        }

        if (patIdxEnd == 0) {
            return true; // Pattern contains only '*', which matches anything
        }

        // Process characters before first star
        while ((ch = patArr[patIdxStart]) != '*' && strIdxStart <= strIdxEnd) {
            if (ch != '?') {
                if (ch != strArr[strIdxStart]) {
                    return false; // Character mismatch
                }
            }
            patIdxStart++;
            strIdxStart++;
        }
        if (strIdxStart > strIdxEnd) {
            // All characters in the string are used. Check if only '*'s are
            // left in the pattern. If so, we succeeded. Otherwise failure.
            for (int i = patIdxStart; i <= patIdxEnd; i++) {
                if (patArr[i] != '*') {
                    return false;
                }
            }
            return true;
        }

        // Process characters after last star
        while ((ch = patArr[patIdxEnd]) != '*' && strIdxStart <= strIdxEnd) {
            if (ch != '?') {
                if (ch != strArr[strIdxEnd]) {
                    return false; // Character mismatch
                }
            }
            patIdxEnd--;
            strIdxEnd--;
        }
        if (strIdxStart > strIdxEnd) {
            // All characters in the string are used. Check if only '*'s are
            // left in the pattern. If so, we succeeded. Otherwise failure.
            for (int i = patIdxStart; i <= patIdxEnd; i++) {
                if (patArr[i] != '*') {
                    return false;
                }
            }
            return true;
        }

        // process pattern between stars. padIdxStart and patIdxEnd point
        // always to a '*'.
        while (patIdxStart != patIdxEnd && strIdxStart <= strIdxEnd) {
            int patIdxTmp = -1;
            for (int i = patIdxStart + 1; i <= patIdxEnd; i++) {
                if (patArr[i] == '*') {
                    patIdxTmp = i;
                    break;
                }
            }
            if (patIdxTmp == patIdxStart + 1) {
                // Two stars next to each other, skip the first one.
                patIdxStart++;
                continue;
            }
            // Find the pattern between padIdxStart & padIdxTmp in str between
            // strIdxStart & strIdxEnd
            int patLength = (patIdxTmp - patIdxStart - 1);
            int strLength = (strIdxEnd - strIdxStart + 1);
            int foundIdx = -1;
            strLoop:
            for (int i = 0; i <= strLength - patLength; i++) {
                for (int j = 0; j < patLength; j++) {
                    ch = patArr[patIdxStart + j + 1];
                    if (ch != '?') {
                        if (ch != strArr[strIdxStart + i + j]) {
                            continue strLoop;
                        }
                    }
                }

                foundIdx = strIdxStart + i;
                break;
            }

            if (foundIdx == -1) {
                return false;
            }

            patIdxStart = patIdxTmp;
            strIdxStart = foundIdx + patLength;
        }

        // All characters in the string are used. Check if only '*'s are left
        // in the pattern. If so, we succeeded. Otherwise failure.
        for (int i = patIdxStart; i <= patIdxEnd; i++) {
            if (patArr[i] != '*') {
                return false;
            }
        }
        return true;
    }


    private static String[] tokenizeClassNameAsArray(String className) {
        char sep = '.';
        int start = 0;
        int len = className.length();
        int count = 0;
        for (int pos = 0; pos < len; pos++) {
            if (className.charAt(pos) == sep) {
                if (pos != start) {
                    count++;
                }
                start = pos + 1;
            }
        }
        if (len != start) {
            count++;
        }
        String[] l = new String[count];
        count = 0;
        start = 0;
        for (int pos = 0; pos < len; pos++) {
            if (className.charAt(pos) == sep) {
                if (pos != start) {
                    String tok = className.substring(start, pos);
                    l[count++] = tok;
                }
                start = pos + 1;
            }
        }
        if (len != start) {
            String tok = className.substring(start);
            l[count/*++*/] = tok;
        }
        return l;
    }


}