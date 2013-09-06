package com.l7tech.util;

import com.l7tech.util.Functions.Unary;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.ArrayList;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;

/**
 * Utility methods for dealing with classes, class names and resources.
 */
public class ClassUtils {

    //- PUBLIC

    /**
     * Strips the package name and any enclosing class names from a fully-qualified class name.
     * For example, if passed "java.lang.String",
     * would return "String"; if passed "com.example.Foo.Bar", returns "Bar".
     *
     * @param fullName the fully-qualified class name.  Must not be null or empty.
     * @return the class part only of the name.  Never null, but might be empty if fullName is empty or ends in a dot.
     * @throws NullPointerException if fullName is null
     */
    public static String getClassName(String fullName) {
        if (fullName == null) throw new NullPointerException();
        if (fullName.length() < 2) return fullName;

        int dotpos = fullName.lastIndexOf('.');
        if (dotpos < 0) return fullName;
        return fullName.substring(dotpos + 1);
    }

    /**
     * Strips the package name and any enclosing class names from a fully-qualified class name.
     * For example, if passed "java.lang.String",
     * would return "String"; if passed "com.example.Foo.Bar", returns "Bar".
     *
     * @param clazz the class whose name to extract.  Must not be null.
     * @return the class part only of the name.  Never null, but might be empty if the input class has a pathological name.
     * @throws NullPointerException if clazz is null
     */
    public static String getClassName(Class clazz) {
        return getClassName(clazz.getName());
    }

    /**
     * Strips the package name and any enclosing class names from a fully-qualified class name.
     * For example, if passed "java.lang.String",
     * would return "String"; if passed "com.example.Foo$Bar", returns "Bar".
     *
     * @param fullName the fully-qualified class name.  Must not be null or empty.
     * @return the class part only of the name.  Never null, but might be empty if fullName is empty or ends in a dot.
     * @throws NullPointerException if fullName is null
     */
    public static String getInnerClassName( final String fullName ) {
        String name = getClassName( fullName );
        int pos = name.lastIndexOf( '$' );
        if ( pos < 0 ) {
            return name;
        } else {
            return name.substring( pos + 1 );
        }
    }

    /**
     * Strips the package name and any enclosing class names from a fully-qualified class name.
     * For example, if passed "java.lang.String",
     * would return "String"; if passed "com.example.Foo$Bar", returns "Bar".
     *
     * @param clazz the class whose name to extract.  Must not be null.
     * @return the class part only of the name.  Never null, but might be empty if fullName is empty or ends in a dot.
     * @throws NullPointerException if fullName is null
     */
    public static String getInnerClassName( final Class clazz ) {
        return getInnerClassName( clazz.getName() );
    }

    /**
     * Strips the class name and returns just the package name from a fully-qualified class name.
     * For example, if passed "java.lang.String",
     * would return "java.lang"; and if passed "MumbleFrotz$Foofy$2$11", returns "".
     *
     * @param fullName  the fully qualified class name whose package to extract.  Required.
     * @return the package name, which may be empty (if this is in the default package) but will never be null.
     */
    public static String getPackageName(String fullName) {
        int di = fullName.lastIndexOf(".");
        if (di < 2)
            return "";
        return fullName.substring(0, di);
    }

    /**
     * Strips the class name and returns just the package name from a fully-qualified class name.
     * For example, if passed "java.lang.String",
     * would return "java.lang"; and if passed "MumbleFrotz$Foofy$2$11", returns "".
     *
     * @param clazz  the class whose package to extract.  Required.
     * @return the package name, which may be empty (if this is in the default package) but will never be null.
     */
    public static String getPackageName(Class clazz) {
        return getPackageName(clazz.getName());
    }

    /**
     * Strip the specified suffix, if the string ends with it.
     *
     * @param name     the string to strip, ie "com.yoyodyne.layer7.assertion"
     * @param suffix   the suffix to strip, ie ".assertion"
     * @return the name with any matching suffix stripped, ie "com.yoyodyne.layer7"
     */
    public static String stripSuffix(String name, String suffix) {
        if (name.endsWith(suffix))
            name = name.length() <= suffix.length() ? "" : name.substring(0, name.length() - suffix.length());
        return name;
    }

    /**
     * Strip the specified prefix, if the string begins with it.
     *
     * @param name     the string to strip, ie "com.yoyodyne.layer7.assertion.composite.grouped"
     * @param prefix   the suffix to strip, ie "com.yoyodyne.layer7.assertion."
     * @return the name with any matching prefix stripped, ie "composite.grouped"
     */
    public static String stripPrefix(String name, String prefix) {
        if (name.startsWith(prefix))
            name = name.length() <= prefix.length() ? "" : name.substring(prefix.length());
        return name;
    }

