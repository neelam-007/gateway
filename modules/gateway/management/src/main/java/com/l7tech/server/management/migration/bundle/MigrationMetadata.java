package com.l7tech.server.management.migration.bundle;

import com.l7tech.objectmodel.*;
import com.l7tech.objectmodel.migration.MigrationMapping;
import com.l7tech.objectmodel.migration.MigrationMappingSelection;
import static com.l7tech.objectmodel.migration.MigrationMappingSelection.NONE;
import com.l7tech.util.Pair;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.server.management.api.node.MigrationApi;

import javax.xml.bind.annotation.*;
import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Summary of a Migration Bundle, consisting of:
 * <ul>
 * <li>A set of headers for all exported entities in the bundle, including dependencies.</li>
 * <li>Migration Mappings, describing the dependencies and mapping types for the entities in the bundle.</li>
 * <li></li>
 * </ul>
 *
 * Not thread-safe. 
 *
 * @see com.l7tech.objectmodel.migration.MigrationMapping
 * @author jbufu
 */
@XmlRootElement
@XmlType(propOrder={"headers", "mappings", "originalHeaders"})
public class MigrationMetadata {

    private static final Logger logger = Logger.getLogger(MigrationMetadata.class.getName());

    /**
     * Headers for all the items in the Migration Bundle. Used only by/during JAXB marshalling / unmarshalling operations.
     */
    private Set<EntityHeader> headers = new HashSet<EntityHeader>();
    private Set<EntityHeader> originalHeaders = new HashSet<EntityHeader>(); // needed to apply properties through UsesEntities

    /**
     * Headers for all the items in the Migration Bundle. Used by migration business logic.
     */
    private Map<EntityHeaderRef, EntityHeader> headersMap = null;
    private Map<EntityHeaderRef, EntityHeader> originalHeadersMap = null;

    private Set<MigrationMapping> mappings = new HashSet<MigrationMapping>();
    // easy access to dependencies / dependents
    private Map<EntityHeaderRef, Set<MigrationMapping>> mappingsBySource;
    private Map<EntityHeaderRef, Set<MigrationMapping>> mappingsByTarget;

    // --- header operations ---

    @XmlElementWrapper(name="headers")
    @XmlElementRef
    public Collection<EntityHeader> getHeaders() {
        if (headersMap != null)
            // immutable, throws unsupported operation on modification attempts
            return headersMap.values();
        else
            // should be used by JAXB unmarshalling only
            return headers;
    }

    public void setHeaders(Collection<EntityHeader> headers) {
        // state reset: switching to "map cache uninitialized"
        this.headersMap = null;
        this.headers = new HashSet<EntityHeader>(headers);
    }

    @XmlElementWrapper(name="originalHeaders")
    @XmlElementRef
    public Collection<EntityHeader> getOriginalHeaders() {
        if (originalHeadersMap != null)
            // immutable, throws unsupported operation on modification attempts
            return originalHeadersMap.values();
        else
            // should be used by JAXB unmarshalling only
            return originalHeaders;
    }

    public void setOriginalHeaders(Collection<EntityHeader> headers) {
        // state reset: switching to "map cache uninitialized"
        this.originalHeadersMap = null;
        this.originalHeaders = new HashSet<EntityHeader>(headers);
    }

    private void initHeadersCache() {
        // state change: JAXB init over; switching to the internal map representation
        HashMap<EntityHeaderRef, EntityHeader> headersMap = new HashMap<EntityHeaderRef, EntityHeader>();
        for(EntityHeader header : headers) {
            headersMap.put(EntityHeaderRef.fromOther(header), header);
        }
        this.headersMap = headersMap;
        this.headers = null;

        HashMap<EntityHeaderRef, EntityHeader> originalHeadersMap = new HashMap<EntityHeaderRef, EntityHeader>();
        for(EntityHeader header : originalHeaders) {
            originalHeadersMap.put(EntityHeaderRef.fromOther(header), header);
        }
        this.originalHeadersMap = originalHeadersMap;
        this.originalHeaders = null;

        logger.log(Level.FINEST, "Headers cache initialized.");
    }

    private Map<EntityHeaderRef, EntityHeader> getHeadersMap() {
        if (headersMap == null) initHeadersCache();
        return headersMap;
    }

    private Map<EntityHeaderRef, EntityHeader> getOriginalHeadersMap() {
        if (originalHeadersMap == null) initHeadersCache();
        return originalHeadersMap;
    }

    public void addHeader(EntityHeader header) {
        getHeadersMap().put(EntityHeaderRef.fromOther(header), header);
    }

    public boolean hasHeader(EntityHeaderRef headerRef) {
        return getHeadersMap().containsKey(EntityHeaderRef.fromOther(headerRef));
    }

