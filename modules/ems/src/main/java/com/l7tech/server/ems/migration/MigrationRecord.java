package com.l7tech.server.ems.migration;

import com.l7tech.objectmodel.imp.NamedEntityImp;
import com.l7tech.server.management.migration.bundle.MigrationBundle;
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

    private MigrationSummary summary;
    private String summaryXml;

    private MigrationBundle bundle;
    private String bundleXml;

    private int size;

    public MigrationRecord() {
    }

    public MigrationRecord( final String name,
                            final User user,
                            final MigrationSummary summary,
                            final MigrationBundle bundle) {
        this._name = name==null ? "" : name;
        this.provider = user.getProviderId();
        this.userId = user.getId();
        this.timeCreated = summary.getTimeCreated();
        setSummaryXml(summary.serializeXml());
        setBundleXml(bundle.serializeXml());
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
    public String getSourceClusterName() {
        return summary == null ? null : summary.getSourceClusterName();
    }

    @Transient
    public String getSourceClusterGuid() {
        return summary == null ? null : summary.getSourceClusterGuid();
    }

    @Transient
    public String getTargetClusterName() {
        return summary == null ? null : summary.getTargetClusterName();
    }

    @Transient
    public String getTargetClusterGuid() {
        return summary == null ? null : summary.getTargetClusterGuid();
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
