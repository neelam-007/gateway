package com.l7tech.external.assertions.samlpassertion;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;


/**
 * User: megery
 * Date: Nov 13, 2008
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
    SamlpRequestBuilderAssertionTest.class,
    SamlpResponseEvaluationAssertionTest.class
    })
public class SamlpAssertionsTest {
}
