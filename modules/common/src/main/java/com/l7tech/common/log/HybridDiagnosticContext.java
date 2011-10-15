package com.l7tech.common.log;

import static com.l7tech.util.CollectionUtils.join;
import static com.l7tech.util.CollectionUtils.toList;
import com.l7tech.util.Functions;
import com.l7tech.util.Functions.Nullary;
import static com.l7tech.util.Functions.map;
import static java.util.Collections.singleton;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Nested/Mapped diagnostic context.
 *
 * <p>You can add/remove keys as with a mapped diagnostic context, at runtime
 * keys for a given nesting are cleared when that context is popped.</p>
 */
public class HybridDiagnosticContext {

    //- PUBLIC

    /**
     * Perform a callback with a nested diagnostic context.
     *
     * <p>This should be used for contexts that can be nested such as a call
     * stack. It should not be used for keys that are single valued, such as
     * a thread identifier.</p>
     *
     * @param name The context key (required)
     * @param value The context value (required)
     * @param callback The callback to run in the context (required)
     * @param <R> The return type
     * @return The value returned by callback
     */
    public static <R> R doInContext( @NotNull final String name,
                                     @NotNull final String value,
                                     @NotNull final Nullary<R> callback ) {
        return doInContext(
                Collections.<String,Collection<String>>singletonMap( name, singleton( value ) ),
                callback );
    }

    /**
     * Perform a callback with a nested diagnostic context.
     *
     * <p>This should be used for contexts that can be nested such as a call
     * stack. It should not be used for keys that are single valued, such as
     * a thread identifier.</p>
     *
     * @param name The context key (required)
     * @param values The context values (required)
     * @param callback The callback to run in the context (required)
     * @param <R> The return type
     * @return The value returned by callback
     */
    public static <R> R doInContext( @NotNull final String name,
                                     @NotNull final Collection<String> values,
                                     @NotNull final Nullary<R> callback ) {
        return doInContext( Collections.singletonMap( name, values ), callback );
    }

    /**
     * Perform a callback with a nested diagnostic context.
     *
     * <p>This should be used for contexts that can be nested such as a call
     * stack. It should not be used for keys that are single valued, such as
     * a thread identifier.</p>
     *
     * @param contextMap The map of context keys to (multiple) context values (required)
     * @param callback The callback to run in the context (required)
     * @param <R> The return type
     * @return The value returned by callback
     */
    public static <R> R doInContext( @NotNull final Map<String,Collection<String>> contextMap,
                                     @NotNull final Nullary<R> callback ) {
        final Map<String,List<String>> immutableContext = immutable( contextMap );
        try {
            for( final Map.Entry<String,List<String>> contextEntry : immutableContext.entrySet() ) {
                context.get().push( contextEntry.getKey(), contextEntry.getValue() );
            }
            return callback.call();
        } finally {
            for( final Map.Entry<String,List<String>> contextEntry : immutableContext.entrySet() ) {
                context.get().pop( contextEntry.getKey(), contextEntry.getValue() );
            }
        }
    }

    /**
     * Perform an action using the saved context.
     *
     * <p>If the given context is null then an empty context is used.</p>
     *
     * @param savedDiagnosticContext The context to use, may be null.
     * @param callback The callback to perform with the given context (required)
     * @param <R> The return type
     * @return The value from the callback
     */
    public static <R> R doWithContext( @Nullable final SavedDiagnosticContext savedDiagnosticContext,
                                       @NotNull  final Nullary<R> callback ) {
        final DiagnosticContext current = context.get();
        try {
            final DiagnosticContext temporary = new DiagnosticContext();
            if ( savedDiagnosticContext != null ) {
                temporary.restore( savedDiagnosticContext );
            }
            context.set( temporary );
            return callback.call();
        } finally {
            context.set( current );
        }
    }

    /**
     * Get the first value for the given context key.
     *
     * @param key The context key (required)
     * @return The value (may be null)
     */
    public static String getFirst( @NotNull final String key )  {
        final List<String> values = context.get().peek( key );
        return values!=null && !values.isEmpty() ? values.get( 0 ) : null;
    }

    /**
     * Get all context values for the given context key.
     *
     * @param key The context key (required)
     * @return The values (may be null)
     */
    public static List<String> get( @NotNull final String key )  {
        return context.get().peek( key );
    }

    /**
     * Put a single value into the context for the given context key.
     *
     * @param key The context key (required)
     * @param value The value to set (required)
     */
    public static void put( @NotNull final String key,
                            @NotNull final String value )  {
        context.get().put( key, new ArrayList<List<String>>(singleton(Collections.singletonList( value ))));
    }

