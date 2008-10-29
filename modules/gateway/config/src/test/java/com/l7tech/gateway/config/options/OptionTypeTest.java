package com.l7tech.gateway.config.options;

import org.junit.Test;
import org.junit.Assert;
import com.l7tech.gateway.config.client.options.OptionType;

import java.util.regex.Pattern;

/**
 *
 */
public class OptionTypeTest {

    @Test
    public void testRegexCompilation() {
        for ( OptionType type : OptionType.values() ) {
            if ( type.getDefaultRegex() != null ) {
                Pattern.compile(type.getDefaultRegex());
            }
        }
    }

    @Test
    public void testTimeStampRegex() {
        Assert.assertTrue("Regex passes", Pattern.matches(OptionType.TIMESTAMP.getDefaultRegex(), "2001-01-31 00:00:00"));
        Assert.assertTrue("Regex passes", Pattern.matches(OptionType.TIMESTAMP.getDefaultRegex(), "2001-12-01 01:59:59"));
        Assert.assertTrue("Regex passes", Pattern.matches(OptionType.TIMESTAMP.getDefaultRegex(), "2001-10-10 00:00:01"));
        Assert.assertTrue("Regex passes", Pattern.matches(OptionType.TIMESTAMP.getDefaultRegex(), "0000-02-20 00:10:10"));

        Assert.assertFalse("Regex fails", Pattern.matches(OptionType.TIMESTAMP.getDefaultRegex(), ""));
        Assert.assertFalse("Regex fails", Pattern.matches(OptionType.TIMESTAMP.getDefaultRegex(), "01-12-00 01:59:59"));
        Assert.assertFalse("Regex fails", Pattern.matches(OptionType.TIMESTAMP.getDefaultRegex(), "2001-02-20 30:10:10"));
        Assert.assertFalse("Regex fails", Pattern.matches(OptionType.TIMESTAMP.getDefaultRegex(), "2001-02-20 00:60:10"));
        Assert.assertFalse("Regex fails", Pattern.matches(OptionType.TIMESTAMP.getDefaultRegex(), "2001-02-20 00:10:60"));
    }

    @Test
    public void testBooleanRegex() {
        Assert.assertTrue("Regex passes", Pattern.matches(OptionType.BOOLEAN.getDefaultRegex(), "true"));
        Assert.assertTrue("Regex passes", Pattern.matches(OptionType.BOOLEAN.getDefaultRegex(), "yes"));
        Assert.assertTrue("Regex passes", Pattern.matches(OptionType.BOOLEAN.getDefaultRegex(), "t"));
        Assert.assertTrue("Regex passes", Pattern.matches(OptionType.BOOLEAN.getDefaultRegex(), "y"));
        Assert.assertTrue("Regex passes", Pattern.matches(OptionType.BOOLEAN.getDefaultRegex(), "True"));
        Assert.assertTrue("Regex passes", Pattern.matches(OptionType.BOOLEAN.getDefaultRegex(), "Yes"));
        Assert.assertTrue("Regex passes", Pattern.matches(OptionType.BOOLEAN.getDefaultRegex(), "false"));
        Assert.assertTrue("Regex passes", Pattern.matches(OptionType.BOOLEAN.getDefaultRegex(), "no"));
        Assert.assertTrue("Regex passes", Pattern.matches(OptionType.BOOLEAN.getDefaultRegex(), "f"));
        Assert.assertTrue("Regex passes", Pattern.matches(OptionType.BOOLEAN.getDefaultRegex(), "n"));
        Assert.assertTrue("Regex passes", Pattern.matches(OptionType.BOOLEAN.getDefaultRegex(), "False"));
        Assert.assertTrue("Regex passes", Pattern.matches(OptionType.BOOLEAN.getDefaultRegex(), "No"));

        Assert.assertFalse("Regex fails", Pattern.matches(OptionType.BOOLEAN.getDefaultRegex(), ""));
        Assert.assertFalse("Regex fails", Pattern.matches(OptionType.BOOLEAN.getDefaultRegex(), "fa"));
        Assert.assertFalse("Regex fails", Pattern.matches(OptionType.BOOLEAN.getDefaultRegex(), "y e s"));
        Assert.assertFalse("Regex fails", Pattern.matches(OptionType.BOOLEAN.getDefaultRegex(), "asdf"));
        Assert.assertFalse("Regex fails", Pattern.matches(OptionType.BOOLEAN.getDefaultRegex(), "yy"));
    }
}
