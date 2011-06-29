package com.l7tech.util;

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
}
