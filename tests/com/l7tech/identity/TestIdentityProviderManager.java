package com.l7tech.identity;

import junit.framework.TestCase;
import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.identity.imp.IdentityProviderConfigManagerImp;

import java.sql.SQLException;

/**
 * @author alex
 */
public class TestIdentityProviderManager extends TestCase {
    public static void main( String[] args ) {
    }

    public void setUp() {

        try {
            IdentityProviderConfigManager ipm = new IdentityProviderConfigManagerImp();
        } catch ( SQLException se ) {
            fail( se.toString() );
        }
    }
}
