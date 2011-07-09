package com.l7tech.util;

import com.l7tech.util.Functions.Unary;
import static com.l7tech.util.TextUtils.isEmpty;
import static com.l7tech.util.TextUtils.isNotEmpty;
import static com.l7tech.util.TextUtils.trim;
import static org.junit.Assert.*;
import static org.junit.Assert.assertFalse;
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

        final Option<String> option2 = Option.some( "value" );
        assertTrue( "Option with value", option2.isSome() );

        final Option<String> option3 = Option.optional( null );
        assertFalse( "Empty option", option3.isSome() );

        final Option<String> option4 = Option.optional( "value" );
        assertTrue( "Option with value", option4.isSome() );
    }

    @SuppressWarnings({ "ConstantConditions" })
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidConstruction() {
        Option.some( null );
    }

    @Test
    public void testAccessors() {
        assertEquals( "Optional value 1", "value", Option.some( "value" ).some() );
        assertEquals( "Optional value 2", "value", Option.optional( "value" ).some() );
        assertEquals( "Optional value 3", "value", Option.some( "value" ).toNull() );
        assertEquals( "Optional value 4", "value", Option.optional( "value" ).toNull() );
    }

    @Test(expected = IllegalStateException.class)
    public void testInvalidAccess() {
        Option.optional( null ).some();
    }

    @Test
    public void testEither() {
        assertTrue( "Either 1", Option.some( "value" ).toEither( 1 ).isRight() );
        assertTrue( "Either 2", Option.optional( "value" ).toEither( 1 ).isRight() );
        assertTrue( "Either 3", Option.<String>none().toEither( 1 ).isLeft() );
        assertTrue( "Either 4", Option.<String>optional( null ).toEither( 1 ).isLeft() );
    }

    @Test
    public void testList() {
        assertEquals( "List 1", Collections.singletonList( "value" ), Option.some( "value" ).toList() );
        assertEquals( "List 2", Collections.singletonList( "value" ), Option.optional( "value" ).toList() );
        assertEquals( "List 3", Collections.<String>emptyList(), Option.<String>none().toList() );
        assertEquals( "List 4", Collections.<String>emptyList(), Option.<String>optional( null ).toList() );
    }

    @Test
    public void testOrSome() {
        assertEquals( "Or some 1", "value1", Option.some( "value1" ).orSome( "value2" ) );
        assertEquals( "Or some 2", "value2", Option.<String>none().orSome( "value2" ) );
    }

    @Test
    public void testMap() {
        final Unary<String,String> nullr = new Unary<String,String>(){
            @Override
            public String call( final String s ) {
                return null;
            }
        };
        assertEquals( "Map 1", "value1", Option.some( "value1   " ).map( trim() ).some() );
        assertFalse( "Map 2", Option.some( "" ).map( nullr ).isSome() );
        assertFalse( "Map 3", Option.<String>none().map( nullr ).isSome() );
    }

    @Test
    public void testFilter() {
        assertEquals( "Filter 1", "value1", Option.some( "value1" ).filter( isNotEmpty() ).some() );
        assertEquals( "Filter 2", "", Option.some( "" ).filter( isEmpty() ).some() );
        assertFalse( "Filter 3", Option.some( "" ).filter( isNotEmpty() ).isSome() );
        assertFalse( "Filter 4", Option.<String>none().filter( isEmpty() ).isSome() );
    }
}
