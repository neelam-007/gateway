package com.l7tech.server.ems.migration;

import com.l7tech.objectmodel.imp.NamedEntityImp;
import com.l7tech.server.ems.enterprise.SsgCluster;
import com.l7tech.identity.User;

import javax.persistence.*;

/**
 * This entity class stores the information of a migration such as name, id, time created,
 * source cluster, and destination cluster.
 *
 * @Copyright: Layer 7 Tech. Inc.
 * @Author: ghuang
 * @Date: Nov 19, 2008
 */
@Entity
@Table(name="migration")
public class MigrationRecord extends NamedEntityImp {

    private long timeCreated;
    private long provider;
    private String userId;
    private SsgCluster sourceCluster;
    private SsgCluster targetCluster;
    private String summary;
    private byte[] data;

    public MigrationRecord() {
    }

    public MigrationRecord( final String name,
                            final long timeCreated,
                            final User user,
                            final SsgCluster sourceCluster,
                            final SsgCluster targetCluster,
                            final String summary,
                            final byte[] data) {
        this._name = name==null ? "" : name;
        this.timeCreated = timeCreated;
        this.provider = user.getProviderId();
        this.userId = user.getId();
        this.sourceCluster = sourceCluster;
        this.targetCluster = targetCluster;
        this.summary = summary;
        this.data = data;
    }

    @Column(name="time_created", nullable=false)
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

    @ManyToOne(optional=false)
    @JoinColumn(name="target_cluster_oid", nullable=false)
    public SsgCluster getTargetCluster() {
        return targetCluster;
    }

    public void setTargetCluster(SsgCluster targetCluster) {
        this.targetCluster = targetCluster;
    }

    @ManyToOne(optional=false)
    @JoinColumn(name="source_cluster_oid", nullable=false)
    public SsgCluster getSourceCluster() {
        return sourceCluster;
    }

    public void setSourceCluster(SsgCluster sourceCluster) {
        this.sourceCluster = sourceCluster;
    }

    @Column(name="summary", length=10240)
    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    @Basic(fetch=FetchType.LAZY)
    @Column(name="data", length=Integer.MAX_VALUE)
    @Lob
    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }
}
