package com.l7tech.identity;

import junit.framework.TestCase;
import com.l7tech.objectmodel.*;
import com.l7tech.identity.imp.*;

import java.util.Iterator;
import java.util.Random;

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
    static final String TYPE_NAME = "Internal Identity Provider";
    static final String TYPE_DESCRIPTION = "This is a bogus record.";
    static final String CONFIG_NAME = "Test Internal Identity Provider";
    static final String CONFIG_DESC = "This object does not exist!";

    private void deleteAll() throws Exception {
        begin();
        Iterator i = _manager.findAll().iterator();
        IdentityProviderConfig config;
        while ( i.hasNext() ) {
            config = (IdentityProviderConfig)i.next();
            _manager.delete(config);
        }
        commit();
    }

    public void setUp() throws Exception {
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

        commit();
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

    private long randomLong() {
        return _random.nextLong();
    }

    public void testSave() {
        IdentityProviderConfig config = new IdentityProviderConfigImp();
        config.setType( _type );
        config.setName( CONFIG_NAME );
        config.setDescription( CONFIG_DESC );
        // commit
        // find
        // assert equal to template
    }

    public void testSaveAlreadyExists() throws Exception {
        // create
        // commit
        // assert failure
    }

    public void testSaveRollback() throws Exception {
        // create
        // save
        // rollback
        // find
        // assert nonexistent
    }

    public void testUpdate() throws Exception {
        // create
        begin();
        // save
        commit();

        begin();
        // find
        // update
        // commit
        commit();

        begin();
        // find
        commit();
        // assert changed
    }

    public void testUpdateRollback() throws Exception {
        // create
        begin();
        // save
        commit();

        begin();
        // find
        // update
        rollback();

        begin();
        // find
        rollback();
        // assert not changed
    }

    public void testFindOne() throws Exception {
        // create
        begin();
        // save
        // find
        commit();
        // assert exists
    }

    public void testFindAllNoResults() throws Exception {
        begin();
        // findAll()
        commit();
        // assert empty result
    }

    public void testFindAllOneResult() throws Exception {
        // create one
        begin();
        // save
        // findAll()
        commit();
        // assert one result
    }

    public void testFindAll() throws Exception {
        // create multiple
        begin();
        // save
        // findAll()
        commit();
        // assert >1 result
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
    private Random _random;
}