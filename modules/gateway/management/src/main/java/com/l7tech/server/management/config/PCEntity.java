/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.server.management.config;

import com.l7tech.objectmodel.NamedEntity;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.Calendar;

/**
 * Entities in the Process Controller all have a GUID and timestamp (for optimistic locking); most have a name as well.
 * @author alex 
 */
@MappedSuperclass
public abstract class PCEntity implements NamedEntity {
    protected String guid;
    protected String name;
    protected Calendar timestamp;

    @Id @GeneratedValue(generator="system-uuid")
    @GenericGenerator(name="system-uuid", strategy="uuid")
    public String getGuid() {
        return guid;
    }

    public String getName() {
        return name;
    }

    @javax.persistence.Version
    @Temporal(TemporalType.TIMESTAMP)
    public Calendar getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Calendar timestamp) {
        this.timestamp = timestamp;
    }

    public void setGuid(String guid) {
        this.guid = guid;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Transient
    public String getId() {
        return guid;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PCEntity pcEntity = (PCEntity)o;

        if (guid != null ? !guid.equals(pcEntity.guid) : pcEntity.guid != null) return false;
        if (name != null ? !name.equals(pcEntity.name) : pcEntity.name != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = guid != null ? guid.hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        return result;
    }
}
