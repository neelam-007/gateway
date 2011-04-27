package com.l7tech.server.security;

import com.l7tech.identity.IdentityProviderPasswordPolicy;
import com.l7tech.objectmodel.InvalidPasswordException;
import static org.junit.Assert.*;
import org.junit.Test;

import static com.l7tech.identity.IdentityProviderPasswordPolicy.*;

/**
 *
 */
public class PasswordEnforcerManagerTest {

    @Test
    public void testPasswordString() throws Exception {
        assertTrue( "Empty policy", validPassword( "password", null, null ) );

        // MIN_PASSWORD_LENGTH
        assertTrue( "Min length policy 0", validPassword( "", MIN_PASSWORD_LENGTH, 0 ) );
        assertTrue( "Min length policy 0", validPassword( "password", MIN_PASSWORD_LENGTH, 0 ) );
        assertTrue( "Min length policy 1", validPassword( "password", MIN_PASSWORD_LENGTH, 1 ) );
        assertTrue( "Min length policy 8", validPassword( "password", MIN_PASSWORD_LENGTH, 8 ) );
        assertFalse( "Min length policy 9", validPassword( "password", MIN_PASSWORD_LENGTH, 9 ) );
        assertFalse( "Min length policy  max", validPassword( "password", MIN_PASSWORD_LENGTH, Integer.MAX_VALUE ) );

        // MAX_PASSWORD_LENGTH
        assertTrue( "Max length policy 0", validPassword( "", MAX_PASSWORD_LENGTH, 0 ) );
        assertTrue( "Max length policy 9", validPassword( "password", MAX_PASSWORD_LENGTH, 9 ) );
        assertTrue( "Max length policy 8", validPassword( "password", MAX_PASSWORD_LENGTH, 8 ) );
        assertTrue( "Max length policy max", validPassword( "password", MAX_PASSWORD_LENGTH, Integer.MAX_VALUE ) );
        assertFalse( "Max length policy 7", validPassword( "password", MAX_PASSWORD_LENGTH, 7 ) );
        assertFalse( "Max length policy 1", validPassword( "password", MAX_PASSWORD_LENGTH, 1 ) );

        // NO_REPEAT_CHARS
        assertTrue( "Repeat chars min policy false", validPassword( "password", NO_REPEAT_CHARS, false ) );
        assertFalse( "Repeat chars min policy true", validPassword( "password", NO_REPEAT_CHARS, true ) );

        // UPPER_MIN
        assertTrue( "Upper min policy 1", validPassword( "PASSword", UPPER_MIN, 1 ) );
        assertTrue( "Upper min policy 4", validPassword( "PASSword", UPPER_MIN, 4 ) );
        assertFalse( "Upper min policy 1", validPassword( "password", UPPER_MIN, 1 ) );
        assertFalse( "Upper min policy 4", validPassword( "PASsword", UPPER_MIN, 4 ) );

        // LOWER_MIN
        assertTrue( "Lower min policy 1", validPassword( "PASSword", LOWER_MIN, 1 ) );
        assertTrue( "Lower min policy 4", validPassword( "PASSword", LOWER_MIN, 4 ) );
        assertFalse( "Lower min policy 1", validPassword( "PASSWORD", LOWER_MIN, 1 ) );
        assertFalse( "Lower min policy 4", validPassword( "PASSWord", LOWER_MIN, 4 ) );

        // NUMBER_MIN
        assertTrue( "Number min policy 1", validPassword( "passw0rd", NUMBER_MIN, 1 ) );
        assertTrue( "Number min policy 4", validPassword( "p355w0rd", NUMBER_MIN, 4 ) );
        assertFalse( "Number min policy 1", validPassword( "password", NUMBER_MIN, 1 ) );
        assertFalse( "Number min policy 4", validPassword( "p455word", NUMBER_MIN, 4 ) );

        // SYMBOL_MIN
        assertTrue( "Symbol min policy 1", validPassword( "password!", SYMBOL_MIN, 1 ) );
        assertTrue( "Symbol min policy 4", validPassword( "password!!!!", SYMBOL_MIN, 4 ) );
        assertFalse( "Symbol min policy 1", validPassword( "password", SYMBOL_MIN, 1 ) );
        assertFalse( "Symbol min policy 1 space", validPassword( " ", SYMBOL_MIN, 1 ) );
        assertFalse( "Symbol min policy 4", validPassword( "password!!!", SYMBOL_MIN, 4 ) );

        // NON_NUMERIC_MIN
        assertTrue( "Non numeric min policy 1", validPassword( "password", NON_NUMERIC_MIN, 1 ) );
        assertTrue( "Non numeric min policy 1 space", validPassword( " ", NON_NUMERIC_MIN, 1 ) );
        assertTrue( "Non numeric min policy 8", validPassword( "password", NON_NUMERIC_MIN, 8 ) );
        assertFalse( "Non numeric min policy 1", validPassword( "12345678", NON_NUMERIC_MIN, 1 ) );
        assertFalse( "Non numeric min policy 9", validPassword( "password", NON_NUMERIC_MIN, 9 ) );
    }

    @Test
    public void testSupplimentaryCharacters() {
        // This currently passes but should probably fail, there are only 4
        // characters (code points) but 8 code units.
        //
        assertTrue( "Min length policy 8 sup", validPassword( "\u2070E\u20731\u20779\u20C53", MIN_PASSWORD_LENGTH, 8 ) );
    }

    private boolean validPassword( final String password,
                                   final String property,
                                   final Object value ) {
        final IdentityProviderPasswordPolicy policy = new IdentityProviderPasswordPolicy();
        if ( property != null ) {
            policy.setProperty( property, value );
        }

        boolean valid;
        try {
            PasswordEnforcerManager.validatePasswordString( password, policy );
            valid = true;
        } catch ( InvalidPasswordException e ) {
            valid = false;
        }

        return valid;
    }
}
