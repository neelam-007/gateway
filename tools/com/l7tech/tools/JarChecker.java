/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.tools;

import java.io.IOException;
import java.util.List;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

/**
 * Using the current VM's class path, attempt to load every class in the specified jarfile.
 *
 * @author mike
 * @version 1.0
 */
public class JarChecker {
    private static final String SYSPROP_FAIL_ON_PACKAGE = JarChecker.class.getName() + ".failpackages";

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: JarChecker jarfile.jar [ignoreclass]*");
            System.exit(1);
        }

        String jar = args[0];
        String[] ignoreClassNames = new String[args.length-1];
        String[] failurePackageNames = System.getProperty(SYSPROP_FAIL_ON_PACKAGE, "").split(" ");
        System.arraycopy(args, 1, ignoreClassNames, 0, args.length-1);
        try {
            if (!checkPackages(jar, failurePackageNames)) {
                System.out.println("\nERROR: Jar " + jar + " contains unexpected package.");
                System.exit(1);
            }
            if (!checkJar(jar, ignoreClassNames)) {
                System.out.println("\nERROR: There was an error loading at least one class in " + jar);
                System.exit(1);
            }
        } catch (Throwable e) {
            fatalErr(e);
        }

        System.out.println("All classes in " + jar + " are confirmed to be loadable with the current classpath.");
        System.exit(0);
    }

    private static class ClassLoadingException extends Exception {
        public ClassLoadingException(String mess, Throwable cause) {
            super(mess, cause);
        }
    }

    private static boolean checkPackages(String jar, String[] failurePackageNames) throws IOException {
        List errors = new ArrayList();
        List toIgnore = Arrays.asList(failurePackageNames);
        JarFile jarFile = new JarFile(jar);
        Enumeration entries = jarFile.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = (ZipEntry) entries.nextElement();
            String name = entry.getName();
            if (name.endsWith(".class")) {
                String truncName = name.substring(0, name.length() - ".class".length());
                String className = truncName.replace('/', '.').replace('\\', '.');

                Iterator ignorePackageIter = toIgnore.iterator();
                while (ignorePackageIter.hasNext()) {
                    String packageName = (String) ignorePackageIter.next();
                    if (packageName.trim().length() > 0 && className.startsWith(packageName)) {
                        errors.add(className);    
                    }
                }
            }
        }

        if (!errors.isEmpty()) {
            for(java.util.Iterator i = errors.iterator(); i.hasNext(); ) {
                System.out.println("Found class in excluded package: " + i.next());
            }
            return false;
        }

        return true;
    }

    private static boolean checkJar(String jar, String[] ignoreClassNames) throws IOException {
        List errors = new ArrayList();
        List toIgnore = Arrays.asList(ignoreClassNames);
        JarFile jarFile = new JarFile(jar);
        Enumeration entries = jarFile.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = (ZipEntry) entries.nextElement();
            String name = entry.getName();
            if (name.endsWith(".class")) {
                String truncName = name.substring(0, name.length() - ".class".length());
                String className = truncName.replace('/', '.').replace('\\', '.');
                if(toIgnore.contains(className)) {
                    log("Skipping class: " + className);
                }                
                else {
                    //log("Loading class: " + className);
                    try {
                        Class.forName(className);
                    } catch (Throwable e) {
                        final String mess = "ERROR: while loading class: " + className + ": " + e;
                        System.out.println("\n" + mess + "\n");
                        errors.add(new ClassLoadingException(mess, e));
                    }
                }
            }
        }

        if (!errors.isEmpty()) {
            for(java.util.Iterator i = errors.iterator(); i.hasNext(); ) {
                ClassLoadingException c = (ClassLoadingException)i.next();
                System.out.println("\n" + c.getMessage());
                c.printStackTrace(System.out);
            }
            return false;
        }

        return true;
    }

    private static void log(String s) {
        // Uncomment to log some stuff
        System.out.println(s);
    }

    private static void fatalErr(Throwable e) {
        System.out.println("\nERROR: Fatal error: " + e.getMessage());
        e.printStackTrace(System.out);
        System.exit(2);
    }
}
