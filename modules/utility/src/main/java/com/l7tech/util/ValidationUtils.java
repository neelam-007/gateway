package com.l7tech.util;

import com.l7tech.util.Functions.Unary;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collection;
import java.util.regex.Pattern;

import static com.l7tech.util.Option.join;
import static com.l7tech.util.Option.optional;

/**
 * Input validation methods.
 */
public class ValidationUtils {

    //- PUBLIC

    public static final String LETTERS_LOWER = "abcdefghijklmnopqrstuvwxyz";
    public static final String LETTERS_UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    public static final String DIGITS = "0123456789";
    public static final String ALPHA_NUMERIC = LETTERS_LOWER+LETTERS_UPPER+DIGITS;

    /**
     * Check if a string consists only of the given set of characters.
     *
     * @param text the text to check
     * @param allow the allowed characters
     * @return true if valid
     */
    public static boolean isValidCharacters(CharSequence text, String allow) {
        boolean valid = true;

        for(int c=0; c<text.length(); c++) {
            char character = text.charAt(c);
            if(allow.indexOf(character)<0) {
                valid = false;
                break;
            }
        }

        return valid;
    }

    /**
     * Check if a domain name is (syntactically) valid.
     *
     * @param domain the domain name
     * @return true if valid
     */
    public static boolean isValidDomain(String domain) {
        return isValidDomain(domain, false);
    }

    /**
     * Check if a domain name is (syntactically) valid. Literal IP addresses are also considered valid.
     *
     * @param domain the domain name or IP address literal
     * @param allowEmpty true to treat an empty string as valid
     * @return true if valid
     */
    public static boolean isValidDomain(String domain, boolean allowEmpty) {
        boolean present;
        boolean ok = false;

        present = domain != null && domain.length() > 0;

        if (present) {
            boolean validStartAndEnd = true;
            for ( String part : DOMAIN_INVALID_START_OR_END ) {
                if ( domain.startsWith( part ) || domain.endsWith( part ) ) {
                    validStartAndEnd = false;
                    break;
                }
            }

            if(validStartAndEnd && isValidCharacters(domain, DOMAIN_ALLOWED_CHARS)) {
                ok = true;
            }
        }
        else if (allowEmpty) {
            ok = true;
        }

        return ok || InetAddressUtil.isValidIpv6Address(domain);
    }

    /**
     * Checks if the given input is a valid MySQL hostname. MySQL hostnames can contain '%' as a wildcard. They may also have netmasks.
     * See http://dev.mysql.com/doc/refman/5.1/en/account-names.html for the documentation.
     *
     * @param hostName The hostname to validate.
     * @return True if the hostname is a valid mysql hostname. False otherwise.
     */
    public static boolean isValidMySQLHostName(String hostName) {
        if (ValidationUtils.isValidDomain(hostName) || ValidationUtils.isValidDomain(hostName.replace('%', 'P'))) {
            return true;
        }
        //need to check for netmask
        if (hostName.endsWith("/255.255.255.0") ||
                hostName.endsWith("/255.255.0.0") ||
                hostName.endsWith("/255.0.0.0")) {
            return ValidationUtils.isValidDomain(hostName.substring(0, hostName.lastIndexOf('/')));
        }
        return false;
    }

    /**
     * Check if a URI is valid. Intended to validate values for use as an XML Schema anyURI type. Note this validation
     * permits relative URIs.
     *
     * @param uriText the URI text
     * @return null if URI is valid otherwise not null containing error details
     */
    public static String isValidUriString(@Nullable String uriText){
        String error = null;

        boolean present = uriText != null && !uriText.trim().isEmpty();

        if(present){
            try {
                URI test = new URI(uriText);
                if (isQueryWithSquareBracket(test.getQuery())) {
                    error = "Square bracket is not allowed in query.";
                }
            } catch (URISyntaxException e) {
                // so is invalid
                error = ExceptionUtils.getMessage(e);
            }
        } else {
            error = "URI must not be empty";
        }

        return error;
    }

    /**
     * See {@link #isValidUriString(String)} for usage
     *
     * @param uriText the URI text
     * @return true if URI is valid
     */
    public static boolean isValidUri(@Nullable String uriText){
        return isValidUriString(uriText) == null;
    }

