package com.l7tech.identity;

import junit.framework.TestCase;
import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.identity.imp.IdentityProviderConfigManagerImp;
import com.l7tech.objectmodel.PersistenceManager;
import com.l7tech.objectmodel.PersistenceContext;

import java.sql.SQLException;

/**
 * @author alex
 */
public class TestIdentityProviderManager extends TestCase {
    public static void main( String[] args ) {
    }

    public void setUp() throws Exception {
        PersistenceContext context = PersistenceManager.getContext();
        _ipm = new IdentityProviderConfigManagerImp( context );
    }

    private IdentityProviderConfigManager _ipm;
}
