package com.l7tech.external.assertions.ldapwrite;


import com.l7tech.policy.AllAssertionsTest;
import org.junit.Test;


/**
 * Test the LdapWriteAssertion.
 */
public class LdapWriteAssertionTest {

    @Test
    public void testCloneIsDeepCopy() throws Exception {
        AllAssertionsTest.checkCloneIsDeepCopy(new LdapWriteAssertion());
    }

}
