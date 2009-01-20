package com.l7tech.external.assertions.samlpassertion;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 *
 * @author: vchan
 */
public class SamlpMessageGeneratorTestDriver {

    public static Test suite() {
        TestSuite suite = new TestSuite("SamlpMessageGeneratorTest Driver");

        suite.addTestSuite(AuthzMessageGeneratorV2Test.class);
        suite.addTestSuite(AttributeQueryGeneratorV2Test.class);

        suite.addTestSuite(AuthorizationMessageGeneratorV1Test.class);
        suite.addTestSuite(AttributeQueryGeneratorV1Test.class);
        
        suite.addTestSuite(AuthnMessageGeneratorV2Test.class);

        return suite;
    }
}
