package com.l7tech.external.assertions.mongodb.entity;


import com.l7tech.gateway.common.admin.Administrative;
import com.l7tech.gateway.common.security.rbac.Secured;
import com.l7tech.objectmodel.*;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;

import static com.l7tech.gateway.common.security.rbac.MethodStereotype.*;


@Secured(types= EntityType.GENERIC)
@Administrative
public interface MongoDBConnectionEntityAdmin {

    @Secured(stereotype=SAVE_OR_UPDATE)
    Goid save(MongoDBConnectionEntity mongoDBConnectionEntity) throws SaveException, UpdateException;

    @Secured(stereotype=SAVE_OR_UPDATE)
    void update(MongoDBConnectionEntity mongoDBConnectionEntity) throws UpdateException;

    @Secured(stereotype=DELETE_ENTITY)
    void delete(MongoDBConnectionEntity mongoDBConnectionEntity) throws FindException, DeleteException;

    @Transactional(readOnly=true)
    @Secured(stereotype=FIND_ENTITIES)
    Collection<MongoDBConnectionEntity> findByType() throws FindException;

    @Transactional(readOnly=true)
    @Secured(stereotype=FIND_ENTITY)
    MongoDBConnectionEntity findByGoid(Goid goid) throws FindException;

}
