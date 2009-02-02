package com.l7tech.server.management.migration.bundle;

import com.l7tech.objectmodel.*;
import com.l7tech.objectmodel.migration.MigrationDependency;
import com.l7tech.objectmodel.migration.MigrationMappingType;
import com.l7tech.objectmodel.migration.MigrationMappingSelection;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.ServiceDocument;
import com.l7tech.server.management.api.node.MigrationApi;

import javax.xml.bind.annotation.*;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.*;

/**
 * A MigrationBundle contains all the relevant data exported from a source SSG that is needed to migrate functionality
 * (services and policies, primarily) to a different SSG.
 *
 * Since user intervention is typically required to complete migration of functionality, this data abstraction is
 * intended to enable a migration agent (such as the Enterprise Manager) to assist the user in resolving conflicts
 * and mapping of items from the source to the destination SSG.
 *
 * The logical structure of the bundle is:
 * <ul>
 * <li>Migration Metadata</li>
 *   <ul>
 *   <li>Set of all the headers</li>
 *   <li>Migration Mappings</li>
 *   </ul>
 * <li>Exported Items (entity values)</li>
 * </ul>
 *
 * @see com.l7tech.server.management.migration.bundle.MigrationMetadata
 * @see com.l7tech.objectmodel.migration.MigrationDependency
 * @see com.l7tech.server.management.migration.bundle.ExportedItem
 * @author jbufu
 */
@XmlRootElement
@XmlType(propOrder={"metadata", "exportedItems"})
@XmlSeeAlso({PublishedService.class, ServiceDocument.class, ExportedItem.class})
public class MigrationBundle {

    private MigrationMetadata metadata;
    
    private Map<ExternalEntityHeader,ExportedItem> exportedItems = new HashMap<ExternalEntityHeader, ExportedItem>();

    protected MigrationBundle() {}

    public MigrationBundle(MigrationMetadata metadata) {
        this.metadata = metadata;
    }

    public MigrationMetadata getMetadata() {
        return metadata;
    }

    public void setMetadata(MigrationMetadata metadata) {
        this.metadata = metadata;
    }

    @XmlJavaTypeAdapter(JaxbMapType.JaxbMapTypeAdapter.class)
    public Map<ExternalEntityHeader, ExportedItem> getExportedItems() {
        return exportedItems;
    }

    public void setExportedItems(Map<ExternalEntityHeader, ExportedItem> exportedItems) {
        this.exportedItems = exportedItems;
    }


    public boolean hasItem(ExternalEntityHeader header) {
        return exportedItems.containsKey(header);
    }

    public ExportedItem getExportedItem(ExternalEntityHeader header) {
        return exportedItems.get(header);
    }

    public void addExportedItem(ExportedItem item) {
        exportedItems.put(item.getHeader(), item);
    }

    // --- mapping operations ---

    public void mapValue(ExternalEntityHeader dependency, Entity newValue) throws MigrationApi.MigrationException {

        Set<MigrationDependency> dependants = metadata.getDependants(dependency);

        if (dependency == null || newValue == null || dependants == null)
            return;

        // check for conflicting mapping types for this value-mapped dependency
        for (MigrationDependency dep : dependants) {
            if (dep.getMappingType() == MigrationMappingType.BOTH_NONE)
                throw new MigrationApi.MigrationException("Cannot map value for dependency; mapping set to NONE for: " + dep);
        }

        // update mapping
        ExportedItem item = getExportedItem(dependency);
        if (item == null) {
            item = new ExportedItem(dependency, null);
            addExportedItem(item);
        }
        item.setMappedValue(newValue);
    }

    public Set<MigrationDependency> getUnresolvedMappings() throws MigrationApi.MigrationException {
        Set<MigrationDependency> result = new HashSet<MigrationDependency>();
        for(MigrationDependency m : metadata.getDependencies()) {
            MigrationMappingType type = m.getMappingType();
            ExternalEntityHeader targetHeaderRef = m.getDependency();
            ExportedItem targetItem = getExportedItem(targetHeaderRef);
            boolean hasSourceValue = targetItem != null && targetItem.getSourceValue() != null;
            boolean hasMappedValue = targetItem != null && targetItem.getMappedValue() != null;

            switch (type.getNameMapping()) {
                case NONE:
                    if ( ! hasSourceValue )
                        throw new MigrationApi.MigrationException("Source value required but not present in the bundle for: " + targetHeaderRef);
                    break;
                case OPTIONAL:
                    if ( ! hasSourceValue && ! metadata.isMapped(m.getDependant()))
                        result.add(m);
                    break;
                case REQUIRED:
                    if (! metadata.isMapped(m.getDependant()))
                        result.add(m);
                    break;
                default:
                    throw new IllegalStateException("Unknow mapping type: " + type); // should not happen
            }

            switch (type.getValueMapping()) {
                case NONE: // requirement only if there's no name-mapping
                    if ( ! hasSourceValue && type.getNameMapping() != MigrationMappingSelection.NONE && ! metadata.isMapped(m.getDependant()))
                        throw new MigrationApi.MigrationException("Source value required but not present in the bundle for: " + targetHeaderRef);
                    break;
                case OPTIONAL:
                    if ( ! hasSourceValue && ! hasMappedValue)
                        result.add(m);
                    break;
                case REQUIRED:
                    if ( ! hasMappedValue )
                        result.add(m);
                    break;
                default:
                    throw new IllegalStateException("Unknow mapping type: " + type); // should not happen
            }
        }

        return result;
    }

    public Map<ExternalEntityHeader, Entity> getExportedEntities() {
        Map<ExternalEntityHeader, Entity> result = new HashMap<ExternalEntityHeader, Entity>();
        for (Map.Entry<ExternalEntityHeader, ExportedItem> itemEntry : getExportedItems().entrySet()) {
            result.put(itemEntry.getKey(), itemEntry.getValue().getValue());
        }
        return result;
    }

    public Entity getExportedEntity(ExternalEntityHeader header) {
        return hasItem(header) ? getExportedItem(header).getValue() : null;
    }
}
