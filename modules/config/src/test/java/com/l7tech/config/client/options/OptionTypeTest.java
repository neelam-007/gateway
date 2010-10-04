package com.l7tech.config.client.options;

import org.junit.Test;
import org.junit.Assert;

public class OptionTypeTest {

    @Test
    public void testRegexCompilation() {
        OptionType.values();
    }

    @Test
    public void testPortRegex() {
        Assert.assertTrue("Regex passes", OptionType.PORT.matches("1"));
        Assert.assertTrue("Regex passes", OptionType.PORT.matches("65535"));
        Assert.assertTrue("Regex passes", OptionType.PORT.matches("12345"));
        Assert.assertTrue("Regex passes", OptionType.PORT.matches("11"));
        Assert.assertTrue("Regex passes", OptionType.PORT.matches("123"));
        Assert.assertTrue("Regex passes", OptionType.PORT.matches("1239"));

        Assert.assertFalse("Regex fails", OptionType.PORT.matches("0"));
        Assert.assertFalse("Regex fails", OptionType.PORT.matches("-1"));
        Assert.assertFalse("Regex fails", OptionType.PORT.matches("65536"));
        Assert.assertFalse("Regex fails", OptionType.PORT.matches("77777"));
        Assert.assertFalse("Regex fails", OptionType.PORT.matches("B"));
        Assert.assertFalse("Regex fails", OptionType.PORT.matches(""));
    }

    @Test
    public void testIntegerRegex() {
        Assert.assertTrue("Regex passes", OptionType.INTEGER.matches("0"));
        Assert.assertTrue("Regex passes", OptionType.INTEGER.matches("1"));
        Assert.assertTrue("Regex passes", OptionType.INTEGER.matches(Integer.toString(Integer.MAX_VALUE)));
        Assert.assertTrue("Regex passes", OptionType.INTEGER.matches("123456789"));
        Assert.assertTrue("Regex passes", OptionType.INTEGER.matches("987654321"));
        Assert.assertTrue("Regex passes", OptionType.INTEGER.matches("12"));
        Assert.assertTrue("Regex passes", OptionType.INTEGER.matches("1230"));

        Assert.assertFalse("Regex fails", OptionType.INTEGER.matches("-1"));
        Assert.assertFalse("Regex fails", OptionType.INTEGER.matches("01"));
        Assert.assertFalse("Regex fails", OptionType.INTEGER.matches(Integer.toString(Integer.MAX_VALUE)+"0") );
        Assert.assertFalse("Regex fails", OptionType.INTEGER.matches("3333333333"));
        Assert.assertFalse("Regex fails", OptionType.INTEGER.matches("B"));
        Assert.assertFalse("Regex fails", OptionType.INTEGER.matches(""));
    }

    @Test
    public void testIpRegex() {
        Assert.assertTrue("Regex passes", OptionType.IP_ADDRESS.matches("*"));
        Assert.assertTrue("Regex passes", OptionType.IP_ADDRESS.matches("localhost"));
        Assert.assertTrue("Regex passes", OptionType.IP_ADDRESS.matches("0.0.0.0"));
        Assert.assertTrue("Regex passes", OptionType.IP_ADDRESS.matches("255.255.255.255"));
        Assert.assertTrue("Regex passes", OptionType.IP_ADDRESS.matches("1.2.3.4"));
        Assert.assertTrue("Regex passes", OptionType.IP_ADDRESS.matches("11.22.33.44"));

        Assert.assertFalse("Regex fails", OptionType.IP_ADDRESS.matches("256.255.255.255"));
        Assert.assertFalse("Regex fails", OptionType.IP_ADDRESS.matches("255.256.255.255"));
        Assert.assertFalse("Regex fails", OptionType.IP_ADDRESS.matches("255.255.256.255"));
        Assert.assertFalse("Regex fails", OptionType.IP_ADDRESS.matches("255.255.255.256"));
        Assert.assertFalse("Regex fails", OptionType.IP_ADDRESS.matches("300.255.255.255"));
        Assert.assertFalse("Regex fails", OptionType.IP_ADDRESS.matches("**"));
        Assert.assertFalse("Regex fails", OptionType.IP_ADDRESS.matches("l"));
        Assert.assertFalse("Regex fails", OptionType.IP_ADDRESS.matches("llocalhost"));
        Assert.assertFalse("Regex fails", OptionType.IP_ADDRESS.matches(""));
    }

    @Test
    public void testTimeStampRegex() {
        Assert.assertTrue("Regex passes", OptionType.TIMESTAMP.matches("2001-01-31 00:00:00"));
        Assert.assertTrue("Regex passes", OptionType.TIMESTAMP.matches("2001-12-01 01:59:59"));
        Assert.assertTrue("Regex passes", OptionType.TIMESTAMP.matches("2001-10-10 00:00:01"));
        Assert.assertTrue("Regex passes", OptionType.TIMESTAMP.matches("0000-02-20 00:10:10"));

        Assert.assertFalse("Regex fails", OptionType.TIMESTAMP.matches(""));
        Assert.assertFalse("Regex fails", OptionType.TIMESTAMP.matches("01-12-00 01:59:59"));
        Assert.assertFalse("Regex fails", OptionType.TIMESTAMP.matches("2001-02-20 30:10:10"));
        Assert.assertFalse("Regex fails", OptionType.TIMESTAMP.matches("2001-02-20 00:60:10"));
        Assert.assertFalse("Regex fails", OptionType.TIMESTAMP.matches("2001-02-20 00:10:60"));
    }

    @Test
    public void testBooleanRegex() {
        Assert.assertTrue("Regex passes", OptionType.BOOLEAN.matches("true"));
        Assert.assertTrue("Regex passes", OptionType.BOOLEAN.matches("yes"));
        Assert.assertTrue("Regex passes", OptionType.BOOLEAN.matches("t"));
        Assert.assertTrue("Regex passes", OptionType.BOOLEAN.matches("y"));
        Assert.assertTrue("Regex passes", OptionType.BOOLEAN.matches("True"));
        Assert.assertTrue("Regex passes", OptionType.BOOLEAN.matches("Yes"));
        Assert.assertTrue("Regex passes", OptionType.BOOLEAN.matches("false"));
        Assert.assertTrue("Regex passes", OptionType.BOOLEAN.matches("no"));
        Assert.assertTrue("Regex passes", OptionType.BOOLEAN.matches("f"));
        Assert.assertTrue("Regex passes", OptionType.BOOLEAN.matches("n"));
        Assert.assertTrue("Regex passes", OptionType.BOOLEAN.matches("False"));
        Assert.assertTrue("Regex passes", OptionType.BOOLEAN.matches("No"));

        Assert.assertFalse("Regex fails", OptionType.BOOLEAN.matches(""));
        Assert.assertFalse("Regex fails", OptionType.BOOLEAN.matches("fa"));
        Assert.assertFalse("Regex fails", OptionType.BOOLEAN.matches("y e s"));
        Assert.assertFalse("Regex fails", OptionType.BOOLEAN.matches("asdf"));
        Assert.assertFalse("Regex fails", OptionType.BOOLEAN.matches("yy"));
    }
}
