package com.l7tech.server.migration;

import com.l7tech.server.management.migration.bundle.MigrationMetadata;
import com.l7tech.server.management.migration.bundle.MigrationBundle;
import com.l7tech.server.management.migration.bundle.ExportedItem;
//import com.l7tech.server.management.migration.bundle.MigrationDependency;
import com.l7tech.server.management.migration.MigrationManager;
import com.l7tech.objectmodel.migration.MigrationException;
import com.l7tech.objectmodel.migration.PropertyResolver;
import com.l7tech.objectmodel.*;
import com.l7tech.objectmodel.migration.*;
import com.l7tech.server.EntityFinder;
import com.l7tech.server.EntityHeaderUtils;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.gateway.common.service.PublishedService;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.lang.reflect.Method;
import java.io.*;

import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.PlatformTransactionManager;


/**
 * @author jbufu
 */
public class MigrationManagerImpl implements MigrationManager {

    private static final Logger logger = Logger.getLogger(MigrationManagerImpl.class.getName());

    private EntityFinder entityFinder;
    private PlatformTransactionManager transactionManager;

    public MigrationManagerImpl(EntityFinder entityFinder, PlatformTransactionManager transactionManager) {
        this.entityFinder = entityFinder;
        this.transactionManager = transactionManager;
        test();
    }

    public EntityHeaderSet<EntityHeader> listEntities(Class<? extends Entity> clazz) throws MigrationException {
        try {
            return entityFinder.findAll(clazz);
        } catch (FindException e) {
            throw new MigrationException("Error listing entities for " + clazz + " : " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
        }
    }

    public MigrationMetadata findDependencies(Collection<EntityHeader> headers) throws MigrationException {
        MigrationMetadata result = new MigrationMetadata();
        result.setHeaders(headers);

        for (EntityHeader header : headers) {
            findDependenciesRecursive(result, header);
        }

        return result;
    }

    public MigrationBundle exportBundle(final Set<EntityHeader> headers) throws MigrationException {

        return (MigrationBundle) new TransactionTemplate(transactionManager).execute(new TransactionCallback() {
            @Override
            public Object doInTransaction(TransactionStatus transactionStatus) {
                try {
                    MigrationMetadata metadata = findDependencies(headers);
                    MigrationBundle bundle = new MigrationBundle(metadata);
                    for (EntityHeader header : metadata.getHeaders()) {
                        try {
                            if (metadata.isMappingRequired(header))
                                continue; // don't serialize entities that MUST be mapped
                            Entity ent = entityFinder.find(header);
                            logger.log(Level.FINE, "Entity value for header (" + header.toStringVerbose() + ") : " + ent);
                            bundle.addExportedItem(new ExportedItem(header, ent));
                        } catch (FindException e) {
                            throw new MigrationException("Error exporting entity " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
                        }
                    }
                    return bundle;
                } catch (MigrationException e) {
                    return null;
                }
            }
        });
    }

    public Map<EntityHeader, EntityHeaderSet> retrieveMappingCandidates(Set<EntityHeader> mappables) throws MigrationException {

        Map<EntityHeader,EntityHeaderSet> result = new HashMap<EntityHeader,EntityHeaderSet>();

        for (EntityHeader header : mappables) {
            try {
                result.put(header, entityFinder.findAll(EntityHeaderUtils.getEntityClass(header)));
            } catch (FindException e) {
                throw new MigrationException("Error retrieving mapping candidate: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            }
        }

        return result;
    }

    public void importBundle(MigrationBundle bundle) throws MigrationException {
        // todo
    }

    private void findDependenciesRecursive(MigrationMetadata result, EntityHeader header) throws MigrationException {
        try {
            logger.log(Level.FINE, "Finding dependencies for: " + header.toStringVerbose());
            result.addHeader(header); // marks header as processed
            Entity entity = entityFinder.find(header); // load the entity
            for (Method method : entity.getClass().getMethods()) {
                if (MigrationUtils.isDependency(method)) {
                    PropertyResolver resolver = MigrationUtils.getResolver(method);
                    try {
                        Map<EntityHeader, Set<MigrationMapping>> deps = resolver.getDependencies(header, entity, method);
                        for (EntityHeader depHeader : deps.keySet()) {
                            for (MigrationMapping mapping : deps.get(depHeader)) {
                                result.addMapping(mapping);
                                logger.log(Level.FINE, "Added mapping: " + mapping);
                                if (!result.hasHeader(depHeader))
                                    findDependenciesRecursive(result, depHeader);
                            }
                        }
                    } catch (PropertyResolverException e) {
                        throw new MigrationException("Error getting dependencies for property: " + method, e);
                    }
                }
            }
        } catch (FindException e) {
            throw new MigrationException("Error loading the entity for header: " + header, e);
        }
    }

    private void test() {
        try {
            Set<EntityHeader> headers = listEntities(PublishedService.class);
            logger.log(Level.FINE, "Retrieved " + headers.size() + " entities.");

            MigrationBundle bundle = exportBundle(headers);

            //Collection<Class<? extends Entity>> entityClasses = EntityTypeRegistry.getAllEntityClasses();
            Collection<Class> jaxbClasses = new HashSet<Class>() {{
                add(MigrationBundle.class);
                add(EntityType.POLICY.getEntityClass());
                add(EntityType.SERVICE.getEntityClass());
            }};

            JAXBContext jaxbc = JAXBContext.newInstance(jaxbClasses.toArray(new Class[jaxbClasses.size()]));
            Marshaller marshaller = jaxbc.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

            OutputStream out = new ByteArrayOutputStream();
            marshaller.marshal(bundle, out);
            System.out.println(out.toString());

/*
            Unmarshaller unmarshaller = jaxbc.createUnmarshaller();
            MigrationBundle bundle2 = (MigrationBundle) unmarshaller.unmarshal(new ByteArrayInputStream(out.toString().getBytes()));
            logger.log(Level.FINE, "Unmarshalling done: " + bundle2);
*/


        } catch (Exception e) {
            logger.log(Level.WARNING, "Error listing entities,", e);
        }
    }
}
