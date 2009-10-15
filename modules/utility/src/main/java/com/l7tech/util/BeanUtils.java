package com.l7tech.util;

import static com.l7tech.util.Functions.Unary;
import static com.l7tech.util.Functions.grep;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.InvocationHandler;
import java.util.*;

/**
 * Utilities for working with Java beans.
 */
public final class BeanUtils {
    private static final Object[] EMPTY_ARRAY = new Object[0];

    private BeanUtils() {}

    /**
     * Shallow copies all bean properties from source to destination.  Uses Introspector to find the properties,
     * so the behavior of this method can be configured class wide by defining a BeanInfo.
     *
     * @param source  the source object.  Required.
     * @param destination  the target object.  Required.
     * @throws RuntimeException if there is a problem introspecting the source class
     * @throws IllegalAccessException if Java access control prevents a getter or setter from being invoked
     * @throws java.lang.reflect.InvocationTargetException if a getter or setter throws an exception
     */
    public static <T> void copyProperties(T source, T destination) throws InvocationTargetException, IllegalAccessException {
        try {
            BeanInfo stuff = Introspector.getBeanInfo(source.getClass());
            PropertyDescriptor[] props = stuff.getPropertyDescriptors();
            copyProperties(source, destination, Arrays.asList(props));
        } catch (IntrospectionException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Shallow copies only the specified bean properties from source to destination.
     *
     * @param source  the source object.  Required.
     * @param destination  the target object.  Required.
     * @param props  the properties to copy.  Only these properties will be copied.
     * @throws IllegalAccessException if Java access control prevents a getter or setter from being invoked
     * @throws java.lang.reflect.InvocationTargetException if a getter or setter throws an exception
     */
    public static <T> void copyProperties(T source, T destination, Iterable<PropertyDescriptor> props)
            throws IllegalAccessException, InvocationTargetException
    {
        for (PropertyDescriptor prop : props) {
            Method getter = prop.getReadMethod();
            Method setter = prop.getWriteMethod();
            if (getter != null && setter != null) {
                Object value = getter.invoke(source, EMPTY_ARRAY);
                setter.invoke(destination, value);
            }
        }
    }

    /**
     * Copy properties from the given Map to the destination bean.
     *
     * @param source The source map (required)
     * @param sourcePrefix The source property prefix (optional)
     * @param destination The target bean (required)
     * @throws IllegalAccessException if Java access control prevents a setter from being invoked
     * @throws java.lang.reflect.InvocationTargetException if a setter throws an exception
     */
    public static void copyProperties( final Map<String,?> source,
                                       final String sourcePrefix,
                                       final Object destination ) throws IllegalAccessException, InvocationTargetException {
        List<String> propertyNames = new ArrayList<String>();
        String prefix = sourcePrefix == null ? "" : sourcePrefix;
        for ( String name : source.keySet() ) {
            if ( name.startsWith( prefix )) {
                propertyNames.add( name.substring(prefix.length()));
            }
        }

        String[] propNames = propertyNames.toArray( new String[propertyNames.size()] );
        Set<PropertyDescriptor> props = includeProperties(getProperties(destination.getClass()), propNames);
        for ( PropertyDescriptor prop : props ) {
            Method setter = prop.getWriteMethod();
            if (setter != null) {
                Object value = source.get( prefix + prop.getName() );
                setter.invoke(destination, value);
            }
        }
    }

    /**
     * Returns a new mutable set of PropertyDescriptor instances for all BeanInfo properties of the specified class
     * that have both getters and setters.
     *
     * @param clazz  the class to introspect.  Required
     * @return a Set of property descriptors.  May be empty but never null.
     */
    public static <T> Set<PropertyDescriptor> getProperties(Class<T> clazz) {
        try {
            return grep(new HashSet<PropertyDescriptor>(),
                        Arrays.asList(Introspector.getBeanInfo(clazz).getPropertyDescriptors()),
                        new Unary<Boolean, PropertyDescriptor>() {
                @Override
                public Boolean call(PropertyDescriptor propertyDescriptor) {
                    return propertyDescriptor.getReadMethod() != null && propertyDescriptor.getWriteMethod() != null;
                }
            });
        } catch (IntrospectionException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Create a new set filtering out all property descriptors with names that do not match any of the specified names.
     *
     * @param in the source set to filter.  Will not be modified.  Required.
     * @param propertyNamesToInclude  zero or more names of properties to allow through to the returned set.
     * @return a new Set that may contain the same or a fewer number of property descriptors.  May be empty but never null.
     */
    public static Set<PropertyDescriptor> includeProperties(Set<PropertyDescriptor> in, String... propertyNamesToInclude) {
        final Set<String> goodNames = new HashSet<String>(Arrays.asList(propertyNamesToInclude));
        return grep(new HashSet<PropertyDescriptor>(), in, new Unary<Boolean, PropertyDescriptor>() {
            @Override
            public Boolean call(PropertyDescriptor propertyDescriptor) {
                return goodNames.contains(propertyDescriptor.getName());
            }
        });
    }

    /**
     * Create a new set filtering out all property descriptors with names matching any of the specified names.
     *
     * @param in the source set to filter.  Will not be modified.  Required.
     * @param propertyNamesToOmit  zero or more names of properties to filter out of the returned set.
     * @return a new Set that may contain the same or a fewer number of property descriptors.  May be empty but never null.
     */
    public static Set<PropertyDescriptor> omitProperties(Set<PropertyDescriptor> in, String... propertyNamesToOmit) {
        final Set<String> badNames = new HashSet<String>(Arrays.asList(propertyNamesToOmit));
        return grep(new HashSet<PropertyDescriptor>(), in, new Unary<Boolean, PropertyDescriptor>() {
            @Override
            public Boolean call(PropertyDescriptor propertyDescriptor) {
                return !badNames.contains(propertyDescriptor.getName());
            }
        });
    }

    /**
     * Create an instance of the given class backed by the given collection.
     *
     * <p>This is useful if you want to set properties on a set of beans as though
     * they were a single instance.</p>
     *
     * @param interfaceClass The instance class
     * @param instances The instances (must be at least one)
     * @return The proxy instance
     */
    @SuppressWarnings({"unchecked"})
    public static <T> T collectionBackedInstance( final Class<T> interfaceClass,
                                                  final Collection<? extends T> instances ) {
        return (T) Proxy.newProxyInstance( BeanUtils.class.getClassLoader(), new Class[]{interfaceClass}, new InvocationHandler(){
            @Override
            public Object invoke( final Object proxy, final Method method, final Object[] args ) throws Throwable {
                Object result = null;

                for ( T instance : instances ) {
                    result = method.invoke( instance, args );
                    if ( result != null ) {
                        break;                        
                    }
                }

                return result;
            }
        } );
    }
}
