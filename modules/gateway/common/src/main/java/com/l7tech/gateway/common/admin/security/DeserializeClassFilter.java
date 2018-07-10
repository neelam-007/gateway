package com.l7tech.gateway.common.admin.security;

import com.l7tech.util.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Dedicated {@code ClassFilter} to whitelist classes allowed to be deserialized via sprint remoting.
 */
public class DeserializeClassFilter extends CompositeClassFilter {
    @Nullable
    private static ClassFilter classFilter;

    /**
     * Do not use this constructor!<br/>
     * Use {@link #getInstance()} instead.
     */
    DeserializeClassFilter() {
        super(
                new ArraysAndPrimitivesClassFilter(), /** allows arrays, all primitives and their object wrappers {@link ArraysAndPrimitivesClassFilter#PRIMITIVE_WRAPPERS} */
                new StringClassFilter(
                        join(
                                ALLOWED_CLASSES,  // allowed well-known classes
                                extract(PROP_WHITELIST_CLASSES) // allowed classes declared in system property
                        ),
                        Collections.<String>emptySet(), // empty as we don't care about constructors
                        Collections.<String>emptySet()  // empty as we don't care about methods
                ),
                // finally use annotation class filter
                // preferably added last, as the above two are using hash and should be very fast
                new DeserializeAnnotationClassFilter(ALLOWED_ANNOTATION_PACKAGE_PREFIXES) /** allows classes annotated with {@link DeserializeSafe} */
        );
    }

    @NotNull
    static public ClassFilter getInstance() {
        // lazy initialize classFilter
        if (classFilter == null) {
            classFilter = new DeserializeClassFilter();
        }
        return classFilter;
    }

    private static final Pattern COMMA_PATTERN = Pattern.compile("\\s*,\\s*");

    /**
     * Extracts the values from the specified system property.<br/>
     * Values are separated with comma (,).
     */
    private static Collection<String> extract(@NotNull final String propName) {
        final Collection<String> strings = new ArrayList<>();
        String val = SyspropUtil.getString(propName, null);
        if (val != null && val.length() > 0) {
            String[] split = COMMA_PATTERN.split(val);
            if (split != null) {
                for (String s : split) {
                    if (s != null) {
                        s = s.trim();
                        if (s.length() > 0) {
                            strings.add(s);
                        }
                    }
                }
            }
        }
        return strings;
    }

    /**
     * Joins the specified {@code collections} into a new read-only {@code HashSet}.
     */
    @SafeVarargs
    private static Set<String> join(@NotNull final Collection<String> ... collections) {
        final Set<String> ret = new HashSet<>();
        for (final Collection<String> c : collections) {
            ret.addAll(c);
        }
        return Collections.unmodifiableSet(ret);
    }

    // system property for adding allowed classes
    private static final String PROP_WHITELIST_CLASSES = "com.l7tech.server.DeserializeSafe.allowClasses";

    // Add new well known allowed classes here
    static final Collection<String> ALLOWED_CLASSES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "org.springframework.remoting.support.RemoteInvocation",
            "java.lang.Object",
            "java.lang.String",
            "java.lang.Number",
            "java.lang.Enum",
            "java.util.ArrayList",
            "java.util.HashMap",
            "java.util.LinkedHashMap",
            "java.util.TreeMap",
            "java.util.HashSet",
            "java.util.TreeSet",
            "org.hibernate.collection.PersistentBag",
            ////////////////////////////////////
            // AsyncAdmin
            ////////////////////////////////////
            "com.l7tech.gateway.common.AsyncAdminMethods$JobId",
            ////////////////////////////////////
            // Entity
            ////////////////////////////////////
            "com.l7tech.objectmodel.Goid",
            "com.l7tech.objectmodel.EntityType",
            "org.hibernate.collection.AbstractPersistentCollection",
            "com.l7tech.objectmodel.imp.NamedEntityWithPropertiesImp",
            "com.l7tech.objectmodel.imp.NamedEntityImp",
            "com.l7tech.objectmodel.imp.PersistentEntityImp"
            ////////////////////////////////////
    )));

    // Add new allowed annotation package prefixes here
    static final Collection<String> ALLOWED_ANNOTATION_PACKAGE_PREFIXES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "com.l7tech.",
            "com.ca."
    )));
}
