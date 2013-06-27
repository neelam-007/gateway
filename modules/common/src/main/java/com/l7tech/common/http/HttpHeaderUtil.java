package com.l7tech.common.http;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Class for utility methods pertaining to HTTP headers.
 */
public class HttpHeaderUtil {
    private HttpHeaderUtil() {}

    // Respect whole-word "gzip" or "*", unless the value tries to use any fancy "identity" or qvalue preferences in which case NO GZIP FOR YOU
    private static final Pattern ACCEPT_GZIP = Pattern.compile("(?!.*(?:;q=|identity).*)(?:^|\\b|\\s)(?:gzip|\\*)(?:\\b|\\s|$)", Pattern.CASE_INSENSITIVE);

    /**
     * Check if the specified accept-encoding header appears to allow a gzipped response.
     *
     * @param header the header value to examine.  If null, this method will always return false.
     * @return true if this header value appears to allow either "gzip" or "*" encodings with nonzero qvalue.
     */
    public static boolean acceptsGzipResponse(@Nullable String header) {
        // The spec for accept headers is hilariously, eye-poppingly overdesigned, allowing a client to specify (for example)
        // "gzip;q=3,deflate,compress;q=0.1,identity;q=0.9", which would appear to mean that this particular client
        // likes gzip precisely three times more than it likes deflate, and supports compress as well but would prefer you don't
        // use it unless you really need to, in fact even better to use no encoding at all in that case, there's a dear,
        // but if you must use compress anyway I understand and won't hold it against you

        // We'll deal with only the simplest subset of valid header values (no identity or qvalue preferences),
        // defaulting to false if there is any doubt.
        return header != null && ACCEPT_GZIP.matcher(header).find();
    }

    /**
     * bug fix for SSG-6921
     * searches one header value out of multiple according to search rules
     * - Accepted values are: off, first, last, 0, 1, 2, 3, ...n,  -1, -2, -3, ...
     * - Positive values define index from head
     * - Negative values specify index from tail
     * - When search rule is "off" or null, value is treated as a single header value
     * and GenericHttpException is thrown if multiple headers found
     * @param headers - HttpHeaders
     * @param name - String header name
     * @param searchRule- String see explanation above
     * @return - String value of the header
     * @throws GenericHttpException
     */
    public static String searchHeaderValue(@NotNull final HttpHeaders headers, @NotNull final String name, @Nullable final String searchRule) throws GenericHttpException {
        String val = null;
        //use header rules to get the proper header out of the multiple headers
        if(searchRule != null && !searchRule.trim().equalsIgnoreCase("off")) {
            List<String> vals = headers.getValues(name);
            if(!vals.isEmpty()) {
                String rule = searchRule.trim().toLowerCase();
                try {
                    final int lastIndex = vals.size() - 1;
                    //get the first value
                    if(rule.equals("first") || rule.equals("0")) {
                        val = vals.get(0);
                    }
                    else {
                        //get the last value
                        if(rule.equals("last") || rule.equals(Integer.toString(lastIndex))) {
                            val = vals.get(lastIndex);
                        }
                        else {
                            //get numerical value of the header
                            try {
                                int i =  Integer.parseInt(rule);
                                if(i > 0) {
                                    val = vals.get(i);
                                }
                                else {
                                    val = vals.get(lastIndex + i); //negative values means we searching from tail
                                }
                            } catch (NumberFormatException e) {
                                throw new GenericHttpException("Cannot parse header " + name + " index " + rule);
                            }
                        }
                    }
                } catch (IndexOutOfBoundsException e) {
                    throw new GenericHttpException("Incorrect header index (" + rule +  ") found for header " + name);
                }
            }
        }
        else {
            val = headers.getOnlyOneValue(name);//default behavior does not expect more then one header value
        }

        return val;
    }
}
