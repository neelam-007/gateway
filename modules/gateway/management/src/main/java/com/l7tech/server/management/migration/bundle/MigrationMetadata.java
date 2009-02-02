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
    private Set<ExternalEntityHeader> headers = new HashSet<ExternalEntityHeader>();

    private Set<MigrationDependency> dependencies = new HashSet<MigrationDependency>();
    private Map<ExternalEntityHeader, Set<MigrationDependency>> dependenciesBySource;
    private Map<ExternalEntityHeader, Set<MigrationDependency>> dependenciesByTarget;

    private Map<ExternalEntityHeader, ExternalEntityHeader> mappings = new HashMap<ExternalEntityHeader, ExternalEntityHeader>();
    private Map<ExternalEntityHeader, ExternalEntityHeader> copies = new HashMap<ExternalEntityHeader, ExternalEntityHeader>();

    @XmlElementRef
    public Set<ExternalEntityHeader> getHeaders() {
        return headers;
    }

    public void setHeaders(Set<ExternalEntityHeader> headers) {
        this.headers = headers;
    }

    public void addHeader(ExternalEntityHeader header) {
        headers.add(header);
    }

    public void removeHeader(ExternalEntityHeader header) {
        headers.remove(header);
    }

    public boolean hasHeader(ExternalEntityHeader header) {
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
    public Map<ExternalEntityHeader, ExternalEntityHeader> getMappings() {
        return mappings;
    }

    public void setMappings(Map<ExternalEntityHeader, ExternalEntityHeader> mappings) {
        this.mappings = mappings;
    }

    public Collection<ExternalEntityHeader> getMappedHeaders() {
        return mappings.values();
    }

    public ExternalEntityHeader getMapping(ExternalEntityHeader header) {
        return mappings.get(header);
    }

    public boolean isMapped(ExternalEntityHeader header) {
        return mappings.containsKey(header);
    }

    public void addMappingOrCopy(ExternalEntityHeader source, ExternalEntityHeader target, boolean isCopy) {
        if (isCopy) {
            copies.put(source, target);
            mappings.remove(source);
        } else {
            mappings.put(source, target);
            copies.remove(source);
        }
    }

    public ExternalEntityHeader getCopiedOrMapped(ExternalEntityHeader header) {
        if (wasCopied(header))
            return getCopied(header);
        else if (isMapped(header))
            return getMapping(header);
        else
            return null;
    }

    @XmlJavaTypeAdapter(JaxbMapType.JaxbMapTypeAdapter.class)
    public Map<ExternalEntityHeader, ExternalEntityHeader> getCopies() {
        return copies;
    }

    public void setCopies(Map<ExternalEntityHeader, ExternalEntityHeader> copies) {
        this.copies = copies;
    }

    public Collection<ExternalEntityHeader> getCopiedHeaders() {
        return copies.values();
    }

    public ExternalEntityHeader getCopied(ExternalEntityHeader header) {
        return copies.get(header);
    }

    public boolean wasCopied(ExternalEntityHeader header) {
        return copies.containsKey(header);
    }

    private void initDependenciesCache()  {
        dependenciesBySource = new HashMap<ExternalEntityHeader, Set<MigrationDependency>>();
        dependenciesByTarget = new HashMap<ExternalEntityHeader, Set<MigrationDependency>>();
        for (MigrationDependency dependency : this.dependencies) {
            addDependencies(dependency.getDependant(), Collections.singleton(dependency));
            addDependants(dependency.getDependency(), Collections.singleton(dependency));
        }
        logger.log(Level.FINEST, "Dependencies cache initialized.");
    }

    private void addDependencies(ExternalEntityHeader source, Set<MigrationDependency> deps) {
        getDependencies(source).addAll(deps);
    }

    private void addDependants(ExternalEntityHeader target, Set<MigrationDependency> deps) {
        getDependants(target).addAll(deps);
    }

    public Set<MigrationDependency> getDependencies(ExternalEntityHeader source)  {
        if (dependenciesBySource == null) initDependenciesCache();

        if ( ! dependenciesBySource.containsKey(source))
            dependenciesBySource.put(source, new HashSet<MigrationDependency>());

        return dependenciesBySource.get(source);
    }

    public Set<MigrationDependency> getDependants(ExternalEntityHeader target) {
        if (dependenciesByTarget == null) initDependenciesCache();

        if ( ! dependenciesByTarget.containsKey(target))
            dependenciesByTarget.put(target, new HashSet<MigrationDependency>());

        return dependenciesByTarget.get(target);
    }

    public boolean isMappingRequired(ExternalEntityHeader header) throws MigrationApi.MigrationException {
        for (MigrationDependency dependency : getDependants(header)) {
            if ( dependency.getMappingType().getNameMapping() == MigrationMappingSelection.REQUIRED ||
                 dependency.getMappingType().getValueMapping() == MigrationMappingSelection.REQUIRED)
                return true;
        }
        return false;
    }

    public boolean includeInExport(ExternalEntityHeader header) throws MigrationApi.MigrationException {
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

    public Set<ExternalEntityHeader> getMappableDependencies() {
        Set<ExternalEntityHeader> result = new HashSet<ExternalEntityHeader>();
        for(MigrationDependency dep : dependencies) {
            if (dep.getMappingType().getNameMapping() != NONE || dep.getMappingType().getValueMapping() != NONE)
                result.add(dep.getDependency());
        }
        return result;
    }
}