    /**
     * Put the given values into the context for the given context key
     *
     * @param key The context key (required)
     * @param values The context values (required)
     */
    public static void put( @NotNull final String key,
                            @NotNull final List<String> values )  {
        context.get().put( key, new ArrayList<List<String>>(singleton(toList( values ))) );
    }

    /**
     * Remove the values for the given context key.
     *
     * @param key The context key (required)
     */
    public static void remove( @NotNull final String key  )  {
        context.get().remove( key );
    }

    /**
     * Reset the diagnostic context.
     *
     * <p>All keys and values are cleared.</p>
     */
    public static void reset() {
        context.remove();
    }

    /**
     * Save the current diagnostic context.
     *
     * @return The saved context
     */
    @NotNull
    public static SavedDiagnosticContext save() {
        return context.get().save();
    }

    /**
     * Restore the given context.
     *
     * @param savedDiagnosticContext The context to restore
     * @return The current context
     */
    @NotNull
    public static SavedDiagnosticContext restore( @NotNull final SavedDiagnosticContext savedDiagnosticContext ) {
        return context.get().restore( savedDiagnosticContext );
    }

    /**
     * Saved diagnostic context.
     *
     * @see #save
     * @see #restore
     * @see #doWithContext
     */
    public static class SavedDiagnosticContext {
        private final Map<String,List<List<String>>> properties;

        private SavedDiagnosticContext( final DiagnosticContext context ) {
            this.properties = immutable(context.properties);
        }
    }

    //- PACKAGE

    static Collection<String> getAll( final String key ) {
        return join( context.get().get( key ) );
    }

    //- PRIVATE

    private static <R> Map<String,List<R>> immutable( final Map<String,? extends Collection<R>> map ) {
        return Collections.unmodifiableMap( map( map, null, Functions.<String>identity(), new Functions.Unary<List<R>, Collection<R>>() {
                @Override
                public List<R> call( final Collection<R> strings ) {
                    return toList( strings );
                }
            } ) );
    }

    private static final ThreadLocal<DiagnosticContext> context = new ThreadLocal<DiagnosticContext>(){
        @Override
        protected DiagnosticContext initialValue() {
            return new DiagnosticContext();
        }
    };

    private static class DiagnosticContext {
        /**
         * Context properties are a stack which may contain multiple values
         * for each level.
         */
        private final Map<String,List<List<String>>> properties = new HashMap<String,List<List<String>>>();
        private SavedDiagnosticContext savedContext;

        private DiagnosticContext() {
        }

        public List<List<String>> get( final String key ) {
            return properties.get( key );
        }

        public List<List<String>> put( final String key, final List<List<String>> values ) {
            invalidateSaved();
            return properties.put( key, values );
        }

        public List<List<String>> remove( final String key ) {
            invalidateSaved();
            return properties.remove( key );
        }

        public void push( final String key, final List<String> value ) {
            invalidateSaved();
            List<List<String>> values = properties.get( key );
            if ( values == null ) {
                values = new ArrayList<List<String>>();
                properties.put( key, values );
            }
            values.add( value );
        }

        public List<String> peek( final String key ) {
            final List<List<String>> values = properties.get( key );
            return values==null || values.isEmpty() ? null : values.get( values.size()-1 );
        }

        public List<String> pop( final String key, @Nullable final List<String> value ) {
            invalidateSaved();
            final List<List<String>> values = properties.get( key );
            final List<String> popped;
            if ( values==null || values.isEmpty() ) {
                popped = null;
            } else if ( value == null || value.equals( values.get( values.size() - 1 ) ) ) {
                popped = values.remove( values.size() - 1 );
            } else {
                popped = null;
            }
            return popped;
        }

        private void invalidateSaved() {
            savedContext = null;
        }

        private SavedDiagnosticContext save() {
            if ( savedContext == null ) {
                savedContext = new SavedDiagnosticContext( this );
            }
            return savedContext;
        }

        private SavedDiagnosticContext restore( final SavedDiagnosticContext savedDiagnosticContext ) {
            final SavedDiagnosticContext currentContext = save();
            properties.clear();
            for ( Entry<String, List<List<String>>> stringListEntry : savedDiagnosticContext.properties.entrySet() ) {
                properties.put( stringListEntry.getKey(), new ArrayList<List<String>>( stringListEntry.getValue() ) );
            }
            return currentContext;
        }
    }
}
