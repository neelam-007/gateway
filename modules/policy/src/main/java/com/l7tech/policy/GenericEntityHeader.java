package com.l7tech.policy;

import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Entity header for a GenericEntity.
 */
@XmlRootElement
public class GenericEntityHeader extends EntityHeader {
    private String entityClassName;

    public GenericEntityHeader(GenericEntity entity) {
        super(entity.getId(), EntityType.GENERIC, entity.getName(), entity.getDescription(), entity.getVersion());
        this.entityClassName = entity.getEntityClassName();
    }

    public GenericEntityHeader(String id, String name, String description, Integer version, String entityClassName) {
        super(id, EntityType.GENERIC, name, description, version);
        this.entityClassName = entityClassName;
    }

    public String getEntityClassName() {
        return entityClassName;
    }

    public void setEntityClassName(String entityClassName) {
        this.entityClassName = entityClassName;
    }
}
