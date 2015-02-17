package com.l7tech.util;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Already-looked-up charsets, for creating new String instances with new String(bytes, Charsets.UTF8) rather than new String(bytes, "UTF-8").
 */
public class Charsets {
    public static final Charset DEFAULT = Charset.defaultCharset();
    public static final Charset ASCII = StandardCharsets.US_ASCII;
    public static final Charset UTF8 = StandardCharsets.UTF_8;
    public static final Charset UTF16 = StandardCharsets.UTF_16; // Optional byte-order mark
    public static final Charset UTF16LE = StandardCharsets.UTF_16LE;
    public static final Charset UTF16BE = StandardCharsets.UTF_16BE;
    public static final Charset X_UTF16LE_BOM = Charset.forName("X-UTF-16LE-BOM");
    public static final Charset UTF32 = Charset.forName("UTF-32"); // probably mandatory BOM
    public static final Charset UTF32LE = Charset.forName("UTF-32LE");
    public static final Charset UTF32BE = Charset.forName("UTF-32BE");
    //public static final Charset CP1047 = Charset.forName("Cp1047"); // aka. IBM EBCDIC 1047.  Charset reported not to be available in SSM applet running on Windows XP
    public static final Charset ISO8859 = StandardCharsets.ISO_8859_1;
    //public static final Charset WINDOWS1252 = Charset.forName("windows-1252"); // Seems to work everywhere, but commented out as it is not currently required
}
