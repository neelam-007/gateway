package com.l7tech.identity;

import junit.framework.TestCase;
import com.l7tech.objectmodel.*;
import com.l7tech.identity.imp.*;

import java.util.Iterator;

/**
 * @author alex
 */
public class TestIdentityProviderConfigManager extends TestCase {
    /**
     * test <code>TestIdentityProviderConfigManager</code> constructor
     */
    public TestIdentityProviderConfigManager() {
        super( "Test IdentityProviderConfigManager" );
    }

    static final String TYPE_CLASSNAME = "com.l7tech.identity.internal.InternalIdentityProvider";
    static final String TYPE_NAME = "Internal IdentityAssertion Provider";
    static final String TYPE_DESCRIPTION = "This is a bogus record.";
    static final String CONFIG_NAME = "Test Internal IdentityAssertion Provider";
    static final String CONFIG_NAME2 = "Second Value of Name Field";
    static final String CONFIG_DESC = "This object does not exist!";

    private void deleteAll() throws Exception {
        try {
            begin();
            Iterator i = _manager.findAll().iterator();
            IdentityProviderConfig config;
            while ( i.hasNext() ) {
                config = (IdentityProviderConfig)i.next();
                _manager.delete(config);
            }
        } finally {
            commit();
        }
    }

    public void setUp() throws Exception {
        try {
            _manager = new IdentityProviderConfigManagerImp();
            deleteAll();

            begin();
            IdentityProviderTypeManager iptm = new IdentityProviderTypeManagerImp();

            _type = new IdentityProviderTypeImp();
            _type.setClassName( TYPE_CLASSNAME );
            _type.setName( TYPE_NAME );
            _type.setDescription( TYPE_DESCRIPTION );

            long typeOid = iptm.save( _type );

            assertTrue( "IdentityProviderType not created! (typeOid was " + typeOid + ")", typeOid >= 0 );
        } finally {
            commit();
        }
    }

    public void tearDown() throws Exception {
        deleteAll();
        _manager = null;
        _type = null;
    }

    private void begin() throws Exception {
        PersistenceContext.getCurrent().beginTransaction();
    }

    private void commit() throws Exception {
        PersistenceContext.getCurrent().commitTransaction();
    }

    private void rollback() throws Exception {
        PersistenceContext.getCurrent().rollbackTransaction();
    }

    private IdentityProviderConfig makeConfig() {
        IdentityProviderConfig config = new IdentityProviderConfigImp();
        config.setType( _type );
        config.setName( CONFIG_NAME );
        config.setDescription( CONFIG_DESC );
        return config;
    }

    public void testSave() throws Exception {
        IdentityProviderConfig config = makeConfig();
        long oid;
        try {
            begin();
            oid = _manager.save( config );
        } finally {
            commit();
        }

        try {
            begin();
            config = _manager.findByPrimaryKey( oid );
            assertEquals( CONFIG_NAME, config.getName() );
            assertEquals( CONFIG_DESC, config.getDescription() );
            assertEquals( _type.getOid(), config.getType().getOid() );
        } finally {
            commit();
        }
    }

    public void testSaveAlreadyExists() throws Exception {
        IdentityProviderConfig config = makeConfig();
        long oid;
        try {
            begin();
            oid = _manager.save(config);
        } finally {
            commit();
        }

        config = makeConfig();
        try {
            try {
                begin();
                config.setOid( oid );
            } finally {
                commit();
                fail( "Incorrectly saved record with duplicate oid" );
            }
        } catch ( TransactionException se ) {
            assertTrue( "Correctly failed to save record with duplicate oid", true );
        }
    }

    public void testSaveRollback() throws Exception {
        IdentityProviderConfig config = makeConfig();
        begin();
        long oid = _manager.save(config);
        rollback();

        begin();
        try {
            config = _manager.findByPrimaryKey( oid );
            if ( config != null )
                fail( "Incorrectly found an object whose creation was rolled back!" );
        } catch ( FindException fe ) {
            assertTrue( "Correctly failed to find rolled-back object", true );
        } finally {
            commit();
        }
    }

