package com.l7tech.util;

import static com.l7tech.util.CollectionUtils.*;
import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 *
 */
public class CollectionUtilsTest {

    @Test(expected= NoSuchElementException.class)
    public void testSimpleIterable() {
        final List<String> list1 = Arrays.asList( "1", "2" );
        final List<String> list2 = Arrays.asList( "3", "4" );
        final List<String> list3 = Arrays.asList( "5", "6" );

        final Iterator<Object> iterator0 = iterable().iterator();
        assertFalse( "no more values", iterator0.hasNext() );

        final Iterator<String> iterator1 = iterable( list1 ).iterator();
        assertEquals("value 1", "1", iterator1.next() );
        assertEquals("value 2", "2", iterator1.next() );
        assertFalse( "no more values", iterator1.hasNext() );

        final Iterator<String> iterator2 = iterable( list1, list2 ).iterator();
        assertEquals("value 1", "1", iterator2.next() );
        assertEquals("value 2", "2", iterator2.next() );
        assertEquals("value 3", "3", iterator2.next() );
        assertEquals("value 4", "4", iterator2.next() );
        assertFalse( "no more values", iterator2.hasNext() );

        final Iterator<String> iterator3 = iterable( list1, list2, list3 ).iterator();
        assertEquals("value 1", "1", iterator3.next() );
        assertEquals("value 2", "2", iterator3.next() );
        assertEquals("value 3", "3", iterator3.next() );
        assertEquals("value 4", "4", iterator3.next() );
        assertEquals("value 5", "5", iterator3.next() );
        assertEquals("value 6", "6", iterator3.next() );
        assertFalse( "no more values", iterator3.hasNext() );
        iterator3.next(); // throw
    }

    @Test(expected= NoSuchElementException.class)
    public void testSomeEmptyIterable() {
        final List<String> list0 = Arrays.asList( );
        final List<String> list1 = Arrays.asList( "1" );
        final List<String> list2 = Arrays.asList( );
        final List<String> list3 = Arrays.asList( "2", "3", "4" );

        final Iterator<String> iterator4 = iterable( list0, list1, list2, list3 ).iterator();
        assertEquals("value 1", "1", iterator4.next() );
        assertEquals("value 2", "2", iterator4.next() );
        assertEquals("value 3", "3", iterator4.next() );
        assertEquals("value 4", "4", iterator4.next() );
        assertFalse( "no more values", iterator4.hasNext() );
        iterator4.next(); // throw
    }

    @Test
    public void testRemoveWithIterable() {
        final List<String> list1 = new ArrayList<String>(Arrays.asList( "1", "2" ));
        final List<String> list2 = new ArrayList<String>(Arrays.asList( "3", "4" ));
        final List<String> list3 = new ArrayList<String>(Arrays.asList( "5", "6" ));

        final Iterator<String> iterator3 = iterable( list1, list2, list3 ).iterator();
        assertEquals("value 1", "1", iterator3.next() );
        iterator3.remove();
        assertEquals("value 2", "2", iterator3.next() );
        iterator3.remove();
        assertEquals("value 3", "3", iterator3.next() );
        iterator3.remove();
        assertEquals("value 4", "4", iterator3.next() );
        iterator3.remove();
        assertEquals("value 5", "5", iterator3.next() );
        iterator3.remove();
        assertEquals("value 6", "6", iterator3.next() );
        iterator3.remove();
        assertFalse( "no more values", iterator3.hasNext() );

        assertTrue( "list 1 empty", list1.isEmpty() );
        assertTrue( "list 2 empty", list2.isEmpty() );
        assertTrue( "list 3 empty", list3.isEmpty() );
    }

    @Test
    public void testJoin() {
        final List<String> list1 = new ArrayList<String>(Arrays.asList( "1", "2" ));
        final List<String> list2 = new ArrayList<String>(Arrays.asList( "3", "4" ));
        final List<String> list3 = new ArrayList<String>(Arrays.asList( "5", "6" ));

        final List<List<String>> listList = new ArrayList<List<String>>(Arrays.<List<String>>asList( list1, list2, list3 ));
        final List<String> expectedResultList = new ArrayList<String>(Arrays.asList( "1", "2", "3", "4", "5", "6" ));

        assertEquals( "Joined list", expectedResultList, join( listList ) );
        assertEquals( "Joined empty list", new ArrayList<String>(), join( new ArrayList<List<String>>() ) );
    }

    @Test
    public void testList() {
        final List<String> list1 = list( "1", "2" );
        final List<String> list2 = list();
        final List<String> list3 = list( "3", "4", "5", "6" );

        assertEquals( "list1", Arrays.asList( "1", "2" ), list1 );
        assertEquals( "list2", Collections.<String>emptyList(), list2 );
        assertEquals( "list3", Arrays.asList( "3", "4", "5", "6" ), list3 );

        final List<List<String>> listList = list( list1, list2, list3 );
        final List<List<String>> expectedResultList = new ArrayList<List<String>>( Arrays.asList(
                Arrays.asList( "1", "2"),
                Collections.<String>emptyList(),
                Arrays.asList( "3", "4", "5", "6" ) ) );

        assertEquals( "listList", expectedResultList, listList );
    }

    @Test
    public void testForeach() {
        final List<String> result1 = new ArrayList<String>();
        foreach( list( "1", "2", null ), true, new Functions.UnaryVoid<String>() {
            @Override
            public void call( final String s ) {
                result1.add( s );
            }
        } );
        assertEquals( "List result (nulls)", list( "1", "2", null ), result1 );

        final List<String> result2 = new ArrayList<String>();
        foreach( list( "1", "2", null ), false, new Functions.UnaryVoid<String>() {
            @Override
            public void call( final String s ) {
                result2.add( s );
            }
        } );
        assertEquals( "List result (no nulls)", list( "1", "2" ), result2 );
    }
}
