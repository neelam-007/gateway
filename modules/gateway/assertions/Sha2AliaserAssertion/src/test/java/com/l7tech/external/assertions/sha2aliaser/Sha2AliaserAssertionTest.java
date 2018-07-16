package com.l7tech.external.assertions.sha2aliaser;

import com.l7tech.external.assertions.sha2aliaser.server.Sha2AliaserModuleLoadListener;
import com.l7tech.policy.AllAssertionsTest;
import com.l7tech.util.SyspropUtil;
import org.junit.Test;

import java.security.MessageDigest;

/**
 * Test the Sha2AliaserAssertion.
 */
public class Sha2AliaserAssertionTest {
    @Test
    public void testCloneIsDeepCopy() throws Exception {
        AllAssertionsTest.checkCloneIsDeepCopy( new Sha2AliaserAssertion() );
    }

    @Test
    public void testDefaultAliasesGetInstalledOk() throws Exception {
        SyspropUtil.clearProperty( Sha2AliaserModuleLoadListener.PROP_ADD_ALIASES );
        Sha2AliaserModuleLoadListener.onModuleLoaded( null );
        MessageDigest.getInstance( "SHA512", "SUN" );
        MessageDigest.getInstance( "SHA256", "SUN" );
        MessageDigest.getInstance( "SHA384", "SUN" );
    }
}
