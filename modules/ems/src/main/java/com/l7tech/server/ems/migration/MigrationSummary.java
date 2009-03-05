package com.l7tech.server.ems.migration;

import com.l7tech.server.ems.enterprise.SsgCluster;
import com.l7tech.server.management.migration.bundle.MigratedItem;
import com.l7tech.objectmodel.ExternalEntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.ValueReferenceEntityHeader;

import javax.xml.bind.JAXB;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlElementRef;
import java.util.Collection;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.io.Serializable;

/**
 * @author jbufu
 */
@XmlRootElement
public class MigrationSummary implements Serializable {

    private long timeCreated = System.currentTimeMillis();

    // migration result
    private String sourceClusterGuid;
    private String sourceClusterName;
    private String targetClusterGuid;
    private String targetClusterName;
    private Collection<MigratedItem> migratedItems;
    private boolean dryRun;

    // summary-worth data from the bundle
    private String targetFolderPath;
    private boolean migrateFolders = false;
    private boolean overwrite = false;
    private boolean enableNewServices = false;

    public MigrationSummary() {
    }

    public MigrationSummary(SsgCluster sourceCluster, SsgCluster targetCluster, Collection<MigratedItem> migratedItems, boolean dryRun,
                            String targetFolderPath, boolean migrateFolders, boolean overwrite, boolean enableNewServices) {

        this.sourceClusterGuid = sourceCluster.getGuid();
        this.sourceClusterName = sourceCluster.getName();
        this.targetClusterGuid = targetCluster.getGuid();
        this.targetClusterName = targetCluster.getName();
        this.migratedItems = migratedItems;
        this.dryRun = dryRun;

        this.targetFolderPath = targetFolderPath;
        this.migrateFolders = migrateFolders;
        this.overwrite = overwrite;
        this.enableNewServices = enableNewServices;
    }

    public long getTimeCreated() {
        return timeCreated;
    }

    public void setTimeCreated(long timeCreated) {
        this.timeCreated = timeCreated;
    }

    public String getSourceClusterGuid() {
        return sourceClusterGuid;
    }

    public void setSourceClusterGuid(String sourceClusterGuid) {
        this.sourceClusterGuid = sourceClusterGuid;
    }

    public String getSourceClusterName() {
        return sourceClusterName;
    }

    public void setSourceClusterName(String sourceClusterName) {
        this.sourceClusterName = sourceClusterName;
    }

    public String getTargetClusterGuid() {
        return targetClusterGuid;
    }

    public void setTargetClusterGuid(String targetClusterGuid) {
        this.targetClusterGuid = targetClusterGuid;
    }

    public String getTargetClusterName() {
        return targetClusterName;
    }

    public void setTargetClusterName(String targetClusterName) {
        this.targetClusterName = targetClusterName;
    }

    @XmlElementRef
    public Collection<MigratedItem> getMigratedItems() {
        return migratedItems;
    }

    public void setMigratedItems(Collection<MigratedItem> migratedItems) {
        this.migratedItems = migratedItems;
    }

    public boolean isDryRun() {
        return dryRun;
    }

    public void setDryRun(boolean dryRun) {
        this.dryRun = dryRun;
    }

    public String getTargetFolderPath() {
        return targetFolderPath;
    }

    public void setTargetFolderPath(String targetFolderPath) {
        this.targetFolderPath = targetFolderPath;
    }

    public boolean isMigrateFolders() {
        return migrateFolders;
    }

    public void setMigrateFolders(boolean migrateFolders) {
        this.migrateFolders = migrateFolders;
    }

    public boolean isOverwrite() {
        return overwrite;
    }

    public void setOverwrite(boolean overwrite) {
        this.overwrite = overwrite;
    }

    public boolean isEnableNewServices() {
        return enableNewServices;
    }

