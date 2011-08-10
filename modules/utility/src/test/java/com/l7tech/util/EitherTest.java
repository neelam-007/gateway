package com.l7tech.util;

import static com.l7tech.util.Either.*;
import com.l7tech.util.Functions.Unary;
import com.l7tech.util.Functions.UnaryThrows;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 * Unit tests for Either
 */
public class EitherTest {

    @Test
    public void testBasicLeft() {
        final Either<String,Integer> either = left( "text" );
        assertTrue( "left", either.isLeft() );
        assertFalse( "right", either.isRight() );
        assertEquals( "left value", "text", either.left() );
    }

    @Test
    public void testBasicRight() {
        final Either<String,Integer> either = right( 12 );
        assertFalse( "left", either.isLeft() );
        assertTrue( "right", either.isRight() );
        assertEquals( "right value", 12L, (long)either.right() );
    }

    @Test
    public void testOptionalLeft() {
        final Either<Option<String>,Integer> either = leftOption( "text" );
        assertTrue( "left", either.isLeft() );
        assertFalse( "right", either.isRight() );
        assertEquals( "left value", "text", either.left().some() );
    }

    @Test
    public void testOptionalRight() {
        final Either<String,Option<Integer>> either = rightOption( 12 );
        assertFalse( "left", either.isLeft() );
        assertTrue( "right", either.isRight() );
        assertEquals( "right value", 12L, (long)either.right().some() );
    }

    @Test
    public void testOptionalNullLeft() {
        final Either<Option<String>,Integer> either = leftOption( null );
        assertTrue( "left", either.isLeft() );
        assertFalse( "right", either.isRight() );
        assertFalse( "some left value", either.left().isSome() );
    }

    @Test
    public void testOptionalNullRight() {
        final Either<String,Option<Integer>> either = rightOption( null );
        assertFalse( "left", either.isLeft() );
        assertTrue( "right", either.isRight() );
        assertFalse( "some right value", either.right().isSome() );
    }

    @Test
    public void testToOption() {
        final Either<String,String> eitherL = left( "a" );
        final Either<String,String> eitherR = right( "a" );

        assertTrue( "Left is some", eitherL.toLeftOption().isSome() );
        assertTrue( "Right is some", eitherR.toRightOption().isSome() );
        assertFalse( "Right is none", eitherL.toRightOption().isSome() );
        assertFalse( "Left is none", eitherR.toLeftOption().isSome() );
    }

    @Test
    public void testSwap() {
        final Either<String,Long> eitherL = left( "a" );
        final Either<String,Long> eitherR = right( 1L );

        assertEquals( "Left", "a", eitherL.left() );
        assertEquals( "Swapped left", "a", eitherL.swap().right() );
        assertEquals( "Right", 1L, (long)eitherR.right() );
        assertEquals( "Swapped right", 1L, (long)eitherR.swap().left() );
    }

    @Test
    public void testEither() {
        final Unary<String,String> returner = new Unary<String,String>(){
            @Override
            public String call( final String value ) {
                return value;
            }
        };
        final Unary<String,String> failer = new Unary<String,String>(){
            @Override
            public String call( final String value ) {
                fail( "Failer invoked" );
                return null;
            }
        };

        assertEquals( "left text", "text", Either.<String,String>left( "text" ).either( returner, failer ) );
        assertEquals( "right text", "text", Either.<String, String>right( "text" ).either( failer, returner ) );

        final UnaryThrows<String,String,IllegalArgumentException> returnerThrows = new UnaryThrows<String,String,IllegalArgumentException>(){
            @Override
            public String call( final String value ) {
                return value;
            }
        };
        final UnaryThrows<String,String,IllegalArgumentException> thrower = new UnaryThrows<String,String,IllegalArgumentException>(){
            @Override
            public String call( final String value ) throws IllegalArgumentException {
                throw new IllegalArgumentException( "Thrower invoked" );
            }
        };

        assertEquals( "left text", "text", Either.<String,String>left( "text" ).either( returnerThrows, thrower ) );
        assertEquals( "right text", "text", Either.<String,String>right( "text" ).either( thrower, returnerThrows ) );
        try {
            Either.<String,String>left( "text" ).either( thrower, returnerThrows );
            fail("Expected exception");
        } catch ( IllegalArgumentException e ){  }
        try {
            Either.<String,String>right( "text" ).either( returnerThrows, thrower );
            fail("Expected exception");
        } catch ( IllegalArgumentException e ){  }
    }
}
