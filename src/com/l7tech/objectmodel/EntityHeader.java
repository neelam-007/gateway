/*
 * Layer 7 Technologies, inc.
 * User: flascelles
 * Date: May 9, 2003
 *
 * $Id$
 */

package com.l7tech.objectmodel;

import com.l7tech.objectmodel.imp.NamedEntityImp;

/**
 * Header objects are used to refer to objects in find methods
 * of all managers
 * @version $Revision$
 * @author flascelles
 */
public class EntityHeader extends NamedEntityImp {

    public EntityHeader(long oid, EntityType type, String name, String description) {
        setOid(oid);
        strId = Long.toString(oid);
        setType(type);
        setName(name);
        setDescription(description);
    }

    public EntityHeader(String id, EntityType type, String name, String description) {
        setStrId(id);
        setType(type);
        setName(name);
        setDescription(description);
    }

    public EntityHeader() {
        type = EntityType.UNDEFINED;
        description = "";
    }

    public void setOid( long oid ) {
        _oid = oid;
        strId = Long.toString(oid);
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

    public String toString() {
        return "EntityHeader. Name=" + getName() + ", oid=" + getOid() + ", description=" + description;
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
        if (strId != null) {
            try {
                _oid = Long.parseLong(strId);
            } catch (Exception e) {
                _oid = DEFAULT_OID;
            }
        }
    }

    // ************************************************
    // PRIVATES
    // ************************************************
    private EntityType type;
    private String description;
    private String strId;
}
