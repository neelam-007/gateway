package com.l7tech.server.log;

import org.junit.Test;
import org.junit.Assert;

import java.io.ObjectInputStream;
import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.logging.Level;

import com.l7tech.util.ResourceUtils;
import com.l7tech.util.HexUtils;

/**
 * 
 */
public class LogFileConfigurationTest {

    private static final String SER_DUMP = "rO0ABXNyABNqYXZhLnV0aWwuQXJyYXlMaXN0eIHSHZnHYZ0DAAFJAARzaXpleHAAAAABdwQAAAAKc3IAKmNvbS5sN3RlY2guc2VydmVyLmxvZy5Mb2dGaWxlQ29uZmlndXJhdGlvbgAAAAAAAAABAgAGWgAGYXBwZW5kSQAFY291bnRJAAVsZXZlbEkABWxpbWl0TAAHZmlsZXBhdHQAEkxqYXZhL2xhbmcvU3RyaW5nO0wADWZvcm1hdFBhdHRlcm5xAH4AA3hwAQAAAAoAAAK8ATiAAHQAJi9ob21lL3N0ZXZlL3NzZy92YXIvbG9ncy9zc2dfJWdfJXUubG9ndABCJTEkdGIgJTEkdGUsICUxJHRZICUxJHRsOiUxJHRNOiUxJHRTICUxJFRwICU1JGQgJTMkcyVuJTIkczogJTQkcyVueA==";

    @SuppressWarnings({"ConstantConditions"})
    @Test
    public void testReadSerializedLogConfig() throws Exception {
        ObjectInputStream in = null;
        try {
            in = new ObjectInputStream(new ByteArrayInputStream(HexUtils.decodeBase64(SER_DUMP)));
            Object object = in.readObject();

            Assert.assertTrue("read list", object instanceof List);
            Assert.assertEquals("list of one", 1, ((List)object).size());

            Object item =  ((List)object).iterator().next();
            Assert.assertTrue("is log config", item instanceof LogFileConfiguration);

            LogFileConfiguration config = (LogFileConfiguration) item;
            Assert.assertEquals("Log count", 10, config.getCount());
            Assert.assertEquals("Log file", "/home/steve/ssg/var/logs/ssg_%g_%u.log", config.getFilepat());
            Assert.assertEquals("Log pattern", "%1$tb %1$te, %1$tY %1$tl:%1$tM:%1$tS %1$Tp %5$d %3$s%n%2$s: %4$s%n", config.getFormatPattern());
            Assert.assertEquals("Log level", Level.CONFIG.intValue(), config.getLevel());
            Assert.assertEquals("Log limit", 20480000, config.getLimit());
        } finally {
            ResourceUtils.closeQuietly(in);
        }
    }

}
