package com.l7tech.external.assertions.simplegatewaymetricextractor.server;

import com.l7tech.external.assertions.simplegatewaymetricextractor.SimpleGatewayMetricExtractorEntity;
import com.l7tech.external.assertions.simplegatewaymetricextractor.SimpleGatewayMetricExtractorGenericEntityAdmin;
import com.l7tech.objectmodel.*;
import com.l7tech.policy.GenericEntityHeader;
import com.l7tech.policy.assertion.ExtensionInterfaceBinding;
import com.l7tech.server.entity.GenericEntityManager;
import org.springframework.context.ApplicationContext;

import java.util.Collection;
import java.util.Collections;

public class GenericEntityManagerSimpleGatewayMetricExtractorServerSupport {

    private static class Instance {
        GenericEntityManagerSimpleGatewayMetricExtractorServerSupport support = null;
        GenericEntityManager genericEntityManager = null;

        Instance(GenericEntityManagerSimpleGatewayMetricExtractorServerSupport support, GenericEntityManager genericEntityManager) {
            this.support = support;
            this.genericEntityManager = genericEntityManager;
        }
    }
    private static Instance instance = null;

    private EntityManager<SimpleGatewayMetricExtractorEntity, GenericEntityHeader> entityManager;

    public static synchronized GenericEntityManagerSimpleGatewayMetricExtractorServerSupport getInstance(final ApplicationContext context) {
        if (instance == null) {
            GenericEntityManagerSimpleGatewayMetricExtractorServerSupport s = new GenericEntityManagerSimpleGatewayMetricExtractorServerSupport();
            instance = new Instance(s, s.init(context));
        }
        return instance.support;
    }

    private GenericEntityManager init(ApplicationContext context) {
        GenericEntityManager gem = context.getBean("genericEntityManager", GenericEntityManager.class);
        gem.registerClass(SimpleGatewayMetricExtractorEntity.class, null);
        entityManager = gem.getEntityManager(SimpleGatewayMetricExtractorEntity.class);
        return gem;
    }

    static synchronized void clearInstance() {
        if (instance != null) {
            instance.support = null;
            instance.genericEntityManager.unRegisterClass(SimpleGatewayMetricExtractorEntity.class.getName());
            instance.genericEntityManager = null;
            instance = null;
        }
    }

    public Collection<ExtensionInterfaceBinding> getExtensionInterfaceBindings() {
        ExtensionInterfaceBinding binding = new ExtensionInterfaceBinding<>(SimpleGatewayMetricExtractorGenericEntityAdmin.class, null, new SimpleGatewayMetricExtractorGenericEntityAdmin() {
            @Override
            public Collection<SimpleGatewayMetricExtractorEntity> findAll() throws FindException {
                return entityManager.findAll();
            }

            @Override
            public SimpleGatewayMetricExtractorEntity getEntity() throws FindException {
                return entityManager.findByUniqueName(SimpleGatewayMetricExtractorEntity.ENTITY_UNIQUE_NAME);
            }

            @Override
            public Goid save(SimpleGatewayMetricExtractorEntity entity) throws SaveException, UpdateException {
                if (SimpleGatewayMetricExtractorEntity.DEFAULT_GOID.equals(entity.getGoid())) {
                    return entityManager.save(entity);
                } else {
                    entityManager.update(entity);
                    return entity.getGoid();
                }
            }

            @Override
            public void delete(SimpleGatewayMetricExtractorEntity entity) throws DeleteException, FindException {
                entityManager.delete(entity);
            }
        });
        return Collections.singletonList(binding);
    }
}