    /**
     * Is the given class name for an array type.
     *
     * <p>This currently returns false for primitive arrays.</p>
     *
     * @param name The class name to check.
     * @return True if the class is an array class
     */
    public static boolean isArrayClassName( final String name ) {
        // From :
        //   http://java.sun.com/docs/books/jvms/second_edition/html/ConstantPool.doc.html#73272
        //
        // For an array class of M dimensions, the name begins with M occurrences
        // of the ASCII "[" character followed by a representation of the element type:
        //
        // If the element type is a primitive type, it is represented by the
        // corresponding field descriptor.
        //
        // Otherwise, if the element type is a reference type, it is represented by the
        // ASCII "L" character followed by the fully qualified name of the element type
        // followed by the ASCII ";" character.
        return name.startsWith( ARRAY_PREFIX ) && name.endsWith( ARRAY_SUFFIX );
    }

    /**
     * Get the array element class name.
     *
     * @param name The class name
     * @return The given name or the element name for an array class.
     */
    public static String getArrayElementClassName( final String name ) {
        String arrayClass = name;

        final int index = name.indexOf( 'L' );
        if ( isArrayClassName(name) &&
                index > 0 &&
                index < 10 && // limits array dimensions
                index < name.length() -1 ) {
            arrayClass = name.substring( index+1, name.length() -1 );
        }

        return arrayClass;
    }

    /**
     * Return the name of the specified type as it would appear in Java source.
     * For most types this is the same as type.getName().  For arrays, this
     * converts the type name from something like "[B" or "[Ljava.lang.String;"
     * into "byte[]" or "java.lang.String[]" respectively.
     *
     * @param type name of type whose friendly name to produce.  Required.
     * @return a java name for the type.
     */
    public static String getJavaTypeName(Class<?> type) {
        if (!type.isArray())
            return type.getName();

        String name;
        int bracketsNeeded = 0;
        Class<?> componentType = type;
        do {
            bracketsNeeded++;
            name = componentType.getName();
            componentType = componentType.getComponentType();
        } while (componentType != null);

        StringBuilder sb = new StringBuilder(name);
        for (int i = 1; i < bracketsNeeded; ++i) {
            sb.append("[]");
        }
        return sb.toString();
    }

    /**
     * Return a string describing the specified method in a way that includes only relevant information
     * for uniquely identifying it (method name, argument types).
     *
     * @param method method to name.  Required.
     * @return a name for the method that includes the fully qualified class name, method name, and parenthesized argument type names
     *         while omitting other information (such as modifiers, return type, and declared exceptions).
     */
    public static String getMethodName(@NotNull Method method) {
        StringBuilder sb = new StringBuilder();
        sb.append(getJavaTypeName(method.getDeclaringClass()));
        sb.append('.');
        sb.append(method.getName());
        sb.append('(');
        boolean first = true;
        for (Class<?> type : method.getParameterTypes()) {
            if (!first)
                sb.append(',');
            sb.append(getJavaTypeName(type));
            first = false;
        }
        sb.append(')');
        return sb.toString();
    }

    /**
     * Return a string describing the specified constructor that is sufficient to uniquely identify it
     * but that omits other information.
     *
     * @param constructor the constructor to name.  Required.
     * @return a string containing the fully qualified class name and a parenthesized list of argument types.
     */
    public static String getConstructorName(@NotNull Constructor constructor) {
        StringBuilder sb = new StringBuilder();
        sb.append(getJavaTypeName(constructor.getDeclaringClass()));
        sb.append('(');
        boolean first = true;
        for (Class<?> type : constructor.getParameterTypes()) {
            if (!first)
                sb.append(',');
            sb.append(getJavaTypeName(type));
            first = false;
        }
        sb.append(')');
        return sb.toString();
    }

    /**
     * List the resources contained in the path.
     *
     * <p>WARNING: This should work for JAR / file resources, but will not work
     * in all scenarios unless you use an index.</p>
     *
     * @param baseClass The base class for resource resolution
     * @param resourcePath The path to the resource directory / listing file (must be a directory, use a "/")
     * @return The collection of resources (never null)
     */
    public static Collection<URL> listResources( final Class baseClass,
                                                 final String resourcePath ) throws IOException {
        URL resourceBaseUrl = baseClass.getResource( resourcePath );
        List<URL> resourceUrls = new ArrayList<URL>();

        if ( resourceBaseUrl != null ) {
            BufferedReader reader = null;
            try {
                reader = new BufferedReader( new InputStreamReader(resourceBaseUrl.openStream()) );
                String name;
                while( (name = reader.readLine()) != null ) {
                    resourceUrls.add( new URL(resourceBaseUrl, name) );
                }
            } finally {
                ResourceUtils.closeQuietly( reader );
            }
        }

        return resourceUrls;
    }

    /**
     * Get a function that casts to the given type.
     *
     * @param clazz The class for the type
     * @param <T> The target type
     * @return The function
     */
    public static <T> Unary<T,Object> cast( final Class<T> clazz ) {
        return new Unary<T,Object>() {
            @Override
            public T call( final Object o ) {
                return clazz.isInstance( o ) ? clazz.cast( o ) : null;
            }
        };
    }

    //- PRIVATE

    private static final String ARRAY_PREFIX = "[";
    private static final String ARRAY_SUFFIX = ";";
}
