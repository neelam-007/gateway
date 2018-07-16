package com.l7tech.server;

import com.l7tech.test.BugId;
import com.l7tech.util.MockConfig;
import com.l7tech.util.SyspropUtil;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.Properties;

public class SystemPropertiesTest {

    private static final String PREFIX = "com.l7tech.server";
    private static final String TEST_PROP_1 = "testProperty";
    private static final String TEST_PROP_1_FULL_PATH = PREFIX + "." + TEST_PROP_1;
    private static final String TEST_PROP_2 = "differentProperty";
    private static final String TEST_PROP_2_FULL_PATH = PREFIX + "." + TEST_PROP_2;
    private static final String PROCESS_CONTROLLER_PRESENT = "processControllerPresent";
    private static final String PROCESS_CONTROLLER_FULL_PATH = PREFIX + "." + PROCESS_CONTROLLER_PRESENT;

    private SystemProperties systemProperties;

    @Before
    public void setUp() {
        systemProperties = new SystemProperties(new MockConfig(Collections.emptyMap()));
    }
    
    @After
    public void cleanUp() {
        SyspropUtil.clearProperties(TEST_PROP_1_FULL_PATH, TEST_PROP_2_FULL_PATH, PROCESS_CONTROLLER_FULL_PATH);
    }

    @Test
    @BugId("DE372409")
    public void setSystemProperties_dockerEnvironment_commandLineTakesPrecedence() {

        // System properties set via command line are present at launch
        System.setProperty(TEST_PROP_1_FULL_PATH, "commandLineValue");

        // Simulate loading a system.properties file
        Properties sysProps = new Properties();
        sysProps.setProperty(TEST_PROP_1, "systemPropertiesValue");
        sysProps.setProperty(TEST_PROP_2, "shouldExist"); // Check that any properties that don't already exist get set
        systemProperties.setSystemProperties(sysProps, PREFIX, false);

        Assert.assertEquals("commandLineValue", SyspropUtil.getProperty(TEST_PROP_1_FULL_PATH));
        Assert.assertEquals("shouldExist", SyspropUtil.getProperty(TEST_PROP_2_FULL_PATH));
    }

    @Test
    public void setSystemProperties_nonDockerEnvironment_overwritesExisting() {
        // System properties set via command line are present at launch
        System.setProperty(TEST_PROP_1_FULL_PATH, "commandLineValue");

        // Simulate loading a system.properties file
        Properties sysProps = new Properties();
        sysProps.setProperty(TEST_PROP_1, "systemPropertiesValue");
        sysProps.setProperty(PROCESS_CONTROLLER_PRESENT, "true");
        sysProps.setProperty(TEST_PROP_2, "shouldExist");
        systemProperties.setSystemProperties(sysProps, PREFIX, false);

        Assert.assertTrue(SyspropUtil.getBoolean(PROCESS_CONTROLLER_FULL_PATH));
        Assert.assertEquals("systemPropertiesValue", SyspropUtil.getProperty(TEST_PROP_1_FULL_PATH));
        Assert.assertEquals("shouldExist", SyspropUtil.getProperty(TEST_PROP_2_FULL_PATH));

        // Simulate loading another .properties file
        Properties sysProps2 = new Properties();
        sysProps.setProperty(TEST_PROP_2, "badValue");
        systemProperties.setSystemProperties(sysProps2, PREFIX, false);
        Assert.assertEquals("shouldExist", SyspropUtil.getProperty(TEST_PROP_2_FULL_PATH));
    }
}
