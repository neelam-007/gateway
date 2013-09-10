package com.l7tech.external.assertions.whichmodule.server;

import com.l7tech.external.assertions.whichmodule.DemoGenericEntity;
import com.l7tech.external.assertions.whichmodule.DemoGenericEntityAdmin;
import com.l7tech.objectmodel.*;
import com.l7tech.policy.GenericEntityHeader;
import com.l7tech.policy.assertion.ExtensionInterfaceBinding;
import com.l7tech.server.entity.GenericEntityManager;
import org.springframework.context.ApplicationContext;

import java.util.Collection;
import java.util.Collections;

/**
 * Server-side glue for demoing the generic entity manager.
 * This exposes a generic entity manager via a simple admin extension interface.
 */
public class GenericEntityManagerDemoServerSupport {
    private static GenericEntityManagerDemoServerSupport instance;

    private EntityManager<DemoGenericEntity, GenericEntityHeader> demoGenericEntityManager;

    public static synchronized GenericEntityManagerDemoServerSupport getInstance(final ApplicationContext context) {
        if (instance == null) {
            GenericEntityManagerDemoServerSupport s = new GenericEntityManagerDemoServerSupport();
            s.init(context);
            instance = s;
        }
        return instance;
    }

    public void init(ApplicationContext context) {
        GenericEntityManager gem = context.getBean("genericEntityManager", GenericEntityManager.class);
        gem.registerClass(DemoGenericEntity.class, null);
        demoGenericEntityManager = gem.getEntityManager(DemoGenericEntity.class);
    }

    public Collection<ExtensionInterfaceBinding> getExtensionInterfaceBindings() {
        ExtensionInterfaceBinding binding = new ExtensionInterfaceBinding<DemoGenericEntityAdmin>(DemoGenericEntityAdmin.class, null, new DemoGenericEntityAdmin() {
            @Override
            public Collection<DemoGenericEntity> findAll() throws FindException {
                return demoGenericEntityManager.findAll();
            }

            @Override
            public Goid save(DemoGenericEntity entity) throws SaveException, UpdateException {
                if (DemoGenericEntity.DEFAULT_GOID.equals(entity.getGoid())) {
                    return demoGenericEntityManager.save(entity);
                } else {
                    demoGenericEntityManager.update(entity);
                    return entity.getGoid();
                }
            }

            @Override
            public void delete(DemoGenericEntity entity) throws DeleteException, FindException {
                demoGenericEntityManager.delete(entity);
            }
        });
        return Collections.singletonList(binding);
    }
}
