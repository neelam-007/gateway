package com.l7tech.util;

import com.l7tech.util.Functions.*;
import static com.l7tech.util.Functions.reduce;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Represents an optional value, use instead of returning null.
 */
public class Option<T> implements Serializable {

    //- PUBLIC

    /**
     * Create an option from a possibly null value.
     *
     * @param value The value
     * @param <T> The optional type
     * @return The option
     */
    @NotNull
    public static <T> Option<T> optional( @Nullable final T value ) {
        return new Option<T>( value );
    }

    /**
     * Create an option from a non null value.
     *
     * @param value The value
     * @param <T> The optional type
     * @return The option
     */
    @NotNull
    public static <T> Option<T> some( @NotNull final T value ) {
        return new Option<T>( value );
    }

    /**
     * Create an empty option.
     *
     * @param <T> The optional type
     * @return The option
     */
    @NotNull
    public static <T> Option<T> none() {
        return new Option<T>( null );
    }

    /**
     * Create an option from the first value in the given iterable.
     *
     * @param iterable The iterable (may be null)
     * @param <T> The iterable type
     * @return the optional value.
     */
    @NotNull
    public static <T> Option<T> first( @Nullable final Iterable<T> iterable ) {
        T value = null;

        if ( iterable != null ) {
            final Iterator<T> iterator = iterable.iterator();
            if ( iterator.hasNext() ) {
                value = iterator.next();
            }
        }

        return optional( value );
    }

    /**
     * Create an option from the first value in the given array.
     *
     * @param array The array (may be null)
     * @param <T> The array type
     * @return the optional value.
     */
    @NotNull
    public static <T> Option<T> first( @Nullable final T[] array ) {
        return array==null || array.length < 1 ?
                Option.<T>none() :
                optional( array[0] );
    }

    /**
     * Join the given options.
     *
     * @param option The nested option
     * @param <T> The nested optional type
     * @return The joined option
     */
    @NotNull
    public static <T> Option<T> join( @NotNull final Option<Option<T>> option ) {
        return option.isSome() ? option.some() : Option.<T>none();
    }

    /**
     * Does this option have a value?
     *
     * @return true if there is a value
     */
    public boolean isSome() {
        return value != null;
    }

    /**
     * The value of this option if present, or the other value
     *
     * <p>Get the value of this option if there is one, else return the given
     * other value.</p>
     *
     * @param other The other value
     * @return The option or alternative
     */
    @NotNull
    public T orSome( @NotNull final T other ) {
        return value != null ? value : other;
    }

    /**
     * The value of this option if present, or the other value
     *
     * <p>Get the value of this option if there is one, else return value from
     * evaluation of the other function.</p>
     *
     * @param other A function returning the other value
     * @return The option or alternative
     */
    @NotNull
    public T orSome( @NotNull final Nullary<T> other ) {
        return value != null ? value : other.call();
    }

    /**
     * Get the value of this option.
     *
     * @return The value
     * @throws IllegalStateException if there is no value
     */
    @NotNull
    public T some() {
        if ( value == null ) throw new IllegalStateException( "Value is null" );
        return value;
    }

    /**
     * This option if some, else the other option.
     *
     * @param other The alternative option.
     * @return This option or the given alternative.
     */
    public Option<T> orElse( @NotNull Option<T> other ) {
        return value != null ? this : other;
    }

    /**
     * This option if some, else the other option.
     *
     * @param other A function returning the alternative option.
     * @return This option or the given alternative.
     */
    public Option<T> orElse( @NotNull Nullary<Option<T>> other ) {
        return value != null ? this : other.call();
    }

    /**
     * Get the possibly null value of this option.
     *
     * <p>Use of this method should generally be avoided.</p>
     *
     * @return The value
     */
    public T toNull() {
        return value;
    }

    /**
     * Evaluate the given predicate on this options value.
     *
     * @param predicate The predicate to evaluate
     * @return True if a value exists and the predicate returns true.
     */
    public boolean exists( @NotNull Unary<Boolean,? super T> predicate ) {
        return isSome() && predicate.call( some() );
    }

    /**
     * Evaluate the given predicate on this options value.
     *
     * @param predicate The predicate to evaluate
     * @param <E> The exception type
     * @return True if a value exists and the predicate returns true.
     * @throws E if the predicate throws E
     */
    public <E extends Throwable> boolean exists( @NotNull UnaryThrows<Boolean,? super T, E> predicate ) throws E {
        return isSome() && predicate.call( some() );
    }

    /**
     * Filter the optional value with the given function.
     *
     * @param predicate The filtering predicate
     * @return The optional value, none if this option is none or the filter returns false.
     */
    public Option<T> filter( @NotNull Unary<Boolean,? super T> predicate ) {
        final Option<T> filtered;

        if ( isSome() && predicate.call( some() ) ) {
            filtered = this;
        } else {
            filtered = Option.none();
        }

        return filtered;
    }

    /**
     * Evaluate the given effect if this option has a value.
     *
     * @param effect The effect to evaluate.
     */
    public void foreach( @NotNull UnaryVoid<? super T> effect ) {
        if ( isSome() ) {
            effect.call( some() );
        }
    }

    /**
     * Map the given function across this optional value.
     *
     * @param mapper The mapping function
     * @param <O> The result type
     * @return The optional value, none if this option is none or the mapping function returns null.
     */
    public <O> Option<O> map( @NotNull Unary<O,? super T> mapper ) {
        final Option<O> mapped;

        if ( isSome() ) {
            mapped = Option.optional( mapper.call( some() ) );
        } else {
            mapped = Option.none();
        }

        return mapped;
    }

    /**
     * First class map function for options.
     *
     * @param <O1> The type parameter of the first option
     * @param <O2> The type parameter of the second option
     * @return The mapping function.
     */
    public static <O1,O2> Binary<Option<O1>,Option<O2>,Unary<O1,? super O2>> map() {
        return new Binary<Option<O1>,Option<O2>,Functions.Unary<O1,? super O2>>(){
            @Override
            public Option<O1> call( final Option<O2> option,
                                   final Unary<O1, ? super O2> mapper ) {
                return option.map( mapper );
            }
        };
    }

    /**
     * Convert this option to an either.
     *
     * @param other The value to use if this option has no value
     * @param <O> The other type
     * @return An either that contains this value if present, else the given value.
     */
    @NotNull
    public <O> Either<O,T> toEither( @NotNull O other ) {
        return isSome() ?
                Either.<O,T>right( some( ) ) :
                Either.<O,T>left( other );
    }

    /**
     * A list representation of the optional value.
     *
     * @return The empty or single valued list.
     */
    @NotNull
    public List<T> toList() {
        return isSome() ?
                Collections.singletonList( some() ) :
                Collections.<T>emptyList();
    }

    /**
     * Reduce a list of options to a list of values.
     *
     * @param values The values to process
     * @param <O> The option type
     * @return The list of values
     */
    @NotNull
    public static <O> List<O> somes( @NotNull final Iterable<Option<O>> values ) {
        return reduce( values, new ArrayList<O>(), new Binary<ArrayList<O>, ArrayList<O>, Option<O>>() {
            @Override
            public ArrayList<O> call( final ArrayList<O> someValues, final Option<O> option ) {
                if ( option.isSome() ) someValues.add( option.some() );
                return someValues;
            }
        } );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Option option = (Option) o;

        if (value != null ? !value.equals(option.value) : option.value != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return value != null ? value.hashCode() : 0;
    }

    //- PRIVATE

    private final T value;

    public Option( final T value ) {
        this.value = value;
    }
}
