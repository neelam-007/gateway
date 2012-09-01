package com.l7tech.util;

import org.jetbrains.annotations.NotNull;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Plumbing for building simple callbacks that don't deserve to have a special interface created just for them.
 * <p/>
 * There is no NullaryVoid interface provided; for that just use Runnable.
 * <p/>
 * Requires Java 5.
 * @noinspection PublicInnerClass,InterfaceNamingConvention,StaticMethodNamingConvention
 */
public final class Functions {
    static TimeSource timeSource = new TimeSource();
    private Functions() {}

    /**
     * A function that takes no arguments and returns a value.
     * Provided for symmetry; instead of this, you might consider using the mostly-equivalent but more idiomatic Callable.
     */
    public interface Nullary<R> {
        R call();
    }

    /**
     * A function that takes no arguments, returns a value, and may throw an exception  
     */
    public interface NullaryThrows<R, E extends Throwable> {
        R call() throws E;
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

    /** A function that takes one argument, returns void and may throw one type of exception. */
    public interface UnaryVoidThrows<P1, E extends Throwable> {
        void call(P1 p1) throws E;
    }

    /** A function that takes two arguments and returns a value. */
    public interface Binary<R, P1, P2> {
        R call(P1 p1, P2 p2);
    }

    /** A function that takes two arguments and returns a value, and may throw one type of exception.  */
    public interface BinaryThrows<R, P1, P2, E extends Throwable> {
        R call(P1 p1, P2 p2) throws E;
    }

    /** A function that takes two arguments and returns void. */
    public interface BinaryVoid<P1, P2> {
        void call(P1 p1, P2 p2);
    }

    /** A function that takes two arguments, returns void and may throw one type of exception. */
    public interface BinaryVoidThrows<P1, P2, E extends Throwable> {
        void call(P1 p1, P2 p2) throws E;
    }

    /** A function that takes three arguments and returns a value. */
    public interface Ternary<R, P1, P2, P3> {
        R call(P1 p1, P2 p2, P3 p3);
    }

    /** A function that takes three arguments, returns a value and may throw one type of exception. */
    public interface TernaryThrows<R, P1, P2, P3, E extends Throwable> {
        R call(P1 p1, P2 p2, P3 p3) throws E;
    }

    /** A function that takes three arguments and returns void. */
    public interface TernaryVoid<P1, P2, P3> {
        void call(P1 p1, P2 p2, P3 p3);
    }

    /** A function that takes three arguments, returns void and may throw one type of exception */
    public interface TernaryVoidThrows<P1, P2, P3, E extends Throwable> {
        void call(P1 p1, P2 p2, P3 p3) throws E;
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
     * Partially apply the given function.
     *
     * @param unary The unary function
     * @param param1 The first parameter
     * @param <R> The return type
     * @param <P1> The first parameter type
     * @return the nullary function
     */
    public static <R,P1> Nullary<R> partial( final Unary<R,P1> unary,
                                             final P1 param1 ) {
        return new Nullary<R>() {
            @Override
            public R call() {
                return unary.call( param1 );
            }
        };
    }

    /**
     * Partially apply the given function.
     *
     * @param unary The unary function
     * @param param1 The first parameter
     * @param <R> The return type
     * @param <P1> The first parameter type
     * @param <T> The exception type
     * @return the nullary function
     */
    public static <R,P1,T extends Throwable> NullaryThrows<R,T> partial( final UnaryThrows<R,P1,T> unary,
                                                                         final P1 param1 ) throws T {
        return new NullaryThrows<R,T>() {
            @Override
            public R call() throws T {
                return unary.call( param1 );
            }
        };
    }

    /**
     * Partially apply the given function.
     *
     * @param unary The unary function
     * @param param1 The first parameter
     * @param <P1> The first parameter type
     * @param <T> The exception type
     * @return the nullary function
     */
    public static <P1,T extends Throwable> NullaryVoidThrows<T> partial( final UnaryVoidThrows<P1,T> unary,
                                                                         final P1 param1 ) throws T {
        return new NullaryVoidThrows<T>() {
            @Override
            public void call() throws T {
                unary.call( param1 );
            }
        };
    }

