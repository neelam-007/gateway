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
import com.l7tech.server.EntityCrud;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.gateway.common.service.PublishedService;

import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.lang.reflect.Method;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;

import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;


/**
 * @author jbufu
 */
public class MigrationManagerImpl implements MigrationManager {

    private static final Logger logger = Logger.getLogger(MigrationManagerImpl.class.getName());

    private EntityFinder entityFinder;
    private PlatformTransactionManager transactionManager;
    private EntityCrud entityCrud;

    public MigrationManagerImpl(EntityFinder entityFinder, PlatformTransactionManager transactionManager, EntityCrud entityCrud) {
        this.entityFinder = entityFinder;
        this.transactionManager = transactionManager;
        this.entityCrud = entityCrud;
    }

    public Collection<EntityHeader> listEntities(Class<? extends Entity> clazz) throws MigrationException {
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

    public MigrationBundle exportBundle(final Collection<EntityHeader> headers) throws MigrationException {

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

    public Map<EntityHeader, EntityHeaderSet> retrieveMappingCandidates(Collection<EntityHeader> mappables) throws MigrationException {

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


    @Transactional(rollbackFor = Throwable.class)
    public void importBundle(MigrationBundle bundle) throws MigrationException {

        MigrationErrors errors = validateBundle(bundle);
        if (! errors.isEmpty())
            //logger.log(Level.WARNING, "Migration bundle validation failed.", errors);
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
                if (! bundle.getMetadata().isUploadedByParent(header))
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
                        logger.log(Level.WARNING, "Errors while applying mapping: ", e);
                        errors.add(mapping, e);
                    }
                }
            } catch (MigrationException e) {
                logger.log(Level.WARNING, "Errors while applying mapping: ", e);
                errors.add(header, e);
            }
        }
        if (! errors.isEmpty())
            //logger.log(Level.WARNING, "Errors while applying mappings for the entities to import.", errors);
            throw new MigrationException("Errors while applying mappings for the entities to import.", errors);

        try {
            doUpload(entitiesToImport);
        } catch (org.hibernate.exception.ConstraintViolationException e1) {
            logger.log(Level.FINE, "test for duplicate entity on upload...", e1);
        } catch (DuplicateObjectException e2) {
            logger.log(Level.FINE, "Duplicate entity during import.", e2);
        } catch (SaveException e3) {
            throw new MigrationException("Import failed.", e3);
        }
    }

    private void doUpload(Map<EntityHeader, Entity> entitiesToImport) throws SaveException {
        for (Entity entity : entitiesToImport.values()) {
            try {
                if (entityFinder.find(EntityHeaderUtils.fromEntity(entity)) != null) {
                    logger.log(Level.FINE, "Entity already exists or has been uploaded, skipping." + entity);
                    continue;
                }
            } catch (FindException e) {
                // not found, upload new
            }
            entityCrud.save(entity);
        }
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
        Entity entity;
        try {
            entity = loadEntity(header);
        } catch (Exception e) {
            return;
        }
        for (Method method : entity.getClass().getMethods()) {
            if (MigrationUtils.isDependency(method)) {
                PropertyResolver resolver = MigrationUtils.getResolver(method);
                Map<EntityHeader, Set<MigrationMapping>> deps;
                try {
                    deps = resolver.getDependencies(header, entity, method);
                } catch (MigrationException e) {
                    throw new MigrationException("Error getting dependencies for property: " + method, e);
                }
                for (EntityHeader depHeader : deps.keySet()) {
                    for (MigrationMapping mapping : deps.get(depHeader)) {
                        result.addMapping(mapping);
                        logger.log(Level.FINE, "Added mapping: " + mapping);
                        if (!result.hasHeader(depHeader))
                            findDependenciesRecursive(result, depHeader);
                    }
                }
            }
        }
    }

    private Entity loadEntity(EntityHeader header) throws MigrationException {
        try {
            Entity ent = entityFinder.find(header); // load the entity
            if (ent == null)
                throw new MigrationException("Error loading the entity for header: " + header);
            return ent;
        } catch (FindException e) {
            throw new MigrationException("Error loading the entity for header: " + header, e);
        }
    }

    private void test() {
        try {
            Collection<EntityHeader> headers = listEntities(PublishedService.class);
            logger.log(Level.FINE, "Retrieved " + headers.size() + " entities.");

            MigrationBundle bundle = exportBundle(new HashSet(headers));

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
