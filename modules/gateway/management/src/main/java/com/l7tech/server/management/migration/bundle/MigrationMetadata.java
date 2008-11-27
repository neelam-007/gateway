package com.l7tech.server.management.migration.bundle;

import com.l7tech.objectmodel.*;
import com.l7tech.objectmodel.migration.MigrationMapping;
import com.l7tech.objectmodel.migration.MigrationMappingSelection;
import com.l7tech.objectmodel.migration.MigrationException;
import com.l7tech.objectmodel.migration.MigrationMappingType;

import javax.xml.bind.annotation.*;
import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Summary of a Migration Bundle, consisting of:
 * <ul>
 * <li>Manifest: a set of headers for all exported entities in the bundle, including dependencies.</li>
 * <li>Dependencies: map of dependency headers, for easier manipulation of the bundle.</li>
 * <li></li>
 * </ul>
 *
 * Not thread-safe. 
 *
 * @author jbufu
 */
@XmlRootElement
@XmlType(propOrder={"headers", "mappings"})
public class MigrationMetadata {

    private static final Logger logger = Logger.getLogger(MigrationMetadata.class.getName());

    /**
     * Headers for all the items in the Migration Bundle. Used only by/during JAXB marshalling / unmarshalling operations.
     */
    private Set<EntityHeader> headers = new HashSet<EntityHeader>();

    /**
     * Headers for all the items in the Migration Bundle. Used by migration business logic.
     */
    private Map<EntityHeaderRef, EntityHeader> headersMap = null;

    private Set<MigrationMapping> mappings = new HashSet<MigrationMapping>();
    // easy access to dependencies / dependents
    private Map<EntityHeaderRef, Set<MigrationMapping>> mappingsBySource = new HashMap<EntityHeaderRef, Set<MigrationMapping>>();
    private Map<EntityHeaderRef, Set<MigrationMapping>> mappingsByTarget = new HashMap<EntityHeaderRef, Set<MigrationMapping>>();

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

    private void initHeadersCache() {
        // state change: JAXB init over; switching to the internal map representation
        HashMap<EntityHeaderRef, EntityHeader> headersMap = new HashMap<EntityHeaderRef, EntityHeader>();
        for(EntityHeader header : headers) {
            headersMap.put(EntityHeaderRef.fromOther(header), header);
        }
        this.headersMap = headersMap;
        this.headers = null;
    }

