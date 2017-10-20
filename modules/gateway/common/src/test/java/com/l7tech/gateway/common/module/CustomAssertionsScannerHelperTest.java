package com.l7tech.gateway.common.module;

import org.junit.Assert;
import org.junit.Test;

import java.util.jar.JarEntry;

import static org.junit.Assert.*;

public class CustomAssertionsScannerHelperTest {
    @Test
    public void isCustomAssertionPropertiesFile() throws Exception {
        final String customAssertionPropertiesFile = "Custom.properties";
        final CustomAssertionsScannerHelper customAssertionsScannerHelper = new CustomAssertionsScannerHelper(customAssertionPropertiesFile);

        Assert.assertTrue(customAssertionsScannerHelper.isCustomAssertionPropertiesFile(new JarEntry(customAssertionPropertiesFile)));
        Assert.assertTrue(customAssertionsScannerHelper.isCustomAssertionPropertiesFile(new JarEntry("/"+customAssertionPropertiesFile)));

        //TODO: should this really be a valid case?
        Assert.assertTrue(customAssertionsScannerHelper.isCustomAssertionPropertiesFile(new JarEntry(customAssertionPropertiesFile + ".bad")));

        Assert.assertFalse(customAssertionsScannerHelper.isCustomAssertionPropertiesFile(new JarEntry(customAssertionPropertiesFile + "/")));
        Assert.assertFalse(customAssertionsScannerHelper.isCustomAssertionPropertiesFile(new JarEntry("bad"+customAssertionPropertiesFile)));
    }

}