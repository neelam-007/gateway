package com.l7tech.util;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

/**
 * Plumbing for building simple callbacks that don't deserve to have a special interface created just for them.
 * <p/>
 * There is no NullaryVoid interface provided; for that just use Runnable.
 * <p/>
 * Requires Java 5.
 * @noinspection PublicInnerClass,InterfaceNamingConvention,StaticMethodNamingConvention
 */
public final class Functions {
    private Functions() {}

    /**
     * A function that takes no arguments and returns a value.
     * Provided for symmetry; instead of this, you might consider using the mostly-equivalent but more idiomatic Callable.
     */
    public interface Nullary<R> {
        R call();
    }

    /**
     * A function that takes no arguments and returns no value, like Runnable, but with an Exception.
     */
    public interface NullaryVoidThrows<E extends Throwable> {
        void call() throws E;
    }


    /** A function that takes one argument and returns a value. */
    public interface Unary<R, P1> {
        R call(P1 p1);
    }

    /** A function that takes one argument, returns a value, and may throw one type of exception. */
    public interface UnaryThrows<R, P1, E extends Throwable> {
        R call(P1 p1) throws E;
    }

    /** A function that takes one argument and returns void. */
    public interface UnaryVoid<P1> {
        void call(P1 p1);
    }

    /** A function that takes two arguments and returns a value. */
    public interface Binary<R, P1, P2> {
        R call(P1 p1, P2 p2);
    }

    /** A function that takes two arguments and returns void. */
    public interface BinaryVoid<P1, P2> {
        void call(P1 p1, P2 p2);
    }

    /** A function that takes three arguments and returns a value. */
    public interface Ternary<R, P1, P2, P3> {
        R call(P1 p1, P2 p2, P3 p3);
    }

    /** A function that takes three arguments and returns void. */
    public interface TernaryVoid<P1, P2, P3> {
        void call(P1 p1, P2 p2, P3 p3);
    }

    /** A function that takes four arguments and returns a value. */
    public interface Quaternary<R, P1, P2, P3, P4> {
        R call(P1 p1, P2 p2, P3 p3, P4 p4);
    }

    /** A function that takes four arguments and returns void. */
    public interface QuaternaryVoid<P1, P2, P3, P4> {
        void call(P1 p1, P2 p2, P3 p3, P4 p4);
    }

    /** A funciton that takes five arguments and returns a value. */
    public interface Quinary<R, P1, P2, P3, P4, P5> {
        R call(P1 p1, P2 p2, P3 p3, P4 p4, P5 p5);
    }

    /** A function that takes five arguments and returns void. */
    public interface QuinaryVoid<P1, P2, P3, P4, P5> {
        void call(P1 p1, P2 p2, P3 p3, P4 p4, P5 p5);
    }

    /** A function that takes six arguments and returns a value. */
    public interface Sestary<R, P1, P2, P3, P4, P5, P6> {
        R call(P1 p1, P2 p2, P3 p3, P4 p4, P5 p5, P6 p6);
    }

    /** A function that takes six arguments and returns void. */
    public interface SestaryVoid<P1, P2, P3, P4, P5, P6> {
        void call(P1 p1, P2 p2, P3 p3, P4 p4, P5 p5, P6 p6);
    }

    // That's probably plenty ridiculous enough; past that, can switch to Variadic

    /** A function that takes any number of arguments and returns a value. */
    public interface Variadic<R, P> {
        R call(P ... args);
    }

    /** A function that takes any number of arguments and returns void. */
    public interface VariadicVoid<P> {
        void call(P ... args);
    }

    /**
     * Transforms a collection of items by applying a map function to each input item
     * to obtain a corresponding output item.
     *
     * @param in a collection of input items
     * @param transformation a transformation to apply to each input item to produce the corresponding output item
     * @return a collection of output items
     */
    public static <I,O> List<O> map(Iterable<I> in, Unary<O,I> transformation) {
        List<O> ret = new ArrayList<O>();
        for (I i : in)
            ret.add(transformation.call(i));
        return ret;
    }

    /**
     * Transforms an Iterator by applying a map function to each item
     * to obtain a corresponding output item.
     *
     * @param in an iterator of input items
     * @param transformation a transformation to apply to each input item to produce the corresponding output item
     * @return an iterator of output items
     */
    public static <I,O> Iterator<O> map(final Iterator<I> in, final Unary<O,I> transformation) {
        return new Iterator<O>(){
            public boolean hasNext() { return in.hasNext(); }
            public O next() { return transformation.call(in.next()); }
            public void remove() { in.remove(); }
        };
    }

    /**
     * Search the specified collection for the first object matching predicate.
     *
     * @param in the collection to search
     * @param predicate the predicate to match
     * @return the first matching object, or null if no match was found
     */
    public static <I> I grepFirst(Iterable<I> in, Unary<Boolean, I> predicate) {
        for (I i : in) {
            if (predicate.call(i))
                return i;
        }
        return null;
    }

    /**
     * Returns <code>true</code> if any element matches the predicate, <code>false</code> otherwise.
     *  
     * @param in the iterable to search
     * @param predicate the predicate to evaluate
     * @param <I> the type of element in the iterable
     * @return <code>true</code> if any element matches the predicate, <code>false</code> otherwise.
     */
    public static <I> boolean exists(Iterable<I> in, Unary<Boolean, I> predicate) {
        return grepFirst(in, predicate) != null;
    }

