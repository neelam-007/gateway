package com.l7tech.util;

import org.jetbrains.annotations.NotNull;

import java.beans.ExceptionListener;
import java.io.InputStream;
import java.util.*;

/**
 * <p>A builder to build an instance of {@link SafeXMLDecoder}, this is a convenient method of creating and adding
 * new filters to the decoder.</p>
 */
public final class SafeXMLDecoderBuilder {

    private InputStream inputStream;
    private List<ClassFilter> classFilters;
    private Object owner;
    private ExceptionListener exceptionListener;
    private ClassLoader classLoader;

    private static final Set<String> classes = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "java.jang.Object",
            "java.lang.String",
            "java.util.TreeSet",
            "java.util.HashMap",
            "java.util.LinkedList",
            "java.util.LinkedHashMap",
            "java.util.TreeMap"
    )));

    private static final Set<String> constructors = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "java.jang.Object()",
            "java.util.TreeSet()",
            "java.util.HashMap()",
            "java.util.LinkedList()",
            "java.util.LinkedHashMap()",
            "java.util.TreeMap()"
    )));
    private static final Set<String> methods = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "java.lang.reflect.Array.set(java.lang.Object,int,java.lang.Object)",
            "java.util.LinkedList.add(java.lang.Object)",
            "java.util.TreeSet.add(java.lang.Object)",
            "java.util.HashMap.remove(java.lang.Object)",
            "java.util.HashMap.put(java.lang.Object,java.lang.Object)",
            "java.util.TreeMap.put(java.lang.Object,java.lang.Object)"
    )));

    public SafeXMLDecoderBuilder(@NotNull final InputStream inputStream) {
        this.inputStream = inputStream;
        classFilters = new ArrayList<>();
        classFilters.add(new AnnotationClassFilter(null, Arrays.asList("com.l7tech.")));
        classFilters.add(new StringClassFilter(classes, constructors, methods));
        this.exceptionListener = new ExceptionListener() {
            @Override
            public void exceptionThrown(Exception e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        };
    }

    public SafeXMLDecoderBuilder setOwner(final Object owner) {
        this.owner = owner;
        return this;
    }

    public SafeXMLDecoderBuilder setExceptionListener(final ExceptionListener exceptionListener) {
        this.exceptionListener = exceptionListener;
        return this;
    }

    public SafeXMLDecoderBuilder setClassLoader(final ClassLoader classLoader) {
        this.classLoader = classLoader;
        return this;
    }

    public SafeXMLDecoderBuilder addClassFilter(@NotNull final ClassFilter classFilter) {
        classFilters.add(classFilter);
        return this;
    }

    public SafeXMLDecoder build(){
        return new SafeXMLDecoder(new CompositeClassFilter(classFilters.toArray(new ClassFilter[classFilters.size()])), inputStream, owner, exceptionListener, classLoader);
    }
}