    /**
     * Partially apply the given function.
     *
     * @param binary The binary function.
     * @param param1 The first parameter
     * @param <R> The return type
     * @param <P1> The first parameter type
     * @param <P2> The second parameter type
     * @return the unary function.
     */
    public static <R, P1,P2> Unary<R,P2> partial( final Binary<R,P1,P2> binary,
                                                  final P1 param1 ) {
        return new Unary<R,P2>() {
            @Override
            public R call( final P2 param2 ) {
                return binary.call( param1,  param2);
            }
        };
    }

    /**
     * Partially apply the given function.
     *
     * @param binary The binary function.
     * @param param1 The first parameter
     * @param <P1> The first parameter type
     * @param <P2> The second parameter type
     * @return the unary function.
     */
    public static <R,P1,P2,T extends Throwable> UnaryThrows<R,P2,T> partial( final BinaryThrows<R,P1,P2,T> binary,
                                                                             final P1 param1 ) {
        return new UnaryThrows<R,P2,T>() {
            @Override
            public R call( final P2 param2 ) throws T {
                return binary.call( param1,  param2);
            }
        };
    }

    /**
     * Partially apply the given function.
     *
     * @param binary The binary function.
     * @param param1 The first parameter
     * @param <P1> The first parameter type
     * @param <P2> The second parameter type
     * @return the unary function.
     */
    public static <P1,P2> UnaryVoid<P2> partial( final BinaryVoid<P1,P2> binary,
                                                 final P1 param1 ) {
        return new UnaryVoid<P2>() {
            @Override
            public void call( final P2 param2 ) {
                binary.call( param1,  param2);
            }
        };
    }

    /**
     * Partially apply the given function.
     *
     * @param binary The binary function.
     * @param param1 The first parameter
     * @param <P1> The first parameter type
     * @param <P2> The second parameter type
     * @return the unary function.
     */
    public static <P1,P2,T extends Throwable> UnaryVoidThrows<P2,T> partial( final BinaryVoidThrows<P1,P2,T> binary,
                                                                             final P1 param1 ) {
        return new UnaryVoidThrows<P2,T>() {
            @Override
            public void call( final P2 param2 ) throws T {
                binary.call( param1,  param2);
            }
        };
    }

    /**
     * Partially apply the given function.
     *
     * @param binary The binary function.
     * @param param1 The first parameter
     * @param param2 The second parameter
     * @param <R> The return type
     * @param <P1> The first parameter type
     * @param <P2> The second parameter type
     * @return the unary function.
     */
    public static <R, P1,P2> Nullary<R> partial( final Binary<R,P1,P2> binary,
                                                 final P1 param1,
                                                 final P2 param2 ) {
        return new Nullary<R>() {
            @Override
            public R call() {
                return binary.call( param1,  param2);
            }
        };
    }

    /**
     * Partially apply the given function.
     *
     * @param binary The binary function.
     * @param param1 The first parameter
     * @param param2 The second parameter
     * @param <P1> The first parameter type
     * @param <P2> The second parameter type
     * @return the unary function.
     */
    public static <R,P1,P2,T extends Throwable> NullaryThrows<R,T> partial( final BinaryThrows<R,P1,P2,T> binary,
                                                                            final P1 param1,
                                                                            final P2 param2 ) {
        return new NullaryThrows<R,T>() {
            @Override
            public R call() throws T {
                return binary.call( param1,  param2 );
            }
        };
    }

    /**
     * Partially apply the given function.
     *
     * @param binary The binary function.
     * @param param1 The first parameter
     * @param param2 The second parameter
     * @param <P1> The first parameter type
     * @param <P2> The second parameter type
     * @return the unary function.
     */
    public static <P1,P2,T extends Throwable> NullaryVoidThrows<T> partial( final BinaryVoidThrows<P1,P2,T> binary,
                                                                            final P1 param1,
                                                                            final P2 param2 ) {
        return new NullaryVoidThrows<T>() {
            @Override
            public void call() throws T {
                binary.call( param1,  param2);
            }
        };
    }

    /**
     * Return a function that returns the given value.
     *
     * @param value The value to return.
     * @param <R> The value.
     * @return A function returning the value.
     */
    public static <R> Nullary<R> nullary( final R value ) {
        return new Nullary<R>() {
            @Override
            public R call() {
                return value;
            }
        };
    }

