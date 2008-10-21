/*
 * Layer 7 Technologies, inc.
 * User: flascelles
 * Date: May 9, 2003
 *
 * $Id$
 */

package com.l7tech.objectmodel;


import java.io.Serializable;

/**
 * Header objects are used to refer to objects in find methods
 * of all managers
 * @version $Revision$
 * @author flascelles
 */
public class EntityHeader implements Serializable, Comparable {

    public EntityHeader(String id, EntityType type, String name, String description) {
        this.strId = id;
        this.type = type;
        this.name = name;
        this.description = description;
    }

    public EntityHeader(long oid, EntityType type, String name, String description) {
        this.strId = Long.toString(oid);
        this.type = type;
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

    public void setOid( long oid ) {
        strId = Long.toString(oid);
    }

    public long getOid() {
        try {
            return Long.parseLong(strId);
        } catch (Exception e) {
            return DEFAULT_OID;
        }
    }

    public EntityType getType() {
        return type;
    }

    public void setType(EntityType type) {
        this.type = type;
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

    /**
     * if the oid was set but not the StrId, the strId will be returned as Long.toString(oid)
     * @return the id
     */
    public String getStrId() {
        return strId;
    }

    /**
     * oid and strId are interchangeable. setting this will override the oid property if it contains a parseable string.
     */
    public void setStrId(String strId) {
        this.strId = strId;
    }

    // ************************************************
    // PRIVATES
    // ************************************************

    private static final long serialVersionUID = -1752153501322477805L;

    private EntityType type;
    private String description;
    private String strId;
    protected String name;
    private static final long DEFAULT_OID = -1;

    public int compareTo(Object o) {
        if (o == null) throw new NullPointerException();
        EntityHeader other = (EntityHeader)o;
        // bugzilla 2786: if only one of the two is a MAXED_OUT_SEARCH_RESULT, then it should be on top
        if (!(this instanceof LimitExceededMarkerIdentityHeader && other instanceof LimitExceededMarkerIdentityHeader)) {
            if (this instanceof LimitExceededMarkerIdentityHeader) {
                return -1;
            } else if (other instanceof LimitExceededMarkerIdentityHeader) {
                return 1;
            }
        }
        if (strId != null && other.strId != null) {
            if (strId.equals(other.strId)) return 0;
        }
        if ( name.equals(((EntityHeader)o).name) ) {
            return type.compareTo(((EntityHeader)o).type);
        }
        return name.compareTo(((EntityHeader)o).name);
    }
}
