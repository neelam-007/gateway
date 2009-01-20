package com.l7tech.external.assertions.samlpassertion;

import junit.framework.TestCase;
import junit.framework.Test;
import junit.framework.TestSuite;

import java.util.logging.Logger;

import org.springframework.context.ApplicationContext;
import com.l7tech.server.ServerConfigStub;

/**
 * User: megery
 * Date: Nov 13, 2008
 */
public class SamlpAssertionsTest extends TestCase {

    private static final Logger log = Logger.getLogger(SamlpAssertionsTest.class.getName());

    public SamlpAssertionsTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(
                SamlpRequestBuilderAssertionTest.class,
                SamlpResponseEvaluationAssertionTest.class
        );
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
}
