package com.l7tech.server.management.migration.bundle;

import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.ExternalEntityHeader;
import com.l7tech.objectmodel.JaxbMapType;
import com.l7tech.objectmodel.migration.MigrationDependency;
import com.l7tech.objectmodel.migration.MigrationMappingSelection;

import javax.xml.bind.annotation.*;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.io.Serializable;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.objectmodel.migration.MigrationMappingSelection.NONE;

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
@XmlType(propOrder={"headers", "dependencies", "mappings", "copies", "targetFolder"})
public class MigrationMetadata implements Serializable {

    private static final Logger logger = Logger.getLogger(MigrationMetadata.class.getName());
    private final static String ROOT_FOLDER_OID = "-5002";
    private final static String ROOT_FOLDER_GOID = "0000000000000000ffffffffffffec76";

    /**
     * Headers for all the items in the Migration Bundle.
     */
    private Set<ExternalEntityHeader> headers = new HashSet<ExternalEntityHeader>();

    private Set<MigrationDependency> dependencies = new HashSet<MigrationDependency>();
    private Map<ExternalEntityHeader, Set<MigrationDependency>> dependenciesBySource;
    private Map<ExternalEntityHeader, Set<MigrationDependency>> dependenciesByTarget;

    private Map<ExternalEntityHeader, ExternalEntityHeader> mappings = new HashMap<ExternalEntityHeader, ExternalEntityHeader>();
    private Map<ExternalEntityHeader, ExternalEntityHeader> copies = new HashMap<ExternalEntityHeader, ExternalEntityHeader>();

    private ExternalEntityHeader targetFolder;
    private boolean migrateFolders = false;
    private boolean overwrite = false;
    private boolean enableNewServices = false;

    @XmlElementRef
    public Set<ExternalEntityHeader> getHeaders() {
        return headers;
    }

    public void setHeaders(Set<ExternalEntityHeader> headers) {
        this.headers = headers;
    }

    public Set<ExternalEntityHeader> getAllHeaders() {
        Set<ExternalEntityHeader> allHeaders = new HashSet<ExternalEntityHeader>();
        allHeaders.addAll(headers);
        for(MigrationDependency dep : dependencies) {
            allHeaders.add(dep.getDependant());
            allHeaders.add(dep.getDependency());
        }
        return allHeaders;
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

    public void removeDependency(MigrationDependency dependency) {
        dependencies.remove(dependency);
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

    public ExternalEntityHeader getTargetFolder() {
        return targetFolder;
    }

    public void setTargetFolder(ExternalEntityHeader targetFolder) {
        this.targetFolder = targetFolder;
    }

    @XmlAttribute
    public boolean isMigrateFolders() {
        return migrateFolders;
    }

    public void setMigrateFolders(boolean migrateFolders) {
        this.migrateFolders = migrateFolders;
    }

    @XmlAttribute
    public boolean isOverwrite() {
        return overwrite;
    }

    public void setOverwrite(boolean overwrite) {
        this.overwrite = overwrite;
    }

    @XmlAttribute
    public boolean isEnableNewServices() {
        return enableNewServices;
    }

    public void setEnableNewServices(boolean enableNewServices) {
        this.enableNewServices = enableNewServices;
    }

    public boolean isMappingRequired(ExternalEntityHeader header) {
        if (MigrationMappingSelection.REQUIRED == header.getValueMapping())
            return true;
        for (MigrationDependency dependency : getDependants(header)) {
            if ( MigrationMappingSelection.REQUIRED == dependency.getMappingType() )
                return true;
        }
        return false;
    }

    public boolean includeInExport(ExternalEntityHeader header) {
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

    public Set<MigrationDependency> getMappableDependencies() {
        Set<MigrationDependency> result = new HashSet<MigrationDependency>();
        for(MigrationDependency dep : dependencies) {
            if (dep.getMappingType() != NONE || dep.getDependency().isValueMappable())
                result.add(dep);
        }
        return result;
    }

    public ExternalEntityHeader getRootFolder() {
        ExternalEntityHeader rootFolder = null;
        for (ExternalEntityHeader header : getAllHeaders()) {
            //Check if it is the root folder, either oid is -5002 or goid is 0000000000000000ffffffffffffec76
            if (header.getType() == EntityType.FOLDER &&  (ROOT_FOLDER_OID.equals(header.getExternalId()) || ROOT_FOLDER_GOID.equals(header.getExternalId()))) {
                if (rootFolder != null)
                    throw new IllegalStateException("More than one root folders found in the bundle.");
                rootFolder = header;
            }
        }
        return rootFolder;
    }
}
