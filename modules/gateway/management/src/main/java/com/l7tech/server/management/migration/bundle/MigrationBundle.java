package com.l7tech.server.management.migration.bundle;

import com.l7tech.objectmodel.*;
import com.l7tech.objectmodel.migration.MigrationMapping;
import com.l7tech.objectmodel.migration.MigrationMappingType;
import com.l7tech.objectmodel.migration.MigrationException;
import com.l7tech.objectmodel.migration.MigrationMappingSelection;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.ServiceDocument;

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
 *   <li>Set of all the headers</li>
 *   <li>Migration Mappings</li>
 *   </ul>
 * <li>Exported Items (entity values)</li>
 * </ul>
 *
 * @see com.l7tech.server.management.migration.bundle.MigrationMetadata
 * @see com.l7tech.objectmodel.migration.MigrationMapping
 * @see com.l7tech.server.management.migration.bundle.ExportedItem
 * @author jbufu
 */
@XmlRootElement
@XmlType(propOrder={"metadata", "exportedItems"})
@XmlSeeAlso({PublishedService.class, ServiceDocument.class})
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

    public boolean hasItem(EntityHeaderRef headerRef) {
        return getItemsMap().containsKey(EntityHeaderRef.fromOther(headerRef));
    }

    public ExportedItem getExportedItem(EntityHeaderRef headerRef) {
        return getItemsMap().get(EntityHeaderRef.fromOther(headerRef));
    }

    public void addExportedItem(ExportedItem item) {
        getItemsMap().put(EntityHeaderRef.fromOther(item.getHeaderRef()), item);
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
        ExportedItem item = getExportedItem(dependency);
        if (item == null) {
            item = new ExportedItem(dependency, null);
            addExportedItem(item);
        }
        item.setMappedValue(newValue);
    }

    public Set<MigrationMapping> getUnresolvedMappings() throws MigrationException {
        Set<MigrationMapping> result = new HashSet<MigrationMapping>();
        for(MigrationMapping m : metadata.getMappings()) {
            MigrationMappingType type = m.getType();
            EntityHeaderRef targetHeaderRef = m.getDependency();
            ExportedItem targetItem = getExportedItem(targetHeaderRef);
            boolean hasSourceValue = targetItem != null && targetItem.getSourceValue() != null;
            boolean hasMappedValue = targetItem != null && targetItem.getMappedValue() != null;

            switch (type.getNameMapping()) {
                case NONE:
                    if ( ! hasSourceValue )
                        throw new MigrationException("Source value required but not present in the bundle for: " + targetHeaderRef);
                    break;
                case OPTIONAL:
                    if ( ! hasSourceValue && ! m.isMappedDependency())
                        result.add(m);
                    break;
                case REQUIRED:
                    if (! m.isMappedDependency())
                        result.add(m);
                    break;
                default:
                    throw new IllegalStateException("Unknow mapping type: " + type); // should not happen
            }

            switch (type.getValueMapping()) {
                case NONE: // requirement only if there's no name-mapping
                    if ( ! hasSourceValue && type.getNameMapping() != MigrationMappingSelection.NONE && ! m.isMappedDependency())
                        throw new MigrationException("Source value required but not present in the bundle for: " + targetHeaderRef);
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
}
