package com.l7tech.spring.remoting.rmi.codebase;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Class ClassSetAnnotationTest.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class ClassSetAnnotationTest extends TestCase {
    /**
     * test <code>ClassSetAnnotationTest</code> constructor
     */
    public ClassSetAnnotationTest(String name) {
        super(name);
    }

    /**
     * create the <code>TestSuite</code> for the
     * ClassSetAnnotationTest <code>TestCase</code>
     */
    public static Test suite() {
        TestSuite suite = new TestSuite(ClassSetAnnotationTest.class);
        return suite;
    }

    public void setUp() throws Exception {
        // put set up code here
    }

    public void tearDown() throws Exception {
        // put tear down code here
    }

    public void testIncludes() throws Exception {
        final String codebaseUrl = "http://wheel";
        System.setProperty("java.rmi.server.codebase", codebaseUrl);
        ClassSetAnnotation.setIncludePatterns("**");
        ClassSetAnnotation rmiClassLoaderSpi = new ClassSetAnnotation();
        assertEquals(rmiClassLoaderSpi.getClassAnnotation(this.getClass()), codebaseUrl);
        assertNotNull(rmiClassLoaderSpi.getClassAnnotation(String.class));

        ClassSetAnnotation.setIncludePatterns("com.l7tech.**");
        assertEquals(rmiClassLoaderSpi.getClassAnnotation(this.getClass()), codebaseUrl);
        assertNull(rmiClassLoaderSpi.getClassAnnotation(String.class));
    }

    /**
     * Test <code>ClassSetAnnotationTest</code> main.
     */
    public static void main(String[] args) throws
      Throwable {
        junit.textui.TestRunner.run(suite());
    }
}