    /**
     * Check if an (absolute) URL is valid.
     *
     * @param urlText the URL text
     * @return true if the url is valid
     */
    public static boolean isValidUrl(String urlText) {
        return isValidUrl(urlText, false);
    }

    /**
     * Check if an (absolute) URL is valid.
     *
     * @param urlText the URL text
     * @param allowEmpty true to treat an empty string as valid
     * @return true if the url is valid
     */
    public static boolean isValidUrl(String urlText, boolean allowEmpty) {
        boolean present;
        boolean ok = false;

        present = urlText != null && urlText.length() > 0;

        if (present) {
            try {
                URL test = new URL(urlText);
                if ( !"file".equalsIgnoreCase(test.getProtocol()) ) {
                    String host = test.getHost();
                    ok = host!=null && host.length()>0 && urlText.indexOf(' ') < 0;
                } else {
                    ok = true; // file URLs do not need a host
                }
            } catch (MalformedURLException e) {
                // so is invalid
            }
        }
        else if (allowEmpty) {
            ok = true;
        }

        return ok;
    }

    /**
     * Get a regular expression suitable for validation of an HTTP(S) url.
     *
     * <p>This is not intended to support username/password in the url (e.g. http://user:pass@example.com)</p>
     *
     * @return The url regex.
     */
    public static String getHttpUrlRegex() {
        return REGEX_HTTP_URL;
    }

    /**
     * Check if an (absolute) URL is valid.
     *
     * @param urlText the URL text
     * @param allowEmpty true to treat an empty string as valid
     * @param schemes the permitted URL schemes (null for any)
     * @return true if the url is valid
     */
    public static boolean isValidUrl(String urlText, boolean allowEmpty, Collection<String> schemes) {
        boolean present;
        boolean ok = false;

        present = urlText != null && urlText.length() > 0;

        if (present) {
            try {
                URI test = new URI(urlText);
                String host = test.getHost();
                if (host!=null && host.length()>0) {
                    if (schemes == null || schemes.contains(test.getScheme())) {
                        ok = true;
                    } 
                }
                if (isQueryWithSquareBracket(test.getQuery())) {
                    ok = false;
                }

            } catch (URISyntaxException e) {
                // so is invalid
            }
        }
        else if (allowEmpty) {
            ok = true;
        }

        return ok;
    }

    /**
     * According to RFC 3986 section 3.2.2
     * "A host identified by an Internet Protocol literal address, version
     * [RFC3513] or later, is distinguished by enclosing the IP literal
     * within square brackets ("[" and "]").  This is the only place where
     * square bracket characters are allowed in the URI syntax."
     *
     * "[", "]" are added under RFC2732, however, from Java URI implementation
     * it applied to the whole URL instead only on the Internet Protocol literal address.
     *
     * The query should not allow string with "[" or "]"
     * Refer to bug 11594
     *
     * @param q query String
     * @return True of query contains "[" or "]" otherwise false
     */
    private static boolean isQueryWithSquareBracket(String q) {
        if (q != null) {
            return (q.contains("[") || q.contains("]"));
        }
        return false;
    }



    /**
     * Check if an integer string is valid.
     *
     * @param intText The value as a string
     * @param allowEmpty true to treat an empty string as valid
     * @param min The minimum allowed value (inclusive)
     * @param max The maximum allowed value (inclusive)
     * @return True if a valid integer in the required range.
     */
    public static boolean isValidInteger(String intText, boolean allowEmpty, int min, int max) {
        boolean valid = false;

        if ( intText != null && intText.length() > 0 ) {
            try {
                int value = Integer.parseInt( intText );
                valid = value >= min && value <= max;                
            } catch ( NumberFormatException nfe ) {
                // so is invalid
            }
        } else {
            valid = allowEmpty;
        }

        return valid;
    }
    /**
     * Check if the string is a valid goid (16 bytes hexadecimal value)
     *
     * @param goidText The value as a string
     * @param allowEmpty true to treat an empty string as valid
     * @return True if a goid.
     */
    public static boolean isValidGoid(String goidText, boolean allowEmpty) {
        boolean valid = false;

        if ( goidText != null && goidText.length() > 0 ) {
            try {
                valid =  HexUtils.unHexDump(goidText).length==16;
            } catch ( IOException nfe ) {
                // so is invalid
            }
        } else {
            valid = allowEmpty;
        }

        return valid;
    }

