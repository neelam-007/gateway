package com.l7tech.common.log;

import com.l7tech.common.log.HybridDiagnosticContextMatcher.MatcherRules;

import java.util.logging.LogRecord;

/**
 * Logging filter that uses the diagnostic context.
 */
public class HybridDiagnosticContextFilter implements SerializableFilter {

    //- PUBLIC

    public HybridDiagnosticContextFilter( final MatcherRules rules ) {
        this.rules = rules;
    }

    @Override
    public boolean isLoggable( final LogRecord record ) {
        HybridDiagnosticContext.put( HybridDiagnosticContextKeys.LOGGER_NAME, record.getLoggerName() );
        return HybridDiagnosticContextMatcher.matches( rules );
    }

    //- PRIVATE

    private static final long serialVersionUID = 1L;

    private final MatcherRules rules;
}
