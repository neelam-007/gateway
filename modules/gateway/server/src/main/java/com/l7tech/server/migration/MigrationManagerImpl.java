package com.l7tech.server.migration;

import com.l7tech.server.management.migration.bundle.MigrationMetadata;
import com.l7tech.server.management.migration.bundle.MigrationBundle;
import com.l7tech.server.management.migration.bundle.ExportedItem;
import com.l7tech.server.management.migration.MigrationManager;
import com.l7tech.objectmodel.migration.MigrationException;
import com.l7tech.objectmodel.migration.PropertyResolver;
import com.l7tech.objectmodel.*;
import com.l7tech.objectmodel.migration.*;
import static com.l7tech.objectmodel.migration.MigrationMappingSelection.*;
import static com.l7tech.objectmodel.migration.MigrationException.*;
import com.l7tech.server.EntityFinder;
import com.l7tech.server.EntityHeaderUtils;
import com.l7tech.util.ExceptionUtils;

import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.lang.reflect.Method;

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

        final MigrationException[] thrown = new MigrationException[1];
        MigrationBundle result = (MigrationBundle) new TransactionTemplate(transactionManager).execute(new TransactionCallback() {
            @Override
            public Object doInTransaction(TransactionStatus transactionStatus) {
                try {
                    MigrationMetadata metadata = findDependencies(headers);
                    MigrationBundle bundle = new MigrationBundle(metadata);
                    for (EntityHeader header : metadata.getHeaders()) {
                        if (metadata.isMappingRequired(header))
                            continue;// don't serialize entities that MUST be mapped
                        Entity ent = loadEntity(header);
                        logger.log(Level.FINE, "Entity value for header (" + header.toStringVerbose() + ") : " + ent);
                        bundle.addExportedItem(new ExportedItem(header, ent));
                    }
                    return bundle;
                } catch (MigrationException e) {
                    thrown[0] = e;
                    return null;
                }
            }
        });

        if (thrown[0] != null)
            throw thrown[0];
        else
            return result;
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

        // bundle validation
        MigrationErrors errors = validateBundle(bundle);
        if (! errors.isEmpty())
            throw new MigrationException("Migration bundle validation failed.", errors);

        // load entities
        Map<EntityHeaderRef, Entity> entities = new HashMap<EntityHeaderRef, Entity>();
        Map<EntityHeader, Entity> entitiesToImport = new HashMap<EntityHeader, Entity>();
        for (EntityHeader header : bundle.getMetadata().getHeaders()) {

            Entity ent;
            try {
                // try the local ssg first
                entities.put(header, loadEntity(header));
            } catch (MigrationException e) {
                // load it from the bundle
                ent = bundle.getExportedItem(header).getValue();
                entities.put(header, ent);
                entitiesToImport.put(header, ent);
            }
        }

        // apply mappings
        Entity sourceEntity, targetEntity;
        for (EntityHeader header : entitiesToImport.keySet()) {
            sourceEntity = entitiesToImport.get(header);
            try {
                for (MigrationMapping mapping : bundle.getMetadata().getMappingsForSource(header)) {
                    targetEntity = entities.get(mapping.getTarget());
                    PropertyResolver resolver = MigrationUtils.getResolver(sourceEntity, mapping.getPropName());
                    try {
                        resolver.applyMapping(sourceEntity, mapping.getPropName(), targetEntity);
                    } catch (MigrationException e) {
                        errors.add(mapping, e);
                    }
                }
            } catch (MigrationException e) {
                errors.add(header, e);
            }
        }
        if (! errors.isEmpty())
            throw new MigrationException("Errors while applying mappings for the entities to import.", errors);


        // todo: upload entities ( entitiesToImport )
    }

    @SuppressWarnings({"ThrowableInstanceNeverThrown"})
    private MigrationErrors validateBundle(MigrationBundle bundle) {

        MigrationErrors errors = new MigrationErrors();
        MigrationMetadata metadata = bundle.getMetadata();

        // check that entity values are available for all headers, either in the bundle or already on the SSG
        for (EntityHeader header : metadata.getHeaders()) {
            if (! bundle.hasItem(header)) {
                try {
                    if (loadEntity(header) == null)
                        errors.add(header, new MigrationException("Null entity retrived for header: " + header));
                } catch (MigrationException e) {
                    errors.add(header, e);
                }
            }
        }

        // dependency-check covered by the above and mapping targets check below

        // mapping requirements
        for(MigrationMapping mapping : metadata.getMappings()) {

            // all headers present in the metadata
            EntityHeaderRef header = mapping.getSource();
            if (! metadata.hasHeader(header))
                errors.add(header, new MigrationException("Header listed as the source of a dependency, but not included in bundle metadata: " + header));
            header = mapping.getTarget();
            if (! metadata.hasHeader(header))
                errors.add(header, new MigrationException("Header listed as a dependency, but not included in bundle metadata: " + header));

            // name-mapping required
            if (mapping.getType().getNameMapping() == REQUIRED && ! mapping.isMappedTarget())
                errors.add(mapping, new MigrationException("Unresolved name-mapping: " + mapping));

            // value-mapping required
            if (! mapping.isMappedTarget() && mapping.getType().getValueMapping() == REQUIRED &&
                ! bundle.getExportedItem(mapping.getTarget()).isMappedValue()) {
                errors.add(mapping, new MigrationException("Unresolved value-mapping: " + mapping));
            }
        }

        return errors;
    }

    private void findDependenciesRecursive(MigrationMetadata result, EntityHeader header) throws MigrationException {
        logger.log(Level.FINE, "Finding dependencies for: " + header.toStringVerbose());
        result.addHeader(header);// marks header as processed
        Entity entity = loadEntity(header);
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
                } catch (MigrationException e) {
                    throw new MigrationException("Error getting dependencies for property: " + method, e);
                }
            }
        }
    }

    private Entity loadEntity(EntityHeader header) throws MigrationException {
        try {
            return entityFinder.find(header); // load the entity
        } catch (FindException e) {
            throw new MigrationException("Error loading the entity for header: " + header, e);
        }
    }

}
