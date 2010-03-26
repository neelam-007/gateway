package com.l7tech.util;

import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import static org.junit.Assert.assertFalse;

/**
 *
 */
public class CharsetsTest {
    private static final Logger logger = Logger.getLogger(CharsetsTest.class.getName());

    private static List<Pair<String, Charset>> getCharsetFields() throws IllegalAccessException {
        List<Pair<String, Charset>> charsetFields = new ArrayList<Pair<String, Charset>>();
        Field[] fields = Charsets.class.getFields();
        for (Field field : fields) {
            if (Modifier.isStatic(field.getModifiers())) {
                final Charset charset = (Charset) field.get(null);
                charsetFields.add(new Pair<String, Charset>(field.getName(), charset));
            }
        }
        return charsetFields;
    }

    @Test
    public void testDuplicateCharsets() throws Exception {
        // Ensure all charset fields initialize cleanly, and that there are no duplicates
        List<Pair<String, Charset>> fields = getCharsetFields();
        Set<String> seen = new HashSet<String>();

        for (Pair<String, Charset> field : fields) {
            String fieldName = field.left;
            Charset charset = field.right;
            String charsetName = charset.name();
            logger.info(fieldName + "=\"" + charset + "\"; " + "  aliases: " + charset.aliases() + (seen.contains(charsetName) ? "  /* DUPE */" : ""));
            assertFalse("duplicate field for: " + charsetName, seen.contains(charsetName));
            if (!"DEFAULT".equals(fieldName))
                seen.add(charsetName);
        }
    }
}
