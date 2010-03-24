package com.l7tech.util;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.Charset;

/**
 * Already-looked-up charsets, for creating new String instances with new String(bytes, Charsets.UTF8) rather than new String(bytes, "UTF-8").
 */
public class Charsets {
    public static final Charset DEFAULT = Charset.defaultCharset();
    public static final Charset ASCII = Charset.forName("ASCII");
    public static final Charset UTF8 = Charset.forName("UTF-8");
    public static final Charset UTF16LE = Charset.forName("UTF-16LE");
    public static final Charset UTF16BE = Charset.forName("UTF-16BE");
    public static final Charset X_UTF16LE_BOM = Charset.forName("X-UTF-16LE-BOM");
    public static final Charset UTF32LE = Charset.forName("UTF-32LE");
    public static final Charset UTF32BE = Charset.forName("UTF-32BE");
    public static final Charset CP1047 = Charset.forName("Cp1047");
    public static final Charset ISO8859 = Charset.forName("ISO-8859-1");
    public static final Charset WINDOWS1252 = Charset.forName("windows-1252");

    public static void main(String[] args) throws Exception {
        Field[] fields = Charsets.class.getFields();
        for (Field field : fields) {
            if (Modifier.isStatic(field.getModifiers()))
                System.out.println(field.getDeclaringClass().getSimpleName() + "." + field.getName() + "=\"" + field.get(null) + "\";  /* Present */");
        }
    }
}