    /**
     * Check if an integer string is a valid long.
     *
     * @param longText The value as a string
     * @param allowEmpty true to treat an empty string as valid
     * @param min The minimum allowed value (inclusive)
     * @param max The maximum allowed value (inclusive)
     * @return True if a valid integer in the required range.
     */
    public static boolean isValidLong(String longText, boolean allowEmpty, long min, long max) {
        boolean valid = false;

        if ( longText != null && longText.length() > 0 ) {
            try {
                long value = Long.parseLong( longText );
                valid = value >= min && value <= max;
            } catch ( NumberFormatException nfe ) {
                // so is invalid
            }
        } else {
            valid = allowEmpty;
        }

        return valid;
    }

    /**
     * Check if an integer string is a valid long.
     *
     * @param doubleText The value as a string
     * @param allowEmpty true to treat an empty string as valid
     * @param min The minimum allowed value (inclusive)
     * @param max The maximum allowed value (inclusive)
     * @return True if a valid integer in the required range.
     */
    public static boolean isValidDouble(String doubleText, boolean allowEmpty, double min, double max) {
        return isValidDouble(doubleText, allowEmpty, min, true, max, true);
    }

    /**
     * Check if an integer string is a valid long.
     *
     * @param doubleText The value as a string
     * @param allowEmpty true to treat an empty string as valid
     * @param min The minimum allowed value
     * @param minInclusive Indicates if the minimum is inclusive or not.
     * @param max The maximum allowed value
     * @param maxInclusive Indicates if the maximum is inclusive or not. 
     * @return True if a valid integer in the required range.
     */
    public static boolean isValidDouble(String doubleText, boolean allowEmpty, double min, boolean minInclusive, double max, boolean maxInclusive) {
        boolean valid = false;

        if ( doubleText != null && doubleText.length() > 0 ) {
            try {
                double value = Double.parseDouble( doubleText );
                valid = (minInclusive? value >= min : value > min) && (maxInclusive? value <= max : value < max);
            } catch ( NumberFormatException nfe ) {
                // so is invalid
            }
        } else {
            valid = allowEmpty;
        }

        return valid;
    }

    /**
     * Check if the given name is probably a valid XML name.
     *
     * <p>This method is slightly permissive, since it does not forbid
     * some of the blocks of higher range unicode characters that are
     * not permitted (though there are exceptions within those blocks
     * that are permitted).</p>
     *
     * @param name The name to check
     * @return true if probably valid
     */
    public static boolean isProbablyValidXmlName( final String name ) {
        boolean valid = false;

        if ( name != null ) {
            char[] nameChars = name.toCharArray();
            out:
            if ( nameChars.length > 0 && isValidXmlNameStart(nameChars[0]) ) {
                for ( char character : nameChars ) {
                    if ( !isValidXmlNameCharacter( character ) ) {
                        break out;
                    }
                }
                valid = true;
            }
        }

        return valid;
    }

    /**
     * Check if the given name is probably a valid XML namespace prefix.
     *
     * <p>This method is slightly permissive, since it does not forbid
     * some of the blocks of higher range unicode characters that are
     * not permitted (though there are exceptions within those blocks
     * that are permitted).</p>
     *
     * @param namespacePrefix The namespace prefix to check
     * @return true if probably valid
     */
    public static boolean isProbablyValidXmlNamespacePrefix( final String namespacePrefix ) {
        boolean valid = false;

        if ( namespacePrefix != null ) {
            char[] nameChars = namespacePrefix.toCharArray();
            out:
            if ( nameChars.length > 0 && isValidXmlNameStart(nameChars[0]) ) {
                for ( char character : nameChars ) {
                    if ( !isValidXmlNamespacePrefixCharacter( character ) ) {
                        break out;
                    }
                }
                valid = true;
            }
        }

        return valid;
    }

