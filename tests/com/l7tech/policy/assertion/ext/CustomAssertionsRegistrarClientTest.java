package com.l7tech.policy.assertion.ext;

import com.l7tech.console.util.Preferences;
import com.l7tech.console.util.Registry;
import com.l7tech.policy.assertion.CustomAssertionHolder;
import junit.extensions.TestSetup;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import javax.security.auth.Subject;
import java.rmi.RMISecurityManager;
import java.rmi.server.RMIClassLoader;
import java.security.Permission;
import java.security.Principal;
import java.security.PrivilegedExceptionAction;
import java.util.Collection;
import java.util.Iterator;
import java.util.logging.Logger;

/**
 * Class CustomAssertionsRegistrarClientTest.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com>Emil Marceta</a>
 */
public class CustomAssertionsRegistrarClientTest extends TestCase {
    static Logger logger = Logger.getLogger(CustomAssertionsRegistrarClientTest.class.getName());

    /**
     * test <code>AbstractLocatorTest</code> constructor
     */
    public CustomAssertionsRegistrarClientTest(String name) {
        super(name);
    }

    /**
     * create the <code>TestSuite</code> for the
     * LogCLientTest <code>TestCase</code>
     */
    public static Test suite() {
        final TestSuite suite = new TestSuite(CustomAssertionsRegistrarClientTest.class);
        TestSetup wrapper = new TestSetup(suite) {
            /**
             * @throws Exception on error deleting the stub data store
             */
            protected void setUp() throws Exception {
                System.setSecurityManager(new RMISecurityManager() {
                    public void checkPermission(Permission perm) {}

                    public void checkPermission(Permission perm, Object context) {}
                });
                System.setProperty("com.l7tech.common.locator.properties", "/com/l7tech/console/resources/services.properties");
                Preferences.getPreferences().updateSystemProperties();
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

    public void testVerifyClassAnnotation() throws Exception {
        Collection collection =
          (Collection)Subject.doAs(getSubject(), new PrivilegedExceptionAction() {
              public Object run() throws Exception {
                  CustomAssertionsRegistrar cr = Registry.getDefault().getCustomAssertionsRegistrar();
                  System.out.println(cr.getClass());
                  System.out.println(cr.getClass().getClassLoader());
                  System.out.println(RMIClassLoader.getClassAnnotation(cr.getClass()));
                  return cr.getAssertions(Category.IDENTITY);
              }
          });
        for (Iterator iterator = collection.iterator(); iterator.hasNext();) {
            CustomAssertionHolder ch = (CustomAssertionHolder)iterator.next();
            System.out.println(ch);
            System.out.println(RMIClassLoader.getClassAnnotation(ch.getCustomAssertion().getClass()));
        }
    }

    private Subject getSubject() {
        Subject subject = new Subject();
        subject.getPrincipals().add(new Principal() {
            public String getName() {
                return "admin";
            }
        });
        subject.getPrivateCredentials().add("password".toCharArray());
        return subject;
    }

    /**
     * Test <code>AbstractLocatorTest</code> main.
     */
    public static void main(String[] args) throws
      Throwable {
        junit.textui.TestRunner.run(suite());
    }
}
