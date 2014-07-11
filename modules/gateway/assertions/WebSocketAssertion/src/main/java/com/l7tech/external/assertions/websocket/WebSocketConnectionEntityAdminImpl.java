package com.l7tech.external.assertions.websocket;

import com.l7tech.gateway.common.security.rbac.Secured;
import com.l7tech.objectmodel.*;
import com.l7tech.policy.GenericEntityHeader;

import java.util.Collection;

/**
 * Date: 6/4/12
 * Time: 11:45 AM
 */

@Secured
public class WebSocketConnectionEntityAdminImpl implements WebSocketConnectionEntityAdmin {

    private EntityManager<WebSocketConnectionEntity, GenericEntityHeader> goidEntityManager;

    public WebSocketConnectionEntityAdminImpl(EntityManager<WebSocketConnectionEntity, GenericEntityHeader> goidEntityManager) {
        this.goidEntityManager = goidEntityManager;
    }

    @Override
    public Collection<WebSocketConnectionEntity> findAll() throws FindException {
        return goidEntityManager.findAll();
    }

    @Override
    public Goid save(WebSocketConnectionEntity entity) throws SaveException, UpdateException {
        if (Goid.equals(entity.getGoid(), WebSocketConnectionEntity.DEFAULT_GOID)) {
            return goidEntityManager.save(entity);
        } else {
            goidEntityManager.update(entity);
            return entity.getGoid();
        }
    }

    @Override
    public void delete(WebSocketConnectionEntity entity) throws DeleteException, FindException {
        goidEntityManager.delete(entity);
    }

    @Override
    public WebSocketConnectionEntity findByPrimaryKey(Goid key) throws FindException {
        return goidEntityManager.findByPrimaryKey(key);
    }
}
