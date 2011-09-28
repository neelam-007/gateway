package com.l7tech.util;

import com.l7tech.util.Functions.Unary;
import static com.l7tech.util.Option.none;
import static com.l7tech.util.Option.some;
import static com.l7tech.util.TextUtils.trim;

import java.io.Serializable;

/**
 * Utility for type conversions
 */
public class ConversionUtils {

    //- PUBLIC

    /**
     * Get an identity (noop) converter.
     *
     * @param <T> The source and target type
     * @return The converter
     */
    @SuppressWarnings({ "unchecked" })
    public static <T> Unary<Option<T>,T> getIdentityConverter() {
        return (Unary<Option<T>, T>) identityConverter;
    }

    /**
     * Get a text to integer converter.
     *
     * @return The converter
     */
    public static Unary<Option<Integer>,String> getTextToIntegerConverter() {
        return integerConverter;
    }

    /**
     * Get a text to long converter.
     *
     * @return The converter
     */
    public static Unary<Option<Long>,String> getTextToLongConverter() {
        return longConverter;
    }

    /**
     * Get a text to integer converter for TimeUnit format.
     *
     * @return The converter
     */
    public static Unary<Option<Long>,String> getTimeUnitTextToLongConverter() {
        return timeUnitConverter;
    }

    //- PRIVATE

    private static abstract class TextConverter<T> implements Serializable, Unary<Option<T>,String> {}
    private static abstract class Converter<T> implements Serializable, Unary<Option<T>,T> {}

    private static final Converter<?> identityConverter = new Converter() {
        @Override
        public Option<Object> call( final Object value ) {
            return some( value );
        }
    };

    private static final TextConverter<Integer> integerConverter = new TextConverter<Integer>() {
        @Override
        public Option<Integer> call( final String text ) {
            try {
                return some( Integer.parseInt( trim( text ) ) );
            } catch ( final NumberFormatException e ) {
                return none();
            }
        }
    };

    private static final TextConverter<Long> longConverter = new TextConverter<Long>() {
        @Override
        public Option<Long> call( final String text ) {
            try {
                return some( Long.parseLong( trim( text ) ) );
            } catch ( final NumberFormatException e ) {
                return none();
            }
        }
    };

    private static final TextConverter<Long> timeUnitConverter = new TextConverter<Long>() {
        @Override
        public Option<Long> call( final String text ) {
            try {
                return some( TimeUnit.parse( trim( text ), TimeUnit.MINUTES, true ) );
            } catch ( final NumberFormatException e ) {
                return none();
            }
        }
    };
}

