package com.l7tech.server.ems.migration;

import com.l7tech.objectmodel.imp.NamedEntityImp;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.server.management.migration.bundle.MigrationBundle;
import com.l7tech.server.management.migration.bundle.MigratedItem;
import com.l7tech.server.ems.enterprise.SsgCluster;
import com.l7tech.identity.User;

import javax.persistence.*;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.JAXB;

import org.hibernate.annotations.Proxy;
import org.hibernate.annotations.Type;

import java.io.StringReader;
import java.io.StringWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.ArrayList;

/**
 * This entity class stores the information of a migration such as name, id, time created,
 * source cluster, and destination cluster.
 *
 * @Copyright: Layer 7 Tech. Inc.
 * @Author: ghuang
 * @Date: Nov 19, 2008
 */
@Entity
@Proxy(lazy=false)
@Table(name="migration")
@XmlRootElement
public class MigrationRecord extends NamedEntityImp {

    private long timeCreated;
    private long provider;
    private String userId;
    private SsgCluster sourceCluster;
    private SsgCluster targetCluster;
    private String summaryXml;
    private String bundleXml;
    private int size;

    private MigrationSummary summary;
    private MigrationBundle bundle;

    public MigrationRecord() {
    }

    public MigrationRecord( final String name,
                            final User user,
                            final SsgCluster sourceCluster,
                            final SsgCluster targetCluster,
                            final MigrationSummary summary,
                            final MigrationBundle bundle) {
        this._name = name==null ? "" : name;
        this.provider = user.getProviderId();
        this.userId = user.getId();
        this.sourceCluster = sourceCluster;
        this.targetCluster = targetCluster;
        this.timeCreated = summary.getTimeCreated();
        this.summary = summary;
        this.bundle = bundle;
        calculateSize();
    }

    @Column(name="time_created", nullable=false, updatable = false)
    public long getTimeCreated() {
        return timeCreated;
    }

    public void setTimeCreated(long timeCreated) {
        this.timeCreated = timeCreated;
    }

    @Column(name="provider", nullable=false)
    public long getProvider() {
        return provider;
    }

    public void setProvider( final long providerId ) {
        this.provider = providerId;
    }

    @Column(name="user_id", nullable=false, length=255)
    public String getUserId() {
        return userId;
    }

    public void setUserId( final String userId ) {
        this.userId = userId;
    }

    @XmlTransient
    @ManyToOne(optional=true)
    @JoinColumn(name="target_cluster_oid", nullable=true)
    public SsgCluster getTargetCluster() {
        return targetCluster;
    }

    public void setTargetCluster(SsgCluster targetCluster) {
        this.targetCluster = targetCluster;
    }

    @XmlTransient
    @ManyToOne(optional=false)
    @JoinColumn(name="source_cluster_oid", nullable=false)
    public SsgCluster getSourceCluster() {
        return sourceCluster;
    }

    public void setSourceCluster(SsgCluster sourceCluster) {
        this.sourceCluster = sourceCluster;
    }

    @Column(name="summary_xml", length = 1024*1024)
    @Lob
    public synchronized String getSummaryXml() {
        if (summaryXml == null && summary != null)
            summaryXml = summary.serializeXml();

        return summaryXml;
    }

    public synchronized void setSummaryXml(String summaryXml) {
        this.summaryXml = summaryXml;
        summary = MigrationSummary.deserializeXml(summaryXml);
    }

    @Basic(fetch=FetchType.LAZY)
    @Column(name="bundle_zipxml", length=Integer.MAX_VALUE)
    @Type(type="com.l7tech.server.util.CompressedStringType")
    @Lob
    public synchronized String getBundleXml() {
        if (bundleXml == null && bundle != null)
            bundleXml = bundle.serializeXml();

        return bundleXml;
    }

    public synchronized void setBundleXml(String bundleXml) {
        this.bundleXml = bundleXml;
        bundle = bundleXml == null ? null : MigrationBundle.deserializeXml(bundleXml);
    }

    @XmlTransient
    @Column(name="size")
    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    // - Convenience accessors

    @Transient
    public MigrationSummary getMigrationSummary() {
        return summary;
    }

    @Transient
    public String getSourceClusterName() {
        return summary == null ? null : summary.getSourceClusterName();
    }

    @Transient
    public String getSourceClusterGuid() {
        return summary == null ? null : summary.getSourceClusterGuid();
    }

    @XmlTransient
    @Transient
    public Collection<String> getSourceItems() {
        List<String> items = new ArrayList<String>();

        if ( summary != null ) {
            for ( MigratedItem item : summary.getMigratedItems() ) {
                if ( item.getSourceHeader() != null &&
                     item.getSourceHeader().getExternalId() != null ) {
                    if ( item.getSourceHeader().getType() == EntityType.FOLDER )
                        continue;
                    
                    items.add( item.getSourceHeader().getExternalId() );
                }
            }
        }

        return items;
    }

    @Transient
    public String getTargetClusterName() {
        return summary == null ? null : summary.getTargetClusterName();
    }

    @Transient
    public String getTargetClusterGuid() {
        return summary == null ? null : summary.getTargetClusterGuid();
    }

    @XmlTransient
    @Transient
    public Collection<String> getTargetItems() {
        List<String> items = new ArrayList<String>();

        if ( summary != null ) {
            for ( MigratedItem item : summary.getMigratedItems() ) {
                if ( item.getTargetHeader() != null &&
                     item.getTargetHeader().getExternalId() != null ) {
                    items.add( item.getTargetHeader().getExternalId() );                    
                }
            }
        }

        return items;
    }

    @XmlTransient
    @Transient
    public String getTargetFolderId() {
        String id = "";

        if ( summary != null ) {
            for ( MigratedItem item : summary.getMigratedItems() ) {
                if ( item.getTargetHeader() != null &&
                     item.getTargetHeader().getExternalId() != null &&
                     item.getTargetHeader().getType() == EntityType.FOLDER &&
                     item.getTargetHeader().getDescription() != null &&
                     item.getTargetHeader().getDescription().equals( summary.getTargetFolderPath() )) {
                    id = item.getTargetHeader().getExternalId();
                    break;
                }
            }
        }

        return id;
    }

    public String serializeXml() {
        StringWriter out = new StringWriter( bundleXml==null ? 4096 : bundleXml.length() );
        JAXB.marshal( this, out );
        return out.toString();
    }

    public static MigrationRecord deserializeXml(String xml) {
        return JAXB.unmarshal(new StringReader(xml), MigrationRecord.class);
    }

    void calculateSize() {
        try {
            this.size = MigrationArtifactResource.zip( this.serializeXml() ).length;
        } catch ( IOException ioe ) {
            this.size = 1; // set to non-zero value
        }
    }    
}
