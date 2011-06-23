package com.l7tech.external.assertions.ncesdeco.server;

import com.l7tech.external.assertions.ncesdeco.NcesDecoratorAssertion;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 * Test the NcesDecoratorAssertion.
 */
public class ServerNcesDecoratorAssertionTest {

    @Test
    public void testFeatureSetName() throws Exception {
        assertEquals("Feature set name", "assertion:NcesDecorator", new NcesDecoratorAssertion().getFeatureSetName());
    }

}
