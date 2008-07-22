package com.l7tech.util;

import static com.l7tech.util.Functions.*;
import com.l7tech.util.Functions;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Test cases for Functions and its inner classes.
 */
public class FunctionsTest extends TestCase {
    private static final Logger log = Logger.getLogger(FunctionsTest.class.getName());

    public FunctionsTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(FunctionsTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    // Some lists

    private static final List<String> FRUITS = Arrays.asList("apple", "orange", "pear", "banana", "peach");
    private static final List<String> LANGS = Arrays.asList("perl", "scala", "java", "php", "ruby", "python", "fortran", "jscript");
    private static final List<String> TLAS = Arrays.asList("IBM", "SWF", "PHP", "TLA", "DTR", "BBY");

    // Some predicates

    private static final Unary<Boolean,String> HAS_VOWEL = new Unary<Boolean, String>() {
        final Pattern vowelPat = Pattern.compile("[aeiouyAEIOUY]");
        public Boolean call(String s) {
            return vowelPat.matcher(s).find();
        }
    };

    private static final Unary<Boolean,String> HAS_JAY = new Unary<Boolean, String>() {
        public Boolean call(String s) {
            return s.contains("j");
        }
    };

    private static final Unary<Boolean,String> CURRENTLY_THOUGHT_COOL_BY_REDDIT = new Unary<Boolean, String>() {
        public Boolean call(String s) {
            return "ruby".equals(s) || "python".equals(s) || "haskell".equals(s);
        }
    };

    // Some transforms

    private static final Unary<String,String> TO_UPPER = new Unary<String, String>() {
        public String call(String s) {
            return s.toUpperCase();
        }
    };

    private static final Unary<String,String> TO_LOWER = new Unary<String, String>() {
        public String call(String s) {
            return s.toLowerCase();
        }
    };
    
    public void testGrep() throws Exception {
        assertEquals("perl comes first", "perl", grepFirst(LANGS, HAS_VOWEL));
        assertNull("no jay tla", grepFirst(TLAS, HAS_JAY));
        assertEquals("all fruits have vowels", FRUITS, grep(FRUITS, HAS_VOWEL));
        assertTrue("no fruits have jay", grep(FRUITS, HAS_JAY).isEmpty());
        assertEquals("two langs have jay", 2, grep(LANGS, HAS_JAY).size());
        assertEquals("three TLAs lack vowels", 3, grep(TLAS, negate(HAS_VOWEL)).size());

        Set<String> ordered = new LinkedHashSet<String>();
        Object got = grep(ordered, FRUITS, HAS_VOWEL);
        assertTrue(got == ordered);
        assertEquals("all fruits collected", FRUITS, new ArrayList<String>(ordered));

        grep(ordered, TLAS, HAS_JAY);
        assertEquals("no TLAs added", FRUITS, new ArrayList<String>(ordered));

        grep(ordered, LANGS, CURRENTLY_THOUGHT_COOL_BY_REDDIT);
        assertEquals("must add ruby and python", FRUITS.size() + 2, ordered.size());
    }

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

    public void testReduce() throws Exception {
        int totalLen = reduce(TLAS, 0, new Binary<Integer, Integer, String>() {
            public Integer call(Integer current, String s) {
                return current + s.length();
            }
        });

        assertEquals("TLAs are each three letters long", 3 * TLAS.size(), totalLen);
    }

    public void testPropertyTransform() throws Exception {
        List<byte[]> bytes = map(FRUITS, Functions.<byte[],String>propertyTransform(String.class, "bytes"));
        assertEquals(bytes.size(), FRUITS.size());
        for (int i = 0; i < bytes.size(); ++i)
            assertTrue("bytes match", Arrays.equals(bytes.get(i), FRUITS.get(i).getBytes()));
    }
    
    public void testGetterTransform() throws Exception {
        List<String> spacy = map(LANGS, new Unary<String,String>() {
            public String call(String s) {
                return "   " + s + "   ";
            }
        });

        assertEquals("got spaced", spacy.get(2), "   java   ");

        List<String> trimmed = map(spacy, Functions.<String,String>getterTransform(String.class.getMethod("trim")));
        assertEquals("must be trimmed back to normal", LANGS, trimmed);
    }
}
