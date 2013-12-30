package com.l7tech.util;

import com.l7tech.util.Functions.Binary;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
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
    public static <T> List<T> list( final T... items ) {
        return Collections.unmodifiableList( Arrays.asList( items ) );
    }

    /**
     * Create an unmodifiable list with the given contents.
     *
     * <p>This will create a copy of the given collection.</p>
     *
     * @param items The items for the list (must not be null)
     * @param <T> The type of the list
     * @return The read-only list
     */
    public static <T> List<T> toList( final Collection<? extends T> items ) {
        return Collections.unmodifiableList( new ArrayList<T>( items ) );
    }

    /**
     * Create an unmodifiable set with the given contents.
     *
     * @param items The items for the set (must not be null)
     * @param <T> The type of the set
     * @return The read-only set
     */
    //TODO [jdk7] @SafeVarargs
    public static <T> Set<T> set( final T... items ) {
        return Collections.unmodifiableSet( new LinkedHashSet<T>( Arrays.asList( items ) ) );
    }

    /**
     * Create an unmodifiable ordered set with the given contents.
     *
     * <p>This will create a copy of the given collection.</p>
     *
     * @param items The items for the set (must not be null)
     * @param <T> The type of the set
     * @return The read-only set
     */
    public static <T> Set<T> toSet( final Collection<? extends T> items ) {
        return Collections.unmodifiableSet( new LinkedHashSet<T>( items ) );
    }

    /**
     * Create an unmodifiable case insensitive set with the given contents.
     *
     * @param items The items for the set (must not be null)
     * @return The read-only case insensitive set
     */
    public static Set<String> caseInsensitiveSet( String... items ) {
        final Set<String> set = new TreeSet<String>( String.CASE_INSENSITIVE_ORDER );
        set.addAll( Arrays.asList( items ) );
        return Collections.unmodifiableSet( set );
    }

    /**
     * Somewhat safely cast the given collection.
     *
     * <p>This will check the erased type of each item in the collection.</p>
     *
     * @param collection The collection to cast
     * @param type The (erased) item type
     * @param collectionType The (erased) collection type
     * @param fallback The fallback value
     * @param <EIT> The erased item type
     * @param <IT> The item type
     * @param <CT> The collection type
     * @param <C> The result type
     * @return The collection or the fallback value
     */
    @SuppressWarnings({ "unchecked" })
    public static <EIT, IT extends EIT,CT extends Collection<?>, C extends Collection<IT>> C cast(
                                                      final Object collection,
                                                      final Class<CT> collectionType,
                                                      final Class<EIT> type,
                                                      final C fallback ) {
        final C result;
        if ( collectionType.isInstance( collection ) ) {
            boolean itemTypesValid = true;
            for ( final Object item : (CT) collection ) {
                if ( item == null || !type.isInstance( item ) ) {
                    itemTypesValid = false;
                    break;
                }
            }
            if ( itemTypesValid ) {
                result = (C) collection;
            } else {
                result = fallback;
            }
        } else {
            result = fallback;
        }

        return result;
    }

    /**
     * Somewhat safely cast the given map.
     *
     * <p>This will check the erased types for the keys/values in the map.</p>
     *
     * @param map The map to cast
     * @param mapType The (erased) type for the map
     * @param keyType The (erased) type for the keys
     * @param valueType The (erased) type for the values
     * @param fallback The fallback map
     * @param <EKT> The erased key type
     * @param <EVT> The erased value type
     * @param <KT> The actual key type
     * @param <VT> The actual value type
     * @param <MT> The erased map type
     * @param <M> The actual map type
     * @return The map or the fallback value
     */
    @SuppressWarnings({ "unchecked" })
    public static <EKT,EVT, KT extends EKT,VT extends EVT,MT extends Map<?,?>,M extends Map<KT,VT>> M cast(
                                                        final Object map,
                                                        final Class<MT> mapType,
                                                        final Class<EKT> keyType,
                                                        final Class<EVT> valueType,
                                                        final M fallback ) {
        final M result;
        if ( mapType.isInstance( map ) ) {
            boolean entryTypesValid = true;
            for ( final Map.Entry<?,?> entry : ((MT) map).entrySet() ) {
                if ( (entry.getKey()==null || !keyType.isInstance( entry.getKey() ) ) ||
                     (entry.getValue()!=null && !valueType.isInstance( entry.getValue() )) ) {
                    entryTypesValid = false;
                    break;
                }
            }
            if ( entryTypesValid ) {
                result = (M) map;
            } else {
                result = fallback;
            }
        } else {
            result = fallback;
        }

        return result;
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

    /**
     * Join sublists into a mutable list.
     *
     * @param collections The collection of collection subclasses of A
     * @param <T> The joined collection element type
     * @return The joined mutable collection (never null)
     */
    public static <T> Collection<T> join( final Collection<? extends Collection<T>> collections ) {
        final List<T> joined = new ArrayList<T>();

        if ( collections != null ) {
            for ( final Collection<T> collection : collections ) {
                if ( collection != null ) {
                    joined.addAll( collection );
                }
            }
        }

        return joined;
    }

    /**
     * Get an iterable for all the given iterables.
     *
     * TODO [jdk7] @SafeVarargs
     *
     * <p>The returned iterable will support "remove" if supported by the given
     * iterables.</p>
     *
     * @param iterables The iterables to iterate
     * @return An iterable that iterates all the given iterables.
     */
    public static <T> Iterable<T> iterable( final Iterable<? extends T>... iterables ) {
        return new Iterable<T>() {
            @Override
            public Iterator<T> iterator() {
                return new Iterator<T>() {
                    private Iterator<? extends T> currentIterator;
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

    /** @return true if any of the target Objects are contained in the collection. */
    public static boolean containsAny( @Nullable final Collection<?> collection,
                                       @Nullable final Collection<?> targets ) {
        if (collection != null && targets != null) {
            for (final Object t : targets) {
                if (collection.contains( t ))
                    return true;
            }
        }
        return false;
    }

    /**
     * Check if any value in the collection matches any target value.
     *
     * @param collection The collection to match (may be null)
     * @param targets The target values (may be null)
     * @param matcher The matcher to user
     * @param <T> The item type
     * @return True if any value matches
     */
    public static <T> boolean matchesAny( @Nullable final Collection<? extends T> collection,
                                          @Nullable final Collection<? extends T> targets,
                                          @NotNull final Binary<Boolean,T,T> matcher ) {
        if (collection != null && targets != null) {
            for (final T t : targets) {
                for ( final T c : collection ) {
                    if ( matcher.call( t, c ) )  {
                        return true;
                    }
                }
            }
        }
        return false;
    }


    /** @return true if all of the target Objects are contained in the collection. */
    public static boolean containsAll( @Nullable final Collection<?> collection,
                                       @Nullable final Collection<?> targets ) {
        if ( collection != null && targets != null ) {
            return collection.containsAll( targets );
        }
        return false;
    }

    /**
     * Get a mutable list containing the values from the given iterable.
     *
     * @param iterable The iterable (may be null)
     * @param <T> The value type
     * @return The mutable list (never null)
     */
    @NotNull
    public static <T> List<T> toList( final Iterable<T> iterable ) {
        final List<T> ret = new ArrayList<T>();
        if ( iterable != null )
            for ( final T t : iterable ) ret.add( t );
        return ret;
    }

    /**
     * This is a sub list call that will never throw IndexOutOfBoundsException. If the from index is greater then the
     * list length the empty list will be returned. If the toIndex is greater then the list length then a list from the
     * from index to the end of the list will be returned. Otherwise the regular subList call will be made.
     *
     * @param list      The list to subList
     * @param fromIndex low endpoint (inclusive) of the subList
     * @param toIndex   high endpoint (exclusive) of the subList
     * @param <T>       The list type
     * @return The subList
     */
    @NotNull
    public static <T> List<T> safeSubList(final List<T> list, int fromIndex, int toIndex) {
        int listSize = list.size();
        return fromIndex > listSize ? Collections.<T>emptyList() : toIndex > listSize ? list.subList(fromIndex, listSize) : list.subList(fromIndex, toIndex);
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
         * Add an optional entry to the map.
         *
         * <p>If either the key or value is missing then the entry will not be
         * added. A duplicate key will overwrite an existing entry.</p>
         *
         * @param key The key to add
         * @param value The value to add.
         * @return This builder.
         */
        public MapBuilder<K,V> put( final Option<K> key,
                                    final Option<V> value ) {
            if ( key.isSome() && value.isSome() ) {
                map.put( key.some(), value.some() );
            }
            return this;
        }

        /**
         * Construct the mutable result map.
         *
         * @return The mutable map.
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
         * @return The read-only map.
         */
        public Map<K,V> unmodifiableMap() {
            return Collections.unmodifiableMap( map );
        }
    }
}
