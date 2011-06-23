package com.l7tech.external.assertions.samlpassertion;

import junit.framework.Test;
import junit.framework.TestSuite;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 *
 * @author: vchan
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
    AuthzMessageGeneratorV2Test.class,
    AttributeQueryGeneratorV2Test.class,
    AuthorizationMessageGeneratorV1Test.class,
    AttributeQueryGeneratorV1Test.class,
    AuthnMessageGeneratorV2Test.class
    })
public class SamlpMessageGeneratorTestDriver {
}
