package com.l7tech.server.ems.migration;

import com.l7tech.objectmodel.imp.GoidEntityImp;
import com.l7tech.server.ems.enterprise.SsgCluster;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.Proxy;

import javax.persistence.*;

/**
 * 
 */
@Entity
@Proxy(lazy=false)
@Table(name="migration_mapping")
public class MigrationMappingRecord extends GoidEntityImp {

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
        @AttributeOverride(name="externalId",  column=@Column(name="source_external_id", length=512)),
        @AttributeOverride(name="entityId", column=@Column(name="source_entity_id", length=512)),
        @AttributeOverride(name="entityValue", column=@Column(name="source_entity_value", length=8192)),
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
        @AttributeOverride(name="externalId",  column=@Column(name="target_external_id", length=512)),
        @AttributeOverride(name="entityId", column=@Column(name="target_entity_id", length=512)),
        @AttributeOverride(name="entityValue", column=@Column(name="target_entity_value", length=8192)),
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

    public static MigrationMappingRecord reverse(MigrationMappingRecord other) {
        if (other == null) return null;
        MigrationMappingRecord reversed = new MigrationMappingRecord();
        reversed.setSourceCluster(other.getTargetCluster());
        reversed.setSource(other.getTarget());
        reversed.setTargetCluster(other.getSourceCluster());
        reversed.setTarget(other.getSource());
        reversed.setSameEntity(other.isSameEntity());
        // don't set timestamp, since the reversed record doesn't actually exist in DB
        return reversed;
    }
}
