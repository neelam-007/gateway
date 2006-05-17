package com.l7tech.policy.wsp;

/**
 * Utility class to allow setting of the ClassLoader to be used by WSP classes.
 *
 * <p>You would not normally create instances of this class.</p>
 *
 * @author Steve Jones, $Author$
 * @version $Revision$
 */
public class ClassLoaderUtil
{
    /**
     * Set the ClassLoader to use when loading classes.
     *
     * @param classLoader The classloader to use
     */
    public static void setClassloader(ClassLoader classLoader) {
        // any WSP classes with configurable classloaders go here.
        SerializedJavaClassMapping.setClassloader(classLoader);
    }
}
