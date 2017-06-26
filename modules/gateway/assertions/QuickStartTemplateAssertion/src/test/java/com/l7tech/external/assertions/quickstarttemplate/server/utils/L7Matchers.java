package com.l7tech.external.assertions.quickstarttemplate.server.utils;

import org.hamcrest.Description;
import org.hamcrest.Matchers;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Assert;

import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 */
public class L7Matchers {
    private static class RegexMatcher extends TypeSafeMatcher<String> {
        private final String regex;

        private RegexMatcher(final String regex) {
            this.regex = regex;
        }

        @Override
        public void describeTo(final Description description) {
            description.appendText("matches regular expression=\"" + regex + "\"");
        }

        @Override
        public boolean matchesSafely(final String string) {
            Assert.assertThat(string, Matchers.notNullValue());
            return string.matches(regex);
        }
    }
    // matcher method you can call on this matcher class
    public static RegexMatcher matchesRegex(final String regex) {
        Assert.assertThat(regex, Matchers.not(Matchers.isEmptyOrNullString()));
        return new RegexMatcher(regex);
    }

    /**
     * Custom Matcher to match different impl of maps
     */
    private static final class MapMatcher extends TypeSafeMatcher<Map<?, ?>> {
        private final Map<?, ?> expected;

        private MapMatcher(final Map<?, ?> expected) {
            Assert.assertNotNull(expected);
            this.expected = expected;
        }

        @Override
        protected void describeMismatchSafely(final Map<?, ?> item, final Description description) {
            description.appendText("was ").appendValue(toSortedMap(item));
        }

        @Override
        public void describeTo(final Description description) {
            description.appendValue(toSortedMap(expected));
        }

        @Override
        protected boolean matchesSafely(final Map<?, ?> item) {
            return item == expected || (item != null && expected != null && item.equals(expected));
        }

        private static <K, V> Map<K, V> toSortedMap(final Map<K, V> map) {
            if (map == null)
                return null;
            else if (map instanceof SortedMap)
                return map;
            return new TreeMap<>(map);
        }
    }
    public static MapMatcher mapMatcher(final Map<?, ?> expected) {
        return new MapMatcher(expected);
    }
}