    /**
     * Get a validator for an integer (string) in the described range.
     *
     * @param min The minimum value (inclusive)
     * @param max The maximum value (inclusive)
     * @return The validator
     */
    public static Validator<String> getIntegerTextValidator( final int min,
                                                             final int max  ) {
        return new PredicatedValidator<Integer,String>(
                ConversionUtils.getTextToIntegerConverter(),
                minMaxPredicate( min, max ) );
    }

    /**
     * Get a validator for an integer in the described range.
     *
     * <p>NOTE: Validators are serializable, so the given converter should be
     * serializable.</p>
     *
     * @param converter Converter from the input representation to an integer
     * @param min The minimum value (inclusive)
     * @param max The maximum value (inclusive)
     * @param <S> The source (input) type
     * @return The validator
     */
    public static <S> Validator<S> getIntegerValidator( @NotNull final Unary<Option<Integer>,S> converter,
                                                        final int min,
                                                        final int max  ) {
        return new PredicatedValidator<Integer,S>( converter, minMaxPredicate( min, max ) );
    }

    /**
     * Get a validator for a long (string) in the described range.
     *
     * @param min The minimum value (inclusive)
     * @param max The maximum value (inclusive)
     * @return The validator
     */
    public static Validator<String> getLongTextValidator( final long min,
                                                          final long max ) {
        return new PredicatedValidator<Long,String>(
                ConversionUtils.getTextToLongConverter(),
                minMaxPredicate( min, max ) );
    }

    /**
     * Get a validator for a long in the described range.
     *
     * <p>NOTE: Validators are serializable, so the given converter should be
     * serializable.</p>
     *
     * @param converter Converter from the input representation to a long
     * @param min The minimum value (inclusive)
     * @param max The maximum value (inclusive)
     * @param <S> The source (input) type
     * @return The validator
     */
    public static <S> Validator<S> getLongValidator( @NotNull final Unary<Option<Long>,S> converter,
                                                     final long min,
                                                     final long max  ) {
        return new PredicatedValidator<Long,S>( converter, minMaxPredicate( min, max ) );
    }

    /**
     * Get the validator that uses a regular expression.
     *
     * @param pattern The pattern to use.
     * @return The validator
     */
    public static Validator<String> getPatternTextValidator( @NotNull final Pattern pattern ) {
        return new PredicatedValidator<String,String>(
                ConversionUtils.<String>getIdentityConverter(),
                regexPredicate(pattern) );
    }

    /**
     * Get a predicate for a value range.
     *
     * @param min The minimum permitted value
     * @param max The maximum permitted value
     * @param <T> The value type
     * @return The predicate
     */
    public static <T extends Number> Unary<Boolean,T> getMinMaxPredicate( @NotNull T min,
                                                                          @NotNull T max ) {
        return minMaxPredicate( min, max );
    }

    public static boolean isValidMimeHeaderName(String name) {
        return getMimeHeaderNameMessage(name) == null;
    }

    private static final Pattern WHITESPACE = Pattern.compile("\\s");

    public static String getMimeHeaderNameMessage(final String name) {
        if (name == null)
            return "name is null";

        if (WHITESPACE.matcher(name).find())
            return "name contains whitespace";

        // TODO additional validation

        return null;
    }

    public static boolean isValidMimeHeaderValue(String value) {
        return getMimeHeaderValueMessage(value) == null;
    }

    public static String getMimeHeaderValueMessage(final String name) {
        if (name == null)
            return "value is null";

        // TODO additional validation

        return null;
    }

    /**
     * Abstract base class for Validators.
     *
     * @param <S> The source type
     */
    public static abstract class Validator<S> implements Serializable, Unary<Boolean,S> {

        /**
         * Validate the given value.
         *
         * @param value The value being validated
         * @return True if valid
         */
        public abstract boolean isValid( @Nullable S value );

        /**
         * Convenience implementation of a Unary Function that calls <code>isValid</code>.
         *
         * @param value The value to validate
         * @return True if valid
         */
        @Override
        public final Boolean call( final S value ) {
            return isValid( value );
        }
    }

