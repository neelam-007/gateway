package com.l7tech.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public final class CollectionUtils {
    private static final Functions.Unary<String,Object> defaultStringer = new Functions.Unary<String, Object>() {
        @Override
        public String call(Object o) {
            return o.toString();
        }
    };

    private CollectionUtils() { }

    /**
     * Create an unmodifiable list with the given contents.
     *
     * @param items The items for the list (must not be null)
     * @param <T> The type of the list
     * @return The read-only list
     */
    //TODO [jdk7] @SafeVarargs
    public static <T> List<T> list( T... items ) {
        return Collections.unmodifiableList( Arrays.asList( items ) );
    }

    /**
     * Invoke the callback function for each item in the iterable.
     *
     * @param iterable The item to iterate
     * @param callbackForNull True to callback for null values
     * @param callback The callback function
     * @param <T> The iterable type
     */
    public static <T> void foreach( final Iterable<T> iterable,
                                    final boolean callbackForNull,
                                    final Functions.UnaryVoid<? super T> callback) {
        for ( final T item : iterable ) {
            if ( callbackForNull || item != null ) {
                callback.call( item );
            }
        }
    }

    /**
     * Invoke the callback function for each item in the iterable.
     *
     * @param iterable The item to iterate
     * @param callbackForNull True to callback for null values
     * @param callback The callback function
     * @param <T> The iterable type
     * @param <E> The exception type
     */
    public static <T,E extends Throwable> void foreach( final Iterable<T> iterable,
                                    final boolean callbackForNull,
                                    final Functions.UnaryVoidThrows<? super T,E> callback) throws E {
        for ( final T item : iterable ) {
            if ( callbackForNull || item != null ) {
                callback.call( item );
            }
        }
    }

    /**
     * Makes a String representation of the provided Iterable.
     * @param iterable the iterable to traverse
     * @param prefix the prefix to prepend to the result
     * @param delimiter the delimiter to insert between elements of the Iterable
     * @param suffix the suffix to append to the result
     * @param stringer a function that renders an element of the Iterable as a String
     * @return a String representation of the provided Iterable.
     */
    public static <T> String mkString(Iterable<T> iterable, String prefix, String delimiter, String suffix, Functions.Unary<String, ? super T> stringer) {
        StringBuilder sb = new StringBuilder(prefix == null ? "" : prefix);
        for (Iterator<T> it = iterable.iterator(); it.hasNext();) {
            T t = it.next();
            sb.append(stringer.call(t));
            if (it.hasNext()) sb.append(delimiter == null ? "" : delimiter);
        }
        sb.append(suffix == null ? "" : suffix);
        return sb.toString();
    }

    public static <T> String mkString(Iterable<T> iterable, String delimiter, Functions.Unary<String, ? super T> stringer) {
        return mkString(iterable, null, delimiter, null, stringer);
    }

    public static <T> String mkString(Iterable<T> iterable, String delimiter) {
        return mkString(iterable, delimiter, defaultStringer);
    }

    public static <T> String mkString(Iterable<T> iterable, String prefix, String delimiter, String suffix) {
        return mkString(iterable, prefix, delimiter, suffix, defaultStringer);
    }

    public static Set<String> caseInsensitiveSet( String... values ) {
        final Set<String> set = new TreeSet<String>( String.CASE_INSENSITIVE_ORDER );
        set.addAll( Arrays.asList( values ) );
        return set;
    }

    /**
     * Join sublists into a list.
     *
     * @param collections The collection of collection subclasses of A
     * @param <T> The joined collection element type
     * @return The joined collection (never null)
     */
    public static <T> Collection<T> join( final Collection<? extends Collection<T>> collections ) {
        final List<T> joined = new ArrayList<T>();

        for ( final Collection<T> collection : collections ) {
            joined.addAll( collection );
        }

        return joined;
    }

    /**
     * Get an iterable for all the given iterables.
     *
     * @param iterables The iterables to iterate
     * @return An iterable that iterates all the given iterables.
     */
    public static <T> Iterable<T> iterable( final Iterable<T>... iterables ) {
        return new Iterable<T>() {
            @Override
            public Iterator<T> iterator() {
                return new Iterator<T>() {
                    private Iterator<T> currentIterator;
                    private int iterableIndex = 0;

                    @Override
                    public boolean hasNext() {
                        while ( (currentIterator == null || !currentIterator.hasNext()) && iterableIndex < iterables.length ) {
                            currentIterator = iterables[iterableIndex++].iterator();
                        }
                        return currentIterator != null && currentIterator.hasNext();
                    }

                    @Override
                    public T next() {
                        hasNext(); // ensure advance to next iterator if required
                        return currentIterator.next();
                    }

                    @Override
                    public void remove() {
                        currentIterator.remove();
                    }
                };
            }
        };
    }

    /**
     * Create a map builder for a HashMap.
     *
     * @param <K> The key type
     * @param <V> The value type
     * @return The map builder.
     */
    public static <K,V> MapBuilder<K,V> mapBuilder() {
        return MapBuilder.builder();
    }

    /**
     * Create a map builder for a TreeMap.
     *
     * @param <K> The key type
     * @param <V> The value type
     * @return The tree map builder.
     */
    public static <K,V> MapBuilder<K,V> treeMapBuilder() {
        return new MapBuilder<K,V>( new TreeMap<K,V>() );
    }

    /**
     * Builder for maps.
     *
     * <p>Supports construction of mutable and immutable maps.</p>
     *
     * @param <K> The key type
     * @param <V> The value type
     */
    public static final class MapBuilder<K,V> {
        private final Map<K,V> map;

        private MapBuilder( final Map<K,V> map ){
            this.map = map;
        }

        /**
         * Create a map builder for a HashMap.
         *
         * @param <K> The key type
         * @param <V> The value type
         * @return The map builder
         */
        public static <K,V> MapBuilder<K,V> builder() {
            return new MapBuilder<K,V>( new HashMap<K,V>() );
        }

        /**
         * Add an entry to the map.
         *
         * <p>A duplicate key will overwrite an existing entry.</p>
         *
         * @param key The key to add
         * @param value The value to add.
         * @return This builder.
         */
        public MapBuilder<K,V> put( K key, V value ) {
            map.put( key, value );
            return this;
        }

        /**
         * Construct the result map.
         *
         * @return The map.
         */
        public Map<K,V> map() {
            return map;
        }

        /**
         * Construct an immutable result map.
         *
         * <p>Currently the builder will permit modification of the
         * underlying map.</p>
         *
         * @return The map.
         */
        public Map<K,V> unmodifiableMap() {
            return Collections.unmodifiableMap( map );
        }
    }
}
