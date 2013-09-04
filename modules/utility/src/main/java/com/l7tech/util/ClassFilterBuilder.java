package com.l7tech.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * A builder for a composite ClassFilter to use with a SafeXMLDecoder to recognize classes, constructors
 * and methods that are safe to invoke while decoding untrusted XML.
 */
public class ClassFilterBuilder {

    private final List<ClassFilter> classFilters = new ArrayList<>();

    private boolean includeAnnotationFilter = false;
    private ClassLoader annotationClassLoader = null;
    private final List<String> annotationPackagePrefixes = new ArrayList<>();

    private boolean includeStringFilter = false;
    private final Set<String> classes = new HashSet<>();
    private final Set<String> constructors = new HashSet<>();
    private final Set<String> methods = new HashSet<>();

    /**
     * Create a ClassFilterBuilder that allows nothing.
     */
    public ClassFilterBuilder() {
    }

    /**
     * Configure this builder to allow the default whitelist (in addition to any additions that have been
     * made already to this builder or that will be made after this call).
     * <p/>
     * The default whitelist allows any class on a small list of well-known JDK built-in bean and collection
     * classes that are believed to be safe for XML decoding, along with any class whose package name starts with
     * the prefix "com.l7tech." that is annotated with the @XmlSafe annotation.
     *
     * @return this builder
     */
    public ClassFilterBuilder allowDefaults() {
        includeAnnotationFilter = true;
        annotationClassLoader = null;
        annotationPackagePrefixes.addAll(DEFAULT_ANNOTATION_PACKAGE_PREFIXES);

        includeStringFilter = true;
        classes.addAll(DEFAULT_CLASSES);
        constructors.addAll(DEFAULT_CONSTRUCTORS);
        methods.addAll(DEFAULT_METHODS);

        return this;
    }

    /**
     * Add the specified custom class filter to the builder.
     *
     * @param classFilter a custom class filter that may permit additional classes beyond those already permitted.
     * @return this builder
     */
    public ClassFilterBuilder addClassFilter(@NotNull final ClassFilter classFilter) {
        classFilters.add(classFilter);
        return this;
    }

    /**
     * Configure the default annotation class filter to use the specified class loader for loading annotated
     * classes.
     *
     * @param classLoader a class loader to use for loading classes to check for @XmlSafe annotations, or null to use the context class loader.
     * @return this builder
     */
    public ClassFilterBuilder setAnnotationClassLoader(@Nullable ClassLoader classLoader) {
        includeAnnotationFilter = true;
        annotationClassLoader = classLoader;
        return this;
    }

    /**
     * Add the specified prefix to package prefixes whose classes to load and check for @XmlSafe annotations.
     *
     * @param annotatedPackagePrefix a package name prefix such as "com.ca.example.special." under which to allow use of the @XmlSafe annotation.
     * @return this builder
     */
    public ClassFilterBuilder addAnnotatedPackagePrefix(@NotNull final String annotatedPackagePrefix) {
        includeAnnotationFilter = true;
        annotationPackagePrefixes.add(annotatedPackagePrefix);
        return this;
    }

    /**
     * Allow one or more classes (and optionally their default constructors) to be invoked by the SafeXMLDecoder
     * while decoding XML.
     * <p/>
     * To be safe, the listed classes <b>must not have outside side-effects</b> in their static or instance initializers
     * or (if allowDefaultConstructors is true) in their default constructors, either directly or indirectly.
     *
     * @param allowDefaultConstructors true to allow the default constructor of each of the classes
     * @param classnames fully qualified names of classes to allow while decoding XML
     * @return this builder
     */
    public ClassFilterBuilder addClasses(boolean allowDefaultConstructors, String... classnames) {
        includeStringFilter = true;
        final List<String> names = Arrays.asList(classnames);
        classes.addAll(names);
        if (allowDefaultConstructors) {
            constructors.addAll(Functions.map(names, new Functions.Unary<String, String>() {
                @Override
                public String call(String s) {
                    return s + "()";
                }
            }));
        }
        return this;
    }

