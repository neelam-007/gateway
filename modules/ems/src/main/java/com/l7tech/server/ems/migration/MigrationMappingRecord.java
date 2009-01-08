package com.l7tech.server.ems.migration;

import com.l7tech.objectmodel.imp.PersistentEntityImp;
import com.l7tech.server.ems.enterprise.SsgCluster;

import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Column;
import javax.persistence.ManyToOne;
import javax.persistence.JoinColumn;
import javax.persistence.Embedded;
import javax.persistence.AttributeOverrides;
import javax.persistence.AttributeOverride;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

/**
 * 
 */
@Entity
@Table(name="migration_mapping")
public class MigrationMappingRecord extends PersistentEntityImp {

    //- PUBLIC

    public MigrationMappingRecord() {
    }

    @Column(name="created_time", updatable=false, nullable=false)
    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @ManyToOne(optional=false)
    @OnDelete(action=OnDeleteAction.CASCADE)
    @JoinColumn(name="source_cluster_oid", nullable=false)
    public SsgCluster getSourceCluster() {
        return sourceCluster;
    }

    public void setSourceCluster(SsgCluster sourceCluster) {
        this.sourceCluster = sourceCluster;
    }

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name="entityType", column=@Column(name="source_entity_type", length=128)),
        @AttributeOverride(name="entityProviderId",  column=@Column(name="source_entity_provider_id", length=512)),
        @AttributeOverride(name="entityId", column=@Column(name="source_entity_id", length=512)),
        @AttributeOverride(name="entityValue", column=@Column(name="source_entity_value", length=1024)),
        @AttributeOverride(name="entityVersion", column=@Column(name="source_entity_version")),
        @AttributeOverride(name="entityName", column=@Column(name="source_entity_name", length=256)),
        @AttributeOverride(name="entityDescription", column=@Column(name="source_entity_description", length=1024))
    })
    public MigrationMappedEntity getSource() {
        return source;
    }

    public void setSource(MigrationMappedEntity source) {
        this.source = source;
    }

    @ManyToOne(optional=false)
    @OnDelete(action=OnDeleteAction.CASCADE)
    @JoinColumn(name="target_cluster_oid", nullable=false)
    public SsgCluster getTargetCluster() {
        return targetCluster;
    }

    public void setTargetCluster(SsgCluster targetCluster) {
        this.targetCluster = targetCluster;
    }

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name="entityType", column=@Column(name="target_entity_type", length=128)),
        @AttributeOverride(name="entityProviderId",  column=@Column(name="target_entity_provider_id", length=512)),
        @AttributeOverride(name="entityId", column=@Column(name="target_entity_id", length=512)),
        @AttributeOverride(name="entityValue", column=@Column(name="target_entity_value", length=1024)),
        @AttributeOverride(name="entityVersion", column=@Column(name="target_entity_version")),
        @AttributeOverride(name="entityName", column=@Column(name="target_entity_name", length=256)),
        @AttributeOverride(name="entityDescription", column=@Column(name="target_entity_description", length=1024))
    })
    public MigrationMappedEntity getTarget() {
        return target;
    }

    public void setTarget(MigrationMappedEntity target) {
        this.target = target;
    }

    @Column(name="same_entity", updatable=false, nullable=false)
    public boolean isSameEntity() {
        return sameEntity;
    }

    public void setSameEntity(boolean sameEntity) {
        this.sameEntity = sameEntity;
    }

    //- PRIVATE

    private long timestamp;
    private SsgCluster sourceCluster;
    private MigrationMappedEntity source;
    private SsgCluster targetCluster;
    private MigrationMappedEntity target;
    private boolean sameEntity;
}
