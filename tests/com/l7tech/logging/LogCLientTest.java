package com.l7tech.logging;

import com.l7tech.logging.LogAdmin;
import com.l7tech.identity.StubDataStore;
import com.l7tech.common.util.Locator;
import junit.extensions.TestSetup;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.io.File;
import java.util.logging.Logger;

/**
 * Class LogCLientTest.
 * @author <a href="mailto:emarceta@layer7-tech.com>Emil Marceta</a>
 */
public class LogCLientTest extends TestCase {
    static Logger logger = Logger.getLogger(LogCLientTest.class.getName());

    /**
     * test <code>AbstractLocatorTest</code> constructor
     */
    public LogCLientTest(String name) {
        super(name);

    }

    /**
     * create the <code>TestSuite</code> for the
     * LogCLientTest <code>TestCase</code>
     */
    public static Test suite() {
        final TestSuite suite = new TestSuite(LogCLientTest.class);
        TestSetup wrapper = new TestSetup(suite) {
            /**
             * test setup that deletes the stub data store; will trigger
             * store recreate
             * sets the environment
             * @throws Exception on error deleting the stub data store
             */
            protected void setUp() throws Exception {
                File f = new File(StubDataStore.DEFAULT_STORE_PATH);
                if (f.exists()) {
                    f.delete();
                }
                System.setProperty("com.l7tech.common.locator.properties",
                        "/com/l7tech/console/resources/services.properties");
            }

            protected void tearDown() throws Exception {
                ;
            }
        };
        return wrapper;
    }

    public void setUp() throws Exception {
        // put set up code here
    }

    public void tearDown() throws Exception {
        // put tear down code here
    }

    public void testInvokeLogService() throws Exception {
        LogAdmin log = (LogAdmin)Locator.getDefault().lookup(LogAdmin.class);
        if (log == null) throw new IllegalStateException("cannot obtain log remote reference");

        String[] logs = log.getSystemLog(0, 50);
        for (int i = 0; i < logs.length; i++) {
            String s = logs[i];
            System.out.println(s);
        }
    }

    /**
     * Test <code>AbstractLocatorTest</code> main.
     */
    public static void main(String[] args) throws
      Throwable {
        junit.textui.TestRunner.run(suite());
    }
}
