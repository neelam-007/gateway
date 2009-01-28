package com.l7tech.config.client.options;

import org.junit.Test;
import org.junit.Assert;

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
    public void testPortRegex() {
        Assert.assertTrue("Regex passes", Pattern.matches(OptionType.PORT.getDefaultRegex(), "1"));
        Assert.assertTrue("Regex passes", Pattern.matches(OptionType.PORT.getDefaultRegex(), "65535"));
        Assert.assertTrue("Regex passes", Pattern.matches(OptionType.PORT.getDefaultRegex(), "12345"));
        Assert.assertTrue("Regex passes", Pattern.matches(OptionType.PORT.getDefaultRegex(), "11"));
        Assert.assertTrue("Regex passes", Pattern.matches(OptionType.PORT.getDefaultRegex(), "123"));
        Assert.assertTrue("Regex passes", Pattern.matches(OptionType.PORT.getDefaultRegex(), "1239"));

        Assert.assertFalse("Regex fails", Pattern.matches(OptionType.PORT.getDefaultRegex(), "0"));
        Assert.assertFalse("Regex fails", Pattern.matches(OptionType.PORT.getDefaultRegex(), "-1"));
        Assert.assertFalse("Regex fails", Pattern.matches(OptionType.PORT.getDefaultRegex(), "65536"));
        Assert.assertFalse("Regex fails", Pattern.matches(OptionType.PORT.getDefaultRegex(), "77777"));
        Assert.assertFalse("Regex fails", Pattern.matches(OptionType.PORT.getDefaultRegex(), "B"));
        Assert.assertFalse("Regex fails", Pattern.matches(OptionType.PORT.getDefaultRegex(), ""));
    }

    @Test
    public void testIntegerRegex() {
        Assert.assertTrue("Regex passes", Pattern.matches(OptionType.INTEGER.getDefaultRegex(), "0"));
        Assert.assertTrue("Regex passes", Pattern.matches(OptionType.INTEGER.getDefaultRegex(), "1"));
        Assert.assertTrue("Regex passes", Pattern.matches(OptionType.INTEGER.getDefaultRegex(), Integer.toString(Integer.MAX_VALUE)));
        Assert.assertTrue("Regex passes", Pattern.matches(OptionType.INTEGER.getDefaultRegex(), "123456789"));
        Assert.assertTrue("Regex passes", Pattern.matches(OptionType.INTEGER.getDefaultRegex(), "987654321"));
        Assert.assertTrue("Regex passes", Pattern.matches(OptionType.INTEGER.getDefaultRegex(), "12"));
        Assert.assertTrue("Regex passes", Pattern.matches(OptionType.INTEGER.getDefaultRegex(), "1230"));

        Assert.assertFalse("Regex fails", Pattern.matches(OptionType.INTEGER.getDefaultRegex(), "-1"));
        Assert.assertFalse("Regex fails", Pattern.matches(OptionType.INTEGER.getDefaultRegex(), "01"));
        Assert.assertFalse("Regex fails", Pattern.matches(OptionType.INTEGER.getDefaultRegex(), Integer.toString(Integer.MAX_VALUE)+"0") );
        Assert.assertFalse("Regex fails", Pattern.matches(OptionType.INTEGER.getDefaultRegex(), "3333333333"));
        Assert.assertFalse("Regex fails", Pattern.matches(OptionType.INTEGER.getDefaultRegex(), "B"));
        Assert.assertFalse("Regex fails", Pattern.matches(OptionType.INTEGER.getDefaultRegex(), ""));
    }

    @Test
    public void testIpRegex() {
        Assert.assertTrue("Regex passes", Pattern.matches(OptionType.IP_ADDRESS.getDefaultRegex(), "*"));
        Assert.assertTrue("Regex passes", Pattern.matches(OptionType.IP_ADDRESS.getDefaultRegex(), "localhost"));
        Assert.assertTrue("Regex passes", Pattern.matches(OptionType.IP_ADDRESS.getDefaultRegex(), "0.0.0.0"));
        Assert.assertTrue("Regex passes", Pattern.matches(OptionType.IP_ADDRESS.getDefaultRegex(), "255.255.255.255"));
        Assert.assertTrue("Regex passes", Pattern.matches(OptionType.IP_ADDRESS.getDefaultRegex(), "1.2.3.4"));
        Assert.assertTrue("Regex passes", Pattern.matches(OptionType.IP_ADDRESS.getDefaultRegex(), "11.22.33.44"));

        Assert.assertFalse("Regex fails", Pattern.matches(OptionType.IP_ADDRESS.getDefaultRegex(), "256.255.255.255"));
        Assert.assertFalse("Regex fails", Pattern.matches(OptionType.IP_ADDRESS.getDefaultRegex(), "255.256.255.255"));
        Assert.assertFalse("Regex fails", Pattern.matches(OptionType.IP_ADDRESS.getDefaultRegex(), "255.255.256.255"));
        Assert.assertFalse("Regex fails", Pattern.matches(OptionType.IP_ADDRESS.getDefaultRegex(), "255.255.255.256"));
        Assert.assertFalse("Regex fails", Pattern.matches(OptionType.IP_ADDRESS.getDefaultRegex(), "300.255.255.255"));
        Assert.assertFalse("Regex fails", Pattern.matches(OptionType.IP_ADDRESS.getDefaultRegex(), "**"));
        Assert.assertFalse("Regex fails", Pattern.matches(OptionType.IP_ADDRESS.getDefaultRegex(), "l"));
        Assert.assertFalse("Regex fails", Pattern.matches(OptionType.IP_ADDRESS.getDefaultRegex(), "llocalhost"));
        Assert.assertFalse("Regex fails", Pattern.matches(OptionType.IP_ADDRESS.getDefaultRegex(), ""));
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
