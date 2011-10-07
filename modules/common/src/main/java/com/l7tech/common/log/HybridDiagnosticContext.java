package com.l7tech.common.log;

import static com.l7tech.util.CollectionUtils.toList;
import com.l7tech.util.Functions;
import com.l7tech.util.Functions.Nullary;
import static java.util.Collections.unmodifiableMap;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
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

    public static <R> R doInContext( final String name,
                                     final String value,
                                     final Nullary<R> callback ) {
        try {
            context.get().push( name, value );
            return callback.call();
        } finally {
            context.get().pop( name, value );
        }
    }

    /**
     * Perform an action using the saved context.
     *
     * <p>If the given context is null then the current context is used.</p>
     *
     * @param savedDiagnosticContext The context to use, may be null.
     * @param callback The callback to perform with the given context.
     * @param <R> The return type
     * @return The value from the callback
     */
    public static <R> R doWithContext( final SavedDiagnosticContext savedDiagnosticContext,
                                       final Nullary<R> callback ) {
        final DiagnosticContext current = context.get();
        try {
            if ( savedDiagnosticContext != null ) {
                final DiagnosticContext temporary = new DiagnosticContext();
                temporary.restore( savedDiagnosticContext );
                context.set( temporary );
            }
            return callback.call();
        } finally {
            context.set( current );
        }
    }

    public static String get( final String key  )  {
        return context.get().peek( key );
    }

    public static void put( final String key,
                            final String value )  {
        context.get().put( key, Collections.singletonList( value ) );
    }

    public static void remove( final String key  )  {
        context.get().remove( key );
    }

    public static void reset() {
        context.remove();
    }

    public static SavedDiagnosticContext save() {
        return context.get().save();
    }

    public static SavedDiagnosticContext restore( final SavedDiagnosticContext savedDiagnosticContext ) {
        return context.get().restore( savedDiagnosticContext );
    }

    public static class SavedDiagnosticContext {
        private final Map<String,List<String>> properties;

        private SavedDiagnosticContext( final DiagnosticContext context ) {
            this.properties = unmodifiableMap( Functions.map( context.properties, null, Functions.<String>identity(), new Functions.Unary<List<String>,List<String>>(){
                @Override
                public List<String> call( final List<String> strings ) {
                    return toList( strings );
                }
            } ) );
        }
    }

    //- PACKAGE

    static List<String> getAll( final String key ) {
        return context.get().get( key );
    }

    //- PRIVATE

    private static final ThreadLocal<DiagnosticContext> context = new ThreadLocal<DiagnosticContext>(){
        @Override
        protected DiagnosticContext initialValue() {
            return new DiagnosticContext();
        }
    };

    private static class DiagnosticContext {
        private final Map<String,List<String>> properties = new HashMap<String,List<String>>();
        private SavedDiagnosticContext savedContext;

        private DiagnosticContext() {
        }

        public List<String> get( final String key ) {
            return properties.get( key );
        }

        public List<String> put( final String key, final List<String> values ) {
            invalidateSaved();
            return properties.put( key, values );
        }

        public List<String> remove( final String key ) {
            invalidateSaved();
            return properties.remove( key );
        }

        public void push( final String key, final String value ) {
            invalidateSaved();
            List<String> values = properties.get( key );
            if ( values == null ) {
                values = new ArrayList<String>();
                properties.put( key, values );
            }
            values.add( value );
        }

        public String peek( final String key ) {
            final List<String> values = properties.get( key );
            return values==null || values.isEmpty() ? null : values.get( values.size()-1 );
        }

        public String pop( final String key, @Nullable final String value ) {
            invalidateSaved();
            final List<String> values = properties.get( key );
            final String popped;
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
            for ( Entry<String, List<String>> stringListEntry : savedDiagnosticContext.properties.entrySet() ) {
                properties.put( stringListEntry.getKey(), new ArrayList<String>( stringListEntry.getValue() ) );
            }
            return currentContext;
        }
    }
}