    //- PRIVATE

    private static final String DOMAIN_ALLOWED_CHARS = LETTERS_LOWER + LETTERS_UPPER + DIGITS + ".-";
    private static final String[] DOMAIN_INVALID_START_OR_END = {"-","."};
    private static final String REGEX_HTTP_URL = "^(?:[hH][tT][tT][pP][sS]?://[a-zA-Z0-9\\._-]{1,255}(?::(?:6(?:[1-4]\\d{3}|(?:5(?:[0-4]\\d{2}|5(?:[0-2]\\d|3[0-5]))))|[1-5]\\d{4}|(?!0)\\d{2,4}|[1-9]))?(?:[\\?/][a-zA-Z0-9$\\-_\\.+!\\*\\?'\\(\\),:/\\\\%@=&;*'~#]{0,1024})?)$";


    private static <T extends Number> ValidationPredicate<T> minMaxPredicate( @NotNull final T min,
                                                                              @NotNull final T max ) {
        return new ValidationPredicate<T>(){
            @Override
            public Boolean call( final T t ) {
                return t.longValue() >= min.longValue() && t.longValue() <= max.longValue();
            }
        };
    }

    private static ValidationPredicate<String> regexPredicate( @NotNull final Pattern pattern ) {
        return new ValidationPredicate<String>(){
            @Override
            public Boolean call( final String text ) {
                return pattern.matcher( text ).matches();
            }
        };
    }

    private static interface ValidationPredicate<T> extends Serializable, Unary<Boolean, T> {}

    private static final class PredicatedValidator<T,S> extends Validator<S> {
        private final Unary<Option<T>,S> converter;
        private final ValidationPredicate<T> predicate;

        private PredicatedValidator( @NotNull final Unary<Option<T>,S> converter,
                                     @NotNull final ValidationPredicate<T> predicate ) {
            this.converter = converter;
            this.predicate = predicate;
        }

        @Override
        public final boolean isValid( final S value ) {
            return join( optional( value ).map( converter ) ).exists( predicate );
        }
    }

    /**
     * The XML spec lists these characters as permitted :
     *   Ll, Lu, Lo, Lm, Lt, or Nl, or else be '_' (#x5F)
     *
     * In Java categories:
     *   LOWERCASE_LETTER
     *   UPPERCASE_LETTER
     *   OTHER_LETTER
     *   MODIFIER_LETTER
     *   TITLECASE_LETTER
     *   LETTER_NUMBER
     */
    private static boolean isValidXmlNameStart( final char character ) {
        int type = Character.getType( character );

        return character=='_' ||
                type==Character.LOWERCASE_LETTER ||
                type==Character.UPPERCASE_LETTER ||
                type==Character.OTHER_LETTER ||
                type==Character.MODIFIER_LETTER ||
                type==Character.TITLECASE_LETTER ||
                type==Character.LETTER_NUMBER;
    }

    /**
     * The XML spec lists these characters as permitted :
     *   Mc, Mn, Nd, Pc, or Cf '-' (#x2D), '.' (#x2E), ':' (#x3A) or '·' (#xB7; middle dot)
     *
     * This is in addition to those permitted for the name start.
     *
     * In Java categories:
     *   COMBINING_SPACING_MARK
     *   NON_SPACING_MARK
     *   DECIMAL_DIGIT_NUMBER
     *   CONNECTOR_PUNCTUATION
     *   FORMAT
     */
    private static boolean isValidXmlNameCharacter( final char character ) {
        int type = Character.getType( character );

        return isValidXmlNameStart( character ) ||
                character==0x2D ||
                character==0x2E ||
                character==0x3A ||
                character==0xB7 ||
                type==Character.COMBINING_SPACING_MARK ||
                type==Character.NON_SPACING_MARK ||
                type==Character.DECIMAL_DIGIT_NUMBER ||
                type==Character.CONNECTOR_PUNCTUATION ||
                type==Character.FORMAT;
    }

    /**
     * Any valid name character, except ':'.
     */
    private static boolean isValidXmlNamespacePrefixCharacter( final char character ) {
        return character != ':' && isValidXmlNameCharacter( character );    
    }
}
