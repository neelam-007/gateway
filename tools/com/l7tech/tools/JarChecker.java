/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.tools;

import java.io.IOException;
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
            System.err.println("Usage: JarChecker jarfile.jar");
            System.exit(1);
        }

        String jar = args[0];
        try {
            checkJar(jar);
        } catch (Throwable e) {
            fatalErr(e);
        }

        System.err.println("All classes in " + jar + " are confirmed to be loadable with the current classpath.");
        System.exit(0);
    }

    private static void checkJar(String jar) throws IOException {
        boolean hadErrors = false;
        JarFile jarFile = new JarFile(jar);
        Enumeration entries = jarFile.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = (ZipEntry) entries.nextElement();
            String name = entry.getName();
            if (name.endsWith(".class")) {
                String truncName = name.substring(0, name.length() - ".class".length());
                String className = truncName.replace('/', '.').replace('\\', '.');
                if (className.indexOf('$') > -1) {
                    log("Skipping inner class " + name);
                } else {
                    log("Loading class: " + className);
                    try {
                        Class.forName(className);
                    } catch (Throwable e) {
                        System.err.println("ERROR: while loading class: " + className + ": " + e);
                        e.printStackTrace(System.err);
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
        System.err.println("ERROR: Fatal error: " + e.getMessage());
        e.printStackTrace(System.err);
        System.exit(1);
    }
}