    public void testUpdate() throws Exception {
        IdentityProviderConfig config = makeConfig();
        long oid;
        try {
            begin();
            oid = _manager.save( config );
        } finally {
            commit();
        }

        try {
            begin();
            config = _manager.findByPrimaryKey( oid );
            config.setName( CONFIG_NAME2 );
            _manager.update(config);
        } finally {
            commit();
        }

        try {
            begin();
            config = _manager.findByPrimaryKey( oid );
            assertEquals( CONFIG_NAME2, config.getName() );
        } finally {
            commit();
        }
    }

    public void testUpdateRollback() throws Exception {
        IdentityProviderConfig config = makeConfig();
        long oid;
        try {
            begin();
            oid = _manager.save( config );
        } finally {
            commit();
        }

        try {
            begin();
            config = _manager.findByPrimaryKey( oid );
            config.setName( CONFIG_NAME2 );
            _manager.update(config);
        } finally {
            rollback();
        }

        try {
            begin();
            config = _manager.findByPrimaryKey( oid );
            assertEquals( CONFIG_NAME, config.getName() );
        } finally {
            commit();
        }
    }

    public void testFindAllOneResult() throws Exception {
        IdentityProviderConfig config = makeConfig();
        long oid;
        try {
            begin();
            oid = _manager.save( config );
        } finally {
            commit();
        }

        try {
            begin();
            Iterator i = _manager.findAll().iterator();
            while ( i.hasNext() ) {
                config = (IdentityProviderConfig)i.next();
                assertFalse( "Too many results returned from findAll()!", i.hasNext() );
            }

            assertEquals( oid, config.getOid() );
            assertEquals( CONFIG_NAME, config.getName() );
            assertEquals( CONFIG_DESC, config.getDescription() );
        } catch ( FindException fe ) {
            fail( "Failed to find newly created object!" );
        } finally {
            commit();
        }
    }

    public void testFindAllNoResults() throws Exception {
        try {
            begin();
            Iterator i = _manager.findAll().iterator();
            assertFalse( "Incorrectly found an object!", i.hasNext() );
        } finally {
            commit();
        }
    }

    public void testFindByPrimaryKey() throws Exception {
        long oid;
        IdentityProviderConfig config;
        try {
            begin();
            config = makeConfig();
            oid = _manager.save(config);
        } finally {
            commit();
        }

        try {
            begin();
            config = _manager.findByPrimaryKey( oid );
            assertNotNull("findByPrimaryKey() returned null", config);
        } catch ( FindException fe ) {
            fail( "findByPrimaryKey() threw FindException");
        } finally {
            commit();
        }
    }

    public void testFindAll() throws Exception {
        IdentityProviderConfig config = makeConfig();
        long oid;
        try {
            begin();
            oid = _manager.save( config );
        } finally {
            commit();
        }

        config = makeConfig();
        try {
            begin();
            oid = _manager.save( config );
        } finally {
            commit();
        }

        try {
            begin();
            Iterator i = _manager.findAll().iterator();
            int j = 0;
            while ( i.hasNext() ) {
                config = (IdentityProviderConfig)i.next();
                j++;
            }
            assertEquals( "Expected two results, got " + j, j, 2 );
        } catch ( FindException fe ) {
            fail( "Failed to find newly created objects!" );
        } finally {
            commit();
        }
    }

    public void testCreateAndDelete() throws Exception {
        // create
        begin();
        // save
        // delete
        commit();
        // assert nonexistent
    }

    public void testDeleteNonexistent() throws Exception {
        begin();
        // delete
        commit();
        // assert failure
    }

    public void testDeleteRollback() throws Exception {
        begin();

        rollback();
    }

    private IdentityProviderType _type;
    private IdentityProviderConfigManager _manager;
}