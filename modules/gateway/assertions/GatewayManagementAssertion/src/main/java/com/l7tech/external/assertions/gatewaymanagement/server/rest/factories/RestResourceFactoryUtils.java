package com.l7tech.external.assertions.gatewaymanagement.server.rest.factories;

import com.l7tech.objectmodel.Goid;
import com.l7tech.util.Functions;

import java.util.Arrays;
import java.util.List;

/**
 * This contains utilities used by resourceFactories.
 *
 * @author Victor Kazakov
 */
public class RestResourceFactoryUtils {
    /**
     * The default string conversion function to use when building search filters.
     */
    public static Functions.UnaryThrows<String, String, IllegalArgumentException> stringConvert = new StringConvert();

    /**
     * The default boolean conversion function to use when building search filters.
     */
    public static Functions.UnaryThrows<Boolean, String, IllegalArgumentException> booleanConvert = new Functions.UnaryThrows<Boolean, String, IllegalArgumentException>() {
        @Override
        public Boolean call(String s) throws IllegalArgumentException {
            return Boolean.parseBoolean(s);
        }
    };

    /**
     * The default id(goid) conversion function to use when building search filters.
     */
    public static Functions.UnaryThrows<Goid, String, IllegalArgumentException> goidConvert = new Functions.UnaryThrows<Goid, String, IllegalArgumentException>() {
        @Override
        public Goid call(String s) throws IllegalArgumentException {
            return s == null || s.isEmpty() ? null : Goid.parseGoid(s);
        }
    };

    /**
     * The default long conversion function to use when building search filters.
     */
    public static Functions.UnaryThrows<Long, String, IllegalArgumentException> longConvert = new Functions.UnaryThrows<Long, String, IllegalArgumentException>() {
        @Override
        public Long call(String s) throws IllegalArgumentException {
            return Long.parseLong(s);
        }
    };


    /**
     * The default int conversion function to use when building search filters.
     */
    public static Functions.UnaryThrows<Integer, String, IllegalArgumentException> intConvert = new Functions.UnaryThrows<Integer, String, IllegalArgumentException>() {
        @Override
        public Integer call(String s) throws IllegalArgumentException {
            return Integer.parseInt(s);
        }
    };

    /**
     * Creates a string conversion function that allows a specific set of string to be used. Can also specify case
     * sensitivity. False by default
     */
    public static class StringConvert implements Functions.UnaryThrows<String, String, IllegalArgumentException> {
        private final List<String> allowedStrings;
        private final boolean caseSensitive;

        StringConvert(String... allowedStrings) {
            this(false, allowedStrings);
        }

        StringConvert(boolean caseSensitive, String... allowedStrings) {
            this.caseSensitive = caseSensitive;
            this.allowedStrings = allowedStrings == null ? null : caseSensitive ? Arrays.asList(allowedStrings) : Functions.map(Arrays.asList(allowedStrings), new Functions.Unary<String, String>() {
                @Override
                public String call(String s) {
                    return s.toLowerCase();
                }
            });
        }

        @Override
        public String call(String s) throws IllegalArgumentException {
            if (allowedStrings != null && !allowedStrings.isEmpty() && !allowedStrings.contains(caseSensitive ? s : s.toLowerCase())) {
                throw new IllegalArgumentException("Invalid Parameter " + s + " expected one of: " + allowedStrings.toString());
            }
            return s;
        }
    }
}
