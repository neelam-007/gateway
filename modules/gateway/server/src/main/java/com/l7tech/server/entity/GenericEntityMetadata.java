package com.l7tech.server.entity;

import com.l7tech.util.ClassFilter;

import java.util.*;

/**
 * Holds metadata that may be needed at registration time for certain generic entity types.
 */
public class GenericEntityMetadata {
    private Set<String> safeXmlClasses;
    private Set<String> safeXmlConstructors;
    private Set<String> safeXmlMethods;
    private List<ClassFilter> safeXmlClassFilter;

    /**
     * List additional classes that should be considered safe to use when using the SafeXMLDecoder
     * to parse properties for the described GenericEntity subclass.
     *
     * @param classnames a list of class names, eg "java.util.Hashtable".
     */
    public GenericEntityMetadata addSafeXmlClasses(String... classnames) {
        if (getSafeXmlClasses() == null)
            safeXmlClasses = new LinkedHashSet<>();
        getSafeXmlClasses().addAll(Arrays.asList(classnames));
        return this;
    }

    /**
     * List additional constructors that should be considered safe to use when using the SafeXMLDecoder
     * to parse properties for the described GenericEntity subclass.
     *
     * @param constructors a list of constructor names, eg "java.util.Hashtable(int,float)".
     */
    public GenericEntityMetadata addSafeXmlConstructors(String... constructors) {
        if (getSafeXmlConstructors() == null)
            safeXmlConstructors = new LinkedHashSet<>();
        getSafeXmlConstructors().addAll(Arrays.asList(constructors));
        return this;
    }

    /**
     * List additional methods that should be considered safe to use when using the SafeXMLDecoder
     * to parse properties for the described GenericEntity subclass.
     *
     * @param methods a list of method names, eg "java.util.Hashtable.put(java.lang.Object,java.lang.Object)".
     */
    public GenericEntityMetadata addSafeXmlMethods(String... methods) {
        if (getSafeXmlMethods() == null)
            safeXmlMethods = new LinkedHashSet<>();
        getSafeXmlMethods().addAll(Arrays.asList(methods));
        return this;
    }

    /**
     * Add a custom class filter for allowing additional classes, constructors, or methods to be used
     * when using the SafeXMLDecoder to deserialize properties for the described GenericEntity subclass.
     * <p/>
     * Never register a wide-open class filter!  Only permit classes, constructors and methods that are designed
     * as pure data objects, where the invoked code has no side-effects other than building up an object in memory
     * (including side-effects like doing any kind of global registration).
     *
     * @param classFilter a custom class filter to use.
     */
    public GenericEntityMetadata addSafeXmlClassFilter(ClassFilter classFilter) {
        if (getSafeXmlClassFilter() == null)
            safeXmlClassFilter = new ArrayList<>();
        getSafeXmlClassFilter().add(classFilter);
        return this;
    }

    Set<String> getSafeXmlClasses() {
        return safeXmlClasses;
    }

    Set<String> getSafeXmlConstructors() {
        return safeXmlConstructors;
    }

    Set<String> getSafeXmlMethods() {
        return safeXmlMethods;
    }

    List<ClassFilter> getSafeXmlClassFilter() {
        return safeXmlClassFilter;
    }
}