    /**
     * Return an identity transform.
     *
     * @param <T> The type
     * @return An identity transform
     */
    public static <T> Unary<T,T> identity() {
        return new Unary<T, T>() {
            @Override
            public T call( final T t ) {
                return t;
            }
        };
    }

    /**
     * Transform an iterable into a Map by applying a transform function to each value in the iterable.
     *
     * Allows any iterable to be turned into a map of any key / value type.
     *
     * @param in iterable to map
     * @param transformation key function to apply to each item in the iterable
     * @param <K> Key type
     * @param <V> Value type
     * @param <I> Iterable type
     * @return Map of the input transformed into a map
     */
    public static <K, V, I> Map<K, V> toMap(Iterable<I> in, Unary<Pair<K, V>, I> transformation) {

        final Map<K, V> map = new HashMap<K, V>();

        for (I i : in) {
            final Pair<K, V> pair = transformation.call(i);
            map.put(pair.getKey(), pair.getValue());
        }

        return map;
    }

    /**
     * Transforms a collection of items by applying a map function to each input item
     * to obtain a corresponding output item.
     *
     * @param in a collection of input items
     * @param transformation a transformation to apply to each input item to produce the corresponding output item
     * @return a collection of output items
     */
    public static <I,O> List<O> map(Iterable<I> in, Unary<O,? super I> transformation) {
        List<O> ret = new ArrayList<O>();
        for (I i : in)
            ret.add(transformation.call(i));
        return ret;
    }