    public EntityHeader getHeader(EntityHeaderRef headerRef) {
        return getHeadersMap().get(EntityHeaderRef.fromOther(headerRef));
    }

    public EntityHeader getOriginalHeader(EntityHeaderRef headerRef) {
        return getOriginalHeadersMap().get(EntityHeaderRef.fromOther(headerRef));
    }

    public void removeHeader(EntityHeaderRef headerRef) {
        EntityHeaderRef ref = EntityHeaderRef.fromOther(headerRef);
        getOriginalHeadersMap().put(EntityHeaderRef.fromOther(headerRef), getHeadersMap().remove(ref));
    }

    public boolean isMappingRequired(EntityHeaderRef headerRef) throws MigrationApi.MigrationException {
        for (MigrationMapping mapping : getMappingsForTarget(EntityHeaderRef.fromOther(headerRef))) {
            if ( mapping.getType().getNameMapping() == MigrationMappingSelection.REQUIRED ||
                 mapping.getType().getValueMapping() == MigrationMappingSelection.REQUIRED)
                return true;
        }
        return false;
    }

    public boolean includeInExport(EntityHeaderRef headerRef) throws MigrationApi.MigrationException {
        if (isMappingRequired(headerRef)) {
            return false;
        } else {
            Set<MigrationMapping> deps = getMappingsForTarget(EntityHeaderRef.fromOther(headerRef));

            if (deps == null || deps.size() == 0) // top-level item
                return true;

            for (MigrationMapping mapping : deps) {
                if (mapping.isExport())
                    return true;
            }

            return false;
        }
    }

    // --- mapping / dependencies operations ---

    private void initMappingsCache()  {
        mappingsBySource = new HashMap<EntityHeaderRef, Set<MigrationMapping>>();
        mappingsByTarget = new HashMap<EntityHeaderRef, Set<MigrationMapping>>();
        for (MigrationMapping mapping : this.mappings) {
            addMappingsForSource(mapping.getDependant(), Collections.singleton(mapping));
            addMappingsForTarget(mapping.getDependency(), Collections.singleton(mapping));
        }
        logger.log(Level.FINEST, "Mappings cache initialized.");
    }

    @XmlElementWrapper(name="mappings")
    @XmlElementRef
    public Set<MigrationMapping> getMappings() {
        return mappings;
    }

    public void setMappings(Set<MigrationMapping> mappings) throws MigrationApi.MigrationException {
        // state reset: switching to "map cache uninitialized"
        this.mappingsBySource = null;
        this.mappingsByTarget = null;
        this.mappings = mappings;
    }

    public Set<EntityHeader> getMappableDependencies() {
        Set<EntityHeader> result = new HashSet<EntityHeader>();
        for(MigrationMapping m : mappings) {
            if (m.getType().getNameMapping() != NONE || m.getType().getValueMapping() != NONE)
                result.add(getHeader(m.getDependency()));
        }
        return result;
    }

    public void addMappings(Set<MigrationMapping> mappings) throws MigrationApi.MigrationException {
        for (MigrationMapping mapping : mappings) {
            addMapping(mapping);
        }
    }

    public void addMapping(MigrationMapping mapping) throws MigrationApi.MigrationException {
        logger.log(Level.FINEST, "Adding mapping: {0}", mapping);
        if (mapping == null) return;

/*
        MigrationMapping conflicting = hasConflictingMapping(mapping);
        if (conflicting != null) {
            if (mapping.getType().getValueMapping() != MigrationMappingSelection.OPTIONAL) {
                throw new MigrationApi.MigrationException("New mapping: " + mapping + " conflicts with: " + conflicting);
            } else {
                logger.log(Level.WARNING, "New mapping would create a conflict; switching value-mapping from OPTIONAL to REQUIRED for: " + mapping);
                mapping.getType().setValueMapping(MigrationMappingSelection.REQUIRED);
            }
        }
*/

        mappings.add(mapping);
        addMappingsForSource(mapping.getDependant(), Collections.singleton(mapping));
        addMappingsForTarget(mapping.getDependency(), Collections.singleton(mapping));
    }

    @SuppressWarnings({"ThrowableInstanceNeverThrown"})
    public void mapNames(Set<Pair<EntityHeaderRef,EntityHeader>> mappings) throws MigrationApi.MigrationException {
        Set<Pair<EntityHeaderRef,EntityHeader>> toApply = new HashSet<Pair<EntityHeaderRef, EntityHeader>>(mappings);
        Set<Pair<EntityHeaderRef,EntityHeader>> toRemove = new HashSet<Pair<EntityHeaderRef, EntityHeader>>();
        Collection<String> errors = new HashSet<String>();
        do {
            toRemove.clear();
            errors.clear();
            for(Pair<EntityHeaderRef,EntityHeader> pair : toApply) {
                if (getMappingsForTarget(pair.getKey()).isEmpty()) {
                    toRemove.add(pair);
                } else {
                    try {
                        mapName(pair.getKey(), pair.getValue());
                        toRemove.add(pair);
                    } catch (Exception e) {
                        errors.add("Error applying mapping for " + pair.getKey() + " : " + ExceptionUtils.getMessage(e));
                    }
                }
            }
            toApply.removeAll(toRemove);
        } while (! toApply.isEmpty() && !toRemove.isEmpty());

        if (! toApply.isEmpty())
            throw new MigrationApi.MigrationException("Unable to apply mappings.", errors);
    }