    private Map<EntityHeaderRef, EntityHeader> getHeadersMap() {
        if (headersMap == null) initHeadersCache();
        return headersMap;
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

    public void removeHeader(EntityHeaderRef headerRef) {
        getHeadersMap().remove(EntityHeaderRef.fromOther(headerRef));
    }

    public boolean isMappingRequired(EntityHeaderRef headerRef) throws MigrationException {
        for (MigrationMapping mapping : getMappingsForTarget(EntityHeaderRef.fromOther(headerRef))) {
            if ( mapping.getType().getNameMapping() == MigrationMappingSelection.REQUIRED ||
                 mapping.getType().getValueMapping() == MigrationMappingSelection.REQUIRED)
                return true;
        }
        return false;
    }

    // --- mapping / dependencies operations ---

    private void initMappingsCache() throws MigrationException {
        mappingsBySource = new HashMap<EntityHeaderRef, Set<MigrationMapping>>();
        mappingsByTarget = new HashMap<EntityHeaderRef, Set<MigrationMapping>>();
        addMappings(this.mappings);
    }

    @XmlElementWrapper(name="mappings")
    @XmlElementRef
    public Set<MigrationMapping> getMappings() {
        return mappings;
    }

    public void setMappings(Set<MigrationMapping> mappings) throws MigrationException {
        // state reset: switching to "map cache uninitialized"
        this.mappingsBySource = null;
        this.mappingsByTarget = null;
        this.mappings = mappings;
    }

    public Set<EntityHeaderRef> getMappableDependencies() {
        Set<EntityHeaderRef> result = new HashSet<EntityHeaderRef>();
        for(MigrationMapping m : mappings) {
            if (m.getType() != MigrationMappingType.BOTH_NONE)
                result.add(m.getTarget());
        }
        return result;
    }

    public void addMappings(Set<MigrationMapping> mappings) throws MigrationException {
        for (MigrationMapping mapping : mappings) {
            addMapping(mapping);
        }
    }

    public void addMapping(MigrationMapping mapping) throws MigrationException {

        if (mapping == null) return;

        MigrationMapping conflicting = hasConflictingMapping(mapping);

        if (conflicting != null) {
            if (mapping.getType().getValueMapping() != MigrationMappingSelection.OPTIONAL) {
                throw new MigrationException("New mapping: " + mapping + " conflicts with: " + conflicting);
            } else {
                logger.log(Level.WARNING, "New mapping would create a conflict; switching value-mapping from OPTIONAL to REQUIRED for: " + mapping);
                mapping.getType().setValueMapping(MigrationMappingSelection.REQUIRED);
            }
        }

        mappings.add(mapping);
        addMappingsForSource(mapping.getSource(), Collections.singleton(mapping));
        addMappingsForTarget(mapping.getTarget(), Collections.singleton(mapping));
    }

    public void mapName(EntityHeaderRef dependency, EntityHeaderRef newDependency) throws MigrationException {

        if (dependency == null || newDependency == null)
            return;

        // incoming dependencies
        Set<MigrationMapping> mappingsForDependency = getMappingsForTarget(dependency);
        for (MigrationMapping mapping : mappingsForDependency) {
            // todo: one-to-many mappings: decide how/when to apply this mapping (vs a presious / individual selection)
            mapping.setMappedTarget(newDependency);
        }
        addMappingsForTarget(newDependency, mappingsForDependency);
        mappingsByTarget.remove(dependency); // nobody will depend on the old one

        // outgoing dependencies
        for(MigrationMapping mapping : getMappingsForSource(dependency)) {
            getMappingsForTarget(mapping.getTarget()).remove(mapping);
            mappings.remove(mapping);
        }
        mappingsBySource.remove(dependency);

        removeHeader(dependency);
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
    private MigrationMapping hasConflictingMapping(MigrationMapping newMapping) throws MigrationException {

        if (newMapping == null || newMapping.getType().getNameMapping() != MigrationMappingSelection.NONE)
            return null;

        Set<MigrationMapping> mappings = getMappingsForTarget(newMapping.getTarget());
        if (mappings == null) return null;

        EnumSet<MigrationMappingSelection> conflicting =
            newMapping.getType().getValueMapping() != MigrationMappingSelection.NONE ?
                EnumSet.of(MigrationMappingSelection.NONE) : EnumSet.of(MigrationMappingSelection.OPTIONAL, MigrationMappingSelection.REQUIRED);

        for(MigrationMapping m : mappings) {
            if (m.getType().getNameMapping() != MigrationMappingSelection.NONE)
                continue;
            if (conflicting.contains(m.getType().getValueMapping()))
                return m;
        }

        return null;
    }

    public Set<MigrationMapping> getMappingsForSource(EntityHeaderRef source) throws MigrationException {
        if (mappingsBySource == null) initMappingsCache();

        if ( ! mappingsBySource.containsKey(source))
            mappingsBySource.put(source, new HashSet<MigrationMapping>());

        return mappingsBySource.get(source);
    }

    public Set<MigrationMapping> getMappingsForTarget(EntityHeaderRef target) throws MigrationException {
        if (mappingsByTarget == null) initMappingsCache();

        if ( ! mappingsByTarget.containsKey(target))
            mappingsByTarget.put(target, new HashSet<MigrationMapping>());

        return mappingsByTarget.get(target);
    }

    public void addMappingsForSource(EntityHeaderRef source, Set<MigrationMapping> mappings) throws MigrationException {
        getMappingsForSource(source).addAll(mappings);
    }

    public void addMappingsForTarget(EntityHeaderRef target, Set<MigrationMapping> mappings) throws MigrationException {
        getMappingsForTarget(target).addAll(mappings);
    }
}