    /**
     * Transforms a collection of items by applying a map function to each input item
     * to obtain a corresponding output item.
     *
     * @param in a collection of input items
     * @param transformation a transformation to apply to each input item to produce the corresponding output item
     * @return a collection of output items
     * @throws E if the transform function throws E
     */
    public static <I,O,E extends Throwable> List<O> map( final Iterable<I> in,
                                                         final UnaryThrows<O,? super I,E> transformation ) throws E {
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
    public static <I,O> Iterator<O> map(final Iterator<I> in, final Unary<O,? super I> transformation) {
        return new Iterator<O>(){
            @Override
            public boolean hasNext() { return in.hasNext(); }
            @Override
            public O next() { return transformation.call(in.next()); }
            @Override
            public void remove() { in.remove(); }
        };
    }

    /**
     * Transforms a Map by applying a map function to each value.
     *
     * @param map The map the transform
     * @param outputMap The output map (optional)
     * @param keyTransformation The transformation for keys
     * @param valueTransformation The transformation for values
     * @param <KI> The input key type
     * @param <KO> The output key type
     * @param <VI> The input value type
     * @param <VO> The output value type
     * @return A transformed map
     */
    public static <KI,KO,VI,VO> Map<KO,VO> map( final Map<KI,VI> map,
                                                final Map<KO,VO> outputMap,
                                                final Unary<KO,? super KI> keyTransformation,
                                                final Unary<VO,? super VI> valueTransformation ) {
        final Map<KO,VO> out = outputMap == null ? new HashMap<KO, VO>() : outputMap;

        if ( map != null ) {
            for ( final Entry<KI, VI> entry : map.entrySet() ) {
                out.put(
                        keyTransformation.call( entry.getKey() ),
                        valueTransformation.call( entry.getValue() ) );
            }
        }

        return out;
    }

    /**
     * Transforms, flattens, or filters a collection by applying a map function to each value.
     *
     * @param in a collection of input items
     * @param transformation a transformation to apply to each input item to produce the corresponding output item.  all elements returned by the transformation
     *                       will be added to the output.  A null return from the transformation will be treated the same as an empty Iterable.
     * @param <I> the item input type
     * @param <O> the item output type
     * @param <E> an exception type that may be thrown by the mapper
     * @return a collection of output items.  never null
     * @throws E if the mapper throws an exception
     */
    @NotNull
    public static <I,O,E extends Throwable> List<O> flatmap( final Iterable<I> in,
                                                             final UnaryThrows<Iterable<O>,? super I,E> transformation ) throws E {
        List<O> ret = new ArrayList<O>();
        for (I i : in) {
            Iterable<O> mapped = transformation.call(i);
            if (mapped != null) for (O o : mapped) {
                ret.add(o);
            }
        }
        return ret;
    }

    /**
     * Search the specified collection for the first object matching predicate.
     *
     * @param in the collection to search
     * @param predicate the predicate to match
     * @return the first matching object, or null if no match was found
     */
    public static <I> I grepFirst(Iterable<? extends I> in, Unary<Boolean, ? super I> predicate) {
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
    public static <I> boolean exists(@NotNull Iterable<I> in, @NotNull Unary<Boolean, I> predicate) {
        return grepFirst(in, predicate) != null;
    }

    /**
     * Returns <code>true</code> if all elements match the predicate, <code>false</code> otherwise.
     *
     * @param in the iterable to search
     * @param predicate the predicate to evaluate
     * @param <I> the type of element in the iterable
     * @return <code>true</code> if all elements matche the predicate, <code>false</code> otherwise.
     */
    public static <I> boolean forall(@NotNull Iterable<I> in, @NotNull Unary<Boolean, ? super I> predicate) {
        for (I i : in) {
            if (!predicate.call(i))
                return false;
        }
        return true;
    }

    /**
     * Produce a filtered list of all objects matching the predicate.
     *
     * @param in a collection of items to filter
     * @param predicate the filter to apply
     * @return a list of all input objects that matched the filter.  may be empty, but will never be null.
     */
    public static <I> List<I> grep(Iterable<I> in, Unary<Boolean, ? super I> predicate) {
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
    public static <T,C extends Collection<T>> C grep(C out, Iterable<T> in, Unary<Boolean, ? super T> predicate) {
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
            @Override
            public Boolean call(I i) {
                return i != null;
            }
        });
    }

    /**
     * Function chaining.
     *
     * <p>Create a function that evaluates the given functions in order,
     * passing the result of the first function to the second function.</p>
     *
     * @param first The first function to evaluate.
     * @param second The second function to evaluate.
     * @param <R1> The return type of the first function
     * @param <A1> The argument type of the first function
     * @param <R2> The return type of the second function
     * @return The combined function.
     */
    @NotNull
    public static <R1, A1, R2> Unary<R2, A1> then( @NotNull final Unary<R1, A1> first,
                                                   @NotNull final Unary<R2,? super R1> second ) {
        return new Unary<R2, A1>(){
            @Override
            public R2 call( final A1 a1 ) {
                return second.call( first.call( a1 ) );
            }
        };
    }

    /**
     * Return an equality predicate, based on a transform and value.
     *
     * @param transform The transform used to obtain a value
     * @param value The value to compare against.  Required.
     * @param <T> The original type (e.g. the collection type)
     * @param <PT> The value (property) type
     * @return A new predicate that returns true if the property of the given item matches
     */
    public static <T,PT> Unary<Boolean,T> equality( final Unary<PT,T> transform,
                                                    final PT value ) {
        return new Unary<Boolean,T>(){
            @Override
            public Boolean call( final T t ) {
                return value.equals( transform.call( t ) );
            }
        };
    }

    /**
     * Return a new predicate which negates an existing predicate.
     *
     * @param predicate the predicate to negate.  Required.
     * @return a new predicate that returns true in place of false and vice versa
     */
    public static <T> Unary<Boolean,T> negate(final Unary<Boolean,T> predicate) {
        return new Unary<Boolean, T>() {
            @Override
            public Boolean call(T t) {
                return !predicate.call(t);
            }
        };
    }

    /**
     * Predicate combination with logical AND.
     *
     * @param predicate1 The first predicate
     * @param predicate2 The second predicate
     * @param <T> The predicate argument type
     * @return A function that calls the given predicates
     */
    public static <T> Unary<Boolean,T> and( final Unary<Boolean,? super T> predicate1,
                                            final Unary<Boolean,? super T> predicate2 ) {
        return new Unary<Boolean,T>(){
            @Override
            public Boolean call( final T t ) {
                return predicate1.call( t ) && predicate2.call( t );
            }
        };
    }

    /**
     * Predicate combination with logical OR.
     *
     * @param predicate1 The first predicate
     * @param predicate2 The second predicate
     * @param <T> The predicate argument type
     * @return A function that calls the given predicates
     */
    public static <T> Unary<Boolean,T> or( final Unary<Boolean,? super T> predicate1,
                                           final Unary<Boolean,? super T> predicate2 ) {
        return new Unary<Boolean,T>(){
            @Override
            public Boolean call( final T t ) {
                return predicate1.call( t ) || predicate2.call( t );
            }
        };
    }

    /**
     * Memoize the given nullary.
     *
     * <p>This will memoize nulls. For single threaded use only.</p>
     *
     * @param nullary The function to memoize
     * @param <R> The return type
     * @return A memoized version of the nullary
     */
    @NotNull
    public static <R> Nullary<R> memoize( @NotNull final Nullary<R> nullary ) {
        return new Nullary<R>(){
            private Nullary<R> memo;
            @Override
            public R call() {
                return (memo != null ? memo : (memo = nullary( nullary.call() ))).call();
            }
        };
    }

    /**
     * Cache the given nullary for the specified time.
     *
     * <p>WARNING: This implementation permits multiple calls to the underlying
     * nullary if invoked concurrently when a the cached result expires (or is
     * first initialized)</p>
     *
     * @param nullary The function to cache
     * @param <R> The return type
     * @return A cached version of the nullary
     * @see CachedCallable as an alternative for Callable implementations
     */
    @NotNull
    public static <R> Nullary<R> cached( @NotNull final Nullary<R> nullary,
                                         final long maxAge ) {
        return new Nullary<R>(){
            private volatile Pair<Long,R> cached;
            @Override
            public R call() {
                final Pair<Long,R> cached = this.cached;
                final long timeNow = timeSource.currentTimeMillis();
                return (cached != null && cached.left+maxAge>=timeNow ?
                        cached :
                        (this.cached = new Pair<Long, R>( timeNow, nullary.call() ) )).right;
            }
        };
    }

    /**
     * Cache the given unary for the specified time.
     *
     * <p>The caller is reponsible for ensuring that the parameter type is
     * suitable for use as a map key (such as a String)</p>
     *
     * <p>WARNING: This implementation permits multiple calls to the underlying
     * unary if invoked concurrently when a cached entry expires (or is first
     * initialized)</p>
     *
     * @param unary The function to cache
     * @param <R> The return type
     * @param <P1> The parameter type
     * @return A cached version of the unary
     */
    @NotNull
    public static <R,P1> Unary<R,P1> cached( @NotNull final Unary<R,P1> unary,
                                             final long maxAge ) {
        return new Unary<R,P1>(){
            private Map<P1,Nullary<R>> cache = new ConcurrentHashMap<P1, Nullary<R>>();
            @Override
            public R call( final P1 p1 ) {
                return (cache.containsKey(p1) ?
                        cache.get( p1 ) :
                        put( p1, cached( partial( unary, p1 ), maxAge ) )).call();
            }
            private Nullary<R> put( final P1 p1,
                                    final Nullary<R> partialUnary ) {
                cache.put( p1, partialUnary );
                return partialUnary;
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
    public static <I,O> O reduce(Iterable<? extends I> in, O initial, Binary<O, O, I> reduction) {
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
            @Override
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
     * @throws IllegalArgumentException if the specified class cannot be introspected, or does not have a property with the specified name.
     */
    public static <OUT,IN> Unary<OUT,IN> propertyTransform(Class<IN> clazz, String property) {
        try {
            PropertyDescriptor[] props = Introspector.getBeanInfo(clazz).getPropertyDescriptors();
            for (PropertyDescriptor prop : props)
                if (prop.getName().equals(property))
                    return getterTransform(prop.getReadMethod());
            throw new IllegalArgumentException("Class " + clazz + " has no property named " + property);
        } catch (IntrospectionException e) {
            throw new IllegalArgumentException("Unable to instrospect class " + clazz + ": " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
        }
    }

    public static final Unary<Boolean,Object> FUNC_IS_NULL = new Unary<Boolean,Object>() {
            @Override
            public Boolean call( final Object object ) {
                return null == object;
            }
        };

    public static final Unary<Boolean, Object> FUNC_IS_NOT_NULL = negate(FUNC_IS_NULL);

}
