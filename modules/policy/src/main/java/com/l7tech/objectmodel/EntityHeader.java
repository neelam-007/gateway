/*
 * Copyright (C) 2003-2008 Layer 7 Technologies Inc.
 */
package com.l7tech.objectmodel;


import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;

/**
 * Header objects are used to refer to objects in find methods
 * of all managers
 *
 * @author flascelles
 */
@XmlRootElement
@XmlSeeAlso(IdentityHeader.class)
public class EntityHeader extends EntityHeaderRef {
    public EntityHeader(String id, EntityType type, String name, String description) {
        super(type, id);
        this.name = name;
        this.description = description;
    }

    public EntityHeader(long oid, EntityType type, String name, String description) {
        super(type, Long.toString(oid));
        this.name = name;
        this.description = description;
    }

    public EntityHeader() {
        type = EntityType.ANY;
        description = "";
    }

    public String getName() {
        return name;
    }

    public void setName( String name ) {
        this.name = name;
    }

    @XmlAttribute
    public void setOid( long oid ) {
        strId = Long.toString(oid);
    }

    public long getOid() {
        if (strId == null || strId.isEmpty()) return DEFAULT_OID;
        try {
            return Long.parseLong(strId);
        } catch (Exception e) {
            return DEFAULT_OID;
        }
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof EntityHeader)) return false;
        EntityHeader theotherone = (EntityHeader)obj;
        if (theotherone.type != this.type) return false;
        if (getStrId() == null) {
            return theotherone.getStrId() == null;
        }
        return getStrId().equals(theotherone.getStrId());
    }

    public String toStringVerbose() {
        return "EntityHeader. Name=" + getName() + ", oid=" + getStrId() + ", description=" + description +
               ", type = " + type.toString();
    }

    public String toString() {
        return name;
    }

    public int hashCode() {
        int result;
        result = (type != null ? type.hashCode() : 0);
        result = 31 * result + (strId != null ? strId.hashCode() : 0);
        return result;
    }

    // ************************************************
    // PRIVATES
    // ************************************************

    private static final long serialVersionUID = -1752153501322477805L;

    private String description;
    protected String name;
    private static final long DEFAULT_OID = -1;

    public int compareTo(Object o) {
        if (o == null) throw new NullPointerException();
        EntityHeader other = (EntityHeader)o;
        if (strId != null && other.strId != null) {
            if (strId.equals(other.strId)) return 0;
        }
        if ( name.equals(((EntityHeader)o).name) ) {
            return type.compareTo(((EntityHeader)o).type);
        }
        return name.compareTo(((EntityHeader)o).name);
    }
}