    public void setEnableNewServices(boolean enableNewServices) {
        this.enableNewServices = enableNewServices;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(1024);

        // overview
        builder.append( "Migration Options:\n" );
        builder.append( "Folders migrated: " );
        builder.append( toString(migrateFolders) );
        builder.append( "\n" );
        builder.append( "New services enabled: " );
        builder.append( toString(enableNewServices) );
        builder.append( "\n" );
        builder.append( "Existing items overwritten: " );
        builder.append( toString(overwrite) );
        builder.append( "\n" );

        builder.append( " \nMigration Summary:\n" );
        builder.append( "Destination folder: " );
        builder.append( targetFolderPath );
        builder.append( "\n" );
        builder.append( "Services migrated: " );
        builder.append( count(com.l7tech.objectmodel.EntityType.SERVICE) );
        builder.append( "\n" );
        builder.append( "Policy fragments migrated: " );
        builder.append( count(com.l7tech.objectmodel.EntityType.POLICY) );
        builder.append( "\n" );

        // entity details and mappings
        StringBuilder mappingBuilder = new StringBuilder();
        builder.append( " \nMigrated Data:\n" );
        boolean willOverwriteAnything = false;
        MigratedItem.ImportOperation operation;
        if ( migratedItems != null ) {
            for ( MigratedItem item : migratedItems ) {
                operation = item.getOperation();
                if (! operation.modifiesTarget()) {
                    if ( item.getSourceHeader().getType() == EntityType.FOLDER ) {
                        continue; // skip folder mapping since we've already described the destination folder
                    }
                    mappingBuilder.append( item.getSourceHeader().getType().getName().toLowerCase() );
                    mappingBuilder.append( ", " );
                    mappingBuilder.append( item.getSourceHeader().getName()==null ? "" : item.getSourceHeader().getName() );
                    mappingBuilder.append( " (#" );
                    mappingBuilder.append( item.getSourceHeader().getExternalId() );
                    mappingBuilder.append( ") mapped to " );
                    mappingBuilder.append( operation == MigratedItem.ImportOperation.MAP_EXISTING ? "existing " : "");
                    mappingBuilder.append( item.getTargetHeader().getName() );
                    mappingBuilder.append( " (#" );
                    mappingBuilder.append( item.getTargetHeader().getExternalId() );
                    mappingBuilder.append( ")" );
                    mappingBuilder.append( "\n" );
                    continue;
                } else if (operation == MigratedItem.ImportOperation.OVERWRITE) {
                    willOverwriteAnything = true;
                }

                ExternalEntityHeader sourceHeader = item.getSourceHeader();
                if (sourceHeader.getMappedValue() != null) {
                    mappingBuilder.append(sourceHeader.getDisplayNameWithScope()).append(" value mapped to ")
                        .append(sourceHeader.getMappedValue()).append("\n");

                }

                if ( ! (sourceHeader instanceof ValueReferenceEntityHeader) ) {
                    ExternalEntityHeader ih = dryRun ? item.getSourceHeader() : item.getTargetHeader();
                    builder.append(ih.getType().getName().toLowerCase()).append(", ").append(ih.getName() == null ? "" : ih.getName())
                        .append("(#").append(ih.getExternalId()).append(")").append(dryRun ? " will be " : " was ")
                        .append(operation.pastParticiple().toLowerCase()).append("\n");
                }
            }
        }

        if (builder.length() == 0)
            builder.append("None.\n");
        builder.append( "\n" );

        String mappingText = mappingBuilder.toString();
        if ( !mappingText.isEmpty() ) {
            builder.append( " \nMappings:\n" );
            builder.append( mappingText.length() > 0 ? mappingText : "None.\n" );
        }

        if (willOverwriteAnything && dryRun)
            builder.insert(0, "WARNING: Some enties on the target cluster may contain changes that will be overwritten!\n \n");

        return builder.toString();
    }

    private int count( final com.l7tech.objectmodel.EntityType type ) {
        int count = 0;

        for ( MigratedItem item : migratedItems ) {
            if ( item.getSourceHeader().getType() == type && item.getOperation().modifiesTarget()) {
                count++;
            }
        }

        return count;
    }

    private String toString( final boolean value ) {
        return value ? "yes" : "no";
    }

    public String serializeXml() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        JAXB.marshal(this, out);
        return out.toString();
    }

    public static MigrationSummary deserializeXml(String xml) {
        return JAXB.unmarshal(new StringReader(xml), MigrationSummary.class);
    }
}
