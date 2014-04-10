package com.l7tech.external.assertions.siteminder.util;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;

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
}
