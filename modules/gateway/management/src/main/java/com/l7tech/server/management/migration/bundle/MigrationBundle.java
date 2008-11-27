package com.l7tech.server.management.migration.bundle;

import com.l7tech.objectmodel.*;
import com.l7tech.objectmodel.migration.MigrationMapping;
import com.l7tech.objectmodel.migration.MigrationMappingType;
import com.l7tech.objectmodel.migration.MigrationException;
import com.l7tech.objectmodel.migration.MigrationMappingSelection;

import javax.xml.bind.annotation.*;
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
 *   <li>Migration Manifest (set of all the headers)</li>
 *   <li>Dependencies</li>
 *   </ul>
 * <li>Exported Items (optional)</li>
 * </ul>
 *
 * @see com.l7tech.server.management.migration.bundle.MigrationMetadata
 * @see com.l7tech.server.management.migration.bundle.ExportedItem
 * @author jbufu
 */
@XmlRootElement
@XmlType(propOrder={"metadata", "exportedItems"})
public class MigrationBundle {

    private MigrationMetadata metadata;
    
    private Set<ExportedItem> exportedItems = new HashSet<ExportedItem>();
    private Map<EntityHeaderRef,ExportedItem> exportedItemsMap = null;

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


    @XmlElementWrapper(name="values")
    @XmlElementRef
    public Collection<ExportedItem> getExportedItems() {
        if (exportedItemsMap != null)
            // immutable, throws unsupported operation on modification attempts
            return exportedItemsMap.values();
        else
            // should be used by JAXB unmarshalling only
            return exportedItems;
    }

    public void setExportedItems(Collection<ExportedItem> exportedItems) {
        // state reset
        this.exportedItemsMap = null;
        this.exportedItems = new HashSet<ExportedItem>(exportedItems);
    }

    private void initItemsCache() {
        // state change: JAXB init over; switching to the internal map representation
        HashMap<EntityHeaderRef,ExportedItem> itemsMap = new HashMap<EntityHeaderRef, ExportedItem>();
        for(ExportedItem item : exportedItems) {
            itemsMap.put(item.getHeaderRef(), item);
        }
        this.exportedItemsMap = itemsMap;
        this.exportedItems = null;
    }

    private Map<EntityHeaderRef, ExportedItem> getItemsMap() {
        if (exportedItemsMap == null) initItemsCache();
        return exportedItemsMap;
    }

    public void addExportedItem(ExportedItem item) {
        getItemsMap().put(item.getHeaderRef(), item);
    }

    private void addExporteditems(Set<ExportedItem> exportedItems) {
        for (ExportedItem item : exportedItems) {
            addExportedItem(item);
        }
    }

    // --- mapping operations ---

    public void mapValue(EntityHeaderRef dependency, Entity newValue) throws MigrationException {

        Set<MigrationMapping> mappingsForDependency = metadata.getMappingsForTarget(dependency);

        if (dependency == null || newValue == null || mappingsForDependency == null)
            return;

        // check for conflicting mapping types for this value-mapped dependency
        for (MigrationMapping mapping : mappingsForDependency) {
            if (mapping.getType() == MigrationMappingType.BOTH_NONE)
                throw new MigrationException("Cannot map value for dependency; mapping set to NONE for: " + mapping);
        }

        // update mapping
        ExportedItem item = getItemsMap().get(dependency);
        if (item == null) {
            item = new ExportedItem(dependency, null);
            getItemsMap().put(dependency, item);
        }
        item.setMappedValue(newValue);
    }

    public Set<MigrationMapping> getUnresolvedMappings() throws MigrationException {
        Set<MigrationMapping> result = new HashSet<MigrationMapping>();
        Map<EntityHeaderRef,ExportedItem> itemsMap = getItemsMap();
        for(MigrationMapping m : metadata.getMappings()) {

            MigrationMappingType type = m.getType();
            boolean hasSourceValue = itemsMap.get(m.getTarget()) != null && itemsMap.get(m.getTarget()).getSourceValue() != null;
            boolean hasMappedValue = itemsMap.get(m.getTarget()) != null && itemsMap.get(m.getTarget()).getMappedValue() != null;

            switch (type.getNameMapping()) {
                case NONE:
                    if ( ! hasSourceValue )
                        throw new MigrationException("Source value required but not present in the bundle for: " + m.getTarget());
                    break;
                case OPTIONAL:
                    if ( ! hasSourceValue && m.getMappedTarget() == null)
                        result.add(m);
                    break;
                case REQUIRED:
                    if (m.getMappedTarget() == null)
                        result.add(m);
                    break;
                default:
                    throw new IllegalStateException("Unknow mapping type: " + type); // should not happen
            }

            switch (type.getValueMapping()) {
                case NONE: // requirement only if there's no name-mapping
                    if ( ! hasSourceValue && type.getNameMapping() != MigrationMappingSelection.NONE && m.getMappedTarget() == null)
                        throw new MigrationException("Source value required but not present in the bundle for: " + m.getTarget());
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

    public boolean hasItem(EntityHeaderRef headerRef) {
        return getItemsMap().containsKey(headerRef);
    }

    public ExportedItem getExportedItem(EntityHeaderRef headerRef) {
        return getItemsMap().get(headerRef);
    }
}
