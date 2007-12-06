package com.l7tech.common.util;

import java.net.URL;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;

/**
 * Input validation methods.
 *
 * @author $Author$
 * @version $Revision$
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
        boolean present = false;
        boolean ok = false;

        present = domain != null && domain.length() > 0;

        if (present) {
            boolean validStartAndEnd = true;
            for(int i=0; i<DOMAIN_INVALID_START_OR_END.length; i++) {
                String part = DOMAIN_INVALID_START_OR_END[i];
                if(domain.startsWith(part) || domain.endsWith(part)) {
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
        boolean present = false;
        boolean ok = false;

        present = urlText != null && urlText.length() > 0;

        if (present) {
            try {
                URL test = new URL(urlText);
                String host = test.getHost();
                ok = host!=null && host.length()>0;
            } catch (MalformedURLException e) {
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
     * @param allowEmpty true to treat an empty string as valid
     * @param schemes the permitted URL schemes (null for any)
     * @return true if the url is valid
     */
    public static boolean isValidUrl(String urlText, boolean allowEmpty, Collection<String> schemes) {
        boolean present = false;
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
            }
        } else {
            valid = allowEmpty;
        }

        return valid;
    }

    //- PRIVATE

    private static final String DOMAIN_ALLOWED_CHARS = LETTERS_LOWER + LETTERS_UPPER + DIGITS + ".-";
    private static final String[] DOMAIN_INVALID_START_OR_END = {"-","."};
}
