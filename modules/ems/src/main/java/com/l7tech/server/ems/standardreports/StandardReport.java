package com.l7tech.server.ems.standardreports;

import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.imp.NamedEntityImp;
import com.l7tech.server.ems.enterprise.SsgCluster;
import org.hibernate.annotations.*;

import javax.persistence.CascadeType;
import javax.persistence.*;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.HashSet;
import java.util.Set;

/**
 * 
 */
@Entity
@Proxy(lazy=false)
@Table(name="report")
public class StandardReport extends NamedEntityImp {

    //- PUBLIC

    @Column(name="time", nullable=false, updatable=false)
    public long getTime() {
        return time;
    }

    public void setTime( final long time ) {
        this.time = time;
    }

    @Column(name="provider", nullable=false)
    @Type(type = "com.l7tech.server.util.GoidType")
    public Goid getProvider() {
        return provider;
    }

    public void setProvider( final Goid providerId ) {
        this.provider = providerId;
    }

    @Column(name="user_id", nullable=false, length=255)
    public String getUserId() {
        return userId;
    }

    public void setUserId( final String userId ) {
        this.userId = userId;
    }

    @Column(name="description", nullable=false, length=4096)
    public String getDescription() {
        return description;
    }

    public void setDescription( final String description ) {
        this.description = description;
    }

    @Column(name="status", nullable=false, length=32)
    public String getStatus() {
        return status;
    }

    public void setStatus( final String status ) {
        this.status = status;
    }

    @Column(name="status_message", length=255)
    public String getStatusMessage() {
        return statusMessage;
    }

    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }

    @Column(name="status_time", nullable=false)
    public long getStatusTime() {
        return statusTime;
    }

    public void setStatusTime( final long statusTime ) {
        this.statusTime = statusTime;
    }

    @Column(name="submission_host", nullable=false)
    public String getSubmissionHost() {
        return submissionHost;
    }

    public void setSubmissionHost(String submissionHost) {
        this.submissionHost = submissionHost;
    }

    @Column(name="submission_id", nullable=false, length=36)
    public String getSubmissionId() {
        return submissionId;
    }

    public void setSubmissionId(String submissionId) {
        this.submissionId = submissionId;
    }

    @ManyToOne(optional=false)
    @JoinColumn(name="ssg_cluster_oid", nullable=false)
    public SsgCluster getSsgCluster() {
        return cluster;
    }

    public void setSsgCluster( final SsgCluster ssgCluster ) {
        this.cluster = ssgCluster;
    }

    @OneToMany(cascade=CascadeType.ALL, fetch=FetchType.EAGER, mappedBy="report")
    @Fetch(FetchMode.SUBSELECT)
    @Cascade({org.hibernate.annotations.CascadeType.DELETE_ORPHAN, org.hibernate.annotations.CascadeType.ALL})
    @OnDelete(action=OnDeleteAction.CASCADE)
    @BatchSize(size=50)
    public Set<StandardReportArtifact> getArtifacts() {
        return artifacts;
    }

    public void setArtifacts( final Set<StandardReportArtifact> artifacts ) {
        this.artifacts = artifacts;
    }

    //- PRIVATE

    private String description;   // summary
    private String status;

    private long time;  // created
    private long statusTime;
    private String statusMessage;

    private SsgCluster cluster;
    private String submissionHost;
    private String submissionId;

    private Goid provider;
    private String userId;

    private Set<StandardReportArtifact> artifacts = new HashSet<StandardReportArtifact>();

}
