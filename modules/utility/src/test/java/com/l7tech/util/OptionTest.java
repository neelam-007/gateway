package com.l7tech.util;

import com.l7tech.util.Functions.Unary;
import com.l7tech.util.Functions.UnaryThrows;
import com.l7tech.util.Functions.UnaryVoid;
import static com.l7tech.util.Functions.nullary;
import static com.l7tech.util.Option.join;
import static com.l7tech.util.Option.none;
import static com.l7tech.util.Option.optional;
import static com.l7tech.util.Option.some;
import static com.l7tech.util.TextUtils.isEmpty;
import static com.l7tech.util.TextUtils.isNotEmpty;
import static com.l7tech.util.TextUtils.trim;
import static org.junit.Assert.*;
import org.junit.Test;

import java.util.Collections;

/**
 * Unit tests for optional values.
 */
public class OptionTest {

    @Test
    public void testConstruction() {
        final Option<String> option1 = Option.none();
        assertFalse( "Empty option", option1.isSome() );

        final Option<String> option2 = some( "value" );
        assertTrue( "Option with value", option2.isSome() );

        final Option<String> option3 = Option.optional( null );
        assertFalse( "Empty option", option3.isSome() );

        final Option<String> option4 = Option.optional( "value" );
        assertTrue( "Option with value", option4.isSome() );
    }

    @SuppressWarnings({ "ConstantConditions" })
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidConstruction() {
        some( null );
    }

    @Test
    public void testAccessors() {
        assertEquals( "Optional value 1", "value", some( "value" ).some() );
        assertEquals( "Optional value 2", "value", Option.optional( "value" ).some() );
        assertEquals( "Optional value 3", "value", some( "value" ).toNull() );
        assertEquals( "Optional value 4", "value", Option.optional( "value" ).toNull() );
    }

    @Test(expected = IllegalStateException.class)
    public void testInvalidAccess() {
        Option.optional( null ).some();
    }

    @Test
    public void testEither() {
        assertTrue( "Either 1", some( "value" ).toEither( 1 ).isRight() );
        assertTrue( "Either 2", Option.optional( "value" ).toEither( 1 ).isRight() );
        assertTrue( "Either 3", Option.<String>none().toEither( 1 ).isLeft() );
        assertTrue( "Either 4", Option.<String>optional( null ).toEither( 1 ).isLeft() );
    }

    @Test
    public void testList() {
        assertEquals( "List 1", Collections.singletonList( "value" ), some( "value" ).toList() );
        assertEquals( "List 2", Collections.singletonList( "value" ), Option.optional( "value" ).toList() );
        assertEquals( "List 3", Collections.<String>emptyList(), Option.<String>none().toList() );
        assertEquals( "List 4", Collections.<String>emptyList(), Option.<String>optional( null ).toList() );
    }

    @Test
    public void testOrSome() {
        assertEquals( "Or some 1", "value1", some( "value1" ).orSome( "value2" ) );
        assertEquals( "Or some 2", "value2", Option.<String>none().orSome( "value2" ) );

        assertEquals( "Or some function 1", "value1", some( "value1" ).orSome( nullary( "value2" ) ) );
        assertEquals( "Or some function 2", "value2", Option.<String>none().orSome( nullary( "value2" ) ) );
    }

    @Test
    public void testOrElse() {
        assertEquals( "Or else 1", "value1", some( "value1" ).orElse( some( "value2" ) ).some() );
        assertEquals( "Or else 2", "value2", Option.<String>none().orElse( some( "value2" ) ).some() );

        assertEquals( "Or else function 1", "value1", some( "value1" ).orElse( nullary( some( "value2" ) ) ).some() );
        assertEquals( "Or else function 2", "value2", Option.<String>none().orElse( nullary(some( "value2" )) ).some() );
    }

    @Test
    public void testMap() {
        final Unary<String,String> nullr = new Unary<String,String>(){
            @Override
            public String call( final String s ) {
                return null;
            }
        };
        assertEquals( "Map 1", "value1", some( "value1   " ).map( trim() ).some() );
        assertFalse( "Map 2", some( "" ).map( nullr ).isSome() );
        assertFalse( "Map 3", Option.<String>none().map( nullr ).isSome() );

        assertEquals( "Map function 1", "value1", Option.<String, String>map().call( some( "value1   " ), trim() ).some() );
        assertFalse( "Map function 2", Option.<String, String>map().call( some( "" ), nullr ).isSome() );
        assertFalse( "Map function 3", Option.<String, String>map().call( Option.<String>none(), nullr ).isSome() );
    }

    @Test
    public void testExists() {
        assertTrue( "Exists 1", some( "value" ).exists( isNotEmpty() ) );
        assertTrue( "Exists 2", some( "" ).exists( isEmpty() ) );
        assertFalse( "Exists 3", some( "" ).exists( isNotEmpty() ) );
        assertFalse( "Exists 4", Option.<String>none().exists( isEmpty() ) );

        assertTrue( "Exists throws 1", some( "value" ).exists( new UnaryThrows<Boolean,String,IllegalArgumentException>(){
            @Override
            public Boolean call( final String s ) throws IllegalArgumentException {
                return true;
            }
        } ) );
        try {
            some( "value" ).exists( new UnaryThrows<Boolean,String,IllegalArgumentException>(){
                @Override
                public Boolean call( final String s ) throws IllegalArgumentException {
                    throw new IllegalArgumentException("expected");
                }
            } );
            fail("Expected exception from exists");
        } catch ( IllegalArgumentException e ) {
            assertEquals( "expected exception message", "expected", e.getMessage() );
        }
    }

    @Test
    public void testFilter() {
        assertEquals( "Filter 1", "value1", some( "value1" ).filter( isNotEmpty() ).some() );
        assertEquals( "Filter 2", "", some( "" ).filter( isEmpty() ).some() );
        assertFalse( "Filter 3", some( "" ).filter( isNotEmpty() ).isSome() );
        assertFalse( "Filter 4", Option.<String>none().filter( isEmpty() ).isSome() );
    }

    @Test
    public void testForeach() {
        final boolean[] flag = new boolean[1];
        some( "" ).foreach( new UnaryVoid<Object>() {
            @Override
            public void call( final Object o ) {
                flag[0] = true;
            }
        } );
        assertTrue( "foreach evaluated", flag[0] );
        none().foreach( new UnaryVoid<Object>() {
            @Override
            public void call( final Object o ) {
                fail( "should not run" );
            }
        } );
    }

    @Test
    public void testJoin() {
        final Unary<Option<Integer>,Integer> divideBySelf = new Unary<Option<Integer>, Integer>(){
            @Override
            public Option<Integer> call( final Integer integer ) {
                return integer==0 ? Option.<Integer>none() : some( integer / integer );
            }
        };

        assertEquals( "result", 1L, (long)join( optional( 2 ).map( divideBySelf ) ).some() );
        assertFalse( "result", join( optional( 0 ).map( divideBySelf ) ).isSome() );
    }
}
