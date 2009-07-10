package com.l7tech.util;

import java.net.URL;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;

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
     * Check if a domain name is (syntactically) valid.
     *
     * @param domain the domain name
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

        return ok;
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
                String host = test.getHost();
                ok = host!=null && host.length()>0;
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

    //- PRIVATE

    private static final String DOMAIN_ALLOWED_CHARS = LETTERS_LOWER + LETTERS_UPPER + DIGITS + ".-";
    private static final String[] DOMAIN_INVALID_START_OR_END = {"-","."};
    private static final String REGEX_HTTP_URL = "^(?:[hH][tT][tT][pP][sS]?://[a-zA-Z0-9\\._-]{1,255}(?::(?:6(?:[1-4]\\d{3}|(?:5(?:[0-4]\\d{2}|5(?:[0-2]\\d|3[0-5]))))|[1-5]\\d{4}|(?!0)\\d{2,4}|[1-9]))?(?:[\\?/][a-zA-Z0-9$\\-_\\.+!\\*\\?'\\(\\),:/\\\\%@=&;*'~#]{0,1024})?)$";

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
