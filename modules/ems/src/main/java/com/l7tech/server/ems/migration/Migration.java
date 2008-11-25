package com.l7tech.server.ems.migration;

import com.l7tech.objectmodel.imp.NamedEntityImp;
import com.l7tech.server.ems.enterprise.SsgCluster;

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
public class Migration extends NamedEntityImp {
    private long timeCreated;
    private SsgCluster sourceCluster;
    private SsgCluster destinationCluster;
    private String summary;

    public Migration() {
    }

    public Migration(String name, long timeCreated, SsgCluster sourceCluster, SsgCluster destinationCluster, String summary) {
        _name = name;
        this.timeCreated = timeCreated;
        this.sourceCluster = sourceCluster;
        this.destinationCluster = destinationCluster;
        this.summary = summary;
    }

    @Column(name="time_created", nullable=false)
    public long getTimeCreated() {
        return timeCreated;
    }

    public void setTimeCreated(long timeCreated) {
        this.timeCreated = timeCreated;
    }

    @ManyToOne(optional=false)
    @JoinColumn(name="destination_cluster_oid", nullable=false)
    public SsgCluster getDestinationCluster() {
        return destinationCluster;
    }

    public void setDestinationCluster(SsgCluster destinationCluster) {
        this.destinationCluster = destinationCluster;
    }

    @ManyToOne(optional=false)
    @JoinColumn(name="source_cluster_oid", nullable=false)
    public SsgCluster getSourceCluster() {
        return sourceCluster;
    }

    public void setSourceCluster(SsgCluster sourceCluster) {
        this.sourceCluster = sourceCluster;
    }

    @Column(name="summary")
    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }
}
