package com.l7tech.server.management.migration.bundle;

import com.l7tech.objectmodel.*;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.ServiceDocument;
import com.l7tech.gateway.common.service.PublishedServiceAlias;
import com.l7tech.policy.PolicyAlias;

import javax.xml.bind.annotation.*;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import javax.xml.bind.JAXB;
import java.util.*;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.io.Serializable;

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
@XmlSeeAlso({PublishedService.class, PublishedServiceAlias.class, PolicyAlias.class, ServiceDocument.class, ExportedItem.class})
public class MigrationBundle implements Serializable {

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

    public String serializeXml() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        JAXB.marshal(this, out);
        return out.toString();
    }

    public static MigrationBundle deserializeXml(String xml) {
        return JAXB.unmarshal(new StringReader(xml), MigrationBundle.class);
    }

}
