package com.l7tech.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Permits access to the specified classes based on the presence of {@link XmlSafe} annotations on the
 * class, constructor, or method.
 */
public class AnnotationClassFilter implements ClassFilter {
    private static final Logger logger = Logger.getLogger(AnnotationClassFilter.class.getName());

    private final Functions.Unary<Boolean,String> prefixMatcher;
    private final ClassLoader classLoader;

    /**
     * Create an AnnotationClassFilter that will use the specified class loader for loading classes
     * to check their annotations, only loading classes whose names start with one of the specified
     * prefixes.
     *
     * @param classLoader class loader to use for loading classes to check for annotations given a class name, or null to use the context class loader at the time.
     * @param classPrefixes prefixes of classes to load and check annotations on.  Required.
     */
    public AnnotationClassFilter(@Nullable ClassLoader classLoader, @NotNull List<String> classPrefixes) {
        prefixMatcher = TextUtils.matchesAnyPrefix(classPrefixes);
        this.classLoader = classLoader;
    }

    @Override
    public boolean permitClass(@NotNull String classname) {
        if (!prefixMatcher.call(classname)) {
            if (logger.isLoggable(Level.FINE))
                logger.fine("Not checking annotation for non-prefix-matched classname " + classname);
            return false;
        }

        ClassLoader loader = classLoader;
        if (loader == null)
            loader = Thread.currentThread().getContextClassLoader();
        if (loader == null)
            loader = AnnotationClassFilter.class.getClassLoader();

        try {
            Class<?> clazz = loader.loadClass(classname);
            if (clazz != null) {
                return permitClass(clazz);
            }
            return false;

        } catch (ClassNotFoundException e) {
            if (logger.isLoggable(Level.FINE))
                logger.log(Level.FINE, "Unable to load class to check annotation: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            return false;
        }
    }

    /**
     * Check if a class should be permitted, once it has been loaded.
     * <p/>
     * This method just checks the XmlSafe annotation of the class.
     *
     * @param clazz class to check.  Required.
     * @return true if this class is safe to allow while XML decoding.
     */
    protected boolean permitClass(@NotNull Class<?> clazz) {
        XmlSafe classSafety = clazz.getAnnotation(XmlSafe.class);
        if (classSafety != null)
            return classSafety.safe();
        return false;
    }

    @Override
    public boolean permitConstructor(@NotNull Constructor<?> constructor) {
        XmlSafe methodSafety = constructor.getAnnotation(XmlSafe.class);
        if (methodSafety != null) {
            // An explicit safe=false on a constructor overrides any wildcard setting in the class @XmlSafe
            return methodSafety.safe();
        }

        // Check for blanket permits.
        XmlSafe classSafety = constructor.getDeclaringClass().getAnnotation(XmlSafe.class);
        if (classSafety == null)
            return false;

        if (classSafety.allowAllConstructors())
            return true;

        if (classSafety.allowDefaultConstructor() &&
            constructor.getParameterTypes().length == 0) {
            return true;
        }

        return false;
    }

    @Override
    public boolean permitMethod(@NotNull Method method) {
        XmlSafe methodSafety = method.getAnnotation(XmlSafe.class);
        if (methodSafety != null) {
            // An exlicit safe=false on a method overrides any wildcard setting in the class @XmlSafe
            return methodSafety.safe();
        }

        // Check for blanket permits.
        XmlSafe classSafety = method.getDeclaringClass().getAnnotation(XmlSafe.class);
        if (classSafety == null)
            return false;

        if (classSafety.allowAllSetters() && isSetter(method)) {
            return true;
        }

        return false;
    }

    protected boolean isSetter(@NotNull Method method) {
        return !Modifier.isStatic(method.getModifiers()) && method.getName().startsWith("set") && method.getParameterTypes().length == 1;
    }

    protected boolean isGetter(@NotNull Method method) {
        return !Modifier.isStatic(method.getModifiers()) &&
            (method.getName().startsWith("get") || (method.getName().startsWith("is") && boolean.class.equals(method.getReturnType()))) &&
            method.getParameterTypes().length == 0;
    }
}
