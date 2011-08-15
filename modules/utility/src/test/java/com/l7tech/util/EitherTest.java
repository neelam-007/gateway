package com.l7tech.util;

import static com.l7tech.util.Either.*;
import com.l7tech.util.Functions.Unary;
import com.l7tech.util.Functions.UnaryThrows;
import static org.hamcrest.CoreMatchers.*;
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

    @Test
    public void testEqualsAndHashCode() {
        final Either<String,Integer> a1 = left( "a" );
        final Either<String,Integer> a2 = left( "a" );
        final Either<String,Integer> b = left( "b" );
        final Either<String,Integer> c1 = right( 3 );
        final Either<String,Integer> c2 = right( 3 );

        assertThat( "left values equal", a1, equalTo( a2 ) );
        assertThat( "right values equal", c1, equalTo( c2 ) );
        assertThat( "left values not equal", a1, not( equalTo( b ) ) );
        assertThat( "right values not equal", c1, not( equalTo( b ) ) );
        assertThat( "right and left values not equal", Either.<String,String>left("d"), not( equalTo( Either.<String,String>right("d") ) ) );

        assertThat( "left values equal hashCode", a1.hashCode(), equalTo( a2.hashCode() ) );
        assertThat( "right values equal hashCode", c1.hashCode(), equalTo( c2.hashCode() ) );
        assertThat( "left values not equal hashCode", a1.hashCode(), not( equalTo( b.hashCode() ) ) );
        assertThat( "right values not equal hashCode", c1.hashCode(), not( equalTo( b.hashCode() ) ) );
        assertThat( "right and left values not equal hashCode", Either.<String,String>left("d").hashCode(), not( equalTo( Either.<String,String>right("d").hashCode() ) ) );
    }

    @Test
    public void testMap() {
        final Either<String,Integer> left = left( "a" );
        final Either<String,Integer> right = right( 1 );

        final Unary<String,String> stringDoubler = new Unary<String, String>(){
            @Override
            public String call( final String value ) {
                return value + value;
            }
        };
        final Unary<Integer,Integer> intDoubler = new Unary<Integer, Integer>(){
            @Override
            public Integer call( final Integer value ) {
                return value + value;
            }
        };

        assertThat( "Double left left", "aa", equalTo( left.mapLeft( stringDoubler ).left() ) );
        assertThat( "Double left right", "a", equalTo( left.mapRight( intDoubler ).left() ) );
        assertThat( "Double right left", 1, equalTo( right.mapLeft( stringDoubler ).right() ) );
        assertThat( "Double right right", 2, equalTo( right.mapRight( intDoubler ).right() ) );
    }

    @Test
    public void testJoin() {
        final Unary<Either<String,Integer>,Integer> divideBySelf = new Unary<Either<String,Integer>,Integer>(){
            @Override
            public Either<String, Integer> call( final Integer integer ) {
                return integer == 0 ?
                        Either.<String,Integer>left( "Cannot divide by zero" ) :
                        Either.<String,Integer>right( integer / integer );
            }
        };
        final Unary<Either<Integer,String>,Integer> divideBySelfLeft = new Unary<Either<Integer,String>,Integer>(){
            @Override
            public Either<Integer, String> call( final Integer integer ) {
                return divideBySelf.call( integer ).swap();
            }
        };

        assertEquals( "right result", 1L, (long)joinRight( Either.<String,Integer>right( 2 ).mapRight( divideBySelf ) ).right() );
        assertFalse( "right failure", joinRight( Either.<String,Integer>right( 0 ).mapRight( divideBySelf ) ).isRight() );

        assertEquals( "left result", 1L, (long)joinLeft( Either.<Integer,String>left( 2 ).mapLeft( divideBySelfLeft ) ).left() );
        assertFalse( "left failure", joinLeft( Either.<Integer,String>left( 0 ).mapLeft( divideBySelfLeft ) ).isLeft() );
    }
}
