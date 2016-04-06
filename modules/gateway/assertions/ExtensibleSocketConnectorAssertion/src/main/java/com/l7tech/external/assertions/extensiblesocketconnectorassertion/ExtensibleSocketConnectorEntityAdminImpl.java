package com.l7tech.external.assertions.extensiblesocketconnectorassertion;

import com.l7tech.external.assertions.extensiblesocketconnectorassertion.server.MinaCodecFactory;
import com.l7tech.objectmodel.*;
import com.l7tech.policy.GenericEntityHeader;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by IntelliJ IDEA.
 * User: njordan
 * Date: 27/03/12
 * Time: 2:55 PM
 * To change this template use File | Settings | File Templates.
 */
public class ExtensibleSocketConnectorEntityAdminImpl implements ExtensibleSocketConnectorEntityAdmin {
    private static final Logger LOGGER = Logger.getLogger(ExtensibleSocketConnectorEntityAdminImpl.class.getName());

    private EntityManager<ExtensibleSocketConnectorEntity, GenericEntityHeader> entityManager;

    public ExtensibleSocketConnectorEntityAdminImpl(EntityManager<ExtensibleSocketConnectorEntity, GenericEntityHeader> entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public Collection<ExtensibleSocketConnectorEntity> findAll() throws FindException {
        return entityManager.findAll();
    }

    @Override
    public Goid save(ExtensibleSocketConnectorEntity entity) throws SaveException, UpdateException {
        try {
            if (entity.getGoid().equals(ExtensibleSocketConnectorEntity.DEFAULT_GOID)) {
                Goid goid = entityManager.save(entity);
                entity.setGoid(goid);
                return goid;
            } else {
                entityManager.update(entity);
                return entity.getGoid();
            }
        } catch (SaveException e) {
            LOGGER.log(Level.WARNING, "Error saving the ExtensibleSocketConnectorEntity '{0}': {1}", new Object[]{entity.getName(), e.getMessage()});
            throw e;
        } catch (UpdateException e) {
            LOGGER.log(Level.WARNING, "Error updating the ExtensibleSocketConnectorEntity '{0}': {1}", new Object[]{entity.getName(), e.getMessage()});
            throw e;
        }
    }

    @Override
    public void delete(ExtensibleSocketConnectorEntity entity) throws DeleteException, FindException {
        entityManager.delete(entity);
    }

    @Override
    public ExtensibleSocketConnectorEntity findByUniqueName(String name) throws FindException {
        return entityManager.findByUniqueName(name);
    }

    @Override
    public ExtensibleSocketConnectorEntity find(Goid goid) throws FindException {
        return entityManager.findByPrimaryKey(goid);
    }

    @Override
    public Vector<CodecModule> getCodecModules() {
        return MinaCodecFactory.getCodecModules();
    }
}