    public void mapName(EntityHeaderRef dependency, EntityHeader newDependency) throws MigrationApi.MigrationException {
        mapName(dependency, newDependency, true);
    }

    public void mapName(EntityHeaderRef dependency, EntityHeader newDependency, boolean enforceMappingType) throws MigrationApi.MigrationException {

        logger.log(Level.FINE, "Name-mapping: {0} -> {1}.", new Object[]{dependency, newDependency});
        if (dependency == null || newDependency == null)
            return;

        // add new header to the bundle
        removeHeader(dependency);
        addHeader(newDependency);

        // incoming dependencies
        Set<MigrationMapping> mappingsForDependency = getMappingsForTarget(dependency);
        for (MigrationMapping mapping : mappingsForDependency) {
            // todo: one-to-many mappings: decide how/when to apply this mapping (vs a presious / individual selection)
            mapping.mapDependency(newDependency, enforceMappingType);
        }
        mappingsByTarget.remove(EntityHeaderRef.fromOther(dependency)); // nobody will depend on the old one
        addMappingsForTarget(newDependency, mappingsForDependency);

        // outgoing dependencies
        for(MigrationMapping mapping : getMappingsForSource(dependency)) {
            getMappingsForTarget(mapping.getDependency()).remove(mapping);
            mappings.remove(mapping);
        }
        mappingsBySource.remove(EntityHeaderRef.fromOther(dependency));
    }

    /**
     * Checks if the mapping provided as a parameter would introduce a mapping conflict, i.e.
     * if the dependency in the mapping is already required to be mapped in an incompatible way.
     *
     * Any combinations of name-mappings is allowed; if name-mapping is NONE for a dependency,
     * it cannot appear as value-mapped as NONE in one entity, and as OPTIONAL or REQUIRED in another entity.
     *
     * @return the first conflicting mapping found, or null if no conflicting mappings are found.
     */
    private MigrationMapping hasConflictingMapping(MigrationMapping newMapping) throws MigrationApi.MigrationException {

        if (newMapping == null || newMapping.getType().getNameMapping() != NONE)
            return null;

        Set<MigrationMapping> mappings = getMappingsForTarget(newMapping.getDependency());
        if (mappings == null) return null;

        EnumSet<MigrationMappingSelection> conflicting =
            newMapping.getType().getValueMapping() != NONE ?
                EnumSet.of(NONE) : EnumSet.of(MigrationMappingSelection.OPTIONAL, MigrationMappingSelection.REQUIRED);

        for(MigrationMapping m : mappings) {
            if (m.getType().getNameMapping() != NONE)
                continue;
            if (conflicting.contains(m.getType().getValueMapping()))
                return m;
        }

        return null;
    }

    public Set<MigrationMapping> getMappingsForSource(EntityHeaderRef source)  {
        if (mappingsBySource == null) initMappingsCache();

        if ( ! mappingsBySource.containsKey(EntityHeaderRef.fromOther(source)))
            mappingsBySource.put(EntityHeaderRef.fromOther(source), new HashSet<MigrationMapping>());

        return mappingsBySource.get(EntityHeaderRef.fromOther(source));
    }

    public Set<MigrationMapping> getMappingsForTarget(EntityHeaderRef target) {
        if (mappingsByTarget == null) initMappingsCache();

        if ( ! mappingsByTarget.containsKey(EntityHeaderRef.fromOther(target)))
            mappingsByTarget.put(EntityHeaderRef.fromOther(target), new HashSet<MigrationMapping>());

        return mappingsByTarget.get(EntityHeaderRef.fromOther(target));
    }

    private void addMappingsForSource(EntityHeaderRef source, Set<MigrationMapping> mappings) {
        getMappingsForSource(source).addAll(mappings);
    }

    private void addMappingsForTarget(EntityHeaderRef target, Set<MigrationMapping> mappings) {
        getMappingsForTarget(target).addAll(mappings);
    }

    public boolean isUploadedByParent(EntityHeaderRef headerRef) {
        for(MigrationMapping m : getMappingsForTarget(headerRef)) {
            if (m.isUploadedByParent())
                return true;
        }
        return false;
    }
}
