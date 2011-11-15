package com.l7tech.common.http;

import org.jetbrains.annotations.Nullable;

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
}
