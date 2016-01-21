package com.l7tech.external.assertions.siteminder.util;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

/**
 * Created with IntelliJ IDEA.
 * User: yuri
 * Date: 4/9/14
 * Time: 9:02 PM
 * To change this template use File | Settings | File Templates.
 */
public class SiteMinderAssertionUtilTest {
    @Test
    public void shouldRetrieveCnFromUserDn() throws Exception {
        assertEquals("user", SiteMinderAssertionUtil.getCn("CN=user,OU=QA Test Users,DC=domain,DC=local"));
    }

    @Test
    public void shouldValidateHostName() throws Exception {
        String[] validNames = {"gateway", "gateway.ca.com", "gateway123", "gateway-name-123", "gateway-name.ca.com", "123gateway", "192.168.0.10"};
        for(String validName : validNames) {
            assertTrue("Hostname " + validName + " is invalid", SiteMinderAssertionUtil.validateHostname(validName));
        }
        String[] invalidNames = {"$gateway", "W-;jkljk","${gateway-name}","{}()&&^%","gateway0/name","domain."};
        for(String invalidName : invalidNames) {
            assertFalse("Failed to recognize invalid hostname " + invalidName, SiteMinderAssertionUtil.validateHostname(invalidName));
        }

    }
}
