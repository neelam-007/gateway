package com.l7tech.common.log;

import static com.l7tech.common.log.HybridDiagnosticContext.*;
import static com.l7tech.common.log.HybridDiagnosticContextMatcher.*;

import com.l7tech.util.CollectionUtils;
import static com.l7tech.util.CollectionUtils.list;
import static org.junit.Assert.*;
import org.junit.Test;

import java.util.List;

/**
 * Unit tests for HybridDiagnosticContextMatcher
 */
public class HybridDiagnosticContextMatcherTest {

    @Test
    public void testSpecifiedMatch() {
        final MatcherRules rules = new MatcherRules( CollectionUtils.<String, List<String>>mapBuilder()
                .put( "a", list( "1", "2", "3" ) )
                .map() );

        put( "a", "1" );

        assertTrue( "matches 1", matches( rules ) );

        remove( "a" );

        assertFalse( "matches 2", matches( rules ) );

        put( "a", "4" );

        assertFalse( "matches 3", matches( rules ) );
    }

    @Test
    public void testGlobalMatch() {
        setDefaultRules( new MatcherRules( CollectionUtils.<String, List<String>>mapBuilder()
                .put( "a", list( "1", "2", "3" ) )
                .put( "b", list( "101" ) )
                .map()  ) );

        put( "a", "1" );

        assertFalse( "matches 1", matches() );

        put( "b", "101" );

        assertTrue( "matches 2", matches() );
    }
}
