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
                .put( "b", list( "1", "2", "3" ) )
                .map(), list( "b") );

        put( "a", "1" );
        put( "b", "1" );

        assertTrue( "matches 1", matches( rules ) );

        remove( "a" );

        assertFalse( "matches 2", matches( rules ) );

        put( "a", "4" );

        assertFalse( "matches 3", matches( rules ) );

        remove( "a" );
        remove( "b" );
        put( "a", "1" );
        put( "b", "1111" );

        assertTrue( "matches prefix", matches( rules ) );
    }

    @Test
    public void testGlobalMatch() {
        setDefaultRules( new MatcherRules( CollectionUtils.<String, List<String>>mapBuilder()
                .put( "a", list( "1", "2", "3" ) )
                .put( "b", list( "101" ) )
                .put( "c", list( "123" ) )
                .map(), list("c")  ) );

        put( "a", "1" );

        assertFalse( "matches 1", matches() );

        put( "b", "101" );
        put( "c", "123" );

        assertTrue( "matches 2", matches() );

        remove( "c" );
        put( "c", "1" );

        assertFalse( "matches prefix 1", matches() );

        remove( "c" );
        put( "c", "12345" );

        assertTrue( "matches prefix 2", matches() );
    }
}
