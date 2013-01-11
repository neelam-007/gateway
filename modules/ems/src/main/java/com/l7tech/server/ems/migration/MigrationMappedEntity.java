package com.l7tech.server.ems.migration;


import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.ExternalEntityHeader;

import javax.persistence.Embeddable;
import javax.persistence.Column;
import javax.persistence.Enumerated;
import javax.persistence.EnumType;


/**
 *
 */
@Embeddable
public class MigrationMappedEntity {

    //- PUBLIC

    public MigrationMappedEntity() {
    }

    @Column(name="entity_type", length=128)
    @Enumerated(EnumType.STRING)
    public EntityType getEntityType() {
        return entityType;
    }

    public void setEntityType(EntityType entityType) {
        this.entityType = entityType;
    }

    @Column(name="entity_id", length=512)
    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    @Column(name="external_id", length=512)
    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    @Column(name="entity_value", length=8192)
    public String getEntityValue() {
        return entityValue;
    }

    public void setEntityValue(String entityValue) {
        this.entityValue = entityValue;
    }

    @Column(name="entity_version")
    public Integer getEntityVersion() {
        return entityVersion;
    }

    public void setEntityVersion(Integer entityVersion) {
        this.entityVersion = entityVersion;
    }

    @Column(name="entity_name", length=256)
    public String getEntityName() {
        return entityName;
    }

    public void setEntityName(String entityName) {
        this.entityName = entityName;
    }

    @Column(name="entity_description", length=1024)
    public String getEntityDescription() {
        return entityDescription;
    }

    public void setEntityDescription(String entityDescription) {
        this.entityDescription = entityDescription;
    }

    @SuppressWarnings({"deprecation"})
    public static ExternalEntityHeader asEntityHeader( final MigrationMappedEntity entity ) {
        return new ExternalEntityHeader(entity.getExternalId(), entity.getEntityType(), entity.getEntityId(), entity.getEntityName(), entity.getEntityDescription(), entity.getEntityVersion());
    }

    //- PRIVATE

    private EntityType entityType;
    private String externalId;
    private String entityId;
    private String entityValue;
    private Integer entityVersion;
    private String entityName;
    private String entityDescription;
}
