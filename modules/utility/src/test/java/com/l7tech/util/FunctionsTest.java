package com.l7tech.util;

import static com.l7tech.util.Functions.*;
import org.junit.Test;
import static org.junit.Assert.*;


import java.util.*;
import java.util.regex.Pattern;

/**
 * Test cases for Functions and its inner classes.
 */
public class FunctionsTest {

    // Some lists

    private static final List<String> FRUITS = Arrays.asList("apple", "orange", "pear", "banana", "peach");
    private static final List<String> LANGS = Arrays.asList("perl", "scala", "java", "php", "ruby", "python", "fortran", "jscript");
    private static final List<String> TLAS = Arrays.asList("IBM", "SWF", "PHP", "TLA", "DTR", "BBY");

    // Some predicates

    private static final Unary<Boolean,String> HAS_VOWEL = new Unary<Boolean, String>() {
        final Pattern vowelPat = Pattern.compile("[aeiouyAEIOUY]");
        @Override
        public Boolean call(String s) {
            return vowelPat.matcher(s).find();
        }
    };

    private static final Unary<Boolean,String> HAS_JAY = new Unary<Boolean, String>() {
        @Override
        public Boolean call(String s) {
            return s.contains("j");
        }
    };

    private static final Unary<Boolean,String> CURRENTLY_THOUGHT_COOL_BY_REDDIT = new Unary<Boolean, String>() {
        @Override
        public Boolean call(String s) {
            return "ruby".equals(s) || "python".equals(s) || "haskell".equals(s);
        }
    };

    // Some transforms

    private static final Unary<String,String> TO_UPPER = new Unary<String, String>() {
        @Override
        public String call(String s) {
            return s.toUpperCase();
        }
    };

    private static final Unary<String,String> TO_LOWER = new Unary<String, String>() {
        @Override
        public String call(String s) {
            return s.toLowerCase();
        }
    };

    @Test
    public void testPartial() throws Exception {
        final Pair<String,String> result = new Pair<String, String>( "one", "two" );
        final Binary<Pair<String,String>,String,String> pairer = new Binary<Pair<String,String>,String,String>() {
            @Override
            public Pair<String, String> call( final String left, final String right ) {
                return new Pair<String, String>( left, right );
            }
        };
        assertEquals( "Binary pair", result, partial( pairer, "one" ).call( "two" ) );

        final BinaryThrows<Pair<String,String>,String,String,Exception> pairerThrows = new BinaryThrows<Pair<String,String>,String,String,Exception>() {
            @Override
            public Pair<String, String> call( final String left, final String right ) {
                return new Pair<String, String>( left, right );
            }
        };
        assertEquals( "BinaryThrows pair", result, partial( pairerThrows, "one" ).call( "two" ) );

        final Pair[] holder1 = new Pair[1];
        final BinaryVoid<String,String> pairerVoid = new BinaryVoid<String,String>() {
            @Override
            public void call( final String left, final String right ) {
                holder1[0] = new Pair<String, String>( left, right );
            }
        };
        partial( pairerVoid, "one" ).call( "two" );
        assertEquals( "BinaryVoid pair", result, holder1[0] );

        final Pair[] holder2 = new Pair[1];
        final BinaryVoidThrows<String,String,Exception> pairerVoidThrows = new BinaryVoidThrows<String,String,Exception>() {
            @Override
            public void call( final String left, final String right ) {
                holder2[0] = new Pair<String, String>( left, right );
            }
        };
        partial( pairerVoidThrows, "one" ).call( "two" );
        assertEquals( "BinaryVoidThrows pair", result, holder2[0] );
    }

    @Test
    public void testGrep() throws Exception {
        assertEquals("perl comes first", "perl", grepFirst(LANGS, HAS_VOWEL));
        assertNull("no jay tla", grepFirst(TLAS, HAS_JAY));
        assertEquals("all fruits have vowels", FRUITS, grep(FRUITS, HAS_VOWEL));
        assertTrue("no fruits have jay", grep(FRUITS, HAS_JAY).isEmpty());
        assertEquals("two langs have jay", 2L, (long) grep( LANGS, HAS_JAY ).size() );
        assertEquals("three TLAs lack vowels", 3L, (long) grep( TLAS, negate( HAS_VOWEL ) ).size() );

        Set<String> ordered = new LinkedHashSet<String>();
        Object got = grep(ordered, FRUITS, HAS_VOWEL);
        assertTrue(got == ordered);
        assertEquals("all fruits collected", FRUITS, new ArrayList<String>(ordered));

        grep(ordered, TLAS, HAS_JAY);
        assertEquals("no TLAs added", FRUITS, new ArrayList<String>(ordered));

        grep(ordered, LANGS, CURRENTLY_THOUGHT_COOL_BY_REDDIT);
        assertEquals("must add ruby and python", (long) (FRUITS.size() + 2), (long) ordered.size() );
    }

    @Test
    public void testMap() throws Exception {
        List<String> upperFruits = map(FRUITS, TO_UPPER);

        for (int i = 0; i < upperFruits.size(); ++i) {
            String fruit = upperFruits.get(i);
            assertTrue(!fruit.equals(FRUITS.get(i)));
            assertTrue(fruit.toLowerCase().equals(FRUITS.get(i)));
        }

        List<String> lowered = map(upperFruits, TO_LOWER);
        assertEquals("should return to normal", FRUITS, lowered);
    }

    @Test
    public void testReduce() throws Exception {
        int totalLen = reduce(TLAS, 0, new Binary<Integer, Integer, String>() {
            @Override
            public Integer call(Integer current, String s) {
                return current + s.length();
            }
        });

        assertEquals("TLAs are each three letters long", (long) (3 * TLAS.size()), (long) totalLen );
    }

    @Test
    public void testPropertyTransform() throws Exception {
        List<byte[]> bytes = map(FRUITS, Functions.<byte[],String>propertyTransform(String.class, "bytes"));
        assertEquals( (long) bytes.size(), (long) FRUITS.size() );
        for (int i = 0; i < bytes.size(); ++i)
            assertTrue("bytes match", Arrays.equals(bytes.get(i), FRUITS.get(i).getBytes()));
    }
    
    @Test
    public void testGetterTransform() throws Exception {
        List<String> spacy = map(LANGS, new Unary<String,String>() {
            @Override
            public String call(String s) {
                return "   " + s + "   ";
            }
        });

        assertEquals("got spaced", spacy.get(2), "   java   ");

        List<String> trimmed = map(spacy, Functions.<String,String>getterTransform(String.class.getMethod("trim")));
        assertEquals("must be trimmed back to normal", LANGS, trimmed);
    }
}
