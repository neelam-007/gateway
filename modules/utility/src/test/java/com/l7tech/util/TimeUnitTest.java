package com.l7tech.util;

import com.l7tech.test.BugNumber;
import static com.l7tech.util.TimeUnit.parse;
import static org.junit.Assert.*;
import org.junit.Test;


public class TimeUnitTest {
    
    @Test
    public void testParser() throws Exception {
        assertEquals( "60s == one minute", (long) (60 * 1000), parse( "60s" ) );
        assertEquals( "1ms == one millisecond", 1L, parse( "1ms" ) );
        assertEquals( "1h == one hour", (long) (60 * 60 * 1000), parse( "1h" ) );
        assertEquals( "1000(default ms) == one second", 1000L, parse( "1000" ) );
        assertEquals( "12m == 720000 ms", 720000L, parse( "12m" ) );
        assertEquals( "1d == 86400000ms", 86400000L, parse( "1d" ) );
        assertEquals( "1,000,000 == 1000s", 1000000L, parse( "1,000,000" ) );
        assertEquals( "1.5s == 1500ms", 1500L, parse( "1.5s" ) );
        assertEquals( ".5s == 500ms", 500L, parse( ".5s" ) );
        assertEquals( ".5d == 43200000", 43200000L, parse( ".5d" ) );
        assertEquals( "-1s == -1000s", -1000L, parse( "-1s" ) );
        assertEquals( "0 == 0", 0L, parse( "0" ) );
        assertEquals( "0.0 == 0", 0L, parse( "0.0" ) );
        assertEquals( ".000 == 0", 0L, parse( ".000" ) );
        parse( "12345678901234567890" ); // Ensure 20-digit input passes
        assertEquals( "60s + non-strict == one minute", (long) (60 * 1000), parse( ", 6, ,, 0, , ,    s," ) ); // Ensure commas and spaces ignored
        try { parse( "-12345678901234567890" ); fail( "Expected exception was not thrown" ); } catch (Exception e) {}
        try { parse( "0.0.0" ); fail( "Expected exception was not thrown" ); } catch (Exception e) {}
        try { parse( "-" ); fail( "Expected exception was not thrown" ); } catch (Exception e) {}
        try { parse( "" ); fail( "Expected exception was not thrown" ); } catch (Exception e) {}
        try { parse( null );fail( "Expected exception was not thrown" ); } catch (Exception e) {}
    }

    @BugNumber(11123)
    @Test
    public void testStrictParsing() {
        assertEquals( "60s (strict no space) == one minute", (long) (60 * 1000), parse( "60s", TimeUnit.SECONDS, true ) );
        assertEquals( "60s (strict with space) == one minute", (long) (60 * 1000), parse( "60 s", TimeUnit.SECONDS, true ) );
        try { parse( "60  s", TimeUnit.SECONDS, true ); fail("Expected exception for extra space was not thrown"); } catch (Exception e) {}
        try { parse( " 60s", TimeUnit.SECONDS, true ); fail("Expected exception for leading space was not thrown"); } catch (Exception e) {}
        try { parse( "60s ", TimeUnit.SECONDS, true ); fail("Expected exception for trailing space was not thrown"); } catch (Exception e) {}
    }
}