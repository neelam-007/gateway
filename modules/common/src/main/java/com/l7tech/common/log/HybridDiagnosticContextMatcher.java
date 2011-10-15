package com.l7tech.common.log;


import com.l7tech.util.CollectionUtils;
import static com.l7tech.util.CollectionUtils.toList;
import com.l7tech.util.Functions;
import com.l7tech.util.Functions.Binary;
import com.l7tech.util.Functions.Unary;
import static com.l7tech.util.Functions.map;
import static java.util.Collections.unmodifiableMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The matcher compares matcher rules to the current diagnostic context.
 */
public class HybridDiagnosticContextMatcher {

    //- PUBLIC

    /**
     * Test if the given rules match the current context.
     *
     * @param rules The diagnostic context matching rules (required)
     * @return True if the rules match the current context
     */
    public static boolean matches( @NotNull final MatcherRules rules ) {
        boolean matches = true;

        for ( final Entry<String,List<String>> entry : rules.template.entrySet() ) {
            final Collection<String> values = HybridDiagnosticContext.getAll( entry.getKey() );
            if ( !rules.isPrefixMatch( entry.getKey() ) ) {
                if ( !CollectionUtils.containsAny( values, entry.getValue() ) ) {
                    matches = false;
                    break;
                }
            } else { // prefix match
                if ( !CollectionUtils.matchesAny( values, entry.getValue(), prefixMatcher ) ) {
                    matches = false;
                    break;
                }
            }
        }

        return matches;
    }

    /**
     * Test if the default rules match the current context.
     *
     * @return True if the default rules match the current context
     */
    public static boolean matches() {
        return matches( getDefaultRules() );
    }

    /**
     * Get the global default matching rules
     *
     * @return The rules
     */
    @NotNull
    public static MatcherRules getDefaultRules() {
        return globalRules.get();
    }

    /**
     * Set the global default matching rules
     *
     * @param rules The matching rules to use (if null an empty set of rules is installed)
     * @return The previous matching rules
     */
    @NotNull
    public static MatcherRules setDefaultRules( @Nullable final MatcherRules rules ) {
        return globalRules.getAndSet( rules == null ? new MatcherRules() : rules );
    }

    /**
     * A set of rules used to match values in the diagnostic context.
     */
    public static final class MatcherRules implements Serializable {
        private static final long serialVersionUID = 1L;

        private final Map<String,List<String>> template;
        private final Set<String> prefixTemplates;

        /**
         * Create an empty set of rules.
         */
        public MatcherRules() {
            template = Collections.emptyMap();
            prefixTemplates = Collections.emptySet();
        }

        /**
         * Create rules using the given values.
         *
         * @param template The map of context keys to match targets (ORed)
         * @param prefixMatchProperties The set of properties that should be matched by prefix
         */
        public MatcherRules( final Map<String,List<String>> template,
                             final Collection<String> prefixMatchProperties ) {
            this.template = unmodifiableMap( map( template, null, Functions.<String>identity(), new Unary<List<String>, List<String>>() {
                @Override
                public List<String> call( final List<String> strings ) {
                    return toList( strings );
                }
            } ) );
            this.prefixTemplates = prefixMatchProperties==null ?
                    Collections.<String>emptySet() :
                    Collections.unmodifiableSet( new HashSet<String>(prefixMatchProperties) );
        }

        private boolean isPrefixMatch( final String name ) {
            // could be null when deserialized
            return prefixTemplates!=null && prefixTemplates.contains(name);
        }
    }

    //- PRIVATE

    private static final AtomicReference<MatcherRules> globalRules = new AtomicReference<MatcherRules>(new MatcherRules());
    private static final Binary<Boolean,String,String> prefixMatcher = new Binary<Boolean,String,String>(){
        @Override
        public Boolean call( final String target, final String value ) {
            return value!=null && target!=null && value.startsWith( target );
        }
    };
}
