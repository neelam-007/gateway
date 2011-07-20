package com.l7tech.util;

import static com.l7tech.util.CollectionUtils.list;
import static com.l7tech.util.Either.*;
import com.l7tech.util.Functions.Unary;
import com.l7tech.util.Functions.UnaryThrows;
import static org.junit.Assert.*;
import org.junit.Test;

import java.util.List;

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
    public void testLists() {
        final List<Either<String,String>> list = list(
                Either.<String,String>right( "right1" ),
                Either.<String,String>left( "left1" ),
                Either.<String,String>right( "right2" ),
                Either.<String,String>left( "left2" ) );

        assertEquals( "lefts", list( "left1", "left2" ), lefts( list ) );
        assertEquals( "rights", list( "right1", "right2" ), rights( list ) );
    }
}
