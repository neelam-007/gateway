package com.l7tech.util;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.*;
import java.util.logging.Logger;

/**
 * A {@code ClassFilter} that allows arrays (object or primitive) and primitive types (including their object wrappers).
 */
public class ArraysAndPrimitivesClassFilter implements ClassFilter {
    private static final Logger logger = Logger.getLogger(ArraysAndPrimitivesClassFilter.class.getName());

    private static final String ARRAY_PREFIX_JLS = "[";
    private static final String ARRAY_SUFFIX_CANONICAL = "[]";

    @Override
    public boolean permitClass(@NotNull final String className) {
        // object or primitive arrays are allowed by default.
        // array contents will be checked in future calls.
        if (isArrayClassName(className)) {
            return true;
        }
        // otherwise unwrap class name
        final String primitiveType = TYPE_FIELD_DESCRIPTORS.get(className);
        final String name = (primitiveType != null) ? primitiveType : className;
        if (!DEFAULT_PRIMITIVE_CLASSES.contains(name)) {
            logger.fine("class not permitted: " + className);
            return false;
        }
        return true;
    }

    @Override
    public boolean permitConstructor(@NotNull final Constructor<?> constructor) {
        String name = ClassUtils.getConstructorName(constructor);
        if (!DEFAULT_PRIMITIVE_CONSTRUCTORS.contains(name)) {
            logger.fine("constructor not permitted: " + name);
            return false;
        }
        return true;
    }

    @Override
    public boolean permitMethod(@NotNull final Method method) {
        String name = ClassUtils.getMethodName(method);
        if (!DEFAULT_PRIMITIVE_METHODS.contains(name)) {
            logger.fine("method not permitted: " + name);
            return false;
        }
        return true;
    }

    /**
     * Is the given class name for an object or primitive array.
     *
     * @param className    The class name to check.
     * @return {@code true} if the the {@code className} is an object or primitive array class, {@code false} otherwise.
     */
    private static boolean isArrayClassName(@NotNull final String className) {
        return className.startsWith(ARRAY_PREFIX_JLS) || className.contains(ARRAY_SUFFIX_CANONICAL);
    }

    /**
     * Utility method to unwrap array element class name (if any) and convert to canonical name.<br/>
     * Based on:
     * http://docs.oracle.com/javase/specs/jvms/se8/html/jvms-4.html#jvms-4.2
     *
     * @param className    class name.  Required and cannot be {@code null}.
     */
    final String unwrapClassName(@NotNull String className) {
        // first check if the class name array is in canonical form
        int index = className.indexOf("[]");
        if (index > 0) {
            // this is canonical form so return the type
            return className.substring(0, index);
        } else {
            // JLS form
            int brackets = 0;
            // remove array brackets (if any)
            while (className.startsWith("[")) {
                brackets++;
                className = className.substring(1);
            }
            if (brackets > 0) {
                // this is array
                if (className.startsWith("L")) {
                    // this is object type
                    className = className.substring(
                            1,
                            className.endsWith(";")
                                    ? className.length() - 1
                                    : className.length()
                    );
                } else if (className.length() > 0) {
                    // this is primitive type
                    className = TYPE_FIELD_DESCRIPTORS.get(className.substring(0, 1));
                }
            } else {
                // not array
                final String primitiveType = TYPE_FIELD_DESCRIPTORS.get(className);
                if (primitiveType != null) {
                    className = primitiveType;
                }
            }
            // finally return the class name
            return className;
        }
    }

    private static final Class[] PRIMITIVE_WRAPPERS = {
            Boolean.class,
            Byte.class,
            Character.class,
            Short.class,
            Integer.class,
            Long.class,
            Double.class,
            Float.class,
            Void.class
    };

    // Field Descriptors from:
    // http://docs.oracle.com/javase/specs/jvms/se8/html/jvms-4.html#jvms-4.3.2
    private static final Map<String, String> TYPE_FIELD_DESCRIPTORS = Collections.unmodifiableMap(CollectionUtils.<String, String>mapBuilder()
                    .put("Z", "boolean")
                    .put("B", "byte")
                    .put("C", "char")
                    .put("S", "short")
                    .put("I", "int")
                    .put("J", "long")
                    .put("D", "double")
                    .put("F", "float")
                    .map()
    );

    /**
     * all primitive types and their wrapper object types
     */
    private static final Set<String> DEFAULT_PRIMITIVE_CLASSES = Collections.unmodifiableSet(new HashSet<>(
            CollectionUtils.join(
                    Arrays.asList(
                            Arrays.asList(
                                    Boolean.TYPE.getName(),
                                    Byte.TYPE.getName(),
                                    Character.TYPE.getName(),
                                    Short.TYPE.getName(),
                                    Integer.TYPE.getName(),
                                    Long.TYPE.getName(),
                                    Double.TYPE.getName(),
                                    Float.TYPE.getName(),
                                    Void.TYPE.getName()
                            ),
                            Functions.map(Arrays.asList(PRIMITIVE_WRAPPERS), new Functions.Unary<String, Class>() {
                                @Override
                                public String call(final Class aClass) {
                                    return aClass.getName();
                                }
                            })
                    )
            )
    ));

    // todo: remove if not needed
    private static final Set<String> DEFAULT_PRIMITIVE_CONSTRUCTORS = Collections.unmodifiableSet(new HashSet<>(
            CollectionUtils.join(
                    Functions.map(Arrays.asList(PRIMITIVE_WRAPPERS), new Functions.Unary<Collection<String>, Class>() {
                        @Override
                        public Collection<String> call(final Class aClass) {
                            return Functions.map(Arrays.asList(aClass.getConstructors()), new Functions.Unary<String, Constructor>() {
                                @Override
                                public String call(final Constructor constructor) {
                                    return ClassUtils.getConstructorName(constructor);
                                }
                            });
                        }
                    })
            )
    ));

    // todo: remove if not needed
    private static final Set<String> DEFAULT_PRIMITIVE_METHODS = Collections.unmodifiableSet(new HashSet<>(
            CollectionUtils.join(
                    Functions.map(Arrays.asList(PRIMITIVE_WRAPPERS), new Functions.Unary<Collection<String>, Class>() {
                        @Override
                        public Collection<String> call(final Class aClass) {
                            return Functions.map(Arrays.asList(aClass.getDeclaredMethods()), new Functions.Unary<String, Method>() {
                                @Override
                                public String call(final Method method) {
                                    return ClassUtils.getMethodName(method);
                                }
                            });
                        }
                    })
            )
    ));
}
