package com.l7tech.external.assertions.mongodb.entity;


import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.gateway.common.audit.LoggingAudit;
import com.l7tech.objectmodel.*;
import com.l7tech.policy.GenericEntityHeader;

import java.util.Collection;
import java.util.logging.Logger;


public final class MongoDBConnectionEntityAdminImpl implements MongoDBConnectionEntityAdmin {
    private static final Logger logger = Logger.getLogger(MongoDBConnectionEntityAdminImpl.class.getName());
    private EntityManager<MongoDBConnectionEntity, GenericEntityHeader> entityManager;
    private final Audit auditor;
    private static MongoDBConnectionEntityAdmin instance;


    private MongoDBConnectionEntityAdminImpl(){
         auditor = null;
    }

    //Not sure if this really needs to be a singleton
    private MongoDBConnectionEntityAdminImpl(EntityManager<MongoDBConnectionEntity, GenericEntityHeader> entityManager) {
        this.entityManager = entityManager;
        this.auditor = new LoggingAudit(logger);
    }

    public static MongoDBConnectionEntityAdmin getInstance(EntityManager<MongoDBConnectionEntity, GenericEntityHeader> entityManager){
        if (instance == null){
            instance = new MongoDBConnectionEntityAdminImpl(entityManager);
        }
        return instance;
    }

    @Override
    public Goid save(MongoDBConnectionEntity mongoDBConnectionEntity) throws SaveException, UpdateException {
        return entityManager.save(mongoDBConnectionEntity);
    }

    @Override
    public void update(MongoDBConnectionEntity mongoDBConnectionEntity) throws UpdateException {
        entityManager.update(mongoDBConnectionEntity);
    }


    @Override
    public void delete(MongoDBConnectionEntity mongoDBConnectionEntity) throws FindException, DeleteException {
        entityManager.delete(mongoDBConnectionEntity);
    }

    @Override
    public MongoDBConnectionEntity findByGoid(Goid goid) throws FindException {
        return entityManager.findByPrimaryKey(goid);
    }

    @Override
    public Collection<MongoDBConnectionEntity> findByType() throws FindException {

        Collection<MongoDBConnectionEntity> col = entityManager.findAll();
        if (col != null) {
            Collection<MongoDBConnectionEntity> col2 = (Collection) col;
            return col2;
        }

        return null;
    }



}