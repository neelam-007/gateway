/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.objectmodel;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;

/** @author alex */
@XmlRootElement(name="entityHeaderRef")
public class EntityHeaderRef implements Serializable, Comparable {
    protected EntityType type;
    protected String strId;

    public EntityHeaderRef(EntityType type, String id) {
        this.type = type;
        this.strId = id;
    }

    public static EntityHeaderRef fromOther(EntityHeaderRef headerRef) {
        return new EntityHeaderRef(headerRef.getType(), headerRef.getStrId());
    }

    protected EntityHeaderRef() { }

    @XmlAttribute
    public EntityType getType() {
        return type;
    }

    public void setType(EntityType type) {
        this.type = type;
    }

    /**
     * if the oid was set but not the StrId, the strId will be returned as Long.toString(oid)
     * @return the id
     */
    @XmlAttribute
    public String getStrId() {
        return strId;
    }

    /**
     * oid and strId are interchangeable. setting this will override the oid property if it contains a parseable string.
     */
    public void setStrId(String strId) {
        this.strId = strId;
    }

    public int compareTo(Object o) {
        if (o == null) throw new NullPointerException();
        EntityHeaderRef other = (EntityHeaderRef)o;
        if (type == other.type) {
            return strId != null && other.strId != null ? strId.compareTo(other.strId) : strId == null ? -1 : 1;
        } else {
            return type.compareTo(other.type);
        }
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EntityHeaderRef)) return false;

        EntityHeaderRef that = (EntityHeaderRef) o;

        return !(strId != null ? !strId.equals(that.strId) : that.strId != null) && type == that.type;
    }

    public int hashCode() {
        int result;
        result = (type != null ? type.hashCode() : 0);
        result = 31 * result + (strId != null ? strId.hashCode() : 0);
        return result;
    }

    public String toString() {
        return "EntityHeaderRef: type=" + type + " id=" + strId;
    }
}