    /**
     * Produce a filtered list of all objects matching the predicate.
     *
     * @param in a collection of items to filter
     * @param predicate the filter to apply
     * @return a list of all input objects that matched the filter.  may be empty, but will never be null.
     */
    public static <I> List<I> grep(Iterable<I> in, Unary<Boolean, I> predicate) {
        return grep(new ArrayList<I>(), in, predicate);
    }

    /**
     * Filter all objects matching the predicate into the specified collection.
     *
     * @param out  a container in which to collect matching values.  Required.
     * @param in   a collection of values to match.  Required.
     * @param predicate the filter to apply
     * @return a reference to the out container.
     */
    public static <T,C extends Collection<T>> C grep(C out, Iterable<T> in, Unary<Boolean, T> predicate) {
        for (T i : in) {
            if (predicate.call(i))
                out.add(i);
        }
        return out;
    }

    /**
     * Convenience method that filters all null elements out of a list.
     *
     * @param in an Iterable to filter.  Required.
     * @return a new ArrayList containing all non-null elements from the Iterable.  Never null but may be empty.
     */
    public static <I> List<I> grepNotNull(Iterable<I> in) {
        return grep(new ArrayList<I>(), in, new Unary<Boolean, I>() {
            public Boolean call(I i) {
                return i != null;
            }
        });
    }

    /**
     * Return a new predicate which negates an existing predicate.
     *
     * @param predicate the predicate to negate.  Required.
     * @return a new predicate that returns true in place of false and vice versa
     */
    public static <T> Unary<Boolean,T> negate(final Unary<Boolean,T> predicate) {
        return new Unary<Boolean, T>() {
            public Boolean call(T t) {
                return !predicate.call(t);
            }
        };
    }

    /**
     * Reduce a collection of items to a single summary item using the specified reduction function and initial
     * value.
     *
     * @param in a collection to reduce.
     * @param initial the initial value to use as the left hand argument to the first call to reduction
     * @param reduction the reduction to apply
     * @return the result of applying the reduction to each item in the collection
     */
    public static <I,O> O reduce(Iterable<I> in, O initial, Binary<O, O, I> reduction) {
        O ret = initial;
        for (I i : in)
            ret = reduction.call(ret, i);
        return ret;
   }

    /**
     * A convenience wrapper for {@link Collections#sort(java.util.List)} that always
     * makes a copy of the collection before sorting it.
     *
     * @param collection the collection to sort
     * @return a copy of the collection that has been sorted
     */
    public static <T extends Comparable<? super T>> List<T> sort(Collection<T> collection) {
        List<T> copy = new ArrayList<T>(collection);
        Collections.sort(copy);
        return copy;
    }

    /**
     * A convenience wrapper for {@link Collections#sort(java.util.List, java.util.Comparator)}
     * that always makes a copy of the collection before
     * sorting it.
     *
     * @param collection the source collection.  It will not be modified by this method.
     * @param comparator the comparator to use when sorting the collection.
     * @return a new copy of the collection, sorted using the specified comparator.
     */
    public static <T> List<T> sort(Collection<T> collection, Comparator<? super T> comparator) {
        List<T> copy = new ArrayList<T>(collection);
        Collections.sort(copy, comparator);
        return copy;
    }

    /**
     * Convert any non-void nullary method (ie, Component.getWidth()) into a transformation that will,
     * when passed an instance (ie, of Component) return the result of invoking the method on that
     * instance (ie, its width).
     *
     * @param getter a nullary, non-void method of some class.  Required.
     *               If this is a static method the resulting transform will ignore any objects passed into it.
     * @return a Unary that takes an instance of that class and returns the result of invoking the getter on it.  Never null
     */
    public static <OUT,IN> Unary<OUT,IN> getterTransform(final Method getter) {
        return new Unary<OUT,IN>() {
            public OUT call(IN in) {
                try {
                    //noinspection unchecked
                    return (OUT)getter.invoke(in);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException("Unable to invoke method " + getter.getName() + ": " + ExceptionUtils.getMessage(e), e);
                } catch (InvocationTargetException e) {
                    throw new RuntimeException("Unable to invoke method " + getter.getName() + ": " + ExceptionUtils.getMessage(e), e);
                }
            }
        };
    }

    /**
     * Create a transformation that will, when passed an instance of some class (ie, Component)
     * return the value of that instances property (ie, "width").
     * <p/>
     * Creating a transform may be quite slow (as it may need to load BeanInfo),
     * but invoking the resulting transform should usually only be slightly slow (ie as fast as any reflective
     * method invocation).
     *
     * @param clazz     the class whose instances are to be transformed (ie, Component).  Required
     * @param property  the property the transform should produce (ie, "width").  Required.
     * @return a Unary that, when passed an instance of the class, will return the corresponding property.
     */
    public static <OUT,IN> Unary<OUT,IN> propertyTransform(Class<IN> clazz, String property) {
        try {
            PropertyDescriptor[] props = Introspector.getBeanInfo(clazz).getPropertyDescriptors();
            for (PropertyDescriptor prop : props)
                if (prop.getName().equals(property))
                    return getterTransform(prop.getReadMethod());
            throw new IllegalArgumentException("Class " + clazz + " has no property named " + property);
        } catch (IntrospectionException e) {
            throw new IllegalArgumentException("Unable to instrospect class " + clazz + ": " + ExceptionUtils.getMessage(e));
        }
    }
}
