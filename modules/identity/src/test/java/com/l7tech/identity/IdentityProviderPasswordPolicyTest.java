package com.l7tech.identity;

import static org.junit.Assert.*;
import org.junit.Test;

/**
 * Created by IntelliJ IDEA.
 * User: wlui
 */
public class IdentityProviderPasswordPolicyTest {

    @Test
    public void testSave(){
        final IdentityProviderPasswordPolicy policy = new IdentityProviderPasswordPolicy();
        policy.setProperty(IdentityProviderPasswordPolicy.MAX_PASSWORD_LENGTH,5);
        assertEquals("password length", Integer.valueOf(5), policy.getIntegerProperty( IdentityProviderPasswordPolicy.MAX_PASSWORD_LENGTH ));
    }

    @Test
    public void testStrengthCheck() {
        final IdentityProviderPasswordPolicy policy1 = new IdentityProviderPasswordPolicy(false, false, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, false);
        assertTrue("strength of self 1", policy1.hasStrengthOf( policy1 ));

        final IdentityProviderPasswordPolicy policy2 = new IdentityProviderPasswordPolicy(true, true, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, true);
        assertTrue("strength of self 2", policy2.hasStrengthOf( policy2 ));
        assertTrue("stronger than empty", policy2.hasStrengthOf( policy1 ));
        assertFalse("empty is weaker", policy1.hasStrengthOf( policy2 ));

        final IdentityProviderPasswordPolicy policy3 = new IdentityProviderPasswordPolicy(true, true, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, true);
        final IdentityProviderPasswordPolicy policy4 = new IdentityProviderPasswordPolicy(true, true, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, true);
        assertTrue("strength of identical 1", policy3.hasStrengthOf( policy4 ));
        assertTrue("strength of identical 2", policy4.hasStrengthOf( policy3 ));

        policy3.setProperty( IdentityProviderPasswordPolicy.FORCE_PWD_CHANGE, false );
        assertFalse("weaker force change 1", policy3.hasStrengthOf( policy4 ));
        assertTrue("weaker force change 2", policy4.hasStrengthOf( policy3 ));
        policy3.setProperty( IdentityProviderPasswordPolicy.FORCE_PWD_CHANGE, true );

        policy3.setProperty( IdentityProviderPasswordPolicy.PASSWORD_EXPIRY, 1 );
        assertTrue("stronger password expiry 1", policy3.hasStrengthOf( policy4 ));
        assertFalse("stronger password expiry 2", policy4.hasStrengthOf( policy3 ));
        policy3.setProperty( IdentityProviderPasswordPolicy.PASSWORD_EXPIRY, 2 );

        policy3.setProperty( IdentityProviderPasswordPolicy.MIN_PASSWORD_LENGTH, 3 );
        assertTrue("stronger min password 1", policy3.hasStrengthOf( policy4 ));
        assertFalse("stronger min password 2", policy4.hasStrengthOf( policy3 ));
        policy3.setProperty( IdentityProviderPasswordPolicy.MIN_PASSWORD_LENGTH, 2 );

        policy3.setProperty( IdentityProviderPasswordPolicy.MAX_PASSWORD_LENGTH, 3 );
        assertTrue("stronger max password 1", policy3.hasStrengthOf( policy4 ));
        assertFalse("stronger max password 2", policy4.hasStrengthOf( policy3 ));
        policy3.setProperty( IdentityProviderPasswordPolicy.MAX_PASSWORD_LENGTH, 2 );
    }
}
