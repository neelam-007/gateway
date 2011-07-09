package com.l7tech.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

/**
 * Represents an optional value, use instead of returning null.
 */
public class Option<T> {

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
     * @return The option or alternative
     */
    @NotNull
    public T orSome( @NotNull final T other ) {
        return value != null ? value : other;
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
     * Filter the optional value with the given function.
     *
     * @param filter The filter function
     * @return The optional value, none if this optoin is none or the filter returns false.
     */
    public Option<T> filter( @NotNull Functions.Unary<Boolean,? super T> filter ) {
        final Option<T> filtered;

        if ( isSome() && filter.call( some() ) ) {
            filtered = this;
        } else {
            filtered = Option.none();
        }

        return filtered;
    }

    /**
     * Map the given function across this optional value.
     *
     * @param mapper The mapping function
     * @param <O> The result type
     * @return The optional value, none if this option is none or the mapping function returns null.
     */
    public <O> Option<O> map( @NotNull Functions.Unary<O,? super T> mapper ) {
        final Option<O> mapped;

        if ( isSome() ) {
            mapped = Option.optional( mapper.call( some() ) );
        } else {
            mapped = Option.none();
        }

        return mapped;
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

    //- PRIVATE

    private final T value;

    public Option( final T value ) {
        this.value = value;
    }
}
