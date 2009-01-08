package com.l7tech.server.management.migration.bundle;

import com.l7tech.objectmodel.*;
import com.l7tech.objectmodel.migration.MigrationDependency;
import com.l7tech.objectmodel.migration.MigrationMappingSelection;
import static com.l7tech.objectmodel.migration.MigrationMappingSelection.NONE;
import com.l7tech.server.management.api.node.MigrationApi;

import javax.xml.bind.annotation.*;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
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
 * @see com.l7tech.objectmodel.migration.MigrationDependency
 * @author jbufu
 */
@XmlRootElement
@XmlType(propOrder={"headers", "dependencies", "mappings", "copies"})
public class MigrationMetadata {

    private static final Logger logger = Logger.getLogger(MigrationMetadata.class.getName());

    /**
     * Headers for all the items in the Migration Bundle.
     */
    private Set<EntityHeader> headers = new HashSet<EntityHeader>();

    private Set<MigrationDependency> dependencies = new HashSet<MigrationDependency>();
    private Map<EntityHeader, Set<MigrationDependency>> dependenciesBySource;
    private Map<EntityHeader, Set<MigrationDependency>> dependenciesByTarget;

    private Map<EntityHeader, EntityHeader> mappings = new HashMap<EntityHeader, EntityHeader>();
    private Map<EntityHeader, EntityHeader> copies = new HashMap<EntityHeader, EntityHeader>();

    @XmlElementRef
    public Set<EntityHeader> getHeaders() {
        return headers;
    }

    public void setHeaders(Set<EntityHeader> headers) {
        this.headers = headers;
    }

    public void addHeader(EntityHeader header) {
        headers.add(header);
    }

    public void removeHeader(EntityHeader header) {
        headers.remove(header);
    }

    public boolean hasHeader(EntityHeader header) {
        return headers.contains(header);
    }

    @XmlElementWrapper(name="dependencies")
    @XmlElementRef
    public Set<MigrationDependency> getDependencies() {
        return dependencies;
    }

    public void setDependencies(Set<MigrationDependency> dependencies) {
        this.dependencies = dependencies;
    }

    public void addDependency(MigrationDependency dependency) {
        logger.log(Level.FINEST, "Adding dependency: {0}", dependency);
        if (dependency == null) return;
        dependencies.add(dependency);
        getDependencies(dependency.getDependant()).add(dependency);
        getDependants(dependency.getDependency()).add(dependency);
    }

    @XmlJavaTypeAdapter(JaxbMapType.JaxbMapTypeAdapter.class)
    public Map<EntityHeader, EntityHeader> getMappings() {
        return mappings;
    }

    public void setMappings(Map<EntityHeader, EntityHeader> mappings) {
        this.mappings = mappings;
    }

    public Collection<EntityHeader> getMappedHeaders() {
        return mappings.values();
    }

    public EntityHeader getMapping(EntityHeader header) {
        return mappings.get(header);
    }

    public boolean isMapped(EntityHeader header) {
        return mappings.containsKey(header);
    }

    public void addMappingOrCopy(EntityHeader source, EntityHeader target, boolean isCopy) {
        if (isCopy) {
            copies.put(source, target);
            mappings.remove(source);
        } else {
            mappings.put(source, target);
            copies.remove(source);
        }
    }

    public EntityHeader getCopiedOrMapped(EntityHeader header) {
        if (wasCopied(header))
            return getCopied(header);
        else if (isMapped(header))
            return getMapping(header);
        else
            return null;
    }

    @XmlJavaTypeAdapter(JaxbMapType.JaxbMapTypeAdapter.class)
    public Map<EntityHeader, EntityHeader> getCopies() {
        return copies;
    }

    public void setCopies(Map<EntityHeader, EntityHeader> copies) {
        this.copies = copies;
    }

    public Collection<EntityHeader> getCopiedHeaders() {
        return copies.values();
    }

    public EntityHeader getCopied(EntityHeader header) {
        return copies.get(header);
    }

    public boolean wasCopied(EntityHeader header) {
        return copies.containsKey(header);
    }

    private void initDependenciesCache()  {
        dependenciesBySource = new HashMap<EntityHeader, Set<MigrationDependency>>();
        dependenciesByTarget = new HashMap<EntityHeader, Set<MigrationDependency>>();
        for (MigrationDependency dependency : this.dependencies) {
            addDependencies(dependency.getDependant(), Collections.singleton(dependency));
            addDependants(dependency.getDependency(), Collections.singleton(dependency));
        }
        logger.log(Level.FINEST, "Dependencies cache initialized.");
    }

    private void addDependencies(EntityHeader source, Set<MigrationDependency> deps) {
        getDependencies(source).addAll(deps);
    }

    private void addDependants(EntityHeader target, Set<MigrationDependency> deps) {
        getDependants(target).addAll(deps);
    }

    public Set<MigrationDependency> getDependencies(EntityHeader source)  {
        if (dependenciesBySource == null) initDependenciesCache();

        if ( ! dependenciesBySource.containsKey(source))
            dependenciesBySource.put(source, new HashSet<MigrationDependency>());

        return dependenciesBySource.get(source);
    }

    public Set<MigrationDependency> getDependants(EntityHeader target) {
        if (dependenciesByTarget == null) initDependenciesCache();

        if ( ! dependenciesByTarget.containsKey(target))
            dependenciesByTarget.put(target, new HashSet<MigrationDependency>());

        return dependenciesByTarget.get(target);
    }

    public boolean isMappingRequired(EntityHeader header) throws MigrationApi.MigrationException {
        for (MigrationDependency dependency : getDependants(header)) {
            if ( dependency.getMappingType().getNameMapping() == MigrationMappingSelection.REQUIRED ||
                 dependency.getMappingType().getValueMapping() == MigrationMappingSelection.REQUIRED)
                return true;
        }
        return false;
    }

    public boolean includeInExport(EntityHeader header) throws MigrationApi.MigrationException {
        if (isMappingRequired(header)) {
            return false;
        } else {
            Set<MigrationDependency> deps = getDependants(header);

            if (deps == null || deps.size() == 0) // top-level item
                return true;

            for (MigrationDependency dependency : deps) {
                if (dependency.isExport())
                    return true;
            }

            return false;
        }
    }

    public Set<EntityHeader> getMappableDependencies() {
        Set<EntityHeader> result = new HashSet<EntityHeader>();
        for(MigrationDependency dep : dependencies) {
            if (dep.getMappingType().getNameMapping() != NONE || dep.getMappingType().getValueMapping() != NONE)
                result.add(dep.getDependency());
        }
        return result;
    }
}
