package com.l7tech.util;

import java.net.URL;

/**
 * The class determines the absolute pathname of the class file
 * containing the specified class name, as prescribed  by the
 * current classpath.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com>Emil Marceta</a>
 */
public class JWhich {
    private String className;
    private ClassLoader classLoader;

    /**
     * Construct the new <code>JWhich</code> that determines
     * the absolute pathname of the class file containing the
     * specified class name, as prescribed by the current classpath.
     *
     * @param cls the class.
     */
    public JWhich(Class cls) {
        this(cls.getName());
    }

    /**
     * Construct the new <code>JWhich</code> that determines
     * the absolute pathname of the class file containing the
     * specified class name, as prescribed by the current classpath.
     *
     * @param className Name of the class.
     */
    public JWhich(String className) {
        this.className = className;
    }

    /**
     * Returns the absolute pathname of the class file
     * containing the specified class name, as prescribed
     * by the current classpath or <b>null</b> if the class
     * is not found
     *
     * @return the class absolute pathname or <b>null</b> if
     *         none found
     */
    public String which() {
        if (!className.startsWith("/")) {
            className = "/" + className;
        }
        className = className.replace('.', '/');
        className = className + ".class";

        URL classUrl = getClass().getResource(className);

        if (classUrl != null) {
            return classUrl.getFile();
        }
        return null;
    }

    /**
     * @return the string represenation of the instance
     */
    public String toString() {
        String s = which();
        if (s != null) {
            return "\nClass '" + className +
                    "' found in \n'" + s + "'";
        }
        return "\nClass '" + className +
                "' not found in current classpath";
    }


    public static void main(String[] args) {
        if (args.length > 0) {
            System.out.println(new JWhich(args[0]));
        } else {
            System.err.println("Usage: java JWhich <classname>");
        }
    }
}

