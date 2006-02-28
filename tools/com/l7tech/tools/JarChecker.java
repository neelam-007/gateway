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
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

/**
 * Using the current VM's class path, attempt to load every class in the specified jarfile.
 *
 * @author mike
 * @version 1.0
 */
public class JarChecker {
    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: JarChecker jarfile.jar [ignoreclass]*");
            System.exit(1);
        }

        String jar = args[0];
        String[] ignoreClassNames = new String[args.length-1];
        System.arraycopy(args, 1, ignoreClassNames, 0, args.length-1);
        try {
            checkJar(jar, ignoreClassNames);
        } catch (Throwable e) {
            fatalErr(e);
        }

        System.out.println("All classes in " + jar + " are confirmed to be loadable with the current classpath.");
        System.exit(0);
    }

    private static void checkJar(String jar, String[] ignoreClassNames) throws IOException {
        boolean hadErrors = false;
        List toIgnore = Arrays.asList(ignoreClassNames);
        JarFile jarFile = new JarFile(jar);
        Enumeration entries = jarFile.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = (ZipEntry) entries.nextElement();
            String name = entry.getName();
            if (name.endsWith(".class")) {
                String truncName = name.substring(0, name.length() - ".class".length());
                String className = truncName.replace('/', '.').replace('\\', '.');
                if (className.indexOf('$') > -1) {
                    log("Undecorating inner class " + name);
                    
                }
                if(toIgnore.contains(className)) {
                    log("Skipping class: " + className);
                }                
                else {
                    log("Loading class: " + className);
                    try {
                        Class.forName(className);
                    } catch (Throwable e) {
                        System.out.println("ERROR: while loading class: " + className + ": " + e);
                        e.printStackTrace(System.out);
                        hadErrors = true;
                    }
                }
            }
        }

        if (hadErrors)
            throw new RuntimeException("There were errors while loading at least one class.");
    }

    private static void log(String s) {
        // Uncomment to log some stuff
        System.out.println(s);
    }

    private static void fatalErr(Throwable e) {
        System.out.println("ERROR: Fatal error: " + e.getMessage());
        e.printStackTrace(System.out);
        System.exit(1);
    }
}
