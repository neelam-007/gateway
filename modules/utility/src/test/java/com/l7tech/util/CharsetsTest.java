package com.l7tech.util;

import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import static org.junit.Assert.assertFalse;

/**
 *
 */
public class CharsetsTest {
    private static final Logger logger = Logger.getLogger(CharsetsTest.class.getName());

    @Test
    public void testDuplicateCharsets() throws Exception {
        // Ensure all charset fields initialize cleanly, and that there are no duplicates
        Set<String> seen = new HashSet<String>();
        Field[] fields = Charsets.class.getFields();
        for (Field field : fields) {
            if (Modifier.isStatic(field.getModifiers())) {
                final Charset charset = (Charset) field.get(null);
                String name = charset.name();
                logger.fine(field.getDeclaringClass().getSimpleName() + "." + field.getName() + "=\"" + charset + "\";" + (seen.contains(name) ? "  /* DUPE */" : ""));
                assertFalse("duplicate alias for: " + name, seen.contains(name));
                if (!"DEFAULT".equals(field.getName()))
                    seen.add(name);
            }
        }
    }
}
