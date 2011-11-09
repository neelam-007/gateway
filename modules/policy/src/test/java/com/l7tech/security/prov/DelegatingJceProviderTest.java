package com.l7tech.security.prov;

import com.l7tech.test.util.TestUtils;
import org.junit.Test;

/**
 *
 */
public class DelegatingJceProviderTest {

    @Test
    public void testDelegation() {
        TestUtils.assertOverridesAllMethods( JceProvider.class, DelegatingJceProvider.class );
    }
}