    /**
     * Allow one or more constructors to be invoked by the SafeXMLDecoder while decoding XML.
     * Only constructors whose classes are already permitted will be permitted.
     * The constructor name format is the fully qualified name of the class followed by a parenthesized
     * comma-delimited list of argument type names, using the reflection syntax for type names with the exception
     * that arrays are indicated using Java source syntax.
     * Examples:
     * <pre>
     *   java.lang.Object()
     *   com.example.Simple(int,boolean,java.lang.String)
     *   com.example.Boxed(java.lang.Integer,java.lang.Float,java.lang.Double)
     *   com.example.ComplexInnerAndAnonymousInnerClasses$Inner$3$1(com.example.Test$1$1$Blarg[][],int,java.lang.String)
     * </pre>
     * To be safe, the listed constructors <b>must not have outside side-effects</b>, either directly or indirectly.
     *
     * @param constructorNames names of constructors to permit, in the above format.
     * @return this builder
     */
    public ClassFilterBuilder addConstructors(String... constructorNames) {
        includeStringFilter = true;
        constructors.addAll(Arrays.asList(constructorNames));
        return this;
    }

    /**
     * Allow one or more methods to be invoked by the SafeXMLDecoder while decoding XML.
     * Only methods whose classes are already permitted will be permitted.
     * The method name format is the fully qualified name of the class, followed by a dot, the name of the method,
     * and a parenthesized comma-delimited list of argument type names, using the reflection syntax for type names
     * with the exception that arrays are indicated using Java source syntax.
     * Examples:
     * <pre>
     *     java.lang.Object.hashCode()
     *     java.lang.reflect.Array.set(java.lang.Object,int,java.lang.Object)
     *     java.util.HashMap.remove(java.lang.Object)
     *     com.example.ComplexInnerAndAnonymousInnerClasses$Inner$3$1.doThing(com.example.Test$1$1$Blarg[][],int,java.lang.String)
     * </pre>
     * @param methodNames names of methods to permit, in the above format.
     * @return this builder
     */
    public ClassFilterBuilder addMethods(String... methodNames) {
        includeStringFilter = true;
        methods.addAll(Arrays.asList(methodNames));
        return this;
    }

    /**
     * Build a ClassFilter using the current builder configuration.
     *
     * @return a ClassFilter implementation.  Never null.
     */
    public ClassFilter build() {
        List<ClassFilter> filters = new ArrayList<>();
        filters.addAll(classFilters);

        if (includeAnnotationFilter)
            filters.add(new AnnotationClassFilter(annotationClassLoader, annotationPackagePrefixes));

        if (includeStringFilter)
            filters.add(new StringClassFilter(classes, constructors, methods));

        return new CompositeClassFilter(filters.toArray(new ClassFilter[filters.size()]));
    }

    private final List<String> DEFAULT_ANNOTATION_PACKAGE_PREFIXES = Arrays.asList(
        "com.l7tech."
    );

    private static final Set<String> DEFAULT_CLASSES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
        "java.jang.Object",
        "java.lang.String",
        "java.util.TreeSet",
        "java.util.HashMap",
        "java.util.ArrayList",
        "java.util.LinkedList",
        "java.util.LinkedHashMap",
        "java.util.TreeMap"
    )));

    private static final Set<String> DEFAULT_CONSTRUCTORS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
        "java.jang.Object()",
        "java.util.TreeSet()",
        "java.util.HashMap()",
        "java.util.ArrayList()",
        "java.util.LinkedList()",
        "java.util.LinkedHashMap()",
        "java.util.TreeMap()"
    )));
    private static final Set<String> DEFAULT_METHODS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
        "java.lang.reflect.Array.set(java.lang.Object,int,java.lang.Object)",
        "java.util.ArrayList.add(java.lang.Object)",
        "java.util.LinkedList.add(java.lang.Object)",
        "java.util.TreeSet.add(java.lang.Object)",
        "java.util.HashMap.remove(java.lang.Object)",
        "java.util.HashMap.put(java.lang.Object,java.lang.Object)",
        "java.util.TreeMap.put(java.lang.Object,java.lang.Object)"
    )));
}
