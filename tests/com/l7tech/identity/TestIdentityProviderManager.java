package com.l7tech.identity;

import junit.framework.TestCase;
import com.l7tech.identity.IdentityProviderConfigManager;
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
        _ipm = new com.l7tech.identity.IdProvConfManagerServer();
    }

    private IdentityProviderConfigManager _ipm;
}
