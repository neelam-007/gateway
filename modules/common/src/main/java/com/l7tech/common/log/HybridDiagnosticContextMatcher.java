package com.l7tech.common.log;


import com.l7tech.util.CollectionUtils;
import static com.l7tech.util.CollectionUtils.toList;
import com.l7tech.util.Functions;
import com.l7tech.util.Functions.Unary;
import static com.l7tech.util.Functions.map;
import static java.util.Collections.unmodifiableMap;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The matcher compares matcher rules to the current diagnostic context.
 */
public class HybridDiagnosticContextMatcher {

    //- PUBLIC

    public static boolean matches( final MatcherRules rules ) {
        boolean matches = true;

        for ( final Entry<String,List<String>> entry : rules.template.entrySet() ) {
            final List<String> values = HybridDiagnosticContext.getAll( entry.getKey() );
            if ( !CollectionUtils.containsAny( values, entry.getValue() ) ) {
                matches = false;
                break;
            }
        }

        return matches;
    }

    public static boolean matches() {
        return matches( getDefaultRules() );
    }

    public static MatcherRules getDefaultRules() {
        return globalRules.get();
    }

    public static MatcherRules setDefaultRules( final MatcherRules rules ) {
        return globalRules.getAndSet( rules == null ? new MatcherRules() : rules );
    }

    public static final class MatcherRules implements Serializable {
        private static final long serialVersionUID = 1L;

        private final Map<String,List<String>> template;

        public MatcherRules() {
            template = Collections.emptyMap();
        }

        public MatcherRules( final Map<String,List<String>> template ) {
            this.template = unmodifiableMap( map( template, null, Functions.<String>identity(), new Unary<List<String>, List<String>>() {
                @Override
                public List<String> call( final List<String> strings ) {
                    return toList( strings );
                }
            } ) );
        }
    }

    //- PRIVATE

    private static final AtomicReference<MatcherRules> globalRules = new AtomicReference<MatcherRules>(new MatcherRules());
}
