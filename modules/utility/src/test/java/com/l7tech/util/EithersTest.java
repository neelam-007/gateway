package com.l7tech.util;

import static com.l7tech.util.CollectionUtils.list;
import static com.l7tech.util.Either.left;
import static com.l7tech.util.Eithers.*;
import static org.junit.Assert.*;
import org.junit.Test;

import java.util.List;

/**
 * Units tests for Eithers
 */
public class EithersTest {

    //- PUBLIC

    @Test
    public void testLists() {
        final List<Either<String,String>> list = list(
                Either.<String,String>right( "right1" ),
                Either.<String,String>left( "left1" ),
                Either.<String,String>right( "right2" ),
                Either.<String,String>left( "left2" ) );

        assertEquals( "lefts", list( "left1", "left2" ), Eithers.lefts( list ) );
        assertEquals( "rights", list( "right1", "right2" ), Eithers.rights( list ) );
    }

    @Test
    public void testConstructAndExtract() {
        // extract
        try {
            extract( left( new TestException1() ) );
            fail("Expected TestException1 for extract");
        } catch ( TestException1 e ) {
        }
        try {
            assertEquals( "Extracted value", "A", extract( Either.<TestException1,String>right( "A" ) ) );
        } catch ( Exception e ) {
            fail("Unexpected exception for extract");
        }

        // extract2
        try {
            extract2( Eithers.<TestException1,TestException2,String>left2_1( new TestException1() ) );
            fail("Expected TestException1 for extract2");
        } catch ( Exception e ) {
            assertTrue( "Expected TestException1 for extract2", TestException1.class.isInstance(e) );
        }
        try {
            extract2( Eithers.<TestException1,TestException2,String>left2_2( new TestException2() ) );
            fail("Expected TestException2 for extract2");
        } catch ( Exception e ) {
            assertTrue( "Expected TestException2 for extract2", TestException2.class.isInstance(e) );
        }
        try {
            assertEquals( "Extracted value 2", "A", extract2( Eithers.<TestException1,TestException2,String>right2( "A" ) ) );
        } catch ( Exception e ) {
            fail("Unexpected exception for extract2");
        }

        // extract3
        try {
            extract3( Eithers.<TestException1,TestException2,TestException3,String>left3_1( new TestException1() ) );
            fail("Expected TestException1 for extract3");
        } catch ( Exception e ) {
            assertTrue( "Expected TestException1 for extract3", TestException1.class.isInstance(e) );
        }
        try {
            extract3( Eithers.<TestException1,TestException2,TestException3,String>left3_2( new TestException2() ) );
            fail("Expected TestException2 for extract3");
        } catch ( Exception e ) {
            assertTrue( "Expected TestException2 for extract3", TestException2.class.isInstance(e) );
        }
        try {
            extract3( Eithers.<TestException1,TestException2,TestException3,String>left3_3( new TestException3() ) );
            fail("Expected TestException3 for extract3");
        } catch ( Exception e ) {
            assertTrue( "Expected TestException3 for extract3", TestException3.class.isInstance(e) );
        }
        try {
            assertEquals( "Extracted value 3", "A", extract3( Eithers.<TestException1,TestException2,TestException3,String>right3( "A" ) ) );
        } catch ( Exception e ) {
            fail("Unexpected exception for extract3");
        }

        // extract4
        try {
            extract4( Eithers.<TestException1,TestException2,TestException3,TestException4,String>left4_1( new TestException1() ) );
            fail("Expected TestException1 for extract4");
        } catch ( Exception e ) {
            assertTrue( "Expected TestException1 for extract4", TestException1.class.isInstance(e) );
        }
        try {
            extract4( Eithers.<TestException1,TestException2,TestException3,TestException4,String>left4_2( new TestException2() ) );
            fail("Expected TestException2 for extract4");
        } catch ( Exception e ) {
            assertTrue( "Expected TestException2 for extract4", TestException2.class.isInstance(e) );
        }
        try {
            extract4( Eithers.<TestException1,TestException2,TestException3,TestException4,String>left4_3( new TestException3() ) );
            fail("Expected TestException3 for extract4");
        } catch ( Exception e ) {
            assertTrue( "Expected TestException3 for extract4", TestException3.class.isInstance(e) );
        }
        try {
            extract4( Eithers.<TestException1,TestException2,TestException3,TestException4,String>left4_4( new TestException4() ) );
            fail("Expected TestException4 for extract4");
        } catch ( Exception e ) {
            assertTrue( "Expected TestException4 for extract4", TestException4.class.isInstance(e) );
        }
        try {
            assertEquals( "Extracted value 4", "A", extract4( Eithers.<TestException1,TestException2,TestException3,TestException4,String>right4( "A" ) ) );
        } catch ( Exception e ) {
            fail("Unexpected exception for extract4");
        }

        // extract5
        try {
            extract5( Eithers.<TestException1,TestException2,TestException3,TestException4,TestException5,String>left5_1( new TestException1() ) );
            fail("Expected TestException1 for extract5");
        } catch ( Exception e ) {
            assertTrue( "Expected TestException1 for extract5", TestException1.class.isInstance(e) );
        }
        try {
            extract5( Eithers.<TestException1,TestException2,TestException3,TestException4,TestException5,String>left5_2( new TestException2() ) );
            fail("Expected TestException2 for extract5");
        } catch ( Exception e ) {
            assertTrue( "Expected TestException2 for extract5", TestException2.class.isInstance(e) );
        }
        try {
            extract5( Eithers.<TestException1,TestException2,TestException3,TestException4,TestException5,String>left5_3( new TestException3() ) );
            fail("Expected TestException3 for extract5");
        } catch ( Exception e ) {
            assertTrue( "Expected TestException3 for extract5", TestException3.class.isInstance(e) );
        }
        try {
            extract5( Eithers.<TestException1,TestException2,TestException3,TestException4,TestException5,String>left5_4( new TestException4() ) );
            fail("Expected TestException4 for extract5");
        } catch ( Exception e ) {
            assertTrue( "Expected TestException4 for extract5", TestException4.class.isInstance(e) );
        }
        try {
            extract5( Eithers.<TestException1,TestException2,TestException3,TestException4,TestException5,String>left5_5( new TestException5() ) );
            fail("Expected TestException5 for extract5");
        } catch ( Exception e ) {
            assertTrue( "Expected TestException5 for extract5", TestException5.class.isInstance(e) );
        }
        try {
            assertEquals( "Extracted value 5", "A", extract5( Eithers.<TestException1,TestException2,TestException3,TestException4,TestException5,String>right5( "A" ) ) );
        } catch ( Exception e ) {
            fail("Unexpected exception for extract5");
        }

        // extract6
        try {
            extract6( Eithers.<TestException1,TestException2,TestException3,TestException4,TestException5,TestException6,String>left6_1( new TestException1() ) );
            fail("Expected TestException1 for extract6");
        } catch ( Exception e ) {
            assertTrue( "Expected TestException1 for extract6", TestException1.class.isInstance(e) );
        }
        try {
            extract6( Eithers.<TestException1,TestException2,TestException3,TestException4,TestException5,TestException6,String>left6_2( new TestException2() ) );
            fail("Expected TestException2 for extract6");
        } catch ( Exception e ) {
            assertTrue( "Expected TestException2 for extract6", TestException2.class.isInstance(e) );
        }
        try {
            extract6( Eithers.<TestException1,TestException2,TestException3,TestException4,TestException5,TestException6,String>left6_3( new TestException3() ) );
            fail("Expected TestException3 for extract6");
        } catch ( Exception e ) {
            assertTrue( "Expected TestException3 for extract6", TestException3.class.isInstance(e) );
        }
        try {
            extract6( Eithers.<TestException1,TestException2,TestException3,TestException4,TestException5,TestException6,String>left6_4( new TestException4() ) );
            fail("Expected TestException4 for extract6");
        } catch ( Exception e ) {
            assertTrue( "Expected TestException4 for extract6", TestException4.class.isInstance(e) );
        }
        try {
            extract6( Eithers.<TestException1,TestException2,TestException3,TestException4,TestException5,TestException6,String>left6_5( new TestException5() ) );
            fail("Expected TestException5 for extract6");
        } catch ( Exception e ) {
            assertTrue( "Expected TestException5 for extract6", TestException5.class.isInstance(e) );
        }
        try {
            extract6( Eithers.<TestException1,TestException2,TestException3,TestException4,TestException5,TestException6,String>left6_6( new TestException6() ) );
            fail("Expected TestException6 for extract6");
        } catch ( Exception e ) {
            assertTrue( "Expected TestException6 for extract6", TestException6.class.isInstance(e) );
        }
        try {
            assertEquals( "Extracted value 6", "A", extract6( Eithers.<TestException1,TestException2,TestException3,TestException4,TestException5,TestException6,String>right6( "A" ) ) );
        } catch ( Exception e ) {
            fail("Unexpected exception for extract6");
        }

        // extract7
        try {
            extract7( Eithers.<TestException1,TestException2,TestException3,TestException4,TestException5,TestException6,TestException7,String>left7_1( new TestException1() ) );
            fail("Expected TestException1 for extract7");
        } catch ( Exception e ) {
            assertTrue( "Expected TestException1 for extract7", TestException1.class.isInstance(e) );
        }
        try {
            extract7( Eithers.<TestException1,TestException2,TestException3,TestException4,TestException5,TestException6,TestException7,String>left7_2( new TestException2() ) );
            fail("Expected TestException2 for extract7");
        } catch ( Exception e ) {
            assertTrue( "Expected TestException2 for extract7", TestException2.class.isInstance(e) );
        }
        try {
            extract7( Eithers.<TestException1,TestException2,TestException3,TestException4,TestException5,TestException6,TestException7,String>left7_3( new TestException3() ) );
            fail("Expected TestException3 for extract7");
        } catch ( Exception e ) {
            assertTrue( "Expected TestException3 for extract7", TestException3.class.isInstance(e) );
        }
        try {
            extract7( Eithers.<TestException1,TestException2,TestException3,TestException4,TestException5,TestException6,TestException7,String>left7_4( new TestException4() ) );
            fail("Expected TestException4 for extract7");
        } catch ( Exception e ) {
            assertTrue( "Expected TestException4 for extract7", TestException4.class.isInstance(e) );
        }
        try {
            extract7( Eithers.<TestException1,TestException2,TestException3,TestException4,TestException5,TestException6,TestException7,String>left7_5( new TestException5() ) );
            fail("Expected TestException5 for extract7");
        } catch ( Exception e ) {
            assertTrue( "Expected TestException5 for extract7", TestException5.class.isInstance(e) );
        }
        try {
            extract7( Eithers.<TestException1,TestException2,TestException3,TestException4,TestException5,TestException6,TestException7,String>left7_6( new TestException6() ) );
            fail("Expected TestException6 for extract7");
        } catch ( Exception e ) {
            assertTrue( "Expected TestException6 for extract7", TestException6.class.isInstance(e) );
        }
        try {
            extract7( Eithers.<TestException1,TestException2,TestException3,TestException4,TestException5,TestException6,TestException7,String>left7_7( new TestException7() ) );
            fail("Expected TestException7 for extract7");
        } catch ( Exception e ) {
            assertTrue( "Expected TestException7 for extract7", TestException7.class.isInstance(e) );
        }
        try {
            assertEquals( "Extracted value 7", "A", extract7( Eithers.<TestException1,TestException2,TestException3,TestException4,TestException5,TestException6,TestException7,String>right7( "A" ) ) );
        } catch ( Exception e ) {
            fail("Unexpected exception for extract7");
        }
    }

    //- PRIVATE

    private static final class TestException1 extends Exception { }
    private static final class TestException2 extends Exception { }
    private static final class TestException3 extends Exception { }
    private static final class TestException4 extends Exception { }
    private static final class TestException5 extends Exception { }
    private static final class TestException6 extends Exception { }
    private static final class TestException7 extends Exception { }
}
