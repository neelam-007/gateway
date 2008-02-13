package com.l7tech.server;

import com.l7tech.common.message.KnobblyMessageTest;
import com.l7tech.common.xml.TarariLoader;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * Runs a bunch of exisiting test suites with Tarari enabled.  Make sure run this with fork="yes".
 */
@RunWith(value= Suite.class)
@Suite.SuiteClasses(value={
        PolicyProcessingTest.class,
        KnobblyMessageTest.class
        })
public class AllTarariTests {
    @BeforeClass
    public static void setUp() throws Exception {
        System.setProperty(TarariLoader.ENABLE_PROPERTY, "true");
    }
}
