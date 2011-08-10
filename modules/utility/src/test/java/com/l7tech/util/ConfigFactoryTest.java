package com.l7tech.util;

import com.l7tech.util.ConfigFactory.DefaultConfig;
import static org.junit.Assert.*;
import org.junit.Test;

import java.util.Properties;

/**
 * JUnit tests for ConfigFactory
 */
public class ConfigFactoryTest {

    /**
     * Test access of system properties without mappings.
     */
    @Test
    public void testSystemPropertyAccess() {
        SyspropUtil.setProperty( "com.l7tech.util.ConfigFactoryTest.prop1", "value1" );
        SyspropUtil.setProperty( "com.l7tech.util.ConfigFactoryTest.int1", "1" );
        SyspropUtil.setProperty( "com.example.prop1", "value1" );

        assertEquals( "System prop1", "value1", ConfigFactory.getProperty( "com.l7tech.util.ConfigFactoryTest.prop1" ) );
        assertEquals( "System prop int1", 1L, (long)ConfigFactory.getIntProperty( "com.l7tech.util.ConfigFactoryTest.int1", 0 ) );
        assertNull( "Non l7 system property", ConfigFactory.getProperty( "com.example.prop1" ) );
    }

    /**
     * Test that default values are used
     */
    @Test
    public void testDefaults() {
        assertEquals( "String property default", "default", ConfigFactory.getProperty( "com.doesn'texist.prop", "default" ) );
        assertEquals( "Boolean property default", true, ConfigFactory.getBooleanProperty( "com.doesn'texist.prop", true ) );
        assertEquals( "Integer property default", (long) 1, ConfigFactory.getIntProperty( "com.doesn'texist.prop", 1 ) );
        assertEquals( "Long property default", 1, ConfigFactory.getLongProperty( "com.doesn'texist.prop", 1L ) );
        assertEquals( "Time unit property default", 1L, ConfigFactory.getTimeUnitProperty( "com.doesn'texist.prop", 1L ) );
    }

    @Test
    public void testDefaultConfigConversions() {
        final Properties properties = new Properties( );
        properties.setProperty( "prop", "value" );
        properties.setProperty( "boolean", "true" );
        properties.setProperty( "integer", "1" );
        properties.setProperty( "long", "1" );
        properties.setProperty( "timeunit", "1ms" );

        final Config config = new DefaultConfig( properties, 0L );

        assertEquals( "String property default", "value", config.getProperty( "prop", "default" ) );
        assertEquals( "Boolean property default", true, config.getBooleanProperty( "boolean", false ) );
        assertEquals( "Integer property default", (long) 1, config.getIntProperty( "integer", 100 ) );
        assertEquals( "Long property default", 1, config.getLongProperty( "long", 100L ) );
        assertEquals( "Time unit property default", 1L, config.getTimeUnitProperty( "timeunit", 100L ) );
    }

    @Test
    public void testDefaultConfigDefaults() {
        final Properties properties = new Properties( );
        properties.setProperty( "prop.default", "value" );
        properties.setProperty( "boolean.default", "true" );
        properties.setProperty( "integer.default", "1" );
        properties.setProperty( "long.default", "1" );
        properties.setProperty( "timeunit.default", "1ms" );

        final Config config = new DefaultConfig( properties, 0L );

        assertEquals( "String property default", "value", config.getProperty( "prop", "default" ) );
        assertEquals( "Boolean property default", true, config.getBooleanProperty( "boolean", false ) );
        assertEquals( "Integer property default", (long) 1, config.getIntProperty( "integer", 100 ) );
        assertEquals( "Long property default", 1, config.getLongProperty( "long", 100L ) );
        assertEquals( "Time unit property default", 1L, config.getTimeUnitProperty( "timeunit", 100L ) );
    }

    @Test
    public void testDefaultConfigExpansion() {
        final Properties properties = new Properties( );
        properties.setProperty( "value", "value" );
        properties.setProperty( "true", "true" );
        properties.setProperty( "1", "1" );
        properties.setProperty( "prop.default", "${value}" );
        properties.setProperty( "boolean", "true" );
        properties.setProperty( "integer.default", "${1}" );
        properties.setProperty( "long.default", "${1}" );
        properties.setProperty( "timeunit.default", "${1}ms" );

        final Config config = new DefaultConfig( properties, 0L );

        assertEquals( "String property default", "value", config.getProperty( "prop", "default" ) );
        assertEquals( "Boolean property default", true, config.getBooleanProperty( "boolean", false ) );
        assertEquals( "Integer property default", (long) 1, config.getIntProperty( "integer", 100 ) );
        assertEquals( "Long property default", 1, config.getLongProperty( "long", 100L ) );
        assertEquals( "Time unit property default", 1L, config.getTimeUnitProperty( "timeunit", 100L ) );
    }

    /**
     * Verify that system properties override properties override property defaults override provided defaults
     */
    @Test
    public void testDefaultConfigPrecedence() {
        SyspropUtil.setProperty( "com.l7tech.util.ConfigFactoryTest.prop", "value1" );

        final Properties properties = new Properties( );
        properties.setProperty( "prop.default", "value3" );
        properties.setProperty( "prop", "value2" );
        properties.setProperty( "prop.systemProperty", "com.l7tech.util.ConfigFactoryTest.prop" );
        properties.setProperty( "prop1.default", "value2" );
        properties.setProperty( "prop1", "value1" );
        properties.setProperty( "prop2.default", "value1" );

        final Config config = new DefaultConfig( properties, 0L );

        assertEquals( "Prop", "value1", config.getProperty( "prop", "default" ) );
        assertEquals( "Prop1", "value1", config.getProperty( "prop1", "default" ) );
        assertEquals( "Prop2", "value1", config.getProperty( "prop2", "default" ) );
    }

    /**
     * Verify that defaults are used if the configured value is invalid
     */
    @Test
    public void testInvalidIgnored() {
        final Properties properties = new Properties( );
        properties.setProperty( "integer", "a" );
        properties.setProperty( "long", "b" );
        properties.setProperty( "timeunit", "c" );

        final Config config = new DefaultConfig( properties, 0L );

        assertEquals( "Integer property default", (long) 1, config.getIntProperty( "integer", 1 ) );
        assertEquals( "Long property default", 1, config.getLongProperty( "long", 1L ) );
        assertEquals( "Time unit property default", 1L, config.getTimeUnitProperty( "timeunit", 1L ) );
    }
}
